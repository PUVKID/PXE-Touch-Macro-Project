package kr.yunseok.touchmacro;

import org.junit.Test;
import java.lang.reflect.InvocationTargetException;
import static org.junit.Assert.*;

public class AiNumbersTest {
    private long call(String method, Object... arguments) throws Exception {
        Class<?> type;
        try { type=Class.forName("kr.yunseok.touchmacro.AiNumbers"); }
        catch(ClassNotFoundException error) { fail("AI 숫자 계약 구현이 없습니다"); return -1; }
        try {
            if(arguments.length==1)return ((Number)type.getMethod(method,String.class).invoke(null,arguments)).longValue();
            return ((Number)type.getMethod(method,String.class,String.class).invoke(null,arguments)).longValue();
        } catch(InvocationTargetException error) { throw (Exception)error.getCause(); }
    }
    @Test public void nativeAndSinoIntegersAreExact() throws Exception {
        String[] names={"한","두","세","열다섯","스물","스무","스물 다섯","십오","이십삼","천삼백이십일","십만","일억"," 000015 "};
        int[] expected={1,2,3,15,20,20,25,15,23,1321,100000,100000000,15};
        for(int i=0;i<names.length;i++)assertEquals(names[i],expected[i],call("integer",names[i]));
    }
    @Test public void durationsUseExactDecimalMilliseconds() throws Exception {
        assertEquals(1500,call("duration","1.5","SEC"));
        assertEquals(1,call("duration","0.001","SEC"));
        assertEquals(75000,call("duration","1.25","MIN"));
        assertEquals(3600000,call("duration","한","HOUR"));
        assertEquals(86400000,call("duration","24","HOUR"));
    }
    @Test public void malformedNumbersAndSubMillisecondsAreRejected() throws Exception {
        for(String text:new String[]{""," ","-1","+1","NaN","Inf","1.5","일일","십백","이이십","영십","스무하나","123abc","1\t","100000001"}) {
            try { call("integer",text); fail("잘못된 정수를 수락함: "+text); } catch(IllegalArgumentException expected) { }
        }
        for(String[] pair:new String[][]{{"0.0001","SEC"},{"1.5","MS"},{"-1","MIN"},{"25","HOUR"},{"1","UNKNOWN"}}) {
            try { call("duration",pair[0],pair[1]); fail("잘못된 시간을 수락함"); } catch(IllegalArgumentException expected) { }
        }
    }
}
