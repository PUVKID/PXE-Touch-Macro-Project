package kr.yunseok.touchmacro;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** 시계 등 부가 원문 변화와 직접 반복 시범의 안정 단서를 구분합니다. */
public class BehaviorAndroidAnchorTest {
    private BehaviorExample.Observation obs(String previous,String... raw){List<BehaviorExample.Token> tokens=new ArrayList<>();for(int i=0;i<raw.length;i++)tokens.add(new BehaviorExample.Token(raw[i],1,i*10+1,50,i*10+9,"ELEMENT"));return new BehaviorExample.Observation(new BehaviorExample.Binding("task","session","example.target",1,0,3,10,100,200),tokens,previous,new double[8]);}
    private BehaviorExample row(String id,String action,String previous,String... raw){return new BehaviorExample(id,obs(previous,raw),action,obs(action,raw),"DEMONSTRATION",null,true,false);}
    @Test public void twoDirectExamplesSupportStableWordsDespiteClockChange(){BehaviorExample.Replay r=new BehaviorExample.Replay().append(row("a","tap","","확인","12:01")).append(row("b","tap","","확인","12:02"));assertEquals("tap",BehaviorAndroidSession.supportedAction(r,"task","example.target",obs("","확인","12:03")));assertNull(BehaviorAndroidSession.supportedAction(r,"task","example.target",obs("","12:03")));}
    @Test public void oneExampleAndWrongPreviousAreUnsupported(){BehaviorExample.Replay r=new BehaviorExample.Replay().append(row("a","tap","","확인"));assertNull(BehaviorAndroidSession.supportedAction(r,"task","example.target",obs("","확인")));r=r.append(row("b","tap","","확인"));assertNull(BehaviorAndroidSession.supportedAction(r,"task","example.target",obs("other","확인")));}
    @Test public void forbiddenWordsAndMultipleSupportingActionsBlockInput(){BehaviorExample.Replay r=new BehaviorExample.Replay().append(row("a","tap","","확인")).append(row("b","tap","","확인")).append(row("c","back","","취소")).append(row("d","back","","취소"));assertEquals("tap",BehaviorAndroidSession.supportedAction(r,"task","example.target",obs("","확인")));assertNull(BehaviorAndroidSession.supportedAction(r,"task","example.target",obs("","확인","취소")));}
    @Test public void sharedNonDiscriminatingAnchorIsNotSupport(){BehaviorExample.Replay r=new BehaviorExample.Replay().append(row("a","tap","","공통")).append(row("b","tap","","공통")).append(row("c","back","","공통")).append(row("d","back","","공통"));assertNull(BehaviorAndroidSession.supportedAction(r,"task","example.target",obs("","공통")));}
}
