package kr.yunseok.touchmacro;

import android.os.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** worker 시작 전 일시정지와 정지를 제어해 측정 초기화 순서를 확인합니다. */
@RunWith(AndroidJUnit4.class) public class PauseStartSafetyTest {
    final GuidedFlowTest h=new GuidedFlowTest();final CountDownLatch release=new CountDownLatch(1);
    @Before public void boot()throws Exception{h.boot();h.fixture();}
    @After public void end(){release.countDown();h.end();}
    Field field(String name)throws Exception{Field result=MacroService.class.getDeclaredField(name);result.setAccessible(true);return result;}
    @Test public void stopBeforeWorkerInitializationCannotReplaceFinishedMeasurements()throws Exception {
        ExecutorService worker=(ExecutorService)field("worker").get(h.svc);CountDownLatch blocked=new CountDownLatch(1);
        worker.execute(()->{blocked.countDown();try{release.await(5,TimeUnit.SECONDS);}catch(InterruptedException stopped){Thread.currentThread().interrupt();}});
        assertTrue("worker 순서 제어가 시작되지 않았습니다",blocked.await(2,TimeUnit.SECONDS));
        Macro macro=new Macro();macro.name="즉시 정지 측정 보존";macro.startDelay=0;Step wait=new Step("WAIT");wait.duration=1000;wait.after=0;macro.steps.add(wait);
        h.main(()->{h.svc.start(macro);h.svc.stop();});Object stopped=field("executionMetrics").get(h.svc);long started=field("startedAt").getLong(h.svc),finished=field("finishedAt").getLong(h.svc);String frozen=h.svc.performanceText();
        release.countDown();worker.submit(()->{}).get(3,TimeUnit.SECONDS);
        Bundle data=new Bundle();data.putString("stream","PXE16_STOP_DIAGNOSTIC running="+h.svc.running+", same_metrics="+(stopped==field("executionMetrics").get(h.svc))+", started_before="+started+", started_after="+field("startedAt").getLong(h.svc)+", finished_before="+finished+", finished_after="+field("finishedAt").getLong(h.svc)+"\n");h.in.sendStatus(0,data);
        assertFalse("정지한 실행이 다시 실행 상태가 됐습니다",h.svc.running);
        assertSame("정지한 큐 실행이 완료된 측정 객체를 바꿨습니다",stopped,field("executionMetrics").get(h.svc));
        assertEquals("정지 뒤 시작 시각이 바뀌었습니다",started,field("startedAt").getLong(h.svc));assertEquals("정지 뒤 완료 시각이 바뀌었습니다",finished,field("finishedAt").getLong(h.svc));
        SystemClock.sleep(250);assertEquals("정지 뒤 측정 표시가 변했습니다",frozen,h.svc.performanceText());
    }
    @Test public void pauseBeforeWorkerInitializationKeepsTheActiveClockFrozen()throws Exception {
        ExecutorService worker=(ExecutorService)field("worker").get(h.svc);CountDownLatch blocked=new CountDownLatch(1);
        worker.execute(()->{blocked.countDown();try{release.await(5,TimeUnit.SECONDS);}catch(InterruptedException stopped){Thread.currentThread().interrupt();}});
        assertTrue("worker 순서 제어가 시작되지 않았습니다",blocked.await(2,TimeUnit.SECONDS));
        Macro macro=new Macro();macro.name="즉시 일시정지 시계 진단";macro.startDelay=0;Step wait=new Step("WAIT");wait.duration=1000;wait.after=0;macro.steps.add(wait);
        h.main(()->{h.svc.start(macro);h.svc.togglePause();});long pausedBefore=field("pauseStartedAt").getLong(h.svc);assertTrue(h.svc.paused);assertTrue(pausedBefore>0);
        long oldStarted=field("startedAt").getLong(h.svc);release.countDown();h.waitFor(()->{try{return field("startedAt").getLong(h.svc)!=oldStarted;}catch(Exception error){throw new RuntimeException(error);}},3000,"worker 초기화가 실행되지 않았습니다");
        Method clock=MacroService.class.getDeclaredMethod("activeTime");clock.setAccessible(true);long first=(Long)clock.invoke(h.svc);SystemClock.sleep(300);long second=(Long)clock.invoke(h.svc);long pausedAfter=field("pauseStartedAt").getLong(h.svc);
        Bundle data=new Bundle();data.putString("stream","PXE16_PAUSE_DIAGNOSTIC paused="+h.svc.paused+", before="+pausedBefore+", after="+pausedAfter+", active_delta="+(second-first)+", metrics="+h.svc.performanceText()+"\n");h.in.sendStatus(0,data);
        assertTrue("worker 초기화가 즉시 일시정지를 해제했습니다",h.svc.paused);
        assertTrue("worker 초기화가 pauseStartedAt을 지웠습니다",pausedAfter>0);
        assertTrue("일시정지 중 활성 시간이 흐릅니다: "+(second-first),second-first<=2);
        String frozen=h.svc.performanceText();SystemClock.sleep(250);assertEquals("일시정지 중 측정 표시가 변했습니다",frozen,h.svc.performanceText());
        h.main(()->h.svc.togglePause());h.waitFor(()->!h.svc.running,4000,"계속 실행 뒤 수동 WAIT가 완료되지 않았습니다");assertEquals(1,h.svc.watchCompleted);
        h.main(()->{h.svc.start(macro);h.svc.stop();});String stopped=h.svc.performanceText();SystemClock.sleep(250);assertEquals("중단 뒤 측정 표시가 변했습니다",stopped,h.svc.performanceText());
    }
}
