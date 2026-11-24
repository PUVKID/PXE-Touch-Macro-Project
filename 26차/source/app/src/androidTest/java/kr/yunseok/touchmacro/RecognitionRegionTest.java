package kr.yunseok.touchmacro;

import android.graphics.Rect;
import android.widget.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognizer;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** 전체 OCR의 경계 중심·동일 글자 순번·우선순위 캐시와 원래 터치 좌표를 보존합니다. */
@RunWith(AndroidJUnit4.class) public class RecognitionRegionTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    final AtomicInteger captures=new AtomicInteger(),recognitions=new AtomicInteger(),taps=new AtomicInteger();
    final AtomicInteger inputWidth=new AtomicInteger(),inputHeight=new AtomicInteger();
    final AtomicReference<Text> firstText=new AtomicReference<>();
    Field screenshotField,recognizerField;
    MacroService.ScreenshotSource originalScreenshot;
    TextRecognizer originalRecognizer;

    @Before public void boot()throws Exception{
        h.boot();h.fixture();
        screenshotField=field("screenshotSource");originalScreenshot=(MacroService.ScreenshotSource)screenshotField.get(h.svc);
        recognizerField=field("recognizer");originalRecognizer=(TextRecognizer)recognizerField.get(h.svc);
        h.main(()->h.entry.setOnClickListener(v->taps.incrementAndGet()));
        h.waitFor(()->h.a.getPackageName().equals(h.svc.activePackage()),4000,"검사 대상 앱 확인 실패");
        screenshotField.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{captures.incrementAndGet();originalScreenshot.take(executor,callback);});
        recognizerField.set(h.svc,Proxy.newProxyInstance(TextRecognizer.class.getClassLoader(),new Class<?>[]{TextRecognizer.class},(proxy,method,args)->{
            if(method.getName().equals("process")){InputImage image=(InputImage)args[0];recognitions.incrementAndGet();inputWidth.set(image.getWidth());inputHeight.set(image.getHeight());}
            Object result=method.invoke(originalRecognizer,args);
            if(method.getName().equals("process")){
                @SuppressWarnings("unchecked") com.google.android.gms.tasks.Task<Text> task=(com.google.android.gms.tasks.Task<Text>)result;
                task.addOnSuccessListener(text->firstText.compareAndSet(null,text));
            }
            return result;
        }));
    }
    @After public void end()throws Exception{
        h.end();
        if(screenshotField!=null)screenshotField.set(h.svc,originalScreenshot);
        if(recognizerField!=null)recognizerField.set(h.svc,originalRecognizer);
    }
    Field field(String name)throws Exception{Field value=MacroService.class.getDeclaredField(name);value.setAccessible(true);return value;}
    Step text(){Step value=new Step("TEXT_TAP");value.text="안내 대상";value.match="EXACT";value.source="OCR";value.timeout=0;value.immediate=true;value.after=0;return value;}
    Macro macro(Step step){Macro value=new Macro();value.name="실제 OCR 영역 검사";value.startDelay=0;value.maxMinutes=0;value.steps.add(step);return value;}
    Rect bounds(){Rect[] result={null};h.main(()->result[0]=h.bounds(h.entry));return result[0];}
    void region(Step step,Rect rectangle){int[] size=h.svc.size();step.left=rectangle.left*100.0/size[0];step.top=rectangle.top*100.0/size[1];step.right=rectangle.right*100.0/size[0];step.bottom=rectangle.bottom*100.0/size[1];}
    String normalized(String value){return value.replaceAll("\\s+","").toLowerCase(java.util.Locale.ROOT);}
    String evidence(){return "입력="+inputWidth.get()+"×"+inputHeight.get()+", 캡처="+captures.get()+", OCR="+recognitions.get()+", 터치="+taps.get()+"\n"+h.svc.progressText()+"\n"+h.svc.logText();}

    @Test public void aBoundaryCrossingWordWhoseCenterIsInsideStillTouchesTheOriginalButton()throws Exception{
        h.main(()->h.svc.start(macro(text())));h.waitFor(()->!h.svc.running,12000,"기준 화면의 실제 OCR 터치가 완료되지 않음");
        assertEquals(evidence(),1,taps.get());h.waitFor(()->firstText.get()!=null,2000,"기준 실제 OCR 결과가 없습니다");
        Rect nativeButton=bounds();Text actualText=firstText.get();
        Method detector=MacroService.class.getDeclaredMethod("ocrHit",Step.class,Text.class);detector.setAccessible(true);
        MacroService.Hit fullHit=(MacroService.Hit)detector.invoke(h.svc,text(),actualText);
        assertNotNull("같은 실제 OCR 결과의 전체 영역에서 글자가 없습니다",fullHit);assertEquals("안내대상",normalized(fullHit.text));
        Rect word=new Rect(fullHit.rect);assertTrue(word.toString(),word.width()>12&&word.height()>8);
        // 같은 실제 OCR 결과로 중심 필터를 비교해 프레임 사이의 인식 흔들림과 분리합니다.
        Rect center=new Rect(word.centerX()-3,word.centerY()-3,word.centerX()+3,word.centerY()+3);
        assertTrue(word.toString(),word.left<center.left&&word.right>center.right&&word.top<center.top&&word.bottom>center.bottom);
        Step bounded=text();region(bounded,center);MacroService.Hit boundaryHit=(MacroService.Hit)detector.invoke(h.svc,bounded,actualText);
        assertNotNull("글자 중심을 포함한 6px 영역에서 기존 일치가 사라졌습니다",boundaryHit);assertEquals(word,boundaryHit.rect);assertEquals(fullHit.rect,boundaryHit.rect);
        assertEquals(normalized(fullHit.text),normalized(boundaryHit.text));
        assertEquals(word.exactCenterX(),boundaryHit.rect.exactCenterX(),0);assertEquals(word.exactCenterY(),boundaryHit.rect.exactCenterY(),0);
        float centerX=boundaryHit.rect.exactCenterX(),centerY=boundaryHit.rect.exactCenterY();
        assertTrue(center.toString(),centerX>=center.left&&centerX<=center.right&&centerY>=center.top&&centerY<=center.bottom);
        assertTrue(nativeButton.toString(),centerX>=nativeButton.left&&centerX<nativeButton.right&&centerY>=nativeButton.top&&centerY<nativeButton.bottom);
        // 글자와 겹치더라도 중심을 제외한 영역은 기존 중심 필터에서 거절해야 합니다.
        Rect overlap=new Rect(word.left-3,word.top,word.left+3,word.top+6);assertTrue(Rect.intersects(word,overlap));
        assertFalse(overlap.toString(),centerX>=overlap.left&&centerX<=overlap.right&&centerY>=overlap.top&&centerY<=overlap.bottom);
        Step excluded=text();region(excluded,overlap);assertNull("중심이 밖인 글자를 단순 교집합으로 허용했습니다",detector.invoke(h.svc,excluded,actualText));
        int[] size=h.svc.size();Step tap=new Step("TAP");tap.x=boundaryHit.rect.exactCenterX()*100.0/size[0];tap.y=boundaryHit.rect.exactCenterY()*100.0/size[1];tap.duration=100;tap.after=0;
        h.main(()->h.svc.start(macro(tap)));h.waitFor(()->!h.svc.running,12000,"판정된 원래 좌표의 실제 터치가 완료되지 않음");
        assertEquals("경계에 걸친 글자의 원래 화면 좌표가 보존되지 않았습니다\n"+evidence(),2,taps.get());
        assertEquals(evidence(),1,captures.get());assertEquals(evidence(),1,recognitions.get());
        assertEquals(evidence(),size[0],inputWidth.get());assertEquals(evidence(),size[1],inputHeight.get());
    }
    @Test public void priorityScanSharesOneFullImageAndFindsTheSecondAreaOnce()throws Exception{
        Rect[] first={null};h.main(()->{LinearLayout parent=(LinearLayout)h.entry.getParent();Button other=new Button(h.a);other.setText("다른 항목");parent.addView(other,0,new LinearLayout.LayoutParams(-1,Ui.dp(h.a,70)));other.post(()->first[0]=h.bounds(other));});
        h.in.waitForIdleSync();h.waitFor(()->first[0]!=null,2000,"첫 번째 영역 배치 실패");Rect second=bounds();
        Step wait=new Step("WAIT");wait.duration=5000;Macro macro=macro(wait);
        WatchConfig value=new WatchConfig();value.rule.text="안내 대상";value.rule.match="EXACT";value.rule.source="OCR";value.rule.immediate=true;
        int[] size=h.svc.size();value.priorityAreas.add(new WatchArea(first[0].left*100.0/size[0],first[0].top*100.0/size[1],first[0].right*100.0/size[0],first[0].bottom*100.0/size[1]));
        value.priorityAreas.add(new WatchArea(second.left*100.0/size[0],second.top*100.0/size[1],second.right*100.0/size[0],second.bottom*100.0/size[1]));
        value.targetPackage=h.a.getPackageName();value.macroName=macro.name;value.startMs=0;value.repetitions=1;value.entryMs=300;value.returnMs=300;value.settleMs=300;
        h.main(()->{Store.put(h.a,macro);h.svc.startWatch(value);});h.waitFor(()->taps.get()==1,12000,"두 번째 우선 영역의 실제 터치가 없습니다");
        assertEquals(evidence(),1,captures.get());assertEquals(evidence(),1,recognitions.get());assertTrue(evidence(),h.svc.logText().contains("2순위에서 발견"));
        assertEquals(evidence(),size[0],inputWidth.get());assertEquals(evidence(),size[1],inputHeight.get());
    }
    @Test public void occurrenceTwoTouchesOnlyTheSecondIdenticalWord()throws Exception{
        AtomicInteger firstTaps=new AtomicInteger();h.main(()->{LinearLayout parent=(LinearLayout)h.entry.getParent();Button first=new Button(h.a);first.setText("안내 대상");first.setOnClickListener(v->firstTaps.incrementAndGet());parent.addView(first,0,new LinearLayout.LayoutParams(-1,Ui.dp(h.a,70)));});h.in.waitForIdleSync();
        Step step=text();step.occurrence=2;h.main(()->h.svc.start(macro(step)));h.waitFor(()->!h.svc.running,12000,"두 번째 동일 글자의 실제 터치가 완료되지 않음");
        assertEquals(evidence(),0,firstTaps.get());assertEquals(evidence(),1,taps.get());assertEquals(evidence(),1,captures.get());assertEquals(evidence(),1,recognitions.get());
    }
}
