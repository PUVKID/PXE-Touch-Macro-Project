"""훈련 전용 새 이름·선두 문구 합성의 참조·수치 경계를 검사합니다."""
import copy
import random
import unittest


class DiversifyTests(unittest.TestCase):
    def module(self):
        from ai_model import diversify
        return diversify

    def row(self):
        from ai_model.schema import parse_plan
        command = '다음대로 해 주세요. "사과" 포인트를 눌러. "홈으로 가고 정지😀\n"라고 입력해. 15초 기다려'
        point_start = command.index("사과"); text_start = command.index("홈으로 가고")
        text_end = command.index('"', text_start); number_start = command.index("15")
        tokens = ["PLAN", "TAP", {"token":"COPY","start":point_start,"end":point_start+2}, "INPUT", {"token":"COPY","start":text_start,"end":text_end}, "WAIT", {"token":"COPY","start":number_start,"end":number_start+2}, "SEC", "END", "EOS"]
        context = {"points":{"사과":{"x":25,"y":70}},"planning_mode":"MACRO"}
        return {"id":"train-diversify-probe","split":"train","kind":"sequence","family":"존댓말지시","command":command,"tokens":tokens,"context":context,"plan":parse_plan(tokens,command,context)}

    def test_full_quoted_rename_updates_gold_context_and_copy_without_touching_numbers(self):
        module = self.module(); row = self.row(); before = copy.deepcopy(row)
        literal = row["plan"]["body"][1]["text"]
        changed = module.rename_quoted(row, {"사과":"새 위치 🐾",literal:"새 입력 문구"}, neutral_prefix="요청: ")
        self.assertEqual(changed["plan"]["body"][0]["point_ref"], "새 위치 🐾")
        self.assertEqual(changed["plan"]["body"][1]["text"], "새 입력 문구")
        self.assertEqual(changed["plan"]["body"][2]["duration_ms"], 15000)
        self.assertEqual(changed["context"]["points"]["새 위치 🐾"], {"x":25,"y":70})
        self.assertIn("15초 기다려", changed["command"])
        self.assertEqual(row, before)
        self.assertTrue(changed["command"].startswith("요청: "))

    def test_alias_collision_partial_keys_and_nontraining_rows_reject(self):
        module = self.module(); row = self.row(); literal = row["plan"]["body"][1]["text"]
        for aliases in ({"사과":"같음",literal:"같음"},{"사":"새이름"},{"사과":"새이름"}):
            with self.assertRaises(ValueError): module.rename_quoted(row,aliases)
        row["split"] = "dev"
        with self.assertRaises(ValueError): module.rename_quoted(row,{"사과":"새이름",literal:"다른이름"})

    def test_numeric_literal_is_preserved_and_snapshot_payload_is_not_renamed(self):
        module = self.module()
        from ai_model.schema import parse_plan
        command = '"코드12"라고 입력해'; tokens=["PLAN","INPUT",{"token":"COPY","start":1,"end":5},"END","EOS"]
        row = {"id":"train-number-literal","split":"train","command":command,"context":{"planning_mode":"MACRO"},"tokens":tokens,"plan":parse_plan(tokens,command,{"planning_mode":"MACRO"})}
        self.assertEqual(module.rename_quoted(row, {})["plan"], row["plan"])
        with self.assertRaises(ValueError): module.rename_quoted(row,{"코드12":"다른34"})

    def test_seed_repeat_modes_lengths_and_global_rng_independence(self):
        module = self.module(); row = self.row(); state = random.getstate()
        for mode in module.ALPHABETS:
            for length in (1,4,24):
                one = module.sample_aliases(row,random.Random(12),mode=mode,length=length)
                two = module.sample_aliases(row,random.Random(12),mode=mode,length=length)
                self.assertEqual(one,two)
                self.assertTrue(all(len(value)==length for value in one.values()))
        self.assertEqual(random.getstate(),state)

    def test_generated_train_gold_and_registered_payloads_remain_consistent(self):
        module = self.module()
        from ai_model.data import generate_split
        from ai_model.schema import parse_plan
        for row in generate_split("train",32,seed=1729):
            aliases=module.sample_aliases(row,random.Random(row["id"]),mode="mixed",length=8)
            changed=module.rename_quoted(row,aliases,neutral_prefix=module.PREFIXES[2])
            self.assertEqual(parse_plan(changed["tokens"],changed["command"],changed["context"]),changed["plan"])
            for kind in module.REGISTERED_KINDS:
                if kind in row["context"]:
                    for name,payload in row["context"][kind].items(): self.assertEqual(changed["context"][kind][aliases.get(name,name)],payload)

    def test_prefix_diversity_does_not_edit_an_input_literal_containing_prefix_words(self):
        module = self.module(); row=self.row(); literal=row["plan"]["body"][1]["text"]
        for prefix in module.PREFIXES:
            result=module.rename_quoted(row,{"사과":"라벨",literal:"다음대로 해 주세요. 라벨"},neutral_prefix=prefix)
            self.assertEqual(result["plan"]["body"][1]["text"],"다음대로 해 주세요. 라벨")
            self.assertTrue(result["command"].startswith(prefix))


if __name__=="__main__": unittest.main()
