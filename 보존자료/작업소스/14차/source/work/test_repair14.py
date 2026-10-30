from pathlib import Path
p=Path('app/src/androidTest/java/kr/yunseok/touchmacro/PxeRedesignTest.java');s=p.read_text(encoding='utf-8')
a=s.index('    @Test public void nativeMotion');s=s[:a]+'''    View privateView(Object owner,String name){try{java.lang.reflect.Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return (View)f.get(owner);}catch(Exception e){throw new RuntimeException(e);}}
    void targets(View root){if(root instanceof Button||root instanceof ImageButton){assertTrue("터치 너비: "+root.getContentDescription(),root.getWidth()>=Ui.dp(h.a,48));assertTrue("터치 높이: "+root.getContentDescription(),root.getHeight()>=Ui.dp(h.a,48));}if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++)targets(group.getChildAt(i));}}
'''+s[a:]
s=s.replace('float original=Settings.Global.getFloat','h.main(()->{Ui.prefs(h.a).edit().putInt("scale",100).commit();h.svc.controller().show();});h.in.waitForIdleSync();h.main(()->targets(privateView(h.svc.controller(),"panel")));h.main(()->h.svc.controller().close());\n        float original=Settings.Global.getFloat')
s=s.replace('target[0]=described("실행 상태");Ui.enter(target[0]);alpha[0]=target[0].getAlpha();','described("동작").performClick();target[0]=privateView(h.a,"page");alpha[0]=target[0].getAlpha();')
s=s.replace('Ui.enter(target[0]);assertEquals(1f,target[0].getAlpha()','described("설정").performClick();target[0]=privateView(h.a,"page");assertEquals(1f,target[0].getAlpha()')
p.write_text(s,encoding='utf-8')
