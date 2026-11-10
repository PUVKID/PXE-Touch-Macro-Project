package kr.yunseok.touchmacro;

import org.json.JSONObject;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** 직접 시범의 원문·형식과 교정 관계를 검사합니다. */
public class BehaviorExampleTest {
    static BehaviorExample.Binding binding(){return new BehaviorExample.Binding("task","session","demo.app",1,1,1,100,1080,1920);}
    static BehaviorExample.Observation observation(String text){return new BehaviorExample.Observation(binding(),List.of(new BehaviorExample.Token(text,10,20,100,60,"ELEMENT")),"",new double[8]);}
    static BehaviorExample.Registry registry(){return new BehaviorExample.Registry().append(new BehaviorExample.Action("tap",BehaviorExample.Type.TEXT_TAP,"다음",0,false)).append(new BehaviorExample.Action("back",BehaviorExample.Type.BACK,null,0,false)).append(new BehaviorExample.Action("finish",BehaviorExample.Type.FINISH,null,0,false));}
    static BehaviorExample example(String id,String text,String action,String supersedes){return new BehaviorExample(id,observation(text),action,observation("결과"),supersedes==null?"DEMONSTRATION":"CORRECTION",supersedes,true,false);}
    static BehaviorExample.Replay replay(){BehaviorExample.Replay replay=new BehaviorExample.Replay();String[] texts={"가","나","다","라","마","바","사","아"};for(int i=0;i<texts.length;i++)replay=replay.append(example("e"+i,texts[i],i<4?"tap":i<7?"back":"finish",null));return replay;}

    @Test public void actionTypesAndStrictPayloads(){
        assertEquals(6,BehaviorExample.Type.values().length);
        for(BehaviorExample.Type type:BehaviorExample.Type.values()){
            String raw=type==BehaviorExample.Type.TEXT_TAP||type==BehaviorExample.Type.WAIT_TEXT?"확인🙂e\u0301":null;
            BehaviorExample.Action a=new BehaviorExample.Action("id",type,raw,0,false);
            assertEquals(raw,BehaviorExample.Action.fromJson(a.json()).targetRaw);
        }
        assertThrows(IllegalArgumentException.class,()->new BehaviorExample.Action("x",BehaviorExample.Type.TEXT_TAP," ",0,false));
        assertThrows(IllegalArgumentException.class,()->new BehaviorExample.Action("x",BehaviorExample.Type.BACK,"글자",0,false));
        for(Object bad:new Object[]{true,1.0,"1"}){
            JSONObject value=new BehaviorExample.Action("wait",BehaviorExample.Type.WAIT_TEXT,"글자",10,false).json();value.put("timeout_ms",bad);
            assertThrows(IllegalArgumentException.class,()->BehaviorExample.Action.fromJson(value));
        }
        JSONObject extra=new BehaviorExample.Action("back",BehaviorExample.Type.BACK,null,0,false).json().put("x",50);
        assertThrows(IllegalArgumentException.class,()->BehaviorExample.Action.fromJson(extra));
    }
    @Test public void dynamicDictionaryOrderAndLimit(){
        BehaviorExample.Registry r=new BehaviorExample.Registry();for(int i=0;i<500;i++)r=r.append(new BehaviorExample.Action("id"+i,BehaviorExample.Type.TEXT_TAP,"글자"+i,0,false));
        assertEquals(500,r.size());assertEquals("id10",BehaviorExample.Registry.fromJson(r.json()).get(10).id);
        BehaviorExample.Registry full=r;assertThrows(IllegalArgumentException.class,()->full.append(new BehaviorExample.Action("new",BehaviorExample.Type.BACK,null,0,false)));
        assertThrows(IllegalArgumentException.class,()->registry().append(registry().get(0)));
        org.json.JSONArray reordered=registry().json();reordered.getJSONObject(0).put("index",1);
        assertThrows(IllegalArgumentException.class,()->BehaviorExample.Registry.fromJson(reordered));
    }
    @Test public void correctionKeepsProvenanceAndRejectsUnknownParents(){
        BehaviorExample.Replay original=replay();BehaviorExample.Replay corrected=original.append(example("c0","가","back","e0"));
        assertEquals(8,original.active("task").size());assertEquals(9,corrected.all().size());assertEquals(8,corrected.active("task").size());
        assertEquals("back",corrected.active("task").stream().filter(e->e.id.equals("c0")).findFirst().get().actionId);
        assertThrows(IllegalArgumentException.class,()->original.append(example("bad","가","back","missing")));
        assertThrows(IllegalArgumentException.class,()->original.append(example("e0","가","back",null)));
        assertThrows(IllegalArgumentException.class,()->corrected.append(example("conflict","가","tap","e0")));
        assertEquals("가",BehaviorExample.fromJson(corrected.all().get(8).json()).before.tokens.get(0).raw);
    }
    @Test public void rawUnicodeAndImmutableObservation(){
        String raw="🙂e\u0301\n\u0085\\\"정지해";BehaviorExample.Observation value=observation(raw);
        assertEquals(raw,BehaviorExample.Observation.fromJson(value.json()).tokens.get(0).raw);
        assertThrows(IllegalArgumentException.class,()->observation("\ud800"));
        assertThrows(UnsupportedOperationException.class,()->value.tokens.clear());
        double[] state=new double[8];BehaviorExample.Observation copied=new BehaviorExample.Observation(binding(),value.tokens,"",state);state[0]=1;
        assertEquals(0,copied.state()[0],0);double[] exported=copied.state();exported[0]=1;assertEquals(0,copied.state()[0],0);
    }
    @Test public void serializationJsonExceptionRetainsCause(){
        IllegalArgumentException failure=assertThrows(IllegalArgumentException.class,()->BehaviorExample.object("bad",Double.NaN));
        assertTrue(failure.getCause() instanceof org.json.JSONException);
    }
}
