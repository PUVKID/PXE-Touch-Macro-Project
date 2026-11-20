package kr.yunseok.touchmacro;

import android.app.AlertDialog;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 기존 15초 벤치를 보존하고 실제 업무의 별도 120초 기능 수락을 확인합니다. */
@RunWith(AndroidJUnit4.class)
public class FloatingFunctionalCycleTest {
    private static final long CYCLE_LIMIT_MS=120000;
    private final FloatingPlayTest controls=new FloatingPlayTest();
    private final GuidedFlowTest h=controls.h;
    private boolean returnStateObserved,dialogShowing,dialogAttached,dialogFocused,activityFocused;

    @Before public void boot()throws Exception{controls.boot();}
    @After public void end(){controls.end();}

    @Test public void namedMacroCompletesExactlyFifteenActionsAndReturnsWithinTwoMinutes()throws Throwable{
        String run=InstrumentationRegistry.getArguments().getString("pxe_run_id");
        long cycleStart=0,cycleComplete=0;boolean assertionsPassed=false;Throwable primary=null;
        try{
            assertNotNull("독립 실행 식별자 pxe_run_id가 필요합니다",run);
            assertTrue("실행 식별자는 안전한 ASCII 1~64자여야 합니다",run.matches("[A-Za-z0-9_-]{1,64}"));
            Button[] action={null};Rect[] at={null};
            h.main(()->{LinearLayout body=new LinearLayout(h.a);body.setPadding(40,100,40,100);action[0]=new Button(h.a);action[0].setText("등록한 매크로 동작");action[0].setOnClickListener(v->controls.actions++);body.addView(action[0],new LinearLayout.LayoutParams(-1,Ui.dp(h.a,80)));h.detail=new AlertDialog.Builder(h.a).setTitle("작업 화면").setView(body).create();h.detail.show();});
            h.in.waitForIdleSync();
            h.main(()->{at[0]=h.bounds(action[0]);h.detail.dismiss();h.entry.setOnClickListener(v->{controls.entries++;h.detail.show();});});
            Macro current=new Macro();current.name="현재 편집 중인 다른 동작";Step wait=new Step("WAIT");wait.duration=10;wait.after=0;current.steps.add(wait);
            Macro selected=new Macro();selected.name="탐지에 등록한 매크로";Step tap=new Step("TAP");tap.duration=60;tap.after=0;int[] size=h.svc.size();tap.x=at[0].exactCenterX()*100.0/size[0];tap.y=at[0].exactCenterY()*100.0/size[1];selected.steps.add(tap);
            WatchConfig p=new WatchConfig();p.rule.text="안내 대상";p.rule.source="ACCESS";p.rule.match="EXACT";p.macroName=selected.name;p.repetitions=15;p.startMs=100;p.entryMs=300;p.returnMs=300;p.repeatGapMs=0;p.cooldownMs=600000;p.targetPackage=h.a.getPackageName();
            h.main(()->{Store.put(h.a,selected);Store.save(h.a,current);WatchStore.save(h.a,p);});
            String original=Store.load(h.a).json().toString();
            controls.expand();
            // 탐지 버튼을 누르는 시간부터 진입·15회·뒤로 복귀까지 같은 단조 기한을 사용합니다.
            cycleStart=SystemClock.elapsedRealtime();
            controls.click("글자 탐지 시작");
            h.waitFor(()->h.svc.watching,Math.min(3000,remaining(cycleStart)),"재생이 탐지로 시작되지 않았습니다");
            h.waitFor(()->h.svc.watchCompleted==1,remaining(cycleStart),"별도 120초 기능 검사에서 탐지 한 바퀴가 완료되지 않았습니다");
            cycleComplete=SystemClock.elapsedRealtime();
            assertTrue("진입·15회·복귀가 120초를 넘었습니다",cycleComplete-cycleStart<=CYCLE_LIMIT_MS);
            assertEquals("진입이 중복되었습니다",1,controls.entries);
            assertEquals("등록한 매크로가 정확히 15회 실행되어야 합니다",15,controls.actions);
            assertEquals(original,Store.load(h.a).json().toString());
            assertEquals("완료 후 쉬는 중",h.svc.watchPhase);
            // 같은 단언 시점의 UI 상태를 메인 스레드에서 함께 읽습니다.
            h.main(()->{dialogShowing=h.detail.isShowing();android.view.View decor=h.detail.getWindow()==null?null:h.detail.getWindow().getDecorView();dialogAttached=decor!=null&&decor.isAttachedToWindow();dialogFocused=decor!=null&&decor.hasWindowFocus();activityFocused=h.a.hasWindowFocus();returnStateObserved=true;});
            assertFalse("작업 화면에서 탐지 화면으로 돌아오지 않았습니다",dialogShowing);
            controls.expand();controls.click("일시정지");
            h.waitFor(()->h.svc.paused,3000,"일시정지되지 않았습니다");
            assertEquals(15,controls.actions);assertEquals(1,controls.entries);
            controls.click("계속 실행");h.waitFor(()->!h.svc.paused,3000,"다시 실행되지 않았습니다");
            controls.click("정지");h.waitFor(()->!h.svc.running,3000,"정지되지 않았습니다");
            assertEquals("완료 뒤 추가 입력이 발생했습니다",15,controls.actions);
            assertEquals("완료 뒤 다시 진입했습니다",1,controls.entries);
            assertionsPassed=true;
        }catch(Throwable failure){primary=failure;throw failure;}
        finally{
            // 고정 계측 관측의 실패가 원래 기능 단언 실패를 덮지 않게 보존합니다.
            try{publishMetrics(run,cycleStart,cycleComplete,assertionsPassed,primary);}
            catch(Throwable observation){if(primary!=null)primary.addSuppressed(observation);else throw observation;}
        }
    }

