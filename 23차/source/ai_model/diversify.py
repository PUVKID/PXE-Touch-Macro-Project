"""기존 train 구조에 새 literal·등록명과 중립 선두를 붙이는 훈련 전용 합성입니다."""
import argparse
from collections import Counter
import copy
import hashlib
import json
from pathlib import Path
import random
import shutil
import unicodedata

from .data import _semantic_signature
from .rephrase import CoverageError, digest, read_rows
from .schema import parse_plan
from .tokenizer import CodepointTokenizer

REGISTERED_KINDS = ("points", "gestures", "regions", "macros", "apps", "actions")
TEXT_REFERENCE_FIELDS = {"text", "reject_text", "point_ref", "gesture_ref", "app_ref", "macro_ref", "region_ref", "action_ref", "region_refs"}
OLD_PREFIX = "다음대로 해 주세요. "
PREFIXES = ("", "요청: ", "실행 지시: ", "이번 자동화 지시입니다. ", "설명에 적힌 작업을 진행해 주세요. ", "지금부터 적는 동작의 설명을 전달합니다. ", "자동화할 작업 내용은 아래에 적었습니다. ", "이 메시지의 작업 설명을 읽고 정해진 동작과 조건을 따라 진행해 주세요. ", OLD_PREFIX)
KNOWN_CHARACTERS = "사과배수집정문확인우편지도입구설정창고열매교환"
ALPHABETS = {"known": KNOWN_CHARACTERS, "hangul": "".join(chr(value) for value in range(0xAC00, 0xAC60)), "ascii": "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-", "emoji": "🙂🌱🚀⭐🧩🐾🍀🎈", "mixed": KNOWN_CHARACTERS+"ABxy07"+"🙂🌱🚀", "spaced": KNOWN_CHARACTERS+"ABxy"}
SUFFIX_CUES = ("직선 경로", "곡선 경로", "축소 경로", "확대 경로", "다중 터치 경로", "녹화 경로", "검색 영역", "목록 영역", "진입 확인 영역")


def quoted_spans(command):
    result, opening, escaped = [], None, False
    for index, char in enumerate(command):
        if char == '"' and not escaped:
            if opening is None: opening = index+1
            else: result.append((opening,index,command[opening:index])); opening=None
        escaped = char == "\\" and not escaped
    if opening is not None: raise ValueError("닫히지 않은 인용문")
    return result


