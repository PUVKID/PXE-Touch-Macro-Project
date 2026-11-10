"""훈련 문자 어휘와 엄격 UTF-8 fallback을 사용하는 자체 토크나이저입니다."""
from collections import Counter
import json
import math
from pathlib import Path


class CodepointTokenizer:
    PAD, BOS, EOS, SEP = 0, 1, 2, 3
    BYTE_BASE, CHAR_BASE = 4, 260

    def __init__(self, characters):
        if len(set(characters)) != len(characters) or any(len(c) != 1 for c in characters):
            raise ValueError("문자 어휘 형식 오류")
        self.characters = list(characters)
        self.char_to_id = {c: i+self.CHAR_BASE for i, c in enumerate(characters)}

    @property
    def size(self):
        return self.CHAR_BASE + len(self.characters)

    @classmethod
    def fit(cls, train_sources, max_characters=1788):
        counts = Counter(c for source in train_sources for c in source)
        return cls(sorted(counts, key=lambda c: (-counts[c], ord(c)))[:max_characters])

    def encode(self, source):
        if not isinstance(source, str):
            raise ValueError("입력 문자열 필요")
        ids, offsets = [], []
        for index, char in enumerate(source):
            # 정규화하지 않으며 UTF-16 surrogate도 조용히 바꾸지 않습니다.
            encoded = [self.char_to_id[char]] if char in self.char_to_id else [self.BYTE_BASE+b for b in char.encode("utf-8", errors="strict")]
            ids.extend(encoded); offsets.extend([(index, index+1)] * len(encoded))
        return {"ids": ids, "offsets": offsets, "source": source}

    def decode(self, ids):
        output, pending = [], bytearray()
        def flush():
            if pending:
                output.append(bytes(pending).decode("utf-8", errors="strict")); pending.clear()
        for token in ids:
            if type(token) is not int:
                raise ValueError("정수 토큰 필요")
            if self.BYTE_BASE <= token < self.CHAR_BASE:
                pending.append(token-self.BYTE_BASE)
            elif self.CHAR_BASE <= token < self.size:
                flush(); output.append(self.characters[token-self.CHAR_BASE])
            else:
                raise ValueError("원문에 특수·알 수 없는 토큰이 있습니다")
        flush()
        return "".join(output)

    def encode_request(self, command, context):
        # 화면 OCR 등 비신뢰 자료는 입력에 포함하지 않습니다. 등록 이름만 직렬화합니다.
        inventory = {kind: sorted(context.get(kind, {})) for kind in ("points", "gestures", "regions", "macros", "apps", "actions") if isinstance(context.get(kind, {}), dict)}
        inventory["planning_mode"] = context.get("planning_mode")
        result = self.encode(command)
        command_length = len(result["ids"])
        suffix = self.encode(json.dumps(inventory, ensure_ascii=False, sort_keys=True, separators=(",", ":")))
        result["ids"] += [self.SEP] + suffix["ids"]
        result["offsets"] += [None] * (1+len(suffix["ids"]))
        result["command_tokens"] = command_length
        return result

    def encode_request_training(self, command, context, *, byte_probability, rng, max_input=1024, stats=None):
        """훈련에서만 알려진 문자 표현을 UTF-8 byte로 바꾸며 원문·어휘는 유지합니다."""
        if type(byte_probability) not in (int, float) or not math.isfinite(byte_probability) or not 0 <= byte_probability <= 1:
            raise ValueError("byte 증강 확률은 0~1의 유한한 수여야 합니다")
        if type(max_input) is not int or max_input < 1:
            raise ValueError("입력 상한은 양의 정수여야 합니다")
        if byte_probability and not callable(getattr(rng, "random", None)):
            raise ValueError("byte 증강에는 별도 난수 생성기가 필요합니다")
        base = self.encode_request(command, context)
        if len(base["ids"]) > max_input:
            raise ValueError("기본 입력부터 상한을 넘습니다: 원문을 자르지 않습니다")
        result = base
        sampled = converted = skipped = 0
        if byte_probability:
            remaining = max_input-len(base["ids"])
            ids, offsets, command_length = [], [], 0
            for index, (token, span) in enumerate(zip(base["ids"], base["offsets"])):
                replacement = [token]
                if token >= self.CHAR_BASE and rng.random() < byte_probability:
                    sampled += 1
                    byte_ids = [self.BYTE_BASE+byte for byte in self.characters[token-self.CHAR_BASE].encode("utf-8", errors="strict")]
                    extra = len(byte_ids)-1
                    # 문자 전체를 유지합니다. 예산 부족 시 기존 문자 표현을 그대로 둡니다.
                    if extra <= remaining:
                        replacement = byte_ids; remaining -= extra; converted += 1
                    else:
                        skipped += 1
                ids.extend(replacement); offsets.extend([span]*len(replacement))
                if index < base["command_tokens"]: command_length += len(replacement)
            result = {"ids": ids, "offsets": offsets, "source": command, "command_tokens": command_length}
        if stats is not None:
            counts = {"rows": 1, "source_tokens": len(result["ids"]), "command_tokens": result["command_tokens"], "sampled_codepoints": sampled, "converted_codepoints": converted, "budget_skipped_codepoints": skipped}
            for name, count in counts.items(): stats[name] = stats.get(name, 0)+count
            stats["max_source_tokens"] = max(stats.get("max_source_tokens", 0), len(result["ids"]))
            byte_counts = stats.setdefault("byte_id_counts", [0]*256)
            command_counts = stats.setdefault("command_byte_id_counts", [0]*256)
            for index, token in enumerate(result["ids"]):
                if self.BYTE_BASE <= token < self.CHAR_BASE:
                    byte_counts[token-self.BYTE_BASE] += 1
                    if index < result["command_tokens"]: command_counts[token-self.BYTE_BASE] += 1
        return result

    def span_to_tokens(self, encoded, start, end):
        offsets = encoded["offsets"]
        candidates = [i for i, span in enumerate(offsets) if span is not None and start <= span[0] and span[1] <= end]
        if not candidates or offsets[candidates[0]][0] != start or offsets[candidates[-1]][1] != end:
            raise ValueError("원문 구간이 토큰과 맞지 않습니다")
        return candidates[0], candidates[-1]

    def tokens_to_span(self, encoded, start, end):
        offsets = encoded["offsets"]
        if type(start) is not int or type(end) is not int or not 0 <= start <= end < len(offsets) or offsets[start] is None or offsets[end] is None:
            raise ValueError("복사 토큰 범위 오류")
        if start and offsets[start-1] == offsets[start] or end+1 < len(offsets) and offsets[end+1] == offsets[end]:
            raise ValueError("UTF-8 문자 중간 복사는 허용하지 않습니다")
        return offsets[start][0], offsets[end][1]

    def save(self, path):
        Path(path).write_text(json.dumps({"format": "pxe-codepoint-utf8-v1", "characters": self.characters, "normalization": "none", "offset_unit": "unicode-codepoint-half-open"}, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")

    @classmethod
    def load(cls, path):
        value = json.loads(Path(path).read_text(encoding="utf-8"))
        if value.get("format") != "pxe-codepoint-utf8-v1" or value.get("normalization") != "none":
            raise ValueError("지원하지 않는 토크나이저")
        return cls(value["characters"])
