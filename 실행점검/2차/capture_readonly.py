import datetime
import hashlib
import json
import pathlib
import struct
import subprocess
import sys
import threading
import time
import zlib

ROOT = pathlib.Path(__file__).resolve().parent
ADB = r"C:\Program Files\BlueStacks_nxt\HD-Adb.exe"
KST = datetime.timezone(datetime.timedelta(hours=9))
TASK = "bs-speed-five-20261021"
SESSION = "01a131ad-d167-7873-9aa6-4d81ff131c60"
DEVICES = [("한컴오피스", 5555), ("라이노", 5565), ("어도비", 5575), ("로직 프로", 5585), ("가상악기 1", 5595), ("가상악기 2", 5605)]


def now():
    return datetime.datetime.now(KST).isoformat(timespec="milliseconds")


def log(record):
    with (ROOT / "캡처명령기록.jsonl").open("a", encoding="utf-8", newline="\n") as output:
        output.write(json.dumps(record, ensure_ascii=False) + "\n")
    print(json.dumps(record, ensure_ascii=False), flush=True)


def run(argv, stdout_limit=8 * 1024 * 1024):
    started = now()
    started_clock = time.monotonic()
    process = subprocess.Popen(argv, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.PIPE, cwd=ROOT)
    chunks = {"stdout": [], "stderr": []}
    counts = {"stdout": 0, "stderr": 0}
    limits = {"stdout": stdout_limit, "stderr": 65536}
    overflow = threading.Event()

    def collect(stream, name):
        while True:
            part = stream.read(65536)
            if not part:
                return
            remaining = limits[name] - counts[name]
            counts[name] += len(part)
            if remaining > 0:
                chunks[name].append(part[:remaining])
            if counts[name] > limits[name]:
                overflow.set()
                return

    readers = [threading.Thread(target=collect, args=(process.stdout, "stdout"), daemon=True), threading.Thread(target=collect, args=(process.stderr, "stderr"), daemon=True)]
    for reader in readers:
        reader.start()
    timed_out = False
    while process.poll() is None:
        if overflow.is_set() or time.monotonic() - started_clock > 15:
            timed_out = not overflow.is_set()
            process.kill()
            break
        time.sleep(0.025)
    process.wait(timeout=2)
    for reader in readers:
        reader.join(timeout=2)
    output = b"".join(chunks["stdout"])
    error = b"".join(chunks["stderr"])
    record = {"start_kst": started, "end_kst": now(), "duration_seconds": round(time.monotonic() - started_clock, 3), "argv": argv, "cwd": str(ROOT), "exit_code": process.returncode, "original_stdout_bytes": counts["stdout"], "captured_stdout_bytes": len(output), "original_stderr_bytes": counts["stderr"], "captured_stderr_bytes": len(error), "truncated": overflow.is_set(), "timeout": timed_out, "stderr_utf8": error.decode("utf-8", errors="strict"), "session": SESSION, "task": TASK, "repair_used": 1, "app_source_setting_changes": 0}
    if timed_out or overflow.is_set() or process.returncode != 0:
        record["status"] = "FAILED_STOP"
        log(record)
        raise RuntimeError("명령 실행 실패")
    return output, record


def validate_png(data):
    if not data.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ValueError("PNG 서명이 올바르지 않습니다")
    offset = 8
    width = height = None
    ended = False
    while offset < len(data):
        if offset + 12 > len(data):
            raise ValueError("PNG 청크가 부족합니다")
        length = struct.unpack(">I", data[offset:offset + 4])[0]
        kind = data[offset + 4:offset + 8]
        end = offset + 12 + length
        if end > len(data):
            raise ValueError("PNG 데이터가 부족합니다")
        payload = data[offset + 8:offset + 8 + length]
        expected = struct.unpack(">I", data[offset + 8 + length:end])[0]
        if zlib.crc32(kind + payload) & 0xffffffff != expected:
            raise ValueError("PNG CRC가 일치하지 않습니다")
        if kind == b"IHDR":
            width, height = struct.unpack(">II", payload[:8])
        offset = end
        if kind == b"IEND":
            ended = True
            break
    if not ended or offset != len(data) or not width or not height:
        raise ValueError("PNG 끝부분 또는 크기가 올바르지 않습니다")
    return width, height


def main():
    if sys.argv[1:] == ["setup"]:
        commands = [[ADB, "-P", "5044", "start-server"]]
        commands.extend([ADB, "-P", "5044", "connect", f"127.0.0.1:{port}"] for _, port in DEVICES)
        for argv in commands:
            output, record = run(argv, 65536)
            record["stdout_utf8"] = output.decode("utf-8", errors="strict")
            if "connect" in argv and not ("connected to" in record["stdout_utf8"] or "already connected" in record["stdout_utf8"]):
                record["status"] = "FAILED_STOP"
                log(record)
                raise RuntimeError("연결 성공 문구가 확인되지 않았습니다")
            record["status"] = "PASS"
            log(record)
        return
    if len(sys.argv) != 3 or sys.argv[1] != "capture" or sys.argv[2] not in ["1", "2", "3", "4", "5"]:
        raise ValueError("허용된 요청은 setup 또는 capture 1~5입니다")
    round_number = int(sys.argv[2])
    for index, (name, port) in enumerate(DEVICES, start=1):
        argv = [ADB, "-P", "5044", "-s", f"127.0.0.1:{port}", "exec-out", "screencap", "-p"]
        output, record = run(argv)
        width, height = validate_png(output)
        filename = f"화면_{round_number:02d}_{index:02d}_{name.replace(' ', '')}.png"
        target = ROOT / filename
        with target.open("xb") as saved:
            saved.write(output)
        record.update({"status": "PASS", "round": round_number, "window_name": name, "serial": f"127.0.0.1:{port}", "image_file": filename, "width": width, "height": height, "png_crc_all_chunks": "PASS", "sha256": hashlib.sha256(output).hexdigest()})
        log(record)


try:
    main()
except Exception as error:
    log({"status": "FAILED_STOP", "at_kst": now(), "session": SESSION, "task": TASK, "argv": sys.argv, "cwd": str(ROOT), "error_type": type(error).__name__, "error": str(error), "repair_used": 1, "app_source_setting_changes": 0})
    sys.exit(1)