def rename_quoted(row, aliases, neutral_prefix=""):
    if row.get("split") != "train" or neutral_prefix not in PREFIXES: raise ValueError("훈련 split과 검토한 중립 선두만 허용합니다")
    spans = quoted_spans(row["command"])
    eligible = {text for start,end,text in spans if not any(char.isnumeric() for char in text)}
    if set(aliases) != eligible: raise ValueError("부분 문자열 치환·인용 누락·숫자 literal 치환 거절")
    if len(set(aliases.values())) != len(aliases): raise ValueError("새 이름 일대일 대응 충돌")
    if any(not isinstance(value,str) or not value.strip() or not 1 <= len(value) <= 24 or '"' in value or "\\" in value for value in aliases.values()): raise ValueError("새 이름 형식·길이 오류")
    result = copy.deepcopy(row)
    prefix_end = len(OLD_PREFIX) if row["command"].startswith(OLD_PREFIX) else 0
    edits = [(0,prefix_end,neutral_prefix)] + [(start,end,aliases[text]) for start,end,text in spans if text in aliases]
    edits.sort()
    if any(left[1] > right[0] for left,right in zip(edits,edits[1:])): raise ValueError("선두와 인용 편집 교차")
    def shifted(position, start_boundary=False):
        return position+sum(len(value)-(end-start) for start,end,value in edits if end < position or end == position and (end != start or start_boundary))
    pieces, cursor = [], 0
    for start,end,value in edits: pieces += [row["command"][cursor:start],value]; cursor=end
    pieces.append(row["command"][cursor:]); result["command"]="".join(pieces)
    for before,after in zip(row["tokens"],result["tokens"]):
        if not isinstance(before,dict): continue
        contained = [(start,end,value) for start,end,value in edits if start <= before["start"] and before["end"] <= end]
        if contained:
            start,end,value=contained[0]
            if (start,end)!=(before["start"],before["end"]): raise ValueError("복합 인용 내부 부분 COPY는 합성하지 않습니다")
            after["start"]=shifted(start,True); after["end"]=after["start"]+len(value)
        else:
            if any(start < before["end"] and before["start"] < end for start,end,value in edits): raise ValueError("수치 COPY 교차")
            after["start"],after["end"]=shifted(before["start"],True),shifted(before["end"])
        old=row["command"][before["start"]:before["end"]]
        if result["command"][after["start"]:after["end"]] != aliases.get(old,old): raise ValueError("새 COPY 원문 불일치")
    def rename_gold(value,key=None):
        if isinstance(value,list): return [rename_gold(item,key) for item in value]
        if isinstance(value,dict): return {name:rename_gold(item,name) for name,item in value.items()}
        return aliases.get(value,value) if isinstance(value,str) and key in TEXT_REFERENCE_FIELDS else value
    result["plan"]=rename_gold(row["plan"])
    for kind in REGISTERED_KINDS:
        if isinstance(row["context"].get(kind),dict):
            before=row["context"][kind]
            result["context"][kind]={aliases.get(name,name):copy.deepcopy(payload) for name,payload in before.items()}
            if len(result["context"][kind])!=len(before): raise ValueError("기존 등록키와 새 이름 충돌")
            for name,payload in before.items():
                if result["context"][kind][aliases.get(name,name)]!=payload: raise ValueError("등록 스냅샷 내용 변경")
    if parse_plan(result["tokens"],result["command"],result["context"])!=result["plan"]: raise ValueError("새 합성 gold 정합 오류")
    if _semantic_signature(result["plan"])!=_semantic_signature(row["plan"]): raise ValueError("문자열 슬롯 이외 구조·수치·정책 변경")
    result["diversification"]={"parent_id":row["id"],"aliases":aliases,"neutral_prefix":neutral_prefix,"original_prefix_removed":prefix_end,"literal_targets_are_new_synthetic":bool(aliases),"meaning_identity_with_parent_claimed":False}
    if "augmentation" in result:
        result["parent_rephrase_augmentation"]=result.pop("augmentation")
        result["parent_rephrase_augmentation"]["edit_offsets_apply_to_parent_command_only"]=True
    return result


def sample_aliases(row,rng,mode=None,length=None,forbidden=(),reserved_parts=(),length_cap=24):
    if mode is not None and mode not in ALPHABETS or length is not None and (type(length) is not int or not 1<=length<=24): raise ValueError("합성 이름 모드·길이 오류")
    forbidden=set(forbidden); aliases={}
    for old in sorted({text for start,end,text in quoted_spans(row["command"]) if not any(char.isnumeric() for char in text)}):
        for attempt in range(2000):
            selected=mode or rng.choice(tuple(ALPHABETS)); size=length if length is not None else rng.randint(1,length_cap)
            chars=[rng.choice(ALPHABETS[selected]) for _ in range(size)]
            if selected=="spaced" and size>=3: chars[rng.randrange(1,size-1)]=" "
            if selected=="mixed" and size>=3: chars[0]=rng.choice(KNOWN_CHARACTERS); chars[1]=rng.choice("ABxy07"); chars[-1]=rng.choice("🙂🌱🚀")
            value="".join(chars)
            if value!=old and value not in forbidden and value not in aliases.values() and not any(part in value for part in reserved_parts): aliases[old]=value; break
        else: raise CoverageError("충돌 없는 새 이름 부족: "+row["id"])
    return aliases


