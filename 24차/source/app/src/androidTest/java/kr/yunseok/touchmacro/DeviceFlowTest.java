package kr.yunseok.touchmacro;
import android.app.*;
import android.content.*;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class DeviceFlowTest {
 Instrumentation in; MainActivity activity; MacroService svc;
 private void main(Runnable r){in.runOnMainSync(r);}
 private void screen() throws Exception {
  main(()->{try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("testScreen");m.setAccessible(true);m.invoke(activity);}catch(Exception e){throw new RuntimeException(e);}});
  SystemClock.sleep(400);
 }
 private TextView find(View v,String t){if(v instanceof TextView&&((TextView)v).getText().toString().equals(t))return (TextView)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){TextView result=find(g.getChildAt(i),t);if(result!=null)return result;}}return null;}
 private boolean has(String text){final boolean[] result={false};main(()->result[0]=find(activity.getWindow().getDecorView(),text)!=null);return result[0];}
 private Step textStep(String source,String target){Step s=new Step("TEXT_TAP");s.text=target;s.source=source;s.match="EXACT";s.after=100;s.timeout=4000;return s;}
 private Macro macro(Step... steps){Macro m=new Macro();m.startDelay=100;m.maxMinutes=2;for(Step s:steps)m.steps.add(s);return m;}
 private void start(Macro m){main(()->svc.start(m));}
 private void done(){long until=SystemClock.elapsedRealtime()+30000;while(svc.running&&SystemClock.elapsedRealtime()<until)SystemClock.sleep(100);assertFalse("Macro never finished: "+svc.logText(),svc.running);SystemClock.sleep(200);}
 private void completed(){done();assertEquals(svc.logText(),"완료",svc.status);}
 @Test public void realGesturesTextOcrBackRetryPauseAndStop() throws Exception {
  in=InstrumentationRegistry.getInstrumentation();
  UiAutomation automation=in.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
  for(String command:new String[]{"settings put secure enabled_accessibility_services \"\"","settings put secure accessibility_enabled 0","settings put secure enabled_accessibility_services kr.yunseok.touchmacro/kr.yunseok.touchmacro.MacroService","settings put secure accessibility_enabled 1"}){try(android.os.ParcelFileDescriptor fd=automation.executeShellCommand(command);java.io.FileInputStream stream=new java.io.FileInputStream(fd.getFileDescriptor())){byte[] b=new byte[4096];while(stream.read(b)!=-1){}}}

  activity=(MainActivity)in.startActivitySync(new Intent(in.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  long until=SystemClock.elapsedRealtime()+15000;while(MacroService.get()==null&&SystemClock.elapsedRealtime()<until)SystemClock.sleep(100);
  svc=MacroService.get();assertNotNull("Enable accessibility with adb first",svc);
  screen();start(macro(textStep("ACCESS","화면 확인")));completed();assertTrue("Accessibility click did not reach button",has("성공 횟수: 1"));
  screen();start(macro(textStep("OCR","테스트 확인")));completed();assertTrue("Offline Korean OCR did not reach canvas",has("OCR 성공 횟수: 1"));
  screen();final AlertDialog[] dialog={null};main(()->{dialog[0]=new AlertDialog.Builder(activity).setTitle("다른 화면").setMessage("원하는 버튼은 뒤로간 화면에 있습니다").setPositiveButton("닫기",null).create();dialog[0].show();});SystemClock.sleep(300);
  Step recover=textStep("ACCESS","화면 확인");recover.failure="BACK_RETRY";recover.retries=2;recover.timeout=300;recover.retryDelay=400;start(macro(recover));completed();assertFalse(dialog[0].isShowing());assertTrue("Back retry did not find previous screen",has("성공 횟수: 1"));assertTrue(svc.logText().contains("뒤로가기 1/2"));
  screen();Step branch=new Step("TEXT_BRANCH");branch.text="화면 확인";branch.source="ACCESS";branch.match="EXACT";branch.timeout=0;branch.found=3;branch.missing=2;branch.after=100;
  start(macro(branch,new Step("STOP"),textStep("ACCESS","화면 확인")));completed();assertTrue(has("성공 횟수: 1"));
  screen();branch.text="절대로없는단어";branch.found=2;branch.missing=3;start(macro(branch,textStep("ACCESS","화면 확인"),new Step("STOP")));done();assertTrue(has("성공 횟수: 0"));assertEquals("정지",svc.status);
  screen();final Rect[] bounds={null};main(()->{TextView b=find(activity.getWindow().getDecorView(),"화면 확인");Rect r=new Rect();b.getGlobalVisibleRect(r);bounds[0]=r;});
  int[] size=svc.size();Step tap=new Step("TAP");tap.x=bounds[0].exactCenterX()*100/size[0];tap.y=bounds[0].exactCenterY()*100/size[1];tap.after=100;tap.repeat=3;
  start(macro(tap));completed();assertTrue("Repeated coordinates failed",has("성공 횟수: 3"));
  screen();Step longTap=new Step(tap.json());longTap.type="LONG";longTap.duration=600;longTap.repeat=1;start(macro(longTap));completed();assertTrue("Long press failed",has("성공 횟수: 1"));
  screen();Step twice=new Step(tap.json());twice.type="DOUBLE";twice.repeat=1;start(macro(twice));completed();assertTrue("Double tap failed",has("성공 횟수: 2"));
  screen();Macro repeated=macro(textStep("ACCESS","화면 확인"));repeated.rounds=2;repeated.gap=100;start(repeated);completed();assertTrue("Macro loop failed",has("성공 횟수: 2"));
  screen();Macro delayed=macro(textStep("ACCESS","화면 확인"));delayed.startDelay=800;start(delayed);main(()->svc.paused=true);SystemClock.sleep(1100);assertTrue(svc.running);assertTrue("Paused run touched the screen",has("성공 횟수: 0"));main(()->svc.paused=false);completed();assertTrue(has("성공 횟수: 1"));
  screen();Step wait=new Step("WAIT");wait.duration=2000;wait.after=0;start(macro(wait,textStep("ACCESS","화면 확인")));SystemClock.sleep(300);main(svc::stop);SystemClock.sleep(2200);assertFalse(svc.running);assertTrue("Stopped run still touched the screen",has("성공 횟수: 0"));
  screen();final EditText[] input={null};main(()->{LinearLayout box=(LinearLayout)((ViewGroup)activity.findViewById(android.R.id.content)).getChildAt(0);input[0]=new EditText(activity);input[0].setHint("입력 테스트");box.addView(input[0]);input[0].requestFocus();});SystemClock.sleep(300);
  Step type=new Step("INPUT");type.text="윤석 입력 테스트";type.after=100;start(macro(type));completed();assertEquals("윤석 입력 테스트",input[0].getText().toString());
  screen();Macro stored=macro(textStep("ACCESS","화면 확인"));stored.name="기기 저장 테스트";main(()->Store.put(activity,stored));assertTrue(Store.library(activity).containsKey(stored.name));assertEquals(stored.name,Store.load(activity).name);
  Step missing=textStep("ACCESS","절대로없는최종글자");missing.failure="BACK_RETRY";missing.retries=2;missing.timeout=0;missing.retryDelay=400;
  start(macro(missing));done();assertTrue(svc.status.startsWith("중단"));assertTrue(svc.logText().contains("뒤로가기 2/2"));assertTrue(svc.status.contains("글자를 찾지 못했습니다"));
  android.util.Log.i("YUNSEOK_QA","PASS: accessibility, Korean OCR offline, back/recheck, branches, tap-repeat, long, double, macro-loop, pause/resume, stop, input, save/load, retry limit");
 }
}
