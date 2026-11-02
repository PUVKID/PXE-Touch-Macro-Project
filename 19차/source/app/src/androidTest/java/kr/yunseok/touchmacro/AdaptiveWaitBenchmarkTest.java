package kr.yunseok.touchmacro;

import android.graphics.Color;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognizer;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** 동일한 실제 OCR·버튼 전환으로 15회 전체 실행과 예정 위치의 터치를 측정합니다. */
@RunWith(AndroidJUnit4.class) public class AdaptiveWaitBenchmarkTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    final String[] labels={"첫 단계","다음 단계"};
    final Button[] buttons=new Button[2];
    final AtomicInteger expected=new AtomicInteger(),taps=new AtomicInteger(),wrong=new AtomicInteger(),downs=new AtomicInteger();
    final AtomicInteger processCalls=new AtomicInteger(),inputWidth=new AtomicInteger(),inputHeight=new AtomicInteger(),inputSizeChanges=new AtomicInteger();
    Field recognizerField;TextRecognizer originalRecognizer;
    @Before public void boot()throws Exception {
        h.boot();h.fixture();h.main(()->{
            LinearLayout body=(LinearLayout)h.entry.getParent();body.removeAllViews();body.setBackgroundColor(Color.WHITE);
            body.setOnTouchListener((v,event)->{if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downs.incrementAndGet();wrong.incrementAndGet();}return true;});
            for(int i=0;i<2;i++){
                final int index=i;Button button=new Button(h.a);buttons[i]=button;button.setText(i==0?labels[0]:"대기 중");
                button.setTypeface(h.a.getResources().getFont(R.font.nanum_gothic_bold));button.setTextSize(24);
                body.addView(button,new LinearLayout.LayoutParams(-1,Ui.dp(h.a,88)));
                button.setOnTouchListener((v,event)->{
                    if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downs.incrementAndGet();Rect rect=h.bounds(button);
                        if(expected.get()!=index||!rect.contains((int)event.getRawX(),(int)event.getRawY()))wrong.incrementAndGet();}
                    return false;
                });
                button.setOnClickListener(v->{if(expected.get()!=index){wrong.incrementAndGet();return;}taps.incrementAndGet();button.setText("완료됨");int next=(index+1)%2;expected.set(next);buttons[next].setText(labels[next]);});
            }
        });h.in.waitForIdleSync();h.waitFor(()->h.a.getPackageName().equals(h.svc.activePackage()),4000,"OCR 속도 검사 대상 앱 확인 실패");
        recognizerField=MacroService.class.getDeclaredField("recognizer");recognizerField.setAccessible(true);originalRecognizer=(TextRecognizer)recognizerField.get(h.svc);
        // 원본 인식 Task를 그대로 반환하며 실제 입력 이미지의 숫자 치수만 관찰합니다.
        recognizerField.set(h.svc,Proxy.newProxyInstance(TextRecognizer.class.getClassLoader(),new Class<?>[]{TextRecognizer.class},(proxy,method,args)->{
            if(method.getName().equals("process")&&args!=null&&args.length>0&&args[0] instanceof InputImage){InputImage input=(InputImage)args[0];if(processCalls.getAndIncrement()==0){inputWidth.set(input.getWidth());inputHeight.set(input.getHeight());}else if(inputWidth.get()!=input.getWidth()||inputHeight.get()!=input.getHeight())inputSizeChanges.incrementAndGet();}
            try{return method.invoke(originalRecognizer,args);}catch(InvocationTargetException error){throw error.getCause();}
        }));
    }
    @After public void end()throws Exception{try{h.end();}finally{if(recognizerField!=null&&originalRecognizer!=null)recognizerField.set(h.svc,originalRecognizer);}}
    void report(String value){android.os.Bundle data=new android.os.Bundle();data.putString("stream",value+"\n");h.in.sendStatus(0,data);}
    String glyphMetrics(){
        int minWidth=Integer.MAX_VALUE,minHeight=Integer.MAX_VALUE,count=0;StringBuilder sizes=new StringBuilder();Rect ink=new Rect();
        for(int row=0;row<labels.length;row++){if(row>0)sizes.append(';');sizes.append("row").append(row).append("_text_px=").append(buttons[row].getTextSize()).append("_ink=");
            for(int index=0;index<labels[row].length();index++){if(Character.isWhitespace(labels[row].charAt(index)))continue;buttons[row].getPaint().getTextBounds(labels[row],index,index+1,ink);if(count>0)sizes.append('/');sizes.append(ink.width()).append('x').append(ink.height());minWidth=Math.min(minWidth,ink.width());minHeight=Math.min(minHeight,ink.height());count++;}}
        return "font_sp=24, font_scale="+h.a.getResources().getConfiguration().fontScale+", density_dpi="+h.a.getResources().getDisplayMetrics().densityDpi+", glyph_count="+count+", min_ink="+minWidth+"x"+minHeight+", "+sizes;
    }
    void measure(boolean adaptive)throws Exception {
        Macro value=new Macro();value.name="실제 OCR 15회 비교";value.startDelay=0;value.gap=0;value.rounds=15;value.maxMinutes=0;
        for(String label:labels){Step step=new Step("TEXT_TAP");step.text=label;step.match="EXACT";step.source="OCR";step.timeout=12000;step.after=2000;value.steps.add(step);}
        org.json.JSONObject stored=value.json();stored.put("adaptiveWait",adaptive);Macro run=new Macro(stored);
        final String[] glyph={""};h.main(()->glyph[0]=glyphMetrics());int[] screen=h.svc.size();report("PXE16_BENCHMARK_GEOMETRY screen="+screen[0]+"x"+screen[1]+", "+glyph[0]);
        long before=SystemClock.elapsedRealtime();h.main(()->h.svc.start(run));
        AssertionError waitingFailure=null;
        try{h.waitFor(()->!h.svc.running,adaptive?125000:210000,"실제 OCR 15회 실행이 제한 안에 끝나지 않았습니다");}
        catch(AssertionError failure){waitingFailure=failure;}
        long elapsed=SystemClock.elapsedRealtime()-before;String metrics;
        try{metrics=(String)MacroService.class.getMethod("performanceText").invoke(h.svc);}catch(NoSuchMethodException absent){metrics="이전 APK 측정 API 없음";}
        String result="adaptive="+adaptive+", rounds="+h.svc.watchCompleted+", elapsed_ms="+elapsed+", expected_taps=30, taps="+taps.get()+", downs="+downs.get()+", wrong="+wrong.get()+", "+metrics;
        System.out.println("PXE16_BENCHMARK "+result);
        report("PXE16_BENCHMARK "+result+"\nPXE16_BENCHMARK_INPUT process_calls="+processCalls.get()+", actual_input="+inputWidth.get()+"x"+inputHeight.get()+", size_changes="+inputSizeChanges.get());
        if(waitingFailure!=null)throw waitingFailure;
        assertEquals(result,15,h.svc.watchCompleted);assertEquals(result,30,taps.get());assertEquals(result,30,downs.get());assertEquals(result,0,wrong.get());
        if(adaptive)assertTrue("15회 120초 목표를 초과했습니다: "+result,elapsed<=120000);
    }
    @Test public void fifteenRoundsBaselineWithExplicitDelay()throws Exception{measure(false);}
    @Test public void fifteenRoundsAdaptiveCompletesWithinTwoMinutes()throws Exception{measure(true);}
}
