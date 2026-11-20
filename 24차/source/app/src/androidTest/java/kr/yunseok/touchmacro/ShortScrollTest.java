package kr.yunseok.touchmacro;

import android.os.SystemClock;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** 실제 주입된 제스처의 화면 좌표로 자동 스크롤 두 경로를 확인합니다. */
@RunWith(AndroidJUnit4.class) public class ShortScrollTest {
    final GuidedFlowTest h=new GuidedFlowTest();final AtomicInteger ups=new AtomicInteger();
    volatile float startY,endY;Field source;Object original;
    @Before public void boot()throws Exception{
        h.boot();h.fixture();source=MacroService.class.getDeclaredField("accessibilitySource");source.setAccessible(true);original=source.get(h.svc);
        source.set(h.svc,(MacroService.ScreenSource)token->new ArrayList<>());
        h.main(()->{FrameLayout target=new FrameLayout(h.a);target.setBackgroundColor(0xffffffff);target.setOnTouchListener((view,event)->{if(event.getActionMasked()==MotionEvent.ACTION_DOWN)startY=event.getRawY();if(event.getActionMasked()==MotionEvent.ACTION_UP){endY=event.getRawY();ups.incrementAndGet();}return true;});h.a.setContentView(target);});
        h.in.waitForIdleSync();h.waitFor(()->h.a.getPackageName().equals(h.svc.activePackage()),4000,"대상 앱 확인 실패");
    }
    @After public void end()throws Exception{h.end();source.set(h.svc,original);}
    void assertShort(){int height=h.svc.size()[1];assertEquals(height*.75,startY,2);assertEquals(height*.525,endY,2);assertEquals(height*.225,startY-endY,3);}
    @Test public void missingTextRetryUsesHalfAutomaticScroll()throws Exception{
        Step step=new Step("TEXT_WAIT");step.text="없는 글자";step.source="ACCESS";step.timeout=0;step.failure="SCROLL_RETRY";step.retries=1;step.retryDelay=300;step.after=0;
        Macro m=new Macro();m.startDelay=0;m.maxMinutes=0;m.steps.add(step);h.main(()->h.svc.start(m));h.waitFor(()->ups.get()==1,5000,"자동 재시도 스크롤 없음");h.main(()->h.svc.stop());assertShort();
    }
    @Test public void missingWatchTextUsesHalfAutomaticScroll()throws Exception{
        Macro macro=new Macro();macro.name="자동 스크롤 검사";macro.steps.add(new Step("TAP"));
        WatchConfig p=new WatchConfig();p.rule.text="없는 글자";p.rule.source="ACCESS";p.direction="SCROLL_DOWN";p.targetPackage=h.a.getPackageName();p.startMs=0;p.intervalSeconds=1;p.settleMs=300;p.macroName=macro.name;
        h.main(()->{Store.put(h.a,macro);h.svc.startWatch(p);});h.waitFor(()->ups.get()==1,5000,"목록 새로고침 스크롤 없음");h.main(()->h.svc.stop());assertShort();
    }
    @Test public void manuallyRecordedSwipeCoordinatesStayUnchanged()throws Exception{
        Step step=new Step("SWIPE");step.x=50;step.x2=50;step.y=70;step.y2=20;step.duration=450;step.after=0;Macro m=new Macro();m.startDelay=0;m.maxMinutes=0;m.steps.add(step);
        h.main(()->h.svc.start(m));h.waitFor(()->ups.get()==1,5000,"수동 스와이프 없음");h.waitFor(()->!h.svc.running,2000,"수동 동작 완료 실패");int height=h.svc.size()[1];assertEquals(height*.70,startY,2);assertEquals(height*.20,endY,2);
    }
}
