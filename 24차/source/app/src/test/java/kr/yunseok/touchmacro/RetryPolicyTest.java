package kr.yunseok.touchmacro;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** 새 미발견 정책의 기본값·저장 호환과 실행 전 참조 검증을 확인합니다. */
public class RetryPolicyTest {
    Step text(String failure)throws Exception{return new Step(new JSONObject().put("type","TEXT_TAP").put("text","대상").put("failure",failure));}
    Macro macro(Step step,Step recovery){Macro m=new Macro();m.steps.add(step);if(recovery!=null)m.steps.add(recovery);return m;}
    void rejected(Macro m){try{m.validate();fail("잘못된 복구 설정이 허용됐습니다");}catch(IllegalArgumentException expected){}}
    @Test public void oldDefaultsAndFiniteZeroKeepMeaning()throws Exception{Step s=text("RETRY");s.retries=0;Macro m=macro(s,null);m.validate();assertFalse(s.json().optBoolean("retryForever",false));assertEquals(0,s.json().optInt("recoveryTarget",0));assertEquals(3,text("BACK_RETRY").retries);}
    @Test public void infiniteAndActionSelectionRoundTrip()throws Exception{JSONObject o=text("ACTION_RETRY").json().put("retryForever",true).put("recoveryTarget",2);Step s=new Step(o);JSONObject stored=new Step(s.json()).json();assertTrue(stored.optBoolean("retryForever",false));assertEquals(2,stored.optInt("recoveryTarget",0));macro(s,new Step("WAIT")).validate();}
    @Test public void infiniteRetryHasNoHiddenWatchDeadline()throws Exception{Step s=new Step(text("RETRY").json().put("retryForever",true));Macro m=macro(s,null);m.maxMinutes=0;assertEquals(0,m.watchDeadline(1000,0));assertEquals(5000,m.watchDeadline(1000,5000));m.maxMinutes=1;assertEquals(61000,m.watchDeadline(1000,0));}
    @Test public void newInfiniteAndLegacyWaitCannotBothBeEnabled()throws Exception{rejected(macro(new Step(text("RETRY").json().put("retryForever",true).put("waitForever",true)),null));}
    @Test public void actionTargetMustBeEnabledExistingOrdinaryStep()throws Exception{Step s=new Step(text("ACTION_RETRY").json().put("recoveryTarget",2));macro(s,new Step("WAIT")).validate();Step disabled=new Step("WAIT");disabled.enabled=false;rejected(macro(s,disabled));rejected(macro(s,null));for(String t:new String[]{"TEXT_WAIT","GOTO","STOP"}){Step bad=new Step(t);bad.text="대상";rejected(macro(s,bad));}}
    @Test public void pullRefreshPolicyAndFiniteValuesRoundTrip()throws Exception{Step s=text("PULL_RETRY");s.retries=1;s.retryDelay=300;macro(s,null).validate();assertEquals("PULL_RETRY",new Step(s.json()).failure);assertEquals(1,new Step(s.json()).retries);}
    @Test public void infiniteOnlyAppliesToRetryPolicies()throws Exception{for(String failure:new String[]{"STOP","SKIP"})rejected(macro(new Step(text(failure).json().put("retryForever",true)),null));}
}
