"""납품 APK와 이전 차수 보존, 앱 이름·버전·서명을 직접 확인합니다."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import struct
import zlib
import subprocess
import zipfile

from project_paths import BUILD, CANONICAL, DELIVERY, JDK, SDK
ROOT = CANONICAL
CERTIFICATE = '57339c32b3661e18f18a7f9f7a08c539130211ab93ab0000812f99c949c5ad3e'


def digest(path):
    result = hashlib.sha256()
    with path.open('rb') as stream:
        while block := stream.read(1024 * 1024):
            result.update(block)
    return result.hexdigest()


def command(argv):
    env = dict(os.environ, JAVA_HOME=str(JDK))
    result = subprocess.run(argv, shell=False, env=env, cwd=ROOT,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            timeout=30)
    output = result.stdout.decode('utf-8', errors='strict')
    if result.returncode:
        raise RuntimeError(f'검사 실패 {result.returncode}: {argv!r}\n{output[:8000]}')
    return output


BEHAVIOR_CLASSES = tuple('Lkr/yunseok/touchmacro/' + name + ';' for name in (
    'BehaviorFeatures', 'BehaviorPolicy', 'BehaviorExample', 'BehaviorBundle',
    'BehaviorAndroidBridge', 'BehaviorAndroidSession', 'BehaviorTaskStore',
    'MacroService', 'FloatingController', 'MainActivity'))


def dex_class_definitions(data):
    # 공식 DEX의 class_defs → type_ids → string_ids만 따라갑니다. 단순 문자열 검색은 하지 않습니다.
    # https://source.android.com/docs/core/runtime/dex-format
    if len(data) < 112 or data[:8] not in tuple(b'dex\n' + version + b'\0' for version in (b'035', b'037', b'038', b'039', b'040')):
        raise RuntimeError('지원하지 않거나 잘린 DEX 헤더')
    def uint(offset):
        if offset < 0 or offset + 4 > len(data):
            raise RuntimeError('DEX 정수 범위 오류')
        return struct.unpack_from('<I', data, offset)[0]
    if uint(32) != len(data) or uint(36) != 112 or uint(40) != 0x12345678:
        raise RuntimeError('DEX 크기·헤더·바이트 순서 불일치')
    if uint(8) != zlib.adler32(data[12:]) & 0xffffffff or data[12:32] != hashlib.sha1(data[32:]).digest():
        raise RuntimeError('DEX 내부 체크섬 불일치')
    def table(size_offset, item_size):
        count, offset = uint(size_offset), uint(size_offset + 4)
        if count == 0:
            if offset != 0:
                raise RuntimeError('빈 DEX 테이블의 잘못된 위치')
        elif offset < 112 or offset % 4 or count > (len(data) - offset) // item_size:
            raise RuntimeError('DEX 테이블 범위 오류')
        return count, offset
    strings, string_off = table(56, 4)
    types, type_off = table(64, 4)
    classes, class_off = table(96, 32)
    data_size, data_off = uint(104), uint(108)
    data_end = data_off + data_size
    if data_off < 112 or data_end > len(data):
        raise RuntimeError('DEX 데이터 영역 범위 오류')
    def descriptor(index):
        if index >= strings:
            raise RuntimeError('DEX 문자열 참조 범위 오류')
        offset = uint(string_off + index * 4)
        if not data_off <= offset < data_end:
            raise RuntimeError('DEX 문자열 위치 범위 오류')
        length = 0
        for shift in range(0, 35, 7):
            if offset >= data_end:
                raise RuntimeError('잘린 DEX 문자열 길이')
            byte = data[offset]
            offset += 1
            if shift == 28 and byte > 15:
                raise RuntimeError('DEX 문자열 길이 정수 초과')
            length |= (byte & 127) << shift
            if byte < 128:
                break
        end = data.find(b'\0', offset, data_end)
        if end < 0:
            raise RuntimeError('DEX 문자열 끝 없음')
        raw = data[offset:end]
        # MUTF-8의 UTF-16 단위 길이를 검증하며 서로게이트도 원시 바이트로 보존합니다.
        cursor = units = 0
        while cursor < len(raw):
            first = raw[cursor]
            width = 1 if 1 <= first < 128 else 2 if 0xc0 <= first <= 0xdf else 3 if 0xe0 <= first <= 0xef else 0
            if not width or cursor + width > len(raw) or any(not 0x80 <= value <= 0xbf for value in raw[cursor + 1:cursor + width]):
                raise RuntimeError('DEX MUTF-8 형식 오류')
            if width == 2:
                value = ((first & 31) << 6) | (raw[cursor + 1] & 63)
                if value < 128 and value != 0:
                    raise RuntimeError('DEX MUTF-8 과잉 인코딩')
            elif width == 3 and (((first & 15) << 12) | ((raw[cursor + 1] & 63) << 6) | (raw[cursor + 2] & 63)) < 0x800:
                raise RuntimeError('DEX MUTF-8 과잉 인코딩')
            cursor += width
            units += 1
        if units != length or not raw.startswith(b'L') or not raw.endswith(b';'):
            raise RuntimeError('DEX 클래스 이름 또는 길이 불일치')
        return raw
    definitions = set()
    for number in range(classes):
        class_index = uint(class_off + 32 * number)
        if class_index >= types:
            raise RuntimeError('DEX 클래스 타입 참조 범위 오류')
        name = descriptor(uint(type_off + class_index * 4))
        if name in definitions:
            raise RuntimeError('DEX 클래스 정의 중복')
        definitions.add(name)
    return definitions


def language_bundle_status(archive):
    required = {'bundle.json', 'export-report.json', 'tokenizer.json', 'encoder.onnx', 'decoder.onnx'}
    found = {name[len('assets/ai/'):] for name in archive.namelist() if name.startswith('assets/ai/')}
    if not found:
        return 'absent-language-model'
    if found != required:
        raise RuntimeError('언어 번들 일부 누락 또는 알 수 없는 자산')
    def read(name, limit):
        info = archive.getinfo('assets/ai/' + name)
        if not 1 <= info.file_size <= limit:
            raise RuntimeError('언어 번들 파일 크기 상한 초과: ' + name)
        return archive.read(info)
    try:
        bundle = json.loads(read('bundle.json', 1024 * 1024).decode('utf-8', errors='strict'))
        names = required - {'bundle.json'}
        if set(bundle) != {'format', 'files'} or bundle['format'] != 'pxe-ai-bundle-v1' or set(bundle['files']) != names:
            raise RuntimeError('AI 번들 네 파일 명세가 다릅니다')
        for name in names:
            record = bundle['files'][name]
            limit = 1024 * 1024 if name.endswith('.json') else 64 * 1024 * 1024
            data = read(name, limit)
            if set(record) != {'bytes', 'sha256'} or type(record['bytes']) is not int or not 1 <= record['bytes'] <= limit or not isinstance(record['sha256'], str) or re.fullmatch('[0-9a-f]{64}', record['sha256']) is None or len(data) != record['bytes'] or hashlib.sha256(data).hexdigest() != record['sha256']:
                raise RuntimeError('AI 번들 네 파일의 크기·해시 불일치: ' + name)
        report = json.loads(read('export-report.json', 1024 * 1024).decode('utf-8', errors='strict'))
        if report.get('format') != 'FP32' or report.get('allclose') is not True or report.get('argmax_equal') is not True:
            raise RuntimeError('번들 자체 모델 FP32 정합 보고서가 없습니다')
        for name in ('encoder.onnx', 'decoder.onnx'):
            data = read(name, 64 * 1024 * 1024)
            record = report['files'][name]
            if len(data) != record['bytes'] or hashlib.sha256(data).hexdigest() != record['sha256']:
                raise RuntimeError('번들 모델 파일·정합 보고서 해시가 다릅니다')
        tokenizer = json.loads(read('tokenizer.json', 1024 * 1024).decode('utf-8', errors='strict'))
        if tokenizer.get('normalization') != 'none' or tokenizer.get('offset_unit') != 'unicode-codepoint-half-open':
            raise RuntimeError('APK 토크나이저 계약 불일치')
    except (KeyError, TypeError, ValueError) as error:
        raise RuntimeError('언어 번들 JSON 형식 또는 참조 오류') from error
    return 'validated-language-bundle'


def validate_zip_payload(archive, expected_abis):
    names = archive.namelist()
    if len(names) != len(set(names)):
        raise RuntimeError('APK ZIP 항목 중복')
    if archive.testzip() is not None:
        raise RuntimeError('APK 압축 데이터 손상')
    abis = {name.split('/')[1] for name in names if name.startswith('lib/') and name.endswith('.so')}
    if abis != expected_abis:
        raise RuntimeError(f'APK 지원 기기 불일치: {abis!r}')
    # 언어 자산 유무와 무관하게 기존 런타임·두 원문 고지를 검사합니다.
    for name in ('assets/ONNXRuntime-LICENSE.txt', 'assets/ONNXRuntime-ThirdPartyNotices.txt'):
        if name not in names or not archive.read(name):
            raise RuntimeError('CPU 런타임 고지 누락: ' + name)
    for abi in expected_abis:
        for name in ('libonnxruntime.so', 'libonnxruntime4j_jni.so'):
            if 'lib/' + abi + '/' + name not in names:
                raise RuntimeError('CPU 런타임 누락: ' + abi + '/' + name)
    definitions = set()
    dex_names = [name for name in names if re.fullmatch(r'classes(?:[2-9]|[1-9][0-9]+)?\.dex', name)]
    dex_names.sort(key=lambda name: 1 if name == 'classes.dex' else int(name[7:-4]))
    expected_names = ['classes.dex'] + ['classes' + str(index) + '.dex' for index in range(2, len(dex_names) + 1)]
    if not dex_names or dex_names != expected_names:
        raise RuntimeError('APK DEX 목록 누락 또는 불연속')
    for name in dex_names:
        if archive.getinfo(name).file_size > 128 * 1024 * 1024:
            raise RuntimeError('DEX 검사 자원 상한 초과')
        current = dex_class_definitions(archive.read(name))
        if current & definitions:
            raise RuntimeError('여러 DEX 사이 클래스 정의 중복')
        definitions.update(current)
    missing = [name for name in BEHAVIOR_CLASSES if name.encode('ascii') not in definitions]
    if missing:
        raise RuntimeError('행동학습 DEX 클래스 정의 누락: ' + ', '.join(missing))
    return {'abis': sorted(abis), 'language_bundle': language_bundle_status(archive),
            'behavior_class_definitions': list(BEHAVIOR_CLASSES), 'dex_files': dex_names}


def main():
    baseline = json.loads((DELIVERY / '22차_보존기준.json').read_text(encoding='utf-8-sig'))
    for entry in baseline:
        path = Path(entry['path'])
        if path.stat().st_size != entry['bytes'] or digest(path).upper() != entry['sha256']:
            raise RuntimeError(f'이전 차수 변경 감지: {path}')
    print(f'22차 원본 보존: {len(baseline)}개 통과')
    output = BUILD / 'work/artifact-verification'
    output.mkdir(parents=True, exist_ok=True)
    tools = SDK / 'build-tools/35.0.0'
    variants = [('공용', 'universal', {'arm64-v8a', 'x86_64'})]
    for label, short, expected_abis in variants:
        source = DELIVERY / f'PXE_23차_v2.3_{label}.apk'
        target = output / f'{short}.apk'
        shutil.copyfile(source, target)
        if digest(source) != digest(target):
            raise RuntimeError('검사용 복사본의 해시 불일치')
        # Windows Android 도구의 한글 경로 제한을 피하되 납품본 해시를 대조합니다.
        ascii_target = BUILD / f'work/artifact-verification/{short}.apk'
        metadata = command([str(tools / 'aapt.exe'), 'dump', 'badging', str(ascii_target)])
        if not re.search(r"package: name='kr\.yunseok\.touchmacro'.*versionCode='23'.*versionName='2\.3\.0'", metadata):
            raise RuntimeError('APK 앱 ID 또는 버전 불일치')
        if "application-label:'PXE'" not in metadata:
            raise RuntimeError('APK 앱 이름 불일치')
        signed = command([str(JDK / 'bin/java.exe'),
                          '-Dfile.encoding=UTF-8', '-jar', str(tools / 'lib/apksigner.jar'),
                          'verify', '--print-certs', str(ascii_target)])
        certificates = re.findall(r'certificate SHA-256 digest: ([0-9a-fA-F]+)', signed)
        if certificates != [CERTIFICATE]:
            raise RuntimeError('기존 앱과 APK 서명 불일치')
        with zipfile.ZipFile(target) as archive:
            payload = validate_zip_payload(archive, expected_abis)
            abis = set(payload['abis'])
            language = '언어 모델 없음' if payload['language_bundle'] == 'absent-language-model' else '언어 번들 검증됨'
        if abis != expected_abis:
            raise RuntimeError(f'APK 지원 기기 불일치: {abis!r}')
        permissions=command([str(tools/'aapt.exe'),'dump','permissions',str(ascii_target)])
        if 'android.permission.INTERNET' in permissions or 'android.permission.ACCESS_NETWORK_STATE' in permissions:
            raise RuntimeError('오프라인 APK에 네트워크 권한이 있습니다')
        print(f'{label}: 앱 ID·PXE·23/2.3.0·기존 서명·ABI·CRC·{language}·행동학습10클래스 정의·네트워크 권한 없음 통과 / SHA256 {digest(source)}')


if __name__ == '__main__':
    main()
