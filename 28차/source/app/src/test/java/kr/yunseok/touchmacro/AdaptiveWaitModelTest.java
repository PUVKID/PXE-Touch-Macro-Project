package kr.yunseok.touchmacro;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** 적응 대기는 명시 선택으로만 저장하며 기존 시간과 수동 대기 의미를 보존합니다. */
public class AdaptiveWaitModelTest {
    private Macro fixture() {
        Macro macro=new Macro();macro.startDelay=123;macro.gap=789;macro.rounds=15;
        Step wait=new Step("WAIT");wait.duration=456;wait.after=654;macro.steps.add(wait);
        return macro;
    }
    @Test public void oldProfileKeepsAdaptiveWaitOffAndAllExplicitTimes() {
        Macro loaded=new Macro(fixture().json());loaded.validate();
        assertFalse(loaded.json().optBoolean("adaptiveWait",false));
        assertEquals(123,loaded.startDelay);assertEquals(789,loaded.gap);
        assertEquals(456,loaded.steps.get(0).duration);assertEquals(654,loaded.steps.get(0).after);
    }
    @Test public void explicitAdaptiveSelectionSurvivesLoadingAndSaving()throws Exception {
        JSONObject stored=fixture().json();stored.put("adaptiveWait",true);
        Macro loaded=new Macro(stored);loaded.validate();
        assertTrue("명시한 적응 대기 선택이 저장 왕복에서 사라졌습니다",loaded.json().optBoolean("adaptiveWait",false));
        assertEquals(15,loaded.rounds);assertEquals(789,loaded.gap);
        assertEquals("WAIT",loaded.steps.get(0).type);assertEquals(456,loaded.steps.get(0).duration);
        assertEquals(654,loaded.steps.get(0).after);
    }
}
