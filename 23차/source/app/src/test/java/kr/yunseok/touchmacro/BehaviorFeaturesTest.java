package kr.yunseok.touchmacro;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** 원문 특징과 OCR 계층의 중복·모호성을 검사합니다. */
public class BehaviorFeaturesTest {
    @Test public void featureMovesWithTextAndDoesNotUseCoordinates(){
        BehaviorExample.Observation a=BehaviorExampleTest.observation("확인🙂e\u0301");
        BehaviorExample.Observation b=new BehaviorExample.Observation(a.binding,List.of(new BehaviorExample.Token("확인🙂e\u0301",300,500,600,550,"LINE")),"",new double[8]);
        assertArrayEquals(BehaviorFeatures.encode(a),BehaviorFeatures.encode(b),0);
        assertEquals(584,BehaviorFeatures.encode(a).length);
        assertFalse(Arrays.equals(BehaviorFeatures.encode(a),BehaviorFeatures.encode(BehaviorExampleTest.observation("다른 글자"))));
    }
    @Test public void duplicateLevelsAndSmallestDemonstrationCandidate(){
        var word=new BehaviorExample.Token("확인",10,10,50,30,"ELEMENT");
        var line=new BehaviorExample.Token("확인 버튼",5,5,100,50,"LINE");
        var same=new BehaviorExample.Token("확인",10,10,50,30,"BLOCK");
        var tokens=List.of(line,word,same);
        assertEquals(2,BehaviorFeatures.deduplicate(tokens).size());
        assertEquals("확인",BehaviorFeatures.proposeAt(tokens,20,20).get(0).raw);
        assertEquals(1,BehaviorFeatures.proposeAt(tokens,20,20).size());
        assertEquals(0,BehaviorFeatures.proposeAt(tokens,1000,1000).size());
    }
    @Test public void currentResolutionDoesNotPickFirstSeparatedDuplicate(){
        var a=new BehaviorExample.Token("확인",10,10,50,30,"ELEMENT");
        var nested=new BehaviorExample.Token("확인",5,5,60,40,"LINE");
        assertEquals(10,BehaviorFeatures.resolveCurrent(List.of(a,nested),"확인").left);
        var separate=new BehaviorExample.Token("확인",100,100,150,130,"ELEMENT");
        assertThrows(IllegalArgumentException.class,()->BehaviorFeatures.resolveCurrent(List.of(a,separate),"확인"));
        assertThrows(IllegalArgumentException.class,()->BehaviorFeatures.resolveCurrent(List.of(a),"없는 글자"));
    }
    @Test public void previousHashIsStableAndCollisionsAreReported(){
        assertArrayEquals(BehaviorFeatures.previousAction("id-1"),BehaviorFeatures.previousAction("id-1"),0);
        assertEquals(64,BehaviorFeatures.previousAction("id-1").length);
        assertEquals(1,Arrays.stream(BehaviorFeatures.previousAction("id-1")).filter(v->v!=0).count());
        assertArrayEquals(new double[64],BehaviorFeatures.previousAction(""),0);
        List<String> ids=new ArrayList<>();for(int i=0;i<500;i++)ids.add("id"+i);
        assertTrue(BehaviorFeatures.previousCollisions(ids)>0);
    }
}
