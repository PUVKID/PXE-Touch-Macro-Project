package kr.yunseok.touchmacro;
import android.content.*;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;

/** 실제 메인 화면의 제어·재시작 연결과 확대 글꼴 배치를 확인합니다. */
@RunWith(AndroidJUnit4.class) public class MainControls28Test {
 final GuidedFlowTest h=new GuidedFlowTest();float originalScale;
 @Before public void boot()throws Exception{h.boot();originalScale=h.a.getResources().getConfiguration().fontScale;h.main(()->SetupFlow.finished(h.a));}
 @After public void end()throws Exception{h.end();h.shell("settings put system font_scale "+originalScale);}
 void invoke(String name,Class<?>[] types,Object... args){try{Method method=MainActivity.class.getDeclaredMethod(name,types);method.setAccessible(true);method.invoke(h.a,args);}catch(Exception error){throw new AssertionError(error);}}
 void render(){invoke("render",new Class<?>[]{});}
 View named(View view,String label){if(android.text.TextUtils.equals(label,view.getContentDescription()))return view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=named(group.getChildAt(i),label);if(found!=null)return found;}}return null;}
 Button button(View view,String label){if(view instanceof Button&&android.text.TextUtils.equals(label,((Button)view).getText()))return (Button)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){Button found=button(group.getChildAt(i),label);if(found!=null)return found;}}return null;}
 void nav(String name){h.main(()->{View target=named(h.a.getWindow().getDecorView(),name);assertNotNull(name,target);assertTrue(target.performClick());});h.in.waitForIdleSync();}
 void click(String label){h.main(()->{Button target=button(h.a.getWindow().getDecorView(),label);assertNotNull(label,target);assertTrue(target.isShown());assertTrue(target.performClick());});h.in.waitForIdleSync();}
 Macro waiting(){Macro m=new Macro();m.name="메인 제어 검사";m.startDelay=0;Step wait=new Step("WAIT");wait.duration=600000;wait.after=0;m.steps.add(wait);return m;}
 long generation(){try{Field field=MacroService.class.getDeclaredField("generation");field.setAccessible(true);return ((AtomicLong)field.get(h.svc)).get();}catch(Exception error){throw new AssertionError(error);}}
 MainActivity launch(){return (MainActivity)h.in.startActivitySync(new Intent(h.in.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));}
 @Test public void everyTabShowsPauseContinueAndStop()throws Exception{
  h.main(()->h.svc.restart(waiting()));h.a=launch();h.in.waitForIdleSync();assertTrue(h.svc.running);assertTrue("편집 화면 진입은 자동 일시정지",h.svc.paused);
  for(String tab:new String[]{"탐지","동작","글자","설정"}){
   nav(tab);h.main(()->{Button resume=button(h.a.getWindow().getDecorView(),"계속 실행");Button stop=button(h.a.getWindow().getDecorView(),"정지");assertNotNull(tab,resume);assertNotNull(tab,stop);assertTrue(resume.isShown());assertTrue(stop.isShown());});
  }
  h.screenshot("28_main_paused_all_tabs.png");click("계속 실행");assertFalse(h.svc.paused);assertTrue(h.svc.running);click("일시정지");assertTrue(h.svc.paused);h.screenshot("28_main_paused_controls.png");click("정지");assertFalse(h.svc.running);
 }
 @Test public void changingWordStartsNewGenerationWithoutManualStop()throws Exception{
  h.main(()->{h.svc.restart(waiting());h.svc.togglePause();Ui.prefs(h.a).edit().putString("word","변경한 글자").putString("word_source","ACCESS").apply();});
  long previous=generation();h.main(()->{invoke("quickWord",new Class<?>[]{boolean.class},false);h.svc.togglePause();});
  assertTrue("새 실행 세대 생성",generation()>previous);assertTrue(h.svc.running);assertTrue("검사 중 실제 터치 방지",h.svc.paused);assertEquals("변경한 글자",FloatingController.wordStep(h.a).text);
  // 이 검사는 메인 호출 연결·새 세대·새 입력을 확인하며 실제 OCR 성공 검사와 구분합니다.
 }
 int assertButtonLayouts(View view){int count=0;if(view instanceof Button&&view.isShown()&&view.getWidth()>0){Button b=(Button)view;assertNotNull(b.getText().toString(),b.getLayout());assertTrue("최소 조작 높이: "+b.getText(),b.getHeight()>=Ui.dp(h.a,48));assertTrue("버튼 글자 높이: "+b.getText(),b.getLayout().getHeight()<=b.getHeight()-b.getCompoundPaddingTop()-b.getCompoundPaddingBottom());for(int i=0;i<b.getLayout().getLineCount();i++)assertEquals("말줄임 없음: "+b.getText(),0,b.getLayout().getEllipsisCount(i));String complete=b.getText().toString().replaceAll("\\s+$","");int visibleLines=Math.min(b.getLayout().getLineCount(),b.getMaxLines());if(!complete.isEmpty()){assertTrue("표시 줄 존재: "+b.getText(),visibleLines>0);assertTrue("마지막 글자까지 표시: "+b.getText(),b.getLayout().getLineEnd(visibleLines-1)>=complete.length());}count++;}if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)count+=assertButtonLayouts(group.getChildAt(i));}return count;}
 @Test public void enlargedKoreanTextWrapsInBothThemes()throws Exception{
  h.shell("settings put system font_scale 1.3");SystemClock.sleep(500);
  for(int mode=1;mode<=2;mode++){
   final int selected=mode;h.main(()->{Ui.prefs(h.a).edit().putInt("theme",selected).apply();WatchConfig config=WatchStore.load(h.a);config.rule.text="긴 한글 문구도 잘리지 않고 화면 너비에 맞춰 여러 줄로 읽을 수 있는 탐지 글자";WatchStore.save(h.a,config);});
   h.a=launch();h.in.waitForIdleSync();assertEquals(1.3f,h.a.getResources().getConfiguration().fontScale,.05f);
   h.main(()->{View target=named(h.a.getWindow().getDecorView(),"현재 탐지할 글자");assertNotNull(target);TextView word=(TextView)target;assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT,word.getLayoutParams().height);assertTrue("긴 탐지 글자 줄바꿈",word.getLineCount()>=2);assertTrue(word.getLayout().getHeight()<=word.getHeight()-word.getCompoundPaddingTop()-word.getCompoundPaddingBottom());assertTrue(assertButtonLayouts(h.a.getWindow().getDecorView())>=3);});
   h.screenshot(mode==1?"28_main_light_130_watch.png":"28_main_dark_130_watch.png");nav("글자");h.main(()->assertTrue(assertButtonLayouts(h.a.getWindow().getDecorView())>=3));h.screenshot(mode==1?"28_main_light_130_text.png":"28_main_dark_130_text.png");nav("설정");h.screenshot(mode==1?"28_main_light_130_settings.png":"28_main_dark_130_settings.png");
  }
 }
}
