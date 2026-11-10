"""학습 모델의 타입 토큰을 엄격하게 검사합니다. 자연어 의미 파서는 아닙니다."""
from copy import deepcopy
import re
from . import snapshots
from .numbers import parse_integer, parse_duration

SCHEMA = "pxe.plan.v1"
STEP_TYPES = ("TAP", "DOUBLE", "LONG", "SWIPE", "WAIT", "TEXT_TAP", "TEXT_WAIT", "TEXT_BRANCH", "BACK", "HOME", "RECENTS", "INPUT", "APP", "GOTO", "STOP", "CURVE", "PINCH_IN", "PINCH_OUT", "MULTI_TAP", "GESTURE", "TEXT_CHECK", "NOTIFICATIONS")
NO_ARGS = {"BACK", "HOME", "RECENTS", "STOP", "NOTIFICATIONS"}
POINT_ACTIONS = {"TAP", "DOUBLE", "LONG"}
GESTURE_ACTIONS = {"SWIPE", "CURVE", "PINCH_IN", "PINCH_OUT", "MULTI_TAP", "GESTURE"}
TEXT_ACTIONS = {"TEXT_TAP", "TEXT_WAIT", "TEXT_CHECK"}
POLICIES = ("STOP", "SKIP", "BACK_RETRY", "RETRY", "SCROLL_RETRY", "PULL_RETRY", "ACTION_RETRY")
REASONS = ("NEGATED", "AMBIGUOUS", "MISSING_REFERENCE", "UNSUPPORTED", "INCOMPLETE", "LIMIT")
TOKENS = ("PAD", "BOS", "EOS", "PLAN", "END", "COPY", "MS", "SEC", "MIN", "HOUR", "IF_PRESENT", "IF_ABSENT", "THEN", "ELSE", "END_IF", "REPEAT", "END_REPEAT", "CALL", "WATCH", "REJECT", "FOREVER", "IN_REGION", "WAIT_FOREVER", "WATCH_REGION", "ENTRY", "ENTRY_ALL", "ENTRY_REGION", "ENTRY_TIMEOUT", "REJECT_TEXT", "NO_REJECT", "BACK_TO_FIRST", "NO_ENTRY", "END_WATCH", "DEFAULT_COOLDOWN", "DEFAULT_INTERVAL", "DEFAULT_ENTRY_TIMEOUT", "ENTRY_DEFAULT_REGION") + STEP_TYPES + tuple("F_"+p for p in POLICIES) + REASONS
TOKEN_TO_ID = {token: index for index, token in enumerate(TOKENS)}


class PlanError(ValueError):
    """검증 실패는 완성 계획 전체를 거절합니다."""


def _integer(value, lower, upper, label):
    if type(value) is not int or not lower <= value <= upper:
        raise PlanError(label+" 범위를 확인하세요")
    return value


def _keys(value, expected):
    if type(value) is not dict or set(value) != set(expected):
        raise PlanError("계획의 필수 필드 또는 알 수 없는 필드가 있습니다")


def _text(value):
    if not isinstance(value, str) or not value.strip() or len(value) > 1024:
        raise PlanError("문구가 없거나 너무 깁니다")
    return value


def _reference(context, kind, name):
    if not isinstance(context, dict) or not isinstance(context.get(kind, {}), dict) or name not in context.get(kind, {}):
        raise PlanError("등록되지 않은 참조: "+kind)
    return context[kind][name]


def _registered_region(context, name):
    try:
        return snapshots.region(_reference(context, "regions", name))
    except ValueError as exc:
        raise PlanError(str(exc)) from exc


def _registered_macro(context, name):
    try:
        return snapshots.macro(_reference(context, "macros", name), STEP_TYPES)
    except ValueError as exc:
        raise PlanError(str(exc)) from exc


