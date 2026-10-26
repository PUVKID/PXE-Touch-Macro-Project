package kr.yunseok.touchmacro;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;

/** 이전 앱에서 저장한 설정을 PXE 업데이트 후 그대로 읽는지 검사합니다. */
@RunWith(AndroidJUnit4.class) public class PxeUpgradeTest {
    Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    @Test public void seedPreviousVersion()throws Exception{
        Context c=context();assertEquals(9,c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionCode);
        Macro m=new Macro();m.name="이전 설정 유지 검증";Step wait=new Step("WAIT");wait.duration=1250;wait.repeat=3;m.steps.add(wait);Store.put(c,m);
        WatchConfig p=new WatchConfig();p.rule.text="유지할 글자";p.macroName=m.name;p.repetitions=15;p.cooldownMs=600000;p.intervalSeconds=10;p.priorityAreas.add(new WatchArea(5,20,95,35));p.priorityAreas.add(new WatchArea(5,40,95,55));WatchStore.save(c,p);SetupFlow.finished(c);Ui.prefs(c).edit().putInt("theme",2).commit();
    }
    @Test public void verifyAfterPxeUpdate()throws Exception{
        Context c=context();assertEquals(11,c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionCode);assertEquals("PXE",c.getApplicationInfo().loadLabel(c.getPackageManager()).toString());
        Macro m=Store.load(c);assertEquals("이전 설정 유지 검증",m.name);assertEquals(1250,m.steps.get(0).duration);assertEquals(3,m.steps.get(0).repeat);assertTrue(Store.library(c).containsKey(m.name));
        WatchConfig p=WatchStore.load(c);assertEquals("유지할 글자",p.rule.text);assertEquals(m.name,p.macroName);assertEquals(15,p.repetitions);assertEquals(600000,p.cooldownMs);assertEquals(10,p.intervalSeconds);assertEquals(2,p.priorityAreas.size());assertFalse(SetupFlow.active(c));assertEquals(2,Ui.prefs(c).getInt("theme",0));
    }
}
