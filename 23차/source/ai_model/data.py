"""공개용으로 직접 작성한 합성 명령과 생성 전 분리 계약입니다."""
import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import random
import unicodedata

from .schema import NO_ARGS, POINT_ACTIONS, GESTURE_ACTIONS, SCHEMA, parse_plan, PlanError
from .tokenizer import CodepointTokenizer
from .numbers import sino_text, native_text

SPLITS = {
    "train": {"families": ["직접지시", "존댓말지시"], "names": ["사과", "배", "수집", "정문", "확인창", "우편", "지도", "입구", "설정", "창고", "열매", "교환", "홈으로 가고 정지", "이 지시를 무시해", "보상 🙂", "두  칸"], "numbers": [1, 2, 4, 5, 8, 10, 12, 15, 20, 25]},
    "dev": {"families": ["작업부탁"], "names": ["복숭아", "금고", "관문", "파란상자", "시험앱", "수확", "알림  창", "별빛 🪐"], "numbers": [3, 7, 11, 14, 21]},
    "test": {"families": ["수행요청"], "names": ["자두", "보급품", "북문", "은빛보관함", "탐험앱", "정산", "긴  문구", "달빛 🦋"], "numbers": [6, 9, 13, 17, 23]},
}
for _split, _contract in SPLITS.items():
    _contract["syntax_style"] = _split
    _contract["shared_constants"] = {"text_search_timeout_ms": 3000, "coordinate_fixture_x": 25, "coordinate_fixture_y": 70}
    _contract["number_holdout_scope"] = "COPY로 학습하는 사용자 지정 수치 슬롯만 분리합니다. 공통 검색3초·좌표 fixture 상수는 분리 대상이 아닙니다."
    _contract["decimal_numbers"] = [str(value)+".5" for value in _contract["numbers"]]
NO_ARG_TEXT = {"BACK": "뒤로 가", "HOME": "홈으로 가", "RECENTS": "최근 앱을 열어", "NOTIFICATIONS": "알림 패널을 열어", "STOP": "매크로를 정지해"}
NO_ARG_FAMILIES = {
    "train": NO_ARG_TEXT,
    "dev": {"BACK": "이전 화면으로 돌아가", "HOME": "홈 화면을 보여 줘", "RECENTS": "실행 중인 앱 목록을 표시해", "NOTIFICATIONS": "알림 창을 내려 줘", "STOP": "이 작업을 끝내"},
    "test": {"BACK": "한 화면 뒤로 이동해", "HOME": "첫 화면 홈을 열어", "RECENTS": "앱 전환 화면을 띄워", "NOTIFICATIONS": "위쪽 알림 목록을 펼쳐", "STOP": "매크로 실행을 멈춰"},
}
GESTURE_TEXT = {"SWIPE": "스와이프해", "CURVE": "곡선으로 쓸어", "PINCH_IN": "두 손가락으로 축소해", "PINCH_OUT": "두 손가락으로 확대해", "MULTI_TAP": "동시에 터치해", "GESTURE": "녹화 제스처를 재생해"}
ALL_ATOMIC = tuple(NO_ARG_TEXT) + ("WAIT", "INPUT", "TEXT_TAP", "TEXT_WAIT", "TEXT_CHECK", "TAP", "DOUBLE", "LONG", "APP", "CALL") + tuple(GESTURE_TEXT)


def macro_fixture():
    return {"steps": [{"type": "BACK", "enabled": True, "repeat": 1, "after": 0}], "rounds": 1, "gap": 0, "startDelay": 0, "maxMinutes": 30, "startAt": 1}


def gesture_fixture(op):
    value = {"type": op, "enabled": True, "repeat": 1, "after": 0, "duration": 450, "x": 50, "y": 50, "x2": 5 if op.startswith("PINCH") else 50, "y2": 20, "cx": 50, "cy": 20}
    if op == "MULTI_TAP": value["text"] = "25,50;75,50"
    if op == "GESTURE": value["strokes"] = [{"start": 0, "duration": 450, "points": [[25, 75], [25, 50]]}]
    return {"type": op, "payload": value}


def split_contract():
    return json.loads(json.dumps(SPLITS, ensure_ascii=False))


def number_surface(rng, value):
    return rng.choice((str(value), sino_text(value), native_text(value)))