def validate_plan(plan, context):
    """AST의 의미를 재해석하지 않고 필드·범위·등록 참조를 확인합니다."""
    if type(plan) is not dict or plan.get("schema") != SCHEMA:
        raise PlanError("지원하지 않는 계획 형식")
    if plan.get("status") == "reject":
        _keys(plan, ("schema", "status", "reason"))
        if plan["reason"] not in REASONS:
            raise PlanError("알 수 없는 거절 사유")
        return deepcopy(plan)
    _keys(plan, ("schema", "status", "body"))
    if plan["status"] != "ready":
        raise PlanError("완성되지 않은 계획")
    node_count = [0]

    def body(nodes, depth=0):
        if not isinstance(nodes, list) or not nodes or depth > 4:
            raise PlanError("빈 계획 또는 조건 중첩 한도 초과")
        for node in nodes:
            node_count[0] += 1
            if node_count[0] > 128 or type(node) is not dict:
                raise PlanError("계획 동작 수 한도 초과")
            op = node.get("op")
            if op in NO_ARGS:
                _keys(node, ("op",))
            elif op == "WAIT":
                _keys(node, ("op", "duration_ms"))
                _integer(node["duration_ms"], 1, 600000, "대기 시간")
            elif op == "INPUT":
                _keys(node, ("op", "text")); _text(node["text"])
            elif op in POINT_ACTIONS:
                _keys(node, ("op", "point_ref"))
                point = _reference(context, "points", _text(node["point_ref"]))
                if type(point) is not dict or set(point) != {"x", "y"} or any(type(point[k]) not in (int, float) or not 0 <= point[k] <= 100 for k in ("x", "y")):
                    raise PlanError("등록 포인트 좌표가 유효하지 않습니다")
            elif op in GESTURE_ACTIONS:
                _keys(node, ("op", "gesture_ref"))
                gesture = _reference(context, "gestures", _text(node["gesture_ref"]))
                if type(gesture) is not dict or gesture.get("type") != op or not isinstance(gesture.get("payload"), dict):
                    raise PlanError("등록 제스처 유형이 일치하지 않습니다")
                if gesture["payload"].get("type") != op:
                    raise PlanError("등록 경로 payload 유형이 일치하지 않습니다")
                try: snapshots.step(gesture["payload"], STEP_TYPES, 1)
                except ValueError as exc: raise PlanError(str(exc)) from exc
            elif op == "APP":
                _keys(node, ("op", "app_ref"))
                package = _reference(context, "apps", _text(node["app_ref"]))
                if not isinstance(package, str) or not re.fullmatch(r"[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z0-9_]+)+", package):
                    raise PlanError("등록 앱 패키지 형식 오류")
            elif op in TEXT_ACTIONS:
                _keys(node, ("op", "text", "match", "region_ref", "timeout_ms", "wait_forever", "missing"))
                _text(node["text"])
                if node["match"] != "EXACT":
                    raise PlanError("현재 모델은 정확히 일치만 지원합니다")
                if node["region_ref"] is not None:
                    _registered_region(context, node["region_ref"])
                if type(node["wait_forever"]) is not bool or node["wait_forever"] and op not in ("TEXT_TAP", "TEXT_WAIT"):
                    raise PlanError("계속 기다리기 유형 오류")
                _integer(node["timeout_ms"], 0, 600000, "검색 시간")
                missing = node["missing"]
                _keys(missing, ("policy", "retries", "forever", "action_ref"))
                if missing["policy"] not in POLICIES or type(missing["forever"]) is not bool:
                    raise PlanError("미발견 정책 오류")
                _integer(missing["retries"], 0, 100, "재시도")
                if missing["policy"] == "ACTION_RETRY":
                    action = _reference(context, "actions", missing["action_ref"])
                    if type(action) is not dict or action.get("type") not in NO_ARGS | POINT_ACTIONS | GESTURE_ACTIONS | {"WAIT", "INPUT", "APP"} or action.get("type") == "STOP":
                        raise PlanError("복구 동작은 일반 동작만 허용합니다")
                    try: snapshots.step(action, STEP_TYPES, 1)
                    except ValueError as exc: raise PlanError(str(exc)) from exc
                elif missing["action_ref"] is not None:
                    raise PlanError("불필요한 복구 참조")
                retry = missing["policy"] not in ("STOP", "SKIP")
                if (missing["forever"] or missing["retries"]) and not retry:
                    raise PlanError("정지·건너뛰기에 재시도를 지정할 수 없습니다")
                if op == "TEXT_CHECK" and retry:
                    raise PlanError("글자 확인의 재시도는 현재 미지원입니다")
                if node["wait_forever"] and retry:
                    raise PlanError("계속 기다리기와 부재 재시도를 함께 지정할 수 없습니다")
            elif op == "IF":
                _keys(node, ("op", "text", "test", "then", "else"))
                _text(node["text"])
                if node["test"] not in ("PRESENT", "ABSENT"):
                    raise PlanError("조건 종류 오류")
                body(node["then"], depth+1); body(node["else"], depth+1)
            elif op == "REPEAT":
                _keys(node, ("op", "count", "body"))
                _integer(node["count"], 1, 1000, "반복")
                body(node["body"], depth+1)
            elif op == "CALL":
                _keys(node, ("op", "macro_ref"))
                _registered_macro(context, _text(node["macro_ref"]))
            elif op == "GOTO":
                _keys(node, ("op", "target")); _integer(node["target"], 1, len(nodes), "이동 번호")
                if depth:
                    raise PlanError("중첩 계획의 원시 GOTO는 미지원입니다")
            elif op == "WATCH":
                if not isinstance(context, dict) or context.get("planning_mode") != "WATCH":
                    raise PlanError("현재 문맥이 목록 탐지 모드가 아닙니다")
                _keys(node, ("op", "text", "macro_ref", "repetitions", "cooldown_ms", "interval_ms", "region_refs", "entry"))
                _text(node["text"]); _registered_macro(context, node["macro_ref"])
                if not isinstance(node["region_refs"], list) or len(node["region_refs"]) > 6 or len(set(node["region_refs"])) != len(node["region_refs"]):
                    raise PlanError("목록 영역은 서로 다른 최대6개입니다")
                rectangles = [_registered_region(context, name) for name in node["region_refs"]]
                for i, left in enumerate(rectangles):
                    for right in rectangles[:i]:
                        if max(left["left"], right["left"]) < min(left["right"], right["right"]) and max(left["top"], right["top"]) < min(left["bottom"], right["bottom"]):
                            raise PlanError("우선순위 영역이 서로 겹칩니다")
                entry = node["entry"]
                if entry is not None:
                    _keys(entry, ("text", "region_ref", "timeout_ms", "reject_text", "failure", "back_count"))
                    _text(entry["text"])
                    if entry["region_ref"] is not None: _registered_region(context, entry["region_ref"])
                    _integer(entry["timeout_ms"], 100, 60000, "진입 확인 시간")
                    if entry["reject_text"] is not None: _text(entry["reject_text"])
                    if entry["failure"] != "BACK_TO_FIRST" or type(entry["back_count"]) is not int or entry["back_count"] != 1:
                        raise PlanError("진입 실패는 뒤로1회 후1순위 재탐색입니다")
                _integer(node["repetitions"], 1, 100000, "목록 반복")
                _integer(node["cooldown_ms"], 0, 86400000, "완료 후 대기")
                _integer(node["interval_ms"], 1000, 86400000, "새로고침 간격")
                if node["interval_ms"] % 1000 or len(nodes) != 1 or depth:
                    raise PlanError("목록 계획은 독립 계획이며 간격은 정수 초입니다")
            else:
                raise PlanError("알 수 없는 동작")
    body(plan["body"])
    return deepcopy(plan)


