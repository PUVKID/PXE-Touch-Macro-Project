package kr.yunseok.touchmacro;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ContinuousWaitTest {
    @Test public void oldZeroTimeoutRemainsSingleCheckAndKeepsAfter()throws Exception{
        Step step=new Step(new JSONObject("{\"type\":\"TEXT_TAP\",\"text\":\"확인\",\"timeout\":0,\"after\":1234}"));
        step.validate(1);assertFalse(step.waitForever);assertFalse(step.immediate);assertEquals(0,step.timeout);assertEquals(1234,step.completionDelay());
    }
    @Test public void oldFiniteTimeoutDoesNotAcquireNewOptions(){
        Step step=new Step("TEXT_WAIT");step.text="다음";step.timeout=600000;Step loaded=new Step(step.json());
        loaded.validate(1);assertFalse(loaded.waitForever);assertFalse(loaded.immediate);assertEquals(600000,loaded.timeout);
    }
    @Test public void explicitOptionsRoundTripWithoutOverwritingSavedDelay(){
        Step step=new Step("TEXT_TAP");step.text="계속";step.timeout=0;step.after=9999;step.afterJitter=100;step.waitForever=true;step.immediate=true;
        Step loaded=new Step(step.json());loaded.validate(1);assertTrue(loaded.waitForever);assertTrue(loaded.immediate);assertEquals(0,loaded.completionDelay());assertEquals(9999,loaded.after);assertEquals(100,loaded.afterJitter);
        loaded.immediate=false;assertEquals(9999,loaded.completionDelay());
    }
    @Test(expected=IllegalArgumentException.class) public void branchCannotHideMissingBranchForever(){Step step=new Step("TEXT_BRANCH");step.text="확인";step.waitForever=true;step.validate(1);}
    @Test(expected=IllegalArgumentException.class) public void coordinateActionCannotEnableTextImmediate(){Step step=new Step("TAP");step.immediate=true;step.validate(1);}
    @Test public void explicitUnlimitedWaitHasNoHiddenThirtyMinuteWatchDeadline(){Macro m=new Macro();m.maxMinutes=0;Step s=new Step("TEXT_WAIT");s.text="다음";s.waitForever=true;m.steps.add(s);assertEquals(0,m.watchDeadline(1000,0));assertEquals(5000,m.watchDeadline(1000,5000));m.maxMinutes=2;assertEquals(121000,m.watchDeadline(1000,0));}
    @Test public void oldUnlimitedWatchSequenceKeepsExistingThirtyMinuteBoundary(){Macro m=new Macro();m.maxMinutes=0;m.steps.add(new Step("WAIT"));assertEquals(1801000,m.watchDeadline(1000,0));}
}
