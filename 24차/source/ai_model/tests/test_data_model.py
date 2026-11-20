"""학습 전 분리와 실제 학습 갱신을 확인하는 검사입니다."""
import importlib
import unittest


class DataTests(unittest.TestCase):
    def test_training_byte_probability_zero_preserves_inference_and_rng(self):
        import random
        from ai_model.tokenizer import CodepointTokenizer
        tokenizer = CodepointTokenizer.fit(["한글 인용 abc"])
        context = {"points": {"한글 위치": {}}, "planning_mode": "MACRO"}
        rng = random.Random(19); state = rng.getstate()
        actual = tokenizer.encode_request_training("한글 인용 abc", context, byte_probability=0, rng=rng)
        self.assertEqual(actual, tokenizer.encode_request("한글 인용 abc", context))
        self.assertEqual(rng.getstate(), state)

    def test_training_forced_bytes_preserve_text_copy_and_context_boundary(self):
        import random
        from ai_model.tokenizer import CodepointTokenizer
        command = '입력 "뒤로\n가\t😀\u0085끝"'
        context = {"points": {"한글😀지점": {}}, "planning_mode": "MACRO"}
        tokenizer = CodepointTokenizer.fit([command, "한글😀지점"])
        encoded = tokenizer.encode_request_training(command, context, byte_probability=1, rng=random.Random(9))
        length = encoded["command_tokens"]
        self.assertEqual(tokenizer.decode(encoded["ids"][:length]), command)
        self.assertTrue(all(tokenizer.BYTE_BASE <= token < tokenizer.CHAR_BASE for token in encoded["ids"][:length]))
        self.assertTrue(any(tokenizer.BYTE_BASE+128 <= token < tokenizer.CHAR_BASE for token in encoded["ids"][length+1:]))
        start, end = command.index("😀"), command.index("😀")+1
        first, last = tokenizer.span_to_tokens(encoded, start, end)
        self.assertEqual(last-first+1, 4)
        self.assertEqual(tokenizer.tokens_to_span(encoded, first, last), (start, end))
        with self.assertRaises(ValueError): tokenizer.tokens_to_span(encoded, first+1, last)
        with self.assertRaises(ValueError): tokenizer.tokens_to_span(encoded, first, last-1)
        with self.assertRaises(ValueError): tokenizer.tokens_to_span(encoded, length+1, length+1)
        self.assertTrue(all(offset is None for offset in encoded["offsets"][length:]))

    def test_training_byte_sampling_reproducible_without_global_shuffle_effect(self):
        import random
        from ai_model.tokenizer import CodepointTokenizer
        command = "한글😀복사" * 12
        tokenizer = CodepointTokenizer.fit([command])
        context = {"planning_mode": "MACRO"}
        state = random.getstate()
        one = tokenizer.encode_request_training(command, context, byte_probability=0.3, rng=random.Random(21))
        two = tokenizer.encode_request_training(command, context, byte_probability=0.3, rng=random.Random(21))
        other = tokenizer.encode_request_training(command, context, byte_probability=0.3, rng=random.Random(22))
        self.assertEqual(one, two)
        self.assertNotEqual(one["ids"], other["ids"])
        self.assertEqual(state, random.getstate())

    def test_training_byte_expansion_obeys_whole_character_budget_without_truncation(self):
        import random
        from ai_model.tokenizer import CodepointTokenizer
        command = "😀한" * 400
        tokenizer = CodepointTokenizer.fit([command])
        context = {"planning_mode": "MACRO"}
        base = tokenizer.encode_request(command, context)
        self.assertLessEqual(len(base["ids"]), 1024)
        context_text = tokenizer.decode(base["ids"][base["command_tokens"]+1:])
        self.assertGreater(len(command.encode("utf-8"))+1+len(context_text.encode("utf-8")), 1024)
        stats = {}
        encoded = tokenizer.encode_request_training(command, context, byte_probability=1, rng=random.Random(7), max_input=1024, stats=stats)
        self.assertLessEqual(len(encoded["ids"]), 1024)
        self.assertGreater(stats["budget_skipped_codepoints"], 0)
        self.assertEqual(tokenizer.decode(encoded["ids"][:encoded["command_tokens"]]), command)
        self.assertEqual(tokenizer.decode(encoded["ids"][encoded["command_tokens"]+1:]), tokenizer.decode(base["ids"][base["command_tokens"]+1:]))
        self.assertEqual(encoded["offsets"][encoded["command_tokens"]-1], (len(command)-1, len(command)))
        with self.assertRaises(ValueError): tokenizer.encode_request_training(command, context, byte_probability=1, rng=random.Random(7), max_input=len(base["ids"])-1)

    def test_training_byte_probability_and_limit_are_strict(self):
        import random
        from ai_model.tokenizer import CodepointTokenizer
        tokenizer = CodepointTokenizer.fit(["한"])
        for probability in (-0.1, 1.1, float("nan"), float("inf"), True, "0.3"):
            with self.assertRaises(ValueError): tokenizer.encode_request_training("한", {}, byte_probability=probability, rng=random.Random(1))
        for limit in (0, -1, True, 1024.0):
            with self.assertRaises(ValueError): tokenizer.encode_request_training("한", {}, byte_probability=0.3, rng=random.Random(1), max_input=limit)

    def test_semantic_split_signature_masks_all_nested_reference_names(self):
        from ai_model.data import _semantic_signature
        left = [{"op": "IF", "text": "사과", "test": "PRESENT", "then": [{"op": "TEXT_WAIT", "text": "배", "region_ref": "첫 영역", "timeout_ms": 3000}], "else": [{"op": "WATCH", "macro_ref": "수집", "region_refs": ["목록1", "목록2"], "entry": {"text": "확인", "region_ref": "진입 영역", "reject_text": "돌아가기", "timeout_ms": 1000}}]}]
        right = [{"op": "IF", "text": "자두", "test": "PRESENT", "then": [{"op": "TEXT_WAIT", "text": "복숭아", "region_ref": "다른 영역", "timeout_ms": 7000}], "else": [{"op": "WATCH", "macro_ref": "정산", "region_refs": ["우선1", "우선2"], "entry": {"text": "승인", "region_ref": "상세 영역", "reject_text": "취소", "timeout_ms": 9000}}]}]
        self.assertEqual(_semantic_signature(left), _semantic_signature(right))

    def test_distinct_gesture_types_never_overwrite_an_earlier_reference(self):
        from ai_model.data import Sentence, _atomic, split_contract
        from ai_model.schema import parse_plan
        import random
        contract = split_contract()["train"]
        contract["names"] = ["동일 이름"]
        sentence = Sentence(); sentence.tokens = ["PLAN"]
        context = {"gestures": {}}
        first = _atomic(sentence, random.Random(1), contract, context, "PINCH_IN")
        sentence.say(". 이어서 ")
        second = _atomic(sentence, random.Random(1), contract, context, "PINCH_OUT")
        sentence.tokens += ["END", "EOS"]
        self.assertNotEqual(first["gesture_ref"], second["gesture_ref"])
        self.assertEqual(parse_plan(sentence.tokens, sentence.command, context)["body"], [first, second])

    def test_manifest_separates_families_names_numbers_before_generation(self):
        try:
            data = importlib.import_module("ai_model.data")
        except ModuleNotFoundError:
            self.fail("고정 분리 생성기가 아직 없습니다")
        contract = data.split_contract()
        for field in ("families", "names", "numbers", "decimal_numbers"):
            pools = [set(contract[s][field]) for s in ("train", "dev", "test")]
            for left, right in ((0, 1), (0, 2), (1, 2)):
                self.assertFalse(pools[left] & pools[right], field)

    def test_synthetic_gold_is_exactly_parsed_without_keyword_semantics(self):
        try:
            from ai_model.data import generate_split
            from ai_model.schema import parse_plan
        except ModuleNotFoundError:
            self.fail("합성 정답 생성기가 아직 없습니다")
        rows = generate_split("train", 96, seed=1729)
        self.assertEqual(len(rows), 96)
        for row in rows:
            self.assertEqual(parse_plan(row["tokens"], row["command"], row["context"]), row["plan"], row["id"])


if __name__ == "__main__":
    unittest.main()
