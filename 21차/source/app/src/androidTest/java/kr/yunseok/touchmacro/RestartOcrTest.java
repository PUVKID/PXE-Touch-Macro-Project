package kr.yunseok.touchmacro;

import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 실제 OCR의 늦은 완료와 새 글자 실행의 수명·측정 분리를 확인합니다. */
@RunWith(AndroidJUnit4.class) public class RestartOcrTest {
    final OcrStallRecoveryTest base=new OcrStallRecoveryTest();
    @Before public void boot()throws Exception{base.boot();}
    @After public void end()throws Exception{base.end();}
    @Test public void startWithNewWordWhileOldOcrPendingDiscardsOldHitWithoutParallelRecognition()throws Exception{
        base.countCaptures();base.delayFirstRecognition();base.start();base.h.waitFor(()->base.firstText.get()!=null,18000,"첫 실제 OCR이 준비되지 않았습니다");
        Macro next=new Macro();next.name="인식 지연 복구 검사";next.startDelay=0;next.maxMinutes=0;Step word=base.text();word.text="새 글자";next.steps.add(word);
        base.h.main(()->{base.h.entry.setText("새 글자");base.h.svc.start(next);});SystemClock.sleep(400);
        assertTrue(base.h.svc.running);assertEquals(1,base.recognitions.get());assertEquals(1,base.captures.get());assertEquals(0,base.taps.get());
        Object pending=((AtomicReference<?>)base.field("pendingRecognition").get(base.h.svc)).get();assertNotNull(pending);
        java.lang.reflect.Field bitmap=pending.getClass().getDeclaredField("bitmap");bitmap.setAccessible(true);assertFalse(((android.graphics.Bitmap)bitmap.get(pending)).isRecycled());
        base.h.main(()->base.delayedText.trySetResult(base.firstText.get()));base.h.waitFor(()->!base.h.svc.running,18000,"늦은 이전 OCR 뒤 새 글자가 실행되지 않았습니다");
        assertEquals(2,base.recognitions.get());assertEquals(2,base.captures.get());assertEquals(1,base.taps.get());assertEquals(1,base.h.svc.hudProgress().completed);
        assertTrue("이전 OCR 측정이 새 실행에 더해졌습니다",base.h.svc.performanceText().contains("OCR 1회"));
    }
}
