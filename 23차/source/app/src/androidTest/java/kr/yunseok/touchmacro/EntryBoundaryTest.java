package kr.yunseok.touchmacro;

import android.app.AlertDialog;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 실제 두 화면 복귀와 연결 해제 직후 남아 있는 정지 버튼을 검사합니다. */
@RunWith(AndroidJUnit4.class) public class EntryBoundaryTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    AlertDialog inner,outer;final AtomicInteger backs=new AtomicInteger(),entries=new AtomicInteger(),actions=new AtomicInteger();
    @Before public void boot()throws Exception{h.boot();}
    @After public void end(){h.end();h.main(()->{if(inner!=null)inner.dismiss();if(outer!=null)outer.dismiss();});}
    @Test public void twoBacksKeepCompletedCountAcrossPauseAndResumeThenRescanFirstPriority()throws Exception{
        h.fixture();
        h.main(()->{
            h.entry.setText("목록 항목");
            LinearLayout inside=new LinearLayout(h.a);inside.setOrientation(LinearLayout.VERTICAL);TextView reject=new TextView(h.a);reject.setText("사과");reject.setTextSize(30);inside.addView(reject);
            Button action=new Button(h.a);action.setText("매크로 실행 동작");action.setOnClickListener(v->actions.incrementAndGet());inside.addView(action,new LinearLayout.LayoutParams(-1,Ui.dp(h.a,70)));
            inner=new AlertDialog.Builder(h.a).setTitle("두 번째 진입 화면").setView(inside).create();
            outer=new AlertDialog.Builder(h.a).setTitle("첫 번째 진입 화면").setMessage("배가 없는 중간 화면").create();
            inner.setOnDismissListener(d->{backs.incrementAndGet();h.svc.togglePause();});
            outer.setOnDismissListener(d->{backs.incrementAndGet();h.entry.setText("목록 항목");});
            h.entry.setOnClickListener(v->{if(entries.incrementAndGet()==1){outer.show();h.svc.togglePause();}else h.svc.togglePause();});
        });
        Macro macro=new Macro();macro.name="다중 복귀 경계 검사";macro.startDelay=0;macro.maxMinutes=0;Step tap=new Step("TEXT_TAP");tap.text="매크로 실행 동작";tap.source="ACCESS";tap.match="EXACT";tap.immediate=true;tap.timeout=500;tap.after=0;macro.steps.add(tap);
        WatchConfig p=new WatchConfig();p.entryCheck=true;p.entryRule.text="배";p.entryRule.source="ACCESS";p.entryRule.match="EXACT";p.entryRule.timeout=500;p.rejectText="사과";
        p.rule.text="목록 항목";p.rule.source="ACCESS";p.rule.match="EXACT";p.rule.immediate=true;p.targetPackage=h.a.getPackageName();p.macroName=macro.name;p.startMs=0;p.entryMs=300;p.returnMs=500;p.backCount=2;p.intervalSeconds=60;p.cooldownMs=60000;
        h.main(()->{Store.put(h.a,macro);h.svc.startWatch(p);});
        h.waitFor(()->observed("첫 번째 진입 화면"),3000,"중간 창의 실제 관측을 확인하지 못했습니다");h.main(()->inner.show());h.waitFor(()->observed("두 번째 진입 화면"),3000,"안쪽 창의 실제 관측을 확인하지 못했습니다");h.main(()->h.svc.togglePause());
        h.waitFor(()->backs.get()==1&&h.svc.paused,4000,"첫 실제 BACK 뒤 일시정지되지 않았습니다");
        SystemClock.sleep(900);assertEquals("일시정지 중 추가 BACK",1,backs.get());assertEquals(1,entries.get());assertEquals(0,h.svc.watchCompleted);assertEquals(0,h.svc.hudProgress().completed);
        h.main(()->h.svc.togglePause());h.waitFor(()->entries.get()==2,6000,"두 화면을 복귀한 뒤 목록 1순위 탐지가 재개되지 않았습니다");
        assertEquals("이미 수행한 BACK 횟수를 잃었습니다",2,backs.get());assertTrue(h.svc.paused);assertEquals(0,actions.get());assertEquals(0,h.svc.watchCompleted);assertEquals(0,h.svc.hudProgress().completed);
    }
    boolean observed(String title){
        try{
            boolean active=false;java.util.ArrayList<WindowPackages.Window> windows=new java.util.ArrayList<>();
            for(android.view.accessibility.AccessibilityWindowInfo window:h.auto.getWindows()){
                try{
                    windows.add(new WindowPackages.Window(window.getId(),window.getType(),window.isActive(),window.isFocused()));
                    if(window.isActive()&&window.getType()==WindowPackages.APPLICATION){
                        android.view.accessibility.AccessibilityNodeInfo root=window.getRoot(),found=null;
                        try{found=h.search(root,title);active=found!=null;}finally{if(found!=null)found.recycle();if(root!=null)root.recycle();}
                    }
                }finally{window.recycle();}
            }
            // 시험 화면이 실제 활성화되고 기존 서비스의 패키지 이벤트로 관측될 때까지 기다립니다.
            WindowPackages packages=(WindowPackages)ControlDiagnostics.field(h.svc,"windowPackages");
            return active&&h.a.getPackageName().equals(packages.resolve(windows,h.a.getPackageName()));
        }catch(Exception error){throw new RuntimeException(error);}
    }
    View text(View root,String value){if(root instanceof TextView&&value.contentEquals(((TextView)root).getText()))return root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View out=text(group.getChildAt(i),value);if(out!=null)return out;}}return null;}
    @Test public void staleStopButtonHandlesServiceLookupNullAndRendersDisconnected()throws Exception{
        Macro macro=new Macro();macro.startDelay=0;Step wait=new Step("WAIT");wait.duration=60000;wait.after=0;macro.steps.add(wait);
        h.main(()->{SetupFlow.finished(h.a);h.svc.start(macro);h.a.refreshSetup();});h.in.waitForIdleSync();
        View[] stop={null};h.main(()->stop[0]=text(h.a.getWindow().getDecorView(),"실행 정지"));assertNotNull(stop[0]);
        Field current=MacroService.class.getDeclaredField("current");current.setAccessible(true);Object original=current.get(null);Throwable[] escaped={null};
        try{
            h.main(()->{try{current.set(null,new java.lang.ref.WeakReference<MacroService>(null));stop[0].performClick();}catch(Throwable error){escaped[0]=error;}});
            assertNull("연결 해제 직후 정지 버튼에서 예외가 전파됐습니다: "+escaped[0],escaped[0]);
            h.main(()->{assertNull(text(h.a.getWindow().getDecorView(),"실행 정지"));assertNotNull(text(h.a.getWindow().getDecorView(),"연결하기"));});
        }finally{h.main(()->{try{current.set(null,original);h.svc.stop();h.a.refreshSetup();}catch(Exception error){throw new RuntimeException(error);}});}
    }
}
