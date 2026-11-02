package kr.yunseok.touchmacro;

import android.accessibilityservice.AccessibilityService;
import android.os.*;
import android.provider.Settings;
import android.content.Intent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** 소유 검사 화면에서 실제 글자 인식과 제어된 캡처 지연을 검증합니다. */
@RunWith(AndroidJUnit4.class) public class ContinuousOcrTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    final AtomicInteger captures=new AtomicInteger(),taps=new AtomicInteger();
    Field source;MacroService.ScreenshotSource original;
    volatile AccessibilityService.TakeScreenshotCallback delayed;
    volatile java.util.concurrent.Executor callbackExecutor;
    @Before public void boot()throws Exception{
        h.boot();h.fixture();source=MacroService.class.getDeclaredField("screenshotSource");source.setAccessible(true);original=(MacroService.ScreenshotSource)source.get(h.svc);
        h.main(()->h.entry.setOnClickListener(v->taps.incrementAndGet()));
        h.waitFor(()->h.a.getPackageName().equals(h.svc.activePackage()),4000,"검사 앱 확인 실패");
    }
    @After public void end()throws Exception{h.end();if(source!=null&&original!=null)source.set(h.svc,original);}
    Step text(String value,String mode){Step s=new Step("TEXT_TAP");s.text=value;s.match="EXACT";s.source=mode;s.timeout=0;s.waitForever=true;s.immediate=true;s.after=60000;return s;}
    void start(Step... steps){Macro m=new Macro();m.name="계속 기다리기 검증";m.startDelay=0;m.maxMinutes=0;for(Step s:steps)m.steps.add(s);h.main(()->h.svc.start(m));}
    void delayFirst()throws Exception{source.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{if(captures.incrementAndGet()==1){callbackExecutor=executor;delayed=callback;}else original.take(executor,callback);});}
    void releaseDelayed(){h.main(()->original.take(callbackExecutor,delayed));}
    @Test public void delayedTextThenNextTextRunsWithoutSavedSixtySecondDelay()throws Exception{
        h.main(()->{h.entry.setText("아직 없음");h.entry.setOnClickListener(v->{int n=taps.incrementAndGet();h.entry.setText(n==1?"두번째 대상":"끝");});});
        start(text("첫번째 대상","ACCESS"),text("두번째 대상","ACCESS"));SystemClock.sleep(900);assertEquals(0,taps.get());assertTrue(h.svc.running);
        long before=SystemClock.elapsedRealtime();h.main(()->h.entry.setText("첫번째 대상"));h.waitFor(()->!h.svc.running,5000,"추가 대기가 생략되지 않았습니다");assertEquals(2,taps.get());assertTrue(SystemClock.elapsedRealtime()-before<5000);
    }
    @Test public void actualOcrTapUsesOneFreshCaptureAndNoSecondFixedCaptureDelay()throws Exception{
        source.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{captures.incrementAndGet();original.take(executor,callback);});
        start(text("안내 대상","OCR"));h.waitFor(()->!h.svc.running,18000,"OCR 인식 후 완료 실패");assertEquals(1,taps.get());assertEquals(1,captures.get());
    }
    @Test public void captureTimeoutUsesOneReserveThenDiscardsLateImage()throws Exception{
        delayFirst();start(text("안내 대상","OCR"));h.waitFor(()->delayed!=null,5000,"캡처 시작 실패");
        // 13차는 첫 무응답 뒤 예비 한 개를 허용하며 오래된 결과를 사용하지 않습니다.
        h.waitFor(()->!h.svc.running,22000,"예비 캡처 복구 실패");assertEquals(2,captures.get());assertEquals(1,taps.get());
        assertTrue(new ErrorJournal(h.a.getFilesDir()).read().contains("OCR_CAPTURE_TIMEOUT"));releaseDelayed();SystemClock.sleep(2500);
        assertEquals(2,captures.get());assertEquals(1,taps.get());
    }
    @Test public void realStopButtonWorksDuringUnansweredCaptureAndLateCallbackCannotTap()throws Exception{
        source.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{if(captures.incrementAndGet()==1){callbackExecutor=executor;delayed=callback;}});
        start(text("안내 대상","OCR"));h.waitFor(()->delayed!=null,5000,"캡처 시작 실패");
        h.waitFor(()->h.svc.progressText().contains("캡처 2개 무응답"),22000,"예비까지 무응답인 복구 대기 실패");assertEquals(2,captures.get());
        AccessibilityNodeInfo button=h.find("■");assertNotNull("캡처 중 실제 정지 버튼이 없습니다\n"+(button==null?ControlDiagnostics.capture(h,"capture-stop-missing"):""),button);assertTrue(button.isVisibleToUser());long before=SystemClock.elapsedRealtime();assertTrue(button.performAction(AccessibilityNodeInfo.ACTION_CLICK));button.recycle();h.waitFor(()->!h.svc.running,1000,"정지가 응답하지 않습니다");assertTrue(SystemClock.elapsedRealtime()-before<1200);
        releaseDelayed();SystemClock.sleep(3000);assertEquals(0,taps.get());assertEquals(2,captures.get());
    }
    @Test public void stopWhileTextNeverAppearsWorks()throws Exception{
        start(text("없는 글자","ACCESS"));SystemClock.sleep(800);assertTrue(h.svc.running);try{h.click("■");}catch(AssertionError error){throw new AssertionError(error.getMessage()+"\n"+ControlDiagnostics.capture(h,"text-stop-missing"),error);}h.waitFor(()->!h.svc.running,1000,"무한 글자 대기 정지 실패");assertEquals(0,taps.get());
    }
    @Test public void pauseDuringCaptureRequiresFreshScreenAfterResume()throws Exception{
        delayFirst();start(text("안내 대상","OCR"));h.waitFor(()->delayed!=null,5000,"캡처 시작 실패");h.main(()->h.svc.togglePause());releaseDelayed();SystemClock.sleep(900);assertEquals(0,taps.get());assertEquals(1,captures.get());assertTrue(h.svc.paused);
        h.main(()->h.svc.togglePause());h.waitFor(()->!h.svc.running,16000,"일시정지 후 재확인 실패");assertEquals(2,captures.get());assertEquals(1,taps.get());
    }
    @Test public void targetChangeDuringDelayedCaptureNeverTouchesOtherApp()throws Exception{
        delayFirst();start(text("안내 대상","OCR"));h.waitFor(()->delayed!=null,5000,"캡처 시작 실패");
        h.main(()->h.a.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));h.waitFor(()->!h.a.getPackageName().equals(h.svc.activePackage()),5000,"대상 변경 실패");releaseDelayed();SystemClock.sleep(3500);assertEquals(0,taps.get());assertTrue(h.svc.running);h.main(()->h.svc.stop());
    }
    @Test public void permissionFailureIsNotRetriedAsMissingText()throws Exception{
        source.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{captures.incrementAndGet();callback.onFailure(2);});start(text("안내 대상","OCR"));h.waitFor(()->!h.svc.running,10000,"권한 오류 중단 실패");assertEquals(1,captures.get());assertEquals(0,taps.get());assertTrue(h.svc.status.contains("권한 없음"));assertTrue(new ErrorJournal(h.a.getFilesDir()).read().contains("OCR_CAPTURE_ERROR_2"));
    }
}
