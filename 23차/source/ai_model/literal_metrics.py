"""공급된 구조·pointer 출력의 복원·타입 거절과 gold 비교를 별도로 집계합니다."""
from .schema import TOKENS, parse_plan


def inspect_prediction(codec, encoded, context, words, pairs, *, gold_tokens=None, gold_plan=None):
    # JSON 배열과 Python tuple의 컨테이너만 맞춥니다. offset 값은 변환하지 않습니다.
    normalized_pairs = [tuple(pair) if type(pair) in (list, tuple) and len(pair) == 2 else None for pair in pairs]
    structure = ["COPY" if isinstance(token, dict) else token for token in gold_tokens] if gold_tokens is not None else None
    restored, raw_copies, error = [], [], None
    try:
        if any(type(word) is not str or word not in TOKENS for word in words):
            raise ValueError("출력 어휘 밖 구조 토큰")
        if len(pairs) != words.count("COPY"):
            raise ValueError("COPY와 pointer 쌍 수 불일치")
        pointer = iter(pairs)
        for word in words:
            if word == "COPY":
                start, end = next(pointer)
                a, b = codec.tokens_to_span(encoded, start, end)
                restored.append({"token": "COPY", "start": a, "end": b})
                raw_copies.append(encoded["source"][a:b])
            else:
                restored.append(word)
        plan = parse_plan(restored, encoded["source"], context)
    except (ValueError, TypeError, IndexError) as exc:
        plan, error = None, str(exc)
    gold_pairs = None
    anchor_pairs = None
    raw_expected = None
    if gold_tokens is not None:
        gold_copies = [token for token in gold_tokens if isinstance(token, dict)]
        gold_pairs = [codec.span_to_tokens(encoded, token["start"], token["end"]) for token in gold_copies]
        raw_expected = [encoded["source"][token["start"]:token["end"]] for token in gold_copies]
        anchor_pairs = [(index, pair) for index, pair in enumerate(gold_pairs) if encoded["roles"][pair[0]] == "literal"]
    ready = bool(plan and plan["status"] == "ready")
    return {
        "prediction_origin": "호출자가 공급한 구조·pointer 출력; 이 함수는 NN을 실행하지 않습니다",
        "plan": plan, "tokens": restored, "error": error,
        "nn_reject_prefix": bool(words and words[0] == "REJECT"),
        "nn_explicit_reject": bool(words and words[0] == "REJECT" and plan and plan["status"] == "reject"),
        "compiler_invalid": plan is None, "accepted_ready": ready,
        "wrong_ready": ready and plan != gold_plan if gold_plan is not None else None,
        "structure_exact": words == structure if structure is not None else None,
        "copy_pair_exact": normalized_pairs == gold_pairs and all(codec.pair_is_valid(encoded, *pair) for pair in pairs) if gold_pairs is not None else None,
        "literal_anchor_exact": len(pairs) == len(gold_pairs) and all(normalized_pairs[index] == pair and codec.pair_is_valid(encoded, *pair) for index, pair in anchor_pairs) if anchor_pairs else None,
        "raw_copy_exact": raw_copies == raw_expected if raw_expected is not None else None,
        "ast_exact": plan == gold_plan if gold_plan is not None else None,
        "raw_copies": raw_copies,
    }
