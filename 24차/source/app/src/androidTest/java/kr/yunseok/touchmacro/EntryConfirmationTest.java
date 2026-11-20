package kr.yunseok.touchmacro;

import android.app.AlertDialog;
import android.graphics.Rect;
import android.os.SystemClock;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 실제 목록 터치·독립 진입 확인·뒤로가기·일시정지를 검사합니다. */
@RunWith(AndroidJUnit4.class) public class EntryConfirmationTest {
    final GuidedFlowTest h=new GuidedFlowTest();final AtomicInteger entries=new AtomicInteger(),actions=new AtomicInteger(),backs=new AtomicInteger();
    Button action;TextView expected,rejected;AlertDialog detail;WatchConfig profile;Macro macro;
    Field screenField;Object originalScreen;final CountDownLatch held=new CountDownLatch(1),release=new CountDownLatch(1);
    @Before public void boot()throws Exception{
        h.boot();h.fixture();
        h.main(()->{
            h.entry.setText("목록 항목");LinearLayout body=new LinearLayout(h.a);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(30,30,30,30);
            expected=new TextView(h.a);expected.setText("배");expected.setTextSize(28);body.addView(expected);
            rejected=new TextView(h.a);rejected.setText("");rejected.setTextSize(28);body.addView(rejected);
            action=new Button(h.a);action.setText("실행 동작");action.setOnClickListener(v->actions.incrementAndGet());body.addView(action,new LinearLayout.LayoutParams(-1,Ui.dp(h.a,70)));
            detail=new AlertDialog.Builder(h.a).setTitle("진입 화면").setView(body).create();detail.show();
        });h.in.waitForIdleSync();Rect[] bounds={null};h.main(()->{bounds[0]=h.bounds(action);detail.dismiss();detail.setOnDismissListener(d->backs.incrementAndGet());h.entry.setOnClickListener(v->{entries.incrementAndGet();detail.show();});});
        macro=new Macro();macro.name="21차 진입 확인 검사";macro.startDelay=0;macro.maxMinutes=0;Step tap=new Step("TAP");tap.x=bounds[0].exactCenterX()*100.0/h.svc.size()[0];tap.y=bounds[0].exactCenterY()*100.0/h.svc.size()[1];tap.duration=50;tap.after=0;macro.steps.add(tap);
        Step rule=new Step("TEXT_WAIT");rule.text="배";rule.source="ACCESS";rule.match="EXACT";rule.timeout=600;
        profile=new WatchConfig(new WatchConfig().json().put("entryCheck",true).put("entryRule",rule.json()).put("rejectText","사과"));
        profile.rule.text="목록 항목";profile.rule.source="ACCESS";profile.rule.match="EXACT";profile.rule.immediate=true;
        profile.macroName=macro.name;profile.targetPackage=h.a.getPackageName();profile.startMs=0;profile.entryMs=300;profile.returnMs=300;profile.settleMs=300;profile.repetitions=1;profile.repeatGapMs=0;profile.cooldownMs=60000;profile.intervalSeconds=60;
        h.main(()->Store.put(h.a,macro));screenField=MacroService.class.getDeclaredField("accessibilitySource");screenField.setAccessible(true);originalScreen=screenField.get(h.svc);
    }
    @After public void end()throws Exception{release.countDown();h.end();if(screenField!=null)screenField.set(h.svc,originalScreen);h.main(()->{if(detail!=null)detail.dismiss();});}
    void start(){h.main(()->h.svc.startWatch(profile));}
    @Test public void expectedWordRunsOneMacroThenCompletesAndBacks()throws Exception{
        start();h.waitFor(()->h.svc.watchCompleted==1,6000,"정상 확인 뒤 완료되지 않았습니다");
        assertEquals(1,entries.get());assertEquals(1,actions.get());assertEquals(1,backs.get());assertEquals(1,h.svc.hudProgress().completed);
    }
    @Test public void explicitRejectWinsWhenBothWordsExistAndImmediatelyRescansWithoutCooldown()throws Exception{
        h.main(()->rejected.setText("사과"));start();h.waitFor(()->entries.get()>=2,5000,"되돌아간 뒤 긴 완료 대기를 적용했습니다");h.main(()->h.svc.stop());
        assertEquals("거절 글자보다 매크로를 먼저 실행했습니다",0,actions.get());assertTrue(backs.get()>=1);assertEquals(0,h.svc.watchCompleted);assertEquals(0,h.svc.hudProgress().completed);
    }
    @Test public void missingExpectedReturnsWithoutAnyMacroOrCompletedCount()throws Exception{
        h.main(()->expected.setText("없음"));start();h.waitFor(()->backs.get()>=1,5000,"정상 부재 뒤 뒤로가지 않았습니다");h.main(()->h.svc.stop());
        assertEquals(0,actions.get());assertEquals(0,h.svc.watchCompleted);assertEquals(0,h.svc.hudProgress().completed);
    }
    @Test public void pauseDoesNotConsumeConfirmationTimeOrDispatchBackThenResumeContinues()throws Exception{
        h.main(()->expected.setText("없음"));start();h.waitFor(()->h.svc.progressText().contains("진입 확인"),3000,"진입 확인 단계가 없습니다");
        h.main(()->h.svc.togglePause());SystemClock.sleep(1000);assertTrue(h.svc.paused);assertEquals(0,backs.get());assertEquals(0,actions.get());
        h.main(()->{expected.setText("배");h.svc.togglePause();});h.waitFor(()->h.svc.watchCompleted==1,4000,"계속 실행 뒤 기존 진입을 완료하지 못했습니다");assertEquals(1,entries.get());assertEquals(1,actions.get());
    }
    @Test public void delayedScreenReadIsRecoveryAndNeverMeansAbsentOrBack()throws Exception{
        screenField.set(h.svc,(MacroService.ScreenSource)token->{if(entries.get()>0){held.countDown();try{release.await();}catch(InterruptedException error){Thread.currentThread().interrupt();}}return ((MacroService.ScreenSource)originalScreen).read(token);});
        start();assertTrue(held.await(4,TimeUnit.SECONDS));SystemClock.sleep(3900);assertTrue(h.svc.running);assertEquals(0,backs.get());assertEquals(0,actions.get());
        h.main(()->h.svc.stop());release.countDown();SystemClock.sleep(400);assertEquals(0,backs.get());assertEquals(0,actions.get());
    }
    @Test public void failedEntryKeepsPersistedDuplicateReservation()throws Exception{
        profile.preventDuplicate=true;h.main(()->{WatchStore.blocked(h.a,profile,false);expected.setText("없음");});start();h.waitFor(()->backs.get()>=1,5000,"진입 확인 실패가 복귀하지 않았습니다");SystemClock.sleep(800);
        assertEquals(1,entries.get());assertTrue(WatchStore.blocked(h.a,profile));assertEquals(0,actions.get());assertEquals(0,h.svc.watchCompleted);
    }
    @Test public void independentEntryRegionExcludesExpectedOutsideIt()throws Exception{
        profile=new WatchConfig(profile.json().put("entryRule",profile.json().getJSONObject("entryRule").put("top",80).put("bottom",99)));
        start();h.waitFor(()->backs.get()>=1,5000,"목록 영역과 독립적인 확인 영역을 사용하지 않았습니다");h.main(()->h.svc.stop());assertEquals(0,actions.get());assertEquals(0,h.svc.watchCompleted);
    }
    @Test public void disabledEntryPreservesLegacyMacroEvenWhenExpectedIsAbsent()throws Exception{
        profile=new WatchConfig(profile.json().put("entryCheck",false));h.main(()->expected.setText("없음"));start();h.waitFor(()->h.svc.watchCompleted==1,5000,"기존 꺼짐 프로필이 바뀌었습니다");assertEquals(1,actions.get());
    }
    @Test public void failedSecondPriorityReturnsToFirstPriorityWithoutMacro()throws Exception{
        AtomicInteger first=new AtomicInteger(),second=new AtomicInteger();Button[] other={null};Rect[] firstArea={null},secondArea={null};
        h.main(()->{h.entry.setText("없음");other[0]=new Button(h.a);other[0].setText("목록 항목");((LinearLayout)h.entry.getParent()).addView(other[0],new LinearLayout.LayoutParams(-1,Ui.dp(h.a,70)));
            expected.setText("없음");h.entry.setOnClickListener(v->{first.incrementAndGet();detail.show();});other[0].setOnClickListener(v->{second.incrementAndGet();detail.show();});detail.setOnDismissListener(d->{backs.incrementAndGet();h.entry.setText("목록 항목");});});
        h.in.waitForIdleSync();h.main(()->{firstArea[0]=h.bounds(h.entry);secondArea[0]=h.bounds(other[0]);});
        for(Rect box:new Rect[]{firstArea[0],secondArea[0]})profile.priorityAreas.add(new WatchArea(0,box.top*100.0/h.svc.size()[1],100,box.bottom*100.0/h.svc.size()[1]));
        start();h.waitFor(()->first.get()==1,5000,"2순위 실패 뒤 1순위부터 다시 탐지하지 않았습니다");h.main(()->h.svc.stop());assertEquals(1,second.get());assertEquals(1,backs.get());assertEquals(0,actions.get());assertEquals(0,h.svc.watchCompleted);
    }
    @Test public void realOcrEntryConfirmationUsesNewScreenAndRunsOnce()throws Exception{
        profile.entryRule.source="OCR";profile.entryRule.timeout=3000;start();h.waitFor(()->h.svc.watchCompleted==1,18000,"실제 OCR 진입 확인을 통과하지 못했습니다");assertEquals(1,entries.get());assertEquals(1,actions.get());assertEquals(1,backs.get());
    }
}
