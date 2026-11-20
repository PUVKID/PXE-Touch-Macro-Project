package kr.yunseok.touchmacro;

import android.graphics.Rect;
import android.view.View;
import android.view.WindowManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONArray;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 제스처의 HUD 보호 기간과 늦은 복원·새 실행 경계를 확인합니다. */
@RunWith(AndroidJUnit4.class) public class HudGestureLeaseTest {
    final RunHudTest r=new RunHudTest();final GuidedFlowTest h=r.h;
    @Before public void boot()throws Exception{r.boot();}
    @After public void end(){r.end();}
    JSONArray stroke(double x,double y)throws Exception{return new JSONArray().put(GesturePaths.stroke(new JSONArray().put(GesturePaths.point(x,y)),0,60));}
    Object prepare(JSONArray strokes)throws Exception{return h.svc.controller().prepareGesture(strokes,h.svc.size()[0],h.svc.size()[1]);}
    boolean waits(Object lease)throws Exception{return lease instanceof Boolean?(Boolean)lease:(Boolean)r.field(lease,"waitForOverlay");}
    void release(Object lease)throws Exception{
        if(lease instanceof Boolean)h.svc.controller().restoreGesture();
        else h.svc.controller().getClass().getDeclaredMethod("restoreGesture",lease.getClass()).invoke(h.svc.controller(),lease);
    }
    RunHud hud()throws Exception{return (RunHud)r.field(h.svc.controller(),"hud");}
    View handle()throws Exception{return (View)r.field(hud(),"handle");}
    void releaseMain(Object lease){h.main(()->{try{release(lease);}catch(Exception error){throw new RuntimeException(error);}});}
    @Test public void absentHudCannotAppearDuringNonCollidingGestureAndReturnsAfterward()throws Exception{
        int[] taps={0};r.fullScreenTarget(taps);r.shortStats();Object[] lease={null};
        h.main(()->{try{h.svc.controller().close();h.svc.controller().show();assertNull(hud());lease[0]=prepare(stroke(50,50));h.svc.controller().update();assertNull("제스처 중 새 HUD를 만들었습니다",hud());assertFalse("비충돌 동작에 충돌 대기를 추가했습니다",waits(lease[0]));}catch(Exception error){throw new RuntimeException(error);}});
        try{h.tap(h.svc.size()[0]/2f,h.svc.size()[1]/2f);h.waitFor(()->taps[0]==1,1000,"보호 중 실제 대상 입력이 차단됐습니다");}
        finally{releaseMain(lease[0]);}
        h.waitFor(()->{try{return r.hud()!=null&&r.hud().isShown()&&handle().isShown();}catch(Exception error){return false;}},2000,"보호 종료 뒤 새 HUD 복원 없음");
    }
    @Test public void configuredHandleBoundsProtectEvenWhenViewIsHidden()throws Exception{
        int[] taps={0};r.fullScreenTarget(taps);r.shortStats();Object[] lease={null};Rect[] grip={null};
        h.main(()->{try{WindowManager.LayoutParams p=(WindowManager.LayoutParams)r.field(hud(),"handleParams");grip[0]=new Rect(p.x,p.y,p.x+p.width,p.y+p.height);hud().visible(false);lease[0]=prepare(stroke(grip[0].exactCenterX()*100/h.svc.size()[0],grip[0].exactCenterY()*100/h.svc.size()[1]));assertTrue("실제 창 좌표의 충돌을 놓쳤습니다",waits(lease[0]));h.svc.controller().update();assertFalse(r.hud().isShown());assertFalse(handle().isShown());}catch(Exception error){throw new RuntimeException(error);}});
        h.in.waitForIdleSync();android.os.SystemClock.sleep(80);
        try{h.tap(grip[0].exactCenterX(),grip[0].exactCenterY());h.waitFor(()->taps[0]==1,1000,"숨긴 손잡이가 실제 대상 입력을 막았습니다");}
        finally{releaseMain(lease[0]);}
        h.waitFor(()->{try{return r.hud().isShown()&&handle().isShown();}catch(Exception error){return false;}},2000,"기존 HUD 복원 없음");
    }
    @Test public void oldReleaseCannotUnlockNewGestureLease()throws Exception{
        r.shortStats();h.main(()->{try{Object old=prepare(stroke(50,50)),current=prepare(stroke(50,50));release(old);h.svc.controller().update();assertFalse("이전 복원이 새 보호를 해제했습니다",r.hud().isShown());assertFalse(handle().isShown());release(current);assertTrue(r.hud().isShown());assertTrue(handle().isShown());}catch(Exception error){throw new RuntimeException(error);}});
    }
    @Test public void newWaitRunResetsOldLeaseAndLateReleaseCannotUnlockItsNewLease()throws Exception{
        r.begin();h.waitFor(()->{try{return r.hud()!=null&&r.hud().isShown();}catch(Exception error){return false;}},3000,"기존 HUD 준비 없음");Object[] old={null};
        h.main(()->{try{old[0]=prepare(stroke(50,50));h.svc.togglePause();h.svc.controller().update();assertTrue(h.svc.paused);assertFalse(r.hud().isShown());assertFalse(handle().isShown());assertTrue("보호 중 기존 HUD의 일시정지 표시가 갱신되지 않았습니다",r.text(r.hud()).contains("일시정지"));h.svc.stop();Macro m=new Macro();m.startDelay=0;m.maxMinutes=0;Step wait=new Step("WAIT");wait.duration=5000;wait.after=0;m.steps.add(wait);h.svc.start(m);}catch(Exception error){throw new RuntimeException(error);}});
        h.in.waitForIdleSync();h.waitFor(()->{try{return h.svc.running&&r.hud().isShown()&&handle().isShown();}catch(Exception error){return false;}},2000,"새 WAIT 실행이 이전 보호로 계속 숨겨졌습니다");
        h.main(()->{try{Object current=prepare(stroke(50,50));release(old[0]);assertFalse("늦은 이전 실행 복원이 새 보호를 해제했습니다",r.hud().isShown());assertFalse(handle().isShown());View stop=ControlDiagnostics.stopView((View)r.field(h.svc.controller(),"panel"));assertTrue("상단 정지가 보호 구간에서 가려졌습니다",stop!=null&&stop.isShown());release(current);assertTrue(r.hud().isShown());assertTrue(handle().isShown());}catch(Exception error){throw new RuntimeException(error);}});
    }
}
