package kr.yunseok.touchmacro;
import org.junit.Test;
import static org.junit.Assert.*;
public class ScrollMotionTest {
    @Test public void automaticScrollIsSlowerAndHalfThePreviousTravel(){assertEquals(900,ScrollMotion.DURATION_MS);assertEquals(22.5,ScrollMotion.START_Y-ScrollMotion.END_Y,0);WatchConfig p=new WatchConfig();p.direction="SCROLL_DOWN";assertEquals(75,p.swipeStartY(),0);assertEquals(52.5,p.swipeEndY(),0);}
    @Test public void pullRefreshTravelIsPreserved(){WatchConfig p=new WatchConfig();assertEquals(30,p.swipeStartY(),0);assertEquals(75,p.swipeEndY(),0);}
}
