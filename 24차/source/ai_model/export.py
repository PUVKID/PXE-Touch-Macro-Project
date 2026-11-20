"""고정한 PyTorch 모델을 encoder와 decoder-step ONNX로 내보냅니다."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import numpy as np
import onnx
import onnxruntime as ort
import torch
from torch import nn
from .evaluate import load_model, predict, sha256
from .codec_contract import input_contract, load_codec
from .literal_codec import LiteralSlotCodec
from .literal_metrics import inspect_prediction
from .schema import TOKENS


class EncoderExport(nn.Module):
    def __init__(self, model):
        super().__init__(); self.model = model

    def forward(self, source, source_keep):
        return self.model.encode(source, source_keep)


class DecoderExport(nn.Module):
    def __init__(self, model):
        super().__init__(); self.model = model

    def forward(self, memory, target, source_keep, copy_keep):
        return self.model.decode(memory, target, source_keep, copy_keep)


def sessions(folder):
    options = ort.SessionOptions()
    options.intra_op_num_threads = 4; options.inter_op_num_threads = 1
    return tuple(ort.InferenceSession(str(Path(folder)/name), sess_options=options, providers=["CPUExecutionProvider"]) for name in ("encoder.onnx", "decoder.onnx"))


def load_export_model(checkpoint_path, tokenizer_path):
    codec = load_codec(tokenizer_path)
    digest = sha256(tokenizer_path)
    model, checkpoint = load_model(checkpoint_path, "cpu", tokenizer=codec, expected_tokenizer_sha256=digest)
    if checkpoint.get("metadata", {}).get("tokenizer_sha256") != digest:
        raise ValueError("export의 실제 tokenizer 파일과 checkpoint hash 불일치")
    return model, checkpoint, codec


def export_models(model, output):
    output = Path(output); output.mkdir(parents=True, exist_ok=True)
    if any((output/name).exists() for name in ("encoder.onnx", "decoder.onnx")):
        raise ValueError("기존 ONNX 파일은 덮어쓰지 않습니다")
    model = model.cpu().eval()
    source = torch.tensor([[4, 5, 6, 7]], dtype=torch.long)
    target = torch.tensor([[1, 3]], dtype=torch.long)
    keep = torch.ones_like(source, dtype=torch.bool)
    with torch.no_grad(): memory = model.encode(source, keep)
    # 고정 PyTorch2.9의 지원되는 legacy exporter를 명시해 ONNXScript 의존성을 추가하지 않습니다.
    torch.onnx.export(EncoderExport(model), (source, keep), str(output/"encoder.onnx"), input_names=["source", "source_keep"], output_names=["memory"], dynamic_axes={"source": {0: "batch", 1: "source_length"}, "source_keep": {0: "batch", 1: "source_length"}, "memory": {0: "batch", 1: "source_length"}}, opset_version=17, dynamo=False)
    torch.onnx.export(DecoderExport(model), (memory, target, keep, keep), str(output/"decoder.onnx"), input_names=["memory", "target", "source_keep", "copy_keep"], output_names=["logits", "start_logits", "end_logits"], dynamic_axes={"memory": {0: "batch", 1: "source_length"}, "target": {0: "batch", 1: "target_length"}, "source_keep": {0: "batch", 1: "source_length"}, "copy_keep": {0: "batch", 1: "source_length"}, "logits": {0: "batch", 1: "target_length"}, "start_logits": {0: "batch", 1: "target_length", 2: "source_length"}, "end_logits": {0: "batch", 1: "target_length", 2: "source_length"}}, opset_version=17, dynamo=False)
    for name in ("encoder.onnx", "decoder.onnx"):
        onnx.checker.check_model(onnx.load(output/name), full_check=True)


def check_parity(model, folder, shapes, atol=0.0001):
    encoder, decoder = sessions(folder)
    maximum, same_argmax, close = 0.0, True, True
    details = []
    model = model.cpu().eval()
    for batch, slen, tlen in shapes:
        source = torch.arange(batch*slen).reshape(batch, slen) % (model.config.input_vocab-4)+4
        target = torch.arange(batch*tlen).reshape(batch, tlen) % (model.config.output_vocab-1)+1
        keep = torch.ones_like(source, dtype=torch.bool); keep[:, -1] = False
        copy_keep = keep.clone()
        with torch.no_grad():
            memory = model.encode(source, keep)
            wanted = (memory,) + model.decode(memory, target, keep, copy_keep)
        actual_memory = encoder.run(None, {"source": source.numpy(), "source_keep": keep.numpy()})[0]
        outputs = (actual_memory,) + tuple(decoder.run(None, {"memory": actual_memory, "target": target.numpy(), "source_keep": keep.numpy(), "copy_keep": copy_keep.numpy()}))
        case_max, case_close, case_argmax = 0.0, True, True
        for expected, actual in zip(wanted, outputs):
            expected = expected.numpy()
            error = float(np.max(np.abs(expected-actual)))
            case_max = max(case_max, error)
            case_close = case_close and bool(np.allclose(expected, actual, atol=atol, rtol=0.0001))
            case_argmax = case_argmax and bool(np.array_equal(expected.argmax(-1), actual.argmax(-1)))
        maximum = max(maximum, case_max); close = close and case_close; same_argmax = same_argmax and case_argmax
        details.append({"batch": batch, "source_length": slen, "target_length": tlen, "max_absolute_error": case_max, "allclose": case_close, "argmax_equal": case_argmax})
    return {"cases": len(details), "allclose": close, "argmax_equal": same_argmax, "max_absolute_error": maximum, "atol": atol, "rtol": 0.0001, "details": details, "android_checked": False}


@torch.no_grad()
def predict_ort(model, codec, command, context, encoder, decoder, atol=0.0001):
    """같은 입력과 공통 COPY 커널로 ORT 생성하며 Torch 결정도 별도로 대조합니다."""
    if not isinstance(codec, LiteralSlotCodec):
        raise ValueError("이 host COPY 정합 경로는 literal540 전용입니다")
    encoded = codec.encode_request(command, context)
    source = np.asarray([encoded["ids"]], dtype=np.int64)
    keep = source != 0
    copy_keep = np.asarray([[a or b for a, b in zip(encoded["start_keep"], encoded["end_keep"])]], dtype=np.bool_)
    memory = encoder.run(None, {"source": source, "source_keep": keep})[0]
    torch_memory = model.encode(torch.from_numpy(source), torch.from_numpy(keep))
    trace = {"allclose": bool(np.allclose(torch_memory.numpy(), memory, atol=atol, rtol=0.0001)),
        "argmax_equal": True, "max_absolute_error": float(np.max(np.abs(torch_memory.numpy()-memory))), "decoder_steps": 0}
    prefix = np.asarray([[TOKENS.index("BOS")]], dtype=np.int64)
    words, pairs = [], []
    for _ in range(model.config.max_output-1):
        actual = decoder.run(None, {"memory": memory, "target": prefix, "source_keep": keep, "copy_keep": copy_keep})
        expected = model.decode(torch_memory, torch.from_numpy(prefix), torch.from_numpy(keep), torch.from_numpy(copy_keep))
        for wanted, found in zip(expected, actual):
            wanted = wanted.numpy()
            trace["max_absolute_error"] = max(trace["max_absolute_error"], float(np.max(np.abs(wanted-found))))
            trace["allclose"] = trace["allclose"] and bool(np.allclose(wanted, found, atol=atol, rtol=0.0001))
        trace["decoder_steps"] += 1
        identifier = int(actual[0][0, -1].argmax())
        trace["argmax_equal"] = trace["argmax_equal"] and identifier == int(expected[0][0, -1].argmax())
        word = TOKENS[identifier]
        words.append(word)
        if word == "COPY":
            start_keep = np.asarray(encoded["start_keep"], dtype=np.bool_)
            if not bool(start_keep.any()):
                break
            start = int(np.where(start_keep, actual[1][0, -1], np.float32(-1e30)).argmax())
            wanted_start = int(np.where(start_keep, expected[1][0, -1].numpy(), np.float32(-1e30)).argmax())
            # teacher gold를 사용하지 않고 실제 ORT 예측 start의 공통 전체구간 mask입니다.
            end_keep = np.asarray(codec.end_mask(encoded, start), dtype=np.bool_)
            end = int(np.where(end_keep, actual[2][0, -1], np.float32(-1e30)).argmax())
            wanted_end = int(np.where(end_keep, expected[2][0, -1].numpy(), np.float32(-1e30)).argmax())
            trace["argmax_equal"] = trace["argmax_equal"] and start == wanted_start and end == wanted_end
            codec.tokens_to_span(encoded, start, end)
            pairs.append((start, end))
        prefix = np.concatenate((prefix, np.asarray([[identifier]], dtype=np.int64)), axis=1)
        if word == "EOS":
            break
    result = inspect_prediction(codec, encoded, context, words, pairs)
    result.update(model_tokens=words, copy_pairs=pairs)
    return result, trace


def check_command_parity(model, codec, folder, rows, atol=0.0001):
    encoder, decoder = sessions(folder)
    details = []
    for row in rows:
        wanted = predict(model, codec, row["command"], row["context"])
        actual, trace = predict_ort(model, codec, row["command"], row["context"], encoder, decoder, atol)
        details.append({"id": row["id"], **trace, "plan_equal": wanted["plan"] == actual["plan"],
            "structure_equal": wanted["model_tokens"] == actual["model_tokens"], "copy_pairs_equal": wanted["copy_pairs"] == actual["copy_pairs"],
            "original_cp_tokens_equal": wanted["tokens"] == actual["tokens"],
            "torch_ast_exact_vs_supplied_gold": wanted["plan"] == row["plan"], "ort_ast_exact_vs_supplied_gold": actual["plan"] == row["plan"]})
    keys = ("allclose", "argmax_equal", "plan_equal", "structure_equal", "copy_pairs_equal", "original_cp_tokens_equal")
    return {"cases": len(details), **{key: all(case[key] for case in details) for key in keys},
        "max_absolute_error": max((case["max_absolute_error"] for case in details), default=0.0),
        "torch_ast_exact_count": sum(case["torch_ast_exact_vs_supplied_gold"] for case in details),
        "ort_ast_exact_count": sum(case["ort_ast_exact_vs_supplied_gold"] for case in details),
        "atol": atol, "rtol": 0.0001, "host_common_pair_mask": True, "normalization": "none", "details": details}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--checkpoint", required=True)
    parser.add_argument("--tokenizer", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--parity-data", action="append", default=[])
    parser.add_argument("--integration-only", action="store_true")
    args = parser.parse_args()
    torch.set_num_threads(4)
    torch.set_num_interop_threads(1)
    model, checkpoint, codec = load_export_model(args.checkpoint, args.tokenizer)
    export_models(model, args.output)
    report = check_parity(model, args.output, [(1, 8, 3), (1, 48, 12), (2, 77, 19)])
    folder = Path(args.output)
    if (folder / "tokenizer.json").exists():
        raise ValueError("기존 tokenizer 출력은 덮어쓰지 않습니다")
    shutil.copyfile(args.tokenizer, folder / "tokenizer.json")
    if sha256(folder / "tokenizer.json") != sha256(args.tokenizer):
        raise ValueError("export tokenizer 바이트 복사 불일치")
    report.update(checkpoint_sha256=sha256(args.checkpoint), config=model.config.to_dict(), output_tokens=list(TOKENS), format="FP32", opset=17, torch=str(torch.__version__), onnx=onnx.__version__, onnxruntime=ort.__version__,
        input_contract=input_contract(codec), tokenizer_sha256=sha256(folder / "tokenizer.json"),
        integration_only=args.integration_only, production_approved=False,
        checkpoint_source_hashes=checkpoint.get("metadata", {}).get("source_hashes"),
        files={name: {"bytes": (folder/name).stat().st_size, "sha256": sha256(folder/name)} for name in ("encoder.onnx", "decoder.onnx", "tokenizer.json")})
    if args.parity_data:
        rows = []
        for path in args.parity_data:
            if Path(path).name.startswith("test"):
                raise ValueError("이 export 정합 명령에는 시험 자료를 사용할 수 없습니다")
            parts = Path(path).read_bytes().split(b"\n")
            if parts[-1] != b"":
                raise ValueError("정합 JSONL의 마지막 LF가 필요합니다")
            rows.extend(json.loads(part.decode("utf-8", errors="strict")) for part in parts[:-1])
        report["command_parity"] = check_command_parity(model, codec, folder, rows)
    (folder/"export-report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    command_check = report.get("command_parity")
    if not report["allclose"] or not report["argmax_equal"] or command_check is not None and not all(command_check[key] for key in ("allclose", "argmax_equal", "plan_equal", "structure_equal", "copy_pairs_equal", "original_cp_tokens_equal")):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
