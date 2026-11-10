package kr.yunseok.touchmacro;

import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;
import android.view.WindowManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 실제 창 플래그와 물리 입력으로 진행창의 입력 경계를 확인합니다. */
@RunWith(AndroidJUnit4.class) public class HudInteractionTest {
    final RunHudTest r=new RunHudTest();final GuidedFlowTest h=r.h;
    @Before public void boot()throws Exception{r.boot();}
    @After public void end(){r.end();}
    View handle()throws Exception{return (View)r.field(r.field(h.svc.controller(),"hud"),"handle");}
    @Test public void bodyIsUntouchableAccessibilityOverlay()throws Exception{
        r.shortStats();WindowManager.LayoutParams p=(WindowManager.LayoutParams)r.hud().getLayoutParams();assertEquals(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,p.type);assertTrue("진행창 본문이 수동 입력을 가로챕니다",(p.flags&WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)!=0);assertFalse(r.hud().isClickable());
    }
    @Test public void manualTapThroughBodyClicksUnderlyingButtonExactlyOnce()throws Exception{
        int[] taps={0};r.fullScreenTarget(taps);r.shortStats();Rect[] body={null};h.main(()->{try{body[0]=h.bounds(r.hud());}catch(Exception e){throw new RuntimeException(e);}});h.tap(body[0].left+body[0].width()/3f,body[0].exactCenterY());h.waitFor(()->taps[0]==1,1000,"본문 아래 실제 버튼이 눌리지 않았습니다");assertEquals(1,taps[0]);
    }
    @Test public void separateHandleIsFortyEightDpAndDragDoesNotCollapseOrClickApp()throws Exception{
        int[] taps={0};r.fullScreenTarget(taps);r.shortStats();View handle=handle();Rect[] before={null};h.main(()->before[0]=h.bounds(handle));assertEquals(Ui.dp(h.a,48),before[0].width());assertEquals(Ui.dp(h.a,48),before[0].height());h.drag(before[0].exactCenterX(),before[0].exactCenterY(),before[0].exactCenterX()-Ui.dp(h.a,40),before[0].exactCenterY()-Ui.dp(h.a,100));h.in.waitForIdleSync();assertFalse(Ui.prefs(h.a).getBoolean("hud_collapsed",false));assertEquals(0,taps[0]);Rect[] after={null};h.main(()->after[0]=h.bounds(handle));assertTrue("손잡이가 이동하지 않았습니다",Math.abs(after[0].top-before[0].top)>Ui.dp(h.a,20));h.tap(after[0].exactCenterX(),after[0].exactCenterY());h.waitFor(()->Ui.prefs(h.a).getBoolean("hud_collapsed",false),1000,"짧은 손잡이 터치로 접히지 않았습니다");assertEquals(0,taps[0]);
    }
    @Test public void manualScrollStartingUnderBodyReachesUnderlyingScrollView()throws Exception{
        android.widget.ScrollView[] scroll={null};h.main(()->{scroll[0]=new android.widget.ScrollView(h.a);android.widget.TextView content=new android.widget.TextView(h.a);content.setText("실제 스크롤 대상\n".repeat(150));content.setTextSize(20);scroll[0].addView(content,new android.widget.ScrollView.LayoutParams(-1,Ui.dp(h.a,2400)));h.a.setContentView(scroll[0]);});h.in.waitForIdleSync();r.shortStats();Rect[] body={null};h.main(()->{try{body[0]=h.bounds(r.hud());}catch(Exception e){throw new RuntimeException(e);}});h.drag(body[0].left+body[0].width()/3f,body[0].exactCenterY(),body[0].left+body[0].width()/3f,body[0].top-Ui.dp(h.a,180));h.waitFor(()->scroll[0].getScrollY()>Ui.dp(h.a,30),1000,"본문에서 시작한 수동 스크롤이 통과하지 않았습니다");assertFalse(Ui.prefs(h.a).getBoolean("hud_collapsed",false));
    }
    @Test public void opacityZeroFiftyHundredAndCaptureMaskKeepHandleIndependent()throws Exception{
        r.shortStats();int panelOpacity=Ui.prefs(h.a).getInt("opacity",95);for(int opacity:new int[]{0,50,100}){h.main(()->{Ui.prefs(h.a).edit().putInt("hud_opacity",opacity).commit();h.svc.controller().update();});assertEquals(opacity/100f,r.hud().getAlpha(),.001f);assertEquals(1f,handle().getAlpha(),.001f);assertTrue(handle().isShown());assertEquals(panelOpacity,Ui.prefs(h.a).getInt("opacity",95));h.screenshot("PXE22_진행창_불투명도"+opacity+".png");}h.main(()->h.svc.controller().setCaptureMode(true));assertFalse(r.hud().isShown());assertFalse(handle().isShown());h.main(()->h.svc.controller().setCaptureMode(false));assertTrue(r.hud().isShown());assertTrue(handle().isShown());
    }
    @Test public void savedDraggedPositionAndCollapseSurviveServiceRecreation()throws Exception{
        r.shortStats();Rect[] start={null};h.main(()->{try{start[0]=h.bounds(handle());}catch(Exception e){throw new RuntimeException(e);}});h.drag(start[0].exactCenterX(),start[0].exactCenterY(),start[0].exactCenterX()-Ui.dp(h.a,35),start[0].exactCenterY()-Ui.dp(h.a,80));r.tapHandle();assertTrue(Ui.prefs(h.a).getBoolean("hud_collapsed",false));int x=Ui.prefs(h.a).getInt("hud_x",-1),y=Ui.prefs(h.a).getInt("hud_y",-1);assertTrue(x>=0);assertTrue(y>=0);MacroService old=h.svc;h.boot();h.fixture();r.shortStats();assertNotSame(old,h.svc);assertTrue(Ui.prefs(h.a).getBoolean("hud_collapsed",false));Rect[] body={null};h.main(()->{try{body[0]=h.bounds(r.hud());}catch(Exception e){throw new RuntimeException(e);}});assertEquals(x,body[0].left);assertEquals(y,body[0].top);assertEquals(Ui.dp(h.a,48),body[0].height());
    }
    @Test public void offscreenStoredPositionClampsAndResetRestoresBottom()throws Exception{
        r.shortStats();h.main(()->{h.svc.controller().close();Ui.prefs(h.a).edit().putInt("hud_x",Integer.MAX_VALUE).putInt("hud_y",Integer.MIN_VALUE).commit();h.svc.controller().show();h.svc.controller().update();});h.in.waitForIdleSync();Rect[] grip={null};h.main(()->{try{grip[0]=h.bounds(handle());}catch(Exception e){throw new RuntimeException(e);}});assertTrue(grip[0].left>=0);assertTrue(grip[0].right<=h.svc.size()[0]);assertTrue(grip[0].top>=0);assertTrue(grip[0].bottom<=h.svc.size()[1]);h.main(()->h.svc.controller().resetHudPosition());h.in.waitForIdleSync();h.main(()->{try{grip[0]=h.bounds(handle());}catch(Exception e){throw new RuntimeException(e);}});assertTrue(grip[0].top>h.svc.size()[1]/2);assertFalse(Ui.prefs(h.a).contains("hud_x"));assertFalse(Ui.prefs(h.a).contains("hud_y"));
    }
    @Test public void gestureOverHandleAloneHidesBothWindowsAndRestoresThem()throws Exception{
        int[] taps={0};r.fullScreenTarget(taps);r.shortStats();Rect[] grip={null};h.main(()->{try{grip[0]=h.bounds(handle());}catch(Exception e){throw new RuntimeException(e);}});Macro m=new Macro();m.startDelay=0;Step tap=new Step("TAP");tap.x=grip[0].exactCenterX()*100.0/h.svc.size()[0];tap.y=grip[0].exactCenterY()*100.0/h.svc.size()[1];tap.duration=100;tap.after=0;m.steps.add(tap);h.main(()->h.svc.start(m));h.waitFor(()->!h.svc.running,4000,"손잡이 위 자동 입력 완료 없음");assertEquals(1,taps[0]);assertFalse(Ui.prefs(h.a).getBoolean("hud_collapsed",false));h.waitFor(()->{try{return r.hud().isShown()&&handle().isShown();}catch(Exception e){return false;}},2000,"두 창이 함께 복원되지 않았습니다");
    }
    @Test public void draggingTopControllerOverHandleKeepsRealStopTouchable()throws Exception{
        int oldX=Ui.prefs(h.a).getInt("panel_x",Ui.dp(h.a,6)),oldY=Ui.prefs(h.a).getInt("panel_y",Ui.dp(h.a,95));try{r.begin();h.waitFor(()->{try{return handle()!=null&&handle().isShown();}catch(Exception e){return false;}},3000,"손잡이 없음");FloatingPlayTest panel=new FloatingPlayTest();panel.h=h;Rect[] logo={null},grip={null};h.main(()->{try{View root=(View)r.field(h.svc.controller(),"panel");View icon=panel.view(root,"PXE 컨트롤 펼치기 · 길게 눌러 정지");logo[0]=h.bounds(icon);grip[0]=h.bounds(handle());}catch(Exception e){throw new RuntimeException(e);}});h.drag(logo[0].exactCenterX(),logo[0].exactCenterY(),grip[0].exactCenterX(),grip[0].exactCenterY());h.waitFor(()->{boolean[] clear={false};h.main(()->{try{clear[0]=!Rect.intersects(h.bounds((View)r.field(h.svc.controller(),"panel")),h.bounds(handle()));}catch(Exception e){throw new RuntimeException(e);}});return clear[0];},2000,"상단 제어창 이동 뒤 손잡이가 정지를 가렸습니다");long at=SystemClock.elapsedRealtime();panel.click("정지");h.waitFor(()->!h.svc.running,1000,"실제 정지 입력이 가려졌습니다");assertTrue(SystemClock.elapsedRealtime()-at<1000);}finally{h.main(()->Ui.prefs(h.a).edit().putInt("panel_x",oldX).putInt("panel_y",oldY).commit());}
    }
    @Test public void stationaryLongPressOnLogoStillStopsTheCurrentRun()throws Exception{
        r.begin();FloatingPlayTest control=new FloatingPlayTest();control.h=h;Rect[] logo={null};h.waitFor(()->{boolean[] ready={false};h.main(()->{try{View panel=(View)r.field(h.svc.controller(),"panel"),icon=control.view(panel,"PXE 컨트롤 펼치기 · 길게 눌러 정지");if(icon!=null&&icon.isLaidOut()){logo[0]=h.bounds(icon);ready[0]=h.svc.running&&logo[0].width()>0&&logo[0].height()>0;}}catch(Exception e){throw new RuntimeException(e);}});return ready[0];},3000,"길게 누르기 실제 로고 준비 없음");long down=SystemClock.uptimeMillis();h.touch(logo[0].exactCenterX(),logo[0].exactCenterY(),android.view.MotionEvent.ACTION_DOWN,down);try{h.waitFor(()->!h.svc.running,1000,"움직이지 않은 길게 누르기의 정지가 사라졌습니다");assertEquals(0,h.svc.hudProgress().completed);}finally{android.view.MotionEvent up=android.view.MotionEvent.obtain(down,SystemClock.uptimeMillis(),android.view.MotionEvent.ACTION_UP,logo[0].exactCenterX(),logo[0].exactCenterY(),0);up.setSource(android.view.InputDevice.SOURCE_TOUCHSCREEN);h.auto.injectInputEvent(up,true);up.recycle();}
    }
}
