"""고정 train에만 쓰는 보호 구간 기반 표현 편집기입니다. 추론에는 사용하지 않습니다."""
import argparse
from collections import Counter
import copy
from dataclasses import asdict, dataclass
import hashlib
import json
from pathlib import Path
import random
import shutil
import unicodedata

from .schema import parse_plan
from .tokenizer import CodepointTokenizer


class CoverageError(ValueError):
    """원문을 보존하며 필요한 본문 변형 수를 만들 수 없을 때 보고합니다."""


@dataclass(frozen=True)
class Edit:
    start: int
    end: int
    replacement: str
    rule: str


def rules():
    value = json.loads(Path(__file__).with_name("rephrase_rules.json").read_text(encoding="utf-8"))
    result = value["rules"]
    if len({item["id"] for item in result}) != len(result): raise ValueError("중복 어구 ID")
    for item in result:
        if not item["source"] or len(set(item["variants"])) < 4 or item["source"] in item["variants"] or not item["meaning"]:
            raise ValueError("검토 가능한 서로 다른 대체 어구4개와 의미 설명이 필요합니다")
    return result


def protected_intervals(row):
    command = row["command"]
    intervals = []
    for token in row["tokens"]:
        if isinstance(token, dict):
            start, end = token["start"], token["end"]
            if type(start) is not int or type(end) is not int or not 0 <= start < end <= len(command): raise ValueError("COPY 위치 형식 오류")
            intervals.append((start, end))
    opening, escaped = None, False
    for index, char in enumerate(command):
        if char == '"' and not escaped:
            if opening is None: opening = index
            else: intervals.append((opening, index+1)); opening = None
        escaped = char == "\\" and not escaped
    if opening is not None: raise ValueError("닫히지 않은 인용문의 표현은 편집하지 않습니다")
    merged = []
    for start, end in sorted(intervals):
        if merged and start <= merged[-1][1]: merged[-1] = (merged[-1][0], max(end, merged[-1][1]))
        else: merged.append((start, end))
    return merged


def intersects(edit, interval):
    start, end = interval
    return start < edit.start < end if edit.start == edit.end else edit.start < end and start < edit.end


def apply_edits(row, edits):
    command = row["command"]
    protected = protected_intervals(row)
    ordered = sorted(edits, key=lambda edit: (edit.start, edit.end))
    previous = None
    for edit in ordered:
        if type(edit.start) is not int or type(edit.end) is not int or not 0 <= edit.start <= edit.end <= len(command) or not isinstance(edit.replacement, str):
            raise ValueError("편집 구간 형식 오류")
        edit.replacement.encode("utf-8", errors="strict")
        if any(intersects(edit, interval) for interval in protected): raise ValueError("COPY·인용 literal과 교차한 편집 거절")
        if previous is not None and (edit.start < previous.end or edit.start == previous.start): raise ValueError("겹치거나 같은 위치의 편집 거절")
        previous = edit
    parts, cursor = [], 0
    for edit in ordered:
        parts += [command[cursor:edit.start], edit.replacement]; cursor = edit.end
    parts.append(command[cursor:])
    result = copy.deepcopy(row)
    result["command"] = "".join(parts)
    def moved(position, is_start):
        shift = 0
        for edit in ordered:
            if edit.start == edit.end:
                if edit.start < position or edit.start == position and is_start: shift += len(edit.replacement)
            elif edit.end <= position: shift += len(edit.replacement)-(edit.end-edit.start)
        return position+shift
    for original, changed in zip(row["tokens"], result["tokens"]):
        if isinstance(original, dict):
            changed["start"], changed["end"] = moved(original["start"], True), moved(original["end"], False)
            if result["command"][changed["start"]:changed["end"]] != command[original["start"]:original["end"]]: raise ValueError("편집 후 COPY 원문 불일치")
    if parse_plan(result["tokens"], result["command"], result["context"]) != row["plan"]: raise ValueError("편집 후 원 gold 변경")
    return result


def edit_sites(row):
    command, protected = row["command"], protected_intervals(row)
    possible = []
    for rule in rules():
        offset = 0
        while (start := command.find(rule["source"], offset)) >= 0:
            end = start+len(rule["source"]); offset = end
            if not any(intersects(Edit(start, end, "", rule["id"]), interval) for interval in protected): possible.append((start, end, rule))
    chosen, cursor = [], 0
    for start, end, rule in sorted(possible, key=lambda item: (item[0], -(item[1]-item[0]), item[2]["id"])):
        if start >= cursor: chosen.append((start, end, rule)); cursor = end
    return chosen


