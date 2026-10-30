from pathlib import Path
import hashlib,json,shutil
root=Path.cwd();p=root/'work/UI단계_동결_수리5.json';archive=root/'work/UI단계_전체참조스냅샷_수리5.json'
shutil.copyfile(p,archive)
r=json.loads(p.read_text(encoding='utf-8'))
ui={'Ui.java','MainActivity.java','FloatingController.java','SetupWizard.java','SetupCoach.java','RunHud.java','RegionPicker.java'}
fixed={'app/build.gradle','빌드.ps1','빌드방법.txt','ROUTING-LESSONS.md','tools/check_artifacts.py','tools/check_device.py','tools/check_upgrade.py'}
tests={'PxeDesignReviewTest.java','PxeUpgradeTest.java','PxeRedesignTest.java'}
owned=[v for v in r.pop('source_files_excluding_metadata') if (v['path'] in fixed or v['path'].startswith('app/src/main/res/') or (v['path'].startswith('app/src/main/java/') and Path(v['path']).name in ui) or (v['path'].startswith('app/src/androidTest/') and Path(v['path']).name in tests))]
r.pop('source_bundle_sha256',None);r.pop('source_file_count',None)
r['ui_owned_files']=owned;r['ui_owned_file_count']=len(owned);r['ui_owned_bundle_sha256']=hashlib.sha256(json.dumps(owned,ensure_ascii=False,sort_keys=True,separators=(',',':')).encode('utf-8')).hexdigest()
r['snapshot_definition']='UI 소유7Java/리소스/버전·빌드/보정 helper/소유 UI·업그레이드 검사/ROUTING만의 상대경로·바이트·SHA 배열. 부모 docs/master/새 OCR 검사·엔진은 동결 소유에서 제외합니다. 중앙 sourceSnapshot/전체 최종동결이 아닙니다.'
r['protected_non_ui_observation']='21개 byte동일은 위 created_kst에 직접 대조한 UI 단계 관측입니다. 별도 deep의 후속 엔진 조사·수리 결과까지 포함하는 보증이 아닙니다.'
r['prior_reference_snapshot']=str(archive)
p.write_text(json.dumps(r,ensure_ascii=False,indent=2),encoding='utf-8')
(root/'work/디자인대조_최종_UI단계.txt').write_text('''PXE14 UI 단계 시안 대조

기준:14차/evidence/탐지홈_라이트다크_시안.png와 설정_동작_제어창_시안.png를 view_image로 확인했습니다.
실제:Android12 PXE14의1080×2280/density420 원본 PNG. UiAutomation 및 실제UI트리bounds로 동작한 ADB screencap이며 브라우저 검증으로 대체하지 않았습니다.

대조한 사항
1. 순백/잉크/청록·민트/선 색상, 생성 시안의 빛 번짐 대신 명세 flat색 적용.
2. 실제 큰 검색어와 찾을 글자 라벨, 탐지 영역 선택 행, 매크로·대기·새로고침 열린 행의 순서.
3. 앱 소유 글자의 나눔고딕 Bold 및 제목/본문/입력/버튼 크기 위계.
4. 원벡터 아이콘과 추가 일관 벡터의 의미/색/정렬, 로그의 목록 아이콘 교정.
5. 큰 회색 카드와 큰 pill 탐색 제거, 얇은 구분선/활성 밑줄, 동작 편집의 얕은 경계면.
6. 실제 서비스 진행·시간값, 키보드 표시중 탐색 가시/실제탭, 긴 문구 스크롤 후 주버튼147px 전체가시.
7. 기본 제어창/안내/영역/번호점/네이티브 체크박스·슬라이더48dp, 번호점 실제drag후 저장중심 일치.
8. 실제탭 진입180ms의 다른 alpha프레임과 종료1.0, 시스템animator scale0에서즉시1.0/translation0. 정지callback지연없음.

직접 view_image 확인: 라이트·다크 홈, 설정, 동작, 글자 IME, 제어창·번호점, 네이티브 설정 팝업, 오류 기록, 글자 동작 세부편집, 설정 하단, 긴 문구 스크롤을 포함한 대표 표면.
허용 차이: 기존 단축창, 시안 예시 대신 실제값, 기존 벡터 시스템, 다크 CTA 먹색 글자, 실제 화면에서 보조 행·설정 하단의 스크롤 접근, OS 상태줄/키보드. 앱 인식 테스트의 파란 캔버스는 기존 OCR 테스트 표면으로 보존했습니다.
위쪽 문구 대조: 시안의 기존 라벨/행/탭 순서를 유지하고 단축창은 기존기능 보존으로 명세에 허용되어 있습니다. 고정 가짜 진행값을 추가하지 않았습니다.
수동 최종 캡처16개:work/ui-final14-repair5. 이전 수동도16개:work/ui-final14. 앞선18개 표현은 집계 오류로 정정했습니다.
자동 UI PNG 및 과거진단파일 구분과 모든 경로/크기/SHA는work/캡처_목록_수리5.json을 따릅니다.

현재 결과는 UI 단계의 명세 대조와 구현자 직접 검사입니다. 새 사용자 OCR 제보는 별도 deep 조사 중이며 전체 최종완료·오류해결·독립 영수증이 아닙니다. 사용자 BlueStacks/노트10+실기기/24시간은 미검증입니다.
''',encoding='utf-8')
print(json.dumps({'ui_owned_files':len(owned),'ui_owned_bundle_sha256':r['ui_owned_bundle_sha256'],'manifest':str(p),'captures':str(root/'work/캡처_목록_수리5.json')},ensure_ascii=False))
