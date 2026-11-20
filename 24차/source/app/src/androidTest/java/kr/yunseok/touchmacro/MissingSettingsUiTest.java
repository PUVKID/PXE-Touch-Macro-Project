package kr.yunseok.touchmacro;

import android.view.*;
import android.view.inspector.WindowInspector;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.lang.reflect.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 기본 편집의 미발견 정책·이름 선택과 이전 저장 조합의 호환을 확인합니다. */
@RunWith(AndroidJUnit4.class) public class MissingSettingsUiTest {
    final EntrySettingsUiTest ui=new EntrySettingsUiTest();final GuidedFlowTest h=ui.h;Macro macro;Step original;
    @Before public void boot()throws Exception{ui.boot();macro=new Macro();Step wait=new Step("WAIT");wait.duration=100;macro.steps.add(wait);original=new Step("TEXT_TAP");original.text="안내 대상";macro.steps.add(original);h.main(()->Store.save(h.a,macro));}
    @After public void end()throws Exception{ui.end();}
    View named(String label){for(View root:WindowInspector.getGlobalWindowViews()){View hit=ui.text(root,label);if(hit!=null)return hit;}return null;}
    void edit()throws Exception{h.main(()->{try{Field f=MainActivity.class.getDeclaredField("macro");f.setAccessible(true);f.set(h.a,macro);Method m=MainActivity.class.getDeclaredMethod("edit",Step.class);m.setAccessible(true);m.invoke(h.a,original);}catch(Exception e){throw new RuntimeException(e);}});h.in.waitForIdleSync();}
    Spinner choice(String label){View text=named(label);assertNotNull(label,text);ViewGroup parent=(ViewGroup)text.getParent();View next=parent.getChildAt(parent.indexOfChild(text)+1);assertTrue(label,next instanceof Spinner);return (Spinner)next;}
    @Test public void newFiniteRecoverySelectionCannotSaveForeverWaitConflict()throws Exception{edit();h.main(()->{choice("글자가 없으면").setSelection(2);((CheckBox)named("글자가 나타날 때까지 계속 기다리기")).setChecked(true);named("적용").performClick();});h.in.waitForIdleSync();Step stored=Store.load(h.a).steps.get(1);assertFalse("새 편집에서 복구가 영원히 실행되지 않는 조합을 저장했습니다",stored.waitForever&&stored.retryPolicy());}
    @Test public void basicPolicyAndNamedRecoverySaveInfiniteAndCancelPreservesValues()throws Exception{edit();h.main(()->{assertTrue(named("글자가 없으면").isShown());Spinner recovery=choice("다시 찾기 전에 실행할 동작");assertTrue(recovery.getAdapter().getItem(1).toString().contains("1번 · 대기"));choice("글자가 없으면").setSelection(6);recovery.setSelection(1);((CheckBox)named("찾을 때까지 계속 재시도")).setChecked(true);named("적용").performClick();});Step stored=Store.load(h.a).steps.get(1);assertEquals("ACTION_RETRY",stored.failure);assertTrue(stored.retryForever);assertFalse(stored.waitForever);assertEquals(1,stored.recoveryTarget);String before=Store.load(h.a).json().toString();macro=Store.load(h.a);original=macro.steps.get(1);edit();h.main(()->{choice("글자가 없으면").setSelection(0);named("취소").performClick();});assertEquals(before,Store.load(h.a).json().toString());}
    @Test public void unchangedLegacyForeverBackJsonRetainsItsOriginalMeaning()throws Exception{original.waitForever=true;original.failure="BACK_RETRY";h.main(()->Store.save(h.a,macro));edit();h.main(()->named("적용").performClick());Step stored=Store.load(h.a).steps.get(1);assertTrue(stored.waitForever);assertFalse(stored.retryForever);assertEquals("BACK_RETRY",stored.failure);assertEquals(3,stored.retries);}
}
