package kr.yunseok.touchmacro;
import org.junit.Test;import static org.junit.Assert.*;
public class RepeatMeaningTest {
 @Test public void individualRepeatAcceptsOneThroughOneThousand(){Step s=new Step("TAP");s.repeat=1;s.validate(1);s.repeat=1000;s.validate(1);assertEquals(1000,new Step(s.json()).repeat);}
 @Test(expected=IllegalArgumentException.class) public void individualRepeatRejectsZero(){Step s=new Step("TAP");s.repeat=0;s.validate(1);}
 @Test(expected=IllegalArgumentException.class) public void individualRepeatRejectsOneThousandAndOne(){Step s=new Step("TAP");s.repeat=1001;s.validate(1);}
}
