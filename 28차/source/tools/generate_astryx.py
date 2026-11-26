"""Rust 테마 컴파일러 결과를 실제 Android 리소스로 생성합니다."""
from pathlib import Path
import argparse
import json
import os
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
PALETTES = {
    'light': dict(accent='#245CE2', background='#F6F7F9', surface='#FFFFFF',
                  text='#161A22', muted='#596270', border='#DDE2E8'),
    'dark': dict(accent='#90B4FF', background='#101216', surface='#1A1D24',
                 text='#F3F5F9', muted='#A9B2C0', border='#323844'),
}

def generate(engine):
    resources = ET.Element('resources')
    for mode, palette in PALETTES.items():
        args = [str(engine), '--format', 'android', '--mode', mode]
        for key, value in palette.items():
            args += ['--' + key, value]
        result = subprocess.run(args, check=True, capture_output=True, encoding='utf-8', timeout=15)
        colors = ET.fromstring(result.stdout)
        expected = {'astryx_' + key: value for key, value in palette.items()}
        actual = {item.attrib['name']: item.text for item in colors}
        if actual != expected:
            raise ValueError('Rust 출력 팔레트가 지정한 PXE 색상과 다릅니다: ' + mode)
        for item in colors:
            item.set('name', item.attrib['name'] + '_' + mode)
            resources.append(item)
    ET.indent(resources, space='    ')
    output = ROOT / 'app/src/main/res/values/astryx_colors.xml'
    data = b'<?xml version="1.0" encoding="utf-8"?>\n' + ET.tostring(resources, encoding='utf-8') + b'\n'
    output.write_bytes(data)
    print('Rust Android 팔레트 2개 생성·원색 대조: ' + str(output))

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--engine', type=Path, default=Path(os.environ.get('PXE_ASTRYX_ENGINE',
        str(ROOT / 'theme-engine/astryx-theme.exe'))))
    args = parser.parse_args()
    generate(args.engine)
