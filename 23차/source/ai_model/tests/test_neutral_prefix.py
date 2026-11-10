"""훈련 전용 중립 안내문의 추첨·COPY 이동·길이 경계와 기본 경로를 검사합니다."""
from copy import deepcopy
import hashlib
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
from ai_model.rephrase import Edit, apply_edits
from ai_model.schema import parse_plan
from ai_model.tokenizer import CodepointTokenizer


class FixedDraw:
    """경계 검사용 제어 추첨이며 실제 신경망 해석 성공과 무관합니다."""
    def __init__(self, index=0):
        self.index, self.calls = index, []

    def random(self):
        self.calls.append('random')
        return 0.25

    def randrange(self, count):
        self.calls.append(('randrange', count))
        return self.index


class NeutralPrefixTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        vectors = json.loads((Path(__file__).resolve().parents[1] / 'literal_codec_vectors.json').read_text(encoding='utf-8'))
        cls.legacy = CodepointTokenizer(vectors['characters'])
        cls.literal = LiteralSlotCodec(vectors['characters'], base_tokenizer_sha256=vectors['base_tokenizer_sha256'])
        cls.config = SimpleNamespace(max_input=1024, max_output=512)

    def helper(self):
        value = getattr(train, 'neutral_prefix_row', None)
        self.assertTrue(callable(value), 'neutral_prefix_row가 아직 구현되지 않았습니다')
        return value

    def row(self, text='정지해🙂e\u0301\n\u0085\\명령', *, reject=False):
        command = f'"{text}" 입력하고 12초 대기'
        start, number = 1, command.index('12')
        tokens = ['REJECT', 'NEGATED', 'EOS'] if reject else ['PLAN', 'INPUT', {'token': 'COPY', 'start': start, 'end': start+len(text)}, 'WAIT', {'token': 'COPY', 'start': number, 'end': number+2}, 'SEC', 'END', 'EOS']
        context = {'planning_mode': 'MACRO'}
        return {'id': 'train-prefix-fixture', 'split': 'train', 'kind': 'reject' if reject else 'sequence', 'command': command, 'tokens': tokens, 'context': context, 'plan': parse_plan(tokens, command, context)}

    def back_row(self, command):
        row = self.row(reject=True)
        row.update(command=command, tokens=['PLAN', 'BACK', 'END', 'EOS'], kind='atomic')
        row['plan'] = parse_plan(row['tokens'], command, row['context'])
        return row

    def same_batch(self, left, right):
        self.assertEqual(len(left), 7)
        self.assertEqual(len(right), 7)
        for a, b in zip(left, right):
            self.assertTrue(torch.equal(a, b))
        for key in ('pointer_start_keep', 'pointer_end_keep'):
            a, b = getattr(left, key), getattr(right, key)
            self.assertTrue(a is b if a is None else torch.equal(a, b), key)
        self.assertEqual(left.input_contract, right.input_contract)

    def test_pool_and_metadata_are_exact_and_training_only(self):
        self.helper()
        draft_pool = ('안내: ', '작업 내용: ', '다음은 작업 내용입니다. ', '아래는 작업 내용입니다. ', '다음 문장은 작업 내용입니다. ', '아래 문장은 작업 내용입니다. ', '작업할 내용은 아래에 있어요. ', '아래 문구에 작업 내용이 있어요. ', '여기에 작업 내용이 있어요. ', '다음에 적은 내용이 작업 내용입니다. ', '다음 문구는 제가 정한 작업 내용입니다. ', '아래 문장으로 작업 내용을 전해요. ')
        self.assertEqual(train.NEUTRAL_PREFIXES, draft_pool)
        self.assertTrue(all(char in self.legacy.char_to_id for text in draft_pool for char in text))
        metadata = train.neutral_prefix_metadata(0.5, 1730002)
        self.assertEqual(metadata['pool'], list(draft_pool))
        expected = hashlib.sha256(json.dumps(list(draft_pool), ensure_ascii=False, separators=(',', ':')).encode('utf-8')).hexdigest()
        self.assertEqual(metadata['pool_sha256'], expected)
        self.assertEqual(metadata['probability'], 0.5)
        self.assertEqual(metadata['seed'], 1730002)
        self.assertTrue(metadata['training_only'])
        self.assertFalse(metadata['inference_policy_changed'])

    def test_zero_is_identity_without_rng_or_codec_calls(self):
        helper, row = self.helper(), self.row()
        draw = FixedDraw()
        with patch.object(self.literal, 'encode_request', side_effect=AssertionError('기본0에서 codec 추가 호출')):
            self.assertIs(helper(row, self.literal, self.config, probability=0, rng=draw), row)
        self.assertEqual(draw.calls, [])

    def test_zero_preserves_all_nine_items_order_and_both_rngs(self):
        self.helper()
        rows = [self.row('가🙂'), self.row(reject=True), self.row('e\u0301')]
        for codec in (self.legacy, self.literal):
            for byte_probability in (0.0, 0.3):
                with self.subTest(codec=codec.size, byte_probability=byte_probability):
                    old_rng, new_rng = random.Random(1730001), random.Random(1730001)
                    prefix_rng = random.Random(1730002)
                    prefix_state, global_state, torch_state = prefix_rng.getstate(), random.getstate(), torch.get_rng_state().clone()
                    old_stats, new_stats = {}, {}
                    selected = [2, 0, 1, 2]
                    old = [encode_row(rows[index], codec, byte_probability=byte_probability, augmentation_rng=old_rng, augmentation_stats=old_stats) if byte_probability else encode_row(rows[index], codec) for index in selected]
                    new = train.encode_selected_rows(rows, selected, codec, self.config, byte_probability=byte_probability, augmentation_rng=new_rng, augmentation_stats=new_stats, neutral_prefix_probability=0, neutral_prefix_rng=prefix_rng, neutral_prefix_stats={})
                    self.same_batch(collate(old, 'cpu'), collate(new, 'cpu'))
                    self.assertEqual(old_rng.getstate(), new_rng.getstate())
                    self.assertEqual(old_stats, new_stats)
                    self.assertEqual(prefix_rng.getstate(), prefix_state)
                    self.assertEqual(random.getstate(), global_state)
                    self.assertTrue(torch.equal(torch.get_rng_state(), torch_state))

    def test_every_prefix_shifts_all_copy_and_preserves_raw_gold_context(self):
        helper, row = self.helper(), self.row()
        original = deepcopy(row)
        for index, prefix in enumerate(train.NEUTRAL_PREFIXES):
            changed = helper(row, self.literal, self.config, probability=1, rng=FixedDraw(index))
            self.assertEqual(changed['command'], prefix+row['command'])
            self.assertEqual(changed['plan'], row['plan'])
            self.assertEqual(changed['context'], row['context'])
            self.assertEqual(parse_plan(changed['tokens'], changed['command'], changed['context']), row['plan'])
            for old, new in zip(row['tokens'], changed['tokens']):
                if isinstance(old, dict):
                    self.assertEqual((new['start'], new['end']), (old['start']+len(prefix), old['end']+len(prefix)))
                    self.assertEqual(changed['command'][new['start']:new['end']], row['command'][old['start']:old['end']])
            self.assertEqual(row, original)

    def test_numeric_copy_at_zero_and_multiple_literals_match_manual_batch(self):
        helper = self.helper()
        command = '12초 대기하고 "뒤로 가🙂" 입력하고 "e\u0301" 입력'
        tokens = ['PLAN', 'WAIT', {'token': 'COPY', 'start': 0, 'end': 2}, 'SEC', 'INPUT', {'token': 'COPY', 'start': command.index('뒤'), 'end': command.index('뒤')+5}, 'INPUT', {'token': 'COPY', 'start': command.index('e'), 'end': command.index('e')+2}, 'END', 'EOS']
        row = self.back_row(command)
        row['tokens'], row['plan'] = tokens, parse_plan(tokens, command, row['context'])
        for codec in (self.legacy, self.literal):
            actual = helper(row, codec, self.config, probability=1, rng=FixedDraw(1))
            manual = apply_edits(row, [Edit(0, 0, train.NEUTRAL_PREFIXES[1], '검사')])
            self.same_batch(collate([encode_row(actual, codec)], 'cpu'), collate([encode_row(manual, codec)], 'cpu'))

    def test_empty_blank_and_escaped_quoted_reject_stay_verbatim(self):
        helper = self.helper()
        for text in ('', '  '):
            with self.assertRaisesRegex(ValueError, '^빈 인용 또는 전부 공백인 인용$'):
                helper(self.row(text, reject=True), self.literal, self.config, probability=1, rng=FixedDraw())
        for text in ('명령\\"그대로', '🙂\u0085\n'):
            row = self.row(text, reject=True)
            changed = helper(row, self.literal, self.config, probability=1, rng=FixedDraw())
            self.assertEqual(changed['command'], train.NEUTRAL_PREFIXES[0]+row['command'])
            self.assertEqual(changed['tokens'], row['tokens'])
            self.assertEqual(changed['plan'], row['plan'])

    def test_positive_reject_and_kind_use_identical_unconditional_draws(self):
        helper = self.helper()
        for seed in range(48):
            rows = [self.row(), self.row(reject=True), self.back_row('뒤로 가')]
            results, states = [], []
            for row in rows:
                rng = random.Random(seed)
                changed = helper(row, self.literal, self.config, probability=0.5, rng=rng)
                results.append(changed['command'][:-len(row['command'])])
                states.append(rng.getstate())
            self.assertEqual(results, [results[0]]*3)
            self.assertEqual(states, [states[0]]*3)

    def test_seed_repeat_and_global_byte_rng_independence(self):
        helper = self.helper()
        row, byte_rng = self.row(), random.Random(1730001)
        before = random.getstate(), torch.get_rng_state().clone(), byte_rng.getstate()
        sequences = []
        for _ in range(2):
            rng = random.Random(1730002)
            sequences.append([helper(row, self.literal, self.config, probability=0.5, rng=rng)['command'] for _ in range(80)])
        self.assertEqual(sequences[0], sequences[1])
        self.assertGreater(len(set(sequences[0])), 5)
        self.assertEqual(random.getstate(), before[0])
        self.assertTrue(torch.equal(torch.get_rng_state(), before[1]))
        self.assertEqual(byte_rng.getstate(), before[2])

    def test_old535_limit_keeps_original_with_exact_reason_without_retry(self):
        helper = self.helper()
        row = self.row('가')
        overhead = len(self.legacy.encode_request(row['command'], row['context'])['ids'])-1
        row = self.row('가'*(1024-overhead))
        self.assertEqual(len(self.legacy.encode_request(row['command'], row['context'])['ids']), 1024)
        self.literal.encode_request(row['command'], row['context'])
        self.assertGreater(len(self.legacy.encode_request('안내: '+row['command'], row['context'])['ids']), 1024)
        for codec in (self.legacy, self.literal):
            stats, draw = {}, FixedDraw()
            self.assertIs(helper(row, codec, self.config, probability=1, rng=draw, stats=stats), row)
            self.assertEqual(stats['old535_limit'], 1)
            self.assertEqual(draw.calls, ['random', ('randrange', 12)])
            self.assertNotIn('applied', stats)

    def test_new540_limit_uses_actual_codec_and_keeps_original(self):
        helper = self.helper()
        command = ' '.join('"가'+str(index)+'"' for index in range(12))
        row = self.back_row(command)
        length = len(self.literal.encode_request(command, row['context'])['ids'])
        row = self.back_row(command+'가'*(1024-length))
        self.assertEqual(len(self.literal.encode_request(row['command'], row['context'])['ids']), 1024)
        self.assertLess(len(self.legacy.encode_request('안내: '+row['command'], row['context'])['ids']), 1024)
        with self.assertRaisesRegex(ValueError, '^새 입력1024 한도 초과: 슬롯·문맥을 자르지 않습니다$'):
            self.literal.encode_request('안내: '+row['command'], row['context'])
        stats = {}
        self.assertIs(helper(row, self.literal, self.config, probability=1, rng=FixedDraw(), stats=stats), row)
        self.assertEqual(stats['new540_limit'], 1)

    def test_configured_lower_limit_and_original_bad_rows_are_not_truncated(self):
        helper, row = self.helper(), self.row()
        length = len(self.literal.encode_request(row['command'], row['context'])['ids'])
        stats = {}
        self.assertIs(helper(row, self.literal, SimpleNamespace(max_input=length, max_output=512), probability=1, rng=FixedDraw(), stats=stats), row)
        self.assertEqual(stats['configured_limit'], 1)
        bad = deepcopy(row)
        bad['tokens'][2]['end'] += 1
        with self.assertRaises(ValueError):
            helper(bad, self.literal, self.config, probability=1, rng=FixedDraw())
        for error in (ValueError('알 수 없는 codec 오류'), UnicodeError('엄격 원문 오류')):
            with patch.object(self.literal, 'encode_request', side_effect=error):
                with self.assertRaises(type(error)) as caught:
                    helper(row, self.literal, self.config, probability=1, rng=FixedDraw())
                self.assertIs(caught.exception, error)

    def test_invalid_context_gold_split_and_probability_propagate(self):
        helper, row = self.helper(), self.row()
        bad = deepcopy(row)
        bad['context']['planning_mode'] = '미지원'
        with self.assertRaises(ValueError):
            helper(bad, self.literal, self.config, probability=1, rng=FixedDraw())
        bad = deepcopy(row)
        bad['plan'] = {'status': '다름'}
        with self.assertRaises(ValueError):
            helper(bad, self.literal, self.config, probability=1, rng=FixedDraw())
        bad = deepcopy(row)
        bad['split'] = 'dev'
        with self.assertRaises(ValueError):
            helper(bad, self.literal, self.config, probability=1, rng=FixedDraw())
        for probability in (-0.1, 1.1, float('nan'), float('inf'), True, '0.5'):
            with self.assertRaises(ValueError):
                helper(row, self.literal, self.config, probability=probability, rng=FixedDraw())

    def test_stats_report_selection_application_and_status_after_draw(self):
        helper, stats = self.helper(), {}
        row, reject = self.row(), self.row(reject=True)
        helper(row, self.literal, self.config, probability=1, rng=FixedDraw(2), stats=stats)
        helper(reject, self.literal, self.config, probability=1, rng=FixedDraw(2), stats=stats)
        helper(row, self.literal, self.config, probability=0, rng=FixedDraw(), stats=stats)
        self.assertEqual(stats['rows_seen'], 3)
        self.assertEqual(stats['selected'], 2)
        self.assertEqual(stats['applied'], 2)
        self.assertEqual(stats['not_selected'], 1)
        self.assertEqual(stats['prefix.P02.applied'], 2)
        self.assertEqual(stats['status.ready.applied'], 1)
        self.assertEqual(stats['status.reject.applied'], 1)
        self.assertEqual(stats['kind.sequence.applied'], 1)
        self.assertEqual(stats['kind.reject.applied'], 1)

    def test_batch_path_has_only_plain_encoder_input_and_original_gold(self):
        self.helper()
        row = self.row()
        original = deepcopy(row)
        actual = train.encode_selected_rows([row], [0, 0], self.literal, self.config, neutral_prefix_probability=1, neutral_prefix_rng=FixedDraw(0), neutral_prefix_stats={})
        manual = apply_edits(row, [Edit(0, 0, '안내: ', '검사')])
        reference = encode_row(manual, self.literal)
        self.assertEqual(actual, [reference, reference])
        self.assertEqual(set(actual[0]), set(reference))
        self.assertEqual(row, original)

    def test_metadata_separates_encoding_attempts_from_completed_optimizer(self):
        self.helper()
        source = inspect.getsource(train._run)
        for field in ('neutral_prefix_augmentation', 'encoding_attempt_counts', 'optimizer_completed_counts', 'pending_batch_rows', 'neutral-prefix-epochs.json'):
            self.assertIn(field, source)
        self.assertLess(source.index('optimizer.step()'), source.index('prefix_completed_counts.update(prefix_counts)'))
        self.assertLess(source.index('prefix_completed_counts.update(prefix_counts)'), source.index('runtime.completed_step(step)'))
        self.assertIn('--neutral-prefix-probability', inspect.getsource(train.main))
        self.assertIn('--neutral-prefix-seed', inspect.getsource(train.main))


if __name__ == '__main__':
    unittest.main()
