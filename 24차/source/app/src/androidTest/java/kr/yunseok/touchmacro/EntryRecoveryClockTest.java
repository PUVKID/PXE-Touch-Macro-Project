package kr.yunseok.touchmacro;

import android.accessibilityservice.AccessibilityService;
import android.os.SystemClock;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognizer;
import java.lang.reflect.*;
import java.util.concurrent.atomic.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 복구를 못 읽은 시간과 정상 화면에서 확인한 부재 시간을 분리합니다. */
@RunWith(AndroidJUnit4.class) public class EntryRecoveryClockTest {
    final EntryConfirmationTest base=new EntryConfirmationTest();
    final AtomicLong normalAt=new AtomicLong();final AtomicInteger recognitions=new AtomicInteger(),captures=new AtomicInteger();
    final AtomicReference<Text> delayedValue=new AtomicReference<>();final TaskCompletionSource<Text> delayed=new TaskCompletionSource<>();
    Field recognizerField,screenshotField;TextRecognizer originalRecognizer;MacroService.ScreenshotSource originalScreenshot;
    volatile AccessibilityService.TakeScreenshotCallback heldCapture;
    @Before public void boot()throws Exception{
        base.boot();base.profile.entryRule.source="OCR";base.profile.entryRule.timeout=3000;base.profile.rejectText="";
        base.h.main(()->base.expected.setText("없음"));
        recognizerField=MacroService.class.getDeclaredField("recognizer");recognizerField.setAccessible(true);originalRecognizer=(TextRecognizer)recognizerField.get(base.h.svc);
        screenshotField=MacroService.class.getDeclaredField("screenshotSource");screenshotField.setAccessible(true);originalScreenshot=(MacroService.ScreenshotSource)screenshotField.get(base.h.svc);
    }
    @After public void end()throws Exception{
        base.h.main(()->base.h.svc.stop());if(delayedValue.get()!=null)base.h.main(()->delayed.trySetResult(delayedValue.get()));
        if(heldCapture!=null)base.h.main(()->heldCapture.onFailure(AccessibilityService.ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT));
        recognizerField.set(base.h.svc,originalRecognizer);screenshotField.set(base.h.svc,originalScreenshot);base.end();
    }
    void proxy(boolean delayFirst)throws Exception{
        recognizerField.set(base.h.svc,Proxy.newProxyInstance(TextRecognizer.class.getClassLoader(),new Class<?>[]{TextRecognizer.class},(proxy,method,args)->{
            if(!method.getName().equals("process"))return method.invoke(originalRecognizer,args);
            @SuppressWarnings("unchecked") Task<Text> actual=(Task<Text>)method.invoke(originalRecognizer,args);int number=recognitions.incrementAndGet();
            if(delayFirst&&number==1){actual.addOnSuccessListener(delayedValue::set).addOnFailureListener(delayed::trySetException);return delayed.getTask();}
            actual.addOnSuccessListener(text->normalAt.compareAndSet(0,SystemClock.elapsedRealtime()));return actual;
        }));
    }
    void assertFreshConfirmationBudget()throws Exception{
        base.h.waitFor(()->normalAt.get()>0,12000,"복구 뒤 첫 실제 OCR 프레임이 준비되지 않았습니다");SystemClock.sleep(2000);
        assertEquals("읽기 복구 시간이 확인 기한을 소비해 첫 정상 부재에서 즉시 뒤로갔습니다",0,base.backs.get());assertEquals(0,base.actions.get());assertEquals(0,base.h.svc.watchCompleted);
        base.h.waitFor(()->base.backs.get()>=1,5000,"새 정상 확인 3초 뒤 복귀하지 않았습니다");base.h.main(()->base.h.svc.stop());
        assertTrue("정상 확인 3초보다 먼저 복귀했습니다",SystemClock.elapsedRealtime()-normalAt.get()>=3000);assertEquals(0,base.actions.get());assertEquals(0,base.h.svc.watchCompleted);
    }
    @Test public void ocrTimeoutRecoveryStartsNewThreeSecondConfirmationBudget()throws Exception{
        proxy(true);base.start();base.h.waitFor(()->delayedValue.get()!=null,18000,"첫 실제 OCR이 준비되지 않았습니다");
        base.h.waitFor(()->base.h.svc.logText().contains("OCR 인식 응답 지연"),14000,"제어한 12초 OCR 응답 지연이 발생하지 않았습니다");assertEquals(0,base.backs.get());
        base.h.main(()->delayed.trySetResult(delayedValue.get()));assertFreshConfirmationBudget();
    }
    @Test public void captureTimeoutRecoveryStartsNewThreeSecondConfirmationBudget()throws Exception{
        proxy(false);screenshotField.set(base.h.svc,(MacroService.ScreenshotSource)(executor,callback)->{if(captures.incrementAndGet()==1)heldCapture=callback;else originalScreenshot.take(executor,callback);});
        base.start();base.h.waitFor(()->base.h.svc.logText().contains("OCR 캡처 응답 지연"),12000,"제어한 8초 캡처 지연이 발생하지 않았습니다");assertEquals(0,base.backs.get());assertFreshConfirmationBudget();
    }
}
