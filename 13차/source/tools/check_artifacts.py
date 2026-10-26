"""납품 APK와 이전 차수 보존, 앱 이름·버전·서명을 직접 확인합니다."""
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
DELIVERY = ROOT.parent
BRIDGE = Path('D:/PXE13Build')
CERTIFICATE = '57339c32b3661e18f18a7f9f7a08c539130211ab93ab0000812f99c949c5ad3e'


def digest(path):
    result = hashlib.sha256()
    with path.open('rb') as stream:
        while block := stream.read(1024 * 1024):
            result.update(block)
    return result.hexdigest()


def command(argv):
    env = dict(os.environ, JAVA_HOME=str(BRIDGE / 'tools/jdk-17.0.20.1+1'))
    result = subprocess.run(argv, shell=False, env=env, cwd=ROOT,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            timeout=30)
    output = result.stdout.decode('utf-8', errors='strict')
    if result.returncode:
        raise RuntimeError(f'검사 실패 {result.returncode}: {argv!r}\n{output[:8000]}')
    return output


def main():
    baseline = json.loads((DELIVERY / '12차_보존기준.json').read_text(encoding='utf-8-sig'))
    for entry in baseline:
        path = Path(entry['path'])
        if path.stat().st_size != entry['bytes'] or digest(path).upper() != entry['sha256']:
            raise RuntimeError(f'이전 차수 변경 감지: {path}')
    print(f'12차 원본 보존: {len(baseline)}개 통과')
    output = ROOT / 'work/artifact-verification'
    output.mkdir(parents=True, exist_ok=True)
    tools = BRIDGE / 'android/android-sdk/build-tools/35.0.0'
    variants = [('노트10플러스', 'phone', {'arm64-v8a'}),
                ('PC용', 'pc', {'arm64-v8a', 'x86_64'})]
    for label, short, expected_abis in variants:
        source = DELIVERY / f'PXE_13차_v1.3_{label}.apk'
        target = output / f'{short}.apk'
        shutil.copyfile(source, target)
        if digest(source) != digest(target):
            raise RuntimeError('검사용 복사본의 해시 불일치')
        # Windows Android 도구의 한글 경로 제한을 피하되 납품본 해시를 대조합니다.
        ascii_target = BRIDGE / f'source/work/artifact-verification/{short}.apk'
        metadata = command([str(tools / 'aapt.exe'), 'dump', 'badging', str(ascii_target)])
        if not re.search(r"package: name='kr\.yunseok\.touchmacro'.*versionCode='13'.*versionName='1\.3\.0'", metadata):
            raise RuntimeError('APK 앱 ID 또는 버전 불일치')
        if "application-label:'PXE'" not in metadata:
            raise RuntimeError('APK 앱 이름 불일치')
        signed = command([str(BRIDGE / 'tools/jdk-17.0.20.1+1/bin/java.exe'),
                          '-Dfile.encoding=UTF-8', '-jar', str(tools / 'lib/apksigner.jar'),
                          'verify', '--print-certs', str(ascii_target)])
        certificates = re.findall(r'certificate SHA-256 digest: ([0-9a-fA-F]+)', signed)
        if certificates != [CERTIFICATE]:
            raise RuntimeError('기존 앱과 APK 서명 불일치')
        with zipfile.ZipFile(target) as archive:
            if archive.testzip() is not None:
                raise RuntimeError('APK 압축 데이터 손상')
            abis = {name.split('/')[1] for name in archive.namelist() if name.startswith('lib/') and name.endswith('.so')}
        if abis != expected_abis:
            raise RuntimeError(f'APK 지원 기기 불일치: {abis!r}')
        print(f'{label}: 앱 ID·PXE·13/1.3.0·기존 서명·ABI·CRC 통과 / SHA256 {digest(source)}')


if __name__ == '__main__':
    main()
