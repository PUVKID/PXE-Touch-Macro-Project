"""codec 의미 fingerprint와 모델·체크포인트·부모 승인 결합을 검사합니다."""
import hashlib
import json
from pathlib import Path
from .literal_codec import FORMAT, KINDS, LiteralSlotCodec
from .schema import TOKENS
from .tokenizer import CodepointTokenizer


def input_contract(codec):
    if not isinstance(codec, (CodepointTokenizer, LiteralSlotCodec)):
        raise ValueError("지원하지 않는 입력 codec")
    literal = isinstance(codec, LiteralSlotCodec)
    base = codec.base if literal else codec
    characters_hash = hashlib.sha256(json.dumps(base.characters, ensure_ascii=False, separators=(",", ":")).encode("utf-8")).hexdigest()
    return {"format": FORMAT if literal else "pxe-codepoint-utf8-v1", "input_vocab": codec.size,
            "characters_sha256": characters_hash, "base_tokenizer_sha256": codec.base_tokenizer_sha256 if literal else None,
            "special_ids": dict(codec.special_ids) if literal else {}, "registry_kinds": list(KINDS) if literal else [],
            "normalization": "none", "offset_unit": "unicode-codepoint-half-open", "max_input": 1024,
            "copy_contract": "literal-anchor-or-contiguous-unquoted-v1" if literal else "legacy-codepoint-span-v1",
            "output_vocab": len(TOKENS), "output_tokens_sha256": hashlib.sha256(json.dumps(list(TOKENS), separators=(",", ":")).encode("utf-8")).hexdigest()}


def load_codec(path):
    value = json.loads(Path(path).read_text(encoding="utf-8"))
    if value.get("format") == FORMAT:
        return LiteralSlotCodec.load(path)
    if value.get("format") == "pxe-codepoint-utf8-v1":
        return CodepointTokenizer.load(path)
    raise ValueError("지원하지 않는 codec format")


def validate_model_codec(model, codec):
    expected = input_contract(codec)
    if model.config.input_vocab != codec.size or model.config.output_vocab != len(TOKENS):
        raise ValueError("모델 차원과 codec/출력72가 다릅니다")
    current = getattr(model, "input_contract", None)
    if isinstance(codec, LiteralSlotCodec) and current != expected or current is not None and current != expected:
        raise ValueError("모델과 입력 codec fingerprint가 다릅니다")


def bind_model_codec(model, codec):
    if model.config.input_vocab != codec.size or model.config.output_vocab != len(TOKENS):
        raise ValueError("모델과 codec 차원 불일치")
    model.input_contract = input_contract(codec)
    return model


def validate_literal_checkpoint(payload, codec, expected_tokenizer_sha256=None):
    if not isinstance(codec, LiteralSlotCodec):
        raise ValueError("v2 체크포인트에는 실제 literal codec이 필요합니다")
    expected = input_contract(codec)
    metadata, config = payload.get("metadata", {}), payload.get("config", {})
    if payload.get("input_contract") != expected or metadata.get("input_contract") != expected:
        raise ValueError("체크포인트의 codec fingerprint 불일치")
    if config.get("input_vocab") != 540 or config.get("output_vocab") != 72 or config.get("max_input") != 1024 or config.get("max_output") != 512:
        raise ValueError("체크포인트의 codec/config 한도 불일치")
    if type(payload.get("step")) is not int or payload["step"] < 0:
        raise ValueError("체크포인트 step 형식 오류")
    if expected_tokenizer_sha256 is not None and metadata.get("tokenizer_sha256") != expected_tokenizer_sha256:
        raise ValueError("실제 tokenizer 파일 hash 불일치")


def validate_training_approval(approval, manifest_hash, hashes, initial_hash):
    required = {"training_allowed", "dataset_manifest_sha256", "source_hashes", "initial_checkpoint_sha256"}
    if type(approval) is not dict or set(approval) != required or approval["training_allowed"] is not True:
        raise ValueError("부모 훈련 승인 exact4필드가 필요합니다")
    if approval["dataset_manifest_sha256"] != manifest_hash or approval["source_hashes"] != hashes or approval["initial_checkpoint_sha256"] != initial_hash:
        raise ValueError("훈련 승인과 고정 자료·소스·초기파일 결합 불일치")
