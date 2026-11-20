package kr.yunseok.touchmacro;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** 전달의 종류·대기·사용자 시간과 시범 확정을 검사하며 SDK 성공을 대신하지 않습니다. */
public class BehaviorAndroidFlowTest {
    private BehaviorExample.Observation obs(String previous,long time){return new BehaviorExample.Observation(new BehaviorExample.Binding("task","session","example.target",1,0,3,time,100,200),List.of(new BehaviorExample.Token("확인",1,1,30,20,"LINE")),previous,new double[8]);}
    @Test public void acceptedBackAndObservedWaitRemainUnconfirmedUntilUserFinishes(){
        for(String result:List.of("ACCEPTED_GLOBAL","OBSERVED_WAIT")){
            BehaviorAndroidSession session=new BehaviorAndroidSession("task",new BehaviorExample.Replay(),null);session.stage(obs("",10),"action",result);
            assertEquals(1,session.draftCount());assertTrue(session.confirmed().all().isEmpty());session.observeBefore(obs("action",40));
            assertTrue(session.confirmed().all().isEmpty());BehaviorExample.Replay confirmed=session.finish(obs("action",50),true);
            assertEquals(1,confirmed.active("task").size());assertEquals(40,confirmed.all().get(0).after.binding.capturedAtMs);
        }
    }
    @Test public void rejectedCancelledAndUnknownDeliveryNeverBecomeGold(){for(String result:List.of("REJECTED","CANCELLED","UNKNOWN")){BehaviorAndroidSession session=new BehaviorAndroidSession("task",new BehaviorExample.Replay(),null);session.stage(obs("",10),"action",result);assertEquals(0,session.draftCount());assertTrue(session.finish(obs("",20),true).all().isEmpty());}}
    @Test public void waitingSupportAndConfidenceAreDistinctFromReadiness(){
        assertEquals(BehaviorAndroidSession.Readiness.WAIT_SUPPORT,BehaviorAndroidSession.readiness(null,null,-1));
        assertEquals(BehaviorAndroidSession.Readiness.WAIT_CONFIDENCE,BehaviorAndroidSession.readiness("a",new double[]{.7,.3},0));
        assertEquals(BehaviorAndroidSession.Readiness.READY,BehaviorAndroidSession.readiness("a",new double[]{.8,.2},0));
    }
    @Test public void inheritedTimeZeroIsUnlimitedAndFiniteTimeIsExact(){assertEquals(0,BehaviorAndroidSession.deadline(100,0));assertEquals(1800100,BehaviorAndroidSession.deadline(100,30));try{BehaviorAndroidSession.deadline(0,-1);fail();}catch(IllegalArgumentException expected){}}
    @Test public void lateResultAfterCancelCannotConfirmEvenWithCompletedDelivery(){BehaviorAndroidSession session=new BehaviorAndroidSession("task",new BehaviorExample.Replay(),null);session.stage(obs("",10),"action","COMPLETED");session.cancel();try{session.finish(obs("action",20),true);fail();}catch(java.util.concurrent.CancellationException expected){}assertTrue(session.confirmed().all().isEmpty());}
    @Test public void unknownTargetWaitsAndKnownThirdPackageRejects(){assertEquals(BehaviorAndroidSession.TargetReadiness.WAIT_TARGET,BehaviorAndroidSession.targetReadiness("","example.target"));assertEquals(BehaviorAndroidSession.TargetReadiness.CHECK_WINDOW,BehaviorAndroidSession.targetReadiness("example.target","example.target"));assertEquals(BehaviorAndroidSession.TargetReadiness.REJECT_TARGET,BehaviorAndroidSession.targetReadiness("other.target","example.target"));}
    @Test public void offThenOnCannotReviveAnOldUiRequest(){assertTrue(BehaviorAndroidSession.enabledRevision(true,1,1));assertFalse(BehaviorAndroidSession.enabledRevision(false,1,1));assertFalse(BehaviorAndroidSession.enabledRevision(true,1,2));}
}
