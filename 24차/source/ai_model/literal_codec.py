"""인용 원문은 side table에 보존하고 NN에는 어휘 경계·동일값 관계만 전달합니다."""
from copy import deepcopy
import hashlib
import json
import math
from pathlib import Path
import re

from .tokenizer import CodepointTokenizer

FORMAT = "pxe-literal-slot-utf8-v1"
KINDS = ("points", "gestures", "regions", "macros", "apps", "actions")
SPECIAL_TOKENS = ("LIT", "ENTITY", "UNMENTIONED", "REC_END", "INVENTORY")


def load_context_json(text):
    """dictionary 생성 전에 중복 JSON 키를 거절합니다. 이미 잃은 중복은 복원하지 않습니다."""
    def unique(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError("문맥 JSON의 중복 키")
            result[key] = value
        return result
    value = json.loads(text, object_pairs_hook=unique)
    if type(value) is not dict:
        raise ValueError("문맥 객체 필요")
    return value


class LiteralSlotCodec:
    MAX_INPUT = 1024

    def __init__(self, characters, *, base_tokenizer_sha256):
        self.base = CodepointTokenizer(characters)
        if self.base.size != 535:
            raise ValueError("고정 v8 입력 어휘535가 필요합니다")
        if not isinstance(base_tokenizer_sha256, str) or not re.fullmatch("[0-9a-f]{64}", base_tokenizer_sha256):
            raise ValueError("기준 tokenizer SHA256 필요")
        self.base_tokenizer_sha256 = base_tokenizer_sha256
        self.special_ids = {name: self.base.size + index for index, name in enumerate(SPECIAL_TOKENS)}

    @property
    def size(self):
        return self.base.size + len(SPECIAL_TOKENS)

    @classmethod
    def from_base_file(cls, path):
        raw = Path(path).read_bytes()
        base = CodepointTokenizer.load(path)
        return cls(base.characters, base_tokenizer_sha256=hashlib.sha256(raw).hexdigest())

    @staticmethod
    def _quotes(command):
        spans, opening, escaped = [], None, False
        for index, char in enumerate(command):
            if opening is None and char in "“”«»「」『』":
                raise ValueError("ASCII 큰따옴표 외 인용 구분자는 지원하지 않습니다")
            if char == '"' and not escaped:
                if opening is None:
                    opening = index + 1
                else:
                    text = command[opening:index]
                    if not text.strip():
                        raise ValueError("빈 인용 또는 전부 공백인 인용")
                    spans.append((opening, index, text))
                    opening = None
            escaped = char == "\\" and not escaped
        if opening is not None:
            raise ValueError("닫히지 않은 인용")
        return spans

    @staticmethod
    def _inventory(context):
        if type(context) is not dict:
            raise ValueError("문맥 객체 필요")
        mode = context.get("planning_mode")
        if mode is not None and (type(mode) is not str or mode not in ("MACRO", "WATCH")):
            raise ValueError("지원하지 않는 계획 모드")
        names = {}
        for index, kind in enumerate(KINDS):
            registry = context.get(kind, {})
            if type(registry) is not dict:
                raise ValueError("등록 문맥은 객체여야 합니다: " + kind)
            for name in registry:
                if type(name) is not str or not name.strip():
                    raise ValueError("등록 이름 형식 오류")
                name.encode("utf-8", errors="strict")
                mask = names.setdefault(name, ["0"] * len(KINDS))
                mask[index] = "1"
        return {name: "".join(mask) for name, mask in names.items()}, mode

    @staticmethod
    def _masks(encoded):
        offsets, roles = encoded["offsets"], encoded["roles"]
        starts, ends = [], []
        for index, (span, role) in enumerate(zip(offsets, roles)):
            starts.append(role == "literal" or role == "raw" and (index == 0 or offsets[index - 1] != span))
            ends.append(role == "literal" or role == "raw" and (index + 1 == len(offsets) or offsets[index + 1] != span))
        encoded["start_keep"], encoded["end_keep"] = starts, ends

    def encode_request(self, command, context):
        if type(command) is not str or not command.strip():
            raise ValueError("명령 원문 필요")
        command.encode("utf-8", errors="strict")
        inventory, mode = self._inventory(context)
        if len(self.base.encode_request(command, context)["ids"]) > self.MAX_INPUT:
            raise ValueError("기존 입력1024 한도 초과: 원문을 자르지 않습니다")
        spans = self._quotes(command)
        values = list(dict.fromkeys(text for start, end, text in spans))
        entity_ids = {text: index for index, text in enumerate(values)}
        entities = [{"id": index, "membership": inventory.get(text, "000000")} for index, text in enumerate(values)]
        unused = {}
        for name, membership in inventory.items():
            if name not in entity_ids:
                unused[membership] = unused.get(membership, 0) + 1
        groups = [{"membership": mask, "count": unused[mask]} for mask in sorted(unused)]
        encoded = {"ids": [], "offsets": [], "roles": [], "regions": [], "source": command,
                   "literals": [], "entities": entities, "unmentioned_groups": groups, "planning_mode": mode,
                   "command_units": [], "original_input_tokens": len(self.base.encode_request(command, context)["ids"])}

        def add(ids, offsets, role, region=None):
            encoded["ids"].extend(ids)
            encoded["offsets"].extend(offsets)
            encoded["roles"].extend([role] * len(ids))
            encoded["regions"].extend([region] * len(ids))

        def metadata(ids):
            add(ids, [None] * len(ids), "metadata")

        def ascii_bytes(text):
            metadata([self.base.BYTE_BASE + byte for byte in text.encode("ascii", errors="strict")])

        def raw_piece(start, end, region):
            if start == end:
                return
            raw = self.base.encode(command[start:end])
            begin = len(encoded["ids"])
            add(raw["ids"], [(a + start, b + start) for a, b in raw["offsets"]], "raw", region)
            encoded["command_units"].append({"kind": "raw", "begin": begin, "end": len(encoded["ids"])})

        cursor = 0
        for occurrence, (start, end, text) in enumerate(spans):
            raw_piece(cursor, start - 1, occurrence)
            anchor = len(encoded["ids"])
            add([self.special_ids["LIT"]], [(start, end)], "literal", -occurrence - 1)
            ascii_bytes(str(entity_ids[text]))
            metadata([self.special_ids["REC_END"]])
            literal = {"occurrence": occurrence, "entity": entity_ids[text], "anchor": anchor,
                       "start": start, "end": end, "quote_start": start - 1, "quote_end": end + 1}
            encoded["literals"].append(literal)
            encoded["command_units"].append({"kind": "literal", "anchor": anchor,
                                              "quote_start": start - 1, "quote_end": end + 1})
            cursor = end + 1
        raw_piece(cursor, len(command), len(spans))
        encoded["command_tokens"] = len(encoded["ids"])
        metadata([self.base.SEP, self.special_ids["INVENTORY"]])
        for entity in entities:
            metadata([self.special_ids["ENTITY"]])
            ascii_bytes(str(entity["id"]) + ":" + entity["membership"])
            metadata([self.special_ids["REC_END"]])
        for group in groups:
            metadata([self.special_ids["UNMENTIONED"]])
            ascii_bytes(group["membership"] + ":" + str(group["count"]))
            metadata([self.special_ids["REC_END"]])
        mode_tokens = self.base.encode(json.dumps({"planning_mode": mode}, ensure_ascii=False, sort_keys=True, separators=(",", ":")))
        metadata(mode_tokens["ids"])
        if len(encoded["ids"]) > self.MAX_INPUT:
            raise ValueError("새 입력1024 한도 초과: 슬롯·문맥을 자르지 않습니다")
        self._masks(encoded)
        return encoded

    def pair_is_valid(self, encoded, start, end):
        if type(start) is not int or type(end) is not int or not 0 <= start <= end < len(encoded["ids"]):
            return False
        if not encoded["start_keep"][start] or not encoded["end_keep"][end]:
            return False
        if encoded["roles"][start] == "literal":
            return start == end
        region = encoded["regions"][start]
        return all(encoded["roles"][index] == "raw" and encoded["regions"][index] == region for index in range(start, end + 1))

    def end_mask(self, encoded, start):
        if type(start) is not int or not 0 <= start < len(encoded["ids"]) or not encoded["start_keep"][start]:
            raise ValueError("유효하지 않은 COPY 시작")
        return [self.pair_is_valid(encoded, start, end) for end in range(len(encoded["ids"]))]

    def tokens_to_span(self, encoded, start, end):
        if not self.pair_is_valid(encoded, start, end):
            raise ValueError("COPY 시작·끝·전체 구간 또는 UTF8 경계 오류")
        return encoded["offsets"][start][0], encoded["offsets"][end][1]

    def span_to_tokens(self, encoded, start, end):
        if type(start) is not int or type(end) is not int or not 0 <= start < end <= len(encoded["source"]):
            raise ValueError("원문 COPY 범위 오류")
        for literal in encoded["literals"]:
            if (start, end) == (literal["start"], literal["end"]):
                return literal["anchor"], literal["anchor"]
        candidates = [index for index, span in enumerate(encoded["offsets"])
                      if encoded["roles"][index] == "raw" and start <= span[0] and span[1] <= end]
        if candidates:
            first, last = candidates[0], candidates[-1]
            if self.tokens_to_span(encoded, first, last) == (start, end):
                return first, last
        raise ValueError("인용 일부·구분자·문맥 또는 불일치 COPY")

    def copy_text(self, encoded, start, end):
        a, b = self.tokens_to_span(encoded, start, end)
        return encoded["source"][a:b]

    def training_masks(self, encoded, spans):
        """target 경계는 전처리 후 loss용으로만 받으며 lexical 입력을 바꾸지 않습니다."""
        result = []
        for span in spans:
            start, end = self.span_to_tokens(encoded, *span)
            end_keep = self.end_mask(encoded, start)
            if not end_keep[end]:
                raise ValueError("학습 COPY target이 공통 추론 mask와 불일치")
            result.append({"start_keep": encoded["start_keep"][:], "end_keep": end_keep})
        return result

    def reconstruct_command(self, encoded):
        pieces = []
        for unit in encoded["command_units"]:
            if unit["kind"] == "raw":
                pieces.append(self.base.decode(encoded["ids"][unit["begin"]:unit["end"]]))
            else:
                pieces.append(encoded["source"][unit["quote_start"]:unit["quote_end"]])
        return "".join(pieces)

    def encode_request_training(self, command, context, *, byte_probability, rng, max_input=1024, stats=None):
        if type(byte_probability) not in (int, float) or not math.isfinite(byte_probability) or not 0 <= byte_probability <= 1:
            raise ValueError("byte 확률은0~1의 유한한 수여야 합니다")
        if type(max_input) is not int or not 1 <= max_input <= self.MAX_INPUT:
            raise ValueError("새 입력 상한은1~1024입니다")
        encoded = self.encode_request(command, context)
        if len(encoded["ids"]) > max_input:
            raise ValueError("새 기본 입력부터 지정 상한 초과")
        converted = skipped = sampled = 0
        if byte_probability:
            if not callable(getattr(rng, "random", None)):
                raise ValueError("분리된 byte RNG 필요")
            result = deepcopy(encoded)
            for key in ("ids", "offsets", "roles", "regions"):
                result[key] = []
            remaining = max_input - len(encoded["ids"])
            positions = []
            for index, token in enumerate(encoded["ids"]):
                positions.append(len(result["ids"]))
                replacement = [token]
                if self.base.CHAR_BASE <= token < self.base.size and rng.random() < byte_probability:
                    sampled += 1
                    expanded = [self.base.BYTE_BASE + byte for byte in self.base.characters[token - self.base.CHAR_BASE].encode("utf-8", errors="strict")]
                    extra = len(expanded) - 1
                    if extra <= remaining:
                        replacement = expanded
                        remaining -= extra
                        converted += 1
                    else:
                        skipped += 1
                result["ids"].extend(replacement)
                for key in ("offsets", "roles", "regions"):
                    result[key].extend([encoded[key][index]] * len(replacement))
            positions.append(len(result["ids"]))
            result["command_tokens"] = positions[encoded["command_tokens"]]
            for literal in result["literals"]:
                literal["anchor"] = positions[literal["anchor"]]
            for unit in result["command_units"]:
                if unit["kind"] == "raw":
                    unit["begin"], unit["end"] = positions[unit["begin"]], positions[unit["end"]]
                else:
                    unit["anchor"] = positions[unit["anchor"]]
            result["augmentation_stats"] = {"sampled_codepoints": sampled, "converted_codepoints": converted, "budget_skipped_codepoints": skipped}
            self._masks(result)
            encoded = result
        if stats is not None:
            for key, value in {"rows": 1, "source_tokens": len(encoded["ids"]), "command_tokens": encoded["command_tokens"],
                               "sampled_codepoints": sampled, "converted_codepoints": converted, "budget_skipped_codepoints": skipped}.items():
                stats[key] = stats.get(key, 0) + value
            stats["max_source_tokens"] = max(stats.get("max_source_tokens", 0), len(encoded["ids"]))
            all_bytes, command_bytes = stats.setdefault("byte_id_counts", [0] * 256), stats.setdefault("command_byte_id_counts", [0] * 256)
            for index, token in enumerate(encoded["ids"]):
                if self.base.BYTE_BASE <= token < self.base.CHAR_BASE:
                    all_bytes[token - self.base.BYTE_BASE] += 1
                    if index < encoded["command_tokens"]:
                        command_bytes[token - self.base.BYTE_BASE] += 1
        return encoded

    def save(self, path):
        value = {"format": FORMAT, "base_format": "pxe-codepoint-utf8-v1", "base_tokenizer_sha256": self.base_tokenizer_sha256,
                 "characters": self.base.characters, "special_tokens": list(SPECIAL_TOKENS), "special_ids": self.special_ids,
                 "normalization": "none", "offset_unit": "unicode-codepoint-half-open", "registry_kinds": list(KINDS),
                 "max_input": self.MAX_INPUT, "copy_contract": "literal-anchor-or-contiguous-unquoted-v1"}
        with Path(path).open("x", encoding="utf-8", newline="\n") as stream:
            json.dump(value, stream, ensure_ascii=False, indent=2)
            stream.write("\n")

    @classmethod
    def load(cls, path):
        value = json.loads(Path(path).read_text(encoding="utf-8"))
        if value.get("format") != FORMAT or value.get("base_format") != "pxe-codepoint-utf8-v1":
            raise ValueError("지원하지 않는 literal codec 형식")
        codec = cls(value["characters"], base_tokenizer_sha256=value["base_tokenizer_sha256"])
        required = {"format", "base_format", "base_tokenizer_sha256", "characters", "special_tokens", "special_ids", "normalization", "offset_unit", "registry_kinds", "max_input", "copy_contract"}
        if set(value) != required or value["special_tokens"] != list(SPECIAL_TOKENS) or value["special_ids"] != codec.special_ids or value["registry_kinds"] != list(KINDS) or value["normalization"] != "none" or value["offset_unit"] != "unicode-codepoint-half-open" or type(value["max_input"]) is not int or value["max_input"] != 1024 or value["copy_contract"] != "literal-anchor-or-contiguous-unquoted-v1":
            raise ValueError("literal codec 토큰·직렬화·한도 계약 불일치")
        return codec
