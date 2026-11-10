package kr.yunseok.touchmacro;

import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONObject;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 소유 별도 UID 앱과 제어 창 관측을 사용합니다. 실제 모델 품질 검사와 구분합니다. */
@RunWith(AndroidJUnit4.class) public class AiStartBoundaryTest {
    static final String TARGET="kr.yunseok.pxe22fixture";
    final GuidedFlowTest h=new GuidedFlowTest();
    final AtomicInteger ownTaps=new AtomicInteger();
    Field windowField;Object originalWindow;
    @Before public void boot()throws Exception{h.boot();h.fixture();h.main(()->h.entry.setOnClickListener(view->ownTaps.incrementAndGet()));windowField=MacroService.class.getDeclaredField("windowSource");windowField.setAccessible(true);originalWindow=windowField.get(h.svc);}
    @After public void end()throws Exception{windowField.set(h.svc,originalWindow);h.shell("am force-stop "+TARGET);h.end();}
    JSONObject context(){return AiJson.object("planning_mode","MACRO","recognition_source","ACCESS","execution_target",TARGET,"points",AiJson.object("실제 버튼",AiJson.object("x",50,"y",10)),"macros",new JSONObject(),"regions",new JSONObject(),"gestures",new JSONObject(),"actions",new JSONObject(),"apps",new JSONObject());}
    AiPlanCompiler.Compiled plan(){JSONObject ast=AiJson.object("schema","pxe.plan.v1","status","ready","body",new org.json.JSONArray().put(AiJson.object("op","TAP","point_ref","실제 버튼")));return AiPlanCompiler.compile(ast,context());}
    void commit(){h.main(()->h.svc.commitAi(h.svc.captureAiLease(),plan()));}
    boolean counter(int count){AccessibilityNodeInfo node=h.find("터치 "+count);if(node==null)return false;node.recycle();return true;}
    void holdUnconfirmedWindow()throws Exception{windowField.set(h.svc,(MacroService.WindowSource)()->Arrays.asList(new WindowPackages.Window(987654,3,true,true)));}
    void waiting(){h.waitFor(()->h.svc.running&&h.svc.progressText().contains("새 화면 확인"),4000,"AI 대상 확인 대기가 없습니다");}
    Macro waitingMacro(){Macro macro=new Macro();macro.name="기존 실행 보존";macro.startDelay=0;macro.maxMinutes=0;Step step=new Step("WAIT");step.duration=600000;step.after=0;macro.steps.add(step);return macro;}
    @Test public void targetAppIsConfirmedBeforeFirstCoordinateAndOwnUiNeverReceivesIt()throws Exception{
        h.shell("am force-stop "+TARGET);commit();h.waitFor(()->!h.svc.running,6000,"새 대상 창 확인 뒤 계획이 완료되지 않았습니다");h.waitFor(()->counter(1),1000,"별도 UID 대상에서 정확히 1회 터치하지 않았습니다");assertEquals(0,ownTaps.get());assertEquals(1,h.svc.hudProgress().completed);
    }
    @Test public void standaloneCallFirstAppPreservesStartAtDisabledSkipAndRepeat()throws Exception{
        h.shell("am force-stop "+TARGET);Macro saved=new Macro();saved.name="첫 앱 실행 보존";saved.startAt=2;saved.rounds=1;saved.startDelay=0;saved.maxMinutes=0;saved.gap=1234;Step excluded=new Step("BACK");excluded.after=0;saved.steps.add(excluded);Step disabled=new Step("APP");disabled.packageName="com.android.settings";disabled.enabled=false;saved.steps.add(disabled);Step app=new Step("APP");app.packageName=TARGET;app.repeat=2;app.after=125;saved.steps.add(app);Step tap=new Step("TAP");tap.x=50;tap.y=10;tap.after=0;saved.steps.add(tap);JSONObject context=context();context.put("execution_target","");AiJson.obj(context.opt("macros")).put("첫 앱",saved.json());AiPlanCompiler.Compiled compiled=AiPlanCompiler.compile(AiJson.object("schema","pxe.plan.v1","status","ready","body",new org.json.JSONArray().put(AiJson.object("op","CALL","macro_ref","첫 앱"))),context);h.main(()->h.svc.commitAi(h.svc.captureAiLease(),compiled));h.waitFor(()->!h.svc.running,7000,"명시 APP 시작 계획이 끝나지 않았습니다");assertTrue(counter(1));assertEquals(0,ownTaps.get());assertEquals(1,h.svc.hudProgress().completed);String log=h.svc.logText();assertTrue(log.contains("3번 앱 실행 완료 (1/2)"));assertTrue(log.contains("3번 앱 실행 완료 (2/2)"));assertEquals(2,compiled.macro().startAt);assertEquals(2,compiled.macro().steps.get(2).repeat);assertEquals(125,compiled.macro().steps.get(2).after);assertEquals(1234,compiled.macro().gap);
    }
    @Test public void actualStopDuringUnconfirmedTargetPreventsAllLateInput()throws Exception{
        h.shell("am force-stop "+TARGET);holdUnconfirmedWindow();commit();waiting();assertEquals(0,ownTaps.get());assertTrue(counter(0));AccessibilityNodeInfo stop=h.find("■");assertNotNull("대상 확인 중 실제 정지 노드가 없습니다",stop);long before=SystemClock.elapsedRealtime();try{assertTrue(stop.performAction(AccessibilityNodeInfo.ACTION_CLICK));}finally{stop.recycle();}h.waitFor(()->!h.svc.running,1000,"실제 정지가 1초 안에 반응하지 않았습니다");assertTrue(SystemClock.elapsedRealtime()-before<=1000);windowField.set(h.svc,originalWindow);SystemClock.sleep(300);assertTrue(counter(0));assertEquals(0,ownTaps.get());
    }
    @Test public void committedGatePausesAndResumesWithoutCancellingTheRun()throws Exception{
        h.shell("am force-stop "+TARGET);holdUnconfirmedWindow();commit();waiting();h.main(()->h.svc.togglePause());assertTrue(h.svc.paused);windowField.set(h.svc,originalWindow);SystemClock.sleep(250);assertTrue(h.svc.running);assertTrue(counter(0));h.main(()->h.svc.togglePause());h.waitFor(()->!h.svc.running,6000,"재개 후 새 대상 관측으로 이어지지 않았습니다");assertTrue(counter(1));assertEquals(0,ownTaps.get());
    }
    @Test public void invalidNewAstDoesNotReplaceExistingRunOrPersistAnySettings()throws Exception{
        String stored=Store.current(h.a),watch=Ui.prefs(h.a).getString("watch_config","");h.main(()->h.svc.start(waitingMacro()));AtomicLong generation=(AtomicLong)ControlDiagnostics.field(h.svc,"generation");long original=generation.get();boolean[] rejected={false};h.main(()->{try{AiPlanCompiler.compile(AiJson.object("schema","pxe.plan.v1","status","ready","body",new org.json.JSONArray().put(AiJson.object("op","TAP","point_ref","여기"))),context());}catch(IllegalArgumentException expected){rejected[0]=true;}});assertTrue(rejected[0]);assertTrue(h.svc.running);assertEquals(original,generation.get());assertEquals(stored,Store.current(h.a));assertEquals(watch,Ui.prefs(h.a).getString("watch_config",""));
    }
    @Test public void planningRawWindowChangeIsDiagnosticButPauseStopAndRestartInvalidatePending()throws Exception{
        h.main(()->h.svc.start(waitingMacro()));MacroService.AiLease[] lease={null};AiCancellation token=new AiCancellation();h.main(()->{lease[0]=h.svc.captureAiLease();h.svc.attachAi(lease[0],token);});((AtomicLong)ControlDiagnostics.field(h.svc,"windowRevision")).incrementAndGet();assertTrue("정적 계획은 raw 창 변경을 실제 입력 허가로 쓰지 않습니다",h.svc.validAiLease(lease[0]));assertFalse(token.cancelled());h.main(()->h.svc.togglePause());assertTrue(token.cancelled());assertFalse(h.svc.validAiLease(lease[0]));h.main(()->h.svc.start(waitingMacro()));assertFalse(h.svc.validAiLease(lease[0]));AiCancellation stopped=new AiCancellation();h.main(()->{lease[0]=h.svc.captureAiLease();h.svc.attachAi(lease[0],stopped);h.svc.stop();});assertTrue(stopped.cancelled());assertFalse(h.svc.validAiLease(lease[0]));
    }
}
