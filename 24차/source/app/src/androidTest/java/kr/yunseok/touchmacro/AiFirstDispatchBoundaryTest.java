package kr.yunseok.touchmacro;

import android.content.Intent;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 실제 전달 API는 그대로 사용하며 미전달 경계만 한 번 제어합니다. 아직 기기 실행 전입니다. */
@RunWith(AndroidJUnit4.class) public class AiFirstDispatchBoundaryTest {
    final AiStartBoundaryTest base=new AiStartBoundaryTest();
    final GuidedFlowTest h=base.h;
    Field observerField;
    @Before public void boot()throws Exception{base.boot();h.shell("am force-stop "+AiStartBoundaryTest.TARGET);observerField=MacroService.class.getDeclaredField("aiInputBoundaryObserver");observerField.setAccessible(true);}
    @After public void end()throws Exception{if(observerField!=null)observerField.set(h.svc,null);base.end();}
    void observe(MacroService.AiInputBoundaryObserver observer)throws Exception{observerField.set(h.svc,observer);}
    void completeOne(){h.waitFor(()->!h.svc.running,7000,"첫 전달 계획이 끝나지 않았습니다");h.waitFor(()->base.counter(1),1500,"대상 입력이 정확히 1회가 아닙니다");assertEquals(0,base.ownTaps.get());}
    void zero(){assertTrue("대상에 미승인 입력이 전달됐습니다",base.counter(0));assertEquals(0,base.ownTaps.get());}
    @Test public void ownHudWindowEventBeforeFreshReadStillAllowsExactlyOneRealTap()throws Exception{
        AtomicBoolean injected=new AtomicBoolean();observe(phase->{if(phase.equals("PREPARED")&&injected.compareAndSet(false,true))h.main(()->{AccessibilityEvent event=AccessibilityEvent.obtain(AccessibilityEvent.TYPE_WINDOWS_CHANGED);event.setPackageName(h.a.getPackageName());try{h.svc.onAccessibilityEvent(event);}finally{event.recycle();}});});base.commit();completeOne();assertTrue(injected.get());
    }
    @Test public void anotherUidAfterInitialGateAndPreparationReceivesNoTap()throws Exception{
        CountDownLatch prepared=new CountDownLatch(1),release=new CountDownLatch(1);AtomicBoolean once=new AtomicBoolean();observe(phase->{if(phase.equals("PREPARED")&&once.compareAndSet(false,true)){prepared.countDown();if(!release.await(4,TimeUnit.SECONDS))throw new IllegalStateException("제어 준비 해제 실패");}});base.commit();assertTrue(prepared.await(3,TimeUnit.SECONDS));AtomicInteger otherUidTaps=new AtomicInteger();try{h.a=(MainActivity)h.in.startActivitySync(new Intent(h.in.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));h.fixture();h.main(()->{Button unexpected=new Button(h.a);unexpected.setText("타 UID 입력 금지");unexpected.setOnTouchListener((v,event)->{if(event.getActionMasked()==MotionEvent.ACTION_DOWN)otherUidTaps.incrementAndGet();return true;});h.a.setContentView(unexpected);});h.waitFor(()->h.a.hasWindowFocus(),2000,"타 UID 창이 활성화되지 않았습니다");}finally{release.countDown();}h.waitFor(()->!h.svc.running,4000,"다른 UID에서 최초 입력을 중단하지 않았습니다");assertEquals(0,otherUidTaps.get());h.shell("am start -W -f 0x20000 -n "+AiStartBoundaryTest.TARGET+"/.FixtureActivity");zero();
    }
    @Test public void windowRevisionAfterFreshReadAndBeforeDispatchDeliversNothing()throws Exception{
        AtomicBoolean changed=new AtomicBoolean();observe(phase->{if(phase.equals("DISPATCH")&&changed.compareAndSet(false,true))((java.util.concurrent.atomic.AtomicLong)ControlDiagnostics.field(h.svc,"windowRevision")).incrementAndGet();});base.commit();h.waitFor(()->!h.svc.running,7000,"창 변경 허가가 폐기되지 않았습니다");assertTrue(changed.get());zero();
    }
    @Test public void pauseDuringPreparationRejectsOldPermitAndResumesWithOneFreshTap()throws Exception{
        AtomicBoolean once=new AtomicBoolean();AtomicInteger preparations=new AtomicInteger();observe(phase->{if(phase.equals("PREPARED")){preparations.incrementAndGet();if(once.compareAndSet(false,true))h.main(()->h.svc.togglePause());}});base.commit();h.waitFor(()->h.svc.paused,4000,"준비 중 일시정지하지 못했습니다");assertTrue(h.svc.running);zero();h.main(()->h.svc.togglePause());completeOne();assertTrue("재개 후 준비·관측을 새로 하지 않았습니다",preparations.get()>=2);
    }
    @Test public void newManualGenerationImmediatelyBeforeDispatchGetsNoOldTap()throws Exception{
        AtomicBoolean replaced=new AtomicBoolean();observe(phase->{if(phase.equals("DISPATCH")&&replaced.compareAndSet(false,true))h.svc.start(base.waitingMacro());});base.commit();h.waitFor(replaced::get,4000,"새 실행 세대를 제어하지 못했습니다");SystemClock.sleep(250);assertTrue(h.svc.running);zero();h.main(()->h.svc.stop());
    }
    @Test public void stopImmediatelyBeforeDispatchGetsNoLateTap()throws Exception{
        AtomicBoolean stopped=new AtomicBoolean();observe(phase->{if(phase.equals("DISPATCH")&&stopped.compareAndSet(false,true))h.svc.stop();});base.commit();h.waitFor(()->stopped.get()&&!h.svc.running,4000,"전달 직전 정지가 반영되지 않았습니다");SystemClock.sleep(250);zero();
    }
    @Test public void explicitAppAfterZeroDelayWaitsForItsNewTargetWithoutExtraLaunch()throws Exception{
        Macro macro=base.waitingMacro();macro.name="명시 앱 전환";macro.steps.get(0).duration=1;Step app=new Step("APP");app.packageName=AiStartBoundaryTest.TARGET;app.after=0;macro.steps.add(app);Step tap=new Step("TAP");tap.x=50;tap.y=10;tap.after=0;macro.steps.add(tap);JSONObject context=base.context();context.put("execution_target","com.android.settings");AiJson.obj(context.opt("macros")).put("명시 앱",macro.json());AiPlanCompiler.Compiled compiled=AiPlanCompiler.compile(AiJson.object("schema","pxe.plan.v1","status","ready","body",new JSONArray().put(AiJson.object("op","CALL","macro_ref","명시 앱"))),context);h.main(()->h.svc.commitAi(h.svc.captureAiLease(),compiled));completeOne();assertEquals(0,compiled.macro().steps.get(1).after);assertEquals(1,compiled.macro().steps.get(1).repeat);String log=h.svc.logText();assertEquals(1,log.split("2번 앱 실행 완료",-1).length-1);
    }
}
