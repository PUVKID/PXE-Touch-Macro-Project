package kr.yunseok.touchmacro;
import android.content.*;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 두 테마의 탐색·입력·비활성·포커스와 실제 화면을 확인합니다. */
@RunWith(AndroidJUnit4.class) public class Ui28Test {
 final GuidedFlowTest h=new GuidedFlowTest();
 @Before public void boot()throws Exception{h.boot();}
 @After public void end(){h.end();}
 View target(View v,String name){if(android.text.TextUtils.equals(name,v.getContentDescription()))return v;if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++){View found=target(group.getChildAt(i),name);if(found!=null)return found;}}return null;}
 void nav(String name){h.main(()->{View found=target(h.a.getWindow().getDecorView(),name);assertNotNull(name,found);assertTrue(found.performClick());});h.in.waitForIdleSync();}
 EditText input(View v){if(v instanceof EditText)return (EditText)v;if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++){EditText found=input(group.getChildAt(i));if(found!=null)return found;}}return null;}
 @Test public void themesNavigationAndControlStates()throws Exception{
  h.main(()->SetupFlow.finished(h.a));
  for(int mode=1;mode<=2;mode++){
   final int selected=mode;h.main(()->Ui.prefs(h.a).edit().putInt("theme",selected).apply());
   h.a=(MainActivity)h.in.startActivitySync(new Intent(h.in.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
   h.in.waitForIdleSync();assertEquals(selected==2,Ui.dark(h.a));
   h.screenshot(mode==1?"28_light.png":"28_dark.png");
   nav("설정");h.screenshot(mode==1?"28_light_settings.png":"28_dark_settings.png");
   nav(mode==1?"화면 모드 · 라이트":"화면 모드 · 다크");h.screenshot(mode==1?"28_light_dialog.png":"28_dark_dialog.png");h.click("취소");
   nav("글자");h.screenshot(mode==1?"28_light_text.png":"28_dark_text.png");
   h.main(()->{EditText field=input(h.a.getWindow().getDecorView());assertNotNull(field);field.setText("Adobe");field.setFocusableInTouchMode(true);assertTrue(field.requestFocus());assertTrue(field.hasFocus());assertEquals("Adobe",Ui.prefs(h.a).getString("word",""));});
   h.screenshot(mode==1?"28_light_input.png":"28_dark_input.png");h.click("인식 설정");h.screenshot(mode==1?"28_light_recognition.png":"28_dark_recognition.png");h.click("취소");
   h.main(()->{
    LinearLayout content=new LinearLayout(h.a);content.setOrientation(1);content.setPadding(Ui.dp(h.a,20),Ui.dp(h.a,20),Ui.dp(h.a,20),Ui.dp(h.a,20));content.setBackgroundColor(Ui.bg(h.a));
    EditText field=new EditText(h.a);field.setText("Adobe");field.setHint("찾을 글자");Ui.font(field);content.addView(field);
    Button action=Ui.button(h.a,"실행");content.addView(action);Button disabled=Ui.button(h.a,"실행 준비 중");disabled.setEnabled(false);content.addView(disabled);
    h.a.setContentView(content);field.setFocusableInTouchMode(true);assertTrue(field.requestFocus());
    assertTrue(field.hasFocus());assertEquals("Adobe",field.getText().toString());assertFalse(disabled.isEnabled());
    field.getBackground().setState(new int[]{android.R.attr.state_enabled,android.R.attr.state_focused});
    action.setSelected(true);assertTrue(action.isSelected());assertNotNull(action.getBackground());
   });h.in.waitForIdleSync();h.screenshot(mode==1?"28_light_controls.png":"28_dark_controls.png");
  }
 }
}
