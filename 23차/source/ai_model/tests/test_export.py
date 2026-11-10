"""실제 ONNX Runtime의 동적 길이 FP32 정합을 확인합니다."""
import importlib
import tempfile
import unittest


class ExportTests(unittest.TestCase):
    def test_literal_checkpoint_requires_real_tokenizer_and_export_binding(self):
        from ai_model import export as module
        self.assertTrue(callable(getattr(module, "load_export_model", None)), "export에 실제 codec 결합이 아직 없습니다")
        import json
        from pathlib import Path
        import torch
        from ai_model.codec_contract import bind_model_codec, input_contract
        from ai_model.literal_codec import LiteralSlotCodec
        from ai_model.model import ModelConfig, PlanTransformer
        from ai_model.train import checkpoint
        from ai_model.schema import TOKENS
        vectors = json.loads((Path(__file__).resolve().parents[1] / "literal_codec_vectors.json").read_text(encoding="utf-8"))
        codec = LiteralSlotCodec(vectors["characters"], base_tokenizer_sha256=vectors["base_tokenizer_sha256"])
        with tempfile.TemporaryDirectory() as directory:
            folder = Path(directory)
            tokenizer = folder / "tokenizer.json"
            codec.save(tokenizer)
            torch.manual_seed(91)
            model = bind_model_codec(PlanTransformer(ModelConfig(input_vocab=540, output_vocab=len(TOKENS), dim=32, layers=1, ff=64)), codec)
            path = folder / "model.pt"
            metadata = {"mode": "probe_cpu", "input_contract": input_contract(codec), "tokenizer_sha256": module.sha256(tokenizer)}
            checkpoint(path, model, metadata, 0)
            loaded, payload, actual_codec = module.load_export_model(path, tokenizer)
            self.assertEqual(loaded.config.input_vocab, 540)
            self.assertEqual(input_contract(actual_codec), payload["input_contract"])
            bad = folder / "old-tokenizer.json"
            codec.base.save(bad)
            with self.assertRaises(ValueError):
                module.load_export_model(path, bad)

    def test_encoder_decoder_match_at_two_unseen_shapes(self):
        try:
            module = importlib.import_module("ai_model.export")
        except ModuleNotFoundError:
            self.fail("ONNX encoder·decoder 내보내기가 아직 없습니다")
        import torch
        from ai_model.model import ModelConfig, PlanTransformer
        torch.manual_seed(71)
        model = PlanTransformer(ModelConfig(input_vocab=300, output_vocab=70, dim=32, heads=4, layers=1, ff=64, dropout=0.0)).eval()
        with tempfile.TemporaryDirectory() as directory:
            module.export_models(model, directory)
            report = module.check_parity(model, directory, [(1, 5, 2), (2, 9, 4)], atol=0.0001)
        self.assertEqual(report["cases"], 2)
        self.assertTrue(report["allclose"])
        self.assertTrue(report["argmax_equal"])


if __name__ == "__main__":
    unittest.main()
