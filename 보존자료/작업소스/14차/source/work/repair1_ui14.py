from pathlib import Path
base=Path('app/src/main/java/kr/yunseok/touchmacro')
p=base/'FloatingController.java';s=p.read_text(encoding='utf-8');s=s.replace('box.setPadding(scaled(3),scaled(3),scaled(3),scaled(3))','box.setPadding(0,0,0,0)').replace('new LinearLayout.LayoutParams(scaled(38),scaled(38))','new LinearLayout.LayoutParams(scaled(48),scaled(48))').replace('scaled(owner.running?84:44):scaled(164),scaled(44)','scaled(owner.running?96:48):scaled(192),scaled(48)').replace('new LinearLayout.LayoutParams(0,scaled(38),1)','new LinearLayout.LayoutParams(0,scaled(48),1)');p.write_text(s,encoding='utf-8')
p=base/'SetupCoach.java';s=p.read_text(encoding='utf-8').replace('Ui.dp(c,44)','Ui.dp(c,48)');p.write_text(s,encoding='utf-8')
p=base/'MainActivity.java';s=p.read_text(encoding='utf-8').replace('super.onCreate(b);blue=Ui.accent(this);','super.onCreate(b);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);blue=Ui.accent(this);');p.write_text(s,encoding='utf-8')
p=Path('app/src/androidTest/java/kr/yunseok/touchmacro/PxeRedesignTest.java');s=p.read_text(encoding='utf-8')
s=s.replace('    ScrollView scroll(View root)', '    EditText edit(View root){if(root instanceof EditText)return (EditText)root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){EditText out=edit(group.getChildAt(i));if(out!=null)return out;}}return null;}\n    ScrollView scroll(View root)')
s=s.replace('review.tab("글자",0);review.tab("탐지",4);\n    }\n    @Test public void dock', '''review.tab("글자",0);
        h.main(()->{EditText field=edit(h.a.findViewById(android.R.id.content));assertNotNull(field);field.requestFocus();((InputMethodManager)h.a.getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(field,InputMethodManager.SHOW_IMPLICIT);});
        h.waitFor(()->{for(android.view.accessibility.AccessibilityWindowInfo window:h.auto.getWindows())if(window.getType()==android.view.accessibility.AccessibilityWindowInfo.TYPE_INPUT_METHOD)return true;return false;},3000,"글자 탭의 키보드가 표시되지 않았습니다");
        h.main(()->{Rect visible=new Rect(),navVisible=new Rect();h.a.getWindow().getDecorView().getWindowVisibleDisplayFrame(visible);View nav=described("탐지");assertTrue(nav.getGlobalVisibleRect(navVisible));assertEquals(h.bounds(nav),navVisible);assertTrue("키보드 위의 탐색 영역",navVisible.bottom<=visible.bottom);});h.screenshot("PXE14_글자_키보드_탐색.png");review.tab("탐지",4);
    }
    @Test public void dock''')
s=s.replace('h.main(()->targets(privateView(h.svc.controller(),"panel")));h.main(()->h.svc.controller().close());','''h.main(()->{View panel=privateView(h.svc.controller(),"panel");targets(panel);View logo=review.byDescription(panel,"PXE 컨트롤 펼치기 · 길게 눌러 정지");assertNotNull(logo);logo.performClick();});h.in.waitForIdleSync();h.main(()->targets(privateView(h.svc.controller(),"panel")));h.screenshot("PXE14_제어창_펼침.png");h.main(()->h.svc.controller().close());
        final SetupCoach[] coach={null};h.main(()->{coach[0]=new SetupCoach(h.svc);coach[0].show("기본 크기 안내 버튼 검사","영역 그리기",()->{},()->{});});h.in.waitForIdleSync();h.main(()->targets(privateView(coach[0],"view")));h.main(()->coach[0].close());''')
p.write_text(s,encoding='utf-8')
print('수리1 기본48dp·실제키보드탐색·실제탭모션 적용')