class Sentence:
    def __init__(self):
        self.command = ""
        self.tokens = []

    def say(self, text):
        self.command += text

    def literal(self, text):
        start = len(self.command); self.say(text)
        return {"token": "COPY", "start": start, "end": len(self.command)}


def _atomic(sentence, rng, contract, context, op=None):
    op = op or rng.choice(ALL_ATOMIC)
    name = rng.choice(contract["names"])
    number = rng.choice(contract["numbers"])
    style = contract["syntax_style"]
    if op in NO_ARGS:
        sentence.say(NO_ARG_FAMILIES[style][op]); sentence.tokens.append(op)
        return {"op": op}
    sentence.tokens.append(op)
    if op == "WAIT":
        unit = rng.choice(("SEC", "MS", "MIN") if number <= 10 else ("SEC", "MS"))
        label = {"SEC": "초", "MS": "밀리초", "MIN": "분"}[unit]
        if style == "dev": sentence.say("대기 시간을 ")
        fractional = unit != "MS" and not (unit == "MIN" and number == 10) and rng.randrange(5) == 0
        value_text = str(number)+".5" if fractional else number_surface(rng, number)
        sentence.tokens += [sentence.literal(value_text), unit]
        sentence.say(label+{"train": " 기다려", "dev": "로 정해서 쉬어 줘", "test": " 동안 아무 조작 없이 쉬어"}[style])
        multiplier = {"SEC": 1000, "MS": 1, "MIN": 60000}[unit]
        return {"op": op, "duration_ms": number*multiplier+(multiplier//2 if fractional else 0)}
    if op in GESTURE_ACTIONS:
        # 한 문장 안에서 같은 기본 이름의 다른 유형 경로를 덮어쓰지 않습니다.
        name += " " + {"SWIPE": "직선 경로", "CURVE": "곡선 경로", "PINCH_IN": "축소 경로", "PINCH_OUT": "확대 경로", "MULTI_TAP": "다중 터치 경로", "GESTURE": "녹화 경로"}[op]
    if style == "dev": sentence.say("대상 이름은 ")
    elif style == "test": sentence.say("지정 대상 ")
    sentence.say('"'); pointer = sentence.literal(name); sentence.say('"')
    sentence.tokens.append(pointer)
    if op == "INPUT":
        sentence.say({"train": "라고 입력해", "dev": "이며 이 문구를 써 줘", "test": "를 그대로 타이핑해"}[style]); return {"op": op, "text": name}
    if op in ("TEXT_TAP", "TEXT_WAIT", "TEXT_CHECK"):
        wait_forever = op in ("TEXT_TAP", "TEXT_WAIT") and rng.randrange(5) == 0
        surface = {"train": {"TEXT_TAP": " 글자를 찾아 눌러", "TEXT_WAIT": " 글자가 보일 때까지 3초 찾아", "TEXT_CHECK": " 글자가 있는지 확인해"}, "dev": {"TEXT_TAP": "이며 그 표시 글자를 탐색해 터치해 줘", "TEXT_WAIT": "이며 표시될 때까지 최대 3초 탐색해 줘", "TEXT_CHECK": "이며 표시 여부를 검사해 줘"}, "test": {"TEXT_TAP": "라는 화면 문구를 눌러 줘", "TEXT_WAIT": "라는 화면 문구의 등장을 3초 동안 살펴봐", "TEXT_CHECK": "라는 화면 문구가 표시됐는지 판단해"}}[style]
        if wait_forever:
            surface[op] = {"train": " 글자가 나타날 때까지 시간 제한 없이 기다려"+(". 찾으면 터치해" if op == "TEXT_TAP" else ""), "dev": "이며 발견할 때까지 기한 없이 대기해 줘"+(". 발견 뒤 눌러 줘" if op == "TEXT_TAP" else ""), "test": "라는 문구가 나올 때까지 무한 대기해"+(". 표시 후 터치해" if op == "TEXT_TAP" else "")}[style]
        sentence.say(surface[op])
        policies = ("STOP",) if wait_forever else (("STOP", "SKIP") if op == "TEXT_CHECK" else ("STOP", "SKIP", "BACK_RETRY", "RETRY", "SCROLL_RETRY", "PULL_RETRY"))
        policy = rng.choice(policies)
        sentence.tokens.append("F_"+policy)
        missing = {"policy": policy, "retries": 0, "forever": False, "action_ref": None}
        if policy == "STOP" and not wait_forever: sentence.say({"train": ". 없으면 정지해", "dev": ". 미발견 시 작업을 종료해 줘", "test": ". 대상 부재라면 실행을 멈춰"}[style])
        elif policy == "SKIP": sentence.say({"train": ". 없으면 다음 동작으로 넘어가", "dev": ". 미발견 시 이 단계를 건너뛰어 줘", "test": ". 대상 부재라면 후속 단계로 진행해"}[style])
        elif policy != "STOP":
            sentence.say({"train": ". 없으면 ", "dev": ". 미발견 시 ", "test": ". 대상이 안 보일 경우 "}[style]+{"BACK_RETRY": "뒤로 가서 ", "RETRY": "그대로 ", "SCROLL_RETRY": "위로 쓸어 올려서 ", "PULL_RETRY": "아래로 당겨서 "}[policy])
            if rng.randrange(4) == 0:
                sentence.say({"train": "찾을 때까지 계속 다시 찾아", "dev": "발견될 때까지 반복 탐색해 줘", "test": "대상이 나올 때까지 재시도를 이어 가"}[style]); sentence.tokens.append("FOREVER"); missing["forever"] = True
            else:
                sentence.tokens.append(sentence.literal(number_surface(rng, number))); sentence.say({"train": "회 다시 찾아", "dev": "번 재탐색해 줘", "test": "차례 재시도해"}[style]); missing["retries"] = number
        region_ref = None
        if rng.randrange(3) == 0:
            region_ref = rng.choice(contract["names"])+" 검색 영역"
            context["regions"][region_ref] = {"left": 0, "top": 10, "right": 100, "bottom": 50}
            sentence.say({"train": '. 검색 영역은 "', "dev": '. 탐색 범위는 등록한 "', "test": '. 이때 인식 위치는 "'}[style])
            sentence.tokens += ["IN_REGION", sentence.literal(region_ref)]
            sentence.say('" 영역으로 한정해')
        if wait_forever: sentence.tokens.append("WAIT_FOREVER")
        return {"op": op, "text": name, "match": "EXACT", "region_ref": region_ref, "timeout_ms": 3000, "wait_forever": wait_forever, "missing": missing}
    if op in POINT_ACTIONS:
        context["points"][name] = {"x": 25, "y": 70}
        phrases = {"train": {"TAP": " 포인트를 눌러", "DOUBLE": " 포인트를 두 번 눌러", "LONG": " 포인트를 길게 눌러"}, "dev": {"TAP": "이며 등록 위치를 터치해 줘", "DOUBLE": "이며 등록 위치를 더블 터치해 줘", "LONG": "이며 등록 위치를 오래 눌러 줘"}, "test": {"TAP": "에 기록된 좌표를 한 번 터치해", "DOUBLE": "에 기록된 좌표를 두 차례 연속 터치해", "LONG": "에 기록된 좌표에서 롱프레스해"}}
        sentence.say(phrases[style][op])
        return {"op": op, "point_ref": name}
    if op in GESTURE_ACTIONS:
        context["gestures"][name] = gesture_fixture(op)
        sentence.say({"train": " 경로로 ", "dev": "이며 이 저장 경로를 사용해서 ", "test": "에 보관된 궤적에 맞춰 "}[style]+GESTURE_TEXT[op]); return {"op": op, "gesture_ref": name}
    if op == "APP":
        context["apps"][name] = "example.public.fixture"
        sentence.say({"train": " 앱을 실행해", "dev": "이며 등록 앱을 열어 줘", "test": "에 연결한 애플리케이션을 시작해"}[style]); return {"op": op, "app_ref": name}
    if op == "CALL":
        context["macros"][name] = macro_fixture()
        sentence.say({"train": " 저장 매크로를 실행해", "dev": "이며 보관한 매크로를 불러 수행해 줘", "test": "라는 기존 매크로의 동작들을 수행해"}[style]); return {"op": op, "macro_ref": name}
    raise ValueError("지원하지 않는 합성 동작")


def _semantic_signature(node):
    if isinstance(node, list):
        return [_semantic_signature(value) for value in node]
    if isinstance(node, dict):
        normalized = {}
        for key, value in node.items():
            if key in ("text", "reject_text"):
                normalized[key] = None if value is None else "<TEXT>"
            elif key in ("point_ref", "gesture_ref", "app_ref", "macro_ref", "region_ref", "action_ref"):
                normalized[key] = None if value is None else "<REFERENCE>"
            elif key == "region_refs":
                normalized[key] = ["<REFERENCE>"]*len(value)
            elif key in ("duration_ms", "timeout_ms", "count", "retries", "repetitions", "cooldown_ms", "interval_ms", "target"):
                normalized[key] = "<NUMBER>"
            else:
                normalized[key] = _semantic_signature(value)
        return normalized
    return node


def generate_split(split, count, seed=1729):
    contract = SPLITS[split]
    rng = random.Random(seed+{"train": 0, "dev": 100000, "test": 200000}[split])
    result, seen = [], set()
    attempts = 0
    while len(result) < count:
        attempts += 1
        if attempts > count*1000:
            raise ValueError("고유 합성 문장이 부족합니다")
        sentence = Sentence()
        family = rng.choice(contract["families"])
        prefix = {"직접지시": "", "존댓말지시": "다음대로 해 주세요. ", "작업부탁": "이 작업을 부탁할게. ", "수행요청": "아래 순서를 수행해 줘. "}[family]
        sentence.say(prefix)
        context = {kind: {} for kind in ("points", "gestures", "regions", "macros", "apps", "actions")}
        context["defaults"] = {"watch_cooldown_ms": 600000, "watch_interval_ms": 10000, "entry_timeout_ms": 3000, "recognition_source": "OCR"}
        kind = rng.choices(("atomic", "sequence", "conditional", "repeat", "watch", "reject"), (20, 30, 18, 16, 8, 8))[0]
        context["planning_mode"] = "WATCH" if kind == "watch" else "MACRO"
        sentence.tokens = ["PLAN"]
        features = {}
        if kind == "reject":
            reason = rng.choice(("NEGATED", "AMBIGUOUS", "MISSING_REFERENCE", "UNSUPPORTED", "INCOMPLETE"))
            name = rng.choice(contract["names"])
            text = {"train": {"NEGATED": '"'+name+'"를 입력하지 마', "AMBIGUOUS": '"'+name+'" 말고 저기를 눌러', "MISSING_REFERENCE": '등록되지 않은 "'+name+'" 포인트를 눌러', "UNSUPPORTED": '"'+name+'"로 결제해', "INCOMPLETE": '"'+name+'"가 있으면 그 다음에는'}, "dev": {"NEGATED": '텍스트 "'+name+'" 입력은 금지야', "AMBIGUOUS": '대상 "'+name+'" 근처의 여기를 터치해 줘', "MISSING_REFERENCE": '저장 목록에 없는 "'+name+'" 위치로 터치해 줘', "UNSUPPORTED": '이름 "'+name+'"의 암호를 추측해 줘', "INCOMPLETE": '만약 대상 "'+name+'"가 보인다면'}, "test": {"NEGATED": '문구 "'+name+'"를 쓰는 동작은 하지 않도록 해', "AMBIGUOUS": '이름 "'+name+'" 옆 저 위치를 선택해', "MISSING_REFERENCE": '위치 등록을 안 한 "'+name+'" 좌표를 사용해', "UNSUPPORTED": '이름 "'+name+'"의 결제 승인을 대신해', "INCOMPLETE": '문구 "'+name+'"의 존재 여부에 따라서'} }[contract["syntax_style"]][reason]
            sentence.say(text); sentence.tokens = ["REJECT", reason, "EOS"]
            plan = {"schema": SCHEMA, "status": "reject", "reason": reason}
        else:
            if kind == "atomic":
                nodes = [_atomic(sentence, rng, contract, context)]
            elif kind == "sequence":
                nodes = []
                for index in range(rng.choice((2, 3, 4))):
                    if contract["syntax_style"] == "train":
                        if index: sentence.say(rng.choice((". 그 뒤에 ", ". 다음으로 ", ", 그리고 ")))
                    elif contract["syntax_style"] == "dev": sentence.say("먼저 " if not index else ". 이어지는 작업으로 ")
                    else: sentence.say(f"{index+1}번 순서: ")
                    nodes.append(_atomic(sentence, rng, contract, context))
            elif kind == "conditional":
                present = bool(rng.randrange(2)); name = rng.choice(contract["names"])
                if split == "dev": sentence.say("표시 여부별로 처리해 줘. ")
                elif split == "test": sentence.say("조건 판단 대상은 ")
                sentence.say('"'); pointer = sentence.literal(name)
                sentence.say({'train': '" 글자가 '+("있으면 " if present else "없으면 "), 'dev': '" 글자가 '+("보일 때는 " if present else "안 보일 때는 "), 'test': '"이야. 이 문구가 '+("존재한다면 " if present else "부재라면 ")}[split])
                sentence.tokens += ["IF_PRESENT" if present else "IF_ABSENT", pointer, "THEN"]
                yes = [_atomic(sentence, rng, contract, context)]
                sentence.say({"train": ". 그렇지 않으면 ", "dev": ". 반대인 경우에는 ", "test": ". 조건의 반대쪽 작업은 "}[split]); sentence.tokens.append("ELSE")
                no = [_atomic(sentence, rng, contract, context)]; sentence.tokens.append("END_IF")
                nodes = [{"op": "IF", "text": name, "test": "PRESENT" if present else "ABSENT", "then": yes, "else": no}]
            elif kind == "repeat":
                number = rng.choice(contract["numbers"])
                sentence.say({"train": "다음 묶음을 ", "dev": "되풀이 횟수를 ", "test": "묶음의 수행 횟수는 "}[split]); pointer = sentence.literal(number_surface(rng, number)); sentence.say({"train": "회 반복해: ", "dev": "번으로 정해 줘. 묶음의 첫 작업은 ", "test": "회야. 아래 두 작업을 하나의 묶음으로 처리해: "}[split])
                sentence.tokens += ["REPEAT", pointer]
                inner = [_atomic(sentence, rng, contract, context)]
                sentence.say({"train": ". 그리고 ", "dev": ". 묶음의 두 번째 작업은 ", "test": ". 두 번째는 "}[split]); inner.append(_atomic(sentence, rng, contract, context))
                sentence.say({"train": ". 여기까지 반복하고 매크로를 정지해", "dev": ". 묶음을 모두 되풀이한 다음 이 작업을 끝내", "test": ". 이 묶음의 수행을 마친 다음 매크로 실행을 멈춰"}[split])
                sentence.tokens += ["END_REPEAT", "STOP"]
                nodes = [{"op": "REPEAT", "count": number, "body": inner}, {"op": "STOP"}]
            else:
                name, macro = rng.sample(contract["names"], 2)
                context["macros"][macro] = macro_fixture()
                sentence.tokens.append("WATCH")
                sentence.say({"train": '"', "dev": '목록 탐색 문구를 "', "test": '자동 목록의 탐지 대상 "'}[split]); sentence.tokens.append(sentence.literal(name)); sentence.say({"train": '"를 찾으면 "', "dev": '"로 정해 줘. 발견 시 보관한 "', "test": '"의 발견을 기준으로 "'}[split])
                sentence.tokens.append(sentence.literal(macro)); sentence.say({"train": '" 저장 매크로를 ', "dev": '" 매크로를 ', "test": '" 매크로의 반복을 '}[split])
                number = rng.choice(contract["numbers"]); sentence.tokens.append(sentence.literal(number_surface(rng, number))); sentence.say({"train": "회 실행해", "dev": "번 돌려 줘", "test": "회로 해"}[split])
                cooldown_mode = rng.randrange(4)
                if cooldown_mode <= 1:
                    cooldown_ms = context["defaults"]["watch_cooldown_ms"]
                    sentence.tokens.append("DEFAULT_COOLDOWN")
                    if cooldown_mode == 1: sentence.say({"train": ". 완료 뒤에는 기본 시간만큼 쉬어", "dev": ". 전체 수행 후 휴식은 기존 기본값으로 해 줘", "test": ". 마친 뒤 대기 길이는 설정된 기본값이야"}[split])
                else:
                    cooldown = rng.choice(contract["numbers"])
                    cooldown_unit = "MIN" if cooldown <= 10 and rng.randrange(2) else "SEC"
                    cooldown_ms = cooldown*(60000 if cooldown_unit == "MIN" else 1000)
                    sentence.say({"train": ". 완료하면 ", "dev": ". 모두 끝난 뒤 휴식 시간은 ", "test": ". 완료 후 쉬는 길이는 "}[split])
                    sentence.tokens += [sentence.literal(number_surface(rng, cooldown)), cooldown_unit]
                    sentence.say(("분" if cooldown_unit == "MIN" else "초")+{"train": " 쉬어", "dev": "로 해 줘", "test": "야"}[split])
                interval_mode = rng.randrange(4)
                if interval_mode <= 1:
                    interval_ms = context["defaults"]["watch_interval_ms"]
                    sentence.tokens.append("DEFAULT_INTERVAL")
                    if interval_mode == 1: sentence.say({"train": ". 그 뒤 기본 간격으로 아래로 당겨 새로고침해", "dev": ". 이후 갱신 주기는 기본값으로 맞추고 아래로 당겨 줘", "test": ". 휴식 이후 설정된 갱신 주기로 아래로 내려 갱신해"}[split])
                else:
                    interval = rng.choice(contract["numbers"]); interval_ms = interval*1000
                    sentence.say({"train": ". 그 뒤 ", "dev": ". 이후 갱신 주기를 ", "test": ". 휴식을 마친 다음 "}[split])
                    sentence.tokens += [sentence.literal(number_surface(rng, interval)), "SEC"]; sentence.say({"train": "초마다 아래로 당겨 새로고침해", "dev": "초로 맞추고 아래로 당기기를 수행해 줘", "test": "초 간격으로 화면을 아래로 내려 갱신해"}[split])
                region_refs = []
                features.update(cooldown_whole_clause_omitted=cooldown_mode == 0, interval_whole_clause_omitted=interval_mode == 0, cooldown_default_explicit=cooldown_mode == 1, interval_default_explicit=interval_mode == 1)
                for region_index in range(rng.choice((0, 1, 2, 3, 6))):
                    region_name = name+" "+("첫째", "둘째", "셋째", "넷째", "다섯째", "여섯째")[region_index]+" 목록 영역"
                    context["regions"][region_name] = {"left": 0, "top": region_index*15, "right": 100, "bottom": (region_index+1)*15}
                    sentence.say({"train": '. 탐지 우선순위 영역은 "', "dev": '. 다음 등록 범위부터 차례로 검색해 줘: "', "test": '. 탐색 순서에 포함할 사각형 "'}[split])
                    sentence.tokens += ["WATCH_REGION", sentence.literal(region_name)]
                    sentence.say('"'); region_refs.append(region_name)
                entry = None
                if rng.randrange(4) != 0:
                    entry_text = rng.choice(contract["names"])
                    sentence.say({"train": '. 단, 매크로 실행 전에 진입 화면에서 "', "dev": '. 실행의 선행 조건으로 이동한 화면에 "', "test": '. 매크로를 시작할 수 있는 진입 확인 문구는 "'}[split])
                    sentence.tokens += ["ENTRY", sentence.literal(entry_text)]; sentence.say('"가 있는지 확인해')
                    entry_region = None
                    region_mode = rng.randrange(3)
                    features["entry_region_mode"] = ("REGISTERED", "ALL", "DEFAULT_FIRST")[region_mode]
                    if region_mode == 0:
                        entry_region = macro+" 진입 확인 영역"
                        context["regions"][entry_region] = {"left": 10, "top": 60, "right": 90, "bottom": 90}
                        sentence.say('. 진입 확인 범위는 "'); sentence.tokens += ["ENTRY_REGION", sentence.literal(entry_region)]; sentence.say('"로 해')
                    elif region_mode == 1:
                        sentence.tokens.append("ENTRY_ALL"); sentence.say(". 진입 확인은 전체 화면에서 해")
                    else:
                        sentence.tokens.append("ENTRY_DEFAULT_REGION")
                        entry_region = region_refs[0] if region_refs else None
                    sentence.tokens.append("ENTRY_TIMEOUT")
                    if rng.randrange(3) == 0:
                        features["entry_timeout_omitted"] = True
                        entry_timeout = context["defaults"]["entry_timeout_ms"]
                        sentence.tokens.append("DEFAULT_ENTRY_TIMEOUT")
                    else:
                        entry_seconds = rng.choice(contract["numbers"]); entry_timeout = entry_seconds*1000
                        sentence.say(". 확인 제한은 "); sentence.tokens += [sentence.literal(number_surface(rng, entry_seconds)), "SEC"]; sentence.say("초로 해")
                    reject_text = None
                    if rng.randrange(2):
                        reject_text = rng.choice(contract["names"])
                        sentence.say('. 되돌아갈 글자는 "'); sentence.tokens += ["REJECT_TEXT", sentence.literal(reject_text)]; sentence.say('"야. 이 글자가 보이면 확인 글자보다 우선해서 되돌아가')
                    else: sentence.tokens.append("NO_REJECT")
                    sentence.say({"train": ". 확인 실패 시 뒤로 한 번 간 뒤 첫 우선순위부터 다시 탐색하고 매크로는 실행하지 마", "dev": ". 선행 조건을 못 채우면 이전 화면으로 한 번 복귀하고 첫 탐색 범위로 돌아가 줘. 매크로를 건너뛰어 줘", "test": ". 확인 대상 부재 또는 거절 대상의 존재는 한 번의 뒤로가기와 처음 목록 재탐색으로 처리하고 매크로를 시작하지 않아"}[split])
                    sentence.tokens.append("BACK_TO_FIRST")
                    entry = {"text": entry_text, "region_ref": entry_region, "timeout_ms": entry_timeout, "reject_text": reject_text, "failure": "BACK_TO_FIRST", "back_count": 1}
                else: sentence.tokens.append("NO_ENTRY")
                sentence.tokens.append("END_WATCH")
                nodes = [{"op": "WATCH", "text": name, "macro_ref": macro, "repetitions": number, "cooldown_ms": cooldown_ms, "interval_ms": interval_ms, "region_refs": region_refs, "entry": entry}]
            sentence.tokens += ["END", "EOS"]
            plan = {"schema": SCHEMA, "status": "ready", "body": nodes}
        signature = hashlib.sha256(json.dumps(_semantic_signature(plan), sort_keys=True).encode()).hexdigest()
        # 원자 동작은 공유하되 복합 의미 조합은 이름·숫자와 독립적으로 분리합니다.
        bucket = int(signature[:8], 16) % 10
        if kind in ("sequence", "conditional", "repeat", "watch") and bucket not in ({"train": set(range(8)), "dev": {8}, "test": {9}}[split]):
            continue
        key = json.dumps([sentence.command, context], ensure_ascii=False, sort_keys=True)
        if key in seen:
            continue
        seen.add(key)
        result.append({"id": f"{split}-{len(result):06d}", "split": split, "family": family, "kind": kind, "features": features, "source": "직접 작성한 공개용 합성", "command": sentence.command, "context": context, "tokens": sentence.tokens, "plan": plan, "semantic_hash": signature})
    return result


def freeze_dataset(output, counts=(12000, 1000, 1000), seed=1729):
    output = Path(output)
    if output.exists():
        raise ValueError("기존 분리 자료를 덮어쓰지 않습니다")
    output.mkdir(parents=True)
    contract = split_contract()
    manifest = {"format": "pxe-split-v5", "numeric_contract": "모델이 COPY로 선택한 수치 슬롯의 아라비아/고유어/한자어 정수 및 정확한 정수ms가 되는 소수 시간. 숫자 값 도메인과 .5 파생 도메인은 생성 전 split별 고정.", "semantic_holdout_kinds": ["sequence", "conditional", "repeat", "watch"], "shared_atomic_kinds": ["atomic", "reject"], "semantic_signature_revision": "모든 text/reject_text·point/gesture/app/macro/region/action 참조와 중첩 entry·숫자 슬롯 정규화; region 목록 길이·null·형제 순서·정책/조건 구분 보존", "default_contract": {"watch_cooldown_ms": 600000, "watch_interval_ms": 10000, "entry_timeout_ms": 3000, "entry_region_omitted": "선택한 첫 watch영역; 영역없으면 전체", "entry_region_explicit_all": "전체 화면", "recognition_source": "UI의 기존 설정 스냅샷; 독립 합성 fixture는OCR", "whole_clause_omission": "WATCH planning_mode 문맥에만 기본 휴식/갱신/진입확인 시간을 적용. MACRO 문맥에서 임의 WATCH 계획 수락 금지."}, "seed": seed, "contract_before_generation": contract, "origin": "개인 대화·실제 화면을 사용하지 않은 자체 합성", "test_usage": "독립 계약 승인 전 평가·학습·역치 조정 금지", "splits": {}}
    all_exact, all_normalized, all_semantic, all_surfaces = {}, {}, {}, {}
    for split, count in zip(("train", "dev", "test"), counts):
        rows = generate_split(split, count, seed)
        validation_errors = []
        for row in rows:
            try:
                if parse_plan(row["tokens"], row["command"], row["context"]) != row["plan"]:
                    validation_errors.append({"id": row["id"], "error": "정답 AST와 parser 결과 불일치"})
            except (PlanError, ValueError) as exc:
                validation_errors.append({"id": row["id"], "error": str(exc)})
        if validation_errors:
            (output / (split+"-gold-validation-failure.json")).write_text(json.dumps(validation_errors, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
            raise ValueError("생성 정답 전수 검증 실패; 해당 자료를 훈련하지 않습니다")
        raw = "".join(json.dumps(row, ensure_ascii=False, sort_keys=True)+"\n" for row in rows).encode("utf-8")
        (output / (split+".jsonl")).write_bytes(raw)
        exact = {hashlib.sha256(row["command"].encode()).hexdigest() for row in rows}
        normalized = {hashlib.sha256(" ".join(unicodedata.normalize("NFC", row["command"]).split()).encode()).hexdigest() for row in rows}
        semantic = {row["semantic_hash"] for row in rows if row["kind"] in ("sequence", "conditional", "repeat", "watch")}
        all_exact[split], all_normalized[split], all_semantic[split] = exact, normalized, semantic
        surfaces = set()
        for row in rows:
            surface = row["command"]
            copies = [token for token in row["tokens"] if isinstance(token, dict)]
            for token in sorted(copies, key=lambda token: token["start"], reverse=True):
                surface = surface[:token["start"]]+"<SLOT>"+surface[token["end"]:]
            for prefix in ("다음대로 해 주세요. ", "이 작업을 부탁할게. ", "아래 순서를 수행해 줘. "):
                if surface.startswith(prefix): surface = surface[len(prefix):]
            # 거절 문장은 COPY가 없으므로 그 안의 등록 문자열을 별도로 마스킹합니다.
            for name in contract[split]["names"]:
                surface = surface.replace(name, "<NAME>")
            surfaces.add(surface)
        all_surfaces[split] = surfaces
        feature_counts = Counter(key for row in rows for key, value in row["features"].items() if value is True)
        manifest["splits"][split] = {"rows": len(rows), "bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest(), "kind_counts": dict(Counter(row["kind"] for row in rows)), "feature_counts": dict(feature_counts), "unique_commands": len(exact), "unique_normalized_commands": len(normalized), "unique_composite_semantics": len(semantic), "gold_parse_checked": len(rows), "gold_parse_errors": 0, "slot_masked_prefix_removed_surface_templates": len(surfaces)}
        if split == "train":
            tokenizer = CodepointTokenizer.fit([row["command"] for row in rows])
            tokenizer.save(output / "tokenizer.json")
            manifest["tokenizer_train_sources_only"] = True
            manifest["tokenizer_size"] = tokenizer.size
    overlaps = {}
    for a, b in (("train", "dev"), ("train", "test"), ("dev", "test")):
        overlaps[a+"_"+b] = {"exact": len(all_exact[a]&all_exact[b]), "normalized": len(all_normalized[a]&all_normalized[b]), "composite_semantic": len(all_semantic[a]&all_semantic[b]), "slot_masked_prefix_removed_surface": len(all_surfaces[a]&all_surfaces[b])}
    manifest["overlaps"] = overlaps
    manifest["near_duplicate_limit"] = "동일 원자 동작·상용 접속어는 공유하며 근접 문구의 임베딩 유사도 검사는 수행하지 않았습니다. 가족·이름·숫자·복합 의미 조합 분리와 exact/NFC 공백 정규화 검사를 구분합니다."
    (output / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    return manifest


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True)
    parser.add_argument("--train", type=int, default=12000)
    parser.add_argument("--dev", type=int, default=1000)
    parser.add_argument("--test", type=int, default=1000)
    args = parser.parse_args()
    print(json.dumps(freeze_dataset(args.output, (args.train, args.dev, args.test)), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
