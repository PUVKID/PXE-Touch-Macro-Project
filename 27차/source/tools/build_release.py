"""등록된 실제 도구 경로로 공용 제품을 빌드하고 변경 영향 검사를 실행합니다."""
from pathlib import Path
import os,subprocess,shutil,sys
from project_paths import PROJECT,CANONICAL,BUILD,SDK,JDK,GRADLE,SIGNING
if not (SIGNING/'key.properties').is_file():
    raise RuntimeError('기존 개인 서명 키가 필요합니다. 소스에는 키를 넣지 않습니다.')
env=os.environ.copy();env.update(JAVA_HOME=str(JDK),ANDROID_HOME=str(SDK),NEX_SIGNING_DIR=str(SIGNING))
subprocess.run([sys.executable,'-B','-X','utf8',str(CANONICAL/'tools/sync_build.py')],check=True,env=env)
subprocess.run([str(GRADLE),':app:assembleRelease',':app:testDebugUnitTest',':app:lintDebug','-PNEX_PC','--console=plain','--no-watch-fs','--no-daemon','--max-workers=1','--project-cache-dir','work/.gradle','-Dorg.gradle.jvmargs=-Xmx1536m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8'],cwd=BUILD,env=env,check=True)
shutil.copyfile(BUILD/'work/build/app/outputs/apk/release/app-release.apk',PROJECT/'27차/PXE_27차_v2.7_공용.apk')