def make_variants(row, seed=1729, tokenizer=None, max_input=1024):
    if row.get("split") != "train": raise ValueError("훈련 split에만 표현 편집을 적용합니다")
    if parse_plan(row["tokens"], row["command"], row["context"]) != row["plan"]: raise ValueError("원 gold가 다릅니다")
    sites = edit_sites(row)
    if not sites: raise CoverageError("검토된 본문 편집 어구 없음: "+row["id"])
    rng = random.Random(int.from_bytes(hashlib.sha256((str(seed)+":"+row["id"]).encode()).digest(), "big"))
    permutations = []
    for site in sites:
        indices = list(range(len(site[2]["variants"]))); rng.shuffle(indices); permutations.append(indices)
    original = copy.deepcopy(row)
    original["augmentation"] = {"origin_id": row["id"], "variant": 0, "body_edits": 0, "edits": []}
    original["id"] = row["id"]+"-v0"
    result, seen = [original], {row["command"]}
    # 전체 본문 변형이 길이를 넘으면 적은 수의 검토 어구를 바꾼 별도 후보를 선택합니다.
    selections = [list(range(len(sites)))] + [[index] for index in range(len(sites))]
    for selection in selections:
        for variant in range(4):
            edits = [Edit(sites[index][0], sites[index][1], sites[index][2]["variants"][permutations[index][variant]], sites[index][2]["id"]) for index in selection]
            changed = apply_edits(row, edits)
            if changed["command"] in seen: continue
            if tokenizer is not None and len(tokenizer.encode_request(changed["command"], changed["context"])["ids"]) > max_input: continue
            seen.add(changed["command"])
            changed["id"] = row["id"]+"-v"+str(len(result))
            changed["augmentation"] = {"origin_id": row["id"], "variant": len(result), "body_edits": len(edits), "edits": [asdict(edit) for edit in edits]}
            result.append(changed)
            if len(result) == 5: return result
    raise CoverageError("상한 내 서로 다른 본문5변형 부족: "+row["id"])


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def read_rows(path):
    parts = Path(path).read_text(encoding="utf-8").split("\n")
    if parts[-1] == "": parts.pop()
    return [json.loads(part) for part in parts]


