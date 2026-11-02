package kr.yunseok.touchmacro;

import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognizer;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** 실제 첫 OCR 성공 뒤 재확인 화면에서만 글자를 없애 대기와 실패 정책을 구분합니다. */
@RunWith(AndroidJUnit4.class) public class TextConfirmationTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    final AtomicInteger captures=new AtomicInteger(),recognitions=new AtomicInteger(),taps=new AtomicInteger();
    final AtomicReference<String> confirmationText=new AtomicReference<>();
    Field screenshotField,recognizerField;
    MacroService.ScreenshotSource originalScreenshot;
    TextRecognizer originalRecognizer;

    @Before public void boot()throws Exception{
        h.boot();h.fixture();
        screenshotField=field("screenshotSource");originalScreenshot=(MacroService.ScreenshotSource)screenshotField.get(h.svc);
        recognizerField=field("recognizer");originalRecognizer=(TextRecognizer)recognizerField.get(h.svc);
        h.main(()->h.entry.setOnClickListener(v->taps.incrementAndGet()));
        h.waitFor(()->h.a.getPackageName().equals(h.svc.activePackage()),4000,"검사 앱 확인 실패");
        // 서비스가 두 번째 캡처를 요청한 뒤 실제 뷰를 바꾸고 그려진 새 화면을 캡처합니다.
        screenshotField.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{
            if(captures.incrementAndGet()==2){h.entry.setText("잠시 없음");h.entry.postDelayed(()->originalScreenshot.take(executor,callback),200);}
            else originalScreenshot.take(executor,callback);
        });
        recognizerField.set(h.svc,Proxy.newProxyInstance(TextRecognizer.class.getClassLoader(),new Class<?>[]{TextRecognizer.class},(proxy,method,args)->{
            if(!method.getName().equals("process"))return method.invoke(originalRecognizer,args);
            @SuppressWarnings("unchecked") Task<Text> actual=(Task<Text>)method.invoke(originalRecognizer,args);
            if(recognitions.incrementAndGet()==2)actual.addOnSuccessListener(result->confirmationText.set(result.getText()));
            return actual;
        }));
    }
    @After public void end()throws Exception{
        h.end();
        if(screenshotField!=null&&originalScreenshot!=null)screenshotField.set(h.svc,originalScreenshot);
        if(recognizerField!=null&&originalRecognizer!=null)recognizerField.set(h.svc,originalRecognizer);
    }
    Field field(String name)throws Exception{Field f=MacroService.class.getDeclaredField(name);f.setAccessible(true);return f;}
    Step text(){Step s=new Step("TEXT_TAP");s.text="안내 대상";s.match="EXACT";s.source="OCR";s.timeout=6000;s.waitForever=true;s.immediate=false;s.after=0;return s;}
    void start(Step step){Macro m=new Macro();m.name="글자 재확인 대기 검사";m.startDelay=0;m.maxMinutes=0;m.steps.add(step);h.main(()->h.svc.start(m));}
    String evidence(){return "캡처="+captures.get()+", OCR="+recognitions.get()+", 터치="+taps.get()+", 실행="+h.svc.running+"\n재확인="+confirmationText.get()+"\n"+h.svc.progressText()+"\n"+h.svc.logText();}
    void confirmActualMissingFrame(){
        h.waitFor(()->confirmationText.get()!=null,18000,"두 번째 실제 OCR 결과가 없습니다");
        assertFalse("실제 재확인 화면에 대상 글자가 남았습니다\n"+evidence(),confirmationText.get().replace(" ","").contains("안내대상"));
        h.waitFor(()->captures.get()>=3||!h.svc.running||h.svc.logText().contains("글자 없음 · 뒤로가기"),5000,"재확인 이후 상태가 관측되지 않았습니다");
    }
    @Test public void foreverWaitSurvivesMissingConfirmationThenTouchesReappearedText()throws Exception{
        start(text());confirmActualMissingFrame();
        assertTrue("계속 기다리기가 재확인 부재에서 종료됐습니다\n"+evidence(),h.svc.running);
        assertEquals(evidence(),0,taps.get());assertFalse(evidence(),h.svc.logText().contains("글자를 찾지 못했습니다"));
        h.main(()->h.entry.setText("안내 대상"));
        h.waitFor(()->!h.svc.running,15000,"재등장한 글자의 재확인과 터치가 완료되지 않았습니다");
        assertEquals(evidence(),1,taps.get());assertTrue(evidence(),h.svc.logText().contains("1번 글자 찾아 터치 완료"));
        assertTrue("새 화면 탐색과 재확인이 없습니다\n"+evidence(),captures.get()>=4);
    }
    @Test public void foreverWaitDoesNotBackRetryAndRealStopStillWorks()throws Exception{
        Step step=text();step.failure="BACK_RETRY";step.retries=2;step.retryDelay=300;start(step);confirmActualMissingFrame();
        assertFalse("계속 기다리기가 재확인 부재를 뒤로가기로 처리했습니다\n"+evidence(),h.svc.logText().contains("글자 없음 · 뒤로가기"));
        assertTrue(evidence(),h.svc.running);assertEquals(evidence(),0,taps.get());
        AccessibilityNodeInfo stop=h.find("■");assertNotNull("재확인 대기 중 실제 정지 버튼이 없습니다\n"+evidence(),stop);
        assertTrue(stop.isVisibleToUser());long before=SystemClock.elapsedRealtime();assertTrue(stop.performAction(AccessibilityNodeInfo.ACTION_CLICK));stop.recycle();
        h.waitFor(()->!h.svc.running,1000,"재확인 대기 중 정지가 응답하지 않습니다");
        assertTrue(SystemClock.elapsedRealtime()-before<1200);assertEquals(evidence(),0,taps.get());
    }
    @Test public void finiteWaitKeepsExistingStopPolicyWhenConfirmationIsMissing()throws Exception{
        ErrorJournal journal=new ErrorJournal(h.a.getFilesDir());String previousErrors=journal.read();
        Step step=text();step.waitForever=false;step.left=4.5;step.top=21.5;step.right=94.5;step.bottom=65.5;start(step);confirmActualMissingFrame();
        h.waitFor(()->!h.svc.running,2000,"유한 대기의 기존 정지 정책이 사라졌습니다");
        assertTrue(evidence(),h.svc.logText().contains("글자를 찾지 못했습니다"));
        assertEquals(evidence(),0,taps.get());assertEquals(evidence(),2,captures.get());assertEquals(evidence(),2,recognitions.get());
        String errors=journal.read();int marker=errors.lastIndexOf(" | TEXT_NOT_FOUND | ");assertTrue("글자 미발견 설정 진단이 없습니다\n"+errors,marker>=0);
        int lineStart=errors.lastIndexOf('\n',marker);int lineEnd=errors.indexOf('\n',marker);String diagnostic=errors.substring(lineStart+1,lineEnd<0?errors.length():lineEnd);
        assertFalse("이번 실패의 새 설정 진단이 아닙니다\n"+diagnostic,previousErrors.contains(diagnostic));
        assertTrue(diagnostic,diagnostic.contains("source=OCR, match=EXACT, 영역=4.5,21.5,94.5,65.5, waitForever=false, immediate=false, timeout_ms=6000"));
        assertFalse("오류 파일에 검색 원문을 추가 저장했습니다\n"+diagnostic,diagnostic.contains("안내 대상"));
    }
    @Test public void immediateTextTapUsesOneImageAndCompletesExactlyOneTouch()throws Exception{
        Step step=text();step.immediate=true;start(step);
        h.waitFor(()->!h.svc.running,18000,"즉시 진행 터치가 완료되지 않았습니다");SystemClock.sleep(500);
        assertEquals(evidence(),1,taps.get());assertEquals(evidence(),1,captures.get());assertEquals(evidence(),1,recognitions.get());
        assertNull("즉시 진행에 불필요한 두 번째 OCR이 실행됐습니다",confirmationText.get());
        assertTrue(evidence(),h.svc.logText().contains("1번 글자 찾아 터치 완료"));
    }
}
