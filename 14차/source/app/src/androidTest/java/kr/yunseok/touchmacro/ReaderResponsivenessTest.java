package kr.yunseok.touchmacro;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;import org.junit.runner.RunWith;
import java.lang.reflect.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class) public class ReaderResponsivenessTest {
    GuidedFlowTest h=new GuidedFlowTest();
    @Before public void boot()throws Exception{h.boot();}
    @After public void end(){h.end();}
    long token()throws Exception{Field f=MacroService.class.getDeclaredField("generation");f.setAccessible(true);return ((AtomicLong)f.get(h.svc)).get();}
    Method reader()throws Exception{Method m=MacroService.class.getDeclaredMethod("readScreen",Callable.class,long.class);m.setAccessible(true);return m;}
    @Test public void blockedReaderLeavesUiAndStopResponsiveAndDiscardsLateResult()throws Exception{
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicReference<Throwable> failure=new AtomicReference<>();AtomicBoolean returned=new AtomicBoolean();
        long token=token();Method reader=reader();
        Thread t=new Thread(()->{try{reader.invoke(h.svc,(Callable<String>)()->{entered.countDown();release.await();return "stale match";},token);returned.set(true);}catch(Throwable e){failure.set(e);} });
        try{
            t.start();assertTrue(entered.await(2,TimeUnit.SECONDS));long before=SystemClock.elapsedRealtime();
            h.main(()->h.svc.stop());assertTrue("UI/stop blocked by reader",SystemClock.elapsedRealtime()-before<1000);
            t.join(1500);assertFalse("Cancelled scan kept waiting",t.isAlive());assertFalse(returned.get());assertNotNull(failure.get());
            release.countDown();assertFalse(returned.get());
        }finally{release.countDown();t.join(2000);}
    }
    @Test public void slowReadTimesOutClearlyWithoutTreatingItAsMissingText()throws Exception{
        CountDownLatch release=new CountDownLatch(1);long before=SystemClock.elapsedRealtime();
        try{
            reader().invoke(h.svc,(Callable<String>)()->{release.await();return "late";},token());fail("Timed-out scan returned a match");
        }catch(InvocationTargetException e){assertTrue(e.getCause().getMessage(),e.getCause().getMessage().contains("화면 정보 응답 지연"));}
        finally{release.countDown();}
        assertTrue(SystemClock.elapsedRealtime()-before<5000);h.main(()->h.svc.log("응답 지연 검증 완료"));
        assertFalse(MacroService.captureError(1).contains("보안 화면"));assertTrue(MacroService.captureError(6).contains("보안 화면"));
    }
}
