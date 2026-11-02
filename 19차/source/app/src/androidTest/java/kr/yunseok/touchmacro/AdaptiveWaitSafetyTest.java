package kr.yunseok.touchmacro;

import android.content.Intent;
import android.os.SystemClock;
import android.os.Bundle;
import android.provider.Settings;
import com.google.mlkit.vision.text.TextRecognizer;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** 선행 대기의 시간·대상·정지 경계와 실제 OCR 재확인을 함께 검사합니다. */
@RunWith(AndroidJUnit4.class) public class AdaptiveWaitSafetyTest {
    final AdaptiveWaitFlowTest f=new AdaptiveWaitFlowTest();
    @Before public void boot()throws Exception{f.boot();}
    @After public void end()throws Exception{f.end();}
    @Test public void missingNextTextStartsItsFiniteTimeoutAfterTheLeadingDelay()throws Exception {
        f.nextAppearsAfterFirst();f.h.main(()->f.h.entry.setOnClickListener(v->{f.firstAt.set(SystemClock.elapsedRealtime());f.firstTaps.incrementAndGet();f.h.entry.setText("완료됨");}));
        Step next=f.text("나중 글자",0);next.timeout=350;
        f.start(f.macro(true,f.text("첫 번째",900),next));f.h.waitFor(()->f.firstTaps.get()==1,4000,"첫 터치가 없습니다");
        f.h.waitFor(()->!f.h.svc.running,4000,"유한 대기 실패가 끝나지 않았습니다");
        long elapsed=SystemClock.elapsedRealtime()-f.firstAt.get();assertTrue("선행after가 다음timeout을 소모했습니다: "+elapsed+"\n"+f.evidence(),elapsed>=1200);
        assertTrue(f.evidence(),f.h.svc.logText().contains("글자를 찾지 못했습니다"));assertEquals(f.evidence(),0,f.secondTaps.get());
    }
    @Test public void pauseDiscardsTransitionEvidenceAndPreservesRemainingActiveDelay()throws Exception {
        f.h.main(()->f.h.entry.setOnClickListener(v->{f.firstAt.set(SystemClock.elapsedRealtime());f.firstTaps.incrementAndGet();f.h.entry.setText("완료됨");}));
        f.sourceField.set(f.h.svc,(MacroService.ScreenSource)token->{f.reads.incrementAndGet();return f.originalSource.read(token);});
        f.start(f.macro(true,f.text("첫 번째",1400),f.text("다음 화면",0)));
        f.h.waitFor(()->f.reads.get()>=3&&f.firstTaps.get()==1,5000,"선행 조건 대기가 시작되지 않았습니다");
        f.h.main(()->{f.h.svc.togglePause();f.second.setText("다음 화면");});SystemClock.sleep(1000);
        assertEquals(f.evidence(),0,f.secondTaps.get());assertTrue(f.evidence(),f.h.svc.paused);
        long resumed=SystemClock.elapsedRealtime();f.h.main(()->f.h.svc.togglePause());
        f.h.waitFor(()->!f.h.svc.running,6000,"일시정지 뒤 새 인식이 완료되지 않았습니다");
        assertEquals(f.evidence(),1,f.secondTaps.get());assertTrue("일시정지 중에 선행after 예산이 소모됐습니다\n"+f.evidence(),f.secondAt.get()-resumed>=1000);
    }
    @Test public void targetChangeAfterReadingTheNextConditionNeverCompletesItsTouch()throws Exception {
        f.nextAppearsAfterFirst();AtomicBoolean changed=new AtomicBoolean();AtomicLong requestedAt=new AtomicLong(),activeAt=new AtomicLong(),returnedAt=new AtomicLong(),transitionUntil=new AtomicLong();
        Field queryStarted=MacroService.class.getDeclaredField("queryStartedAt");queryStarted.setAccessible(true);
        f.sourceField.set(f.h.svc,(MacroService.ScreenSource)token->{ArrayList<MacroService.Hit> actual=f.originalSource.read(token);
            if(f.reads.incrementAndGet()==3){
                requestedAt.set(SystemClock.elapsedRealtime());transitionUntil.set(requestedAt.get()+4000);changed.set(true);f.h.main(()->f.h.a.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
                // 같은 읽기의 3500ms 상한 안에서 실제 대상 변경을 확정한 뒤 이전 결과를 반환합니다.
                long readUntil=Math.min(transitionUntil.get(),queryStarted.getLong(f.h.svc)+3400);
                f.h.waitFor(()->"com.android.settings".equals(f.h.svc.activePackage()),Math.max(0,readUntil-SystemClock.elapsedRealtime()),"이전 snapshot 반환 전 Settings가 실제 활성화되지 않았습니다");activeAt.set(SystemClock.elapsedRealtime());returnedAt.set(SystemClock.elapsedRealtime());
            }
            return actual;});
        Step next=f.text("다음 화면",0);next.waitForever=true;f.start(f.macro(true,f.text("첫 번째",1400),next));
        f.h.waitFor(changed::get,5000,"실제 다음 글자 조회 후 대상 전환이 없습니다");
        f.h.waitFor(()->"com.android.settings".equals(f.h.svc.activePackage()),Math.max(0,transitionUntil.get()-SystemClock.elapsedRealtime()),"소유 Settings 전환이 없습니다");SystemClock.sleep(800);
        Bundle diagnostic=new Bundle();diagnostic.putString("stream","PXE16_TARGET_DIAGNOSTIC case=confirmed_original_transition, requested_at="+requestedAt.get()+", active_observed_at="+activeAt.get()+", returned_at="+returnedAt.get()+", completed="+f.h.svc.logText().contains("2번 글자 찾아 터치 완료")+"\n");f.h.in.sendStatus(0,diagnostic);
        assertEquals(f.evidence(),0,f.secondTaps.get());assertTrue(f.evidence(),f.h.svc.running);
        assertFalse("다른 앱에서 이전 좌표 동작이 완료됐습니다\n"+f.evidence(),f.h.svc.logText().contains("2번 글자 찾아 터치 완료"));
        f.h.main(()->f.h.svc.stop());f.h.waitFor(()->!f.h.svc.running,1000,"대상 전환 대기 중 정지가 응답하지 않습니다");
    }
    @Test public void adaptiveOcrUsesExactlyTheOriginalTwoFreshFramesPerTouch()throws Exception {
        f.nextAppearsAfterFirst();AtomicInteger captures=new AtomicInteger(),tasks=new AtomicInteger();
        Field captureField=MacroService.class.getDeclaredField("screenshotSource");captureField.setAccessible(true);
        Field recognizerField=MacroService.class.getDeclaredField("recognizer");recognizerField.setAccessible(true);
        MacroService.ScreenshotSource capture=(MacroService.ScreenshotSource)captureField.get(f.h.svc);
        TextRecognizer recognizer=(TextRecognizer)recognizerField.get(f.h.svc);
        try{
            captureField.set(f.h.svc,(MacroService.ScreenshotSource)(executor,callback)->{captures.incrementAndGet();capture.take(executor,callback);});
            recognizerField.set(f.h.svc,Proxy.newProxyInstance(TextRecognizer.class.getClassLoader(),new Class<?>[]{TextRecognizer.class},(proxy,method,args)->{if(method.getName().equals("process"))tasks.incrementAndGet();return method.invoke(recognizer,args);}));
            Step first=f.text("첫 번째",2400),next=f.text("다음 화면",0);first.source="OCR";next.source="OCR";
            f.start(f.macro(true,first,next));f.h.waitFor(()->!f.h.svc.running,22000,"실제 OCR 전환이 끝나지 않았습니다");
            assertEquals(f.evidence(),1,f.firstTaps.get());assertEquals(f.evidence(),1,f.secondTaps.get());
            assertEquals("추가 probe 또는 재확인 누락\n"+f.evidence(),4,captures.get());assertEquals(f.evidence(),4,tasks.get());
            assertTrue(f.h.svc.performanceText(),f.h.svc.performanceText().contains("OCR 4회"));
        }finally{captureField.set(f.h.svc,capture);recognizerField.set(f.h.svc,recognizer);}
    }
}
