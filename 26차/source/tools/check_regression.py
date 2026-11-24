"""기존 순수 모델 테스트 전체를 실제 javac와 JUnit으로 실행합니다."""
import os
from pathlib import Path
import subprocess
import tempfile
import threading

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT.parents[1]
JDK = PROJECT / "work/tools/jdk-17.0.20.1+1/bin"
CACHE = Path.home() / ".gradle/caches/modules-2/files-2.1"


def jar(group, name):
    matches = sorted((CACHE / group / name).glob("*/*/*.jar"))
    if not matches:
        raise RuntimeError(f"검사 의존성 없음: {group}/{name}")
    return str(matches[-1])


def run(argv, timeout=23, require_success=False):
    process = subprocess.Popen(argv, cwd=ROOT, shell=False, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT)
    captured = bytearray()
    original = 0
    limit = 16000
    success = False
    failure = False

    def drain():
        nonlocal original, success, failure
        previous = b""
        while block := process.stdout.read(8192):
            original += len(block)
            captured.extend(block[:max(0, limit - len(captured))])
            scan = previous + block
            success |= b"OK (" in scan
            failure |= any(marker in scan for marker in (
                b"FAILURES!!!", b"INSTRUMENTATION_FAILED", b"INSTRUMENTATION_STATUS_CODE: -"))
            previous = scan[-128:]

    reader = threading.Thread(target=drain, daemon=True)
    reader.start()
    try:
        code = process.wait(timeout=timeout)
    except subprocess.TimeoutExpired:
        subprocess.run(["taskkill", "/PID", str(process.pid), "/T", "/F"],
                       shell=False, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                       timeout=3)
        raise RuntimeError("검사 제한 시간 초과: 프로세스 트리 종료")
    finally:
        reader.join(timeout=3)
    if reader.is_alive():
        raise RuntimeError("검사 출력 수집을 끝내지 못했습니다")
    raw = bytes(captured)
    # 출력 상한에 걸린 마지막 UTF-8 문자만 제거하며 원본 손상은 오류로 구분합니다.
    try:
        output = raw.decode("utf-8", errors="strict")
    except UnicodeDecodeError as error:
        if original > len(raw) and error.reason == "unexpected end of data":
            raw = raw[:error.start]
            output = raw.decode("utf-8", errors="strict")
        else:
            raise
    print(output, end="")
    print(f"로그: 원본 {original}바이트 / 캡처 {len(raw)}바이트 / 생략 {original > len(raw)}")
    if code:
        raise SystemExit(code)
    if require_success and (not success or failure):
        raise SystemExit("실제 테스트 성공 표식이 없거나 실패 상태가 있습니다")
    return output


def check(output):
    jars = [jar("junit", "junit"), jar("org.hamcrest", "hamcrest-core"),
            jar("org.json", "json"),
            str(PROJECT / "코드/NEXTools/android-sdk/platforms/android-35/android.jar")]
    classpath = os.pathsep.join(jars)
    package = "kr/yunseok/touchmacro"
    sources = sorted((ROOT / "app/src/main/java" / package).glob("*.java"))
    # Store.sample 검증은 Android Context 메서드를 호출하지 않으며 원본 Store를 컴파일합니다.
    sources = [p for p in sources if "import android" not in p.read_text(encoding="utf-8") or p.name == "Store.java"]
    tests = sorted((ROOT / "app/src/test/java" / package).glob("*Test.java"))
    if not tests:
        raise RuntimeError("회귀 테스트가 비어 있습니다")
    run([str(JDK / "javac.exe"), "-encoding", "UTF-8", "-cp", classpath,
         "-d", str(output), *map(str, sources + tests)])
    run([str(JDK / "java.exe"), "-Dfile.encoding=UTF-8", "-cp",
         str(output) + os.pathsep + classpath, "org.junit.runner.JUnitCore",
         *["kr.yunseok.touchmacro." + p.stem for p in tests]], require_success=True)
    print(f"검사 클래스 {len(tests)}개 / 원본 모델 파일 {len(sources)}개")


def main():
    work = ROOT / "work"
    work.mkdir(exist_ok=True)
    # 호출마다 비어 있는 별도 폴더를 써서 이전 클래스 파일의 성공 오판을 막습니다.
    with tempfile.TemporaryDirectory(prefix="pure-tests-", dir=work) as directory:
        output = Path(directory).resolve()
        output.relative_to(work.resolve())
        check(output)


if __name__ == "__main__":
    main()
