"""NN 갱신 없이 주기 저장·저장 여유·예외 부분 보존 계약을 검사합니다."""
import tempfile
from pathlib import Path
from types import SimpleNamespace
import unittest
from unittest.mock import patch


class RuntimeTests(unittest.TestCase):
    def runtime(self, folder, clock):
        from ai_model import train
        self.assertTrue(callable(getattr(train, "TrainingRuntime", None)), "기한·부분 저장 runtime이 아직 없습니다")
        args = SimpleNamespace(output=folder, wall_seconds=1800, save_reserve_seconds=30, checkpoint_every=1000)
        return train.TrainingRuntime(args, clock=clock)

    def test_deadline_reserves_time_for_partial_save(self):
        now = [0]
        with tempfile.TemporaryDirectory() as folder:
            runtime = self.runtime(folder, lambda: now[0])
            now[0] = 1769
            runtime.check()
            now[0] = 1770
            with self.assertRaises(TimeoutError):
                runtime.check()

    def test_periodic_checkpoint_uses_actual_completed_step(self):
        with tempfile.TemporaryDirectory() as folder:
            runtime = self.runtime(folder, lambda: 0)
            runtime.owns_output = True
            runtime.attach(object(), {"mode": "train"})
            with patch("ai_model.train.checkpoint", return_value="hash") as save:
                runtime.completed_step(999)
                save.assert_not_called()
                runtime.completed_step(1000)
                self.assertEqual(save.call_args.args[3], 1000)
                self.assertEqual(save.call_args.args[0], Path(folder) / "step-001000.pt")

    def test_failure_keeps_original_error_and_partial_step(self):
        import json
        with tempfile.TemporaryDirectory() as folder:
            runtime = self.runtime(folder, lambda: 4)
            runtime.owns_output = True
            runtime.attach(object(), {"mode": "train"})
            runtime.completed_step(7)
            with patch("ai_model.train.checkpoint", return_value="partial-hash") as save:
                runtime.record_failure(ValueError("원래 실패"))
            self.assertEqual(save.call_args.args[3], 7)
            evidence = json.loads((Path(folder) / "failure.json").read_text(encoding="utf-8"))
            self.assertEqual(evidence["error"], "원래 실패")
            self.assertEqual(evidence["actual_optimizer_steps"], 7)
            self.assertEqual(evidence["partial_checkpoint_sha256"], "partial-hash")


if __name__ == "__main__":
    unittest.main()
