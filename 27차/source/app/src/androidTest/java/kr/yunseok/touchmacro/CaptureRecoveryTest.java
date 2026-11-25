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
        final List<Long> responses=Collections.synchronizedList(new ArrayList<>());
        source.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{
            requests.add(SystemClock.elapsedRealtime());
            if(attempts.incrementAndGet()<=2){responses.add(SystemClock.elapsedRealtime());callback.onFailure(3);}
            else ((MacroService.ScreenshotSource)original).take(executor,new android.accessibilityservice.AccessibilityService.TakeScreenshotCallback(){
                @Override public void onSuccess(android.accessibilityservice.AccessibilityService.ScreenshotResult result){responses.add(SystemClock.elapsedRealtime());callback.onSuccess(result);}
                @Override public void onFailure(int error){responses.add(SystemClock.elapsedRealtime());callback.onFailure(error);}
            });
        });
        start();h.waitFor(()->attempts.get()>=2,12000,"Second capture attempt missing");assertEquals(0,entries);
        h.waitFor(()->h.svc.progressText().contains("캡처 간격"),3000,"Retry countdown not shown");
        long before=SystemClock.elapsedRealtime();h.main(()->{});assertTrue("Retry blocked UI",SystemClock.elapsedRealtime()-before<1000);
        h.waitFor(()->h.svc.watchCompleted==1,45000,"OCR did not recover after interval failures");
        assertEquals(1,entries);assertEquals("Expected 2 failures + OCR + fresh pre-tap confirmation",4,attempts.get());
        assertEquals("실제 캡처 응답이 모두 전달되어야 합니다",4,responses.size());
        // 제품의 내부 값을 읽지 않고 15차 명세의 기본 간격과 2배 후퇴·절반 회복을 검증합니다.
        long base=android.os.Build.VERSION.SDK_INT<=30?1100:500;
        long firstGap=requests.get(1)-responses.get(0),secondGap=requests.get(2)-responses.get(1),successGap=requests.get(3)-responses.get(2);
        String timing="실제 응답→다음 요청 간격(ms): 첫 거절 뒤="+firstGap+", 두 번째 거절 뒤="+secondGap+", 성공 후 재확인="+successGap;
        assertTrue("첫 거절 뒤 "+(base*2)+"ms 하한 · "+timing,firstGap>=base*2);assertTrue("두 번째 거절 뒤 "+(base*4)+"ms 하한 · "+timing,secondGap>=base*4);assertTrue("성공 후 "+(base*2)+"ms 하한 · "+timing,successGap>=base*2);
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
