"""초기 가중치와 실제 갱신 증거를 보존하는 오프라인 학습 CLI입니다."""
import argparse
from dataclasses import asdict
import datetime
import hashlib
import json
import math
from pathlib import Path
import random
import shutil
import sys
import time
import torch
from .batching import collate, encode_row, loss_for_batch
from .evaluate import evaluate_rows, sha256, load_model
from .model import ModelConfig, PlanTransformer
from .schema import TOKENS
from .tokenizer import CodepointTokenizer
from .literal_codec import LiteralSlotCodec
from .codec_contract import bind_model_codec, input_contract, load_codec, validate_training_approval
from .rephrase import Edit, apply_edits


NEUTRAL_PREFIXES = (
    "안내: ", "작업 내용: ", "다음은 작업 내용입니다. ", "아래는 작업 내용입니다. ",
    "다음 문장은 작업 내용입니다. ", "아래 문장은 작업 내용입니다. ",
    "작업할 내용은 아래에 있어요. ", "아래 문구에 작업 내용이 있어요. ",
    "여기에 작업 내용이 있어요. ", "다음에 적은 내용이 작업 내용입니다. ",
    "다음 문구는 제가 정한 작업 내용입니다. ", "아래 문장으로 작업 내용을 전해요. ",
)


def neutral_prefix_metadata(probability, seed):
    """공개 고정 문구와 훈련 전용 추첨 계약을 기록합니다."""
    if type(probability) not in (int, float) or not math.isfinite(probability) or not 0 <= probability <= 1:
        raise ValueError("중립 안내문 확률은 0~1의 유한한 수여야 합니다")
    if type(seed) is not int:
        raise ValueError("중립 안내문 seed는 정수여야 합니다")
    return {"training_only": True, "probability": probability, "seed": seed,
        "pool": list(NEUTRAL_PREFIXES),
        "pool_sha256": hashlib.sha256(json.dumps(list(NEUTRAL_PREFIXES), ensure_ascii=False, separators=(",", ":")).encode("utf-8")).hexdigest(),
        "rng_separate_from_shuffle_and_byte": True, "sampling_independent_of_gold_and_kind": True,
        "prefix_choice_uniform": True, "resample_after_overflow": False,
        "overflow_policy": "지정된 codec 길이 초과만 원행 유지·이유 계수; 절단 없음",
        "inference_policy_changed": False, "applied_in_init": False}


def _prefix_count(stats, row, stage, prefix_index=None):
    """추첨이 끝난 뒤에만 정답 상태·종류별 감사 계수를 읽습니다."""
    if stats is None:
        return
    keys = [stage, "status."+str(row.get("plan", {}).get("status", "missing"))+"."+stage,
        "kind."+str(row.get("kind", "missing"))+"."+stage]
    if prefix_index is not None:
        keys.append(f"prefix.P{prefix_index:02d}."+stage)
    for key in keys:
        stats[key] = stats.get(key, 0)+1


