package kr.yunseok.touchmacro;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognizer;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** 한 실제 OCR 프레임의 100개 검색 조건과 별도 새 프레임의 실제 좌표 터치를 구분합니다. */
@RunWith(AndroidJUnit4.class) public class OcrAccuracyCorpusTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    final String[] labels={"확인 101","확인 102","구매 완료","구매 취소","다음 201","다음 202","저장 성공","저장 취소","진행 301","진행 302"};
    final Button[] buttons=new Button[10];
    final Rect[] regions=new Rect[10];
    final AtomicInteger expectedTap=new AtomicInteger(),taps=new AtomicInteger(),wrong=new AtomicInteger(),downs=new AtomicInteger();
    int preparationCaptures,minimumContrast;long preparationMs;
    @Before public void boot()throws Exception {
        h.boot();h.fixture();h.main(()->{
            h.svc.controller().close();LinearLayout body=(LinearLayout)h.entry.getParent();body.removeAllViews();body.setPadding(Ui.dp(h.a,28),Ui.dp(h.a,120),Ui.dp(h.a,28),0);body.setBackgroundColor(Color.WHITE);
            body.setOnTouchListener((v,event)->{if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downs.incrementAndGet();wrong.incrementAndGet();}return true;});
            for(int i=0;i<labels.length;i++){
                final int index=i;Button button=new Button(h.a);buttons[i]=button;button.setText(labels[i]);button.setTextColor(Color.BLACK);button.setTextSize(22);
                button.setTypeface(h.a.getResources().getFont(R.font.nanum_gothic_bold));body.addView(button,new LinearLayout.LayoutParams(-1,Ui.dp(h.a,56)));
                button.setOnTouchListener((v,event)->{if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downs.incrementAndGet();Rect box=h.bounds(button);if(expectedTap.get()!=index||!box.contains((int)event.getRawX(),(int)event.getRawY()))wrong.incrementAndGet();}return false;});
                button.setOnClickListener(v->{if(expectedTap.getAndIncrement()!=index)wrong.incrementAndGet();taps.incrementAndGet();});
            }
        });h.in.waitForIdleSync();h.main(()->{for(int i=0;i<regions.length;i++)regions[i]=h.bounds(buttons[i]);});
        h.waitFor(()->h.a.getPackageName().equals(h.svc.activePackage()),4000,"정확도 검사 앱 확인 실패");
    }
    @After public void end(){h.end();}
    Step rule(int row){
        Step step=new Step("TEXT_TAP");step.text=labels[row];step.source="OCR";step.match="EXACT";step.immediate=true;step.timeout=0;step.after=0;
        int[] size=h.svc.size();Rect bounds=regions[row];step.left=bounds.left*100.0/size[0];step.top=bounds.top*100.0/size[1];step.right=bounds.right*100.0/size[0];step.bottom=bounds.bottom*100.0/size[1];return step;
    }
    void report(String value){android.os.Bundle data=new android.os.Bundle();data.putString("stream",value+"\n");h.in.sendStatus(0,data);}
    Bitmap preparedFrame()throws Exception {
        long started=android.os.SystemClock.elapsedRealtime(),deadline=started+4000;CountDownLatch committed=new CountDownLatch(1);
        Runnable callback=committed::countDown;ViewTreeObserver[] observer={null};Bitmap pending=null;
        try{
            h.main(()->{View body=(View)buttons[0].getParent();assertTrue("시험 화면의 하드웨어 렌더링이 필요합니다",body.isHardwareAccelerated());observer[0]=body.getViewTreeObserver();observer[0].registerFrameCommitCallback(callback);body.invalidate();});
            assertTrue("시험 화면 프레임 제출이 4초 안에 완료되지 않았습니다",committed.await(Math.max(0,deadline-android.os.SystemClock.elapsedRealtime()),TimeUnit.MILLISECONDS));
            // 프레임 제출만으로 화면 표시를 가정하지 않고, 기존 버튼의 채움과 흰 여백을 대조합니다.
            while(android.os.SystemClock.elapsedRealtime()<deadline){
                pending=h.auto.takeScreenshot();preparationCaptures++;assertNotNull("실제 정확도 fixture 화면 캡처 실패",pending);
                boolean ready=true;int contrast=Integer.MAX_VALUE;int[] size=h.svc.size();
                if(pending.getWidth()!=size[0]||pending.getHeight()!=size[1])ready=false;
                for(Rect bounds:regions){
                    int inside=bounds.left+Ui.dp(h.a,12),margin=bounds.left-Ui.dp(h.a,12),y=bounds.centerY();
                    if(bounds.isEmpty()||margin<0||inside>=pending.getWidth()||inside>=bounds.right||y<0||y>=pending.getHeight()){ready=false;break;}
                    int fill=pending.getPixel(inside,y),white=pending.getPixel(margin,y);int difference=Math.abs(Color.red(fill)-Color.red(white))+Math.abs(Color.green(fill)-Color.green(white))+Math.abs(Color.blue(fill)-Color.blue(white));
                    contrast=Math.min(contrast,difference);if((white&0x00ffffff)!=0x00ffffff||difference<=48)ready=false;
                }
                if(ready&&android.os.SystemClock.elapsedRealtime()<=deadline){minimumContrast=contrast;preparationMs=android.os.SystemClock.elapsedRealtime()-started;Bitmap result=pending;pending=null;return result;}
                pending.recycle();pending=null;long remaining=deadline-android.os.SystemClock.elapsedRealtime();if(remaining>0)android.os.SystemClock.sleep(Math.min(16,remaining));
            }
            throw new AssertionError("10개 버튼의 실제 표시가 준비되지 않았습니다 · 준비 상한 4초 · 캡처 "+preparationCaptures);
        }finally{if(pending!=null)pending.recycle();h.main(()->{if(observer[0]!=null&&observer[0].isAlive())observer[0].unregisterFrameCommitCallback(callback);});}
    }
    @Test public void oneRealFrameMeetsOneHundredConditionsAndFreshOcrTouchesOnlyTheirRegions()throws Exception {
        int[] size=h.svc.size();Field width=MacroService.class.getDeclaredField("initialW"),height=MacroService.class.getDeclaredField("initialH");width.setAccessible(true);height.setAccessible(true);width.setInt(h.svc,size[0]);height.setInt(h.svc,size[1]);
        Field recognizerField=MacroService.class.getDeclaredField("recognizer");recognizerField.setAccessible(true);TextRecognizer recognizer=(TextRecognizer)recognizerField.get(h.svc);
        Method detector=MacroService.class.getDeclaredMethod("ocrHit",Step.class,Text.class);detector.setAccessible(true);
        Bitmap bitmap=preparedFrame();int inputWidth=bitmap.getWidth(),inputHeight=bitmap.getHeight();Text actual;
        try{actual=Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap,0)),20,TimeUnit.SECONDS);}finally{bitmap.recycle();}
        int correct=0,positive=0,negative=0;StringBuilder errors=new StringBuilder();
        for(int row=0;row<labels.length;row++)for(int variant=0;variant<10;variant++){
            Step query=rule(row);boolean present=variant<4||variant==9;
            switch(variant){
                case 1:query.text=labels[row].replace(" ","");break;
                case 2:query.text=labels[row].replace(" ","   ");break;
                case 3:query.match="CONTAINS";query.text=labels[row].substring(0,2);break;
                case 4:query.text="없는 문구 999";break;
                case 5:query.text=labels[row]+"0";break;
                case 6:query.text=labels[(row+1)%labels.length];break;
                case 7:query.left=0;query.right=1;break;
                case 8:query.occurrence=2;break;
                case 9:query.match="REGEX";query.text="^"+java.util.regex.Pattern.quote(labels[row].replace(" ",""))+"$";break;
            }
            MacroService.Hit hit=(MacroService.Hit)detector.invoke(h.svc,query,actual);
            boolean matched=present?hit!=null&&regions[row].contains(hit.rect.centerX(),hit.rect.centerY()):hit==null;
            if(matched)correct++;else errors.append("조건 ").append(row*10+variant).append(" · 기대 ").append(present).append(" · 실제 ").append(hit!=null).append('\n');
            if(present)positive++;else negative++;
        }
        report("PXE16_CORPUS frames=1, queries=100, positive="+positive+", negative="+negative+", correct="+correct+", font_sp=22, fixed_regions=10, independent_frames=false, input="+inputWidth+"x"+inputHeight+", preparation_captures="+preparationCaptures+", preparation_ms="+preparationMs+", pixel_pairs=10, contrast_threshold=48, minimum_contrast="+minimumContrast+", preparation_ocr=0");
        assertTrue("통제된 한 프레임의 100조건에서 99개 미만 일치: "+correct+"\n"+errors,correct>=99);
        Macro macro=new Macro();macro.name="원래 좌표와 영역밖 무터치 검사";macro.startDelay=0;macro.gap=0;macro.maxMinutes=0;
        for(int row=0;row<labels.length;row++)macro.steps.add(rule(row));
        Step outside=rule(0);outside.left=0;outside.right=1;outside.failure="SKIP";macro.steps.add(outside);
        h.main(()->h.svc.start(macro));h.waitFor(()->!h.svc.running,60000,"실제 새 OCR 좌표 터치가 완료되지 않았습니다");
        report("PXE16_CORPUS_TOUCH expected=10, taps="+taps.get()+", downs="+downs.get()+", wrong="+wrong.get()+", negative_steps=1, "+h.svc.performanceText());
        assertEquals(h.svc.logText(),10,taps.get());assertEquals(h.svc.logText(),10,downs.get());assertEquals(h.svc.logText(),0,wrong.get());
        assertEquals(h.svc.logText(),1,h.svc.watchCompleted);
    }
}
