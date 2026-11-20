package kr.yunseok.touchmacro;

import android.view.MotionEvent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 실제 서비스의 원자 helper를 제어 호출하며 입력 전달 전 AI 대상·세대 경계를 확인합니다. */
@RunWith(AndroidJUnit4.class) public class AiRecoveryTargetBoundaryTest {
    static final String TARGET="kr.yunseok.pxe22fixture";
    final GuidedFlowTest h=new GuidedFlowTest();
    final ExecutorService calls=Executors.newSingleThreadExecutor();
    final AtomicInteger phases=new AtomicInteger(),downs=new AtomicInteger();
    Field observerField;
    Object originalObserver;
    ThreadLocal<AiStartGate> firstInput;
    @Before @SuppressWarnings("unchecked") public void boot()throws Exception{
        h.boot();h.fixture();
        h.main(()->h.entry.setOnTouchListener((view,event)->{if(event.getActionMasked()==MotionEvent.ACTION_DOWN)downs.incrementAndGet();return false;}));
        observerField=MacroService.class.getDeclaredField("aiInputBoundaryObserver");observerField.setAccessible(true);originalObserver=observerField.get(h.svc);
        observerField.set(h.svc,(MacroService.AiInputBoundaryObserver)phase->phases.incrementAndGet());
        firstInput=(ThreadLocal<AiStartGate>)ControlDiagnostics.field(h.svc,"firstAiInput");
        h.in.waitForIdleSync();h.waitFor(()->h.a.getPackageName().equals(h.svc.activePackage()),4000,"제어 회복 대상 앱 확인 실패");
    }
    @After public void end()throws Exception{
        try{h.end();}finally{calls.shutdownNow();try{assertTrue("제어 helper가 정지 뒤 끝나지 않았습니다",calls.awaitTermination(2,TimeUnit.SECONDS));}finally{if(observerField!=null)observerField.set(h.svc,originalObserver);}}
    }
    Macro waitingMacro(){Macro macro=new Macro();macro.startDelay=0;macro.maxMinutes=0;Step wait=new Step("WAIT");wait.duration=600000;wait.after=0;macro.steps.add(wait);return macro;}
    long start()throws Exception{h.main(()->h.svc.start(waitingMacro()));return ((AtomicLong)ControlDiagnostics.field(h.svc,"generation")).get();}
    Throwable invoke(String helper,String target,long token)throws Exception{
        AiStartGate gate=AiStartGate.macro(waitingMacro(),TARGET,h.a.getPackageName());
        Future<Throwable> result=calls.submit(()->{
            firstInput.set(gate);
            try{
                Method method;
                if(helper.equals("recoveryGesture")){method=MacroService.class.getDeclaredMethod(helper,double.class,double.class,double.class,double.class,long.class,String.class,long.class);method.setAccessible(true);method.invoke(h.svc,50d,10d,50d,10d,60L,target,token);}
                else{method=MacroService.class.getDeclaredMethod(helper,Step.class,String.class,long.class);method.setAccessible(true);Step tap=new Step("TAP");tap.x=50;tap.y=10;tap.after=0;method.invoke(h.svc,tap,target,token);}
                return null;
            }catch(InvocationTargetException error){return error.getCause();}
            finally{firstInput.remove();}
        });
        return result.get(1000,TimeUnit.MILLISECONDS);
    }
    void noInput(){assertEquals("전달 전 준비 경계에 진입했습니다",0,phases.get());assertEquals("기존 대상에 입력이 전달됐습니다",0,downs.get());}
    @Test public void persistentFirstTargetMismatchRejectsBothAtomicHelpersBeforeRetry()throws Exception{
        long token=start();
        for(String helper:new String[]{"recoveryGesture","recoveryMultiGesture"}){Throwable error=invoke(helper,h.a.getPackageName(),token);assertNotNull(helper,error);assertEquals(helper,"TargetChanged",error.getClass().getSimpleName());}
        noInput();
    }
    @Test public void stoppedGenerationExitsBothHelpersWithoutInput()throws Exception{
        long token=start();h.main(()->h.svc.stop());
        for(String helper:new String[]{"recoveryGesture","recoveryMultiGesture"})assertTrue(helper,invoke(helper,TARGET,token) instanceof CancellationException);
        noInput();
    }
    @Test public void newManualGenerationRejectsOldHelpersWithoutTouchingNewRun()throws Exception{
        long old=start(),current=start();assertTrue(current>old);
        for(String helper:new String[]{"recoveryGesture","recoveryMultiGesture"})assertTrue(helper,invoke(helper,TARGET,old) instanceof CancellationException);
        assertTrue(h.svc.running);assertEquals(current,((AtomicLong)ControlDiagnostics.field(h.svc,"generation")).get());noInput();
    }
}
