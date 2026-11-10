"""모델이 선택한 수치 슬롯의 변환과 기존 범위 경계를 확인합니다."""
import importlib
import unittest


class NumberTests(unittest.TestCase):
    def numbers(self):
        try:
            return importlib.import_module("ai_model.numbers")
        except ModuleNotFoundError:
            self.fail("선택된 수치 슬롯의 엄격 변환이 아직 없습니다")

    def test_native_and_sino_numbers_have_exact_integer_values(self):
        module = self.numbers()
        for value, expected in (("한", 1), ("두", 2), ("세", 3), ("열다섯", 15), ("스물", 20), ("스무", 20), ("스물 다섯", 25), ("십오", 15), ("이십삼", 23), ("백이", 102), ("천삼백이십일", 1321), ("십만", 100000), ("0", 0)):
            with self.subTest(value=value): self.assertEqual(module.parse_integer(value), expected)

    def test_decimal_duration_uses_exact_integral_milliseconds(self):
        module = self.numbers()
        for text, unit, expected in (("1.5", "SEC", 1500), ("0.001", "SEC", 1), ("1.25", "MIN", 75000), ("한", "HOUR", 3600000), ("열다섯", "SEC", 15000)):
            with self.subTest(text=text): self.assertEqual(module.parse_duration(text, unit), expected)

    def test_invalid_partial_nonfinite_negative_and_fractional_counts_reject(self):
        module = self.numbers()
        for text in ("", " ", "-1", "+1", "NaN", "Inf", "1.5", "일일", "십백", "이이십", "영십", "스무하나", "123abc", True, 1.0):
            with self.subTest(text=text), self.assertRaises(ValueError): module.parse_integer(text)
        for text, unit in (("0.0001", "SEC"), ("1.5", "MS"), ("NaN", "SEC"), ("-1", "MIN"), ("1", "DAY")):
            with self.subTest(text=text, unit=unit), self.assertRaises(ValueError): module.parse_duration(text, unit)


if __name__ == "__main__":
    unittest.main()
