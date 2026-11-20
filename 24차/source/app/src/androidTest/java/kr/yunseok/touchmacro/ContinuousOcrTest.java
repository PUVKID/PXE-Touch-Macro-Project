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
        CaptureTrace trace=new CaptureTrace(original);Throwable primary=null;
        try{
            source.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{
                int count=captures.incrementAndGet();trace.event("SOURCE_REQUEST",0,-1);
                if(count==1){callbackExecutor=executor;delayed=callback;trace.event("SOURCE_HELD",0,-1);}else trace.take(executor,callback);
            });
            start(text("안내 대상","OCR"));h.waitFor(()->delayed!=null,5000,"캡처 시작 실패");
            trace.event("BEFORE_PAUSE",0,-1);h.main(()->h.svc.togglePause());trace.event("AFTER_PAUSE",0,-1);
            h.main(()->trace.take(callbackExecutor,delayed));SystemClock.sleep(900);trace.event("AFTER_900_MS",0,-1);
            assertEquals(0,taps.get());assertEquals(1,captures.get());assertTrue(h.svc.paused);
            trace.event("BEFORE_RESUME",0,-1);h.main(()->h.svc.togglePause());trace.event("AFTER_RESUME",0,-1);
            h.waitFor(()->!h.svc.running,16000,"일시정지 후 재확인 실패");trace.event("BEFORE_FINAL_ASSERT",0,-1);assertEquals(2,captures.get());assertEquals(1,taps.get());
        }catch(Exception|Error error){primary=error;throw error;}
        finally{try{trace.send();}catch(Exception|Error observation){if(primary!=null)primary.addSuppressed(observation);else throw observation;}}
    }
    @Test public void targetChangeDuringDelayedCaptureNeverTouchesOtherApp()throws Exception{
        delayFirst();start(text("안내 대상","OCR"));h.waitFor(()->delayed!=null,5000,"캡처 시작 실패");
        h.main(()->h.a.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));h.waitFor(()->!h.a.getPackageName().equals(h.svc.activePackage()),5000,"대상 변경 실패");releaseDelayed();SystemClock.sleep(3500);assertEquals(0,taps.get());assertTrue(h.svc.running);h.main(()->h.svc.stop());
    }
    @Test public void permissionFailureIsNotRetriedAsMissingText()throws Exception{
        source.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{captures.incrementAndGet();callback.onFailure(2);});start(text("안내 대상","OCR"));h.waitFor(()->!h.svc.running,10000,"권한 오류 중단 실패");assertEquals(1,captures.get());assertEquals(0,taps.get());assertTrue(h.svc.status.contains("권한 없음"));assertTrue(new ErrorJournal(h.a.getFilesDir()).read().contains("OCR_CAPTURE_ERROR_2"));
    }
    @Test public void capturePreparationDelayRecoversWithOneFreshCaptureAndNoLateTap()throws Exception{
        AtomicLong capturedAt=new AtomicLong();
        source.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{captures.incrementAndGet();capturedAt.compareAndSet(0,SystemClock.elapsedRealtimeNanos());original.take(executor,callback);});
        try(UiPreparationDelay delay=new UiPreparationDelay(h)){
            start(text("안내 대상","OCR"));delay.awaitSelected();
            h.waitFor(()->!h.svc.running,18000,"준비 지연 뒤 새 OCR 터치가 완료되지 않았습니다");
            delay.awaitLateBoundary();delay.assertSettled();
            assertEquals(1,taps.get());assertEquals(1,captures.get());assertEquals(1,delay.delayedCount.get());
            assertTrue("원래 5초 예산 뒤의 새 캡처가 아닙니다",capturedAt.get()>delay.selectedAtNs+5000000000L);
            long until=SystemClock.elapsedRealtime()+500;while(SystemClock.elapsedRealtime()<until){assertEquals("늦은 준비 뒤 중복 터치가 발생했습니다",1,taps.get());SystemClock.sleep(20);}
        }
    }
    @Test public void stopDuringCapturePreparationDelayNeverCapturesOrTouchesLater()throws Exception{
        source.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{captures.incrementAndGet();original.take(executor,callback);});
        try(UiPreparationDelay delay=new UiPreparationDelay(h)){
            start(text("안내 대상","OCR"));delay.awaitSelected();
            // 준비 콜백 선택과 실제 제어창의 접근성 표시 완료를 구분합니다.
            h.waitFor(()->{AccessibilityNodeInfo node=h.find("■");if(node==null)return false;try{return node.isVisibleToUser();}finally{node.recycle();}},3000,"준비 지연 중 실제 정지 버튼이 3초 안에 표시되지 않았습니다");
            AccessibilityNodeInfo stop=h.find("■");assertNotNull("준비 지연 중 실제 정지 버튼이 없습니다",stop);
            long requested=SystemClock.elapsedRealtimeNanos();try{assertTrue(stop.isVisibleToUser());assertTrue(stop.performAction(AccessibilityNodeInfo.ACTION_CLICK));}finally{stop.recycle();}
            h.waitFor(()->!h.svc.running,1000,"준비 지연 중 정지가 1초 안에 반응하지 않았습니다");
            assertTrue(SystemClock.elapsedRealtimeNanos()-requested<=1000000000L);
            delay.awaitLateBoundary();delay.assertSettled();assertEquals(0,captures.get());assertEquals(0,taps.get());assertFalse(h.svc.running);
        }
    }


    /** 화면·글자·사용자 로그 없이 캡처의 고정 상태만 최대 128개 기록합니다. */
    private final class CaptureTrace implements MacroService.ScreenshotSource {
        private final MacroService.ScreenshotSource delegate;
        private final org.json.JSONArray events=new org.json.JSONArray();
        private final AtomicInteger requests=new AtomicInteger();
        private int dropped,observationErrors;
        private String firstObservationError="";
        CaptureTrace(MacroService.ScreenshotSource delegate){this.delegate=delegate;}
        private Object read(String name)throws Exception{Field field=MacroService.class.getDeclaredField(name);field.setAccessible(true);return field.get(h.svc);}
        synchronized void event(String kind,int request,int error){
            if(events.length()>=128){dropped++;return;}
            try{
                events.put(new org.json.JSONObject().put("event",kind).put("request",request).put("error_code",error)
                    .put("elapsed_ns",SystemClock.elapsedRealtimeNanos()).put("paused",h.svc.paused)
                    .put("pause_revision",((AtomicLong)read("pauseRevision")).get()).put("window_revision",((AtomicLong)read("windowRevision")).get())
                    .put("generation",((AtomicLong)read("generation")).get()).put("captures",captures.get()).put("taps",taps.get())
                    .put("running",h.svc.running).put("outcome",read("runOutcome")));
            }catch(Throwable observation){observationErrors++;if(firstObservationError.isEmpty())firstObservationError=observation.getClass().getName();}
        }
        @Override public void take(java.util.concurrent.Executor executor,AccessibilityService.TakeScreenshotCallback callback){
            int request=requests.incrementAndGet();event("OS_REQUEST",request,-1);
            // 같은 executor와 원 결과를 한 번 전달하며 버퍼를 읽거나 해제하지 않습니다.
            delegate.take(executor,new AccessibilityService.TakeScreenshotCallback(){
                @Override public void onSuccess(AccessibilityService.ScreenshotResult result){
                    event("OS_SUCCESS",request,-1);try{callback.onSuccess(result);}finally{event("SUCCESS_DELEGATE_RETURN",request,-1);}
                }
                @Override public void onFailure(int error){
                    event("OS_FAILURE",request,error);try{callback.onFailure(error);}finally{event("FAILURE_DELEGATE_RETURN",request,error);}
                }
            });
        }
        void send()throws Exception{
            event("FINALLY",0,-1);String value;
            synchronized(this){value=new org.json.JSONObject().put("schema_version",1).put("event_limit",128).put("events",events)
                .put("dropped_events",dropped).put("observation_errors",observationErrors).put("first_observation_error_type",firstObservationError).toString();}
            Bundle status=new Bundle();status.putString("pxe.capture.trace",value);
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendStatus(2,status);
        }
    }
}
