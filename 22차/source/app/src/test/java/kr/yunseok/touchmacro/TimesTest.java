package kr.yunseok.touchmacro;
import org.junit.Test;
import static org.junit.Assert.*;
public class TimesTest {
    @Test public void secondsAreReadableAndLosslessForSavedTimings(){for(int value:new int[]{0,1,100,300,500,1000,1500,59000,600000})assertEquals(value,Times.millis(Times.seconds(value)));assertEquals("1",Times.seconds(1000));assertEquals("1.5초",Times.label(1500));assertEquals("0.1초",new Step("WAIT").detail());}
    @Test public void minutesAndSecondsAreHumanReadable(){assertEquals(600000,Times.minutesAndSeconds(10,"0"));assertEquals(90250,Times.minutesAndSeconds(1,"30.25"));assertEquals("10분",Times.durationLabel(600000));}
    @Test public void decimalInputUsesSeconds(){assertEquals(1500,Times.millis("1.5"));assertEquals(500,Times.millis(" 0.5 "));assertEquals(10000,Times.millis("10"));}
    @Test(expected=IllegalArgumentException.class) public void rejectsNegative(){Times.millis("-1");}
    @Test(expected=IllegalArgumentException.class) public void rejectsNonNumber(){Times.millis("NaN");}
    @Test(expected=IllegalArgumentException.class) public void rejectsBeyondPrecision(){Times.millis("0.0001");}
    @Test(expected=IllegalArgumentException.class) public void rejectsOverflow(){Times.millis("999999999999999999");}
}
