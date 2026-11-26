# 문서 수정 전체 비교

기준 커밋: `d17311d9d004b6af931f6a564295f8fc7d29df53`. 실행 코드·이전 원문 diff·배포 자산은 변경하지 않았습니다. 이 비교 문서 자체는 자기참조를 피하기 위해 diff에서 제외합니다.

~~~~~diff
diff --git "a/28\354\260\250/README.md" "b/28\354\260\250/README.md"
index 36af089..2e80364 100644
--- "a/28\354\260\250/README.md"
+++ "b/28\354\260\250/README.md"
@@ -1,5 +1,7 @@
 # PXE 28차 · 2.8.5
 
+**한국어** · [English · LEVEL 1~5](../en/docs/round28.md)
+
 28차 2.8.5는 첨부된 2.8.1과 비교해 확인한 OCR 누락을 수정했습니다. 줄 전체만 비교하던 처리를 독립 단어·문구 비교로 복원했습니다. 2.8.4의 회백색·차콜·회녹색 디자인, 매크로 설정과 하단 조작은 유지합니다.
 
 | 디자인 요소 | 2.8.5 유지 디자인 | 적용 범위 |
diff --git "a/28\354\260\250/\354\203\201\354\204\270\354\204\244\353\252\205.md" "b/28\354\260\250/\354\203\201\354\204\270\354\204\244\353\252\205.md"
index 4652153..9ce4e02 100644
--- "a/28\354\260\250/\354\203\201\354\204\270\354\204\244\353\252\205.md"
+++ "b/28\354\260\250/\354\203\201\354\204\270\354\204\244\353\252\205.md"
@@ -1,5 +1,7 @@
 # PXE 28차 상세 설명 · 2.8.5
 
