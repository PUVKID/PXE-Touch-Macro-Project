package kr.yunseok.touchmacro;
import org.junit.Test;
import static org.junit.Assert.*;
/** 실제 미구현 기능을 먼저 확인하는 계약 검사입니다. */
public class AiFeatureContractTest {
    private void present(String name){try{Class.forName("kr.yunseok.touchmacro."+name);}catch(ClassNotFoundException error){fail(name+" 구현이 없습니다");}}
    @Test public void tokenizerContractExists(){present("AiTokenizer");}
    @Test public void completePlanCompilerExists(){present("AiPlanCompiler");}
    @Test public void cancellableSessionExists(){present("AiPlanningSession");}
}
