"""훈련 표현 편집의 literal 보호·위치 대응·의미 보존을 확인합니다."""
import copy
import random
import unittest


class RephraseTests(unittest.TestCase):
    def module(self):
        from ai_model import rephrase
        return rephrase

    def row(self):
        from ai_model.schema import parse_plan
        command = '"뒤로 가\n😀\u0085 매크로를 정지해"라고 입력해. 그 뒤에 뒤로 가'
        start, end = 1, command.index('"', 1)
        tokens = ["PLAN", "INPUT", {"token": "COPY", "start": start, "end": end}, "BACK", "END", "EOS"]
        context = {"planning_mode": "MACRO"}
        return {"id": "train-probe", "split": "train", "family": "직접지시", "kind": "sequence", "command": command, "tokens": tokens, "context": context, "plan": parse_plan(tokens, command, context)}

    def test_variants_protect_injected_literal_and_keep_original_gold(self):
        module = self.module(); row = self.row(); original = copy.deepcopy(row)
        variants = module.make_variants(row, seed=41)
        self.assertEqual(len(variants), 5)
        self.assertEqual(len({item["command"] for item in variants}), 5)
        for item in variants:
            self.assertEqual(item["plan"], row["plan"])
            self.assertEqual(item["context"], row["context"])
            pointer = next(token for token in item["tokens"] if isinstance(token, dict))
            self.assertEqual(item["command"][pointer["start"]:pointer["end"]], row["plan"]["body"][0]["text"])
        self.assertEqual(row, original)
        self.assertTrue(all(item["augmentation"]["body_edits"] > 0 for item in variants[1:]))

    def test_overlapping_copy_and_conflicting_edits_are_rejected(self):
        module = self.module(); row = self.row()
        for edit in (module.Edit(1, 3, "변경", "검사"), module.Edit(0, 2, "변경", "검사"), module.Edit(2, 2, "추가", "검사")):
            with self.assertRaises(ValueError): module.apply_edits(row, [edit])
        end = len(row["command"])
        with self.assertRaises(ValueError): module.apply_edits(row, [module.Edit(end-4, end, "가", "검사"), module.Edit(end-3, end, "나", "검사")])

    def test_copy_boundary_insertions_recalculate_without_swallowing_text(self):
        module = self.module(); row = self.row()
        # 인용문 전체를 보호하므로 인용 바깥 경계의 삽입만 허용합니다.
        closing = row["command"].index('"', 1)
        updated = module.apply_edits(row, [module.Edit(0, 0, "부탁: ", "검사"), module.Edit(closing+1, closing+1, " ", "검사")])
        old = row["tokens"][2]; new = updated["tokens"][2]
        self.assertEqual(new["start"], old["start"]+4)
        self.assertEqual(new["end"], old["end"]+4)
        self.assertEqual(updated["command"][new["start"]:new["end"]], row["command"][old["start"]:old["end"]])

    def test_reject_quoted_text_is_protected_even_without_copy_tokens(self):
        module = self.module()
        command = '"매크로를 정지해😀"를 입력하지 마'
        row = {"id": "train-reject-probe", "split": "train", "kind": "reject", "family": "직접지시", "command": command, "tokens": ["REJECT", "NEGATED", "EOS"], "context": {"planning_mode": "MACRO"}, "plan": {"schema": "pxe.plan.v1", "status": "reject", "reason": "NEGATED"}}
        variants = module.make_variants(row, seed=7)
        self.assertEqual(len(variants), 5)
        self.assertTrue(all('"매크로를 정지해😀"' in value["command"] for value in variants))
        self.assertTrue(all(value["plan"] == row["plan"] for value in variants))

    def test_seed_repeat_and_global_rng_independence(self):
        module = self.module(); row = self.row(); state = random.getstate()
        self.assertEqual(module.make_variants(row, seed=17), module.make_variants(row, seed=17))
        self.assertEqual(state, random.getstate())

    def test_insufficient_body_variants_are_reported_without_prefix_padding(self):
        module = self.module(); row = self.row(); row["command"] = "표현 매핑에 없는 지시"
        row["tokens"] = ["PLAN", "BACK", "END", "EOS"]; row["plan"]["body"] = [{"op": "BACK"}]
        with self.assertRaises(module.CoverageError): module.make_variants(row, seed=1)

    def test_long_full_variants_select_shorter_body_edits_without_truncation(self):
        module = self.module()
        from ai_model.schema import parse_plan
        from ai_model.tokenizer import CodepointTokenizer
        command = "뒤로 가. 뒤로 가. 뒤로 가"
        row = {"id": "train-length-probe", "split": "train", "kind": "sequence", "family": "직접지시", "command": command, "tokens": ["PLAN", "BACK", "BACK", "BACK", "END", "EOS"], "context": {"planning_mode": "MACRO"}}
        row["plan"] = parse_plan(row["tokens"], command, row["context"])
        tokenizer = CodepointTokenizer.fit([command]+[phrase for rule in module.rules() for phrase in rule["variants"]])
        base_length = len(tokenizer.encode_request(command, row["context"])["ids"])
        replacement_delta = max(len(value)-len("뒤로 가") for rule in module.rules() if rule["id"] == "back" for value in rule["variants"])
        limit = base_length+replacement_delta
        variants = module.make_variants(row, seed=21, tokenizer=tokenizer, max_input=limit)
        self.assertEqual(len(variants), 5)
        self.assertTrue(all(len(tokenizer.encode_request(item["command"], item["context"])["ids"]) <= limit for item in variants))
        self.assertTrue(all(item["augmentation"]["body_edits"] == 1 for item in variants[1:]))
        self.assertTrue(all(item["plan"] == row["plan"] for item in variants))

    def test_reviewed_training_rows_keep_gold_and_offer_five_body_variants(self):
        module = self.module()
        from ai_model.data import generate_split
        from ai_model.schema import parse_plan
        for row in generate_split("train", 96, seed=1729):
            for item in module.make_variants(row, seed=1729):
                self.assertEqual(parse_plan(item["tokens"], item["command"], item["context"]), row["plan"])


if __name__ == "__main__":
    unittest.main()