def freeze_rephrased_dataset(original, output, seed=1730):
    original, output = Path(original), Path(output)
    if output.exists(): raise ValueError("기존 자료는 덮어쓰지 않습니다")
    prior = json.loads((original / "manifest.json").read_text(encoding="utf-8"))
    if digest(original / "train.jsonl") != prior["splits"]["train"]["sha256"]: raise ValueError("원 train 해시 불일치")
    rows = read_rows(original / "train.jsonl")
    if len(rows) != 12000: raise ValueError("고정 원 train12000개 필요")
    # 길이 선택용 어휘만 미리 계산하며 실제 저장 어휘는 완성된 train60000행에서 다시 fit합니다.
    authored = [text for rule in rules() for text in [rule["source"]]+rule["variants"]]
    provisional = CodepointTokenizer.fit([row["command"] for row in rows]+authored)
    output.mkdir()
    transformed, failures = [], []
    for row in rows:
        try: transformed.extend(make_variants(row, seed=seed, tokenizer=provisional))
        except CoverageError as error: failures.append({"id": row["id"], "reason": str(error)})
    if failures:
        (output / "coverage-failure.json").write_text(json.dumps(failures, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
        raise CoverageError("본문5변형 부족 원문 "+str(len(failures))+"개; 원문을 삭제하지 않습니다")
    tokenizer = CodepointTokenizer.fit([row["command"] for row in transformed])
    rule_counts, replacement_counts = Counter(), Counter()
    stats, base_stats = {}, {}
    rng = random.Random(1730001)
    copies, max_length, unique, normalized = 0, 0, set(), set()
    original_by_id = {row["id"]: row for row in rows}
    for row in transformed:
        before = original_by_id[row["augmentation"]["origin_id"]]
        if row["plan"] != before["plan"] or row["context"] != before["context"] or row["semantic_hash"] != before["semantic_hash"]: raise ValueError("원 gold·문맥·의미 집합 변경")
        if parse_plan(row["tokens"], row["command"], row["context"]) != before["plan"]: raise ValueError("전수 gold 검사 실패")
        base = tokenizer.encode_request_training(row["command"], row["context"], byte_probability=0, rng=None, stats=base_stats)
        encoded = tokenizer.encode_request_training(row["command"], row["context"], byte_probability=0.3, rng=rng, stats=stats)
        if tokenizer.decode(encoded["ids"][:encoded["command_tokens"]]) != row["command"]: raise ValueError("원문 복원 불일치")
        for token in row["tokens"]:
            if isinstance(token, dict):
                start, end = tokenizer.span_to_tokens(encoded, token["start"], token["end"])
                if tokenizer.tokens_to_span(encoded, start, end) != (token["start"], token["end"]): raise ValueError("COPY 왕복 불일치")
                copies += 1
        for edit in row["augmentation"]["edits"]:
            rule_counts[edit["rule"]] += 1; replacement_counts[edit["rule"]+":"+edit["replacement"]] += 1
        max_length = max(max_length, len(base["ids"]))
        unique.add(hashlib.sha256(row["command"].encode()).hexdigest())
        normalized.add(hashlib.sha256(" ".join(unicodedata.normalize("NFC", row["command"]).split()).encode()).hexdigest())
    raw = "".join(json.dumps(row, ensure_ascii=False, sort_keys=True)+"\n" for row in transformed).encode("utf-8")
    (output / "train.jsonl").write_bytes(raw)
    tokenizer.save(output / "tokenizer.json")
    shutil.copyfile(Path(__file__).with_name("rephrase_rules.json"), output / "rephrase_rules.json")
    manifest = copy.deepcopy(prior)
    manifest.update(format="pxe-split-v7-rephrase", origin="고정v5 train12000 의미 집합을 유지한 본문 표현 편집 각5개(원문 포함)", seed=seed, parent_manifest_sha256=digest(original / "manifest.json"), parent_train_sha256=prior["splits"]["train"]["sha256"], tokenizer_train_sources_only=True, tokenizer_size=tokenizer.size, rephrase_rules_sha256=digest(output / "rephrase_rules.json"), rephrase_module_sha256=digest(__file__))
    manifest.pop("overlaps", None)
    manifest["surface_overlap_status"] = "원 dev/test 바이트는 그대로 보존. 새 표면 중복의 독립 검사 전이며 구현자가 test문장을 파싱하지 않음"
    manifest["splits"]["train"] = {"rows": len(transformed), "bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest(), "kind_counts": dict(Counter(row["kind"] for row in transformed)), "unique_commands": len(unique), "unique_normalized_commands": len(normalized), "gold_parse_checked": len(transformed), "gold_parse_errors": 0, "original_rows": len(rows), "variants_per_original_including_original": 5, "semantic_hash_set_equal_to_original": {row["semantic_hash"] for row in transformed} == {row["semantic_hash"] for row in rows}}
    for split in ("dev", "test"):
        shutil.copyfile(original / (split+".jsonl"), output / (split+".jsonl"))
        if digest(output / (split+".jsonl")) != prior["splits"][split]["sha256"]: raise ValueError("보존 split의 바이트 해시 불일치")
    manifest["io_scope"] = {"dev_test": "원바이트 복사·해시 읽기만", "dev_test_sentence_parsing": 0, "dev_test_model_evaluations": 0, "independent24_access": 0}
    manifest["train_audit"] = {"original_rows": len(rows), "rows": len(transformed), "copies_roundtripped": copies, "body_edited_rows": sum(row["augmentation"]["body_edits"] > 0 for row in transformed), "prefix_only_variants": 0, "phrase_rule_counts": dict(rule_counts), "phrase_replacement_counts": dict(replacement_counts), "rules_with_observed_edits": len(rule_counts), "distinct_replacements_observed": len(replacement_counts), "base_max_source_tokens": max_length, "base_byte_exposure": base_stats, "augmented_byte_exposure_probability_0_3_seed1730001": stats, "source_truncations": 0, "errors": 0}
    (output / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    return manifest


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--original", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--seed", type=int, default=1730)
    args = parser.parse_args()
    value = freeze_rephrased_dataset(args.original, args.output, args.seed)
    print(json.dumps({"format": value["format"], "rows": value["splits"]["train"]["rows"], "train_sha256": value["splits"]["train"]["sha256"], "tokenizer_size": value["tokenizer_size"], "audit": {key: count for key, count in value["train_audit"].items() if "exposure" not in key and "counts" not in key}, "io_scope": value["io_scope"]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
