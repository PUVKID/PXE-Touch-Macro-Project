package kr.yunseok.touchmacro;
import org.junit.Test;
import static org.junit.Assert.*;

/** 이전 저장 형식에서도 위로 쓸어올리는 자동 새로고침 거리를 절반으로 적용합니다. */
public class ShortRefreshTest {
    @Test public void savedScrollDownProfileUsesHalfDistance(){WatchConfig saved=new WatchConfig();saved.direction="SCROLL_DOWN";WatchConfig restored=new WatchConfig(saved.json());assertEquals(75,restored.swipeStartY(),0);assertEquals(52.5,restored.swipeEndY(),0);assertEquals(22.5,restored.swipeStartY()-restored.swipeEndY(),0);}
    @Test public void pullDownRefreshKeepsOriginalCoordinates(){WatchConfig p=new WatchConfig();assertEquals(30,p.swipeStartY(),0);assertEquals(75,p.swipeEndY(),0);}
}
