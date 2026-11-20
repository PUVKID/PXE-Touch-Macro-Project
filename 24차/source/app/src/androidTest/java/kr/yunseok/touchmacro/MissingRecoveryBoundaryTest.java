package kr.yunseok.touchmacro;

import android.graphics.Rect;
import android.os.SystemClock;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 두 번째 원자 전달 전에 일시정지해 이미 완료한 복구 입력의 중복을 검출합니다. */
@RunWith(AndroidJUnit4.class) public class MissingRecoveryBoundaryTest {
    final MissingRetryFlowTest base=new MissingRetryFlowTest();final GuidedFlowTest h=base.h;
    final AtomicInteger recovered=new AtomicInteger(),finalTaps=new AtomicInteger(),queries=new AtomicInteger();final AtomicBoolean gated=new AtomicBoolean();
    final CountDownLatch held=new CountDownLatch(1),release=new CountDownLatch(1);Field windowField;MacroService.WindowSource originalWindow;
    @Before public void boot()throws Exception{base.boot();windowField=MacroService.class.getDeclaredField("windowSource");windowField.setAccessible(true);originalWindow=(MacroService.WindowSource)windowField.get(h.svc);}
    @After public void end()throws Exception{release.countDown();windowField.set(h.svc,originalWindow);base.end();}
    void run(String type,int repeats,int holdQuery)throws Exception{
        Button[] recover={null};h.main(()->{LinearLayout body=new LinearLayout(h.a);body.setPadding(40,Ui.dp(h.a,180),40,0);body.setOrientation(LinearLayout.VERTICAL);recover[0]=new Button(h.a);recover[0].setText("복구 실제 터치");recover[0].setOnClickListener(v->recovered.incrementAndGet());body.addView(recover[0],new LinearLayout.LayoutParams(-1,Ui.dp(h.a,70)));h.entry=new Button(h.a);h.entry.setText("안내 대상");h.entry.setOnClickListener(v->finalTaps.incrementAndGet());body.addView(h.entry,new LinearLayout.LayoutParams(-1,Ui.dp(h.a,70)));h.a.setContentView(body);});h.in.waitForIdleSync();Rect[] at={null};h.main(()->at[0]=h.bounds(recover[0]));
        Step action=new Step(type);action.x=at[0].exactCenterX()*100.0/h.svc.size()[0];action.y=at[0].exactCenterY()*100.0/h.svc.size()[1];action.repeat=repeats;action.duration=60;action.after=150;
        Step word=new Step(base.word("ACTION_RETRY",true).json().put("recoveryTarget",1));Macro m=new Macro();m.startDelay=0;m.maxMinutes=0;m.startAt=2;m.steps.add(action);m.steps.add(word);
        base.source.set(h.svc,(MacroService.ScreenSource)token->recovered.get()<2?new ArrayList<>():base.original.read(token));
        windowField.set(h.svc,(MacroService.WindowSource)()->{if(recovered.get()==1&&!gated.get()&&queries.incrementAndGet()==holdQuery&&gated.compareAndSet(false,true)){held.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}}return originalWindow.read();});
        base.start(m);assertTrue("첫 복구 뒤 두 번째 전달 전 창 조회에 도달하지 않았습니다",held.await(5,TimeUnit.SECONDS));assertEquals(1,recovered.get());h.main(()->h.svc.togglePause());SystemClock.sleep(300);assertEquals(1,recovered.get());h.main(()->h.svc.togglePause());release.countDown();h.waitFor(()->!h.svc.running,5000,"부분 복구를 이어서 완료하지 못했습니다");assertEquals("일시정지 뒤 이미 성공한 복구 터치를 다시 보냈습니다",2,recovered.get());assertEquals(1,finalTaps.get());assertEquals(1,h.svc.hudProgress().completed);
    }
    @Test public void pausedSecondRepeatNeverReplaysFirstSuccessfulTap()throws Exception{run("TAP",2,2);}
    @Test public void pausedSecondDoubleTapNeverReplaysFirstSuccessfulTap()throws Exception{run("DOUBLE",1,1);}
}