    private static long remaining(long started){return Math.max(0,CYCLE_LIMIT_MS-(SystemClock.elapsedRealtime()-started));}
    private Object field(String name)throws Exception{Field f=MacroService.class.getDeclaredField(name);f.setAccessible(true);return f.get(h.svc);}
    private void publishMetrics(String run,long started,long completed,boolean passed,Throwable failure)throws Exception{
        long now=SystemClock.elapsedRealtime();Method active=MacroService.class.getDeclaredMethod("activeTime");active.setAccessible(true);
        ExecutionMetrics metrics=(ExecutionMetrics)field("executionMetrics");
        ExecutionMetrics.Snapshot measured=metrics.snapshot((Long)active.invoke(h.svc));
        RunProgress progress=h.svc.hudProgress();long elapsed=started==0?0:(completed==0?now:completed)-started;
        JSONObject row=new JSONObject().put("run_id",run==null?JSONObject.NULL:run).put("test_passed",passed).put("failure_type",failure==null?JSONObject.NULL:failure.getClass().getName())
            .put("cycle_start_elapsed_ms",started).put("cycle_complete_elapsed_ms",completed).put("observed_elapsed_ms",now).put("cycle_elapsed_ms",elapsed)
            .put("total_elapsed_ms",started==0?0:now-started).put("cycle_limit_ms",CYCLE_LIMIT_MS).put("legacy_benchmark_limit_ms",15000).put("target_90s_met",completed>0&&elapsed<=90000).put("within_120s",completed>0&&elapsed<=CYCLE_LIMIT_MS)
            .put("service_started_at_ms",((Long)field("startedAt")).longValue()).put("metrics_identity",System.identityHashCode(metrics)).put("generation",((AtomicLong)field("generation")).get())
            .put("return_state_observed",returnStateObserved).put("dialog_showing",returnStateObserved?dialogShowing:JSONObject.NULL).put("dialog_attached",returnStateObserved?dialogAttached:JSONObject.NULL).put("dialog_focused",returnStateObserved?dialogFocused:JSONObject.NULL).put("activity_focused",returnStateObserved?activityFocused:JSONObject.NULL)
            .put("running",h.svc.running).put("paused",h.svc.paused).put("watch_completed",h.svc.watchCompleted).put("entries",controls.entries).put("actions",controls.actions)
            .put("progress_total",progress.total).put("progress_current",progress.current).put("progress_completed",progress.completed).put("progress_step",progress.step)
            .put("step_ms",measured.stepMs).put("steps",measured.steps).put("ocr_ms",measured.ocrMs).put("ocr_count",measured.ocrCount).put("capture_ms",measured.captureMs).put("configured_ms",measured.configuredMs);
        Bundle status=new Bundle();status.putString("pxe.floating.metrics",row.toString());h.in.sendStatus(2,status);
    }
}
