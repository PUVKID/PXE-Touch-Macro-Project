"""모델이 선택한 숫자 span만 엄격한 정수·정확한 ms로 변환합니다."""
from decimal import Decimal, InvalidOperation, localcontext
import re

SINO_DIGITS = dict(zip("일이삼사오육칠팔구", range(1, 10)))
SMALL_UNITS = {"십": 10, "백": 100, "천": 1000}
NATIVE_ONES = {"하나": 1, "한": 1, "둘": 2, "두": 2, "셋": 3, "세": 3, "넷": 4, "네": 4, "다섯": 5, "여섯": 6, "일곱": 7, "여덟": 8, "아홉": 9}
NATIVE_TENS = {"열": 10, "스물": 20, "서른": 30, "마흔": 40, "쉰": 50, "예순": 60, "일흔": 70, "여든": 80, "아흔": 90}
UNIT_MS = {"MS": 1, "SEC": 1000, "MIN": 60000, "HOUR": 3600000}
MAX_INTEGER = 100000000


def _small_sino(text):
    if not text: return 0
    total, pending, previous_unit = 0, None, 10000
    for char in text:
        if char in SINO_DIGITS:
            if pending is not None: raise ValueError("연속 한자 숫자는 정수 표기가 아닙니다")
            pending = SINO_DIGITS[char]
        elif char in SMALL_UNITS:
            unit = SMALL_UNITS[char]
            if unit >= previous_unit: raise ValueError("수 단위는 큰 순서로 한 번씩 사용합니다")
            total += (1 if pending is None else pending)*unit
            pending, previous_unit = None, unit
        else:
            raise ValueError("지원하지 않는 숫자 문자")
    return total + (pending or 0)


def _sino(text):
    if text in ("영", "공"): return 0
    total = 0
    if "억" in text:
        if text.count("억") != 1: raise ValueError("중복 억 단위")
        head, text = text.split("억")
        total = (_small_sino(head) if head else 1)*100000000
    if "만" in text:
        if text.count("만") != 1: raise ValueError("중복 만 단위")
        head, tail = text.split("만")
        total += (_small_sino(head) if head else 1)*10000 + _small_sino(tail)
    else:
        total += _small_sino(text)
    return total


def parse_integer(text):
    if type(text) is not str or not 1 <= len(text) <= 48 or any(char.isspace() and char != " " for char in text):
        raise ValueError("정수 원문 형식 오류")
    text = text.strip(" ")
    if not text: raise ValueError("숫자 원문이 비어 있습니다")
    if re.fullmatch(r"[0-9]{1,9}", text):
        result = int(text)
    else:
        compact = text.replace(" ", "")
        if compact in NATIVE_ONES: result = NATIVE_ONES[compact]
        elif compact == "스무": result = 20
        else:
            result = None
            for prefix, tens in NATIVE_TENS.items():
                if compact == prefix:
                    result = tens; break
                if compact.startswith(prefix) and compact[len(prefix):] in NATIVE_ONES:
                    result = tens + NATIVE_ONES[compact[len(prefix):]]; break
            if result is None:
                result = _sino(compact)
    if not 0 <= result <= MAX_INTEGER: raise ValueError("정수 표현 상한 초과")
    return result


def parse_duration(text, unit):
    if unit not in UNIT_MS or type(text) is not str or not 1 <= len(text) <= 48:
        raise ValueError("시간 단위 또는 원문 형식 오류")
    text = text.strip(" ")
    if re.fullmatch(r"[0-9]{1,9}\.[0-9]{1,9}", text):
        try:
            with localcontext() as context:
                context.prec = 40
                value = Decimal(text)*UNIT_MS[unit]
                if not value.is_finite() or value != value.to_integral_value():
                    raise ValueError("시간은 정확한 정수 ms로 나타나야 합니다")
                result = int(value)
        except InvalidOperation as exc:
            raise ValueError("시간 소수 형식 오류") from exc
    else:
        result = parse_integer(text)*UNIT_MS[unit]
    if not 0 <= result <= 86400000:
        raise ValueError("시간은 최대24시간입니다")
    return result


def sino_text(value):
    if type(value) is not int or not 0 <= value <= 9999: raise ValueError("합성 수사 범위")
    if not value: return "영"
    pieces = []
    for unit, char in ((1000, "천"), (100, "백"), (10, "십")):
        digit, value = divmod(value, unit)
        if digit: pieces.append(("" if digit == 1 else "일이삼사오육칠팔구"[digit-1])+char)
    if value: pieces.append("일이삼사오육칠팔구"[value-1])
    return "".join(pieces)


def native_text(value):
    if type(value) is not int or not 1 <= value <= 99: raise ValueError("합성 고유어 수사 범위")
    ones = ("", "한", "두", "세", "네", "다섯", "여섯", "일곱", "여덟", "아홉")
    tens = ("", "열", "스물", "서른", "마흔", "쉰", "예순", "일흔", "여든", "아흔")
    return tens[value//10] + ones[value%10]
