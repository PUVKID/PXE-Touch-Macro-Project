from pathlib import Path
p=Path('app/src/androidTest/java/kr/yunseok/touchmacro/PxeRedesignTest.java');s=p.read_text(encoding='utf-8')
a=s.index('        final int[] measured=');b=s.index('\n    @Test public void keyboardInput',a)
s=s[:a]+'''        h.waitFor(()->{final boolean[] ready={false};h.main(()->{ScrollView sc=scroll(h.a.findViewById(android.R.id.content));View body=privateView(h.a,"page");ready[0]=sc.getScrollY()==Math.max(0,sc.getChildAt(0).getHeight()-sc.getHeight())&&body.getAlpha()==1f&&body.getTranslationY()==0f;});return ready[0];},2000,"스크롤과 진입 전환이 완료되지 않았습니다");
        h.main(()->{TextView start=text(h.a.findViewById(android.R.id.content),"탐지 시작");Rect visible=new Rect();assertTrue(start.getGlobalVisibleRect(visible));assertEquals(start.getHeight(),visible.height());});h.screenshot("PXE14_긴문구_스크롤.png");review.tab("설정",3);review.tab("동작",1);review.tab("글자",0);review.tab("탐지",4);
    }
'''+s[b:]
p.write_text(s,encoding='utf-8')
p=Path('app/src/main/java/kr/yunseok/touchmacro/MainActivity.java');s=p.read_text(encoding='utf-8').replace('label.equals("오류 기록")?R.drawable.pxe_warning:R.drawable.pxe_text','label.equals("오류 기록")?R.drawable.pxe_warning:R.drawable.pxe_steps');p.write_text(s,encoding='utf-8')
p=Path('app/src/androidTest/java/kr/yunseok/touchmacro/PxeDesignReviewTest.java');s=p.read_text(encoding='utf-8').replace('h.screenshot("PXE_다크_탐지.png");tab("설정",3);','h.screenshot("PXE_다크_탐지.png");tab("동작",1);h.screenshot("PXE_다크_동작.png");tab("글자",0);h.screenshot("PXE_다크_글자.png");tab("설정",3);');p.write_text(s,encoding='utf-8')
print('수리3 완료조건 동기화/기존 목록아이콘 적용, 임시 진단 코드 제거')
