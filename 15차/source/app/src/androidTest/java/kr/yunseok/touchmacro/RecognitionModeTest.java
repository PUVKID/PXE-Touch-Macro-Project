package kr.yunseok.touchmacro;

import android.graphics.Rect;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** 인식 방식의 실제 적용 범위와 반복 탐지·즉시 진행을 소유 화면에서 검사합니다. */
@RunWith(AndroidJUnit4.class) public class RecognitionModeTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    final AtomicInteger reads=new AtomicInteger(),captures=new AtomicInteger(),entries=new AtomicInteger();
    Field screenField,captureField;
    MacroService.ScreenSource originalScreen;
    MacroService.ScreenshotSource originalCapture;

    @Before public void boot()throws Exception{
        h.boot();h.fixture();
        screenField=field("accessibilitySource");originalScreen=(MacroService.ScreenSource)screenField.get(h.svc);
        captureField=field("screenshotSource");originalCapture=(MacroService.ScreenshotSource)captureField.get(h.svc);
        h.main(()->h.entry.setOnClickListener(v->entries.incrementAndGet()));
        h.waitFor(()->h.a.getPackageName().equals(h.svc.activePackage()),4000,"검사 대상 앱 확인 실패");
        captureField.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{captures.incrementAndGet();callback.onFailure(6);});
    }
    @After public void end()throws Exception{
        h.end();
        if(screenField!=null)screenField.set(h.svc,originalScreen);
        if(captureField!=null)captureField.set(h.svc,originalCapture);
    }
    Field field(String name)throws Exception{Field value=MacroService.class.getDeclaredField(name);value.setAccessible(true);return value;}
    Macro macro(Step step){Macro value=new Macro();value.name="인식 방식 범위 검사";value.startDelay=0;value.maxMinutes=0;value.steps.add(step);return value;}
    WatchConfig profile(Macro macro)throws Exception{
        Rect[] region={null};h.main(()->region[0]=h.bounds(h.entry));int[] size=h.svc.size();
        WatchConfig value=new WatchConfig();value.rule.text="안내 대상";value.rule.match="EXACT";value.rule.source="ACCESS";
        value.rule.left=(region[0].left-2)*100.0/size[0];value.rule.top=(region[0].top-2)*100.0/size[1];
        value.rule.right=(region[0].right+2)*100.0/size[0];value.rule.bottom=(region[0].bottom+2)*100.0/size[1];
        value.targetPackage=h.a.getPackageName();value.macroName=macro.name;value.startMs=0;value.repetitions=1;value.entryMs=300;value.returnMs=300;value.settleMs=300;value.repeatGapMs=0;
        h.main(()->Store.put(h.a,macro));return value;
    }
    void countActualReads()throws Exception{screenField.set(h.svc,(MacroService.ScreenSource)token->{reads.incrementAndGet();return originalScreen.read(token);});}
    String evidence(){return "조회="+reads.get()+", 캡처="+captures.get()+", 진입="+entries.get()+"\n"+h.svc.progressText()+"\n"+h.svc.logText();}

    @Test public void strictAccessWithoutExposedTextNeverCapturesAndReportsItsMode()throws Exception{
        screenField.set(h.svc,(MacroService.ScreenSource)token->{reads.incrementAndGet();return new ArrayList<>();});
        Step step=new Step("TEXT_TAP");step.source="ACCESS";step.text="화면이 제공하지 않은 글자";step.timeout=0;step.immediate=true;step.after=0;
        h.main(()->h.svc.start(macro(step)));h.waitFor(()->!h.svc.running,4000,"화면 정보 부재의 유한 실패가 끝나지 않음");
        assertEquals(evidence(),0,captures.get());assertEquals(evidence(),0,entries.get());assertEquals(evidence(),1,reads.get());
        String errors=new ErrorJournal(h.a.getFilesDir()).read();int mark=errors.lastIndexOf(" | TEXT_NOT_FOUND | ");assertTrue(errors,mark>=0);
        int end=errors.indexOf('\n',mark);String last=errors.substring(mark,end<0?errors.length():end);
        assertTrue("요청 방식과 실제 방식이 오류 기록에서 구분되지 않습니다\n"+last,last.contains("requested_source=ACCESS")&&last.contains("actual_source=ACCESS"));
        assertTrue("읽을 수 있는 글자가 없다는 화면 정보 안내가 없습니다\n"+evidence(),h.svc.status.contains("화면 정보"));
        assertFalse(evidence(),h.svc.status.contains("보안 화면"));assertFalse(last,last.contains(step.text));
    }
    @Test public void accessDetectionThenOcrStepReportsActualStageAndSecureRefusal()throws Exception{
        countActualReads();Step step=new Step("TEXT_TAP");step.source="OCR";step.text="안내 대상";step.waitForever=true;step.immediate=true;step.after=0;
        WatchConfig value=profile(macro(step));value.rule.immediate=true;
        h.main(()->h.svc.startWatch(value));h.waitFor(()->!h.svc.running,7000,"실제 OCR 단계의 보안 거부가 정지하지 않음");
        assertEquals(evidence(),1,entries.get());assertEquals(evidence(),1,captures.get());assertEquals(evidence(),0,h.svc.watchCompleted);
        String errors=new ErrorJournal(h.a.getFilesDir()).read();int mark=errors.lastIndexOf(" | OCR_CAPTURE_ERROR_6 | ");assertTrue(errors,mark>=0);
        int end=errors.indexOf('\n',mark);String last=errors.substring(mark,end<0?errors.length():end);
        assertTrue("탐지와 실행 동작의 방식이 분리되어 기록되지 않습니다\n"+last,last.contains("requested_source=OCR")&&last.contains("actual_source=OCR")&&last.contains("실행 동작 1"));
        assertTrue(evidence(),h.svc.status.contains("실행 동작 1"));assertTrue(evidence(),h.svc.status.contains("보안 화면"));
        assertFalse("화면 정보만 고르면 항상 읽힌다는 잘못된 안내가 남았습니다\n"+evidence(),h.svc.status.contains("화면 정보 모드를 사용하세요"));
    }
    @Test public void watchPollsAccessWithoutAnExtraTwoSecondTail()throws Exception{
        CountDownLatch first=new CountDownLatch(1);h.main(()->h.entry.setText("아직 없음"));
        screenField.set(h.svc,(MacroService.ScreenSource)token->{ArrayList<MacroService.Hit> result=originalScreen.read(token);if(reads.incrementAndGet()==1)first.countDown();return result;});
        Step step=new Step("WAIT");step.duration=5000;WatchConfig value=profile(macro(step));value.rule.immediate=true;
        h.main(()->h.svc.startWatch(value));assertTrue("첫 실제 화면 정보 조회가 없습니다",first.await(4,TimeUnit.SECONDS));
        long appeared=SystemClock.elapsedRealtime();h.main(()->h.entry.setText("안내 대상"));
        h.waitFor(()->entries.get()==1,1200,"인식 가능한 글자 뒤에 탐지 루프 2초 대기가 남았습니다");
        assertTrue(evidence(),SystemClock.elapsedRealtime()-appeared<1200);assertEquals(evidence(),0,captures.get());
    }
    @Test public void immediateWatchUsesTheFirstHitWithoutASecondRead()throws Exception{
        countActualReads();AtomicInteger readsAtTouch=new AtomicInteger();h.main(()->h.entry.setOnClickListener(v->{readsAtTouch.set(reads.get());entries.incrementAndGet();}));
        Step step=new Step("WAIT");step.duration=5000;WatchConfig value=profile(macro(step));value.rule.immediate=true;
        h.main(()->h.svc.startWatch(value));h.waitFor(()->entries.get()==1,4000,"즉시 탐지의 실제 진입이 없습니다");
        assertEquals("즉시 진행인데 별도 재확인이 추가되었습니다\n"+evidence(),1,readsAtTouch.get());assertEquals(evidence(),0,captures.get());
    }
    @Test public void defaultWatchStillRechecksBeforeTouching()throws Exception{
        countActualReads();AtomicInteger readsAtTouch=new AtomicInteger();h.main(()->h.entry.setOnClickListener(v->{readsAtTouch.set(reads.get());entries.incrementAndGet();}));
        Step step=new Step("WAIT");step.duration=5000;WatchConfig value=profile(macro(step));assertFalse(value.rule.immediate);
        h.main(()->h.svc.startWatch(value));h.waitFor(()->entries.get()==1,4000,"기본 탐지의 실제 진입이 없습니다");
        assertEquals("기본값의 재확인이 사라졌습니다\n"+evidence(),2,readsAtTouch.get());assertEquals(evidence(),0,captures.get());
    }
}
