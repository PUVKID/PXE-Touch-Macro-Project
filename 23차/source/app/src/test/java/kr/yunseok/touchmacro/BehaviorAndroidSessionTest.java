package kr.yunseok.touchmacro;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** Android 허가·정답 확정 경계를 신경 학습 없이 검사합니다. */
public class BehaviorAndroidSessionTest {
    private BehaviorExample.Observation observation(String previous,String raw,long time){
        return new BehaviorExample.Observation(new BehaviorExample.Binding("task","session","example.target",1,0,3,time,100,200),List.of(new BehaviorExample.Token(raw,1,1,30,20,"LINE")),previous,new double[8]);
    }
    private BehaviorExample row(String id,String action,String raw,String previous){return new BehaviorExample(id,observation(previous,raw,10),action,observation(action,raw,20),"DEMONSTRATION",null,true,false);}
    @Test public void unseenAndConflictingContextsNeverAuthorize(){
        BehaviorExample.Replay replay=new BehaviorExample.Replay().append(row("a","one","확인","")).append(row("a2","one","확인",""));
        assertEquals("one",BehaviorAndroidSession.supportedAction(replay,"task","example.target",observation("","확인",30)));
        assertNull(BehaviorAndroidSession.supportedAction(replay,"task","example.target",observation("","새 글자",30)));
        assertNull(BehaviorAndroidSession.supportedAction(replay.append(row("b","two","확인","")),"task","example.target",observation("","확인",30)));
        assertNull(BehaviorAndroidSession.supportedAction(replay,"task","example.target",observation("one","확인",30)));
    }
    @Test public void probabilityThresholdsAreInclusiveAndDoNotReplaceSupport(){
        assertTrue(BehaviorAndroidSession.confident(new double[]{.80,.20},0));
        assertFalse(BehaviorAndroidSession.confident(new double[]{.79,.21},0));
        assertFalse(BehaviorAndroidSession.confident(new double[]{.90,.10},1));
        assertFalse(BehaviorAndroidSession.confident(new double[]{Double.NaN,.1},0));
    }
    @Test public void allLiveRevisionsAndPauseInvalidatePermit(){
        BehaviorAndroidSession.Stamp a=new BehaviorAndroidSession.Stamp(1,2,3,4,5,6,"example.target",true,false);
        assertTrue(a.matches(new BehaviorAndroidSession.Stamp(1,2,3,4,5,6,"example.target",true,false)));
        assertFalse(a.matches(new BehaviorAndroidSession.Stamp(2,2,3,4,5,6,"example.target",true,false)));
        assertFalse(a.matches(new BehaviorAndroidSession.Stamp(1,3,3,4,5,6,"example.target",true,false)));
        assertFalse(a.matches(new BehaviorAndroidSession.Stamp(1,2,4,4,5,6,"example.target",true,false)));
        assertFalse(a.matches(new BehaviorAndroidSession.Stamp(1,2,3,5,5,6,"example.target",true,false)));
        assertFalse(a.matches(new BehaviorAndroidSession.Stamp(1,2,3,4,6,6,"example.target",true,false)));
        assertFalse(a.matches(new BehaviorAndroidSession.Stamp(1,2,3,4,5,7,"example.target",true,false)));
        assertFalse(a.matches(new BehaviorAndroidSession.Stamp(1,2,3,4,5,6,"other.target",true,false)));
        assertFalse(a.matches(new BehaviorAndroidSession.Stamp(1,2,3,4,5,6,"example.target",false,false)));
        assertFalse(a.matches(new BehaviorAndroidSession.Stamp(1,2,3,4,5,6,"example.target",true,true)));
    }
    @Test public void completedDeliveryIsNotConfirmedGoldAndNextBeforeBecomesAfter(){
        BehaviorAndroidSession s=new BehaviorAndroidSession("task",new BehaviorExample.Replay(),null);
        s.stage(observation("","확인",10),"tap","COMPLETED");
        assertEquals(0,s.confirmed().all().size());
        s.observeBefore(observation("tap","다음",40));
        assertEquals(0,s.confirmed().all().size());
        BehaviorExample.Replay result=s.finish(observation("tap","완료",50),true);
        assertEquals(1,result.active("task").size());assertEquals(40,result.all().get(0).after.binding.capturedAtMs);
    }
    @Test public void cancelAndRejectedDeliveryProduceNoGold(){
        BehaviorAndroidSession s=new BehaviorAndroidSession("task",new BehaviorExample.Replay(),null);
        s.stage(observation("","확인",10),"tap","CANCELLED");
        assertEquals(0,s.finish(observation("","확인",20),true).all().size());
        s.cancel();try{s.stage(observation("","확인",30),"tap","COMPLETED");fail();}catch(java.util.concurrent.CancellationException expected){}
    }
}
