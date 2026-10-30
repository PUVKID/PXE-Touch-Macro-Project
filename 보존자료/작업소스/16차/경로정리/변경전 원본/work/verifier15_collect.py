"""동결 명세의 argv·상한을 유지하며 독립 검사 원문과 검사 수를 수집합니다."""
import codecs
import datetime
import hashlib
import json
import os
import re
import subprocess
import sys
import threading
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(Path.home() / '.codex/hooks'))
from routing_stop import run_check, source_snapshot

EVIDENCE = ROOT / 'evidence'
STATE = ROOT / '.codex/routing-state/df1ea7273e46d16ba9664afc.json'
SESSION = '01a131ad-d167-7873-9aa6-4d81ff131c60'
TASK = 'pxe15-recognition-simplify-20261028'

def now():
    return datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=9))).isoformat()

def save(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def state():
    data = json.loads(STATE.read_text(encoding='utf-8'))
    return {k: data[k] for k in ('session_id', 'task_id', 'mode', 'repair_used', 'input_fix_used', 'verification_failures', 'stop_failures')}

def item_for(group, index):
    if group == 'gradle':
        command = r"[Console]::OutputEncoding=[Text.UTF8Encoding]::new($false); & 'D:\PXE15Build\tools\gradle-8.13\bin\gradle.bat' :app:testDebugUnitTest :app:lintDebug --rerun-tasks --console=plain --no-watch-fs --project-cache-dir work/.gradle; exit $LASTEXITCODE"
        return dict(id='독립 Gradle 단위 및 린트', argv=['C:/WINDOWS/System32/WindowsPowerShell/v1.0/powershell.exe', '-NoProfile', '-NonInteractive', '-Command', command], cwd='D:/PXE15Build/source', timeout_seconds=180)
    if group == 'required':
        return json.loads((ROOT / 'tools/required-check-plan.json').read_text(encoding='utf-8'))['checks'][index]
    plan = json.loads((ROOT / 'tools/recognition-check-plan.json').read_text(encoding='utf-8'))
    return dict(plan['small_screen'], id='320dp 작은 화면 UI6') if group == 'small' else plan['checks'][index]

def child(group, index, label):
    item = item_for(group, index)
    argv = [sys.executable if a == '{python}' else a for a in item['argv']]
    prefix = EVIDENCE / f'verifier15-{label}-{group}-{index}'
    started = now()
    if group == 'gradle':
        os.environ.update(JAVA_HOME='D:/PXE15Build/tools/jdk-17.0.20.1+1', ANDROID_HOME='D:/PXE15Build/android/android-sdk', NEX_SIGNING_DIR='D:/PXE15Build/signing')
    process = subprocess.Popen(argv, cwd=ROOT / item['cwd'], shell=False, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    raw = bytearray()
    original = 0
    cap = 65536
    decoder = codecs.getincrementaldecoder('utf-8')('strict')
    valid = True
    counts = []
    failed_counts = []
    scan = ''
    failed = False
    while block := process.stdout.read(4096):
        original += len(block)
        raw.extend(block[:max(0, cap-len(raw))])
        try:
            text = decoder.decode(block)
        except UnicodeDecodeError:
            valid = False
            text = block.decode('utf-8', errors='replace')
        scan += text
        lines = scan.split('\n')
        scan = lines.pop()
        for line in lines:
            counts.extend(int(x) for x in re.findall(r'OK \((\d+) tests?\)', line))
            failed_counts.extend((int(n), int(f)) for n, f in re.findall(r'Tests run: (\d+),\s*Failures: (\d+)', line))
            failed |= any(x in line for x in ('FAILURES!!!', 'INSTRUMENTATION_FAILED', 'INSTRUMENTATION_STATUS_CODE: -'))
    code = process.wait()
    try:
        decoder.decode(b'', final=True)
    except UnicodeDecodeError:
        valid = False
    capture = bytes(raw)
    if original > len(capture):
        capture = capture.decode('utf-8', errors='ignore').encode('utf-8')
    prefix.with_suffix('.txt').write_bytes(capture)
    expected = item.get('expected_tests')
    actual = sum(counts) + sum(n for n, f in failed_counts)
    passed = code == 0 and valid and not failed and (expected is None or actual == expected)
    extra = {}
    if group == 'gradle' and code == 0:
        suites = [ET.parse(p).getroot() for p in sorted((ROOT / 'work/build/app/test-results/testDebugUnitTest').glob('TEST-*.xml'))]
        totals = {key: sum(int(x.attrib.get(key, '0')) for x in suites) for key in ('tests', 'failures', 'errors', 'skipped')}
        issues = len(ET.parse(ROOT / 'work/build/app/reports/lint-results-debug.xml').getroot().findall('issue'))
        extra = dict(xml_classes=len(suites), unit_totals=totals, lint_issues=issues)
        passed &= len(suites) == 13 and totals == dict(tests=86, failures=0, errors=0, skipped=0) and issues == 0
    result = dict(id=item['id'], started_kst=started, ended_kst=now(), argv=argv, cwd=str(ROOT / item['cwd']),
                  exit_code=code, expected_tests=expected, actual_tests=actual, success_counts=counts, failed_batch_counts=failed_counts,
                  passed=passed, original_bytes=original, captured_bytes=len(capture), omitted=original > len(capture),
                  utf8_valid=valid, output_sha256=hashlib.sha256(capture).hexdigest(),
                  product_changes_by_verifier=0, state=state(), **extra)
    save(prefix.with_suffix('.json'), result)
    if not passed:
        with (EVIDENCE / 'verifier15-failures.jsonl').open('a', encoding='utf-8') as stream:
            stream.write(json.dumps(result, ensure_ascii=False) + '\n')
    print(json.dumps(result, ensure_ascii=False), flush=True)
    return 0 if passed else 1

def run_item(group, index, label, remaining):
    item = item_for(group, index)
    check = dict(id=item['id'], argv=[sys.executable, '-B', '-X', 'utf8', str(Path(__file__).resolve()), 'child', group, str(index), label],
                 cwd=str(ROOT), timeout_seconds=min(item['timeout_seconds'], max(.001, remaining)))
    result = run_check(check)
    print(item['id'], result['status'], result.get('duration_seconds'), flush=True)
    if result['status'] != 'passed':
        failure = dict(recorded_kst=now(), result=result, state=state(), product_changes_by_verifier=0)
        with (EVIDENCE / 'verifier15-failures.jsonl').open('a', encoding='utf-8') as stream:
            stream.write(json.dumps(failure, ensure_ascii=False) + '\n')
    return result

def collect(group, label):
    before = source_snapshot(ROOT)
    dispatch = json.loads(STATE.read_text(encoding='utf-8'))['verification_dispatch']
    if before != dispatch['source']:
        raise SystemExit('검사 전 동결 소스/전역 설정 불일치')
    started = time.monotonic()
    started_kst = now()
    results = []
    plan = json.loads((ROOT / ('tools/required-check-plan.json' if group == 'required' else 'tools/recognition-check-plan.json')).read_text(encoding='utf-8'))
    total = plan['total_timeout_seconds'] if group == 'required' else plan['focused_total_timeout_seconds']
    # 기존 계약의 실제 업데이트→UI→재확인→핵심→화면 순서를 유지합니다.
    indices = list(range(8)) if group == 'required' else [0, 1]
    report = EVIDENCE / f'verifier15-{label}-{group}-summary.json'
    def record():
        save(report, dict(started_kst=started_kst, recorded_kst=now(), elapsed_seconds=time.monotonic()-started,
                         total_limit_seconds=total, source_before=before, source_after=source_snapshot(ROOT), state=state(), results=results))
    for index in indices:
        result = run_item(group, index, label, total-(time.monotonic()-started))
        results.append(result)
        record()
        if result['status'] != 'passed':
            return 1
    if group == 'required':
        small = json.loads((ROOT / 'tools/recognition-check-plan.json').read_text(encoding='utf-8'))['small_screen']
        sys.path.insert(0, str(ROOT / 'tools'))
        from check_regression import run, PROJECT
        prefix = [str(PROJECT / '코드/NEXTools/android-sdk/platform-tools/adb.exe'), '-P', '5043', '-s', 'emulator-5598', 'shell']
        if run(prefix+['getprop', 'ro.boot.qemu.avd_name']).strip() != 'PXE15':
            raise SystemExit('소유 장치 불일치')
        try:
            run(prefix+['wm', 'size', small['size']])
            run(prefix+['wm', 'density', str(small['density'])])
            result = run_item('small', 0, label, total-(time.monotonic()-started))
            results.append(result)
            record()
            if result['status'] != 'passed':
                return 1
        finally:
            run(prefix+['wm', 'size', small['restore']['size']])
            run(prefix+['wm', 'density', str(small['restore']['density'])])
    record()
    if source_snapshot(ROOT) != before:
        raise SystemExit('검사 후 동결 소스/전역 설정 불일치')
    print('독립 검사 집계 완료', group, round(time.monotonic()-started, 3), flush=True)
    return 0

if __name__ == '__main__':
    if sys.argv[1] == 'child':
        raise SystemExit(child(sys.argv[2], int(sys.argv[3]), sys.argv[4]))
    raise SystemExit(collect(sys.argv[1], sys.argv[2]))
