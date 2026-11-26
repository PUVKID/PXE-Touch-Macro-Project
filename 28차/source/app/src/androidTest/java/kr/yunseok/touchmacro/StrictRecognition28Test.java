package kr.yunseok.touchmacro;
import android.os.SystemClock;import androidx.test.ext.junit.runners.AndroidJUnit4;import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class) public class StrictRecognition28Test {
 final GuidedFlowTest h=new GuidedFlowTest();volatile int taps;
 @Before public void boot()throws Exception{h.boot();h.fixture();h.main(()->{h.entry.setAllCaps(false);h.entry.setText("어도비");h.entry.setContentDescription("Adobe");h.entry.setOnClickListener(v->taps++);h.svc.controller().minimize();});}
 @After public void end(){h.end();}
 void verify(String source)throws Exception{
  Macro m=new Macro();m.name="원문 완전 일치";m.startDelay=0;m.gap=0;m.maxMinutes=0;m.rounds=1;Step s=new Step("TEXT_TAP");s.source=source;s.text="Adobe";s.match="CONTAINS";s.ignoreCase=true;s.ignoreSpaces=true;s.waitForever=true;s.timeout=0;s.immediate=true;s.after=0;m.steps.add(s);
  h.main(()->h.svc.start(m));SystemClock.sleep(2500);assertEquals("한글/접근성 설명을 영문으로 터치하면 안 됨",0,taps);assertTrue(h.svc.running);
  h.main(()->h.entry.setText("AdobePro"));SystemClock.sleep(2200);assertEquals("붙은 단어 일부를 터치하면 안 됨",0,taps);
  h.main(()->h.entry.setText("Adobe"));h.waitFor(()->!h.svc.running,12000,"원문 일치 미실행");assertEquals(1,taps);SystemClock.sleep(500);assertEquals(1,taps);
  if("ACCESS".equals(source)){h.main(()->{h.entry.setText("[");s.text="[";s.match="REGEX";h.svc.start(m);});h.waitFor(()->!h.svc.running,12000,"구형 정규식 설정이 리터럴을 거부함");assertEquals(2,taps);}
 }
 @Test public void screenInformationRequiresVisibleWholeText()throws Exception{verify("ACCESS");}
 @Test public void ocrRequiresWholeLineAndRejectsKorean()throws Exception{verify("OCR");}
}
