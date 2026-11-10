"""540 codec의 실제 CPU NN 연결과 typed 입출력 결합을 검사합니다. v9 optimizer step은 없습니다."""
from copy import deepcopy
from pathlib import Path
import json
import random
import tempfile
import unittest
from unittest.mock import patch

import torch
from ai_model.batching import collate, encode_row, loss_for_batch, pointer_logits_for_batch
from ai_model.codec_contract import bind_model_codec, input_contract, load_codec, validate_training_approval
from ai_model.evaluate import load_model, predict
from ai_model.literal_codec import LiteralSlotCodec
from ai_model.model import ModelConfig, PlanTransformer
from ai_model.schema import TOKENS, TOKEN_TO_ID
from ai_model.train import checkpoint, load_fixed_initial


class LiteralNeuralTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        vectors = json.loads((Path(__file__).resolve().parents[1] / "literal_codec_vectors.json").read_text(encoding="utf-8"))
        cls.codec = LiteralSlotCodec(vectors["characters"], base_tokenizer_sha256=vectors["base_tokenizer_sha256"])
        torch.manual_seed(1730)
        cls.model = PlanTransformer(ModelConfig(input_vocab=540, output_vocab=72))
        bind_model_codec(cls.model, cls.codec)
        cls.model.eval()
        unknown = next(chr(value) for value in range(0x1F9E0, 0x1FA00) if chr(value) not in cls.codec.base.char_to_id)
        command = unknown + ' 12 "가" 34'
        at = command.index("가")
        cls.row = {"command": command, "context": {}, "tokens": ["PLAN", "INPUT", {"token": "COPY", "start": at, "end": at + 1}, "WAIT", {"token": "COPY", "start": 2, "end": 4}, "SEC", "END", "EOS"]}
        cls.fingerprint = input_contract(cls.codec)

    def metadata(self, mode="probe_cpu"):
        return {"mode": mode, "seed": 1730, "manifest_sha256": "1" * 64, "tokenizer_sha256": "2" * 64,
                "source_hashes": {"fixture.py": "3" * 64}, "input_contract": self.fingerprint}

    def test_actual_parameter_count_shapes_and_finite_backward(self):
        self.assertEqual(sum(value.numel() for value in self.model.parameters()), 2651976)
        base = encode_row(self.row, self.codec)
        augmented = encode_row(self.row, self.codec, byte_probability=1, augmentation_rng=random.Random(19))
        batch = collate([base, augmented], "cpu")
        self.model.zero_grad(set_to_none=True)
        logits, starts, ends = pointer_logits_for_batch(self.model, batch)
        self.assertEqual(tuple(logits.shape[:2]), tuple(batch[4].shape))
        self.assertEqual(logits.shape[-1], 72)
        self.assertEqual(tuple(starts.shape), (2, len(base["target"]), batch[0].shape[1]))
        self.assertEqual(ends.shape, starts.shape)
        loss, components = loss_for_batch(self.model, batch)
        self.assertTrue(torch.isfinite(loss))
        self.assertTrue(all(torch.isfinite(value) for value in components))
        loss.backward()
        self.assertGreater(self.model.start_query.weight.grad.abs().sum().item(), 0)
        self.assertGreater(self.model.end_query.weight.grad.abs().sum().item(), 0)
        self.assertGreater(self.model.output.weight.grad.abs().sum().item(), 0)
        self.assertTrue(all(torch.isfinite(value.grad).all() for value in self.model.parameters() if value.grad is not None))

    def test_teacher_end_mask_is_conditioned_on_gold_start_and_full_interval(self):
        encoded = self.codec.encode_request(self.row["command"], {})
        row = encode_row(self.row, self.codec)
        batch = collate([row], "cpu")
        for index, start in enumerate(row["starts"]):
            if start == -100:
                continue
            expected = self.codec.end_mask(encoded, start)
            self.assertEqual(batch.pointer_end_keep[0, index, :len(expected)].tolist(), expected)
            self.assertEqual(batch.pointer_start_keep[0, :len(expected)].tolist(), encoded["start_keep"])
        first_raw, last_raw = self.codec.span_to_tokens(encoded, 0, 1)
        self.assertLess(first_raw, last_raw)
        self.assertTrue(batch.pointer_start_keep[0, first_raw])
        self.assertFalse(batch.pointer_start_keep[0, last_raw])
        logits, starts, ends = pointer_logits_for_batch(self.model, batch)
        self.assertTrue((starts[0, :, ~batch.pointer_start_keep[0]] < -1e20).all())
        copy_position = row["target"].index(TOKEN_TO_ID["COPY"])
        self.assertTrue((ends[0, copy_position, ~batch.pointer_end_keep[0, copy_position]] < -1e20).all())

    def test_greedy_uses_predicted_anchor_and_restores_original_text(self):
        command = '"가" 다음 "나🙂" 입력'
        encoded = self.codec.encode_request(command, {})
        second = encoded["literals"][1]["anchor"]
        words = ["PLAN", "INPUT", "COPY", "END", "EOS"]
        index = [0]
        def controlled_logits(memory, prefix, source_keep, copy_keep):
            # 생성 배관의 제어 fixture이며 학습한 의미 예측으로 집계하지 않습니다.
            current = words[index[0]]
            index[0] += 1
            logits = torch.full((1, prefix.shape[1], 72), -30.0)
            logits[0, -1, TOKEN_TO_ID[current]] = 30.0
            starts = torch.zeros((1, prefix.shape[1], memory.shape[1]))
            ends = torch.zeros_like(starts)
            starts[0, -1, second] = 30
            ends[0, -1, -1] = 100
            return logits, starts, ends
        with patch.object(self.model, "decode", side_effect=controlled_logits):
            result = predict(self.model, self.codec, command, {}, max_output=10)
        self.assertEqual(result["copy_pairs"], [(second, second)])
        self.assertEqual(result["plan"]["body"], [{"op": "INPUT", "text": "나🙂"}])
        self.assertFalse(result["compiler_invalid"])

    def test_rename_is_identical_actual_nn_source_without_gold_features(self):
        left = {"command": '"가" 입력', "context": {}, "tokens": ["PLAN", "INPUT", {"start": 1, "end": 2}, "END", "EOS"]}
        right = {"command": '"e\u0301🙂" 입력', "context": {}, "tokens": ["PLAN", "INPUT", {"start": 1, "end": 4}, "END", "EOS"]}
        a, b = encode_row(left, self.codec), encode_row(right, self.codec)
        self.assertEqual(a["source"], b["source"])
        self.assertNotIn("source_raw", a)
        self.assertNotIn("original_input_tokens", a)
        self.assertNotIn("literals", a)
        batch = collate([a, b], "cpu")
        with torch.no_grad():
            memory = self.model.encode(batch[0], batch[2])
        self.assertTrue(torch.equal(memory[0], memory[1]))

    def test_checkpoint_fingerprint_roundtrip_and_mixed_format_reject(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "probe.pt"
            checkpoint(path, self.model, self.metadata(), 0)
            loaded, payload = load_model(path, "cpu", tokenizer=self.codec)
            self.assertEqual(payload["format"], "pxe-plan-transformer-v2")
            self.assertEqual(payload["step"], 0)
            self.assertTrue(torch.equal(loaded.source_embedding.weight, self.model.source_embedding.weight))
            with self.assertRaises(ValueError):
                load_model(path, "cpu", tokenizer=self.codec.base)
            with self.assertRaises(ValueError):
                load_model(path, "cpu")
            changed = LiteralSlotCodec(list(reversed(self.codec.base.characters)), base_tokenizer_sha256=self.codec.base_tokenizer_sha256)
            with self.assertRaises(ValueError):
                load_model(path, "cpu", tokenizer=changed)
            for field in ("input_contract", "metadata", "config"):
                bad = deepcopy(payload)
                if field == "input_contract":
                    bad[field]["characters_sha256"] = "4" * 64
                elif field == "metadata":
                    bad[field]["input_contract"]["special_ids"]["LIT"] = 536
                else:
                    bad[field]["input_vocab"] = 535
                torch.save(bad, path)
                with self.subTest(field=field), self.assertRaises(ValueError):
                    load_model(path, "cpu", tokenizer=self.codec)

    def test_initial_and_exact_four_field_approval_binding(self):
        metadata = self.metadata(mode="init")
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "temporary-zero-step.pt"
            digest = checkpoint(path, self.model, metadata, 0)
            loaded = load_fixed_initial(path, self.model.config, "1" * 64, "2" * 64, {"fixture.py": "3" * 64}, "cpu", tokenizer=self.codec)
            self.assertEqual(loaded.config.input_vocab, 540)
            with self.assertRaises(ValueError):
                load_fixed_initial(path, self.model.config, "1" * 64, "2" * 64, {"fixture.py": "4" * 64}, "cpu", tokenizer=self.codec)
            approval = {"training_allowed": True, "dataset_manifest_sha256": "1" * 64, "source_hashes": {"fixture.py": "3" * 64}, "initial_checkpoint_sha256": digest}
            validate_training_approval(approval, "1" * 64, {"fixture.py": "3" * 64}, digest)
            for field, value in (("training_allowed", False), ("dataset_manifest_sha256", "9" * 64), ("source_hashes", {}), ("initial_checkpoint_sha256", "0" * 64), ("extra", True)):
                bad = {**approval, field: value}
                with self.subTest(field=field), self.assertRaises(ValueError):
                    validate_training_approval(bad, "1" * 64, {"fixture.py": "3" * 64}, digest)

    def test_codec_loader_and_mixed_batch_rejection(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "codec.json"
            self.codec.save(path)
            self.assertEqual(load_codec(path).size, 540)
        new = encode_row(self.row, self.codec)
        legacy = encode_row(self.row, self.codec.base)
        with self.assertRaises(ValueError):
            collate([new, legacy], "cpu")
        with self.assertRaises(ValueError):
            predict(self.model, self.codec.base, self.row["command"], {}, max_output=3)

    def test_actual_random_greedy_is_bounded_not_a_quality_claim(self):
        result = predict(self.model, self.codec, self.row["command"], {}, max_output=3)
        self.assertLessEqual(len(result["model_tokens"]), 3)
        self.assertIn("compiler_invalid", result)
        self.assertIn("nn_explicit_reject", result)


if __name__ == "__main__":
    unittest.main()
