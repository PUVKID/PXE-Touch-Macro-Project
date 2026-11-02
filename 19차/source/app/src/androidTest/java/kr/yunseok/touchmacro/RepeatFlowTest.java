package kr.yunseok.touchmacro;
import android.graphics.Rect;import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class) public class RepeatFlowTest {
 GuidedFlowTest h=new GuidedFlowTest();volatile int first=0,second=0;Button other;StringBuilder order=new StringBuilder();
 @Before public void boot()throws Exception{h.boot();}
 @After public void end(){h.end();}
 @Test public void editStepRepeatMeansThisStepThenNextAndWholeMacroRepeatsSeparately(){
  h.fixture();h.main(()->{LinearLayout parent=(LinearLayout)h.entry.getParent();h.entry.setText("A");h.entry.setOnClickListener(v->{first++;order.append('A');});other=new Button(h.a);other.setText("B");other.setOnClickListener(v->{second++;order.append('B');});parent.addView(other,new LinearLayout.LayoutParams(-1,Ui.dp(h.a,70)));});h.in.waitForIdleSync();
  Rect[] a={null},b={null};h.main(()->{a[0]=h.bounds(h.entry);b[0]=h.bounds(other);});int[] size=h.svc.size();
  Step one=new Step("TAP");one.x=a[0].exactCenterX()*100/size[0];one.y=a[0].exactCenterY()*100/size[1];one.duration=70;one.after=20;one.repeat=3;
  Step two=new Step(one.json());two.x=b[0].exactCenterX()*100/size[0];two.y=b[0].exactCenterY()*100/size[1];two.repeat=1;
  Macro m=new Macro();m.name="반복 의미 검증";m.rounds=2;m.startDelay=0;m.gap=0;m.steps.add(one);m.steps.add(two);
  h.main(()->h.svc.start(m));h.waitFor(()->!h.svc.running,15000,"Repeat macro did not finish");assertEquals(6,first);assertEquals(2,second);assertEquals("AAABAAAB",order.toString());assertTrue(h.svc.progressText().contains("완료"));
  android.util.Log.i("PXE_REPEAT_QA","PASS: edited step A repeat=3, B repeat=1, whole macro repeat=2 yields AAABAAAB (A6/B2), completion and next-action text retained");
 }
}
