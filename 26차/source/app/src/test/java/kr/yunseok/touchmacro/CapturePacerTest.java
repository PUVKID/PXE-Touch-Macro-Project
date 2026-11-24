package kr.yunseok.touchmacro;

import org.junit.Test;
import static org.junit.Assert.*;

/** 실제 응답 시점의 간격과 제한된 후퇴·성공 회복을 검사합니다. */
public class CapturePacerTest {
    @Test public void normalIntervalDoesNotImposeTwoSeconds(){
        CapturePacer pacer=new CapturePacer(100);
        assertEquals(500,pacer.remaining(100));
        assertEquals(1,pacer.remaining(599));
        assertEquals(0,pacer.remaining(600));
    }
    @Test public void intervalRejectionDoublesAndRemainsBounded(){
        CapturePacer pacer=new CapturePacer(0);
        pacer.rejected(1000);assertEquals(1000,pacer.remaining(1000));
        for(int i=0;i<20;i++)pacer.rejected(2000+i);
        assertEquals(12000,pacer.remaining(2019));
    }
    @Test public void successfulResponsesRecoverWithoutDroppingBelowBase(){
        CapturePacer pacer=new CapturePacer(0);
        for(int i=0;i<10;i++)pacer.rejected(i);
        pacer.responded(1000);assertEquals(6000,pacer.remaining(1000));
        pacer.responded(2000);assertEquals(3000,pacer.remaining(2000));
        pacer.responded(3000);assertEquals(1500,pacer.remaining(3000));
        pacer.responded(4000);assertEquals(750,pacer.remaining(4000));
        pacer.responded(5000);assertEquals(500,pacer.remaining(5000));
        pacer.responded(6000);assertEquals(500,pacer.remaining(6000));
    }
    @Test public void lateResponseMovesTheNextDeadlineForward(){
        CapturePacer pacer=new CapturePacer(0);
        pacer.responded(8000);assertEquals(500,pacer.remaining(8000));
        pacer.responded(7000);assertEquals(500,pacer.remaining(8000));
        assertEquals(0,pacer.remaining(8500));
    }
    @Test public void androidElevenMarginIsPreservedAfterRecovery(){
        CapturePacer pacer=new CapturePacer(0,1100);
        assertEquals(1100,pacer.remaining(0));
        pacer.rejected(2000);assertEquals(2200,pacer.remaining(2000));
        pacer.responded(4000);assertEquals(1100,pacer.remaining(4000));
    }
    @Test public void failureResponseKeepsBackoffAndStillMovesDeadline(){
        CapturePacer pacer=new CapturePacer(0);
        pacer.rejected(1000);pacer.failed(2000);
        assertEquals(1000,pacer.remaining(2000));
        pacer.rejected(3000);assertEquals(2000,pacer.remaining(3000));
        pacer.failed(2500);assertEquals(2000,pacer.remaining(3000));
    }
}