def freeze_dataset(original,output,seed=1731):
    original,output=Path(original),Path(output)
    if output.exists(): raise ValueError("기존 합성 자료를 덮어쓰지 않습니다")
    prior=json.loads((original/"manifest.json").read_text(encoding="utf-8"))
    if digest(original/"train.jsonl")!=prior["splits"]["train"]["sha256"]: raise ValueError("원 train 해시 불일치")
    rows=read_rows(original/"train.jsonl")
    if len(rows)!=60000: raise ValueError("고정 v7 train60000행 필요")
    tokenizer=CodepointTokenizer.load(original/"tokenizer.json")
    forbidden={text for row in rows for start,end,text in quoted_spans(row["command"])}
    reserved=[name for split in ("dev","test") for name in prior["contract_before_generation"][split]["names"]]
    output.mkdir(); changed=[]; prefix_counts=Counter(); length_counts=Counter(); retries=Counter(); suffix_before=Counter(); suffix_after=Counter(); alias_unique=set(); known_aliases=0
    for index,row in enumerate(rows):
        rng=random.Random(int.from_bytes(hashlib.sha256((str(seed)+":"+row["id"]).encode()).digest(),"big"))
        for attempt in range(24):
            cap=24 if attempt<8 else 8 if attempt<16 else 2
            aliases=sample_aliases(row,rng,forbidden=forbidden,reserved_parts=reserved,length_cap=cap)
            prefix=rng.choice(PREFIXES+("", "")) if attempt<16 else ""
            candidate=rename_quoted(row,aliases,prefix)
            if len(tokenizer.encode_request(candidate["command"],candidate["context"])["ids"])<=1024: break
        else:
            (output/"eligibility-failure.json").write_text(json.dumps({"parent_id":row["id"],"attempts":24,"reason":"원문을 자르지 않고 상한 내 합성 후보를 만들지 못함"},ensure_ascii=False)+"\n",encoding="utf-8")
            raise CoverageError("상한 내 새 합성 후보 부족")
        candidate["id"]=f"train-v8-{index:06d}"
        candidate["diversification"]["candidate_attempts"]=attempt+1
        candidate["diversification"]["alias_length_cap"]=cap
        changed.append(candidate); retries[attempt+1]+=1; prefix_counts[prefix]+=1
        for old,value in aliases.items():
            length_counts[len(value)]+=1; alias_unique.add(value)
            known_aliases+=all(char in tokenizer.char_to_id for char in value)
        for start,end,value in quoted_spans(row["command"]): suffix_before.update(cue for cue in SUFFIX_CUES if cue in value)
        for start,end,value in quoted_spans(candidate["command"]): suffix_after.update(cue for cue in SUFFIX_CUES if cue in value)
    base_stats,aug_stats={},{}; rng=random.Random(1731001); copies=0; normal_unique=set(); surface_unique=set(); character_kinds=Counter()
    for row in changed:
        if parse_plan(row["tokens"],row["command"],row["context"])!=row["plan"]: raise ValueError("전수 새 gold 검사 실패")
        base=tokenizer.encode_request_training(row["command"],row["context"],byte_probability=0,rng=None,stats=base_stats)
        encoded=tokenizer.encode_request_training(row["command"],row["context"],byte_probability=0.3,rng=rng,stats=aug_stats)
        if tokenizer.decode(encoded["ids"][:encoded["command_tokens"]])!=row["command"]: raise ValueError("원문 복원 오류")
        for token in row["tokens"]:
            if isinstance(token,dict):
                start,end=tokenizer.span_to_tokens(encoded,token["start"],token["end"])
                if tokenizer.tokens_to_span(encoded,start,end)!=(token["start"],token["end"]): raise ValueError("COPY 왕복 오류")
                copies+=1
        for value in row["diversification"]["aliases"].values():
            character_kinds.update(characters=len(value),ascii=sum(ord(char)<128 for char in value),spaces=value.count(" "),hangul=sum(0xAC00<=ord(char)<=0xD7A3 for char in value),astral=sum(ord(char)>0xFFFF for char in value))
        surface_unique.add(hashlib.sha256(row["command"].encode()).hexdigest()); normal_unique.add(hashlib.sha256(" ".join(unicodedata.normalize("NFC",row["command"]).split()).encode()).hexdigest())
    raw="".join(json.dumps(row,ensure_ascii=False,sort_keys=True)+"\n" for row in changed).encode("utf-8")
    (output/"train.jsonl").write_bytes(raw); shutil.copyfile(original/"tokenizer.json",output/"tokenizer.json")
    manifest=copy.deepcopy(prior)
    manifest.update(format="pxe-split-v8-synthetic-alias-prefix",origin="v7 train60000의 구조·수치에 새 literal/등록명·중립선두를 합성한 새 정답 자료",seed=seed,parent_manifest_sha256=digest(original/"manifest.json"),parent_train_sha256=prior["splits"]["train"]["sha256"],tokenizer_policy="v7 train유래535 고정·새문자는기존UTF8bytefallback. v8 어휘 재fit 없음",tokenizer_sha256=digest(output/"tokenizer.json"),diversify_module_sha256=digest(__file__))
    manifest["parent_contract_before_generation"]=manifest.pop("contract_before_generation")
    manifest["contract_before_generation"]={"names":"train전용모드별새합성길이1~24, 원v7인용문구·사전holdoutname부분문자열제외", "prefixes":list(PREFIXES),"source_semantic_holdout":"원train의문자열슬롯정규화구조집합유지; literal과등록명이바뀐새합성이므로원정답의의미동일성을주장하지않음"}
    manifest.pop("train_audit",None)
    manifest["splits"]["train"]={"rows":len(changed),"bytes":len(raw),"sha256":hashlib.sha256(raw).hexdigest(),"kind_counts":dict(Counter(row["kind"] for row in changed)),"unique_commands":len(surface_unique),"unique_normalized_commands":len(normal_unique),"gold_parse_checked":len(changed),"gold_parse_errors":0,"normalized_semantic_hash_set_equal_to_parent":{row["semantic_hash"] for row in changed}=={row["semantic_hash"] for row in rows}}
    for split in ("dev","test"):
        shutil.copyfile(original/(split+".jsonl"),output/(split+".jsonl"))
        if digest(output/(split+".jsonl"))!=prior["splits"][split]["sha256"]: raise ValueError("원 heldout 바이트 불일치")
    manifest["io_scope"]={"dev_test":"원바이트복사·해시읽기만; 사전선언name도메인은부모manifest에서제외용으로읽음","dev_test_sentence_parsing":0,"dev_test_model_evaluations":0,"independent24_access":0}
    manifest["diversification_audit"]={"rows":len(changed),"copies_roundtripped":copies,"new_literal_alias_occurrences":sum(length_counts.values()),"unique_aliases":len(alias_unique),"known_character_only_alias_occurrences":known_aliases,"alias_length_counts":dict(length_counts),"alias_character_kinds":dict(character_kinds),"prefix_counts":dict(prefix_counts),"prefix_cp_lengths":{prefix:len(prefix) for prefix in PREFIXES},"candidate_attempt_histogram":dict(retries),"ineligible_parent_rows":0,"source_truncations":0,"suffix_cue_counts_before":dict(suffix_before),"suffix_cue_counts_after":dict(suffix_after),"alias_exact_overlap_with_parent_quoted_pool":len(alias_unique&forbidden),"alias_containing_reserved_holdout_name":sum(any(name in alias for name in reserved) for alias in alias_unique),"base_byte_exposure":base_stats,"augmented_byte_exposure_probability_0_3_seed1731001":aug_stats,"errors":0}
    manifest["unchanged_training_limitations"]="원v7 본문의4개변형과edit site포함방식을그대로상속. 별도body부분혼합수리는이번에하지않음. 새모델일반화성능미측정."
    (output/"manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    return manifest


def main():
    parser=argparse.ArgumentParser(); parser.add_argument("--original",required=True); parser.add_argument("--output",required=True); parser.add_argument("--seed",type=int,default=1731)
    args=parser.parse_args(); value=freeze_dataset(args.original,args.output,args.seed)
    print(json.dumps({"format":value["format"],"train":value["splits"]["train"],"tokenizer_policy":value["tokenizer_policy"],"audit":{key:item for key,item in value["diversification_audit"].items() if "exposure" not in key}},ensure_ascii=False))

if __name__=="__main__": main()
