"""실제 Rust 출력·실패 경계와 Android에서 소비하는 팔레트를 대조합니다."""
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET
from generate_astryx import PALETTES, ROOT

engine = ROOT / 'theme-engine/astryx-theme.exe'
resources = ET.parse(ROOT / 'app/src/main/res/values/astryx_colors.xml').getroot()
actual = {item.attrib['name']: item.text for item in resources}
for mode, palette in PALETTES.items():
    args = [str(engine), '--format', 'android', '--mode', mode]
    for key, value in palette.items():
        args += ['--' + key, value]
    result = subprocess.run(args, capture_output=True, encoding='utf-8', check=True, timeout=15)
    rendered = {item.attrib['name'] + '_' + mode: item.text for item in ET.fromstring(result.stdout)}
    assert rendered == {name: actual[name] for name in rendered}, mode
    assert len(rendered) == 6, mode
before = (ROOT / 'app/src/main/res/values/astryx_colors.xml').read_bytes()
for args in [[], ['--format','android','--mode','wrong'],
             ['--format','android','--mode','light','--accent','#NOPE00'],
             ['--format','android','--mode','light','--mode','dark']]:
    result = subprocess.run([str(engine), *args], capture_output=True, encoding='utf-8', timeout=15)
    assert result.returncode == 2 and result.stderr.strip() and not result.stdout, args
assert before == (ROOT / 'app/src/main/res/values/astryx_colors.xml').read_bytes()
ui = (ROOT / 'app/src/main/java/kr/yunseok/touchmacro/Ui.java').read_text(encoding='utf-8')
for name in actual:
    assert 'R.color.' + name in ui, '소비하지 않는 Rust 리소스: ' + name
print('Rust 2팔레트 XML 일치·12색 실제 Ui 연결·4실패 경계·원본 리소스 보존: 통과')
