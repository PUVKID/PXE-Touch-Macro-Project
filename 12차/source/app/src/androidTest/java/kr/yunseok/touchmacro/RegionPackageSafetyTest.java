package kr.yunseok.touchmacro;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

/** 앱을 확인할 수 없는 새 설정을 저장하거나 실행하지 않는지 UI로 확인합니다. */
@RunWith(AndroidJUnit4.class) public class RegionPackageSafetyTest {
    final GuidedFlowTest h=new GuidedFlowTest();Field mapping;Object original;
    @Before public void boot()throws Exception{
        h.boot();h.fixture();mapping=MacroService.class.getDeclaredField("windowSource");mapping.setAccessible(true);original=mapping.get(h.svc);
        Macro macro=new Macro();macro.name="대상 앱 확인 검증";Step wait=new Step("WAIT");wait.duration=10;macro.steps.add(wait);
        WatchConfig profile=new WatchConfig();profile.macroName=macro.name;profile.rule.text="안내 대상";
        h.main(()->{Store.put(h.a,macro);WatchStore.save(h.a,profile);SetupFlow.areaIndex(h.a,0);});
    }
    void unavailableWindows(){h.main(()->{try{mapping.set(h.svc,(MacroService.WindowSource)java.util.Collections::emptyList);}catch(Exception e){throw new RuntimeException(e);}});}
    @After public void end()throws Exception{
        if(h.svc!=null)h.main(()->{h.svc.stop();h.svc.controller().cancelRegion();h.svc.hideControls();});
        if(mapping!=null&&original!=null)mapping.set(h.svc,original);h.end();
    }
    @Test public void unknownPackageDoesNotOverwriteSavedRegion()throws Exception{
        h.main(()->{SetupFlow.stage(h.a,1);h.svc.controller().showSetupCoach("REGION");});
        h.click("영역 그리기");h.drag(120,650,950,1050);String before=WatchStore.load(h.a).json().toString();
        unavailableWindows();h.click("이 영역 사용");
        assertEquals(before,WatchStore.load(h.a).json().toString());assertEquals(1,SetupFlow.stage(h.a));assertFalse(h.svc.running);
        AccessibilityNodeInfo button=h.find("이 영역 사용");assertNotNull(button);button.recycle();
    }
    @Test public void unknownPackageDoesNotStartUnboundGuideProfile()throws Exception{
        h.main(()->{SetupFlow.stage(h.a,4);h.svc.controller().showSetupCoach("START");});
        String before=WatchStore.load(h.a).json().toString();unavailableWindows();h.click("이 화면에서 시작");
        assertFalse(h.svc.running);assertTrue(SetupFlow.active(h.a));assertEquals(before,WatchStore.load(h.a).json().toString());
        AccessibilityNodeInfo button=h.find("이 화면에서 시작");assertNotNull(button);button.recycle();
        // 같은 버튼에서 실제 창 조회를 복원하면 정상 실행되어야 합니다.
        h.main(()->{try{mapping.set(h.svc,original);}catch(Exception e){throw new RuntimeException(e);}});
        h.click("이 화면에서 시작");h.waitFor(()->h.svc.running,3000,"창 확인 복원 후 시작");
    }
}