+**한국어** · [English](../en/docs/round28.md)
+
 [LEVEL 1](상세설명.md#user-content-reading-level-1) · [LEVEL 2](상세설명.md#user-content-reading-level-2) · [LEVEL 3](상세설명.md#user-content-reading-level-3) · [LEVEL 4](상세설명.md#user-content-reading-level-4) · [LEVEL 5](상세설명.md#user-content-reading-level-5)
 
 <details>
@@ -9,29 +11,15 @@
 
 ## LEVEL 1 · 처음 사용하는 분
 
-28차 2.8.5는 첨부된 2.8.1과 비교해 확인한 OCR 누락을 수정했습니다. 줄 전체만 비교하던 처리를 독립 단어·문구 비교로 복원했습니다. 2.8.4의 회백색·차콜·회녹색 디자인, 매크로 설정과 하단 조작은 유지합니다.
-
-| 디자인 요소 | 2.8.5 유지 디자인 | 적용 범위 |
+| 입력 예 | 기대 동작 | 구분 |
 |---|---|---|
-| 색상 | 라이트 #F3F3EF / #FBFCF8 카드, 다크 #181C19 / #232924 카드 | 파랑 제거·입력·팝업·위젯 |
-| 포인트 | 라이트 #505C52 / 다크 #BCCDBB | 낮은 채도·선택/포커스/주요 동작 |
-| 글자 | 제목24sp·주요 입력20sp·일반 선택 14sp·동작제목15sp/설명13sp | 시스템 선택창 글자도14sp |
-| 동작 칸 | 전체 너비·최소112dp·제목/설명 각 2줄·더보기 64dp | 긴 내용은 줄임표, 편집창 전체 내용 유지 |
-| 제어 아이콘 | play/pause/stop 24dp 중심 배치·같은48dp 버튼 | 폰트 기호 대신 벡터·공통 색상 |
-| 로고 | 차콜 바탕·밝은 회백색의 둥근 P | 런처·상단·접힘·알림 동일 형상 |
-| 하단 창 | 1/15·화살표·이동·글자 변경·단축키 유지 | 하단 pause/stop 없음 |
+| Adobe 15에서 Adobe 찾기 | 독립 단어를 찾음 | 옆의 다른 단어가 있어도 허용 |
+| 붙어 있는 AdobePro | 일부 Adobe를 고르지 않음 | 부분 문자열 검색과 다름 |
+| 어도비·ADOBE | Adobe 입력과 다르게 처리 | 번역·대소문자 무시 없음 |
+| 미리보기 → 실행 | 영역과 글자를 먼저 확인 | OCR 오독·모든 화면 성공은 보장하지 않음 |
 
-OCR은 독립 단어·문구의 원문과 입력 글자가 완전히 같을 때 찾습니다. `Adobe 15`·`Adobe Pro` 안의 독립 단어 `Adobe`는 찾지만, 붙어 있는 `AdobePro`·`Adobe15`의 일부나 `어도비`·`ADOBE`는 고르지 않습니다. 여러 단어를 입력하면 문구 안 공백·대소문자도 그대로 비교합니다. OCR 요소를 원문과 정확히 대응할 수 없으면 줄·블록 전체 비교로 돌아가며 임의로 글자를 합치거나 바꾸지 않습니다. 화면 정보 방식은 노드의 text 전체와 비교하며 접근성 설명(contentDescription)은 사용하지 않습니다. 자동 방식은 화면 정보에서 못 찾으면 OCR을 시도하므로 두 방식의 단위 차이를 보완합니다. 저장된 포함·정규식·공백/대소문자 무시 옵션은 실행에 적용하지 않습니다. OCR 자체의 오독·화면 상태에 따른 누락까지 모두 없다고 보장하지는 않습니다.
+[공통 OCR 정책·기능·디자인 전체](#user-content-pxe28-current-contract) · [English · same level](../en/docs/round28.md#user-content-reading-level-1)
 
-| 기능 | 현재 동작 | 설정·경계 |
-|---|---|---|
-| 일반 매크로 | 녹화·터치·스와이프·곡선·멀티터치·대기·뒤로가기·분기·반복 | 저장한 순서·동작별 반복과 전체 반복 구분 |
-| 글자 인식 | 내장 한글 OCR·화면 정보·자동 선택 | Adobe와 어도비를 번역해 같은 글자로 바꾸지 않음 |
-| 목록 탐지 | 최대 6개 우선 영역에서 찾고 진입 → 등록 매크로 15회 → 복귀 | 반복·우선 영역·복귀·완료 후 대기·새로고침 시간 사용자 설정 |
-| 중복 방지 | 설정한 재등장 조건과 저장된 잠금 유지 | 글자를 못 읽은 오류와 실제 부재를 구분 |
-| 실행 제어 | 앱과 상단 단축창에서 일시정지·계속 실행·정지 | 하단 진행 위젯에는 해당 버튼 없음 |
-| 글자 단축키 | 등록·중복 억제·삭제·가로 목록 | 최대 30개, 글자·탐지 탭과 펼친 하단 위젯 공유 |
-| 저장·화면 | 매크로 가져오기/내보내기·테마·위젯 위치와 불투명도 | 앱을 삭제하지 않고 업데이트, 중요한 원본은 먼저 내보내기 |
 
 매크로는 터치·기다리기·뒤로가기를 순서대로 저장한 목록입니다. OCR은 화면 이미지에서 글자를 읽는 기능입니다. PXE에는 한글 OCR이 포함되어 화면을 외부로 보내 읽지 않습니다. 원하는 글자를 입력하고 영역을 정한 뒤 매크로를 선택합니다. 먼저 미리보기로 확인하고 실행하세요.
 
@@ -50,28 +38,15 @@ OCR은 독립 단어·문구의 원문과 입력 글자가 완전히 같을 때
 
 ## LEVEL 2 · 기능과 단계의 연결
 
-28차 2.8.5는 첨부된 2.8.1과 비교해 확인한 OCR 누락을 수정했습니다. 줄 전체만 비교하던 처리를 독립 단어·문구 비교로 복원했습니다. 2.8.4의 회백색·차콜·회녹색 디자인, 매크로 설정과 하단 조작은 유지합니다.
-
-| 디자인 요소 | 2.8.5 유지 디자인 | 적용 범위 |
+| 처리 흐름 | 2.8.5 동작 | 다음 단계 |
 |---|---|---|
-| 색상 | 라이트 #F3F3EF / #FBFCF8 카드, 다크 #181C19 / #232924 카드 | 파랑 제거·입력·팝업·위젯 |
-| 포인트 | 라이트 #505C52 / 다크 #BCCDBB | 낮은 채도·선택/포커스/주요 동작 |
-| 글자 | 제목24sp·주요 입력20sp·일반 선택 14sp·동작제목15sp/설명13sp | 시스템 선택창 글자도14sp |
-| 동작 칸 | 전체 너비·최소112dp·제목/설명 각 2줄·더보기 64dp | 긴 내용은 줄임표, 편집창 전체 내용 유지 |
-| 제어 아이콘 | play/pause/stop 24dp 중심 배치·같은48dp 버튼 | 폰트 기호 대신 벡터·공통 색상 |
-| 로고 | 차콜 바탕·밝은 회백색의 둥근 P | 런처·상단·접힘·알림 동일 형상 |
-| 하단 창 | 1/15·화살표·이동·글자 변경·단축키 유지 | 하단 pause/stop 없음 |
+| 화면 정보 | 노드 text 전체 원문 비교 | 자동 방식에서 못 찾으면 OCR |
+| OCR 후보 | 독립 요소·여러 요소 문구를 원문과 대응 | 단어 경계를 통과한 후보만 사용 |
+| 영역 적용 | 완성한 후보의 좌표를 비교 | AdobePro를 잘라 Adobe로 만들지 않음 |
+| 실행·글자 변경 | 현재 실행 대상과 단축키 저장을 연결 | 이전 조회가 끝날 때까지 새 조회 누적 금지 |
 
-OCR은 독립 단어·문구의 원문과 입력 글자가 완전히 같을 때 찾습니다. `Adobe 15`·`Adobe Pro` 안의 독립 단어 `Adobe`는 찾지만, 붙어 있는 `AdobePro`·`Adobe15`의 일부나 `어도비`·`ADOBE`는 고르지 않습니다. 여러 단어를 입력하면 문구 안 공백·대소문자도 그대로 비교합니다. OCR 요소를 원문과 정확히 대응할 수 없으면 줄·블록 전체 비교로 돌아가며 임의로 글자를 합치거나 바꾸지 않습니다. 화면 정보 방식은 노드의 text 전체와 비교하며 접근성 설명(contentDescription)은 사용하지 않습니다. 자동 방식은 화면 정보에서 못 찾으면 OCR을 시도하므로 두 방식의 단위 차이를 보완합니다. 저장된 포함·정규식·공백/대소문자 무시 옵션은 실행에 적용하지 않습니다. OCR 자체의 오독·화면 상태에 따른 누락까지 모두 없다고 보장하지는 않습니다.
+[공통 OCR 정책·기능·디자인 전체](#user-content-pxe28-current-contract) · [English · same level](../en/docs/round28.md#user-content-reading-level-2)
 
-| 수정 | 2.8.5 구현 | 유지한 부분 |
-|---|---|---|
-| 자동 이동 | 목록 하강·미인식 재시도 0.9초·화면 높이 22.5% 이동 | 아래로 당겨 새로고침은 거리 45%·시간 0.9초, 녹화 제스처는 저장값 그대로 |
-| 내부 디자인 | 26차의 단순한 행·구분선 기반, 동작 설명 전체 폭·입력/선택/팝업 간격 | 회백색·차콜·회녹색·나눔고딕700·라이트/다크 저장 |
-| 하단 편집 | 1/15 옆 화살표 → 글자 변경 → 적용 | 숫자 드래그·위치 저장·바깥 화면 터치 |
-| 빠른 글자 전환 | 가로 단축키를 누르면 첫 탐지 글자와 단독 글자 설정을 함께 저장 | 실행 중 탐지/단독 글자는 처음부터 새 실행, 일반 매크로 목록 자체는 변경하지 않음 |
-| 화면 조회 경합 | 글자 교체 직후 이전 조회가 끝날 때까지 취소 가능한 대기 | 조회를 누적하지 않음·실제 미응답 경계·늦은 결과 폐기 유지 |
-| 작은 진행 창 | 접힘 104×44dp, 펼침 너비 280dp·진행/시간/글자/단축키 | 하단 일시정지·정지 제거, 상단과 앱 제어 유지 |
 
 단축키 목록은 하나의 저장소를 공유합니다. 등록 버튼은 목록만 편집하고 단축키 선택은 첫 탐지 글자와 단독 글자 설정을 함께 저장합니다. 실행 중 탐지와 단독 글자 모드는 새 실행 세대로 교체하며 기존 매크로 동작을 재작성하지 않습니다.
 
@@ -92,29 +67,15 @@ OCR은 독립 단어·문구의 원문과 입력 글자가 완전히 같을 때
 
 ## LEVEL 3 · 설정·실행·결과 확인
 
-28차 2.8.5는 첨부된 2.8.1과 비교해 확인한 OCR 누락을 수정했습니다. 줄 전체만 비교하던 처리를 독립 단어·문구 비교로 복원했습니다. 2.8.4의 회백색·차콜·회녹색 디자인, 매크로 설정과 하단 조작은 유지합니다.
-
-| 디자인 요소 | 2.8.5 유지 디자인 | 적용 범위 |
+| 사용·확인 | 현재 기준 | 주의점 |
 |---|---|---|
-| 색상 | 라이트 #F3F3EF / #FBFCF8 카드, 다크 #181C19 / #232924 카드 | 파랑 제거·입력·팝업·위젯 |
-| 포인트 | 라이트 #505C52 / 다크 #BCCDBB | 낮은 채도·선택/포커스/주요 동작 |
-| 글자 | 제목24sp·주요 입력20sp·일반 선택 14sp·동작제목15sp/설명13sp | 시스템 선택창 글자도14sp |
-| 동작 칸 | 전체 너비·최소112dp·제목/설명 각 2줄·더보기 64dp | 긴 내용은 줄임표, 편집창 전체 내용 유지 |
-| 제어 아이콘 | play/pause/stop 24dp 중심 배치·같은48dp 버튼 | 폰트 기호 대신 벡터·공통 색상 |
-| 로고 | 차콜 바탕·밝은 회백색의 둥근 P | 런처·상단·접힘·알림 동일 형상 |
-| 하단 창 | 1/15·화살표·이동·글자 변경·단축키 유지 | 하단 pause/stop 없음 |
+| 설치 | 28차 2.8.5·Android 11+·ARM64/x86_64 | 기존 서명으로 업데이트, 중요한 매크로 내보내기 |
+| 설정 | 글자·최대 6영역·반복·대기·복귀 | 미리보기로 실제 화면을 먼저 확인 |
+| 하단 위젯 | 화살표에서 글자 변경·단축키 선택 | 일시정지·정지는 앱/상단 단축창 |
+| 검사 자료 | 112개 단위·린트 0·후보 4·실제 터치 2 | 게임 15회 시간이나 전체 기능 성공률과 구분 |
 
-OCR은 독립 단어·문구의 원문과 입력 글자가 완전히 같을 때 찾습니다. `Adobe 15`·`Adobe Pro` 안의 독립 단어 `Adobe`는 찾지만, 붙어 있는 `AdobePro`·`Adobe15`의 일부나 `어도비`·`ADOBE`는 고르지 않습니다. 여러 단어를 입력하면 문구 안 공백·대소문자도 그대로 비교합니다. OCR 요소를 원문과 정확히 대응할 수 없으면 줄·블록 전체 비교로 돌아가며 임의로 글자를 합치거나 바꾸지 않습니다. 화면 정보 방식은 노드의 text 전체와 비교하며 접근성 설명(contentDescription)은 사용하지 않습니다. 자동 방식은 화면 정보에서 못 찾으면 OCR을 시도하므로 두 방식의 단위 차이를 보완합니다. 저장된 포함·정규식·공백/대소문자 무시 옵션은 실행에 적용하지 않습니다. OCR 자체의 오독·화면 상태에 따른 누락까지 모두 없다고 보장하지는 않습니다.
+[공통 OCR 정책·기능·디자인 전체](#user-content-pxe28-current-contract) · [English · same level](../en/docs/round28.md#user-content-reading-level-3)
 
-| 기능 | 현재 동작 | 설정·경계 |
-|---|---|---|
-| 일반 매크로 | 녹화·터치·스와이프·곡선·멀티터치·대기·뒤로가기·분기·반복 | 저장한 순서·동작별 반복과 전체 반복 구분 |
-| 글자 인식 | 내장 한글 OCR·화면 정보·자동 선택 | Adobe와 어도비를 번역해 같은 글자로 바꾸지 않음 |
-| 목록 탐지 | 최대 6개 우선 영역에서 찾고 진입 → 등록 매크로 15회 → 복귀 | 반복·우선 영역·복귀·완료 후 대기·새로고침 시간 사용자 설정 |
-| 중복 방지 | 설정한 재등장 조건과 저장된 잠금 유지 | 글자를 못 읽은 오류와 실제 부재를 구분 |
-| 실행 제어 | 앱과 상단 단축창에서 일시정지·계속 실행·정지 | 하단 진행 위젯에는 해당 버튼 없음 |
-| 글자 단축키 | 등록·중복 억제·삭제·가로 목록 | 최대 30개, 글자·탐지 탭과 펼친 하단 위젯 공유 |
-| 저장·화면 | 매크로 가져오기/내보내기·테마·위젯 위치와 불투명도 | 앱을 삭제하지 않고 업데이트, 중요한 원본은 먼저 내보내기 |
 
 Android 11 이상과 ARM64/x86_64를 지원합니다. 같은 앱 ID와 기존 서명을 유지하므로 앱을 삭제하지 않고 업데이트 설치합니다. 실제 16차에서 28차까지 모든 사용자 데이터의 설치 전후 대조는 이번에 수행하지 않았으므로 중요한 매크로를 먼저 내보내 보관하세요.
 
@@ -135,28 +96,15 @@ Android 11 이상과 ARM64/x86_64를 지원합니다. 같은 앱 ID와 기존 
 
 ## LEVEL 4 · 모듈·저장·실패 경계
 
-28차 2.8.5는 첨부된 2.8.1과 비교해 확인한 OCR 누락을 수정했습니다. 줄 전체만 비교하던 처리를 독립 단어·문구 비교로 복원했습니다. 2.8.4의 회백색·차콜·회녹색 디자인, 매크로 설정과 하단 조작은 유지합니다.
-
-| 디자인 요소 | 2.8.5 유지 디자인 | 적용 범위 |
+| 구현 경계 | 역할 | 유지 조건 |
 |---|---|---|
-| 색상 | 라이트 #F3F3EF / #FBFCF8 카드, 다크 #181C19 / #232924 카드 | 파랑 제거·입력·팝업·위젯 |
-| 포인트 | 라이트 #505C52 / 다크 #BCCDBB | 낮은 채도·선택/포커스/주요 동작 |
-| 글자 | 제목24sp·주요 입력20sp·일반 선택 14sp·동작제목15sp/설명13sp | 시스템 선택창 글자도14sp |
-| 동작 칸 | 전체 너비·최소112dp·제목/설명 각 2줄·더보기 64dp | 긴 내용은 줄임표, 편집창 전체 내용 유지 |
-| 제어 아이콘 | play/pause/stop 24dp 중심 배치·같은48dp 버튼 | 폰트 기호 대신 벡터·공통 색상 |
-| 로고 | 차콜 바탕·밝은 회백색의 둥근 P | 런처·상단·접힘·알림 동일 형상 |
-| 하단 창 | 1/15·화살표·이동·글자 변경·단축키 유지 | 하단 pause/stop 없음 |
+| TextRules·OCR 후보 | 원문 위치·단어 경계와 문구 좌표 결합 | 추정 불가능하면 줄·블록 전체 비교 |
+| MacroService | 새 입력 검증 후 설정 저장·실행 세대 교체 | 잘못된 입력은 기존 세대와 설정 보존 |
+| QueryLane·readScreen | 이전 future의 실제 종료를 기다림 | 취소 확인·시간 제한·늦은 결과 폐기 유지 |
+| RunHud·네이티브 UI | 실제 실행 글자·공유 단축키·기존 팔레트 표시 | 저장/실행 대상이 바뀔 때만 목록 재구성 |
 
-OCR은 독립 단어·문구의 원문과 입력 글자가 완전히 같을 때 찾습니다. `Adobe 15`·`Adobe Pro` 안의 독립 단어 `Adobe`는 찾지만, 붙어 있는 `AdobePro`·`Adobe15`의 일부나 `어도비`·`ADOBE`는 고르지 않습니다. 여러 단어를 입력하면 문구 안 공백·대소문자도 그대로 비교합니다. OCR 요소를 원문과 정확히 대응할 수 없으면 줄·블록 전체 비교로 돌아가며 임의로 글자를 합치거나 바꾸지 않습니다. 화면 정보 방식은 노드의 text 전체와 비교하며 접근성 설명(contentDescription)은 사용하지 않습니다. 자동 방식은 화면 정보에서 못 찾으면 OCR을 시도하므로 두 방식의 단위 차이를 보완합니다. 저장된 포함·정규식·공백/대소문자 무시 옵션은 실행에 적용하지 않습니다. OCR 자체의 오독·화면 상태에 따른 누락까지 모두 없다고 보장하지는 않습니다.
+[공통 OCR 정책·기능·디자인 전체](#user-content-pxe28-current-contract) · [English · same level](../en/docs/round28.md#user-content-reading-level-4)
 
-| 수정 | 2.8.5 구현 | 유지한 부분 |
-|---|---|---|
-| 자동 이동 | 목록 하강·미인식 재시도 0.9초·화면 높이 22.5% 이동 | 아래로 당겨 새로고침은 거리 45%·시간 0.9초, 녹화 제스처는 저장값 그대로 |
-| 내부 디자인 | 26차의 단순한 행·구분선 기반, 동작 설명 전체 폭·입력/선택/팝업 간격 | 회백색·차콜·회녹색·나눔고딕700·라이트/다크 저장 |
-| 하단 편집 | 1/15 옆 화살표 → 글자 변경 → 적용 | 숫자 드래그·위치 저장·바깥 화면 터치 |
-| 빠른 글자 전환 | 가로 단축키를 누르면 첫 탐지 글자와 단독 글자 설정을 함께 저장 | 실행 중 탐지/단독 글자는 처음부터 새 실행, 일반 매크로 목록 자체는 변경하지 않음 |
-| 화면 조회 경합 | 글자 교체 직후 이전 조회가 끝날 때까지 취소 가능한 대기 | 조회를 누적하지 않음·실제 미응답 경계·늦은 결과 폐기 유지 |
-| 작은 진행 창 | 접힘 104×44dp, 펼침 너비 280dp·진행/시간/글자/단축키 | 하단 일시정지·정지 제거, 상단과 앱 제어 유지 |
 
 TargetWords는 JSON 등록 목록의 순서·중복·삭제와 공통 단축키 배치를 담당합니다. MacroService.replaceTargetWord는 새 WatchConfig와 단독 Step을 검증하고, 필요한 매크로를 해석한 뒤 word/watch_config를 한 편집으로 저장합니다. 실행 중인 탐지/단독 글자만 restartWatch/restart로 교체합니다. 잘못된 입력은 현재 실행 세대와 설정을 유지합니다.
 
@@ -179,6 +127,35 @@ MainActivity/Ui/styles는 일반 행의 중복 경계와 빈 보조 문구를 
 
 ## LEVEL 5 · 구현·재현·증거 한계
 
+| 검증 대상 | 근거 | 해석 한계 |
+|---|---|---|
+| 회귀 원인 | 같은 합성 Adobe 15의 2.8.4 누락·2.8.1 발견 | 임의 게임 화면의 정확도 측정 아님 |
+| 복구 확인 | 실제 MLKit 후보 4방법·자체 화면 인식/터치 2방법 | 후보 검사를 새 문구의 종단 터치로 합산하지 않음 |
+| 패키지 | 2.8.5/code28·v2 기존 서명·APK/ZIP 해시 | 이번 문서 정리는 제품·설치 파일을 바꾸지 않음 |
+| 운용·성능 | 기존 제어·캡처·백오프 경계 보존 | 장기·게임 15회 속도·99% 인식률 미검증 |
+
+[공통 OCR 정책·기능·디자인 전체](#user-content-pxe28-current-contract) · [English · same level](../en/docs/round28.md#user-content-reading-level-5)
+
+
+
+Macro·CapturePacer의 기존 정의는 보존합니다. Step은 완전 일치 표시와 구형 정규식 검증 경계를 수정했습니다. WatchConfig는 자동 SCROLL_DOWN 끝점이 바뀌었으므로 네 파일 모두가 16/26차와 동일하다는 이전 설명은 현재 적용하지 않습니다. 캡처 간격(API30 1100ms/API31+ 500ms), 캡처/OCR 동시 작업 제한, 백오프와 늦은 응답 폐기 경로는 변경하지 않습니다.
+
+자동 이동은 ScrollMotion.DURATION_MS=900과 START_Y=75, END_Y=52.5를 실제 목록 하강·미인식 재시도 제스처에 연결했습니다. PULL_DOWN은 30→75로 유지합니다. 녹화 SWIPE/GESTURE는 기존 Step의 duration/경로를 사용하는 실행 분기를 유지합니다. 실제 터치 DOWN/UP과 이동 거리를 별도 기기 검사로 확인합니다. 상수 단위 검사만으로 사용감을 검증했다고 확대하지 않습니다.
+
+검사 환경은 전용 Android 12/API31 x86_64 AVD, ADB5047이며 사용자 블루스택·게임과 기본 ADB 서버는 조작하지 않습니다. 단위·린트·기기 결과, 실제 화면과 최종 바이너리의 크기/SHA-256은 검증 결과와 파일정보에 연결합니다. 초기 실패·수정·검사 준비 오류와 중간 실행 종료는 보존하며 다른 실행의 시간을 합산하지 않습니다.
+
+Rust는 Windows 색상 생성기를 빌드 시 실제 호출하는 범위이며 Android OCR 엔진의 Rust 전환이 아닙니다. Graphify는 로컬 Java AST 관계 조사에 사용하며 그래프 수를 속도·정확도로 환산하지 않습니다. 모든 기능의 미래 무오류·수년 무중단·99% 인식률·사용자 게임의 완료 시간을 보장하지 않습니다.
+
+현재 검사 결과와 APK/ZIP 지문은 [검증 결과](검증_결과.md)와 [파일정보](파일정보.json)를 따릅니다. 장기 무중단·모든 기기 무오류·게임의 15회 완료 시간은 측정하지 않았습니다.
+
+[같은 수준 안내](상세설명.md#user-content-reading-level-5) · [직전 27차 같은 수준](../27차/상세설명.md#user-content-reading-level-5)
+
+</details>
+
+<a id="pxe28-current-contract"></a>
+
+## 공통 OCR 정책·기능·디자인
+
 28차 2.8.5는 첨부된 2.8.1과 비교해 확인한 OCR 누락을 수정했습니다. 줄 전체만 비교하던 처리를 독립 단어·문구 비교로 복원했습니다. 2.8.4의 회백색·차콜·회녹색 디자인, 매크로 설정과 하단 조작은 유지합니다.
 
 | 디자인 요소 | 2.8.5 유지 디자인 | 적용 범위 |
@@ -211,17 +188,3 @@ OCR은 독립 단어·문구의 원문과 입력 글자가 완전히 같을 때
 | 빠른 글자 전환 | 가로 단축키를 누르면 첫 탐지 글자와 단독 글자 설정을 함께 저장 | 실행 중 탐지/단독 글자는 처음부터 새 실행, 일반 매크로 목록 자체는 변경하지 않음 |
 | 화면 조회 경합 | 글자 교체 직후 이전 조회가 끝날 때까지 취소 가능한 대기 | 조회를 누적하지 않음·실제 미응답 경계·늦은 결과 폐기 유지 |
 | 작은 진행 창 | 접힘 104×44dp, 펼침 너비 280dp·진행/시간/글자/단축키 | 하단 일시정지·정지 제거, 상단과 앱 제어 유지 |
-
-Macro·CapturePacer의 기존 정의는 보존합니다. Step은 완전 일치 표시와 구형 정규식 검증 경계를 수정했습니다. WatchConfig는 자동 SCROLL_DOWN 끝점이 바뀌었으므로 네 파일 모두가 16/26차와 동일하다는 이전 설명은 현재 적용하지 않습니다. 캡처 간격(API30 1100ms/API31+ 500ms), 캡처/OCR 동시 작업 제한, 백오프와 늦은 응답 폐기 경로는 변경하지 않습니다.
-
-자동 이동은 ScrollMotion.DURATION_MS=900과 START_Y=75, END_Y=52.5를 실제 목록 하강·미인식 재시도 제스처에 연결했습니다. PULL_DOWN은 30→75로 유지합니다. 녹화 SWIPE/GESTURE는 기존 Step의 duration/경로를 사용하는 실행 분기를 유지합니다. 실제 터치 DOWN/UP과 이동 거리를 별도 기기 검사로 확인합니다. 상수 단위 검사만으로 사용감을 검증했다고 확대하지 않습니다.
-
-검사 환경은 전용 Android 12/API31 x86_64 AVD, ADB5047이며 사용자 블루스택·게임과 기본 ADB 서버는 조작하지 않습니다. 단위·린트·기기 결과, 실제 화면과 최종 바이너리의 크기/SHA-256은 검증 결과와 파일정보에 연결합니다. 초기 실패·수정·검사 준비 오류와 중간 실행 종료는 보존하며 다른 실행의 시간을 합산하지 않습니다.
-
-Rust는 Windows 색상 생성기를 빌드 시 실제 호출하는 범위이며 Android OCR 엔진의 Rust 전환이 아닙니다. Graphify는 로컬 Java AST 관계 조사에 사용하며 그래프 수를 속도·정확도로 환산하지 않습니다. 모든 기능의 미래 무오류·수년 무중단·99% 인식률·사용자 게임의 완료 시간을 보장하지 않습니다.
-
-현재 검사 결과와 APK/ZIP 지문은 [검증 결과](검증_결과.md)와 [파일정보](파일정보.json)를 따릅니다. 장기 무중단·모든 기기 무오류·게임의 15회 완료 시간은 측정하지 않았습니다.
-
-[같은 수준 안내](상세설명.md#user-content-reading-level-5) · [직전 27차 같은 수준](../27차/상세설명.md#user-content-reading-level-5)
-
-</details>
diff --git a/docs/changes/2026-11-27-pxe285-english-and-levels.md b/docs/changes/2026-11-27-pxe285-english-and-levels.md
new file mode 100644
index 0000000..df25358
--- /dev/null
+++ b/docs/changes/2026-11-27-pxe285-english-and-levels.md
@@ -0,0 +1,5 @@
+# PXE 2.8.5 영어 안내와 수준별 구성
+
+최신 28차 2.8.5의 영어 개요·LEVEL 1~5 설명과 같은 수준 왕복 링크를 추가했습니다. 기존 26차 영어 원문은 별도 경로에 그대로 보존하고 기본 최종 26차와 새 28차의 검증 범위를 구분했습니다. 상세 두 사본의 반복 정책·디자인·기능·변경 표는 각각 한 번 보존하고 수준별로 개념·흐름·실행·구현·검증 표를 정리했습니다. 코드·APK/ZIP·112개 검사·후보 4/터치 2 근거와 미검증 경계는 보존했습니다.
+
+[수정한 문서 전체 비교](2026-11-27-pxe285-english-and-levels-full.md)
diff --git "a/docs/versions/28\354\260\250.md" "b/docs/versions/28\354\260\250.md"
index 571110c..928050c 100644
--- "a/docs/versions/28\354\260\250.md"
+++ "b/docs/versions/28\354\260\250.md"
@@ -1,5 +1,7 @@
 # PXE 28차 상세 설명 · 2.8.5
 
+**한국어** · [English](../../en/docs/round28.md)
+
 [LEVEL 1](28차.md#user-content-reading-level-1) · [LEVEL 2](28차.md#user-content-reading-level-2) · [LEVEL 3](28차.md#user-content-reading-level-3) · [LEVEL 4](28차.md#user-content-reading-level-4) · [LEVEL 5](28차.md#user-content-reading-level-5)
 
 <details>
@@ -9,29 +11,15 @@
 
 ## LEVEL 1 · 처음 사용하는 분
 
-28차 2.8.5는 첨부된 2.8.1과 비교해 확인한 OCR 누락을 수정했습니다. 줄 전체만 비교하던 처리를 독립 단어·문구 비교로 복원했습니다. 2.8.4의 회백색·차콜·회녹색 디자인, 매크로 설정과 하단 조작은 유지합니다.
-
-| 디자인 요소 | 2.8.5 유지 디자인 | 적용 범위 |
+| 입력 예 | 기대 동작 | 구분 |
 |---|---|---|
-| 색상 | 라이트 #F3F3EF / #FBFCF8 카드, 다크 #181C19 / #232924 카드 | 파랑 제거·입력·팝업·위젯 |
-| 포인트 | 라이트 #505C52 / 다크 #BCCDBB | 낮은 채도·선택/포커스/주요 동작 |
-| 글자 | 제목24sp·주요 입력20sp·일반 선택 14sp·동작제목15sp/설명13sp | 시스템 선택창 글자도14sp |
-| 동작 칸 | 전체 너비·최소112dp·제목/설명 각 2줄·더보기 64dp | 긴 내용은 줄임표, 편집창 전체 내용 유지 |
-| 제어 아이콘 | play/pause/stop 24dp 중심 배치·같은48dp 버튼 | 폰트 기호 대신 벡터·공통 색상 |
-| 로고 | 차콜 바탕·밝은 회백색의 둥근 P | 런처·상단·접힘·알림 동일 형상 |
-| 하단 창 | 1/15·화살표·이동·글자 변경·단축키 유지 | 하단 pause/stop 없음 |
+| Adobe 15에서 Adobe 찾기 | 독립 단어를 찾음 | 옆의 다른 단어가 있어도 허용 |
+| 붙어 있는 AdobePro | 일부 Adobe를 고르지 않음 | 부분 문자열 검색과 다름 |
+| 어도비·ADOBE | Adobe 입력과 다르게 처리 | 번역·대소문자 무시 없음 |
+| 미리보기 → 실행 | 영역과 글자를 먼저 확인 | OCR 오독·모든 화면 성공은 보장하지 않음 |
 
-OCR은 독립 단어·문구의 원문과 입력 글자가 완전히 같을 때 찾습니다. `Adobe 15`·`Adobe Pro` 안의 독립 단어 `Adobe`는 찾지만, 붙어 있는 `AdobePro`·`Adobe15`의 일부나 `어도비`·`ADOBE`는 고르지 않습니다. 여러 단어를 입력하면 문구 안 공백·대소문자도 그대로 비교합니다. OCR 요소를 원문과 정확히 대응할 수 없으면 줄·블록 전체 비교로 돌아가며 임의로 글자를 합치거나 바꾸지 않습니다. 화면 정보 방식은 노드의 text 전체와 비교하며 접근성 설명(contentDescription)은 사용하지 않습니다. 자동 방식은 화면 정보에서 못 찾으면 OCR을 시도하므로 두 방식의 단위 차이를 보완합니다. 저장된 포함·정규식·공백/대소문자 무시 옵션은 실행에 적용하지 않습니다. OCR 자체의 오독·화면 상태에 따른 누락까지 모두 없다고 보장하지는 않습니다.
+[공통 OCR 정책·기능·디자인 전체](#user-content-pxe28-current-contract) · [English · same level](../../en/docs/round28.md#user-content-reading-level-1)
 
-| 기능 | 현재 동작 | 설정·경계 |
-|---|---|---|
-| 일반 매크로 | 녹화·터치·스와이프·곡선·멀티터치·대기·뒤로가기·분기·반복 | 저장한 순서·동작별 반복과 전체 반복 구분 |
-| 글자 인식 | 내장 한글 OCR·화면 정보·자동 선택 | Adobe와 어도비를 번역해 같은 글자로 바꾸지 않음 |
-| 목록 탐지 | 최대 6개 우선 영역에서 찾고 진입 → 등록 매크로 15회 → 복귀 | 반복·우선 영역·복귀·완료 후 대기·새로고침 시간 사용자 설정 |
-| 중복 방지 | 설정한 재등장 조건과 저장된 잠금 유지 | 글자를 못 읽은 오류와 실제 부재를 구분 |
-| 실행 제어 | 앱과 상단 단축창에서 일시정지·계속 실행·정지 | 하단 진행 위젯에는 해당 버튼 없음 |
-| 글자 단축키 | 등록·중복 억제·삭제·가로 목록 | 최대 30개, 글자·탐지 탭과 펼친 하단 위젯 공유 |
-| 저장·화면 | 매크로 가져오기/내보내기·테마·위젯 위치와 불투명도 | 앱을 삭제하지 않고 업데이트, 중요한 원본은 먼저 내보내기 |
 
 매크로는 터치·기다리기·뒤로가기를 순서대로 저장한 목록입니다. OCR은 화면 이미지에서 글자를 읽는 기능입니다. PXE에는 한글 OCR이 포함되어 화면을 외부로 보내 읽지 않습니다. 원하는 글자를 입력하고 영역을 정한 뒤 매크로를 선택합니다. 먼저 미리보기로 확인하고 실행하세요.
 
@@ -50,28 +38,15 @@ OCR은 독립 단어·문구의 원문과 입력 글자가 완전히 같을 때
 
 ## LEVEL 2 · 기능과 단계의 연결
 
-28차 2.8.5는 첨부된 2.8.1과 비교해 확인한 OCR 누락을 수정했습니다. 줄 전체만 비교하던 처리를 독립 단어·문구 비교로 복원했습니다. 2.8.4의 회백색·차콜·회녹색 디자인, 매크로 설정과 하단 조작은 유지합니다.
-
-| 디자인 요소 | 2.8.5 유지 디자인 | 적용 범위 |
+| 처리 흐름 | 2.8.5 동작 | 다음 단계 |
 |---|---|---|
-| 색상 | 라이트 #F3F3EF / #FBFCF8 카드, 다크 #181C19 / #232924 카드 | 파랑 제거·입력·팝업·위젯 |
-| 포인트 | 라이트 #505C52 / 다크 #BCCDBB | 낮은 채도·선택/포커스/주요 동작 |
-| 글자 | 제목24sp·주요 입력20sp·일반 선택 14sp·동작제목15sp/설명13sp | 시스템 선택창 글자도14sp |
-| 동작 칸 | 전체 너비·최소112dp·제목/설명 각 2줄·더보기 64dp | 긴 내용은 줄임표, 편집창 전체 내용 유지 |
-| 제어 아이콘 | play/pause/stop 24dp 중심 배치·같은48dp 버튼 | 폰트 기호 대신 벡터·공통 색상 |
-| 로고 | 차콜 바탕·밝은 회백색의 둥근 P | 런처·상단·접힘·알림 동일 형상 |
-| 하단 창 | 1/15·화살표·이동·글자 변경·단축키 유지 | 하단 pause/stop 없음 |
+| 화면 정보 | 노드 text 전체 원문 비교 | 자동 방식에서 못 찾으면 OCR |
+| OCR 후보 | 독립 요소·여러 요소 문구를 원문과 대응 | 단어 경계를 통과한 후보만 사용 |
+| 영역 적용 | 완성한 후보의 좌표를 비교 | AdobePro를 잘라 Adobe로 만들지 않음 |
+| 실행·글자 변경 | 현재 실행 대상과 단축키 저장을 연결 | 이전 조회가 끝날 때까지 새 조회 누적 금지 |
 
-OCR은 독립 단어·문구의 원문과 입력 글자가 완전히 같을 때 찾습니다. `Adobe 15`·`Adobe Pro` 안의 독립 단어 `Adobe`는 찾지만, 붙어 있는 `AdobePro`·`Adobe15`의 일부나 `어도비`·`ADOBE`는 고르지 않습니다. 여러 단어를 입력하면 문구 안 공백·대소문자도 그대로 비교합니다. OCR 요소를 원문과 정확히 대응할 수 없으면 줄·블록 전체 비교로 돌아가며 임의로 글자를 합치거나 바꾸지 않습니다. 화면 정보 방식은 노드의 text 전체와 비교하며 접근성 설명(contentDescription)은 사용하지 않습니다. 자동 방식은 화면 정보에서 못 찾으면 OCR을 시도하므로 두 방식의 단위 차이를 보완합니다. 저장된 포함·정규식·공백/대소문자 무시 옵션은 실행에 적용하지 않습니다. OCR 자체의 오독·화면 상태에 따른 누락까지 모두 없다고 보장하지는 않습니다.
+[공통 OCR 정책·기능·디자인 전체](#user-content-pxe28-current-contract) · [English · same level](../../en/docs/round28.md#user-content-reading-level-2)
 
-| 수정 | 2.8.5 구현 | 유지한 부분 |
-|---|---|---|
-| 자동 이동 | 목록 하강·미인식 재시도 0.9초·화면 높이 22.5% 이동 | 아래로 당겨 새로고침은 거리 45%·시간 0.9초, 녹화 제스처는 저장값 그대로 |
-| 내부 디자인 | 26차의 단순한 행·구분선 기반, 동작 설명 전체 폭·입력/선택/팝업 간격 | 회백색·차콜·회녹색·나눔고딕700·라이트/다크 저장 |
-| 하단 편집 | 1/15 옆 화살표 → 글자 변경 → 적용 | 숫자 드래그·위치 저장·바깥 화면 터치 |
-| 빠른 글자 전환 | 가로 단축키를 누르면 첫 탐지 글자와 단독 글자 설정을 함께 저장 | 실행 중 탐지/단독 글자는 처음부터 새 실행, 일반 매크로 목록 자체는 변경하지 않음 |
-| 화면 조회 경합 | 글자 교체 직후 이전 조회가 끝날 때까지 취소 가능한 대기 | 조회를 누적하지 않음·실제 미응답 경계·늦은 결과 폐기 유지 |
-| 작은 진행 창 | 접힘 104×44dp, 펼침 너비 280dp·진행/시간/글자/단축키 | 하단 일시정지·정지 제거, 상단과 앱 제어 유지 |
 
 단축키 목록은 하나의 저장소를 공유합니다. 등록 버튼은 목록만 편집하고 단축키 선택은 첫 탐지 글자와 단독 글자 설정을 함께 저장합니다. 실행 중 탐지와 단독 글자 모드는 새 실행 세대로 교체하며 기존 매크로 동작을 재작성하지 않습니다.
 
@@ -92,29 +67,15 @@ OCR은 독립 단어·문구의 원문과 입력 글자가 완전히 같을 때
 
 ## LEVEL 3 · 설정·실행·결과 확인
 
-28차 2.8.5는 첨부된 2.8.1과 비교해 확인한 OCR 누락을 수정했습니다. 줄 전체만 비교하던 처리를 독립 단어·문구 비교로 복원했습니다. 2.8.4의 회백색·차콜·회녹색 디자인, 매크로 설정과 하단 조작은 유지합니다.
-
-| 디자인 요소 | 2.8.5 유지 디자인 | 적용 범위 |
+| 사용·확인 | 현재 기준 | 주의점 |
 |---|---|---|
-| 색상 | 라이트 #F3F3EF / #FBFCF8 카드, 다크 #181C19 / #232924 카드 | 파랑 제거·입력·팝업·위젯 |
-| 포인트 | 라이트 #505C52 / 다크 #BCCDBB | 낮은 채도·선택/포커스/주요 동작 |
-| 글자 | 제목24sp·주요 입력20sp·일반 선택 14sp·동작제목15sp/설명13sp | 시스템 선택창 글자도14sp |
-| 동작 칸 | 전체 너비·최소112dp·제목/설명 각 2줄·더보기 64dp | 긴 내용은 줄임표, 편집창 전체 내용 유지 |
-| 제어 아이콘 | play/pause/stop 24dp 중심 배치·같은48dp 버튼 | 폰트 기호 대신 벡터·공통 색상 |
-| 로고 | 차콜 바탕·밝은 회백색의 둥근 P | 런처·상단·접힘·알림 동일 형상 |
-| 하단 창 | 1/15·화살표·이동·글자 변경·단축키 유지 | 하단 pause/stop 없음 |
+| 설치 | 28차 2.8.5·Android 11+·ARM64/x86_64 | 기존 서명으로 업데이트, 중요한 매크로 내보내기 |
+| 설정 | 글자·최대 6영역·반복·대기·복귀 | 미리보기로 실제 화면을 먼저 확인 |
+| 하단 위젯 | 화살표에서 글자 변경·단축키 선택 | 일시정지·정지는 앱/상단 단축창 |
+| 검사 자료 | 112개 단위·린트 0·후보 4·실제 터치 2 | 게임 15회 시간이나 전체 기능 성공률과 구분 |
 
-OCR은 독립 단어·문구의 원문과 입력 글자가 완전히 같을 때 찾습니다. `Adobe 15`·`Adobe Pro` 안의 독립 단어 `Adobe`는 찾지만, 붙어 있는 `AdobePro`·`Adobe15`의 일부나 `어도비`·`ADOBE`는 고르지 않습니다. 여러 단어를 입력하면 문구 안 공백·대소문자도 그대로 비교합니다. OCR 요소를 원문과 정확히 대응할 수 없으면 줄·블록 전체 비교로 돌아가며 임의로 글자를 합치거나 바꾸지 않습니다. 화면 정보 방식은 노드의 text 전체와 비교하며 접근성 설명(contentDescription)은 사용하지 않습니다. 자동 방식은 화면 정보에서 못 찾으면 OCR을 시도하므로 두 방식의 단위 차이를 보완합니다. 저장된 포함·정규식·공백/대소문자 무시 옵션은 실행에 적용하지 않습니다. OCR 자체의 오독·화면 상태에 따른 누락까지 모두 없다고 보장하지는 않습니다.
+[공통 OCR 정책·기능·디자인 전체](#user-content-pxe28-current-contract) · [English · same level](../../en/docs/round28.md#user-content-reading-level-3)
 
-| 기능 | 현재 동작 | 설정·경계 |
-|---|---|---|
-| 일반 매크로 | 녹화·터치·스와이프·곡선·멀티터치·대기·뒤로가기·분기·반복 | 저장한 순서·동작별 반복과 전체 반복 구분 |
-| 글자 인식 | 내장 한글 OCR·화면 정보·자동 선택 | Adobe와 어도비를 번역해 같은 글자로 바꾸지 않음 |
-| 목록 탐지 | 최대 6개 우선 영역에서 찾고 진입 → 등록 매크로 15회 → 복귀 | 반복·우선 영역·복귀·완료 후 대기·새로고침 시간 사용자 설정 |
-| 중복 방지 | 설정한 재등장 조건과 저장된 잠금 유지 | 글자를 못 읽은 오류와 실제 부재를 구분 |
-| 실행 제어 | 앱과 상단 단축창에서 일시정지·계속 실행·정지 | 하단 진행 위젯에는 해당 버튼 없음 |
-| 글자 단축키 | 등록·중복 억제·삭제·가로 목록 | 최대 30개, 글자·탐지 탭과 펼친 하단 위젯 공유 |
-| 저장·화면 | 매크로 가져오기/내보내기·테마·위젯 위치와 불투명도 | 앱을 삭제하지 않고 업데이트, 중요한 원본은 먼저 내보내기 |
 
 Android 11 이상과 ARM64/x86_64를 지원합니다. 같은 앱 ID와 기존 서명을 유지하므로 앱을 삭제하지 않고 업데이트 설치합니다. 실제 16차에서 28차까지 모든 사용자 데이터의 설치 전후 대조는 이번에 수행하지 않았으므로 중요한 매크로를 먼저 내보내 보관하세요.
 
@@ -135,28 +96,15 @@ Android 11 이상과 ARM64/x86_64를 지원합니다. 같은 앱 ID와 기존 
 
 ## LEVEL 4 · 모듈·저장·실패 경계
 
-28차 2.8.5는 첨부된 2.8.1과 비교해 확인한 OCR 누락을 수정했습니다. 줄 전체만 비교하던 처리를 독립 단어·문구 비교로 복원했습니다. 2.8.4의 회백색·차콜·회녹색 디자인, 매크로 설정과 하단 조작은 유지합니다.
-
-| 디자인 요소 | 2.8.5 유지 디자인 | 적용 범위 |
+| 구현 경계 | 역할 | 유지 조건 |
 |---|---|---|
-| 색상 | 라이트 #F3F3EF / #FBFCF8 카드, 다크 #181C19 / #232924 카드 | 파랑 제거·입력·팝업·위젯 |
-| 포인트 | 라이트 #505C52 / 다크 #BCCDBB | 낮은 채도·선택/포커스/주요 동작 |
-| 글자 | 제목24sp·주요 입력20sp·일반 선택 14sp·동작제목15sp/설명13sp | 시스템 선택창 글자도14sp |
-| 동작 칸 | 전체 너비·최소112dp·제목/설명 각 2줄·더보기 64dp | 긴 내용은 줄임표, 편집창 전체 내용 유지 |
-| 제어 아이콘 | play/pause/stop 24dp 중심 배치·같은48dp 버튼 | 폰트 기호 대신 벡터·공통 색상 |
-| 로고 | 차콜 바탕·밝은 회백색의 둥근 P | 런처·상단·접힘·알림 동일 형상 |
-| 하단 창 | 1/15·화살표·이동·글자 변경·단축키 유지 | 하단 pause/stop 없음 |
+| TextRules·OCR 후보 | 원문 위치·단어 경계와 문구 좌표 결합 | 추정 불가능하면 줄·블록 전체 비교 |
+| MacroService | 새 입력 검증 후 설정 저장·실행 세대 교체 | 잘못된 입력은 기존 세대와 설정 보존 |
+| QueryLane·readScreen | 이전 future의 실제 종료를 기다림 | 취소 확인·시간 제한·늦은 결과 폐기 유지 |
+| RunHud·네이티브 UI | 실제 실행 글자·공유 단축키·기존 팔레트 표시 | 저장/실행 대상이 바뀔 때만 목록 재구성 |
 
-OCR은 독립 단어·문구의 원문과 입력 글자가 완전히 같을 때 찾습니다. `Adobe 15`·`Adobe Pro` 안의 독립 단어 `Adobe`는 찾지만, 붙어 있는 `AdobePro`·`Adobe15`의 일부나 `어도비`·`ADOBE`는 고르지 않습니다. 여러 단어를 입력하면 문구 안 공백·대소문자도 그대로 비교합니다. OCR 요소를 원문과 정확히 대응할 수 없으면 줄·블록 전체 비교로 돌아가며 임의로 글자를 합치거나 바꾸지 않습니다. 화면 정보 방식은 노드의 text 전체와 비교하며 접근성 설명(contentDescription)은 사용하지 않습니다. 자동 방식은 화면 정보에서 못 찾으면 OCR을 시도하므로 두 방식의 단위 차이를 보완합니다. 저장된 포함·정규식·공백/대소문자 무시 옵션은 실행에 적용하지 않습니다. OCR 자체의 오독·화면 상태에 따른 누락까지 모두 없다고 보장하지는 않습니다.
+[공통 OCR 정책·기능·디자인 전체](#user-content-pxe28-current-contract) · [English · same level](../../en/docs/round28.md#user-content-reading-level-4)
 
-| 수정 | 2.8.5 구현 | 유지한 부분 |
-|---|---|---|
-| 자동 이동 | 목록 하강·미인식 재시도 0.9초·화면 높이 22.5% 이동 | 아래로 당겨 새로고침은 거리 45%·시간 0.9초, 녹화 제스처는 저장값 그대로 |
-| 내부 디자인 | 26차의 단순한 행·구분선 기반, 동작 설명 전체 폭·입력/선택/팝업 간격 | 회백색·차콜·회녹색·나눔고딕700·라이트/다크 저장 |
-| 하단 편집 | 1/15 옆 화살표 → 글자 변경 → 적용 | 숫자 드래그·위치 저장·바깥 화면 터치 |
-| 빠른 글자 전환 | 가로 단축키를 누르면 첫 탐지 글자와 단독 글자 설정을 함께 저장 | 실행 중 탐지/단독 글자는 처음부터 새 실행, 일반 매크로 목록 자체는 변경하지 않음 |
-| 화면 조회 경합 | 글자 교체 직후 이전 조회가 끝날 때까지 취소 가능한 대기 | 조회를 누적하지 않음·실제 미응답 경계·늦은 결과 폐기 유지 |
-| 작은 진행 창 | 접힘 104×44dp, 펼침 너비 280dp·진행/시간/글자/단축키 | 하단 일시정지·정지 제거, 상단과 앱 제어 유지 |
 
 TargetWords는 JSON 등록 목록의 순서·중복·삭제와 공통 단축키 배치를 담당합니다. MacroService.replaceTargetWord는 새 WatchConfig와 단독 Step을 검증하고, 필요한 매크로를 해석한 뒤 word/watch_config를 한 편집으로 저장합니다. 실행 중인 탐지/단독 글자만 restartWatch/restart로 교체합니다. 잘못된 입력은 현재 실행 세대와 설정을 유지합니다.
 
@@ -179,6 +127,35 @@ MainActivity/Ui/styles는 일반 행의 중복 경계와 빈 보조 문구를 
 
 ## LEVEL 5 · 구현·재현·증거 한계
 
+| 검증 대상 | 근거 | 해석 한계 |
+|---|---|---|
+| 회귀 원인 | 같은 합성 Adobe 15의 2.8.4 누락·2.8.1 발견 | 임의 게임 화면의 정확도 측정 아님 |
+| 복구 확인 | 실제 MLKit 후보 4방법·자체 화면 인식/터치 2방법 | 후보 검사를 새 문구의 종단 터치로 합산하지 않음 |
+| 패키지 | 2.8.5/code28·v2 기존 서명·APK/ZIP 해시 | 이번 문서 정리는 제품·설치 파일을 바꾸지 않음 |
+| 운용·성능 | 기존 제어·캡처·백오프 경계 보존 | 장기·게임 15회 속도·99% 인식률 미검증 |
+
+[공통 OCR 정책·기능·디자인 전체](#user-content-pxe28-current-contract) · [English · same level](../../en/docs/round28.md#user-content-reading-level-5)
+
+
+
+Macro·CapturePacer의 기존 정의는 보존합니다. Step은 완전 일치 표시와 구형 정규식 검증 경계를 수정했습니다. WatchConfig는 자동 SCROLL_DOWN 끝점이 바뀌었으므로 네 파일 모두가 16/26차와 동일하다는 이전 설명은 현재 적용하지 않습니다. 캡처 간격(API30 1100ms/API31+ 500ms), 캡처/OCR 동시 작업 제한, 백오프와 늦은 응답 폐기 경로는 변경하지 않습니다.
+
+자동 이동은 ScrollMotion.DURATION_MS=900과 START_Y=75, END_Y=52.5를 실제 목록 하강·미인식 재시도 제스처에 연결했습니다. PULL_DOWN은 30→75로 유지합니다. 녹화 SWIPE/GESTURE는 기존 Step의 duration/경로를 사용하는 실행 분기를 유지합니다. 실제 터치 DOWN/UP과 이동 거리를 별도 기기 검사로 확인합니다. 상수 단위 검사만으로 사용감을 검증했다고 확대하지 않습니다.
+
+검사 환경은 전용 Android 12/API31 x86_64 AVD, ADB5047이며 사용자 블루스택·게임과 기본 ADB 서버는 조작하지 않습니다. 단위·린트·기기 결과, 실제 화면과 최종 바이너리의 크기/SHA-256은 검증 결과와 파일정보에 연결합니다. 초기 실패·수정·검사 준비 오류와 중간 실행 종료는 보존하며 다른 실행의 시간을 합산하지 않습니다.
+
+Rust는 Windows 색상 생성기를 빌드 시 실제 호출하는 범위이며 Android OCR 엔진의 Rust 전환이 아닙니다. Graphify는 로컬 Java AST 관계 조사에 사용하며 그래프 수를 속도·정확도로 환산하지 않습니다. 모든 기능의 미래 무오류·수년 무중단·99% 인식률·사용자 게임의 완료 시간을 보장하지 않습니다.
+
+현재 검사 결과와 APK/ZIP 지문은 [검증 결과](../../28차/검증_결과.md)와 [파일정보](../../28차/파일정보.json)를 따릅니다. 장기 무중단·모든 기기 무오류·게임의 15회 완료 시간은 측정하지 않았습니다.
+
+[같은 수준 안내](../읽기안내/레벨5.md#user-content-reading-level-5) · [직전 27차 같은 수준](27차.md#user-content-reading-level-5)
+
+</details>
+
+<a id="pxe28-current-contract"></a>
+
+## 공통 OCR 정책·기능·디자인
+
 28차 2.8.5는 첨부된 2.8.1과 비교해 확인한 OCR 누락을 수정했습니다. 줄 전체만 비교하던 처리를 독립 단어·문구 비교로 복원했습니다. 2.8.4의 회백색·차콜·회녹색 디자인, 매크로 설정과 하단 조작은 유지합니다.
 
 | 디자인 요소 | 2.8.5 유지 디자인 | 적용 범위 |
@@ -211,17 +188,3 @@ OCR은 독립 단어·문구의 원문과 입력 글자가 완전히 같을 때
 | 빠른 글자 전환 | 가로 단축키를 누르면 첫 탐지 글자와 단독 글자 설정을 함께 저장 | 실행 중 탐지/단독 글자는 처음부터 새 실행, 일반 매크로 목록 자체는 변경하지 않음 |
 | 화면 조회 경합 | 글자 교체 직후 이전 조회가 끝날 때까지 취소 가능한 대기 | 조회를 누적하지 않음·실제 미응답 경계·늦은 결과 폐기 유지 |
 | 작은 진행 창 | 접힘 104×44dp, 펼침 너비 280dp·진행/시간/글자/단축키 | 하단 일시정지·정지 제거, 상단과 앱 제어 유지 |
-
-Macro·CapturePacer의 기존 정의는 보존합니다. Step은 완전 일치 표시와 구형 정규식 검증 경계를 수정했습니다. WatchConfig는 자동 SCROLL_DOWN 끝점이 바뀌었으므로 네 파일 모두가 16/26차와 동일하다는 이전 설명은 현재 적용하지 않습니다. 캡처 간격(API30 1100ms/API31+ 500ms), 캡처/OCR 동시 작업 제한, 백오프와 늦은 응답 폐기 경로는 변경하지 않습니다.
-
-자동 이동은 ScrollMotion.DURATION_MS=900과 START_Y=75, END_Y=52.5를 실제 목록 하강·미인식 재시도 제스처에 연결했습니다. PULL_DOWN은 30→75로 유지합니다. 녹화 SWIPE/GESTURE는 기존 Step의 duration/경로를 사용하는 실행 분기를 유지합니다. 실제 터치 DOWN/UP과 이동 거리를 별도 기기 검사로 확인합니다. 상수 단위 검사만으로 사용감을 검증했다고 확대하지 않습니다.
-
-검사 환경은 전용 Android 12/API31 x86_64 AVD, ADB5047이며 사용자 블루스택·게임과 기본 ADB 서버는 조작하지 않습니다. 단위·린트·기기 결과, 실제 화면과 최종 바이너리의 크기/SHA-256은 검증 결과와 파일정보에 연결합니다. 초기 실패·수정·검사 준비 오류와 중간 실행 종료는 보존하며 다른 실행의 시간을 합산하지 않습니다.
-
-Rust는 Windows 색상 생성기를 빌드 시 실제 호출하는 범위이며 Android OCR 엔진의 Rust 전환이 아닙니다. Graphify는 로컬 Java AST 관계 조사에 사용하며 그래프 수를 속도·정확도로 환산하지 않습니다. 모든 기능의 미래 무오류·수년 무중단·99% 인식률·사용자 게임의 완료 시간을 보장하지 않습니다.
-
-현재 검사 결과와 APK/ZIP 지문은 [검증 결과](../../28차/검증_결과.md)와 [파일정보](../../28차/파일정보.json)를 따릅니다. 장기 무중단·모든 기기 무오류·게임의 15회 완료 시간은 측정하지 않았습니다.
-
-[같은 수준 안내](../읽기안내/레벨5.md#user-content-reading-level-5) · [직전 27차 같은 수준](27차.md#user-content-reading-level-5)
-
-</details>
diff --git "a/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2501.md" "b/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2501.md"
index c544c63..38f939a 100644
--- "a/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2501.md"
+++ "b/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2501.md"
@@ -312,3 +312,5 @@ F64는 내부 숫자를 64 비트로 저장한다는 뜻이며 정확도가 64%
 
 
 </details>
+
+[28차 2.8.5 · English at LEVEL 1](../../en/docs/round28.md#user-content-reading-level-1)
diff --git "a/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2502.md" "b/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2502.md"
index a6db7f8..45d0209 100644
--- "a/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2502.md"
+++ "b/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2502.md"
@@ -307,3 +307,5 @@ future를 새로 만들어 다시 입력하는 방식이 아닙니다.
 
 
 </details>
+
+[28차 2.8.5 · English at LEVEL 2](../../en/docs/round28.md#user-content-reading-level-2)
diff --git "a/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2503.md" "b/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2503.md"
index 25414b8..b881489 100644
--- "a/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2503.md"
+++ "b/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2503.md"
@@ -315,3 +315,5 @@ ARM64와 x86_64를 지원하며 인터넷 권한 없이 동작합니다.
 
 
 </details>
+
+[28차 2.8.5 · English at LEVEL 3](../../en/docs/round28.md#user-content-reading-level-3)
diff --git "a/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2504.md" "b/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2504.md"
index 515741c..db029fd 100644
--- "a/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2504.md"
+++ "b/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2504.md"
@@ -304,3 +304,5 @@ MainActivity/Ui/styles는 일반 행의 중복 경계와 빈 보조 문구를 
 
 
 </details>
+
+[28차 2.8.5 · English at LEVEL 4](../../en/docs/round28.md#user-content-reading-level-4)
diff --git "a/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2505.md" "b/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2505.md"
index 6dfc4f6..3bd60d5 100644
--- "a/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2505.md"
+++ "b/docs/\354\235\275\352\270\260\354\225\210\353\202\264/\353\240\210\353\262\2505.md"
@@ -316,3 +316,5 @@ OCR 엔진 자체를 재학습하거나 범용 언어 모델을 탑재하는 구
 
 
 </details>
+
+[28차 2.8.5 · English at LEVEL 5](../../en/docs/round28.md#user-content-reading-level-5)
diff --git a/en/README-revision26.md b/en/README-revision26.md
new file mode 100644
index 0000000..9dfc662
--- /dev/null
+++ b/en/README-revision26.md
@@ -0,0 +1,189 @@
+<a name="-pxe-project"></a>
+
+Additional operational checks: [75 actual OCR operations and five cycles of entry, 15 repetitions, and return (Korean source)](../실행점검/26차_연속운용/README.md) — two new device checks were added without changing the product APK.
+<a id="pxe-project"></a>
+<!-- readme-header32:start -->
+<p align="center"><img src="../assets/readme/icon.svg" alt="" width="72" height="72" /></p>
+<h1 align="center" id="project-title">PXE Project</h1>
+<p align="center">An Android tool that reads screens and acts on conditions</p>
+<p align="center">
+  <a href="../README.md"><img src="../assets/readme/language-ko-link.svg" alt="한국어" width="126" height="32" /></a>
+  <a href="README.md"><img src="../assets/readme/language-en-active.svg" alt="English · current language" width="126" height="32" /></a>
+</p>
+<p align="center">
+  <a href="../docs/읽기안내/레벨1.md"><img src="../assets/readme/level-1.svg" alt="LEVEL 1 - Beginner (Korean source)" width="92" height="32" /></a>
+  <a href="../docs/읽기안내/레벨2.md"><img src="../assets/readme/level-2.svg" alt="LEVEL 2 - Explorer (Korean source)" width="92" height="32" /></a>
+  <a href="../docs/읽기안내/레벨3.md"><img src="../assets/readme/level-3.svg" alt="LEVEL 3 - Practitioner (Korean source)" width="92" height="32" /></a>
+  <a href="../docs/읽기안내/레벨4.md"><img src="../assets/readme/level-4.svg" alt="LEVEL 4 - Advanced (Korean source)" width="92" height="32" /></a>
+  <a href="../docs/읽기안내/레벨5.md"><img src="../assets/readme/level-5.svg" alt="LEVEL 5 - Expert (Korean source)" width="92" height="32" /></a>
+</p>
+<p align="center"><small>LEVEL links open the Korean source at the same reading level.</small></p>
+<picture>
+  <source media="(max-width: 600px) and (prefers-color-scheme: dark)" srcset="../assets/readme/flow-en-dark-mobile.svg" />
+  <source media="(max-width: 600px)" srcset="../assets/readme/flow-en-light-mobile.svg" />
+  <source media="(prefers-color-scheme: dark)" srcset="../assets/readme/flow-en-dark.svg" />
+  <source media="(prefers-color-scheme: light)" srcset="../assets/readme/flow-en-light.svg" />
+  <img src="../assets/readme/flow-en-light.svg" alt="Screen text · regions: User-defined text and screen regions → OCR · condition checks: Check conditions with offline Korean OCR → Touch · macro execution: Run touch, scroll and back actions" width="100%" />
+</picture>
+<!-- readme-header32:end -->
+
+
+An Android macro that checks user-defined text and regions to perform touches, scrolling, and back navigation.
+
+Current app: revision 26 · 2.6.0 · Android 11 or later · ARM64 / x86_64 · offline Korean OCR
+
+[User guide (Korean source)](../최종/사용안내.md) · [Revision 26 details (Korean source)](../docs/versions/26차.md#user-content-reading-level-1) · [Full source (Korean source)](../26차/source) · [Verification results (Korean source)](../26차/검증_결과.md)
+
+<a id="읽는-수준-선택"></a>
+## Choose a reading level
+
+| Level | What to read |
+| --- | --- |
+| [LEVEL 1 - Beginner (Korean source)](../docs/읽기안내/레벨1.md) | An accessible guide from installation to running |
+| [LEVEL 2 - Explorer (Korean source)](../docs/읽기안내/레벨2.md) | How detection and execution flow |
+| [LEVEL 3 - Practitioner (Korean source)](../docs/읽기안내/레벨3.md) | Settings, saving, and building |
+| [LEVEL 4 - Advanced (Korean source)](../docs/읽기안내/레벨4.md) | Implementation, errors, and cancellation |
+| [LEVEL 5 - Expert (Korean source)](../docs/읽기안내/레벨5.md) | Code, execution evidence, and full comparisons |
+
+<a id="27차-준비본--속도와-화면-정리"></a>
+## Revision 27 preparation build · speed and screen refinements
+
+The existing macros and offline OCR are retained, with refinements to capture waiting, status indicators, and light/dark colors. **Revision 26 remains the default final release.** The revision 27 60-minute check is not marked complete because a host reboot left no evidence of its ending.
+
+| Material | What was checked |
+|---|---|
+| [Revision 27 materials (Korean source)](../27차) · [User guide (Korean source)](../27차/사용안내.md) | The universal APK and full package are provided in the local revision 27 folder |
+| [Revision 27 verification results (Korean source)](../27차/검증_결과.md) | 106 unit tests, device checks for affected behavior, and a separate speed comparison; the long-duration assessment is incomplete |
+| [Revision 27 details (Korean source)](../docs/versions/27차.md#user-content-reading-level-1) · [Full comparison (Korean source)](../27차/전체변경_분할/안내.md) | Explanations at the same reading level, actual additions/modifications/deletions, and original values |
+
+On the dedicated Android 12 device, OCR touch tests of 15 operations × 5 segments averaged 10.2 seconds → 9.1 seconds, an **11.2% decrease**. This does not represent the completion time of an entire game macro or accuracy in all environments.
+
+<a id="26차--16차-기반-경량-인식"></a>
+## Revision 26 · lightweight recognition based on revision 16
+
+| Feature | Current behavior |
+| --- | --- |
+| Text recognition | Offline Korean OCR and screen information; Unicode whitespace comparison and prevention of false matches for empty search terms |
+| General macros | Recording, touch, gestures, waiting, conditional branches, back navigation, repetition, and saving |
+| List detection | Up to 6 priorities, repetition counts, duplicate prevention, waiting after completion, and refresh |
+| Execution controls | Ⅱ/▶, stop, move, collapse, opacity, and elapsed time |
+| Screen | Retains revision 16 light/dark modes and the existing design |
+| Slow window responses | Waits without accumulating queries and discards previous responses. Finite settings, stopping, and actual errors remain distinct |
+| Separate AI features | Demonstration training, action models, and ONNX features are excluded. OCR is retained |
+
+<a id="확인한-결과"></a>
+## Observed results
+
+| Item | Revision 26 result |
+| --- | --- |
+| Unit tests and lint | **106 passed · 0 lint errors · 0 warnings** |
+| Device checks of the final shrunk APK | **89 passed** · dedicated Android 12 virtual device |
+| Installer | **32,717,854 bytes** · includes both ABIs · retains the existing signature |
+| Size compared with the PC revision 16 build | **7.4% decrease** · does not indicate speed or recognition accuracy |
+
+The actual user's BlueStacks, game, Note10+, and operation of 6 instances simultaneously were not operated or measured in this round. [Verification results (Korean source)](../26차/검증_결과.md) separate the evidence for each feature from the unverified scope. The APK and full package ZIP are provided in the local revision 26 and final folders; binaries are not uploaded to GitHub.
+
+<a id="빠른-시작"></a>
+## Quick start
+
+1. Install the universal APK as an update over the existing app and enable accessibility and display-over-other-apps permissions.
+2. Follow the guide to define text, regions, and macros, then save them by name.
+3. Set the macro to detect, repetitions, priority regions, return timing, and refresh timing.
+4. After starting, control execution through Ⅱ/▶, stop, and progress/error logs.
+
+<a id="current-24-final"></a>
+[Previous revision 24 description (Korean source)](../docs/versions/24차.md) · [Revision documentation criteria (Korean source)](../docs/차수별_문서작성기준.md)
+
+[Complete revision index (Korean source)](../docs/REVISION_INDEX.md)
+
+<a id="전체-차수"></a>
+## All revisions
+
+Each revision description covers that revision's features, settings, and results. Comparisons with the preceding revision appear under **Changes and differences** in each document.
+
+<details>
+<summary>Expand all revisions and materials</summary>
+
+| Revision | Main changes | Materials |
+| --- | --- | --- |
+| Base | The editing, service, and control sources in NEXSource2 and the partial wrappers in NEXSource3 differ in completeness. | [Material folder (Korean source)](../코드) · [Details at the same level (Korean source)](../docs/versions/기본.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/기본.md) |
+| 1 | Connects MainActivity editing, Store JSON saving, Step validation, and MacroService gestures. | [Material folder (Korean source)](../1차) · [Details at the same level (Korean source)](../docs/versions/1차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/1차.md) |
+| 2 | Compares text actions, OCR preview JSON, usage instructions, and the verification records from that time. | [Material folder (Korean source)](../2차) · [Details at the same level (Korean source)](../docs/versions/2차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/2차.md) |
+| 3 | Connects FloatingController and GesturePaths to separate controls, recording, curves, and multi-touch configuration. | [Material folder (Korean source)](../3차) · [Details at the same level (Korean source)](../docs/versions/3차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/3차.md) |
+| 4 | Refresh, text detection, selected macros, and return form the Watch flow; regions and the notification icon also change. | [Material folder (Korean source)](../4차) · [Details at the same level (Korean source)](../docs/versions/4차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/4차.md) |
+| 5 | The default 15 detection repetitions are not multiplied by the saved macro's full repetition count; WatchConfig is separated from the original. | [Material folder (Korean source)](../5차) · [Details at the same level (Korean source)](../docs/versions/5차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/5차.md) |
+| 6 | Configures up to 6 non-overlapping priority regions, the target app, duplicate locking, and post-completion waiting, with temporary recording for preparation. | [Material folder (Korean source)](../6차) · [Details at the same level (Korean source)](../docs/versions/6차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/6차.md) |
+| 7 | Prepared to run 5 Nox instances simultaneously, but did not start because the program could not be found and disk-space conditions were not met at that time. | [Material folder (Korean source)](../7차) · [Details at the same level (Korean source)](../docs/revision-audit/7차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/7차.md) |
+| 8 | Connects QueryLane screen reading with the response paths in ReaderResponsivenessTest. | [Material folder (Korean source)](../8차) · [Details at the same level (Korean source)](../docs/versions/8차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/8차.md) |
+| 9 | Floating playback does not start without settings; CapturePacer and CaptureRecoveryTest handle late captures and errors. | [Material folder (Korean source)](../9차) · [Details at the same level (Korean source)](../docs/versions/9차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/9차.md) |
+| 10 | Distinguishes the MainActivity bottom navigation, card UI, and PXE name changes from the original behavior. | [Material folder (Korean source)](../10차) · [Details at the same level (Korean source)](../docs/versions/10차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/10차.md) |
+| 11 | Covers the AUTO fallback to OCR when accessibility reading is slow, alongside pure regression, UTF-8, and device tools. | [Material folder (Korean source)](../11차) · [Details at the same level (Korean source)](../docs/versions/11차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/11차.md) |
+| 12 | Distinguishes indefinite waiting for text touch/wait from the lifetime of responses arriving from a previous run or after a pause. | [Material folder (Korean source)](../12차) · [Details at the same level (Korean source)](../docs/versions/12차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/12차.md) |
+| 13 | Manages stopping from a collapsed panel, disconnection, at most 2 unfinished captures, and one actual OCR Task. | [Material folder (Korean source)](../13차) · [Details at the same level (Korean source)](../docs/versions/13차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/13차.md) |
+| 14 | Changes the teal UI, 48dp targets, motion settings, and the return to the original waitForever behavior when TEXT_TAP text disappears. | [Material folder (Korean source)](../14차) · [Details at the same level (Korean source)](../docs/versions/14차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/14차.md) |
+| 15 | Indefinite waiting, skipping extra post-recognition waiting, and immediate list entry have different scopes.<br>Immediate entry is off by default. | [Material folder (Korean source)](../15차) · [Details at the same level (Korean source)](../docs/versions/15차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/15차.md) |
+| 16 | The default-off adaptiveWait applies only to adjacent conditions using the same recognition method and region, when the next text was confirmed absent beforehand. | [Material folder (Korean source)](../16차) · [Details at the same level (Korean source)](../docs/versions/16차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/16차.md) |
+| 17 | Retains a single query, rechecking a fresh screen, retrying completed OCR, and 0.25–2 second intervals, while distinguishing actual disconnection. | [Material folder (Korean source)](../17차) · [Details at the same level (Korean source)](../docs/versions/17차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/17차.md) |
+| 18 | Increments the count only after actual completion, separates 7/15 completed from the 8th run in progress, and displays stage and status text in addition to color. | [Material folder (Korean source)](../18차) · [Details at the same level (Korean source)](../docs/versions/18차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/18차.md) |
+| 19 | Prevents stale coordinates, unnecessary back navigation, and duplicate releases; rechecks the target app while retaining previous completion counts. | [Material folder (Korean source)](../19차) · [Details at the same level (Korean source)](../docs/versions/19차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/19차.md) |
+| Execution check 2 | Checks PNG CRC, width, height, SHA, and command records for the preserved capture tool. | [Material folder (Korean source)](../실행점검/2차) · [Details at the same level (Korean source)](../docs/versions/실행점검-2차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/실행점검-2차.md) |
+| `최종.json` | Reads macro data containing text, coordinates, retries, waiting, and scrolling conditions. | [Material folder (Korean source)](../docs/materials/022-최종설정) · [Original file (Korean source)](../최종.json) · [Details at the same level (Korean source)](../docs/materials/022-최종설정/README.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/versions/최종설정.md) |
+| Unique working materials | Distinguishes ZIP path, link, encryption, and size checks from identical-byte or BOM/newline correspondence, preserving unique sources. | [Material folder (Korean source)](../보존자료/작업소스) · [Details at the same level (Korean source)](../docs/SOURCE_MATERIALS.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/REVISION_AUDIT.md) |
+| 20 · test tools | Observes OCR, 15 repetitions, absence and reappearance, and duplicate blocking, comparing host/device times and generations. | [Material folder (Korean source)](../20차) · [Details at the same level (Korean source)](../docs/versions/20차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/20차.md) |
+| 21 | Connects absence of the checked text, priority for return text, restarting from the beginning for new text, and blocking late responses from an old generation. | [Material folder (Korean source)](../21차) · [Details at the same level (Korean source)](../docs/versions/21차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/21차.md) |
+| 22 | Improves movement/collapse of the small progress window and waiting flows, separating public source from operational materials. | [Material folder (Korean source)](../22차) · [Details at the same level (Korean source)](../docs/versions/22차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/22차.md) |
+| 23 | Binds the F64 MLP with OCR 584 → 32 → K, 2 demonstrations, 192 training steps, 1 automatic touch, 0 inputs after ending, and 64 after import to the version. | [Material folder (Korean source)](../23차) · [Details at the same level (Korean source)](../docs/versions/23차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/23차.md) |
+| 24 | Distinguishes the 1160-dimensional OCR + RGB policy, legacy 584 compatibility, waiting on the same future, control-window repairs, and the 280 passing unit tests in 44 classes from successful build 13. | [Material folder (Korean source)](../24차) · [Details at the same level (Korean source)](../docs/versions/24차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/24차.md) |
+| 27 · preparation build | Capture waiting, status indicators, and color refinements; long-duration assessment incomplete. | [Material folder (Korean source)](../27차) · [Details at the same level (Korean source)](../docs/versions/27차.md#user-content-reading-level-1) · [Full comparison (Korean source)](../docs/revision-audit/27차.md) |
+| 26 | Uses revision 16 basic macros and UI as its baseline, excludes separate AI, and improves text comparison, window responses, and shrinking. | [Material folder (Korean source)](../26차) · [Details at the same level (Korean source)](../docs/versions/26차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/26차.md) |
+| Final | Revision 26 universal APK, full source package ZIP, and user guide. | [Material folder (Korean source)](../최종) · [Details at the same level (Korean source)](../최종/README.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/최종.md) |
+
+</details>
+
+<a id="-23차--선택형-오프라인-시범-학습"></a>
+
+<a id="implementation-details"></a>
+
+<a id="-편집인식제스처저장-모듈"></a>
+
+<a id="overview"></a>
+
+<a id="latest-changes"></a>
+
+<a id="-이전-14차의-변화와-차수-탐색"></a>
+
+<a id="-차수-바로가기"></a>
+
+<a id="preview"></a>
+
+<a id="-입력처리출력"></a>
+
+<a id="introduction"></a>
+
+<a id="-프로젝트-소개"></a>
+
+<a id="-저장소-구성"></a>
+
+<a id="implementation"></a>
+
+<a id="-주요-기술과-실행-흐름"></a>
+
+<a id="-저장인식-요청의-경계"></a>
+
+<a id="️-차수별로-달라진-동작"></a>
+
+<a id="-기록에-남는-정보"></a>
+
+<a id="-빠른-시작"></a>
+
+<a id="data-settings"></a>
+
+<a id="-데이터와-동작-구성"></a>
+
+<a id="troubleshooting"></a>
+
+<a id="️-문제-해결"></a>
+
+<a id="-모든-차수보관본과-전체-차이"></a>
+
+<a id="️-차수별-화면과-검증-이미지"></a>
diff --git a/en/README.md b/en/README.md
index 9dfc662..f9ee851 100644
--- a/en/README.md
+++ b/en/README.md
@@ -1,6 +1,5 @@
 <a name="-pxe-project"></a>
 
-Additional operational checks: [75 actual OCR operations and five cycles of entry, 15 repetitions, and return (Korean source)](../실행점검/26차_연속운용/README.md) — two new device checks were added without changing the product APK.
 <a id="pxe-project"></a>
 <!-- readme-header32:start -->
 <p align="center"><img src="../assets/readme/icon.svg" alt="" width="72" height="72" /></p>
@@ -11,13 +10,13 @@ Additional operational checks: [75 actual OCR operations and five cycles of entr
   <a href="README.md"><img src="../assets/readme/language-en-active.svg" alt="English · current language" width="126" height="32" /></a>
 </p>
 <p align="center">
-  <a href="../docs/읽기안내/레벨1.md"><img src="../assets/readme/level-1.svg" alt="LEVEL 1 - Beginner (Korean source)" width="92" height="32" /></a>
-  <a href="../docs/읽기안내/레벨2.md"><img src="../assets/readme/level-2.svg" alt="LEVEL 2 - Explorer (Korean source)" width="92" height="32" /></a>
-  <a href="../docs/읽기안내/레벨3.md"><img src="../assets/readme/level-3.svg" alt="LEVEL 3 - Practitioner (Korean source)" width="92" height="32" /></a>
-  <a href="../docs/읽기안내/레벨4.md"><img src="../assets/readme/level-4.svg" alt="LEVEL 4 - Advanced (Korean source)" width="92" height="32" /></a>
-  <a href="../docs/읽기안내/레벨5.md"><img src="../assets/readme/level-5.svg" alt="LEVEL 5 - Expert (Korean source)" width="92" height="32" /></a>
+  <a href="docs/round28.md#user-content-reading-level-1"><img src="../assets/readme/level-1.svg" alt="LEVEL 1 - Beginner" width="92" height="32" /></a>
+  <a href="docs/round28.md#user-content-reading-level-2"><img src="../assets/readme/level-2.svg" alt="LEVEL 2 - Explorer" width="92" height="32" /></a>
+  <a href="docs/round28.md#user-content-reading-level-3"><img src="../assets/readme/level-3.svg" alt="LEVEL 3 - Practitioner" width="92" height="32" /></a>
+  <a href="docs/round28.md#user-content-reading-level-4"><img src="../assets/readme/level-4.svg" alt="LEVEL 4 - Advanced" width="92" height="32" /></a>
+  <a href="docs/round28.md#user-content-reading-level-5"><img src="../assets/readme/level-5.svg" alt="LEVEL 5 - Expert" width="92" height="32" /></a>
 </p>
-<p align="center"><small>LEVEL links open the Korean source at the same reading level.</small></p>
+<p align="center"><small>LEVEL links open the English explanation at the selected reading level.</small></p>
 <picture>
   <source media="(max-width: 600px) and (prefers-color-scheme: dark)" srcset="../assets/readme/flow-en-dark-mobile.svg" />
   <source media="(max-width: 600px)" srcset="../assets/readme/flow-en-light-mobile.svg" />
@@ -27,118 +26,27 @@ Additional operational checks: [75 actual OCR operations and five cycles of entr
 </picture>
 <!-- readme-header32:end -->
 
+<a id="overview"></a>
 
-An Android macro that checks user-defined text and regions to perform touches, scrolling, and back navigation.
-
-Current app: revision 26 · 2.6.0 · Android 11 or later · ARM64 / x86_64 · offline Korean OCR
-
-[User guide (Korean source)](../최종/사용안내.md) · [Revision 26 details (Korean source)](../docs/versions/26차.md#user-content-reading-level-1) · [Full source (Korean source)](../26차/source) · [Verification results (Korean source)](../26차/검증_결과.md)
+<a id="introduction"></a>
 
-<a id="읽는-수준-선택"></a>
-## Choose a reading level
+<a id="-프로젝트-소개"></a>
 
-| Level | What to read |
-| --- | --- |
-| [LEVEL 1 - Beginner (Korean source)](../docs/읽기안내/레벨1.md) | An accessible guide from installation to running |
-| [LEVEL 2 - Explorer (Korean source)](../docs/읽기안내/레벨2.md) | How detection and execution flow |
-| [LEVEL 3 - Practitioner (Korean source)](../docs/읽기안내/레벨3.md) | Settings, saving, and building |
-| [LEVEL 4 - Advanced (Korean source)](../docs/읽기안내/레벨4.md) | Implementation, errors, and cancellation |
-| [LEVEL 5 - Expert (Korean source)](../docs/읽기안내/레벨5.md) | Code, execution evidence, and full comparisons |
+An Android tool that checks text and regions, then runs touches, gestures, waits and back navigation.
 
-<a id="27차-준비본--속도와-화면-정리"></a>
-## Revision 27 preparation build · speed and screen refinements
+**Latest revision: 28 · 2.8.5 · Android 11+ · ARM64 / x86_64 · offline Korean OCR.** The existing default final revision 26 is preserved. Revision 28 adds the independently checked word/phrase recovery; it is not a new long-duration or game-speed certification.
 
-The existing macros and offline OCR are retained, with refinements to capture waiting, status indicators, and light/dark colors. **Revision 26 remains the default final release.** The revision 27 60-minute check is not marked complete because a host reboot left no evidence of its ending.
+<a id="읽는-수준-선택"></a>
 
-| Material | What was checked |
+| Reading level | Start in English |
 |---|---|
-| [Revision 27 materials (Korean source)](../27차) · [User guide (Korean source)](../27차/사용안내.md) | The universal APK and full package are provided in the local revision 27 folder |
-| [Revision 27 verification results (Korean source)](../27차/검증_결과.md) | 106 unit tests, device checks for affected behavior, and a separate speed comparison; the long-duration assessment is incomplete |
-| [Revision 27 details (Korean source)](../docs/versions/27차.md#user-content-reading-level-1) · [Full comparison (Korean source)](../27차/전체변경_분할/안내.md) | Explanations at the same reading level, actual additions/modifications/deletions, and original values |
-
-On the dedicated Android 12 device, OCR touch tests of 15 operations × 5 segments averaged 10.2 seconds → 9.1 seconds, an **11.2% decrease**. This does not represent the completion time of an entire game macro or accuracy in all environments.
-
-<a id="26차--16차-기반-경량-인식"></a>
-## Revision 26 · lightweight recognition based on revision 16
-
-| Feature | Current behavior |
-| --- | --- |
-| Text recognition | Offline Korean OCR and screen information; Unicode whitespace comparison and prevention of false matches for empty search terms |
-| General macros | Recording, touch, gestures, waiting, conditional branches, back navigation, repetition, and saving |
-| List detection | Up to 6 priorities, repetition counts, duplicate prevention, waiting after completion, and refresh |
-| Execution controls | Ⅱ/▶, stop, move, collapse, opacity, and elapsed time |
-| Screen | Retains revision 16 light/dark modes and the existing design |
-| Slow window responses | Waits without accumulating queries and discards previous responses. Finite settings, stopping, and actual errors remain distinct |
-| Separate AI features | Demonstration training, action models, and ONNX features are excluded. OCR is retained |
+| LEVEL 1 - Beginner | [Recognize text and use the controls](docs/round28.md#user-content-reading-level-1) |
+| LEVEL 2 - Explorer | [Follow recognition and execution](docs/round28.md#user-content-reading-level-2) |
+| LEVEL 3 - Practitioner | [Install, configure and build](docs/round28.md#user-content-reading-level-3) |
+| LEVEL 4 - Advanced | [Inspect matching, state and cancellation](docs/round28.md#user-content-reading-level-4) |
+| LEVEL 5 - Expert | [Read regression evidence and limits](docs/round28.md#user-content-reading-level-5) |
 
 <a id="확인한-결과"></a>
-## Observed results
-
-| Item | Revision 26 result |
-| --- | --- |
-| Unit tests and lint | **106 passed · 0 lint errors · 0 warnings** |
-| Device checks of the final shrunk APK | **89 passed** · dedicated Android 12 virtual device |
-| Installer | **32,717,854 bytes** · includes both ABIs · retains the existing signature |
-| Size compared with the PC revision 16 build | **7.4% decrease** · does not indicate speed or recognition accuracy |
-
-The actual user's BlueStacks, game, Note10+, and operation of 6 instances simultaneously were not operated or measured in this round. [Verification results (Korean source)](../26차/검증_결과.md) separate the evidence for each feature from the unverified scope. The APK and full package ZIP are provided in the local revision 26 and final folders; binaries are not uploaded to GitHub.
-
-<a id="빠른-시작"></a>
-## Quick start
-
-1. Install the universal APK as an update over the existing app and enable accessibility and display-over-other-apps permissions.
-2. Follow the guide to define text, regions, and macros, then save them by name.
-3. Set the macro to detect, repetitions, priority regions, return timing, and refresh timing.
-4. After starting, control execution through Ⅱ/▶, stop, and progress/error logs.
-
-<a id="current-24-final"></a>
-[Previous revision 24 description (Korean source)](../docs/versions/24차.md) · [Revision documentation criteria (Korean source)](../docs/차수별_문서작성기준.md)
-
-[Complete revision index (Korean source)](../docs/REVISION_INDEX.md)
-
-<a id="전체-차수"></a>
-## All revisions
-
-Each revision description covers that revision's features, settings, and results. Comparisons with the preceding revision appear under **Changes and differences** in each document.
-
-<details>
-<summary>Expand all revisions and materials</summary>
-
-| Revision | Main changes | Materials |
-| --- | --- | --- |
-| Base | The editing, service, and control sources in NEXSource2 and the partial wrappers in NEXSource3 differ in completeness. | [Material folder (Korean source)](../코드) · [Details at the same level (Korean source)](../docs/versions/기본.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/기본.md) |
-| 1 | Connects MainActivity editing, Store JSON saving, Step validation, and MacroService gestures. | [Material folder (Korean source)](../1차) · [Details at the same level (Korean source)](../docs/versions/1차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/1차.md) |
-| 2 | Compares text actions, OCR preview JSON, usage instructions, and the verification records from that time. | [Material folder (Korean source)](../2차) · [Details at the same level (Korean source)](../docs/versions/2차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/2차.md) |
-| 3 | Connects FloatingController and GesturePaths to separate controls, recording, curves, and multi-touch configuration. | [Material folder (Korean source)](../3차) · [Details at the same level (Korean source)](../docs/versions/3차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/3차.md) |
-| 4 | Refresh, text detection, selected macros, and return form the Watch flow; regions and the notification icon also change. | [Material folder (Korean source)](../4차) · [Details at the same level (Korean source)](../docs/versions/4차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/4차.md) |
-| 5 | The default 15 detection repetitions are not multiplied by the saved macro's full repetition count; WatchConfig is separated from the original. | [Material folder (Korean source)](../5차) · [Details at the same level (Korean source)](../docs/versions/5차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/5차.md) |
-| 6 | Configures up to 6 non-overlapping priority regions, the target app, duplicate locking, and post-completion waiting, with temporary recording for preparation. | [Material folder (Korean source)](../6차) · [Details at the same level (Korean source)](../docs/versions/6차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/6차.md) |
-| 7 | Prepared to run 5 Nox instances simultaneously, but did not start because the program could not be found and disk-space conditions were not met at that time. | [Material folder (Korean source)](../7차) · [Details at the same level (Korean source)](../docs/revision-audit/7차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/7차.md) |
-| 8 | Connects QueryLane screen reading with the response paths in ReaderResponsivenessTest. | [Material folder (Korean source)](../8차) · [Details at the same level (Korean source)](../docs/versions/8차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/8차.md) |
-| 9 | Floating playback does not start without settings; CapturePacer and CaptureRecoveryTest handle late captures and errors. | [Material folder (Korean source)](../9차) · [Details at the same level (Korean source)](../docs/versions/9차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/9차.md) |
-| 10 | Distinguishes the MainActivity bottom navigation, card UI, and PXE name changes from the original behavior. | [Material folder (Korean source)](../10차) · [Details at the same level (Korean source)](../docs/versions/10차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/10차.md) |
-| 11 | Covers the AUTO fallback to OCR when accessibility reading is slow, alongside pure regression, UTF-8, and device tools. | [Material folder (Korean source)](../11차) · [Details at the same level (Korean source)](../docs/versions/11차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/11차.md) |
-| 12 | Distinguishes indefinite waiting for text touch/wait from the lifetime of responses arriving from a previous run or after a pause. | [Material folder (Korean source)](../12차) · [Details at the same level (Korean source)](../docs/versions/12차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/12차.md) |
-| 13 | Manages stopping from a collapsed panel, disconnection, at most 2 unfinished captures, and one actual OCR Task. | [Material folder (Korean source)](../13차) · [Details at the same level (Korean source)](../docs/versions/13차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/13차.md) |
-| 14 | Changes the teal UI, 48dp targets, motion settings, and the return to the original waitForever behavior when TEXT_TAP text disappears. | [Material folder (Korean source)](../14차) · [Details at the same level (Korean source)](../docs/versions/14차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/14차.md) |
-| 15 | Indefinite waiting, skipping extra post-recognition waiting, and immediate list entry have different scopes.<br>Immediate entry is off by default. | [Material folder (Korean source)](../15차) · [Details at the same level (Korean source)](../docs/versions/15차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/15차.md) |
-| 16 | The default-off adaptiveWait applies only to adjacent conditions using the same recognition method and region, when the next text was confirmed absent beforehand. | [Material folder (Korean source)](../16차) · [Details at the same level (Korean source)](../docs/versions/16차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/16차.md) |
-| 17 | Retains a single query, rechecking a fresh screen, retrying completed OCR, and 0.25–2 second intervals, while distinguishing actual disconnection. | [Material folder (Korean source)](../17차) · [Details at the same level (Korean source)](../docs/versions/17차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/17차.md) |
-| 18 | Increments the count only after actual completion, separates 7/15 completed from the 8th run in progress, and displays stage and status text in addition to color. | [Material folder (Korean source)](../18차) · [Details at the same level (Korean source)](../docs/versions/18차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/18차.md) |
-| 19 | Prevents stale coordinates, unnecessary back navigation, and duplicate releases; rechecks the target app while retaining previous completion counts. | [Material folder (Korean source)](../19차) · [Details at the same level (Korean source)](../docs/versions/19차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/19차.md) |
-| Execution check 2 | Checks PNG CRC, width, height, SHA, and command records for the preserved capture tool. | [Material folder (Korean source)](../실행점검/2차) · [Details at the same level (Korean source)](../docs/versions/실행점검-2차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/실행점검-2차.md) |
-| `최종.json` | Reads macro data containing text, coordinates, retries, waiting, and scrolling conditions. | [Material folder (Korean source)](../docs/materials/022-최종설정) · [Original file (Korean source)](../최종.json) · [Details at the same level (Korean source)](../docs/materials/022-최종설정/README.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/versions/최종설정.md) |
-| Unique working materials | Distinguishes ZIP path, link, encryption, and size checks from identical-byte or BOM/newline correspondence, preserving unique sources. | [Material folder (Korean source)](../보존자료/작업소스) · [Details at the same level (Korean source)](../docs/SOURCE_MATERIALS.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/REVISION_AUDIT.md) |
-| 20 · test tools | Observes OCR, 15 repetitions, absence and reappearance, and duplicate blocking, comparing host/device times and generations. | [Material folder (Korean source)](../20차) · [Details at the same level (Korean source)](../docs/versions/20차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/20차.md) |
-| 21 | Connects absence of the checked text, priority for return text, restarting from the beginning for new text, and blocking late responses from an old generation. | [Material folder (Korean source)](../21차) · [Details at the same level (Korean source)](../docs/versions/21차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/21차.md) |
-| 22 | Improves movement/collapse of the small progress window and waiting flows, separating public source from operational materials. | [Material folder (Korean source)](../22차) · [Details at the same level (Korean source)](../docs/versions/22차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/22차.md) |
-| 23 | Binds the F64 MLP with OCR 584 → 32 → K, 2 demonstrations, 192 training steps, 1 automatic touch, 0 inputs after ending, and 64 after import to the version. | [Material folder (Korean source)](../23차) · [Details at the same level (Korean source)](../docs/versions/23차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/23차.md) |
-| 24 | Distinguishes the 1160-dimensional OCR + RGB policy, legacy 584 compatibility, waiting on the same future, control-window repairs, and the 280 passing unit tests in 44 classes from successful build 13. | [Material folder (Korean source)](../24차) · [Details at the same level (Korean source)](../docs/versions/24차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/24차.md) |
-| 27 · preparation build | Capture waiting, status indicators, and color refinements; long-duration assessment incomplete. | [Material folder (Korean source)](../27차) · [Details at the same level (Korean source)](../docs/versions/27차.md#user-content-reading-level-1) · [Full comparison (Korean source)](../docs/revision-audit/27차.md) |
-| 26 | Uses revision 16 basic macros and UI as its baseline, excludes separate AI, and improves text comparison, window responses, and shrinking. | [Material folder (Korean source)](../26차) · [Details at the same level (Korean source)](../docs/versions/26차.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/26차.md) |
-| Final | Revision 26 universal APK, full source package ZIP, and user guide. | [Material folder (Korean source)](../최종) · [Details at the same level (Korean source)](../최종/README.md#user-content-reading-level-5) · [Full comparison (Korean source)](../docs/revision-audit/최종.md) |
-
-</details>
 
 <a id="-23차--선택형-오프라인-시범-학습"></a>
 
@@ -146,22 +54,12 @@ Each revision description covers that revision's features, settings, and results
 
 <a id="-편집인식제스처저장-모듈"></a>
 
-<a id="overview"></a>
-
 <a id="latest-changes"></a>
 
-<a id="-이전-14차의-변화와-차수-탐색"></a>
-
-<a id="-차수-바로가기"></a>
-
 <a id="preview"></a>
 
 <a id="-입력처리출력"></a>
 
-<a id="introduction"></a>
-
-<a id="-프로젝트-소개"></a>
-
 <a id="-저장소-구성"></a>
 
 <a id="implementation"></a>
@@ -170,20 +68,67 @@ Each revision description covers that revision's features, settings, and results
 
 <a id="-저장인식-요청의-경계"></a>
 
-<a id="️-차수별로-달라진-동작"></a>
-
 <a id="-기록에-남는-정보"></a>
 
+<a id="-데이터와-동작-구성"></a>
+
+## Revision 28 · Word and Phrase Recovery
+
+The OCR path previously compared only a complete line, so `Adobe` could be missed in `Adobe 15`. Version 2.8.5 restores candidates for exact independent words and multi-element phrases. It still rejects the `Adobe` substring of `AdobePro` or `Adobe15`, and does not translate `어도비` or ignore the case difference in `ADOBE`.
+
+| Material | What it establishes | Limit |
+|---|---|---|
+| [Revision 28 English](docs/round28.md) | Matching rules, controls, settings, implementation and five reading levels | OCR can still misread text |
+| [Korean verification record](../28차/검증_결과.md) | 112 unit tests, lint 0, four actual MLKit candidate methods and two recognition/touch methods | Controlled device evidence, not a game accuracy or speed result |
+| [Package fingerprints](../28차/파일정보.json) | Local APK and ZIP sizes, SHA-256 and existing signing certificate | APK/ZIP delivery remains local; no new GitHub release assets |
+| [Revision 28 source](../28차/source/) | Actual Android and PC build-time Rust sources | Build dependencies and signing material are separate |
+
+<a id="빠른-시작"></a>
+
 <a id="-빠른-시작"></a>
 
 <a id="data-settings"></a>
 
-<a id="-데이터와-동작-구성"></a>
-
 <a id="troubleshooting"></a>
 
 <a id="️-문제-해결"></a>
 
+## Quick Start
+
+1. Export important macros, then install the local universal APK as an update using the existing package/signature.
+2. Enable accessibility and display-over-other-apps permissions; follow the app's screen-capture permission flow for OCR.
+3. Set the target text, up to six priority regions, macro, repetitions and timing.
+4. Check the actual screen in preview before starting. Change text from the lower arrow panel or a saved shortcut.
+5. Use the app or upper control window to pause, resume or stop. The lower progress widget has no pause/stop buttons.
+
+Cancelling text editing preserves the current settings and execution. Opening the app may pause execution; resume continues the existing task, while starting a new target replaces the applicable execution generation.
+
+<a id="27차-준비본--속도와-화면-정리"></a>
+
+<a id="26차--16차-기반-경량-인식"></a>
+
+<a id="current-24-final"></a>
+
+<a id="전체-차수"></a>
+
+<a id="-이전-14차의-변화와-차수-탐색"></a>
+
+<a id="-차수-바로가기"></a>
+
+<a id="️-차수별로-달라진-동작"></a>
+
 <a id="-모든-차수보관본과-전체-차이"></a>
 
 <a id="️-차수별-화면과-검증-이미지"></a>
+
+## Revisions and Evidence
+
+| Revision | Read | Interpretation |
+|---|---|---|
+| 26 · existing default final | [Original English overview and full history](README-revision26.md) · [Korean details](../docs/versions/26차.md) | Historical 106 unit tests and 89 device checks belong to revision 26 |
+| 27 · preparation build | [Korean details](../docs/versions/27차.md) · [Verification](../27차/검증_결과.md) | Earlier 15-operation × five-segment timing is not a 2.8.5 speed result; the 60-minute ending was not established |
+| 28 · latest 2.8.5 | [English, five levels](docs/round28.md) · [Korean details](../28차/상세설명.md) · [Full comparison](../28차/전체변경_분할/변경_1.md) | Word/phrase regression recovery; existing UI and macro control preserved |
+
+[Complete revision index · Korean](../docs/REVISION_INDEX.md) · [Korean project overview](../README.md) · [Current user guide · Korean](../28차/사용안내.md)
+
+The latest tests used a dedicated Android 12/API31 x86_64 AVD and isolated ADB port 5047. User BlueStacks, games and the default ADB server were not operated. Long-running stability, the user's 15-repeat game completion time and a 99% recognition rate remain unverified.
diff --git a/en/docs/round28.md b/en/docs/round28.md
new file mode 100644
index 0000000..64dab13
--- /dev/null
+++ b/en/docs/round28.md
@@ -0,0 +1,202 @@
+# Revision 28 · 2.8.5 Recognition Recovery
+
+**English** · [한국어](../../28차/상세설명.md) · [English overview](../README.md)
+
+Version 2.8.5 repairs a recognition regression: comparing only the complete OCR line could miss the independent word `Adobe` in `Adobe 15`. Candidates for independent words and multi-element phrases are restored. The 2.8.4 interface, scrolling and macro controls remain in place. **The existing default final revision 26 is preserved**; the latest revision 28 is not a claim of completed long-duration or game-speed validation.
+
+| Reading level | Read |
+|---|---|
+| LEVEL 1 - Beginner | [Text, preview and controls](#user-content-reading-level-1) |
+| LEVEL 2 - Explorer | [Recognition and execution flow](#user-content-reading-level-2) |
+| LEVEL 3 - Practitioner | [Installation, settings and building](#user-content-reading-level-3) |
+| LEVEL 4 - Advanced | [Matching, storage and cancellation](#user-content-reading-level-4) |
+| LEVEL 5 - Expert | [Regression evidence and limits](#user-content-reading-level-5) |
+
+<a id="reading-level-1"></a>
+
+## LEVEL 1 - Beginner
+
+### Find the intended word before running a macro
+
+A macro is a saved sequence of touches, waits, gestures and back actions. OCR reads text from the screen image. PXE includes the Korean OCR model locally; screen images do not need to be sent away for OCR. Set a word, define where to look, choose a macro and check the preview before starting.
+
+| Screen text and target | Expected behavior | Reason |
+|---|---|---|
+| Target `Adobe`, screen `Adobe 15` | Find the independent word | Other words may be next to it |
+| Target `Adobe`, screen `Adobe Pro` | Find the independent word | A separated word is valid |
+| Target `Adobe`, screen `AdobePro` or `Adobe15` | Reject the substring | The target is attached to another word/number |
+| Target `Adobe`, screen `어도비` or `ADOBE` | Reject the different spelling | No translation or case folding |
+
+Add a target to the horizontal shortcut list to select it from the app or lower arrow panel. The lower counter shows current/total repetitions and can be dragged to move the widget. **Pause, resume and stop are in the app or upper control window**, not the lower progress widget.
+
+Cancelling a text edit leaves the previous task and settings in place. Applying a valid target replaces the applicable running text-detection task. A new target is not a rewrite of the saved general macro action list. OCR can still misread letters or miss text on complex screens.
+
+[한국어 · same level](../../28차/상세설명.md#user-content-reading-level-1) · [Project guide · Korean](../../docs/읽기안내/레벨1.md) · [Complete shared behavior](#user-content-shared-contract)
+
+<a id="reading-level-2"></a>
+
+## LEVEL 2 - Explorer
+
+### Follow recognition, regions and execution
+
+Screen-information recognition compares the complete accessibility node `text`. It does not use `contentDescription`. OCR instead constructs exact word/phrase candidates from the recognized original line. **AUTO can fall back from screen information to OCR**, so these two recognition units remain distinct.
+
+| Stage | Current behavior | Boundary |
+|---|---|---|
+| Screen information | Compare the complete node text | A word inside a longer node is not automatically the whole-node match |
+| OCR candidates | Align elements with the original line and check word boundaries | Do not invent omitted/replaced characters |
+| Region filtering | Check a completed candidate's coordinates | Cropping `Pro` outside a region must not turn `AdobePro` into `Adobe` |
+| List detection | Search up to six priority regions, enter, run the configured macro repeats, return | Waiting after completion and refresh timing are separate settings |
+| Change target | Save the shared shortcut/target settings and replace the applicable execution generation | Do not accumulate a new screen query while the previous query is still running |
+
+The app and expanded lower panel share one shortcut store. Registration edits the list; selecting a shortcut saves both the first detection target and standalone text target. Duplicate prevention preserves its saved lock and reappearance conditions. An OCR error is not treated as evidence that text is truly absent.
+
+The automatic downward movement takes **0.9 seconds across 22.5% of screen height**. Pull-to-refresh remains **45% over 0.9 seconds**. Recorded gestures use their saved paths and durations; these automatic-motion constants do not rewrite recorded gestures.
+
+[한국어 · same level](../../28차/상세설명.md#user-content-reading-level-2) · [Project guide · Korean](../../docs/읽기안내/레벨2.md) · [Full features and changes](#user-content-shared-contract)
+
+<a id="reading-level-3"></a>
+
+## LEVEL 3 - Practitioner
+
+### Install, configure and check the actual screen
+
+The universal local APK supports **Android 11+**, ARM64 and x86_64, with the existing package ID and signing certificate. Install as an update rather than uninstalling. Export important macros first: this revision did not compare every user's saved data across every update from revision 16 to 28.
+
+| Step | Set or inspect | Expected outcome |
+|---|---|---|
+| Permissions | Accessibility, overlay and the screen-capture permission flow used for OCR | Required services can read/act on the screen |
+| Detection | Target text, up to six regions, macro and repeat count | Preview shows the intended original word/phrase |
+| Timing | Repetitions, completion wait, return and refresh intervals | Per-action repeats and full-macro repeats remain distinct |
+| Change target | Lower arrow → edit text, or a registered shortcut | Valid text updates the applicable target task; cancel preserves it |
+| Resume/stop | App or upper controls | Returning to the app may pause; resume continues the task |
+
+Invalid text stays in the editing dialog without first stopping a valid running task. Recording and structural edits to the action list retain their existing stop conditions. Widget opacity is configurable. Expand the lower arrow to inspect overall progress; it does not contain pause/stop buttons.
+
+### Build from public source
+
+Use the actual Python executable, JDK 17, Android SDK, Gradle and separately held signing material. The existing scripts accept `JAVA_HOME`, `ANDROID_HOME`, `PXE_GRADLE` and `NEX_SIGNING_DIR`. The Rust build script also has documented Windows tool/SDK paths that must be reviewed on another PC; the environment is not automatically installed or portable.
+
+From the revision's `source` folder:
+
+```powershell
+& './theme-engine/빌드.ps1' -Mode test
+& './theme-engine/빌드.ps1' -Mode release
+Copy-Item -LiteralPath './theme-engine/target/release/astryx-theme.exe' -Destination './theme-engine/astryx-theme.exe'
+& './빌드.ps1' -Python $PythonExecutable
+```
+
+Set `$PythonExecutable` to the actual existing Python executable. Stop if a prerequisite command fails. `tools/build_release.py` invokes the Rust palette generator, checks the two palettes, synchronizes the actual build copy, and runs release assembly, unit tests and lint. `-PNEX_PC` includes ARM64 and x86_64. Rust generates Android color XML on the PC; APK users do not install Rust. Private signing keys and execution environments are not in the public source.
+
+| Local delivery | Bytes | How to identify it |
+|---|---|---|
+| Universal APK | 32,732,799 | Version 2.8.5, versionCode 28, existing v2 signature |
+| Installation ZIP | 14,951,973 | Embedded APK bytes match the standalone APK |
+| Full source ZIP | 2,906,316 | 153 file entries checked against the local original source |
+
+Exact SHA-256 values and signer fingerprint are in [package metadata](../../28차/파일정보.json). APK/ZIP files remain local deliveries; no new GitHub release assets were created. These sizes describe packages, not runtime speed or accuracy.
+
+[한국어 · same level](../../28차/상세설명.md#user-content-reading-level-3) · [Project guide · Korean](../../docs/읽기안내/레벨3.md) · [Build instructions · Korean](../../28차/source/빌드방법.txt)
+
+<a id="reading-level-4"></a>
+
+## LEVEL 4 - Advanced
+
+### Match original text, then apply geometry
+
+`TextRules.elementStarts` maps OCR elements monotonically into the original line and allows only whitespace in the gaps. Missing, replaced or unexplained non-whitespace characters make the mapping fail. `elementSpans` accepts a target only when its original substring ends at an element boundary and `completeElement` accepts its word boundaries. Phrase whitespace and case remain exactly as supplied.
+
+| Component | Responsibility | Failure/state boundary |
+|---|---|---|
+| `TextRules` | Original element locations, exact spans and Unicode-aware word boundaries | No partial word, translation, case folding or whitespace normalization at execution |
+| OCR candidate selection | Union the participating element boxes before region filtering | If original mapping is uncertain, fall back to complete line/block comparison |
+| `MacroService.replaceTargetWord` | Validate new configuration and standalone step, resolve the necessary macro, then save `word/watch_config` in one edit | Invalid input preserves the current settings and execution generation |
+| `MacroService.activeTargetWord` | Expose the target in the running execution snapshot | Display follows the actual running target even when saved modes differ |
+| `RunHud` / `TargetWords` | Shared JSON shortcut order, duplicate prevention and deletion | Rebuild shortcuts only when stored text or the active target changes |
+| `QueryLane` / `readScreen` | Wait for the outstanding platform query to finish | Future cancellation alone does not terminate the platform call; retain cancellation checks, real timeout and stale-result rejection |
+
+Unicode letters/digits, combining marks, connector punctuation and format characters participate in the boundary check. This prevents selecting a piece of an attached word, underscore-connected label or combining sequence. Legacy contains/regex/ignore-space/ignore-case settings remain readable where required, but the execution path prioritizes original literal equality via `prepareExact`.
+
+The native UI separates full-width action descriptions from the title/more button, reduces redundant row boundaries and empty helper text, and retains popup width, keyboard resizing and Korean wrapping refinements. It is not a React runtime embedded in Android. Existing native screens and actual Rust-generated palette XML remain in use.
+
+Cancelling the old query does not justify starting unbounded new threads. The code waits in short cancellable intervals until the old work is ready, while preserving the true unresponsive boundary and discarding results from older execution generations.
+
+[한국어 · same level](../../28차/상세설명.md#user-content-reading-level-4) · [Project guide · Korean](../../docs/읽기안내/레벨4.md) · [Revision 28 source](../../28차/source/)
+
+<a id="reading-level-5"></a>
+
+## LEVEL 5 - Expert
+
+### Separate regression recovery from performance claims
+
+The comparison used the same synthetic `Adobe 15` image: the 2.8.4 matching path missed the target while the supplied 2.8.1 path found it. Twenty-seven OCR model/native files were byte-identical between those two packages, which helped isolate candidate selection from a changed recognition model. Version 2.8.5 restores exact independent candidates without changing the preserved UI, scrolling or macro controls.
+
+| Evidence | Observed result | What it does not establish |
+|---|---|---|
+| Release build / unit / lint | Build succeeded; 112 tests, zero failures/errors/skips; lint 0 | Every device or future execution is error-free |
+| Actual MLKit candidate methods | Four passed: independent word, multi-element phrase, attached-word/region boundaries and alternate-label/spelling exclusions | Four end-to-end touches on new phrases |
+| Recognition and touch on the app's test screen | Two passed | User-game behavior or all real-world screen layouts |
+| Package verification | 2.8.5/code 28, existing v2 signing certificate, local ZIP/APK fingerprints | Speed, long-running reliability or recognition accuracy percentage |
+| Public comparison | Full original diff and readable split comparison retained | A new research paper or model-performance result |
+
+The six unique device methods ran on the dedicated Android 12/API31 x86_64 AVD using isolated ADB port 5047. The user's BlueStacks, games and default ADB port 5037 were not operated; the dedicated AVD was closed afterward. Candidate tests ran actual MLKit on synthetic images and checked product candidates/coordinates. Only the other two methods checked recognition through actual touch execution on the app's own screen.
+
+| Preserved observation or failure | Treatment |
+|---|---|
+| First A/B image separated the two labels onto different lines | Preserve failed reproduction assertion; use a single-line `Adobe 15` control to reproduce the actual difference |
+| Missing `Hit` import in the new test | Preserve compilation failure, add the import and confirm the final build |
+| 2.8.4 A/B assertion failed | Preserve as the observed regression; record 2.8.1 and final 2.8.5 success separately |
+| Earlier test rejected `Adobe Pro` | Correct the expectation: independent `Adobe` is valid; attached `AdobePro` is excluded. Historical `WholeLine` method names do not define the current policy |
+
+`Macro` and `CapturePacer` retain their prior definitions. `Step` changed the literal-match display/legacy-regex validation boundary, and `WatchConfig` changed the automatic downward endpoint; therefore a claim that all four files are identical to revisions 16/26 is not current. Capture intervals remain API30 **1,100 ms**, API31+ **500 ms**; existing capture/OCR concurrency limits, backoff and stale-response disposal remain.
+
+`ScrollMotion.DURATION_MS=900`, `START_Y=75`, `END_Y=52.5` control automatic downward/list-retry motion. Pull-down remains 30→75; recorded SWIPE/GESTURE actions keep their own saved durations and paths. Earlier motion/device evidence and the preserved [2.8.4 design screenshots](../../28차/화면_확인/2.8.4/README.md) are historical evidence, not newly captured 2.8.5 UI validation or new timing measurements.
+
+The reported local Graphify Java-AST analysis had 1,634 nodes and 5,544 edges; XML/binaries and some Gradle inputs were outside that extractor's scope. A graph count is not recognition accuracy or performance evidence. Windows Rust is the build-time palette generator, not a Rust replacement for Android OCR.
+
+**Long-term uninterrupted operation, the user's 15-repeat game completion time, all-device success and 99% recognition accuracy remain unverified.** Earlier 75-operation/five-segment measurements belong to their recorded revisions and are not a new 2.8.5 benchmark.
+
+[한국어 · same level](../../28차/상세설명.md#user-content-reading-level-5) · [Project guide · Korean](../../docs/읽기안내/레벨5.md) · [Verification record](../../28차/검증_결과.md) · [Full readable comparison](../../28차/전체변경_분할/변경_1.md)
+
+<a id="shared-contract"></a>
+
+## Shared Recognition, Features and Design
+
+These are the full shared conditions. Level-specific explanations above do not replace them.
+
+### Original-text recognition
+
+OCR compares exact original independent words or phrases. `Adobe` in `Adobe 15` or `Adobe Pro` is valid; the substring in `AdobePro`/`Adobe15`, translated `어도비`, and differently cased `ADOBE` are not. Multi-element phrases preserve their original internal whitespace and case. If OCR elements cannot be aligned exactly with the original text, the program falls back to complete line/block comparison without inventing or joining characters.
+
+Screen-information mode compares complete node text and ignores `contentDescription`. AUTO tries OCR when screen information does not find the target. These are different comparison units. Saved contains/regex/ignore-space/ignore-case options are not applied to the current execution match. OCR misrecognition and screen-dependent omissions remain possible.
+
+| Feature | Current behavior | Setting or limit |
+|---|---|---|
+| General macros | Record, touch, swipe, curve/multi-touch gesture, wait, back, branch and repeat | Preserve saved order; distinguish action repeat from full-macro repeat |
+| Recognition | Offline Korean OCR, screen information, AUTO fallback | No translation between `Adobe` and `어도비` |
+| List detection | Search up to six priority areas, enter, run the registered macro (for example 15 repeats), return | Repetitions, priorities, return, completion wait and refresh timing are configurable |
+| Duplicate prevention | Saved lock and reappearance rules | Reading failure is distinct from actual absence |
+| Execution controls | Pause/resume/stop in app and upper controls | No pause/stop in lower progress widget |
+| Target shortcuts | Add, suppress duplicates, delete and select from shared horizontal lists | Up to 30 entries shared across text/detection tabs and expanded lower widget |
+| Storage and display | Macro import/export, theme, widget position/opacity | Update without uninstalling; export important original macros first |
+
+| Preserved design | Value | Scope |
+|---|---|---|
+| Background/cards | Light `#F3F3EF` / `#FBFCF8`; dark `#181C19` / `#232924` | Low-contrast structure retained from 2.8.4 |
+| Accent | Light `#505C52`; dark `#BCCDBB` | Selection, focus and primary actions; no blue accent |
+| Typography | Title 24sp; primary input 20sp; choices 14sp; action title 15sp / description 13sp | System selection text also 14sp; NanumGothic 700 retained |
+| Action rows | Full-width description; minimum 112dp; title/description up to two lines; more button 64dp | Ellipsis in the row; complete content in the editor |
+| Control icons | 24dp vectors centered in matching 48dp buttons | Shared palette instead of font symbols |
+| Logo | Rounded light `P` on charcoal | Launcher, header, collapsed widget and notification |
+| Lower panel | `1/15`, arrow, movement, target edit and shortcuts | Pause/stop stay in app/upper controls |
+
+| Revision-28 behavior retained by 2.8.5 | Implementation | Separate boundary |
+|---|---|---|
+| Automatic movement | Downward/list-retry 0.9s over 22.5% height | Pull-down 45% over 0.9s; recorded gestures unchanged |
+| Internal layout | Simple rows/dividers, full-width action description, input/popup spacing | Light/dark saved choice and existing native rendering |
+| Lower target edit | Arrow beside `1/15` → edit → apply | Drag counter/position saving and touches outside the panel retained |
+| Shortcut selection | Save first detection and standalone target together | Running applicable modes restart; general macro list unchanged |
+| Query race handling | Cancellable wait for previous query's actual completion | No query accumulation; real unresponsive limit and stale-result rejection retained |
+| Progress widget | Collapsed 104×44dp; expanded width 280dp; progress/time/target/shortcuts | Upper/app pause-stop retained; lower pause-stop removed |
+
+[Korean full contract](../../28차/상세설명.md#user-content-pxe28-current-contract) · [Revision 28 source](../../28차/source/) · [Previous revision 27 · Korean](../../27차/상세설명.md) · [Original revision 26 English and earlier history](../README-revision26.md)
~~~~~
