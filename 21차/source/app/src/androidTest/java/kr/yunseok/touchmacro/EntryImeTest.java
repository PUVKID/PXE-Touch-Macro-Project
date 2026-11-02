package kr.yunseok.touchmacro;

import android.content.Intent;
import android.graphics.Rect;
import android.view.*;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inspector.WindowInspector;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 실제 IME 창 위에서 새 진입 설정의 저장·취소를 직접 조작합니다. */
@RunWith(AndroidJUnit4.class) public class EntryImeTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    @Before public void boot()throws Exception{h.boot();h.main(()->{SetupFlow.finished(h.a);WatchConfig p=new WatchConfig();p.rule.text="IME 목록";WatchStore.save(h.a,p);h.svc.controller().close();});}
    @After public void end()throws Exception{h.shell("input keyevent 4");h.end();}
    View text(View root,String value){if(root instanceof TextView&&value.contentEquals(((TextView)root).getText()))return root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View found=text(group.getChildAt(i),value);if(found!=null)return found;}}return null;}
    void editors(View root,java.util.List<EditText> result){if(root instanceof EditText)result.add((EditText)root);if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)editors(((ViewGroup)root).getChildAt(i),result);}
    int imeTop(){int top=0;for(AccessibilityWindowInfo window:h.auto.getWindows()){if(window.getType()==AccessibilityWindowInfo.TYPE_INPUT_METHOD){Rect rect=new Rect();window.getBoundsInScreen(rect);if(rect.height()>0)top=rect.top;}window.recycle();}return top;}
    void verify(int mode)throws Exception{
        h.main(()->Ui.prefs(h.a).edit().putInt("theme",mode).commit());h.a=(MainActivity)h.in.startActivitySync(new Intent(h.in.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        h.main(()->{try{java.lang.reflect.Method method=MainActivity.class.getDeclaredMethod("entrySettings");method.setAccessible(true);method.invoke(h.a);}catch(Exception error){throw new RuntimeException(error);}});h.in.waitForIdleSync();
        View[] root={null};java.util.ArrayList<EditText> edits=new java.util.ArrayList<>();h.main(()->{for(View candidate:WindowInspector.getGlobalWindowViews())if(text(candidate,"진입 화면을 확인한 뒤 실행")!=null){root[0]=candidate;break;}assertNotNull(root[0]);editors(root[0],edits);((CheckBox)text(root[0],"진입 화면을 확인한 뒤 실행")).setChecked(true);edits.get(0).setText("배");edits.get(1).setText("사과");edits.get(0).requestFocus();((InputMethodManager)h.a.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).showSoftInput(edits.get(0),InputMethodManager.SHOW_IMPLICIT);});
        h.waitFor(()->imeTop()>0,3000,"실제 IME 창이 표시되지 않았습니다");h.in.waitForIdleSync();h.screenshot("PXE21_IME_"+h.svc.size()[0]+"_"+(mode==1?"라이트":"다크")+".png");
        Rect[] save={null},cancel={null};h.main(()->{View positive=text(root[0],"저장"),negative=text(root[0],"취소");assertNotNull(positive);assertNotNull(negative);save[0]=h.bounds(positive);cancel[0]=h.bounds(negative);});int keyboardTop=imeTop();
        android.os.Bundle diagnostic=new android.os.Bundle();diagnostic.putString("stream","PXE21_IME ime_top="+keyboardTop+", save="+save[0]+", cancel="+cancel[0]+"\n");h.in.sendStatus(0,diagnostic);
        assertTrue("저장 버튼이 실제 키보드 뒤에 가려졌습니다: "+save[0]+" / IME "+keyboardTop,save[0].height()>0&&save[0].bottom<=keyboardTop);
        assertTrue("취소 버튼이 실제 키보드 뒤에 가려졌습니다",cancel[0].height()>0&&cancel[0].bottom<=keyboardTop);
        h.tap(save[0].exactCenterX(),save[0].exactCenterY());h.waitFor(()->WatchStore.load(h.a).entryCheck,2000,"키보드를 연 상태의 실제 저장 터치가 적용되지 않았습니다");
        WatchConfig saved=WatchStore.load(h.a);assertEquals("배",saved.entryRule.text);assertEquals("사과",saved.rejectText);assertEquals(3000,saved.entryRule.timeout);assertEquals("IME 목록",saved.rule.text);
    }
    @Test public void lightThemeSaveAndCancelStayAboveRealIme()throws Exception{verify(1);}
    @Test public void darkThemeSaveAndCancelStayAboveRealIme()throws Exception{verify(2);}
}