def neutral_prefix_row(row, tokenizer, config, *, probability=0.0, rng=None, stats=None):
    """훈련 행 앞에만 중립 문구를 붙이며 원행·문맥·골드를 변경하지 않습니다."""
    if type(probability) not in (int, float) or not math.isfinite(probability) or not 0 <= probability <= 1:
        raise ValueError("중립 안내문 확률은 0~1의 유한한 수여야 합니다")
    if stats is not None:
        stats["rows_seen"] = stats.get("rows_seen", 0)+1
    # 기본값0은 codec·편집기·난수 생성기를 추가 호출하지 않습니다.
    if probability == 0:
        _prefix_count(stats, row, "not_selected")
        return row
    if row.get("split") != "train" or rng is None:
        raise ValueError("훈련 행과 별도 중립 안내문 RNG가 필요합니다")
    if rng.random() >= probability:
        _prefix_count(stats, row, "not_selected")
        return row
    index = rng.randrange(len(NEUTRAL_PREFIXES))
    _prefix_count(stats, row, "selected", index)
    prefix = NEUTRAL_PREFIXES[index]
    changed = apply_edits(row, [Edit(0, 0, prefix, "training-neutral-prefix")])
    reason = None
    try:
        encoded = tokenizer.encode_request(changed["command"], changed["context"])
    except ValueError as error:
        # 의미·문맥·인용·문자열 오류는 숨기지 않고, codec의 명시한 두 길이 오류만 구분합니다.
        known_overflow = {
            "기존 입력1024 한도 초과: 원문을 자르지 않습니다": "old535_limit",
            "새 입력1024 한도 초과: 슬롯·문맥을 자르지 않습니다": "new540_limit",
        }
        if not isinstance(tokenizer, LiteralSlotCodec) or str(error) not in known_overflow:
            raise
        reason = known_overflow[str(error)]
    else:
        length = len(encoded["ids"])
        if length > 1024:
            reason = "old535_limit"
        elif length > config.max_input:
            reason = "configured_limit"
        elif stats is not None:
            stats["max_applied_input_tokens"] = max(stats.get("max_applied_input_tokens", 0), length)
            key = f"input_tokens.{length}.applied"
            stats[key] = stats.get(key, 0)+1
    if reason is not None:
        _prefix_count(stats, row, reason, index)
        return row
    _prefix_count(stats, row, "applied", index)
    if stats is not None:
        key = f"codepoints.{len(prefix)}.applied"
        stats[key] = stats.get(key, 0)+1
    return changed


def read_jsonl(path):
    parts = Path(path).read_text(encoding="utf-8").split("\n")
    if parts[-1] == "": parts.pop()
    return [json.loads(part) for part in parts]


def validate_training_rows(rows, tokenizer, config, runtime):
    """전행을 검사하되 인코딩 결과를 보존하지 않습니다."""
    for row in runtime.rows(rows):
        encode_row(row, tokenizer, config.max_input, config.max_output)


def encode_selected_rows(rows, selected, tokenizer, config, *, byte_probability=0.0, augmentation_rng=None, augmentation_stats=None, neutral_prefix_probability=0.0, neutral_prefix_rng=None, neutral_prefix_stats=None):
    """선택 배치에만 안내문을 추첨하고 기존 byte RNG와 인코딩 순서를 유지합니다."""
    selected_rows = [neutral_prefix_row(rows[i], tokenizer, config, probability=neutral_prefix_probability, rng=neutral_prefix_rng, stats=neutral_prefix_stats) for i in selected] if neutral_prefix_probability or neutral_prefix_stats is not None else [rows[i] for i in selected]
    if byte_probability:
        return [encode_row(row, tokenizer, config.max_input, config.max_output, byte_probability=byte_probability, augmentation_rng=augmentation_rng, augmentation_stats=augmentation_stats) for row in selected_rows]
    return [encode_row(row, tokenizer, config.max_input, config.max_output) for row in selected_rows]


def configure_cuda_resources(device):
    """소유 caching allocator 예산을 설정하며 CPU에서는 CUDA API를 호출하지 않습니다."""
    if device != "cuda":
        return {"enabled": False, "caching_allocator_limit_bytes": None, "visible_total_bytes": None,
                "fraction": None, "allocator_backend": None, "whole_process_or_wddm_hard_limit": False}
    if not torch.cuda.is_available():
        raise ValueError("CUDA 실제 연산 환경이 없습니다")
    limit, total = 4 * 1024 ** 3, torch.cuda.get_device_properties(0).total_memory
    fraction = limit / total
    torch.cuda.set_per_process_memory_fraction(fraction, device=0)
    torch.cuda.reset_peak_memory_stats()
    return {"enabled": True, "caching_allocator_limit_bytes": limit, "visible_total_bytes": total,
            "fraction": fraction, "allocator_backend": torch.cuda.memory.get_allocator_backend(),
            "whole_process_or_wddm_hard_limit": False}