def parse_plan(tokens, source, context):
    """COPY는 원문 codepoint 반열린 구간이며 부분 토큰은 실행하지 않습니다."""
    if not isinstance(tokens, list) or not 1 <= len(tokens) <= 512 or not isinstance(source, str) or len(source) > 4096:
        raise PlanError("계획 또는 문맥 상한 초과")
    index = 0

    def pop(expected=None):
        nonlocal index
        if index >= len(tokens):
            raise PlanError("잘린 계획")
        token = tokens[index]; index += 1
        if expected is not None and token != expected:
            raise PlanError("필수 토큰 누락: "+expected)
        return token

    def copy():
        value = pop()
        _keys(value, ("token", "start", "end"))
        if value["token"] != "COPY":
            raise PlanError("원문 복사 토큰 필요")
        start = _integer(value["start"], 0, len(source), "복사 시작")
        end = _integer(value["end"], start+1, len(source), "복사 끝")
        return _text(source[start:end])

    def number():
        try: return parse_integer(copy())
        except ValueError as exc: raise PlanError(str(exc)) from exc

    def duration():
        value, unit = copy(), pop()
        try: return parse_duration(value, unit)
        except ValueError as exc: raise PlanError(str(exc)) from exc

    def duration_or_default(token, key, lo, hi):
        if index < len(tokens) and tokens[index] == token:
            pop()
            defaults = context.get("defaults") if isinstance(context, dict) else None
            if type(defaults) is not dict or key not in defaults:
                raise PlanError("명시된 context 기본값이 없습니다")
            return _integer(defaults[key], lo, hi, "문맥 기본 시간")
        return duration()

    def nodes(stops, depth=0):
        if depth > 4:
            raise PlanError("조건 중첩 한도 초과")
        result = []
        while index < len(tokens) and isinstance(tokens[index], str) and tokens[index] not in stops:
            op = pop()
            if op in NO_ARGS:
                node = {"op": op}
            elif op == "WAIT":
                node = {"op": op, "duration_ms": duration()}
            elif op == "INPUT":
                node = {"op": op, "text": copy()}
            elif op in POINT_ACTIONS:
                node = {"op": op, "point_ref": copy()}
            elif op in GESTURE_ACTIONS:
                node = {"op": op, "gesture_ref": copy()}
            elif op == "APP":
                node = {"op": op, "app_ref": copy()}
            elif op in TEXT_ACTIONS:
                text = copy(); policy = pop()
                if policy not in tuple("F_"+p for p in POLICIES):
                    raise PlanError("미발견 정책 누락")
                policy = policy[2:]
                missing = {"policy": policy, "retries": 0, "forever": False, "action_ref": None}
                if policy not in ("STOP", "SKIP"):
                    if index < len(tokens) and tokens[index] == "FOREVER":
                        pop(); missing["forever"] = True
                    else:
                        missing["retries"] = number()
                    if policy == "ACTION_RETRY":
                        missing["action_ref"] = copy()
                node = {"op": op, "text": text, "match": "EXACT", "region_ref": None, "timeout_ms": 3000, "wait_forever": False, "missing": missing}
                if index < len(tokens) and tokens[index] == "IN_REGION":
                    pop(); node["region_ref"] = copy()
                if index < len(tokens) and tokens[index] == "WAIT_FOREVER":
                    pop(); node["wait_forever"] = True
            elif op in ("IF_PRESENT", "IF_ABSENT", "TEXT_BRANCH"):
                text = copy(); pop("THEN")
                yes = nodes({"ELSE"}, depth+1); pop("ELSE")
                no = nodes({"END_IF"}, depth+1); pop("END_IF")
                node = {"op": "IF", "text": text, "test": "ABSENT" if op == "IF_ABSENT" else "PRESENT", "then": yes, "else": no}
            elif op == "REPEAT":
                count = number(); inner = nodes({"END_REPEAT"}, depth+1); pop("END_REPEAT")
                node = {"op": op, "count": count, "body": inner}
            elif op == "CALL":
                node = {"op": op, "macro_ref": copy()}
            elif op == "GOTO":
                node = {"op": op, "target": number()}
            elif op == "WATCH":
                node = {"op": op, "text": copy(), "macro_ref": copy(), "repetitions": number(), "cooldown_ms": duration_or_default("DEFAULT_COOLDOWN", "watch_cooldown_ms", 0, 86400000), "interval_ms": duration_or_default("DEFAULT_INTERVAL", "watch_interval_ms", 1000, 86400000), "region_refs": [], "entry": None}
                while index < len(tokens) and tokens[index] == "WATCH_REGION":
                    pop(); node["region_refs"].append(copy())
                option = pop()
                if option == "ENTRY":
                    entry_text = copy(); region_option = pop()
                    if region_option == "ENTRY_REGION": entry_region = copy()
                    elif region_option == "ENTRY_ALL": entry_region = None
                    elif region_option == "ENTRY_DEFAULT_REGION": entry_region = node["region_refs"][0] if node["region_refs"] else None
                    else: raise PlanError("진입 확인 영역 누락")
                    pop("ENTRY_TIMEOUT"); entry_timeout = duration_or_default("DEFAULT_ENTRY_TIMEOUT", "entry_timeout_ms", 100, 60000)
                    rejection = pop()
                    if rejection == "REJECT_TEXT": reject_text = copy()
                    elif rejection == "NO_REJECT": reject_text = None
                    else: raise PlanError("진입 거절 문구 설정 누락")
                    pop("BACK_TO_FIRST")
                    node["entry"] = {"text": entry_text, "region_ref": entry_region, "timeout_ms": entry_timeout, "reject_text": reject_text, "failure": "BACK_TO_FIRST", "back_count": 1}
                elif option != "NO_ENTRY": raise PlanError("진입 확인 설정 누락")
                pop("END_WATCH")
            else:
                raise PlanError("알 수 없는 구조 토큰")
            result.append(node)
        return result

    first = pop()
    if first == "REJECT":
        plan = {"schema": SCHEMA, "status": "reject", "reason": pop()}
    elif first == "PLAN":
        plan = {"schema": SCHEMA, "status": "ready", "body": nodes({"END"})}
        pop("END")
    else:
        raise PlanError("계획 시작 토큰 누락")
    pop("EOS")
    if index != len(tokens):
        raise PlanError("종료 뒤 남은 토큰")
    return validate_plan(plan, context)
