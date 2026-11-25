package kr.yunseok.touchmacro;

import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 미완료 설정의 실제 Activity 재개가 실행 중 정지 컨트롤을 숨기지 않는지 검사합니다. */
@RunWith(AndroidJUnit4.class) public class ControlVisibilityTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    @Before public void boot()throws Exception{
        MacroService previous=MacroService.get();h.boot();
        h.waitFor(()->MacroService.get()!=null&&MacroService.get()!=previous,12000,"새 접근성 서비스 연결 실패");h.svc=MacroService.get();
    }
    @After public void end(){h.end();}
    void reopen(){h.a=(MainActivity)h.in.startActivitySync(new Intent(h.in.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));h.in.waitForIdleSync();}
    void hiddenSetup()throws Exception{
        h.main(()->{SetupFlow.begin(h.a);h.svc.controller().show();h.svc.controller().setVisible(true);});reopen();
        boolean[] hidden={false};h.main(()->{try{View panel=(View)ControlDiagnostics.field(h.svc.controller(),"panel");hidden[0]=Boolean.FALSE.equals(ControlDiagnostics.field(h.svc.controller(),"visible"))&&panel!=null&&panel.getVisibility()==View.GONE;}catch(Exception error){throw new RuntimeException(error);}});
        assertTrue("실제 미완료 설정 재개가 숨김 상태를 만들지 못했습니다\n"+ControlDiagnostics.capture(h,"setup-hidden"),hidden[0]);h.fixture();
    }
    Macro macro(){Macro m=new Macro();m.name="정지 컨트롤 표시 검사";m.startDelay=0;m.maxMinutes=0;Step step=new Step("TEXT_WAIT");step.text="진단 화면에 없는 글자";step.source="ACCESS";step.waitForever=true;step.immediate=true;step.timeout=0;step.after=0;m.steps.add(step);return m;}
    void assertStopAndClick(String name){
        long deadline=SystemClock.elapsedRealtime()+3000;
        while(!ControlDiagnostics.stopShown(h)&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(50);
        String evidence=ControlDiagnostics.capture(h,name);assertTrue("실제 정지 뷰가 보이지 않습니다\n"+evidence,ControlDiagnostics.stopShown(h));
        AccessibilityNodeInfo button=h.find("■");assertNotNull("정지 접근성 노드가 없습니다\n"+evidence,button);assertTrue(evidence,button.isVisibleToUser());
        long before=SystemClock.elapsedRealtime();try{assertTrue(evidence,button.performAction(AccessibilityNodeInfo.ACTION_CLICK));}finally{button.recycle();}
        h.waitFor(()->!h.svc.running,1000,"실제 정지 버튼 중단 실패");assertTrue("정지 버튼 응답이 1초를 넘었습니다",SystemClock.elapsedRealtime()-before<=1000);
    }
    @Test public void startAfterIncompleteSetupRestoresRealStopControl()throws Exception{
        hiddenSetup();h.main(()->h.svc.start(macro()));assertStopAndClick("start-after-setup");
    }
    @Test public void activityResumeKeepsRunningStopAndRestartRestoresHiddenControl()throws Exception{
        hiddenSetup();h.main(()->{h.svc.controller().setVisible(true);h.svc.start(macro());});
        h.waitFor(()->h.svc.running,1000,"매크로 시작 실패");reopen();
        assertTrue("설정 화면 재개 중 실행 상태 유실",h.svc.running);assertStopAndClick("resume-while-running");
        // 정지 뒤에는 기존 설정 안내의 숨김 기능이 유지되고 재실행 때만 복원되어야 합니다.
        reopen();assertFalse("미실행 설정 안내에서 컨트롤이 숨겨지지 않았습니다",ControlDiagnostics.stopShown(h));h.fixture();
        h.main(()->h.svc.start(macro()));assertStopAndClick("restart-after-setup");
    }
}
