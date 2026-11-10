"""실제 greedy 생성·전체 AST 일치·오수락과 coverage를 측정합니다."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import time
import torch
from .model import ModelConfig, PlanTransformer
from .schema import PlanError, TOKENS, TOKEN_TO_ID, parse_plan
from .tokenizer import CodepointTokenizer
from .literal_codec import LiteralSlotCodec
from .literal_metrics import inspect_prediction
from .codec_contract import load_codec, validate_literal_checkpoint, validate_model_codec


def sha256(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def load_model(checkpoint_path, device="cpu", *, tokenizer=None, expected_tokenizer_sha256=None):
    checkpoint = torch.load(checkpoint_path, map_location=device, weights_only=True)
    if checkpoint.get("format") not in ("pxe-plan-transformer-v1", "pxe-plan-transformer-v2") or checkpoint.get("output_tokens") != list(TOKENS):
        raise ValueError("체크포인트 형식 또는 출력 토큰 순서가 현재 스키마와 다릅니다")
    if checkpoint.get("config", {}).get("output_vocab") != len(TOKENS):
        raise ValueError("체크포인트 출력 차원과 고정 어휘가 다릅니다")
    if checkpoint["format"] == "pxe-plan-transformer-v2":
        validate_literal_checkpoint(checkpoint, tokenizer, expected_tokenizer_sha256)
    elif isinstance(tokenizer, LiteralSlotCodec) or checkpoint.get("config", {}).get("input_vocab") == 540:
        raise ValueError("기존 v1과 literal540 형식을 혼합할 수 없습니다")
    model = PlanTransformer(ModelConfig(**checkpoint["config"]))
    model.load_state_dict(checkpoint["state_dict"], strict=True)
    if checkpoint["format"] == "pxe-plan-transformer-v2":
        model.input_contract = checkpoint["input_contract"]
    if tokenizer is not None:
        validate_model_codec(model, tokenizer)
    return model.to(device).eval(), checkpoint


def _predict_literal(model, tokenizer, command, context, max_output):
    validate_model_codec(model, tokenizer)
    limit = max_output if max_output is not None else model.config.max_output - 1
    if type(limit) is not int or not 1 <= limit < model.config.max_output:
        return {"plan": None, "tokens": [], "model_tokens": [], "copy_pairs": [], "error": "INPUT_OR_OUTPUT_LIMIT", "compiler_invalid": True, "nn_explicit_reject": False, "min_probability": 0.0}
    try:
        encoded = tokenizer.encode_request(command, context)
    except (ValueError, UnicodeError) as exc:
        return {"plan": None, "tokens": [], "model_tokens": [], "copy_pairs": [], "error": str(exc), "compiler_invalid": True, "nn_explicit_reject": False, "min_probability": 0.0}
    device = next(model.parameters()).device
    source = torch.tensor([encoded["ids"]], dtype=torch.long, device=device)
    source_keep = source.ne(0)
    copy_keep = torch.tensor([[a or b for a, b in zip(encoded["start_keep"], encoded["end_keep"])]], dtype=torch.bool, device=device)
    memory = model.encode(source, source_keep)
    prefix = torch.tensor([[TOKEN_TO_ID["BOS"]]], dtype=torch.long, device=device)
    words, pairs, probabilities = [], [], []
    for _ in range(limit):
        logits, starts, ends = model.decode(memory, prefix, source_keep, copy_keep)
        probabilities_now = logits[0, -1].softmax(-1)
        identifier = int(probabilities_now.argmax())
        probabilities.append(float(probabilities_now[identifier]))
        word = TOKENS[identifier]
        words.append(word)
        if word == "COPY":
            start_keep = torch.tensor(encoded["start_keep"], dtype=torch.bool, device=device)
            if not bool(start_keep.any()):
                result = inspect_prediction(tokenizer, encoded, context, words, pairs)
                result.update(error="NO_COPY_CANDIDATE", model_tokens=words, copy_pairs=pairs, min_probability=0.0)
                return result
            start_scores = starts[0, -1].masked_fill(~start_keep, -1e30)
            start = int(start_scores.argmax())
            # 추론에서는 실제 예측 start로 공통 전체 구간 커널의 끝 mask를 구합니다.
            end_keep = torch.tensor(tokenizer.end_mask(encoded, start), dtype=torch.bool, device=device)
            end_scores = ends[0, -1].masked_fill(~end_keep, -1e30)
            end = int(end_scores.argmax())
            tokenizer.tokens_to_span(encoded, start, end)
            pairs.append((start, end))
            probabilities += [float(start_scores.softmax(-1)[start]), float(end_scores.softmax(-1)[end])]
        prefix = torch.cat((prefix, torch.tensor([[identifier]], device=device)), dim=1)
        if word == "EOS":
            result = inspect_prediction(tokenizer, encoded, context, words, pairs)
            result.update(model_tokens=words, copy_pairs=pairs, min_probability=min(probabilities))
            return result
    result = inspect_prediction(tokenizer, encoded, context, words, pairs)
    result.update(error="TRUNCATED_OUTPUT", model_tokens=words, copy_pairs=pairs, min_probability=min(probabilities, default=0.0))
    return result


@torch.no_grad()
def predict(model, tokenizer, command, context, max_output=None):
    if isinstance(tokenizer, LiteralSlotCodec):
        return _predict_literal(model, tokenizer, command, context, max_output)
    validate_model_codec(model, tokenizer)
    encoded = tokenizer.encode_request(command, context)
    limit = max_output or model.config.max_output-1
    if not command.strip() or len(encoded["ids"]) > model.config.max_input or limit >= model.config.max_output:
        return {"plan": None, "tokens": [], "error": "INPUT_OR_OUTPUT_LIMIT", "min_probability": 0.0}
    device = next(model.parameters()).device
    source = torch.tensor([encoded["ids"]], dtype=torch.long, device=device)
    source_keep = source.ne(0)
    copy_keep = torch.zeros_like(source_keep); copy_keep[:, :encoded["command_tokens"]] = True
    memory = model.encode(source, source_keep)
    prefix = torch.tensor([[TOKEN_TO_ID["BOS"]]], dtype=torch.long, device=device)
    tokens, probabilities = [], []
    for _ in range(limit):
        logits, starts, ends = model.decode(memory, prefix, source_keep, copy_keep)
        probs = logits[0, -1].softmax(-1)
        identifier = int(probs.argmax())
        probabilities.append(float(probs[identifier]))
        word = TOKENS[identifier]
        if word == "COPY":
            start_scores, end_scores = starts[0, -1].clone(), ends[0, -1].clone()
            for index, offset in enumerate(encoded["offsets"]):
                if offset is None:
                    continue
                if index and encoded["offsets"][index-1] == offset:
                    start_scores[index] = -1e30
                if index+1 < len(encoded["offsets"]) and encoded["offsets"][index+1] == offset:
                    end_scores[index] = -1e30
            start = int(start_scores.argmax())
            end_scores[:start] = -1e30
            end = int(end_scores.argmax())
            probabilities += [float(start_scores.softmax(-1)[start]), float(end_scores.softmax(-1)[end])]
            begin_cp, end_cp = tokenizer.tokens_to_span(encoded, start, end)
            tokens.append({"token": "COPY", "start": begin_cp, "end": end_cp})
        else:
            tokens.append(word)
        prefix = torch.cat((prefix, torch.tensor([[identifier]], device=device)), dim=1)
        if word == "EOS":
            try:
                plan = parse_plan(tokens, command, context)
                return {"plan": plan, "tokens": tokens, "error": None, "min_probability": min(probabilities)}
            except (PlanError, ValueError) as exc:
                return {"plan": None, "tokens": tokens, "error": str(exc), "min_probability": min(probabilities)}
    return {"plan": None, "tokens": tokens, "error": "TRUNCATED_OUTPUT", "min_probability": min(probabilities, default=0.0)}


def evaluate_rows(model, tokenizer, rows, output_path=None):
    counts = Counter()
    examples = []
    tick = time.monotonic()
    for row in rows:
        before = time.monotonic()
        result = predict(model, tokenizer, row["command"], row["context"])
        if isinstance(tokenizer, LiteralSlotCodec):
            encoded = tokenizer.encode_request(row["command"], row["context"])
            signals = inspect_prediction(tokenizer, encoded, row["context"], result["model_tokens"], result["copy_pairs"], gold_tokens=row["tokens"], gold_plan=row["plan"])
            result["signal_metrics"] = {key: signals[key] for key in ("structure_exact", "literal_anchor_exact", "copy_pair_exact", "raw_copy_exact", "ast_exact", "nn_explicit_reject", "compiler_invalid", "wrong_ready")}
            for key, value in result["signal_metrics"].items():
                if value is not None:
                    counts[key + "_rows_total"] += 1
                    counts[key + "_rows_true"] += int(value)
        gold, plan = row["plan"], result["plan"]
        accepted = bool(plan and plan["status"] == "ready")
        exact = plan == gold
        gold_reject = gold["status"] == "reject"
        explicit_reject = bool(plan and plan["status"] == "reject")
        counts.update(total=1, exact=int(exact), accepted=int(accepted), wrong_accepted=int(accepted and not exact), gold_reject=int(gold_reject), rejected_gold_reject=int(gold_reject and not accepted), explicit_reject_gold_reject=int(gold_reject and explicit_reject), invalid=int(plan is None))
        examples.append({"id": row["id"], "exact": exact, "accepted": accepted, "gold": gold, "prediction": result, "elapsed_seconds": time.monotonic()-before})
    total = max(counts["total"], 1)
    metrics = dict(counts, ast_exact=counts["exact"]/total, coverage=counts["accepted"]/total, wrong_accepted_per_all=counts["wrong_accepted"]/total, wrong_accepted_per_accepted=counts["wrong_accepted"]/max(counts["accepted"], 1), reject_recall=counts["rejected_gold_reject"]/max(counts["gold_reject"], 1), explicit_reject_recall=counts["explicit_reject_gold_reject"]/max(counts["gold_reject"], 1), elapsed_seconds=time.monotonic()-tick)
    if output_path:
        Path(output_path).write_text(json.dumps({"metrics": metrics, "examples": examples}, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    return metrics


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--checkpoint", required=True)
    parser.add_argument("--tokenizer", required=True)
    parser.add_argument("--data", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--device", choices=("cpu", "cuda"), default="cpu")
    parser.add_argument("--approval")
    args = parser.parse_args()
    if Path(args.data).name.startswith("test") and not args.approval:
        parser.error("heldout 시험은 부모의 독립 평가 승인 파일이 필요합니다")
    if args.approval:
        approval = json.loads(Path(args.approval).read_text(encoding="utf-8"))
        if approval.get("evaluation_data_sha256") != sha256(args.data) or approval.get("evaluation_allowed") is not True:
            parser.error("평가 승인과 실제 자료 hash가 일치하지 않습니다")
    torch.set_num_threads(4)
    tokenizer = load_codec(args.tokenizer)
    model, checkpoint = load_model(args.checkpoint, args.device, tokenizer=tokenizer, expected_tokenizer_sha256=sha256(args.tokenizer))
    if checkpoint.get("metadata", {}).get("tokenizer_sha256") != sha256(args.tokenizer):
        raise ValueError("체크포인트와 실제 토크나이저 hash가 다릅니다")
    # JSONL은 물리적 LF로만 나눕니다. 문자열 내부 U+0085는 자르지 않습니다.
    text = Path(args.data).read_text(encoding="utf-8")
    parts = text.split("\n")
    if parts[-1] == "": parts.pop()
    rows = [json.loads(line) for line in parts]
    print(json.dumps(evaluate_rows(model, tokenizer, rows, args.output), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