def cuda_memory_record(device, step):
    """1단계·50배수에서만 통계를 읽으며 미지원 값은 N/A인 None으로 보존합니다."""
    if device != "cuda" or (step != 1 and step % 50 != 0):
        return None
    backend = torch.cuda.memory.get_allocator_backend()
    stats = torch.cuda.memory_stats()
    native = backend == "native"
    return {"allocator_backend": backend, "allocated_bytes": torch.cuda.memory_allocated(),
            "reserved_bytes": torch.cuda.memory_reserved(), "max_allocated_bytes": torch.cuda.max_memory_allocated(),
            "max_reserved_bytes": torch.cuda.max_memory_reserved(),
            "inactive_split_bytes": stats.get("inactive_split_bytes.all.current") if native else None,
            "inactive_split_bytes_peak": stats.get("inactive_split_bytes.all.peak") if native else None,
            "num_alloc_retries": stats.get("num_alloc_retries") if native else None,
            "num_ooms": stats.get("num_ooms") if native else None,
            "native_backend_specific_stats": native}


def source_hashes():
    folder = Path(__file__).resolve().parent
    return {str(path.relative_to(folder)): sha256(path) for path in sorted(folder.rglob("*.py"))}


def checkpoint(path, model, metadata, step):
    payload = {"format": "pxe-plan-transformer-v1", "output_tokens": list(TOKENS), "config": asdict(model.config), "state_dict": model.state_dict(), "step": step, "metadata": metadata}
    if metadata.get("input_contract", {}).get("format") == "pxe-literal-slot-utf8-v1":
        if type(step) is not int or step < 0 or getattr(model, "input_contract", None) != metadata["input_contract"] or model.config.input_vocab != 540 or model.config.output_vocab != 72:
            raise ValueError("literal 체크포인트의 모델·step·codec 결합 오류")
        payload["format"] = "pxe-plan-transformer-v2"
        payload["input_contract"] = metadata["input_contract"]
    torch.save(payload, path)
    return sha256(path)


def epoch_progress(epoch_started, rows_processed, total_rows, full_completed_before):
    if any(type(value) is not int for value in (epoch_started, rows_processed, total_rows, full_completed_before)) or epoch_started < 1 or total_rows < 1 or not 0 <= rows_processed <= total_rows or full_completed_before < 0:
        raise ValueError("epoch 진행 계수 범위 오류")
    complete = rows_processed == total_rows
    return {"epochs_started": epoch_started, "full_epochs_completed": full_completed_before+int(complete), "last_epoch_rows_processed": rows_processed, "rows_per_full_epoch": total_rows, "last_epoch_complete": complete, "epochs_completed_legacy_semantics": "이전 필드 epochs_completed는 시작한 epoch 수이며 전체 완료 수가 아닙니다"}


def load_fixed_initial(path, config, manifest_hash, tokenizer_hash, hashes, device, *, tokenizer=None):
    model, initial = load_model(path, device, tokenizer=tokenizer, expected_tokenizer_sha256=tokenizer_hash)
    metadata = initial.get("metadata", {})
    if type(initial.get("step")) is not int or initial["step"] != 0 or metadata.get("mode") != "init" or model.config.to_dict() != config.to_dict():
        raise ValueError("승인된 초기0step 모델과 설정이 다릅니다")
    if metadata.get("manifest_sha256") != manifest_hash or metadata.get("tokenizer_sha256") != tokenizer_hash or metadata.get("source_hashes") != hashes:
        raise ValueError("초기 가중치의 자료·어휘·소스 결합이 다릅니다")
    return model


