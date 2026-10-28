package kr.yunseok.touchmacro;

import android.graphics.Color;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** 새 옵션은 공통 JSON으로 전달하여 이전 APK에서도 같은 실제 화면으로 비교합니다. */
@RunWith(AndroidJUnit4.class) public class AdaptiveWaitFlowTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    final AtomicInteger firstTaps=new AtomicInteger(),secondTaps=new AtomicInteger(),reads=new AtomicInteger();
    final AtomicLong firstAt=new AtomicLong(),secondAt=new AtomicLong();
    Button second;
    Field sourceField;
    MacroService.ScreenSource originalSource;

    @Before public void boot()throws Exception {
        h.boot();h.fixture();
        sourceField=MacroService.class.getDeclaredField("accessibilitySource");sourceField.setAccessible(true);
        originalSource=(MacroService.ScreenSource)sourceField.get(h.svc);
        h.main(()->{
            LinearLayout body=(LinearLayout)h.entry.getParent();body.setBackgroundColor(Color.WHITE);
            h.entry.setText("첫 번째");second=new Button(h.a);second.setText("대기 중");
            body.addView(second,new LinearLayout.LayoutParams(-1,Ui.dp(h.a,70)));
            h.entry.setOnClickListener(v->{firstAt.set(SystemClock.elapsedRealtime());firstTaps.incrementAndGet();});
            second.setOnClickListener(v->{secondAt.set(SystemClock.elapsedRealtime());secondTaps.incrementAndGet();});
        });
        h.in.waitForIdleSync();h.waitFor(()->h.a.getPackageName().equals(h.svc.activePackage()),4000,"검사 대상 앱 확인 실패");
    }
    @After public void end()throws Exception {
        h.end();if(sourceField!=null&&originalSource!=null)sourceField.set(h.svc,originalSource);
    }
    Step text(String value,int after) {
        Step step=new Step("TEXT_TAP");step.text=value;step.match="EXACT";step.source="ACCESS";
        step.timeout=4000;step.after=after;return step;
    }
    Macro macro(boolean adaptive,Step... steps)throws Exception {
        Macro value=new Macro();value.name="적응 대기 실제 전환 검사";value.startDelay=0;value.gap=0;value.maxMinutes=0;
        for(Step step:steps)value.steps.add(step);
        org.json.JSONObject stored=value.json();stored.put("adaptiveWait",adaptive);return new Macro(stored);
    }
    void start(Macro value){h.main(()->h.svc.start(value));}
    String evidence(){return "첫터치="+firstTaps.get()+", 다음터치="+secondTaps.get()+", 조회="+reads.get()+", 간격="+(secondAt.get()-firstAt.get())+"\n"+h.svc.progressText()+"\n"+h.svc.logText();}
    void nextAppearsAfterFirst() {
        h.main(()->h.entry.setOnClickListener(v->{firstAt.set(SystemClock.elapsedRealtime());firstTaps.incrementAndGet();h.entry.setText("완료됨");second.setText("다음 화면");}));
    }
    @Test public void newlyAppearedNextTextCutsAfterAndStillRequiresFreshConfirmation()throws Exception {
        nextAppearsAfterFirst();AtomicInteger readsAtNext=new AtomicInteger();
        sourceField.set(h.svc,(MacroService.ScreenSource)token->{reads.incrementAndGet();return originalSource.read(token);});
        h.main(()->second.setOnClickListener(v->{readsAtNext.set(reads.get());secondAt.set(SystemClock.elapsedRealtime());secondTaps.incrementAndGet();}));
        start(macro(true,text("첫 번째",2400),text("다음 화면",0)));
        h.waitFor(()->firstTaps.get()==1,5000,"첫 실제 터치가 없습니다");
        h.waitFor(()->secondTaps.get()==1,1800,"새 다음 글자 뒤에도 2400ms 완료 대기가 남았습니다");
        h.waitFor(()->!h.svc.running,4000,"조건 전환 뒤 완료되지 않았습니다");
        assertTrue(evidence(),secondAt.get()-firstAt.get()<1800);
        assertTrue("다음 터치의 새 화면 재확인이 사라졌습니다\n"+evidence(),readsAtNext.get()>=4);
        assertEquals(evidence(),1,firstTaps.get());assertEquals(evidence(),1,secondTaps.get());
    }
    @Test public void identicalVisibleTextNeedsAbsenceAndReappearanceBeforeSecondTap()throws Exception {
        h.main(()->h.entry.setText("같은 글자"));
        Step first=text("같은 글자",200),next=text("같은 글자",0);next.waitForever=true;
        start(macro(true,first,next));h.waitFor(()->firstTaps.get()>=1,5000,"첫 같은 글자 터치가 없습니다");
        SystemClock.sleep(900);assertEquals("같은 화면의 같은 글자를 연속 터치했습니다\n"+evidence(),1,firstTaps.get());
        assertTrue(evidence(),h.svc.running);
        h.main(()->h.entry.setText("아직 없음"));SystemClock.sleep(600);
        assertEquals(evidence(),1,firstTaps.get());h.main(()->h.entry.setText("같은 글자"));
        h.waitFor(()->!h.svc.running,5000,"사라짐 뒤 재등장한 같은 글자가 진행되지 않았습니다");
        assertEquals(evidence(),2,firstTaps.get());
    }
    @Test public void alreadyVisibleNextTextKeepsExplicitAfter()throws Exception {
        h.main(()->second.setText("다음 화면"));
        start(macro(true,text("첫 번째",900),text("다음 화면",0)));
        h.waitFor(()->!h.svc.running,6000,"기존 완료 대기 경로가 완료되지 않았습니다");
        assertEquals(evidence(),1,firstTaps.get());assertEquals(evidence(),1,secondTaps.get());
        assertTrue("처음부터 있던 글자를 전환 증거로 사용했습니다\n"+evidence(),secondAt.get()-firstAt.get()>=850);
    }
    @Test public void sameTextWaitKeepsItsOriginalPresentConditionMeaning()throws Exception {
        Step first=text("첫 번째",350);Step wait=text("첫 번째",0);wait.type="TEXT_WAIT";
        start(macro(true,first,wait));h.waitFor(()->!h.svc.running,5000,"같은 글자 TEXT_WAIT가 부재를 기다리도록 바뀌었습니다");
        assertEquals(evidence(),1,firstTaps.get());assertEquals(evidence(),0,secondTaps.get());
        assertTrue(evidence(),h.svc.logText().contains("2번 글자 나타날 때까지 대기 완료"));
    }
    @Test public void manualWaitAndRoundGapKeepTheirMeaning()throws Exception {
        h.main(()->second.setText("다음 화면"));
        Step wait=new Step("WAIT");wait.duration=350;wait.after=200;
        Macro value=macro(true,text("첫 번째",0),wait,text("다음 화면",0));value.rounds=2;value.gap=450;
        AtomicLong roundBoundary=new AtomicLong();AtomicLong firstRoundSecond=new AtomicLong();
        h.main(()->{h.entry.setOnClickListener(v->{long now=SystemClock.elapsedRealtime();firstAt.set(now);if(firstTaps.incrementAndGet()==2)roundBoundary.set(now-firstRoundSecond.get());});
            second.setOnClickListener(v->{secondAt.set(SystemClock.elapsedRealtime());firstRoundSecond.compareAndSet(0,secondAt.get());secondTaps.incrementAndGet();});});
        start(value);h.waitFor(()->!h.svc.running,7000,"수동 대기·반복 간격 검사가 완료되지 않았습니다");
        assertEquals(evidence(),2,firstTaps.get());assertEquals(evidence(),2,secondTaps.get());
        assertTrue("수동 WAIT 또는 after가 생략되었습니다\n"+evidence(),secondAt.get()-firstAt.get()>=500);
        assertTrue("전체 반복 gap이 생략되었습니다\n"+evidence(),roundBoundary.get()>=400);
    }
    @Test public void finiteAdaptivePollingDoesNotKeepTheOldSixHundredMillisecondTail()throws Exception {
        h.main(()->h.entry.setText("아직 없음"));CountDownLatch initial=new CountDownLatch(1);AtomicLong appearedAt=new AtomicLong();
        sourceField.set(h.svc,(MacroService.ScreenSource)token->{ArrayList<MacroService.Hit> result=originalSource.read(token);
            if(reads.incrementAndGet()==1){h.main(()->h.entry.setText("첫 번째"));appearedAt.set(SystemClock.elapsedRealtime());initial.countDown();}return result;});
        Step wait=new Step("TEXT_WAIT");wait.source="ACCESS";wait.text="첫 번째";wait.match="EXACT";wait.timeout=4000;wait.after=0;
        start(macro(true,wait));assertTrue(initial.await(4,TimeUnit.SECONDS));
        h.waitFor(()->!h.svc.running,450,"적응 모드 유한 글자 대기에 기존 600ms 재탐지가 남았습니다");
        assertTrue(evidence(),SystemClock.elapsedRealtime()-appearedAt.get()<450);assertEquals(evidence(),0,firstTaps.get());
    }
    @Test public void shortAfterDoesNotWaitForAnExtraSlowProbe()throws Exception {
        nextAppearsAfterFirst();CountDownLatch slowProbe=new CountDownLatch(1);
        sourceField.set(h.svc,(MacroService.ScreenSource)token->{int number=reads.incrementAndGet();if(number==3){slowProbe.countDown();SystemClock.sleep(1400);}return originalSource.read(token);});
        start(macro(true,text("첫 번째",100),text("다음 화면",0)));
        h.waitFor(()->firstTaps.get()==1,4000,"첫 터치가 없습니다");
        h.waitFor(()->h.svc.logText().contains("1회 · 2/2"),500,"추가 탐색이 100ms 완료 대기 상한을 넘겨 다음 동작 진입을 막았습니다");
        assertTrue(slowProbe.await(3,TimeUnit.SECONDS));
    }
    @Test public void realStopWorksWhileIdenticalTextIsBlocked()throws Exception {
        h.main(()->h.entry.setText("같은 글자"));Step next=text("같은 글자",0);next.waitForever=true;
        start(macro(true,text("같은 글자",100),next));h.waitFor(()->firstTaps.get()>=1,4000,"첫 터치가 없습니다");SystemClock.sleep(600);
        assertEquals(evidence(),1,firstTaps.get());AccessibilityNodeInfo stop=h.find("■");assertNotNull("실제 정지 버튼이 없습니다",stop);
        long before=SystemClock.elapsedRealtime();assertTrue(stop.performAction(AccessibilityNodeInfo.ACTION_CLICK));stop.recycle();
        h.waitFor(()->!h.svc.running,1000,"동일 글자 차단 중 실제 정지가 응답하지 않습니다");
        assertTrue(SystemClock.elapsedRealtime()-before<1200);SystemClock.sleep(350);assertEquals(evidence(),1,firstTaps.get());
    }
}
