package kr.yunseok.touchmacro;
import android.os.*;
import android.graphics.Rect;
import android.view.accessibility.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** 실행 교체·일시정지와 실제 이동식 위젯의 제어를 검사합니다. */
@RunWith(AndroidJUnit4.class) public class Control28Test {
 final GuidedFlowTest h=new GuidedFlowTest();
 @Before public void boot()throws Exception{h.boot();h.fixture();h.main(()->Ui.prefs(h.a).edit().putBoolean("hud_expanded",false).putInt("hud_x",30).putInt("hud_y",1500).putInt("hud_opacity",95).putString("word_match","EXACT").apply());}
 @After public void end(){h.end();}
 Macro waiting(int rounds,int duration){Macro m=new Macro();m.name="제어 검증";m.rounds=rounds;m.startDelay=0;m.gap=0;m.maxMinutes=0;Step s=new Step("WAIT");s.duration=duration;s.after=0;m.steps.add(s);return m;}
 AccessibilityNodeInfo desc(AccessibilityNodeInfo node,String value){if(node==null)return null;if(value.contentEquals(node.getContentDescription()==null?"":node.getContentDescription()))return AccessibilityNodeInfo.obtain(node);for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i),found=desc(child,value);if(child!=null)child.recycle();if(found!=null)return found;}return null;}
 AccessibilityNodeInfo desc(String value){for(AccessibilityWindowInfo w:h.auto.getWindows()){AccessibilityNodeInfo root=w.getRoot(),found=desc(root,value);if(root!=null)root.recycle();if(found!=null)return found;}return null;}
 void control(String name){h.waitFor(()->{AccessibilityNodeInfo n=desc(name);if(n==null)return false;n.recycle();return true;},4000,"제어 없음 "+name);AccessibilityNodeInfo n=desc(name);assertTrue(name,n.performAction(AccessibilityNodeInfo.ACTION_CLICK));n.recycle();h.in.waitForIdleSync();}
 @Test public void validRestartReplacesPausedExecutionAndInvalidSettingsPreserveIt()throws Exception{
  h.main(()->h.svc.start(waiting(15,60000)));h.waitFor(()->"1/15".equals(h.svc.compactCounter()),4000,"초기 반복 수");h.main(()->h.svc.togglePause());assertTrue(h.svc.paused);
  h.main(()->{try{WatchConfig bad=new WatchConfig();bad.rule.text="";h.svc.restartWatch(bad);fail("잘못된 탐지 설정 허용");}catch(IllegalArgumentException expected){}assertTrue(h.svc.running);assertTrue(h.svc.paused);});
  h.main(()->h.svc.restart(waiting(2,120)));h.waitFor(()->!h.svc.running,6000,"새 실행 완료");assertFalse(h.svc.paused);assertEquals("2/2",h.svc.compactCounter());SystemClock.sleep(400);assertFalse(h.svc.running);
 }
 @Test public void newWordStartsWithoutPressingStop()throws Exception{
  AtomicInteger taps=new AtomicInteger();h.main(()->{h.entry.setText("새 글자");h.entry.setOnClickListener(v->taps.incrementAndGet());});h.in.waitForIdleSync();
  Macro old=new Macro();old.name="이전 글자 대기";old.startDelay=0;old.maxMinutes=0;Step s=new Step("TEXT_TAP");s.text="이전 글자";s.source="ACCESS";s.waitForever=true;s.timeout=0;s.immediate=true;s.after=0;old.steps.add(s);
  h.waitFor(()->h.find("새 글자")!=null,4000,"실제 새 글자 fixture 없음");
  h.main(()->h.svc.start(old));h.waitFor(()->h.svc.running,4000,"이전 실행 시작");SystemClock.sleep(350);

  h.main(()->{Ui.prefs(h.a).edit().putString("word","새 글자").putString("word_source","ACCESS").apply();Macro next=new Macro();next.name="새 글자 실행";next.startDelay=0;Step word=FloatingController.wordStep(h.a);word.immediate=true;word.after=0;next.steps.add(word);h.svc.restart(next);});
  h.waitFor(()->taps.get()==1&&!h.svc.running,10000,"새 글자 실행 실패");SystemClock.sleep(500);assertEquals("이전 실행의 늦은 터치",1,taps.get());
 }
 @Test public void compactHudMovesExpandsPausesAndDoesNotBlockOutsideTouch()throws Exception{
  AtomicInteger touches=new AtomicInteger();h.main(()->{h.entry.setOnClickListener(v->touches.incrementAndGet());h.svc.start(waiting(15,60000));});h.waitFor(()->h.find("1/15")!=null,5000,"작은 반복 위젯 없음");h.screenshot("28_run_compact.png");
  AccessibilityNodeInfo count=h.find("1/15");Rect before=new Rect();count.getBoundsInScreen(before);count.recycle();h.drag(before.exactCenterX(),before.exactCenterY(),before.exactCenterX()+110,before.exactCenterY()-180);h.waitFor(()->Ui.prefs(h.a).getInt("hud_x",30)!=30,3000,"위젯 위치 저장 안 됨");
  Rect[] target={null};h.main(()->target[0]=h.bounds(h.entry));h.tap(target[0].exactCenterX(),target[0].exactCenterY());h.waitFor(()->touches.get()==1,3000,"위젯 바깥 터치 차단");
  control("진행 상세 펼치기");assertTrue(Ui.prefs(h.a).getBoolean("hud_expanded",false));h.screenshot("28_run_expanded.png");
  control("진행 창 일시정지");assertTrue(h.svc.paused);control("진행 창 계속 실행");assertFalse(h.svc.paused);control("진행 상세 접기");assertFalse(Ui.prefs(h.a).getBoolean("hud_expanded",true));
  control("진행 상세 펼치기");control("진행 창 정지");h.waitFor(()->!h.svc.running,2000,"위젯 정지 실패");
 }
 @Test public void macroTapPassesThroughHudAtSamePosition()throws Exception{
  AtomicInteger touches=new AtomicInteger();h.main(()->{h.entry.setOnClickListener(v->touches.incrementAndGet());h.svc.start(waiting(15,60000));});h.waitFor(()->h.find("1/15")!=null,5000,"진행 위젯 없음");
  Rect[] button={null};h.main(()->button[0]=h.bounds(h.entry));AccessibilityNodeInfo counter=h.find("1/15");Rect start=new Rect();counter.getBoundsInScreen(start);counter.recycle();h.drag(start.exactCenterX(),start.exactCenterY(),button[0].exactCenterX(),button[0].exactCenterY());SystemClock.sleep(300);
  int[] size=h.svc.size();Macro m=new Macro();m.startDelay=0;m.maxMinutes=0;Step tap=new Step("TAP");tap.x=button[0].exactCenterX()*100.0/size[0];tap.y=button[0].exactCenterY()*100.0/size[1];tap.duration=70;tap.after=0;m.steps.add(tap);h.main(()->h.svc.restart(m));h.waitFor(()->touches.get()==1&&!h.svc.running,6000,"위젯이 자동 터치를 가로챔");assertEquals(1,touches.get());
 }
 @Test public void rotatingScreenKeepsHudInsideAndStopsExecution()throws Exception{
  h.main(()->h.svc.start(waiting(15,60000)));h.waitFor(()->h.find("1/15")!=null,5000,"진행 위젯 없음");
  try{assertTrue(h.auto.setRotation(android.app.UiAutomation.ROTATION_FREEZE_90));h.waitFor(()->h.svc.size()[0]>h.svc.size()[1],6000,"가로 화면 전환 실패");h.waitFor(()->!h.svc.running,3000,"회전 후 정지 실패");h.waitFor(()->{AccessibilityNodeInfo arrow=desc("진행 상세 펼치기");if(arrow==null)return false;Rect bounds=new Rect();arrow.getBoundsInScreen(bounds);arrow.recycle();int[] size=h.svc.size();return bounds.top>=0&&bounds.bottom<=size[1]&&bounds.left>=0&&bounds.right<=size[0];},6000,"회전 후 위젯이 화면 밖에 있음");h.screenshot("28_run_landscape.png");}
  finally{h.auto.setRotation(android.app.UiAutomation.ROTATION_FREEZE_0);h.auto.setRotation(android.app.UiAutomation.ROTATION_UNFREEZE);}
 }
 AccessibilityNodeInfo input(AccessibilityNodeInfo n){if(n==null)return null;if("android.widget.EditText".contentEquals(n.getClassName()))return AccessibilityNodeInfo.obtain(n);for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo child=n.getChild(i),found=input(child);if(child!=null)child.recycle();if(found!=null)return found;}return null;}
 @Test public void invalidWordDoesNotOverwriteSettingsAndCancelResumes()throws Exception{
  h.main(()->{Ui.prefs(h.a).edit().putString("word","원래글자").putString("word_match","REGEX").apply();h.svc.start(waiting(15,60000));});h.waitFor(()->"1/15".equals(h.svc.compactCounter()),4000,"기존 실행 대기");long revision=h.svc.runRevision();h.main(()->h.svc.controller().wordDialog());assertTrue(h.svc.paused);
  h.waitFor(()->{for(AccessibilityWindowInfo window:h.auto.getWindows()){AccessibilityNodeInfo root=window.getRoot(),found=input(root);if(root!=null)root.recycle();if(found!=null){found.recycle();return true;}}return false;},4000,"글자 입력창 준비 시간 초과");
  AccessibilityNodeInfo field=null;for(AccessibilityWindowInfo window:h.auto.getWindows()){AccessibilityNodeInfo root=window.getRoot();field=input(root);if(root!=null)root.recycle();if(field!=null)break;}assertNotNull("글자 입력창 없음",field);Bundle value=new Bundle();value.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"[");assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,value));field.recycle();h.click("시작");assertEquals("원래글자",Ui.prefs(h.a).getString("word",""));assertEquals(revision,h.svc.runRevision());assertTrue(h.svc.running);h.click("취소");assertFalse(h.svc.paused);assertEquals(revision,h.svc.runRevision());
 }

}
