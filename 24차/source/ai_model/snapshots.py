"""등록 참조의 필수 데이터와 수치 경계를 기본값 없이 검사합니다."""
import math
import re


def _fail():
    raise ValueError("등록 스냅샷의 필수 데이터 또는 범위가 잘못됐습니다")


def integer(value, lo, hi):
    if type(value) is not int or not lo <= value <= hi: _fail()


def coordinate(value):
    if type(value) not in (int, float) or not math.isfinite(value) or not 0 <= value <= 100: _fail()


def region(value):
    if type(value) is not dict or set(value) != {"left", "top", "right", "bottom"}: _fail()
    for number in value.values(): coordinate(number)
    if value["left"] >= value["right"] or value["top"] >= value["bottom"]: _fail()
    return value


def step(value, types, count):
    if type(value) is not dict or value.get("type") not in types: _fail()
    for field in ("enabled", "repeat", "after"):
        if field not in value: _fail()
    if type(value["enabled"]) is not bool: _fail()
    integer(value["repeat"], 1, 1000); integer(value["after"], 0, 600000)
    op = value["type"]
    if op in ("TAP", "DOUBLE", "LONG", "SWIPE", "CURVE", "PINCH_IN", "PINCH_OUT"):
        for key in ("x", "y"): coordinate(value.get(key))
    if op in ("SWIPE", "CURVE", "PINCH_IN", "PINCH_OUT"):
        for key in ("x2", "y2"): coordinate(value.get(key))
    if op == "CURVE":
        for key in ("cx", "cy"): coordinate(value.get(key))
    if op in ("WAIT", "LONG", "SWIPE", "CURVE", "PINCH_IN", "PINCH_OUT", "MULTI_TAP", "GESTURE"):
        integer(value.get("duration"), 1, 59000 if op != "WAIT" else 600000)
    if op in ("PINCH_IN", "PINCH_OUT"):
        if any(not 0 <= value["x"]+sign*value[key] <= 100 for sign in (-1, 1) for key in ("x2", "y2")): _fail()
    if op == "MULTI_TAP":
        if not isinstance(value.get("text"), str): _fail()
        points = value["text"].split(";")
        if not 1 <= len(points) <= 20: _fail()
        for point in points:
            pair = point.split(",")
            if len(pair) != 2: _fail()
            for number in pair:
                try: coordinate(float(number))
                except (ValueError, TypeError): _fail()
    if op == "GESTURE":
        strokes = value.get("strokes")
        if not isinstance(strokes, list) or not 1 <= len(strokes) <= 20: _fail()
        for stroke in strokes:
            if type(stroke) is not dict or set(stroke) != {"start", "duration", "points"}: _fail()
            integer(stroke["start"], 0, 58999); integer(stroke["duration"], 1, 59000)
            if stroke["start"]+stroke["duration"] > 59000: _fail()
            if not isinstance(stroke["points"], list) or not 1 <= len(stroke["points"]) <= 512: _fail()
            for point in stroke["points"]:
                if not isinstance(point, list) or len(point) != 2: _fail()
                coordinate(point[0]); coordinate(point[1])
    if op == "INPUT" and not isinstance(value.get("text"), str): _fail()
    if op == "APP" and (not isinstance(value.get("packageName"), str) or not re.fullmatch(r"[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z0-9_]+)+", value["packageName"])): _fail()
    if op == "GOTO": integer(value.get("target"), 1, count)
    if op.startswith("TEXT_"):
        if not isinstance(value.get("text"), str) or not value["text"].strip(): _fail()
        integer(value.get("timeout"), 0, 600000)
        if value.get("match") not in ("EXACT", "CONTAINS", "REGEX") or value.get("source") not in ("AUTO", "ACCESS", "OCR"): _fail()
        if value["match"] == "REGEX":
            try: re.compile(value["text"])
            except re.error: _fail()
        if value.get("failure") not in ("STOP", "SKIP", "BACK_RETRY", "RETRY", "SCROLL_RETRY", "PULL_RETRY", "ACTION_RETRY"): _fail()
        for field in ("waitForever", "retryForever", "immediate"):
            if type(value.get(field)) is not bool: _fail()
        if value["waitForever"] and (op not in ("TEXT_TAP", "TEXT_WAIT") or value["retryForever"]): _fail()
        if value["retryForever"] and (op not in ("TEXT_TAP", "TEXT_WAIT") or value["failure"] in ("STOP", "SKIP")): _fail()
        if value["failure"] == "ACTION_RETRY": integer(value.get("recoveryTarget"), 1, count)
        if op == "TEXT_BRANCH":
            integer(value.get("found"), 0, count); integer(value.get("missing"), 0, count)
    return value


def macro(value, types):
    if type(value) is not dict or not isinstance(value.get("steps"), list) or not 1 <= len(value["steps"]) <= 500: _fail()
    for key, lo, hi in (("rounds", 0, 100000), ("gap", 0, 600000), ("startDelay", 0, 600000), ("maxMinutes", 0, 1440), ("startAt", 1, len(value["steps"]))):
        integer(value.get(key), lo, hi)
    for item in value["steps"]: step(item, types, len(value["steps"]))
    if not any(item["enabled"] for item in value["steps"]): _fail()
    for item in value["steps"]:
        if item.get("failure") == "ACTION_RETRY":
            recovery = value["steps"][item["recoveryTarget"]-1]
            if not recovery["enabled"] or recovery["type"].startswith("TEXT_") or recovery["type"] in ("GOTO", "STOP"): _fail()
    return value
