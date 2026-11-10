package kr.yunseok.touchmacro;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** 기존 프로필의 호환성과 진입 확인의 독립 영역·입력 검증을 확인합니다. */
public class WatchEntryConfigTest {
    JSONObject entry(String word)throws Exception{return new Step("TEXT_WAIT").json().put("text",word).put("match","EXACT").put("timeout",3000).put("left",20).put("top",25).put("right",80).put("bottom",75);}
    @Test public void legacyProfileKeepsEntryCheckDisabledWithThreeSecondExactFullScreenDefault()throws Exception{
        WatchConfig p=new WatchConfig(new JSONObject().put("rule",new Step("TEXT_WAIT").json().put("text","목록")));
        JSONObject saved=p.json();assertFalse(saved.optBoolean("entryCheck",true));
        JSONObject rule=saved.optJSONObject("entryRule");assertNotNull("기존 프로필에도 독립 확인 기본값이 필요합니다",rule);
        assertEquals("EXACT",rule.getString("match"));assertEquals(3000,rule.getInt("timeout"));
        assertEquals(0,rule.getDouble("left"),0);assertEquals(100,rule.getDouble("right"),0);
        assertEquals("목록",p.rule.text);assertEquals(15,p.repetitions);
    }
    @Test public void enabledEntryAndOptionalRejectSurviveRoundTripWithoutChangingListRule()throws Exception{
        WatchConfig p=new WatchConfig(new WatchConfig().json().put("entryCheck",true).put("entryRule",entry("배")).put("rejectText","사과"));
        p.rule.text="목록";p.validate();WatchConfig restored=new WatchConfig(p.json());
        assertTrue(restored.json().optBoolean("entryCheck"));assertEquals("사과",restored.json().getString("rejectText"));
        assertEquals("배",restored.json().getJSONObject("entryRule").getString("text"));assertEquals("목록",restored.rule.text);
        assertEquals(0,restored.rule.left,0);assertEquals(20,restored.json().getJSONObject("entryRule").getDouble("left"),0);
    }
    @Test public void priorityAreaCopyKeepsIndependentEntryRegion()throws Exception{
        WatchConfig p=new WatchConfig(new WatchConfig().json().put("entryCheck",true).put("entryRule",entry("배")));
        p.priorityAreas.add(new WatchArea(0,0,100,20));WatchConfig first=p.areaConfig(0);
        assertEquals(20,first.rule.bottom,0);JSONObject region=first.json().getJSONObject("entryRule");
        assertEquals(25,region.getDouble("top"),0);assertEquals(75,region.getDouble("bottom"),0);
    }
    @Test public void enabledEntryRejectsEmptyExpectedBeforeExecution()throws Exception{
        WatchConfig p=new WatchConfig(new WatchConfig().json().put("entryCheck",true).put("entryRule",entry(" ")));
        try{p.validate();fail("빈 확인 글자를 허용했습니다");}catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("글자"));}
    }
    @Test public void enabledEntryRejectsZeroConfirmationTime()throws Exception{
        WatchConfig p=new WatchConfig(new WatchConfig().json().put("entryCheck",true).put("entryRule",entry("배").put("timeout",0)));
        try{p.validate();fail("0초 확인을 허용했습니다");}catch(IllegalArgumentException expected){assertTrue(expected.getMessage().contains("확인"));}
    }
    @Test public void optionalRejectUsesSameValidatedMatchAndRegion()throws Exception{
        WatchConfig p=new WatchConfig(new WatchConfig().json().put("entryCheck",true).put("entryRule",entry("배").put("match","REGEX")).put("rejectText","["));
        try{p.validate();fail("잘못된 되돌아갈 글자 정규식을 허용했습니다");}catch(IllegalArgumentException expected){assertNotNull(expected.getMessage());}
    }
}
