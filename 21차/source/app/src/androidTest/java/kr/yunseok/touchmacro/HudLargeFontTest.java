package kr.yunseok.touchmacro;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 명시적으로 글꼴 배율 1.3을 적용한 별도 기기 배치에서만 실행합니다. */
@RunWith(AndroidJUnit4.class) public class HudLargeFontTest {
    final RunHudTest checks=new RunHudTest();
    @Before public void boot()throws Exception{checks.boot();}
    @After public void end(){checks.end();}
    @Test public void actualLargeFontThemesRemainReadableAndCompact()throws Exception{
        assertEquals("실제 적용 글꼴 배율",1.3f,checks.h.a.getResources().getConfiguration().fontScale,.001f);
        checks.bothThemesAndLongStoppedMessageStayInsideSmallViewport();
    }
}
