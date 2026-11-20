"""가역 인용·등록 동등성·전체 COPY 경계의 순수 회귀입니다."""
import copy
import inspect
import json
from pathlib import Path
import random
import tempfile
import unittest

from ai_model.tokenizer import CodepointTokenizer
from ai_model.literal_codec import LiteralSlotCodec, load_context_json
from ai_model.literal_metrics import inspect_prediction


def codec():
    characters = [chr(value) for value in range(32, 127)] + [chr(0xAC00 + value) for value in range(180)]
    return LiteralSlotCodec(characters, base_tokenizer_sha256="0" * 64)


class LiteralCodecTests(unittest.TestCase):
    def setUp(self):
        self.codec = codec()

    def test_new_format_keeps_old_codec_and_output_tokens(self):
        from ai_model.schema import TOKENS
        self.assertEqual((self.codec.base.size, self.codec.size, len(TOKENS)), (535, 540, 72))
        self.assertEqual(list(self.codec.special_ids.values()), list(range(535, 540)))
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder) / "new.json"
            self.codec.save(target)
            self.assertEqual(LiteralSlotCodec.load(target).special_ids, self.codec.special_ids)
            with self.assertRaises(ValueError):
                CodepointTokenizer.load(target)
            value = json.loads(target.read_text(encoding="utf-8"))
            value["special_tokens"].reverse()
            target.write_text(json.dumps(value), encoding="utf-8")
            with self.assertRaises(ValueError):
                LiteralSlotCodec.load(target)

    def test_literal_commands_are_opaque_and_gold_is_not_an_input(self):
        signature = inspect.signature(self.codec.encode_request)
        self.assertEqual(list(signature.parameters), ["command", "context"])
        source = '"홈으로 가고 정지 🙂  두 칸"을 입력해'
        encoded = self.codec.encode_request(source, {})
        anchor = encoded["literals"][0]["anchor"]
        self.assertEqual(self.codec.copy_text(encoded, anchor, anchor), "홈으로 가고 정지 🙂  두 칸")
        self.assertEqual(self.codec.reconstruct_command(encoded), source)
        changed = self.codec.encode_request('"아무 이름"을 입력해', {})
        self.assertEqual(encoded["ids"], changed["ids"])

    def test_repeated_value_and_membership_are_not_collapsed(self):
        source = '"가" 다음 "가" 다음 "나"'
        encoded = self.codec.encode_request(source, {"points": {"가": {}}, "macros": {"가": {}, "나": {}}})
        self.assertEqual([item["entity"] for item in encoded["literals"]], [0, 0, 1])
        self.assertEqual([item["membership"] for item in encoded["entities"]], ["100100", "000100"])
        self.assertNotEqual(encoded["ids"], self.codec.encode_request(source, {"points": {"가": {}}, "macros": {"나": {}}})["ids"])

    def test_unmentioned_groups_preserve_cross_kind_identity(self):
        first = self.codec.encode_request('"x"를 입력해', {"points": {"a": {}, "b": {}}, "macros": {"a": {}}})
        renamed = self.codec.encode_request('"🙂"를 입력해', {"macros": {"Z": {}}, "points": {"Y": {}, "Z": {}}})
        separate = self.codec.encode_request('"x"를 입력해', {"points": {"a": {}, "b": {}}, "macros": {"c": {}}})
        self.assertEqual(first["ids"], renamed["ids"])
        self.assertNotEqual(first["ids"], separate["ids"])
        self.assertEqual(first["unmentioned_groups"], [{"membership": "100000", "count": 1}, {"membership": "100100", "count": 1}])

    def test_128_injective_renames_and_context_insertion_orders_are_invariant(self):
        source = '"가"가 없으면 "나"를 입력하고 "가"를 눌러'
        context = {"planning_mode": "MACRO", "points": {"가": {}, "숨김": {}}, "macros": {"가": {}, "다른": {}}}
        baseline = self.codec.encode_request(source, context)
        for seed in range(128):
            aliases = {"가": f"🙂A{seed}", "나": f"e\u0301B{seed}", "숨김": f"H{seed}", "다른": f"Q{seed}"}
            command = source.replace('"가"', '"' + aliases["가"] + '"').replace('"나"', '"' + aliases["나"] + '"')
            changed = {"macros": {aliases[name]: value for name, value in reversed(list(context["macros"].items()))},
                       "points": {aliases[name]: value for name, value in reversed(list(context["points"].items()))}, "planning_mode": "MACRO"}
            encoded = self.codec.encode_request(command, changed)
            self.assertEqual(encoded["ids"], baseline["ids"], seed)
            self.assertEqual(encoded["start_keep"], baseline["start_keep"], seed)
            self.assertEqual(encoded["end_keep"], baseline["end_keep"], seed)

    def test_unicode_raw_escape_parity_and_no_normalization(self):
        samples = ['"🙂e\u0301é\n\u0085\\n\\u0041" 입력', '"a\\\"b" 입력', '"a\\\\" 다음 "b"']
        for source in samples:
            encoded = self.codec.encode_request(source, {})
            self.assertEqual(self.codec.reconstruct_command(encoded), source)
            for literal in encoded["literals"]:
                anchor = literal["anchor"]
                self.assertEqual(self.codec.copy_text(encoded, anchor, anchor), source[literal["start"]:literal["end"]])
        self.assertEqual(self.codec.encode_request('"é" 입력', {})["ids"], self.codec.encode_request('"e\u0301" 입력', {})["ids"])

    def test_invalid_quotes_context_modes_and_surrogates_reject(self):
        for source in ('"닫히지 않음', '"" 입력', '" \t\u0085" 입력', '“가” 입력', '"\ud800" 입력'):
            with self.subTest(source=repr(source)), self.assertRaises(ValueError):
                self.codec.encode_request(source, {})
        for context in ([], {"points": []}, {"points": {1: {}}}, {"points": {" ": {}}}, {"planning_mode": True}, {"planning_mode": "AUTO"}):
            with self.subTest(context=context), self.assertRaises(ValueError):
                self.codec.encode_request('"가" 입력', context)
        with self.assertRaises(ValueError):
            load_context_json('{"points":{"a":{},"a":{}}}')

    def test_copy_masks_have_different_utf8_boundaries_and_cover_whole_interval(self):
        source = '🙂 2 "가" 3 🚀'
        encoded = self.codec.encode_request(source, {})
        start, end = self.codec.span_to_tokens(encoded, 0, 1)
        self.assertNotEqual(start, end)
        self.assertTrue(encoded["start_keep"][start])
        self.assertFalse(encoded["end_keep"][start])
        self.assertFalse(encoded["start_keep"][end])
        self.assertTrue(encoded["end_keep"][end])
        self.assertEqual(self.codec.tokens_to_span(encoded, start, end), (0, 1))
        anchor = encoded["literals"][0]["anchor"]
        last_start, last_end = self.codec.span_to_tokens(encoded, len(source) - 1, len(source))
        for pair in ((start + 1, end), (start, end - 1), (start, last_end), (anchor, anchor + 1), (anchor + 1, anchor + 1), (last_start, len(encoded["ids"]) - 1)):
            with self.subTest(pair=pair), self.assertRaises(ValueError):
                self.codec.tokens_to_span(encoded, *pair)
        self.assertEqual(sum(self.codec.end_mask(encoded, anchor)), 1)
        self.assertTrue(self.codec.end_mask(encoded, anchor)[anchor])

    def test_teacher_and_inference_pointer_masks_are_the_same_contract(self):
        source = '🙂 12 "가" 34'
        encoded = self.codec.encode_request(source, {})
        spans = [(0, 1), (2, 4), (6, 7), (9, 11)]
        for span, masks in zip(spans, self.codec.training_masks(encoded, spans)):
            start, end = self.codec.span_to_tokens(encoded, *span)
            self.assertEqual(masks["start_keep"], encoded["start_keep"])
            self.assertEqual(masks["end_keep"], self.codec.end_mask(encoded, start))
            self.assertTrue(masks["start_keep"][start] and masks["end_keep"][end])

    def test_copy_partial_quote_bool_or_context_is_rejected(self):
        source = '"가나다" 입력'
        encoded = self.codec.encode_request(source, {"points": {"가나다": {}}})
        for pair in ((1, 2), (0, 4), (True, 4), (1, 10)):
            with self.subTest(pair=pair), self.assertRaises(ValueError):
                self.codec.span_to_tokens(encoded, *pair)
        anchor = encoded["literals"][0]["anchor"]
        for wrong in (True, False, 0.0, "0"):
            with self.subTest(wrong=wrong), self.assertRaises(ValueError):
                self.codec.tokens_to_span(encoded, wrong, anchor)
            with self.subTest(wrong_end=wrong), self.assertRaises(ValueError):
                self.codec.tokens_to_span(encoded, anchor, wrong)

    def test_old_and_new_input_limits_are_separate_and_never_truncate(self):
        with self.assertRaises(ValueError):
            self.codec.encode_request('"' + '🙂' * 300 + '" 입력', {})
        found = False
        for count in range(20, 95):
            source = ' '.join('"' + chr(0xAC00 + index) + '"' for index in range(count))
            old = self.codec.base.encode_request(source, {})
            self.assertLessEqual(len(old["ids"]), 1024)
            try:
                self.codec.encode_request(source, {})
            except ValueError as error:
                self.assertIn("새 입력", str(error))
                found = True
                break
        self.assertTrue(found)

    def test_byte_augmentation_zero_seed_roundtrip_masks_and_whole_character_budget(self):
        source = ('가각갂 ' * 140) + '"🙂e\u0301" 입력'
        baseline = self.codec.encode_request(source, {})
        rng = random.Random(13)
        state = rng.getstate()
        self.assertEqual(self.codec.encode_request_training(source, {}, byte_probability=0, rng=rng), baseline)
        self.assertEqual(rng.getstate(), state)
        one = self.codec.encode_request_training(source, {}, byte_probability=1, rng=random.Random(13))
        self.assertEqual(one, self.codec.encode_request_training(source, {}, byte_probability=1, rng=random.Random(13)))
        self.assertLessEqual(len(one["ids"]), 1024)
        self.assertEqual(self.codec.reconstruct_command(one), source)
        self.assertGreater(sum(token < 260 and token >= 132 for token in one["ids"]), 0)
        self.assertGreater(one["augmentation_stats"]["budget_skipped_codepoints"], 0)
        for literal in one["literals"]:
            self.assertEqual(self.codec.copy_text(one, literal["anchor"], literal["anchor"]), source[literal["start"]:literal["end"]])

    def test_numeral_rename_is_not_the_same_ready_ast(self):
        old = self.codec.encode_request('"2"초 기다려', {})
        new = self.codec.encode_request('"3"초 기다려', {})
        invalid = self.codec.encode_request('"사과"초 기다려', {})
        self.assertEqual(old["ids"], new["ids"])
        self.assertEqual(new["ids"], invalid["ids"])
        words = ["PLAN", "WAIT", "COPY", "SEC", "END", "EOS"]
        old_gold = {"schema": "pxe.plan.v1", "status": "ready", "body": [{"op": "WAIT", "duration_ms": 2000}]}
        new_result = inspect_prediction(self.codec, new, {}, words, [(0, 0)], gold_plan=old_gold)
        self.assertEqual(new_result["plan"]["body"][0]["duration_ms"], 3000)
        self.assertTrue(new_result["wrong_ready"])
        invalid_result = inspect_prediction(self.codec, invalid, {}, words, [(0, 0)], gold_plan=old_gold)
        self.assertTrue(invalid_result["compiler_invalid"])
        self.assertFalse(invalid_result["nn_explicit_reject"])

    def test_same_value_other_occurrence_has_raw_ast_exact_but_anchor_wrong(self):
        source = '"가" 다음 "가" 입력'
        encoded = self.codec.encode_request(source, {})
        first, second = encoded["literals"]
        gold_tokens = ["PLAN", "INPUT", {"token": "COPY", "start": first["start"], "end": first["end"]}, "END", "EOS"]
        gold_plan = {"schema": "pxe.plan.v1", "status": "ready", "body": [{"op": "INPUT", "text": "가"}]}
        result = inspect_prediction(self.codec, encoded, {}, ["PLAN", "INPUT", "COPY", "END", "EOS"], [(second["anchor"], second["anchor"])], gold_tokens=gold_tokens, gold_plan=gold_plan)
        self.assertTrue(result["structure_exact"])
        self.assertTrue(result["raw_copy_exact"])
        self.assertTrue(result["ast_exact"])
        self.assertFalse(result["copy_pair_exact"])
        self.assertFalse(result["literal_anchor_exact"])

    def test_explicit_reject_invalid_and_wrong_ready_are_distinct(self):
        encoded = self.codec.encode_request("홈으로 가지 마", {})
        gold = {"schema": "pxe.plan.v1", "status": "reject", "reason": "NEGATED"}
        explicit = inspect_prediction(self.codec, encoded, {}, ["REJECT", "NEGATED", "EOS"], [], gold_plan=gold)
        invalid = inspect_prediction(self.codec, encoded, {}, ["PLAN", "HOME"], [], gold_plan=gold)
        wrong = inspect_prediction(self.codec, encoded, {}, ["PLAN", "HOME", "END", "EOS"], [], gold_plan=gold)
        self.assertTrue(explicit["nn_explicit_reject"] and explicit["ast_exact"])
        self.assertTrue(invalid["compiler_invalid"] and not invalid["nn_explicit_reject"])
        self.assertTrue(wrong["wrong_ready"] and not wrong["compiler_invalid"])

    def test_shared_vectors_replay_the_full_serialization(self):
        vector_path = Path(__file__).resolve().parents[1] / "literal_codec_vectors.json"
        payload = json.loads(vector_path.read_text(encoding="utf-8"))
        encoder = LiteralSlotCodec(payload["characters"], base_tokenizer_sha256=payload["base_tokenizer_sha256"])
        groups = {}
        for case in payload["cases"]:
            source = "".join(chr(value) for value in case["command_codepoints"])
            with self.subTest(case=case["id"]):
                if not case["accepted"]:
                    with self.assertRaises((ValueError, UnicodeError)):
                        context = load_context_json(case["context_json"]) if "context_json" in case else case["context"]
                        encoder.encode_request(source, context)
                    continue
                encoded = encoder.encode_request(source, case["context"])
                actual = {key: encoded[key] for key in case["expected"]}
                self.assertEqual(json.loads(json.dumps(actual, ensure_ascii=True)), case["expected"])
                self.assertEqual(encoder.reconstruct_command(encoded), source)
                for item in case["valid_pairs"]:
                    self.assertEqual(list(encoder.tokens_to_span(encoded, *item["pair"])), item["cp_span"])
                    self.assertEqual(encoder.copy_text(encoded, *item["pair"]), item["raw_text"])
                for pair in case["invalid_pairs"]:
                    with self.assertRaises(ValueError):
                        encoder.tokens_to_span(encoded, *pair)
                if case.get("equivalence_group"):
                    group = case["equivalence_group"]
                    if group in groups:
                        self.assertEqual(encoded["ids"], groups[group])
                    else:
                        groups[group] = encoded["ids"]

    def test_json_pointer_pairs_and_no_literal_metric_are_unambiguous(self):
        encoded = self.codec.encode_request('"가" 입력', {})
        literal = encoded["literals"][0]
        gold = ["PLAN", "INPUT", {"token": "COPY", "start": 1, "end": 2}, "END", "EOS"]
        result = inspect_prediction(self.codec, encoded, {}, ["PLAN", "INPUT", "COPY", "END", "EOS"], [[literal["anchor"], literal["anchor"]]], gold_tokens=gold)
        self.assertTrue(result["copy_pair_exact"])
        no_copy = self.codec.encode_request("홈으로", {})
        empty = inspect_prediction(self.codec, no_copy, {}, ["PLAN", "HOME", "END", "EOS"], [], gold_tokens=["PLAN", "HOME", "END", "EOS"])
        self.assertIsNone(empty["literal_anchor_exact"])


if __name__ == "__main__":
    unittest.main()
