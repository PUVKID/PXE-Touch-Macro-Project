package kr.yunseok.touchmacro;
import android.app.AlertDialog;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;import org.junit.runner.RunWith;
import java.lang.reflect.Field;import java.util.*;import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class) public class CaptureRecoveryTest {
    GuidedFlowTest h=new GuidedFlowTest();AtomicInteger attempts=new AtomicInteger();volatile int entries;
    final List<Long> requests=Collections.synchronizedList(new ArrayList<>());
    Object original;Field source;
    @Before public void boot()throws Exception{
        h.boot();h.fixture();source=MacroService.class.getDeclaredField("screenshotSource");source.setAccessible(true);original=source.get(h.svc);
        h.main(()->{h.detail=new AlertDialog.Builder(h.a).setTitle("작업 화면").setMessage("캡처 성공 후에만 들어옵니다").create();h.entry.setOnClickListener(v->{entries++;h.detail.show();});});
    }
    @After public void end()throws Exception{h.end();if(source!=null&&original!=null)source.set(h.svc,original);}
    void inject(int failures)throws Exception{
        source.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{
            requests.add(SystemClock.elapsedRealtime());
            if(attempts.incrementAndGet()<=failures)callback.onFailure(3);
            else ((MacroService.ScreenshotSource)original).take(executor,callback);
        });
    }
    void start(){
        WatchConfig p=new WatchConfig();p.rule.source="OCR";p.rule.text="안내 대상";p.rule.match="EXACT";
        p.startMs=0;p.entryMs=300;p.returnMs=300;p.repeatGapMs=0;p.repetitions=1;p.cooldownMs=600000;
        Macro m=new Macro();m.name="캡처 재시도 검증";Step wait=new Step("WAIT");wait.duration=10;wait.after=0;m.steps.add(wait);p.macroName=m.name;
        h.main(()->{Store.put(h.a,m);h.svc.startWatch(p);});
    }
    @Test public void twoIntervalFailuresRecoverAndFreshOcrConfirmationPrecedesOneEntry()throws Exception{
        inject(2);start();h.waitFor(()->attempts.get()>=2,12000,"Second capture attempt missing");assertEquals(0,entries);
        h.waitFor(()->h.svc.progressText().contains("캡처 간격"),3000,"Retry countdown not shown");
        long before=SystemClock.elapsedRealtime();h.main(()->{});assertTrue("Retry blocked UI",SystemClock.elapsedRealtime()-before<1000);
        h.waitFor(()->h.svc.watchCompleted==1,45000,"OCR did not recover after interval failures");
        assertEquals(1,entries);assertEquals("Expected 2 failures + OCR + fresh pre-tap confirmation",4,attempts.get());
        assertTrue(requests.get(1)-requests.get(0)>=4000);assertTrue(requests.get(2)-requests.get(1)>=8000);assertTrue(requests.get(3)-requests.get(2)>=8000);
        SystemClock.sleep(1000);assertEquals("Executed again during cooldown",1,entries);
    }
    @Test public void stopDuringRetryDoesNotCaptureOrEnterLater()throws Exception{
        inject(Integer.MAX_VALUE);start();h.waitFor(()->attempts.get()==1,8000,"No first capture");
        h.waitFor(()->h.svc.progressText().contains("캡처 간격"),3000,"No retry state");
        long before=SystemClock.elapsedRealtime();h.main(()->h.svc.stop());assertTrue(SystemClock.elapsedRealtime()-before<1000);
        SystemClock.sleep(5000);assertEquals(1,attempts.get());assertEquals(0,entries);assertFalse(h.svc.running);
    }
    @Test public void persistentIntervalErrorKeepsSafeRecoveryUntilUserStops()throws Exception{
        inject(Integer.MAX_VALUE);start();h.waitFor(()->attempts.get()>=3,22000,"캡처 복구 재시도가 없습니다");
        assertTrue(h.svc.running);assertEquals(0,entries);assertEquals(0,h.svc.watchCompleted);h.main(()->h.svc.stop());assertFalse(h.svc.running);
    }
}
