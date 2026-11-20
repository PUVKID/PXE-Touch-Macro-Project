package kr.yunseok.touchmacro;
import org.junit.Test;
import static org.junit.Assert.*;

/** 완료 횟수와 진행 중인 회차가 서로 혼동되지 않는 실제 표시를 검사합니다. */
public class RunProgressTest {
    @Test public void startedRoundHasNotAlreadyCompleted(){
        RunProgress p=new RunProgress(15,8,7,2,4,"글자 찾아 터치");
        assertEquals("7/15회 완료 · 8회째",p.counter());
        assertEquals("동작 2/4 · 글자 찾아 터치",p.stage());
    }
    @Test public void finalCompletionDoesNotAdvertiseAnotherRound(){
        assertEquals("15/15회 완료",new RunProgress(15,0,15,0,4,"").counter());
    }
    @Test public void freshEntryStartsAtZero(){
        assertEquals("0/15회 완료",new RunProgress(15,0,0,0,4,"").counter());
    }
    @Test public void unlimitedRunHasUnboundedDenominator(){
        assertEquals("21/∞회 완료 · 22회째",new RunProgress(0,22,21,1,2,"터치").counter());
    }
}
