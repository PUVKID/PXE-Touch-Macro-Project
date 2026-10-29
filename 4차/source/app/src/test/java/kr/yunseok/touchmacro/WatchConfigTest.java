package kr.yunseok.touchmacro;
import org.junit.Test;
import static org.junit.Assert.*;
public class WatchConfigTest {
    @Test public void defaultIsOneMinutePullDownWithoutTap(){WatchConfig p=new WatchConfig();p.validate();assertEquals(60,p.intervalSeconds);assertEquals(30,p.swipeStartY(),0);assertEquals(75,p.swipeEndY(),0);assertFalse(p.tapFound);assertFalse(p.waitForRefresh);assertEquals(1,p.backCount);}
    @Test public void regionAndMacroSurviveSave(){WatchConfig p=new WatchConfig();p.rule.text="예약 가능";p.rule.left=15;p.rule.top=35;p.rule.right=65;p.rule.bottom=80;p.macroName="내 동작";p.intervalSeconds=90;p.direction="SCROLL_DOWN";p.backCount=2;p.tapFound=true;p.waitForRefresh=true;WatchConfig q=new WatchConfig(p.json());q.validate();assertEquals(p.rule.text,q.rule.text);assertEquals(15,q.rule.left,0);assertEquals(80,q.rule.bottom,0);assertEquals("내 동작",q.macroName);assertEquals(90,q.intervalSeconds);assertEquals(75,q.swipeStartY(),0);assertEquals(30,q.swipeEndY(),0);assertEquals(2,q.backCount);assertTrue(q.tapFound);assertTrue(q.waitForRefresh);}
    @Test(expected=IllegalArgumentException.class) public void rejectsEmptyText(){WatchConfig p=new WatchConfig();p.rule.text="";p.validate();}
    @Test(expected=IllegalArgumentException.class) public void rejectsBackwardsRegion(){WatchConfig p=new WatchConfig();p.rule.left=80;p.rule.right=20;p.validate();}
    @Test(expected=IllegalArgumentException.class) public void rejectsZeroInterval(){WatchConfig p=new WatchConfig();p.intervalSeconds=0;p.validate();}
    @Test(expected=IllegalArgumentException.class) public void rejectsEndlessMacro(){Macro m=new Macro();m.steps.add(new Step("TAP"));m.rounds=0;new WatchConfig().validateMacro(m);}
    @Test(expected=IllegalArgumentException.class) public void rejectsAllDisabled(){Macro m=new Macro();Step s=new Step("TAP");s.enabled=false;m.steps.add(s);new WatchConfig().validateMacro(m);}
}
