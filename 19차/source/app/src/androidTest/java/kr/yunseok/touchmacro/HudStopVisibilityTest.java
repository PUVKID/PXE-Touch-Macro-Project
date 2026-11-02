package kr.yunseok.touchmacro;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 하단 진행창을 숨겨도 무관한 위쪽 정지를 유지하고 다중 경로를 보호합니다. */
@RunWith(AndroidJUnit4.class) public class HudStopVisibilityTest {
    final RunHudTest checks=new RunHudTest();
    final int[] taps={0};
    GuidedFlowTest h;
    android.widget.Button target;
    @Before public void boot()throws Exception{checks.boot();h=checks.h;target=checks.fullScreenTarget(taps);checks.shortStats();}
    @After public void end(){checks.end();}
    View panel()throws Exception{return (View)checks.field(h.svc.controller(),"panel");}
    boolean panelShown(){boolean[] shown={false};h.main(()->{try{View value=panel();shown[0]=value!=null&&value.isShown();}catch(Exception e){throw new RuntimeException(e);}});return shown[0];}
    Rect bounds(View view){Rect[] value={null};h.main(()->value[0]=h.bounds(view));return value[0];}
    boolean hudHidden(){try{return !checks.hud().isShown();}catch(Exception e){throw new RuntimeException(e);}}
    @Test public void longBottomGestureKeepsRealTopStopAndCancelsFollowingAction()throws Exception{
        final int[] canceled={0};h.main(()->target.setOnTouchListener((v,event)->{if(event.getActionMasked()==MotionEvent.ACTION_CANCEL)canceled[0]++;return false;}));
        Rect bottom=bounds(checks.hud());int[] size=h.svc.size();Macro m=new Macro();m.startDelay=0;
        Step hold=new Step("LONG");hold.x=bottom.exactCenterX()*100/size[0];hold.y=bottom.exactCenterY()*100/size[1];hold.duration=4000;hold.after=0;m.steps.add(hold);
        Step next=new Step("TAP");next.x=50;next.y=40;next.duration=70;next.after=0;m.steps.add(next);
        h.main(()->h.svc.start(m));h.waitFor(this::hudHidden,2000,"긴제스처전진행창숨김없음");SystemClock.sleep(350);
        assertTrue("하단진행창만겹쳤는데위쪽정지창도숨겨졌습니다",panelShown());
        assertTrue(h.svc.running);assertTrue("ticker가긴제스처중HUD를되살렸습니다",hudHidden());
        AccessibilityNodeInfo stop=h.find("■");assertNotNull("실제정지노드가없습니다",stop);
        Rect actualStop=new Rect();stop.getBoundsInScreen(actualStop);stop.recycle();long before=SystemClock.elapsedRealtime();h.tap(actualStop.exactCenterX(),actualStop.exactCenterY());
        h.waitFor(()->!h.svc.running,Math.max(1,1000-(SystemClock.elapsedRealtime()-before)),"실제정지버튼1초내응답없음");long stopMs=SystemClock.elapsedRealtime()-before;assertTrue(stopMs<1000);
        assertFalse("정지뒤다음동작이실행되었습니다",h.svc.logText().contains("2번 좌표 터치 완료"));assertEquals(0,h.svc.hudProgress().completed);
        assertEquals("중단한긴누름이대상클릭으로완료되었습니다",0,taps[0]);assertTrue("실제입력취소를관측하지못했습니다",canceled[0]>0);
        h.waitFor(()->{try{return checks.hud().isShown();}catch(Exception e){return false;}},2000,"정지뒤진행창복원실패");
        org.json.JSONObject result=new org.json.JSONObject();result.put("stop_ms",stopMs);result.put("actual_status",h.svc.status);result.put("running",h.svc.running);result.put("target_cancel_events",canceled[0]);result.put("target_clicks",taps[0]);result.put("completed_rounds",h.svc.hudProgress().completed);try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(h.a.getExternalFilesDir(null),"hud-physical-stop-observation.json"))){out.write(result.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    }
    @Test public void firstHudStrokeThenTopPanelStrokeKeepsExistingFullHide()throws Exception{
        Rect bottom=bounds(checks.hud()),top=bounds(panel());int[] size=h.svc.size();
        org.json.JSONArray lower=new org.json.JSONArray().put(GesturePaths.point(bottom.exactCenterX()*100/size[0],bottom.exactCenterY()*100/size[1]));
        org.json.JSONArray upper=new org.json.JSONArray().put(GesturePaths.point(top.exactCenterX()*100/size[0],top.exactCenterY()*100/size[1]));
        Step gesture=new Step("GESTURE");gesture.strokes.put(GesturePaths.stroke(lower,0,70));gesture.strokes.put(GesturePaths.stroke(upper,600,70));gesture.after=0;
        Macro m=new Macro();m.startDelay=0;m.steps.add(gesture);h.main(()->h.svc.start(m));h.waitFor(this::hudHidden,2000,"다중제스처숨김없음");assertFalse("뒤stroke의정지창충돌을놓쳤습니다",panelShown());h.waitFor(()->!h.svc.running,5000,"다중제스처미완료");assertEquals("두실제터치가대상앱에도착하지않았습니다",2,taps[0]);h.waitFor(this::panelShown,2000,"기존전체숨김복원실패");
    }
}