class TrainingRuntime:
    """모델 의미·최적화 설정을 바꾸지 않고 저장과 실행 기한만 관리합니다."""

    def __init__(self, args, *, clock=time.monotonic):
        self.clock, self.started = clock, clock()
        self.output = Path(args.output)
        self.wall_seconds = getattr(args, "wall_seconds", None)
        self.reserve = getattr(args, "save_reserve_seconds", 30)
        self.every = getattr(args, "checkpoint_every", 0)
        if type(self.every) is not int or self.every < 0 or type(self.reserve) is not int or self.reserve < 0:
            raise ValueError("저장 간격과 여유 시간은 비음수 정수여야 합니다")
        if self.wall_seconds is not None and (type(self.wall_seconds) is not int or self.wall_seconds <= self.reserve):
            raise ValueError("전체 기한은 저장 여유보다 커야 합니다")
        self.model, self.metadata, self.step, self.owns_output = None, None, 0, False

    def attach(self, model, metadata):
        self.model, self.metadata = model, metadata

    def check(self):
        if self.wall_seconds is not None and self.clock()-self.started >= self.wall_seconds-self.reserve:
            raise TimeoutError("저장 여유를 남긴 전체 실행 기한에 도달했습니다")

    def completed_step(self, step):
        self.step = step
        if self.every and step % self.every == 0:
            checkpoint(self.output / f"step-{step:06d}.pt", self.model, self.metadata, step)

    def rows(self, rows):
        for row in rows:
            self.check()
            yield row

    def record_failure(self, error):
        if not self.owns_output:
            return
        record = {"failure_kst": datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=9))).isoformat(),
            "argv": [sys.executable, *sys.orig_argv[1:]], "cwd": str(Path.cwd()),
            "error_type": type(error).__name__, "error": str(error), "actual_optimizer_steps": self.step,
            "elapsed_seconds_all_inclusive_run": self.clock()-self.started, "wall_seconds": self.wall_seconds,
            "save_reserve_seconds": self.reserve, "success": False, "partial_checkpoint_sha256": None}
        if self.metadata is not None and "neutral_prefix_counts" in self.metadata:
            record["neutral_prefix_counts"] = self.metadata["neutral_prefix_counts"]
        if self.model is not None and self.metadata is not None:
            try:
                record["partial_checkpoint_sha256"] = checkpoint(self.output / "partial.pt", self.model, self.metadata, self.step)
            except BaseException as save_error:
                record["partial_save_error"] = {"type": type(save_error).__name__, "message": str(save_error)}
        (self.output / "failure.json").write_text(json.dumps(record, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")


def run(args):
    runtime = TrainingRuntime(args)
    try:
        return _run(args, runtime)
    except BaseException as error:
        runtime.record_failure(error)
        raise


def _run(args, runtime):
    runtime.check()
    dataset, output = Path(args.dataset), Path(args.output)
    if output.exists(): raise ValueError("기존 학습 결과는 덮어쓰지 않습니다")
    manifest_path = dataset / "manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if sha256(dataset / "train.jsonl") != manifest["splits"]["train"]["sha256"]:
        raise ValueError("고정 train hash가 다릅니다")
    if args.mode == "train":
        if not args.approval: raise ValueError("최종 학습 전 부모의 독립 계약 승인 파일이 필요합니다")
        approval = json.loads(Path(args.approval).read_text(encoding="utf-8"))
        if not args.initial_checkpoint:
            raise ValueError("승인한 초기 가중치 파일이 필요합니다")
        validate_training_approval(approval, sha256(manifest_path), source_hashes(), sha256(args.initial_checkpoint))
    output.mkdir(parents=True)
    runtime.owns_output = True
    random.seed(args.seed); torch.manual_seed(args.seed)
    torch.set_num_threads(4)
    torch.set_num_interop_threads(1)
    cuda_resources = configure_cuda_resources(args.device)
    tokenizer = load_codec(dataset / "tokenizer.json")
    config = ModelConfig(input_vocab=tokenizer.size, output_vocab=len(TOKENS))
    model = PlanTransformer(config).to(args.device)
    if isinstance(tokenizer, LiteralSlotCodec):
        bind_model_codec(model, tokenizer)
    if args.mode == "train":
        model = load_fixed_initial(args.initial_checkpoint, config, sha256(manifest_path), sha256(dataset / "tokenizer.json"), source_hashes(), args.device, tokenizer=tokenizer)
    metadata = {"started_kst": datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=9))).isoformat(), "mode": args.mode, "seed": args.seed, "manifest_sha256": sha256(manifest_path), "tokenizer_sha256": sha256(dataset / "tokenizer.json"), "source_hashes": source_hashes(), "torch": str(torch.__version__), "device": args.device, "cpu_threads": torch.get_num_threads(), "parameter_numel": sum(p.numel() for p in model.parameters()), "trainable_numel": sum(p.numel() for p in model.parameters() if p.requires_grad), "config": asdict(config), "optimizer": {"name": "AdamW", "learning_rate": args.lr, "weight_decay": 0.01, "grad_clip": 1.0}, "batch_size": args.batch_size, "epochs_requested": args.epochs, "max_steps": args.max_steps, "test_reads": 0, "pretrained_weights": False}
    if isinstance(tokenizer, LiteralSlotCodec):
        metadata["input_contract"] = input_contract(tokenizer)
    metadata["dev_selection"] = {"samples": args.dev_samples, "order": "고정된 앞부분", "patience_epochs": args.patience, "criterion": "AST exact", "test_reads": 0}
    metadata["interop_threads"] = torch.get_num_interop_threads()
    metadata["data_loader_workers"] = 0
    metadata["cuda_allocator"] = cuda_resources
    metadata["runtime_limits"] = {"wall_seconds": runtime.wall_seconds, "save_reserve_seconds": runtime.reserve,
        "checkpoint_every_optimizer_steps": runtime.every, "external_capture_remains_hard_limit": True,
        "timeout_save_is_best_effort": True, "periodic_checkpoint_contains_optimizer_state": False}
    metadata["byte_augmentation"] = {"training_only": True, "probability": args.byte_augmentation_probability, "seed": args.byte_augmentation_seed, "rng_separate_from_shuffle": True, "include_context": True, "max_input": config.max_input, "overflow_policy": "문자 전체 확장이 남은 예산을 넘으면 그 문자의 기존 표현 유지; 잘라내기 없음", "inference_policy_changed": False}
    metadata["neutral_prefix_augmentation"] = neutral_prefix_metadata(getattr(args, "neutral_prefix_probability", 0.0), getattr(args, "neutral_prefix_seed", 1730002))
    runtime.attach(model, metadata)
    initial = {name: tensor.detach().cpu().clone() for name, tensor in model.state_dict().items()}
    if args.mode == "train":
        shutil.copyfile(args.initial_checkpoint, output / "initial.pt")
        metadata["initial_checkpoint_sha256"] = sha256(output / "initial.pt")
        metadata["initial_origin"] = "독립 계약 승인에 고정한 초기파일의 바이트 동일 복사"
        metadata["parent_approval_sha256"] = sha256(args.approval)
    else:
        metadata["initial_checkpoint_sha256"] = checkpoint(output / "initial.pt", model, metadata, 0)
    (output / "initial_metadata.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    if args.mode == "init":
        print(json.dumps(metadata, ensure_ascii=False, indent=2)); return
    rows = read_jsonl(dataset / "train.jsonl")
    if args.mode == "smoke": rows = rows[:32]
    validate_training_rows(rows, tokenizer, config, runtime)
    augmentation_rng = random.Random(args.byte_augmentation_seed)
    augmentation_epochs = []
    neutral_prefix_rng = random.Random(metadata["neutral_prefix_augmentation"]["seed"])
    prefix_epochs = []
    dev_rows = []
    if args.mode == "train":
        if sha256(dataset / "dev.jsonl") != manifest["splits"]["dev"]["sha256"]:
            raise ValueError("고정 dev hash가 다릅니다")
        dev_rows = read_jsonl(dataset / "dev.jsonl")
    optimizer = torch.optim.AdamW(model.parameters(), lr=args.lr, weight_decay=0.01)
    tick, step, best_exact, best_epoch = time.monotonic(), 0, -1.0, 0
    full_epochs_completed = 0
    with (output / "loss.jsonl").open("w", encoding="utf-8", newline="\n") as log:
        for epoch in range(args.epochs):
            model.train()
            augmentation_stats = {}
            prefix_counts, prefix_completed_counts = {}, {}
            prefix_audit = {"epoch": epoch+1, "encoding_attempt_counts": prefix_counts,
                "optimizer_completed_counts": prefix_completed_counts, "pending_batch_rows": 0}
            metadata["neutral_prefix_counts"] = prefix_audit
            processed_rows = 0
            indices = list(range(len(rows))); random.shuffle(indices)
            for offset in range(0, len(indices), args.batch_size):
                runtime.check()
                selected = indices[offset:offset+args.batch_size]
                prefix_audit["pending_batch_rows"] = len(selected)
                training_rows = encode_selected_rows(rows, selected, tokenizer, config, byte_probability=args.byte_augmentation_probability, augmentation_rng=augmentation_rng, augmentation_stats=augmentation_stats, neutral_prefix_probability=metadata["neutral_prefix_augmentation"]["probability"], neutral_prefix_rng=neutral_prefix_rng, neutral_prefix_stats=prefix_counts)
                batch = collate(training_rows, args.device)
                optimizer.zero_grad(set_to_none=True)
                loss, components = loss_for_batch(model, batch)
                if not torch.isfinite(loss): raise ValueError("비유한 학습 손실")
                loss.backward()
                grad_norm = torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0, error_if_nonfinite=True)
                optimizer.step(); step += 1
                prefix_completed_counts.clear()
                prefix_completed_counts.update(prefix_counts)
                prefix_audit["pending_batch_rows"] = 0
                runtime.completed_step(step)
                processed_rows += len(selected)
                record = {"step": step, "epoch": epoch+1, "loss": float(loss.detach()), "token_loss": float(components[0]), "start_loss": float(components[1]), "end_loss": float(components[2]), "grad_norm": float(grad_norm), "elapsed_seconds": time.monotonic()-tick}
                memory = cuda_memory_record(args.device, step)
                if memory is not None:
                    record["cuda_memory"] = memory
                log.write(json.dumps(record)+"\n"); log.flush()
                if step == 1 or step % 50 == 0: print(json.dumps(record), flush=True)
                if step >= args.max_steps: break
            metadata.update(epoch_progress(epoch+1, processed_rows, len(indices), full_epochs_completed))
            full_epochs_completed = metadata["full_epochs_completed"]
            metadata["optimizer_steps_per_full_epoch"] = (len(indices)+args.batch_size-1)//args.batch_size
            prefix_audit["optimizer_step_end"] = step
            prefix_audit["epoch_complete"] = metadata["last_epoch_complete"]
            prefix_epochs.append(prefix_audit)
            (output / "neutral-prefix-epochs.json").write_text(json.dumps(prefix_epochs, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
            print(json.dumps({"epoch": epoch+1, "neutral_prefix": prefix_audit}, ensure_ascii=False), flush=True)
            if args.byte_augmentation_probability:
                augmentation_stats["epoch"] = epoch+1
                augmentation_stats["optimizer_step_end"] = step
                augmentation_stats["epoch_complete"] = metadata["last_epoch_complete"]
                augmentation_epochs.append(augmentation_stats)
                (output / "byte-augmentation-epochs.json").write_text(json.dumps(augmentation_epochs, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
                print(json.dumps({"epoch": epoch+1, "byte_augmentation": {key: value for key, value in augmentation_stats.items() if not key.endswith("_id_counts")}}, ensure_ascii=False), flush=True)
            if dev_rows:
                model.eval()
                # 고정된 개발 앞부분만 매 epoch 관측하며 heldout 시험 파일은 열지 않습니다.
                metrics = evaluate_rows(model, tokenizer, runtime.rows(dev_rows[:args.dev_samples]), output / f"dev-epoch{epoch+1:03d}.json")
                print(json.dumps({"epoch": epoch+1, "dev": metrics}), flush=True)
                if metrics["ast_exact"] > best_exact:
                    best_exact, best_epoch = metrics["ast_exact"], epoch+1
                    checkpoint(output / "best.pt", model, metadata, step)
                elif epoch+1-best_epoch >= args.patience:
                    metadata["early_stop"] = "dev AST exact 개선 중단"; break
            if step >= args.max_steps: break
    runtime.check()
    model.eval()
    final_hash = checkpoint(output / "final.pt", model, metadata, step)
    changes = []
    for name, tensor in model.state_dict().items():
        delta = tensor.detach().cpu().float()-initial[name].float()
        changes.append({"tensor": name, "numel": tensor.numel(), "changed": int(delta.ne(0).sum()), "l1": float(delta.abs().sum()), "max_abs": float(delta.abs().max())})
    metadata.update(optimizer_steps=step, epochs_completed=epoch+1, elapsed_seconds=time.monotonic()-tick, final_checkpoint_sha256=final_hash, best_dev_ast_exact=best_exact if dev_rows else None, best_epoch=best_epoch if dev_rows else None, tensors_changed=sum(c["changed"] > 0 for c in changes), changed_elements=sum(c["changed"] for c in changes), total_l1_change=sum(c["l1"] for c in changes), tensor_changes=changes, cuda_peak_allocated_bytes=torch.cuda.max_memory_allocated() if args.device == "cuda" else None, cuda_peak_reserved_bytes=torch.cuda.max_memory_reserved() if args.device == "cuda" else None)
    metadata["elapsed_seconds_all_inclusive_run"] = runtime.clock()-runtime.started
    if args.mode == "smoke":
        metadata["train_only_smoke_metrics"] = evaluate_rows(model, tokenizer, rows[:8], output / "train-smoke-predictions.json")
    (output / "result.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    print(json.dumps({key: value for key, value in metadata.items() if key not in ("source_hashes", "tensor_changes")}, ensure_ascii=False, indent=2))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--mode", choices=("init", "smoke", "train"), required=True)
    parser.add_argument("--dataset", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--approval")
    parser.add_argument("--initial-checkpoint")
    parser.add_argument("--device", choices=("cpu", "cuda"), default="cuda")
    parser.add_argument("--seed", type=int, default=1729)
    parser.add_argument("--batch-size", type=int, default=32)
    parser.add_argument("--epochs", type=int, default=24)
    parser.add_argument("--max-steps", type=int, default=4500)
    parser.add_argument("--lr", type=float, default=0.0005)
    parser.add_argument("--dev-samples", type=int, default=64)
    parser.add_argument("--patience", type=int, default=6)
    parser.add_argument("--byte-augmentation-probability", type=float, default=0.0)
    parser.add_argument("--byte-augmentation-seed", type=int, default=1729001)
    parser.add_argument("--neutral-prefix-probability", type=float, default=0.0)
    parser.add_argument("--neutral-prefix-seed", type=int, default=1730002)
    parser.add_argument("--checkpoint-every", type=int, default=0)
    parser.add_argument("--wall-seconds", type=int)
    parser.add_argument("--save-reserve-seconds", type=int, default=30)
    args = parser.parse_args()
    if args.batch_size < 1 or args.epochs < 1 or args.max_steps < 1 or not 1 <= args.dev_samples <= 1000:
        parser.error("학습 범위를 확인하세요")
    if not math.isfinite(args.byte_augmentation_probability) or not 0 <= args.byte_augmentation_probability <= 1:
        parser.error("byte 증강 확률은 0~1이어야 합니다")
    if not math.isfinite(args.neutral_prefix_probability) or not 0 <= args.neutral_prefix_probability <= 1:
        parser.error("중립 안내문 확률은 0~1이어야 합니다")
    run(args)


if __name__ == "__main__":
    main()
