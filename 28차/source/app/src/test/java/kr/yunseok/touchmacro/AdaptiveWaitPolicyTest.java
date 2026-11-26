package kr.yunseok.touchmacro;

import org.junit.Test;
import static org.junit.Assert.*;

/** 실제 전환 검사에서 발견한 조기 재터치와 시간 예산의 경계를 순수 정책으로 고정합니다. */
public class AdaptiveWaitPolicyTest {
    Step text(String value){Step step=new Step("TEXT_TAP");step.text=value;step.source="OCR";return step;}
    @Test public void uncertainOrDifferentExecutionPathsKeepTheirExplicitDelay() {
        Macro macro=new Macro();Step first=text("처음"),next=text("다음");
        assertFalse(AdaptiveWaitPolicy.eligible(macro,first,next));macro.adaptiveWait=true;
        assertTrue(AdaptiveWaitPolicy.eligible(macro,first,next));
        first.repeat=2;assertFalse(AdaptiveWaitPolicy.eligible(macro,first,next));first.repeat=1;
        next.source="ACCESS";assertFalse(AdaptiveWaitPolicy.eligible(macro,first,next));next.source="OCR";
        next.left=10;assertFalse(AdaptiveWaitPolicy.eligible(macro,first,next));next.left=0;
        next.type="WAIT";assertFalse(AdaptiveWaitPolicy.eligible(macro,first,next));
        next.type="TEXT_BRANCH";assertFalse(AdaptiveWaitPolicy.eligible(macro,first,next));
    }
    @Test public void sameTextCannotRearmFromOneMissingFrameOrInterruptedAbsence() {
        AdaptiveWaitPolicy.Reappearance gate=new AdaptiveWaitPolicy.Reappearance();
        assertFalse(gate.observe(true));assertFalse(gate.observe(false));assertFalse(gate.observe(true));
        assertFalse(gate.observe(false));gate.interrupt();assertFalse(gate.observe(false));assertFalse(gate.observe(true));
        assertFalse(gate.observe(false));assertFalse(gate.observe(false));assertTrue(gate.observe(true));
    }
    @Test public void leadingDelayDoesNotSpendTheFollowingFiniteTimeout() {
        assertEquals(0,AdaptiveWaitPolicy.timeoutElapsed(1000,3400,3399));
        assertEquals(0,AdaptiveWaitPolicy.timeoutElapsed(1000,3400,3400));
        assertEquals(300,AdaptiveWaitPolicy.timeoutElapsed(1000,3400,3700));
        assertEquals(300,AdaptiveWaitPolicy.timeoutElapsed(3400,0,3700));
    }
}
