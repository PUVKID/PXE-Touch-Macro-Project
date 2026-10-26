package kr.yunseok.touchmacro;

import android.accessibilityservice.AccessibilityService;
import android.os.SystemClock;
import android.content.Intent;
import android.provider.Settings;
import com.google.android.gms.tasks.*;
import com.google.mlkit.vision.text.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** 소유 검사 화면에서 실제 OCR 결과 전달만 늦춰 복구와 오래된 결과 폐기를 검사합니다. */
@RunWith(AndroidJUnit4.class) public class OcrStallRecoveryTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    final AtomicInteger captures=new AtomicInteger(),recognitions=new AtomicInteger(),taps=new AtomicInteger();
    final AtomicReference<Text> firstText=new AtomicReference<>();
    final TaskCompletionSource<Text> delayedText=new TaskCompletionSource<>();
    Field screenshotField,recognizerField;
    MacroService.ScreenshotSource originalScreenshot;
    TextRecognizer originalRecognizer;
    volatile AccessibilityService.TakeScreenshotCallback firstCapture;
    volatile java.util.concurrent.Executor firstExecutor;

    @Before public void boot()throws Exception{
        h.boot();h.fixture();
        screenshotField=field("screenshotSource");originalScreenshot=(MacroService.ScreenshotSource)screenshotField.get(h.svc);
        recognizerField=field("recognizer");originalRecognizer=(TextRecognizer)recognizerField.get(h.svc);
        h.main(()->h.entry.setOnClickListener(v->taps.incrementAndGet()));
        h.waitFor(()->h.a.getPackageName().equals(h.svc.activePackage()),4000,"검사 앱 확인 실패");
    }
    @After public void end()throws Exception{
        h.end();
        if(firstText.get()!=null)delayedText.trySetResult(firstText.get());
        if(screenshotField!=null&&originalScreenshot!=null)screenshotField.set(h.svc,originalScreenshot);
        if(recognizerField!=null&&originalRecognizer!=null)recognizerField.set(h.svc,originalRecognizer);
    }
    Field field(String name)throws Exception{Field f=MacroService.class.getDeclaredField(name);f.setAccessible(true);return f;}
    Step text(){Step s=new Step("TEXT_TAP");s.text="안내 대상";s.match="EXACT";s.source="OCR";s.timeout=0;s.waitForever=true;s.immediate=true;s.after=0;return s;}
    void start(){Macro m=new Macro();m.name="인식 지연 복구 검사";m.startDelay=0;m.maxMinutes=0;m.steps.add(text());h.main(()->h.svc.start(m));}
    void countCaptures()throws Exception{
        screenshotField.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{captures.incrementAndGet();originalScreenshot.take(executor,callback);});
    }
    void delayFirstRecognition()throws Exception{
        recognizerField.set(h.svc,Proxy.newProxyInstance(TextRecognizer.class.getClassLoader(),new Class<?>[]{TextRecognizer.class},(proxy,method,args)->{
            if(!method.getName().equals("process"))return method.invoke(originalRecognizer,args);
            @SuppressWarnings("unchecked") Task<Text> actual=(Task<Text>)method.invoke(originalRecognizer,args);
            if(recognitions.incrementAndGet()!=1)return actual;
            actual.addOnSuccessListener(firstText::set).addOnFailureListener(delayedText::trySetException);
            return delayedText.getTask();
        }));
    }
    void captureWithoutFirstResponse()throws Exception{
        screenshotField.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{
            if(captures.incrementAndGet()==1){firstCapture=callback;firstExecutor=executor;}else originalScreenshot.take(executor,callback);
        });
    }
    @Test public void recognitionTimeoutWaitsForOneTaskThenFreshImageRecovers()throws Exception{
        countCaptures();delayFirstRecognition();start();h.waitFor(()->firstText.get()!=null,18000,"첫 실제 OCR 결과 준비 실패");
        SystemClock.sleep(13000);assertTrue("12초 인식 지연이 실행 종료로 전파됐습니다",h.svc.running);
        assertEquals(0,taps.get());assertEquals(1,captures.get());assertEquals(1,recognitions.get());
        assertTrue(new ErrorJournal(h.a.getFilesDir()).read().contains("OCR_RECOGNITION_TIMEOUT"));
        h.main(()->delayedText.trySetResult(firstText.get()));
        h.waitFor(()->!h.svc.running,18000,"늦은 OCR 완료 후 새 화면 인식 복구 실패");
        assertEquals("늦은 결과로 바로 터치하면 새 캡처가 빠집니다",2,captures.get());assertEquals(2,recognitions.get());assertEquals(1,taps.get());
    }
    @Test public void stoppedRecognitionDoesNotAccumulateAcrossRestart()throws Exception{
        countCaptures();delayFirstRecognition();start();h.waitFor(()->firstText.get()!=null,18000,"첫 OCR 시작 실패");
        h.main(()->h.svc.stop());start();SystemClock.sleep(14000);
        assertTrue(h.svc.running);assertEquals("재실행에서 미완료 OCR 뒤에 작업이 쌓였습니다",1,recognitions.get());assertEquals(1,captures.get());assertEquals(0,taps.get());
        long before=SystemClock.elapsedRealtime();h.main(()->h.svc.stop());assertTrue(SystemClock.elapsedRealtime()-before<1000);
        h.main(()->delayedText.trySetResult(firstText.get()));SystemClock.sleep(1000);assertEquals(0,taps.get());
    }
    @Test public void pauseDuringRecognitionRequiresFreshImageAfterResume()throws Exception{
        countCaptures();delayFirstRecognition();start();h.waitFor(()->firstText.get()!=null,18000,"첫 OCR 시작 실패");
        h.main(()->{h.svc.togglePause();delayedText.trySetResult(firstText.get());});SystemClock.sleep(1000);
        assertEquals(0,taps.get());assertEquals(1,captures.get());assertTrue(h.svc.paused);
        h.main(()->h.svc.togglePause());h.waitFor(()->!h.svc.running,18000,"인식 중 일시정지 후 새 화면 복구 실패");
        assertEquals(2,captures.get());assertEquals(2,recognitions.get());assertEquals(1,taps.get());
    }
    @Test public void targetChangeDuringRecognitionNeverTouchesTheOtherApp()throws Exception{
        countCaptures();delayFirstRecognition();start();h.waitFor(()->firstText.get()!=null,18000,"첫 OCR 시작 실패");
        h.main(()->h.a.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        h.waitFor(()->!h.a.getPackageName().equals(h.svc.activePackage()),5000,"다른 앱 전환 실패");
        SystemClock.sleep(4500);
        assertTrue("미완료 OCR을 기다리느라 대상 앱 대기로 돌아오지 못했습니다: "+h.svc.progressText(),h.svc.progressText().contains("대상 앱 확인 대기"));
        assertEquals(1,captures.get());assertEquals(1,recognitions.get());assertFalse(delayedText.getTask().isComplete());
        Object pending=((AtomicReference<?>)field("pendingRecognition").get(h.svc)).get();assertNotNull("실제 미완료 OCR을 해제했습니다",pending);
        Field bitmap=pending.getClass().getDeclaredField("bitmap");bitmap.setAccessible(true);
        assertFalse("Task 완료 전에 이미지를 해제했습니다",((android.graphics.Bitmap)bitmap.get(pending)).isRecycled());
        h.main(()->delayedText.trySetResult(firstText.get()));SystemClock.sleep(2500);
        assertEquals(0,taps.get());assertTrue(h.svc.running);assertEquals(1,captures.get());
    }
    @Test public void targetChangeDuringNonImmediateConfirmationCannotCompleteTouch()throws Exception{
        countCaptures();
        recognizerField.set(h.svc,Proxy.newProxyInstance(TextRecognizer.class.getClassLoader(),new Class<?>[]{TextRecognizer.class},(proxy,method,args)->{
            if(!method.getName().equals("process"))return method.invoke(originalRecognizer,args);
            @SuppressWarnings("unchecked") Task<Text> actual=(Task<Text>)method.invoke(originalRecognizer,args);
            if(recognitions.incrementAndGet()!=2)return actual;
            actual.addOnSuccessListener(firstText::set).addOnFailureListener(delayedText::trySetException);return delayedText.getTask();
        }));
        Step step=text();step.immediate=false;Macro m=new Macro();m.name="재확인 대상 전환 검사";m.startDelay=0;m.maxMinutes=0;m.steps.add(step);
        h.main(()->h.svc.start(m));h.waitFor(()->firstText.get()!=null,18000,"두 번째 실제 OCR 시작 실패");
        assertEquals(2,captures.get());assertEquals(2,recognitions.get());
        h.main(()->h.a.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        h.waitFor(()->!h.a.getPackageName().equals(h.svc.activePackage()),5000,"다른 앱 전환 실패");
        h.main(()->delayedText.trySetResult(firstText.get()));SystemClock.sleep(2500);
        String evidence="대상="+h.svc.activePackage()+" · 진행="+h.svc.progressText()+"\n"+h.svc.logText();
        // 이 완료 로그는 실제 제스처 onCompleted 뒤 execute가 반환해야만 기록됩니다.
        assertFalse("다른 앱에서 이전 재확인 화면의 터치가 완료됐습니다\n"+evidence,h.svc.logText().contains("1번 글자 찾아 터치 완료"));
        assertTrue(evidence,h.svc.running);assertEquals(0,taps.get());assertEquals(2,captures.get());assertEquals(2,recognitions.get());
        assertTrue(evidence,h.svc.progressText().contains("대상 앱 확인 대기"));
        long before=SystemClock.elapsedRealtime();h.main(()->h.svc.stop());assertTrue(SystemClock.elapsedRealtime()-before<1000);
        Field window=field("windowSource");Object originalWindow=window.get(h.svc);
        try{
            // 대상 창을 아직 모르는 유한 대기에서는 새 OCR이나 다른 앱 터치를 시작하지 않습니다.
            window.set(h.svc,(MacroService.WindowSource)java.util.Collections::emptyList);
            Step finite=text();finite.immediate=false;finite.waitForever=false;finite.timeout=400;
            Macro bounded=new Macro();bounded.name="미확정 대상 유한 대기 검사";bounded.startDelay=0;bounded.maxMinutes=0;bounded.steps.add(finite);
            h.main(()->h.svc.start(bounded));h.waitFor(()->!h.svc.running,2000,"미확정 대상에서 유한 timeout이 지켜지지 않았습니다");
            assertEquals(2,captures.get());assertEquals(2,recognitions.get());assertEquals(0,taps.get());
            assertTrue(h.svc.logText(),h.svc.logText().contains("대상 앱 확인 시간이 초과"));
        }finally{window.set(h.svc,originalWindow);}
    }
    @Test public void finiteTargetChangeDoesNotBackRetryOnAnotherApp()throws Exception{
        h.main(()->h.entry.setText("아직 나타나지 않은 글자"));countCaptures();delayFirstRecognition();
        Step step=text();step.immediate=false;step.waitForever=false;step.timeout=400;step.failure="BACK_RETRY";step.retries=2;step.retryDelay=300;
        Macro m=new Macro();m.name="유한 대기 대상 전환 검사";m.startDelay=0;m.maxMinutes=0;m.steps.add(step);
        h.main(()->h.svc.start(m));h.waitFor(()->firstText.get()!=null,18000,"유한 대기의 첫 실제 OCR 준비 실패");
        h.main(()->h.a.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        h.waitFor(()->"com.android.settings".equals(h.svc.activePackage()),5000,"Settings 대상 전환 실패");
        h.main(()->delayedText.trySetResult(firstText.get()));h.waitFor(()->!h.svc.running,2500,"대상 이탈 후 유한 timeout이 지켜지지 않았습니다");
        String evidence=h.svc.logText();assertTrue(evidence,evidence.contains("대상 앱 확인 시간이 초과"));
        assertFalse("다른 앱에 뒤로가기 재시도를 보냈습니다\n"+evidence,evidence.contains("글자 없음 · 뒤로가기"));
        assertEquals("뒤로가기 등으로 다른 화면이 열렸습니다", "com.android.settings",h.svc.activePackage());
        assertEquals(1,captures.get());assertEquals(1,recognitions.get());assertEquals(0,taps.get());
    }
    @Test public void unansweredFirstCaptureUsesOneReserveAndLateOldResultCannotTouch()throws Exception{
        captureWithoutFirstResponse();start();h.waitFor(()->firstCapture!=null,5000,"첫 캡처 시작 실패");
        h.waitFor(()->!h.svc.running,25000,"첫 캡처 무응답을 예비 캡처로 복구하지 못했습니다");
        assertEquals(2,captures.get());assertEquals(1,taps.get());
        h.main(()->originalScreenshot.take(firstExecutor,firstCapture));SystemClock.sleep(2500);assertEquals(1,taps.get());
    }
    @Test public void twoUnansweredCapturesStayBoundedAcrossRestartAndStop()throws Exception{
        screenshotField.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->captures.incrementAndGet());
        start();h.waitFor(()->captures.get()==2,22000,"제한된 예비 캡처가 없습니다");SystemClock.sleep(10000);
        assertTrue(h.svc.running);assertEquals(2,captures.get());assertEquals(0,taps.get());
        assertTrue(h.svc.progressText().contains("접근성"));h.main(()->h.svc.stop());start();SystemClock.sleep(3000);
        assertEquals("중단 후 재실행이 실제 미응답 개수를 초기화했습니다",2,captures.get());assertTrue(h.svc.running);
        long before=SystemClock.elapsedRealtime();h.main(()->h.svc.stop());assertTrue(SystemClock.elapsedRealtime()-before<1000);assertEquals(0,taps.get());
    }
}
