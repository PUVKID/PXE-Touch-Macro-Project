from pathlib import Path
import shutil
old=Path('D:/PXE13Build/source/work/android-user/avd/PXE13.avd')
root=Path('D:/PXE14Build/source/work/android-user/avd'); new=root/'PXE14.avd'
if new.exists(): raise SystemExit('새 QA 장치 폴더가 이미 존재합니다')
root.mkdir(parents=True,exist_ok=True)
shutil.copytree(old,new,ignore=shutil.ignore_patterns('*.lock','*.pid','snapshots','*.tmp'))
for name in ('config.ini','hardware-qemu.ini','emu-launch-params.txt'):
    p=new/name
    if p.exists():
        s=p.read_text(encoding='utf-8').replace('PXE13','PXE14')
        p.write_text(s,encoding='utf-8')
(root/'PXE14.ini').write_text('avd.ini.encoding=UTF-8\npath=D:\\PXE14Build\\source\\work\\android-user\\avd\\PXE14.avd\ntarget=android-31\n',encoding='utf-8')
print('이전 장치 변경 없이 PXE14 전용 시험 장치 복사 완료')