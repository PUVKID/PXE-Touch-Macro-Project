"""전체 인코딩 캐시 없이 같은 배치를 구성하고 GPU 예산·통계를 CPU에서 제어 검사합니다."""
import ast
from copy import deepcopy
import gc
import inspect
import json
from pathlib import Path
import random
from types import SimpleNamespace
import unittest
from unittest.mock import patch

import torch
from ai_model import train
from ai_model.batching import collate, encode_row
from ai_model.literal_codec import LiteralSlotCodec
from ai_model.tokenizer import CodepointTokenizer


class TrainingResourceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        vectors = json.loads((Path(__file__).resolve().parents[1] / "literal_codec_vectors.json").read_text(encoding="utf-8"))
        cls.codecs = [CodepointTokenizer(vectors["characters"]), LiteralSlotCodec(vectors["characters"], base_tokenizer_sha256=vectors["base_tokenizer_sha256"])]
        cls.config = SimpleNamespace(max_input=1024, max_output=512)

    def helper(self, name):
        value = getattr(train, name, None)
        self.assertTrue(callable(value), f"{name}이 아직 구현되지 않았습니다")
        return value

    def rows(self):
        rows = []
        for index, text in enumerate(("가🙂", "나", "e\u0301")):
            command = f'"{text}" 입력하고 12초 대기'
            start, number = command.index(text), command.index("12")
            rows.append({"id": str(index), "command": command, "context": {}, "tokens": ["PLAN", "INPUT", {"token": "COPY", "start": start, "end": start + len(text)}, "WAIT", {"token": "COPY", "start": number, "end": number + 2}, "SEC", "END", "EOS"]})
        return rows

    def runtime(self):
        return train.TrainingRuntime(SimpleNamespace(output="unused", wall_seconds=None, save_reserve_seconds=30, checkpoint_every=0), clock=lambda: 0)

    def same_nine(self, left, right):
        self.assertEqual(len(left), len(right), 7)
        for index, (old, new) in enumerate(zip(left, right)):
            self.assertTrue(torch.equal(old, new), f"기본 tensor{index} 불일치")
        for key in ("pointer_start_keep", "pointer_end_keep"):
            old, new = getattr(left, key), getattr(right, key)
            if old is None:
                self.assertIsNone(new)
            else:
                self.assertTrue(torch.equal(old, new), key)
        self.assertEqual(left.input_contract, right.input_contract)

    def test_legacy_and_literal_batch_nine_items_order_and_rng_are_identical(self):
        validate = self.helper("validate_training_rows")
        selected_rows = self.helper("encode_selected_rows")
        rows = self.rows()
        for codec in self.codecs:
            with self.subTest(codec=codec.size):
                cached = [encode_row(row, codec) for row in rows]
                random.seed(1730)
                torch.manual_seed(1730)
                augmentation_rng = random.Random(1730001)
                before = (random.getstate(), torch.get_rng_state().clone(), augmentation_rng.getstate())
                self.assertIsNone(validate(rows, codec, self.config, self.runtime()))
                self.assertEqual(random.getstate(), before[0])
                self.assertTrue(torch.equal(torch.get_rng_state(), before[1]))
                self.assertEqual(augmentation_rng.getstate(), before[2])
                for selected in ([2, 0, 1, 2], [1]):
                    new = selected_rows(rows, selected, codec, self.config, byte_probability=0, augmentation_rng=augmentation_rng, augmentation_stats={})
                    self.same_nine(collate([cached[i] for i in selected], "cpu"), collate(new, "cpu"))
                self.assertEqual(random.getstate(), before[0])
                self.assertTrue(torch.equal(torch.get_rng_state(), before[1]))
                self.assertEqual(augmentation_rng.getstate(), before[2])

    def test_augmentation_uses_same_separate_rng_stats_and_nine_items(self):
        selected_rows = self.helper("encode_selected_rows")
        rows, selected = self.rows(), [2, 0, 1, 2]
        for codec in self.codecs:
            with self.subTest(codec=codec.size):
                old_rng, new_rng = random.Random(1730001), random.Random(1730001)
                old_stats, new_stats = {}, {}
                before_python, before_torch = random.getstate(), torch.get_rng_state().clone()
                old = [encode_row(rows[i], codec, 1024, 512, byte_probability=0.3, augmentation_rng=old_rng, augmentation_stats=old_stats) for i in selected]
                new = selected_rows(rows, selected, codec, self.config, byte_probability=0.3, augmentation_rng=new_rng, augmentation_stats=new_stats)
                self.same_nine(collate(old, "cpu"), collate(new, "cpu"))
                self.assertEqual(old_rng.getstate(), new_rng.getstate())
                self.assertEqual(old_stats, new_stats)
                self.assertEqual(random.getstate(), before_python)
                self.assertTrue(torch.equal(torch.get_rng_state(), before_torch))

    def test_validation_rejects_bad_unselected_last_row(self):
        validate = self.helper("validate_training_rows")
        rows = self.rows()
        bad = deepcopy(rows[0])
        bad["command"] = "a" * 1100
        rows.append(bad)
        for codec in self.codecs:
            with self.subTest(codec=codec.size), patch.object(train, "encode_row", wraps=encode_row) as encode:
                with self.assertRaises(ValueError):
                    validate(rows, codec, self.config, self.runtime())
                self.assertEqual(encode.call_count, 4)

    def test_validation_does_not_retain_encoded_rows(self):
        validate = self.helper("validate_training_rows")
        active, peak, calls = 0, 0, 0
        class Tracked:
            def __init__(self):
                nonlocal active, peak, calls
                active += 1
                calls += 1
                peak = max(active, peak)
            def __del__(self):
                nonlocal active
                active -= 1
        with patch.object(train, "encode_row", side_effect=lambda *args, **kwargs: Tracked()):
            self.assertIsNone(validate([{} for _ in range(1000)], object(), self.config, self.runtime()))
        gc.collect()
        self.assertEqual(calls, 1000)
        self.assertLessEqual(peak, 1)
        self.assertEqual(active, 0)

    def test_run_uses_validation_and_selected_encoding_without_cache(self):
        tree = ast.parse(inspect.getsource(train._run))
        names = {node.id for node in ast.walk(tree) if isinstance(node, ast.Name)}
        self.assertNotIn("encoded", names, "전체 인코딩 캐시가 실제 학습 경로에 남았습니다")
        self.assertIn("validate_training_rows", names)
        self.assertIn("encode_selected_rows", names)

    def test_cpu_resource_path_never_calls_cuda(self):
        setup = self.helper("configure_cuda_resources")
        sample = self.helper("cuda_memory_record")
        with patch.object(torch, "cuda") as cuda:
            value = setup("cpu")
            self.assertFalse(value["enabled"])
            self.assertIsNone(value["caching_allocator_limit_bytes"])
            self.assertIsNone(sample("cpu", 1))
            self.assertEqual(cuda.mock_calls, [])

    def test_cuda_budget_is_four_gib_and_metadata_is_actual_fraction(self):
        setup = self.helper("configure_cuda_resources")
        with patch.object(torch, "cuda") as cuda:
            cuda.is_available.return_value = True
            cuda.get_device_properties.return_value = SimpleNamespace(total_memory=16 * 1024 ** 3)
            cuda.memory.get_allocator_backend.return_value = "native"
            value = setup("cuda")
            cuda.set_per_process_memory_fraction.assert_called_once_with(0.25, device=0)
            cuda.reset_peak_memory_stats.assert_called_once_with()
            self.assertEqual(value["caching_allocator_limit_bytes"], 4 * 1024 ** 3)
            self.assertEqual(value["visible_total_bytes"], 16 * 1024 ** 3)
            self.assertEqual(value["fraction"], 0.25)
            self.assertFalse(value["whole_process_or_wddm_hard_limit"])

    def test_telemetry_only_on_step_one_and_multiples_of_fifty(self):
        sample = self.helper("cuda_memory_record")
        with patch.object(torch, "cuda") as cuda:
            for step in (2, 49, 51, 101):
                self.assertIsNone(sample("cuda", step))
            self.assertEqual(cuda.mock_calls, [])
            cuda.memory.get_allocator_backend.return_value = "native"
            cuda.memory_stats.return_value = {"num_alloc_retries": 2, "num_ooms": 0, "inactive_split_bytes.all.current": 17, "inactive_split_bytes.all.peak": 19}
            cuda.memory_allocated.return_value, cuda.memory_reserved.return_value = 23, 29
            cuda.max_memory_allocated.return_value, cuda.max_memory_reserved.return_value = 31, 37
            for step in (1, 50, 100):
                value = sample("cuda", step)
                self.assertEqual(value["allocated_bytes"], 23)
                self.assertEqual(value["reserved_bytes"], 29)
                self.assertEqual(value["max_allocated_bytes"], 31)
                self.assertEqual(value["max_reserved_bytes"], 37)
                self.assertEqual(value["num_alloc_retries"], 2)
                self.assertEqual(value["num_ooms"], 0)
                self.assertEqual(value["inactive_split_bytes"], 17)
            self.assertEqual(cuda.memory_stats.call_count, 3)

    def test_backend_inapplicable_or_missing_stats_are_none(self):
        sample = self.helper("cuda_memory_record")
        with patch.object(torch, "cuda") as cuda:
            cuda.memory_stats.return_value = {}
            for method in ("memory_allocated", "memory_reserved", "max_memory_allocated", "max_memory_reserved"):
                getattr(cuda, method).return_value = 0
            for backend in ("native", "cudaMallocAsync", "other"):
                cuda.memory.get_allocator_backend.return_value = backend
                value = sample("cuda", 1)
                self.assertEqual(value["allocator_backend"], backend)
                for key in ("inactive_split_bytes", "inactive_split_bytes_peak", "num_alloc_retries", "num_ooms"):
                    self.assertIsNone(value[key])


if __name__ == "__main__":
    unittest.main()
