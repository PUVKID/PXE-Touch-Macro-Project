"""23차 AI의 의존성 없는 계약 검사와 선택적 실제 신경망 검사를 실행합니다."""
import argparse
import json
from pathlib import Path
import sys
import unittest


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--packages", help="선택: 실제 전용 PyTorch target 폴더")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    sys.path.insert(0, str(root))
    if args.packages: sys.path.insert(0, str(Path(args.packages).resolve()))
    scanned = 0
    for path in sorted((root / "ai_model").rglob("*.py")):
        source = path.read_bytes().decode("utf-8", errors="strict")
        if "\ufffd" in source: raise ValueError("대체 문자 발견: "+str(path))
        compile(source, str(path), "exec"); scanned += 1
    names = ["ai_model.tests.test_contract", "ai_model.tests.test_data_model", "ai_model.tests.test_numbers", "ai_model.tests.test_rephrase", "ai_model.tests.test_diversify", "ai_model.tests.test_literal_codec"]
    if args.packages: names += ["ai_model.tests.test_neural", "ai_model.tests.test_literal_neural", "ai_model.tests.test_export"]
    suite = unittest.TestSuite(unittest.defaultTestLoader.loadTestsFromName(name) for name in names)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    print(json.dumps({"python_utf8_syntax_files": scanned, "tests_run": result.testsRun, "failures": len(result.failures), "errors": len(result.errors), "neural_tests_requested": bool(args.packages), "model_assets_verified": False, "android_executions": 0}, ensure_ascii=False))
    raise SystemExit(0 if result.wasSuccessful() else 1)


if __name__ == "__main__":
    main()
