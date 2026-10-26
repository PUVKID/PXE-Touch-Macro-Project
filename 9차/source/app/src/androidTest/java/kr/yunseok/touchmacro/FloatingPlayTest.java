package kr.yunseok.touchmacro;
import android.app.*;import android.graphics.Rect;import android.os.SystemClock;import android.view.accessibility.*;import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class) public class FloatingPlayTest {
    GuidedFlowTest h=new GuidedFlowTest();volatile int entries,actions;String lastUi="";
    @Before public void boot()throws Exception{h.boot();h.fixture();h.main(()->{h.svc.controller().show();h.svc.controller().setVisible(true);});}
    @After public void end(){h.end();}
    AccessibilityNodeInfo find(AccessibilityNodeInfo n,String description){
        if(n==null)return null;if(description.contentEquals(n.getContentDescription()==null?"":n.getContentDescription()))return AccessibilityNodeInfo.obtain(n);
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo c=n.getChild(i);AccessibilityNodeInfo hit=find(c,description);if(c!=null)c.recycle();if(hit!=null)return hit;}return null;
    }
    android.view.View view(android.view.View v,String description){if(v==null)return null;if(description.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++){android.view.View found=view(g.getChildAt(i),description);if(found!=null)return found;}}return null;}
    boolean press(String description){
        Rect[] rect={null};h.main(()->{try{java.lang.reflect.Field f=FloatingController.class.getDeclaredField("panel");f.setAccessible(true);android.view.View root=(android.view.View)f.get(h.svc.controller());android.view.View button=view(root,description);lastUi="panel="+root+", button="+button+", current="+(MacroService.get()==h.svc);if(button!=null&&button.isShown()&&button.getWidth()>0)rect[0]=h.bounds(button);}catch(Exception e){throw new RuntimeException(e);}});
        if(rect[0]==null)return false;h.tap(rect[0].exactCenterX(),rect[0].exactCenterY());return true;
    }
    void click(String description){long end=SystemClock.elapsedRealtime()+5000;while(SystemClock.elapsedRealtime()<end){if(press(description))return;SystemClock.sleep(80);}fail(description+" · "+lastUi);}
    void expand(){if(h.svc.controller().isCollapsed()){click("NEX 컨트롤 펼치기 · 길게 눌러 정지");h.waitFor(()->!h.svc.controller().isCollapsed(),3000,"Panel did not expand");}}
    @Test public void playStartsDetectionUsesNamedMacroFifteenTimesAndPauseResumeStillWork()throws Exception{
        Button[] action={null};Rect[] at={null};
        h.main(()->{LinearLayout body=new LinearLayout(h.a);body.setPadding(40,100,40,100);action[0]=new Button(h.a);action[0].setText("등록한 매크로 동작");action[0].setOnClickListener(v->actions++);body.addView(action[0],new LinearLayout.LayoutParams(-1,Ui.dp(h.a,80)));h.detail=new AlertDialog.Builder(h.a).setTitle("작업 화면").setView(body).create();h.detail.show();});h.in.waitForIdleSync();h.main(()->{at[0]=h.bounds(action[0]);h.detail.dismiss();h.entry.setOnClickListener(v->{entries++;h.detail.show();});});
        Macro current=new Macro();current.name="현재 편집 중인 다른 동작";Step wait=new Step("WAIT");wait.duration=10;wait.after=0;current.steps.add(wait);
        Macro selected=new Macro();selected.name="탐지에 등록한 매크로";Step tap=new Step("TAP");tap.duration=60;tap.after=0;int[] size=h.svc.size();tap.x=at[0].exactCenterX()*100.0/size[0];tap.y=at[0].exactCenterY()*100.0/size[1];selected.steps.add(tap);
        WatchConfig p=new WatchConfig();p.rule.text="안내 대상";p.rule.source="ACCESS";p.rule.match="EXACT";p.macroName=selected.name;p.repetitions=15;p.startMs=100;p.entryMs=300;p.returnMs=300;p.repeatGapMs=0;p.cooldownMs=600000;p.targetPackage=h.a.getPackageName();
        h.main(()->{Store.put(h.a,selected);Store.save(h.a,current);WatchStore.save(h.a,p);});String original=Store.load(h.a).json().toString();
        expand();click("글자 탐지 시작");h.waitFor(()->h.svc.watching,3000,"Play ran the current macro instead of detection");
        h.waitFor(()->h.svc.watchCompleted==1,15000,"Floating play did not finish detection cycle");assertEquals(1,entries);assertEquals(15,actions);assertEquals(original,Store.load(h.a).json().toString());assertEquals("완료 후 쉬는 중",h.svc.watchPhase);
        expand();click("일시정지");h.waitFor(()->h.svc.paused,3000,"Pause failed");click("계속 실행");h.waitFor(()->!h.svc.paused,3000,"Resume failed");click("정지");h.waitFor(()->!h.svc.running,3000,"Stop failed");
    }
    @Test public void unconfiguredPlayNeverFallsBackToCurrentMacro()throws Exception{
        h.main(()->{Ui.prefs(h.a).edit().remove("watch_config").commit();h.entry.setOnClickListener(v->entries++);Macro m=new Macro();Step tap=new Step("TAP");Rect r=h.bounds(h.entry);int[] size=h.svc.size();tap.x=r.exactCenterX()*100.0/size[0];tap.y=r.exactCenterY()*100.0/size[1];m.startDelay=0;m.steps.add(tap);Store.save(h.a,m);});
        expand();click("글자 탐지 시작");SystemClock.sleep(1200);assertFalse(h.svc.running);assertEquals(0,entries);
    }
}
