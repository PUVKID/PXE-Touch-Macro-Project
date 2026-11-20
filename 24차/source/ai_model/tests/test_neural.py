"""전용 PyTorch 환경에서 무작위 모델의 실제 미분·입력 마스크를 확인합니다."""
import importlib
import unittest


class NeuralTests(unittest.TestCase):
    def test_partial_epoch_is_distinct_from_a_fully_completed_epoch(self):
        self.model_module()
        from ai_model import train
        partial = train.epoch_progress(10, 36000, 60000, 9)
        self.assertEqual(partial["epochs_started"], 10)
        self.assertEqual(partial["full_epochs_completed"], 9)
        self.assertFalse(partial["last_epoch_complete"])
        complete = train.epoch_progress(10, 60000, 60000, 9)
        self.assertEqual(complete["full_epochs_completed"], 10)
        self.assertTrue(complete["last_epoch_complete"])

    def test_training_byte_augmentation_realigns_gold_and_trains_byte_embeddings(self):
        self.model_module()
        import random
        import torch
        from ai_model.batching import encode_row, collate, loss_for_batch
        from ai_model.model import ModelConfig, PlanTransformer
        from ai_model.schema import TOKENS
        from ai_model.tokenizer import CodepointTokenizer
        command = '입력 "뒤로\n😀끝"'
        start, end = command.index("뒤로"), command.index('"', command.index("뒤로"))
        context = {"points": {"문맥😀지점": {}}, "planning_mode": "MACRO"}
        row = {"command": command, "context": context, "tokens": ["PLAN", "INPUT", {"start": start, "end": end}, "END", "EOS"]}
        tokenizer = CodepointTokenizer.fit([command, "문맥😀지점"])
        base = encode_row(row, tokenizer)
        zero = encode_row(row, tokenizer, byte_probability=0, augmentation_rng=random.Random(31))
        augmented = encode_row(row, tokenizer, byte_probability=1, augmentation_rng=random.Random(31))
        self.assertEqual(base, zero)
        self.assertEqual(base["target"], augmented["target"])
        self.assertNotEqual(base["starts"], augmented["starts"])
        encoded = tokenizer.encode_request_training(command, context, byte_probability=1, rng=random.Random(31))
        self.assertEqual(tokenizer.tokens_to_span(encoded, augmented["starts"][2], augmented["ends"][2]), (start, end))
        batch = collate([base, augmented], "cpu")
        self.assertEqual(tuple(batch[0].shape), (2, len(augmented["source"])))
        self.assertFalse(batch[3][1, augmented["command_tokens"]:].any())
        torch.manual_seed(23)
        model = PlanTransformer(ModelConfig(input_vocab=tokenizer.size, output_vocab=len(TOKENS), dim=32, heads=4, layers=1, ff=64, dropout=0))
        loss, _ = loss_for_batch(model, batch)
        loss.backward()
        observed = sorted({token for token in augmented["source"] if tokenizer.BYTE_BASE+128 <= token < tokenizer.CHAR_BASE})
        self.assertTrue(observed)
        self.assertGreater(model.source_embedding.weight.grad[observed].abs().sum().item(), 0)

    def test_training_initial_weights_bind_step_manifest_tokenizer_and_source(self):
        self.model_module()
        from ai_model import train
        self.assertTrue(callable(getattr(train, "load_fixed_initial", None)), "승인된 초기 가중치의 결합 검사가 아직 없습니다")
        import tempfile
        from pathlib import Path
        import torch
        from ai_model.model import ModelConfig, PlanTransformer
        from ai_model.schema import TOKENS
        config = ModelConfig(input_vocab=300, output_vocab=len(TOKENS), dim=32, heads=4, layers=1, ff=64, dropout=0.0)
        model = PlanTransformer(config)
        payload = {"format": "pxe-plan-transformer-v1", "output_tokens": list(TOKENS), "config": config.to_dict(), "state_dict": model.state_dict(), "step": 0, "metadata": {"mode": "init", "manifest_sha256": "manifest", "tokenizer_sha256": "tokenizer", "source_hashes": {"unit.py": "hash"}}}
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder)/"initial.pt"; torch.save(payload, path)
            loaded = train.load_fixed_initial(path, config, "manifest", "tokenizer", {"unit.py": "hash"}, "cpu")
            self.assertTrue(torch.equal(loaded.source_embedding.weight, model.source_embedding.weight))
            payload["step"] = 1; torch.save(payload, path)
            with self.assertRaises(ValueError): train.load_fixed_initial(path, config, "manifest", "tokenizer", {"unit.py": "hash"}, "cpu")

    def test_checkpoint_refuses_a_different_output_token_order(self):
        module = self.model_module()
        import tempfile
        from pathlib import Path
        import torch
        from ai_model.evaluate import load_model
        from ai_model.schema import TOKENS
        config = module.ModelConfig(input_vocab=300, output_vocab=len(TOKENS), dim=32, heads=4, layers=1, ff=64, dropout=0.0)
        model = module.PlanTransformer(config)
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder)/"sample.pt"
            torch.save({"format": "pxe-plan-transformer-v1", "output_tokens": list(reversed(TOKENS)), "config": config.to_dict(), "state_dict": model.state_dict()}, path)
            with self.assertRaises(ValueError):
                load_model(path)
            torch.save({"format": "pxe-plan-transformer-v1", "output_tokens": list(TOKENS), "config": config.to_dict(), "state_dict": model.state_dict()}, path)
            loaded, _ = load_model(path)
            self.assertEqual(loaded.config.output_vocab, len(TOKENS))

    def model_module(self):
        try:
            return importlib.import_module("ai_model.model")
        except ModuleNotFoundError:
            self.fail("자체 Transformer 구현이 아직 없습니다")

    def test_forward_trains_token_and_both_learned_pointer_heads(self):
        module = self.model_module()
        import torch
        torch.manual_seed(123)
        model = module.PlanTransformer(module.ModelConfig(input_vocab=300, output_vocab=70, dim=32, heads=4, layers=1, ff=64, dropout=0.0))
        source = torch.tensor([[260, 261, 262, 0]])
        target = torch.tensor([[1, 3, 5]])
        before = model.start_query.weight.detach().clone()
        logits, starts, ends = model(source, target, source.ne(0), source.ne(0))
        self.assertEqual(tuple(logits.shape), (1, 3, 70))
        self.assertEqual(tuple(starts.shape), (1, 3, 4))
        loss = -logits.log_softmax(-1)[0, 0, 3]-starts.log_softmax(-1)[0, 1, 0]-ends.log_softmax(-1)[0, 1, 2]
        loss.backward()
        self.assertGreater(model.start_query.weight.grad.abs().sum().item(), 0)
        self.assertGreater(model.end_query.weight.grad.abs().sum().item(), 0)
        optimizer = torch.optim.AdamW(model.parameters(), lr=0.001)
        optimizer.step()
        self.assertFalse(torch.equal(before, model.start_query.weight))

    def test_padding_and_context_are_not_copy_candidates(self):
        module = self.model_module()
        import torch
        model = module.PlanTransformer(module.ModelConfig(input_vocab=300, output_vocab=70, dim=32, heads=4, layers=1, ff=64, dropout=0.0))
        source = torch.tensor([[260, 261, 3, 262, 0]])
        target = torch.tensor([[1]])
        _, starts, ends = model(source, target, source.ne(0), torch.tensor([[True, True, False, False, False]]))
        self.assertLess(starts[0, 0, 3].item(), -1e20)
        self.assertLess(ends[0, 0, 4].item(), -1e20)


if __name__ == "__main__":
    unittest.main()
