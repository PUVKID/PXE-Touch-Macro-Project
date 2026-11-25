package kr.yunseok.touchmacro;
import android.app.*;import android.content.*;import android.os.*;import android.widget.*;import android.graphics.*;import androidx.test.ext.junit.runners.AndroidJUnit4;import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;
/** 밝은/어두운 실제 화면과 반복 상태 갱신에서 불필요한 텍스트 변경을 확인합니다. */
@RunWith(AndroidJUnit4.class) public class Ui27Test {
 final GuidedFlowTest h=new GuidedFlowTest();
 @Before public void boot()throws Exception{h.boot();}
 @After public void end(){h.end();}
 android.view.View target(android.view.View v,String name){if(android.text.TextUtils.equals(name,v.getContentDescription()))return v;if(v instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)v;for(int i=0;i<group.getChildCount();i++){android.view.View found=target(group.getChildAt(i),name);if(found!=null)return found;}}return null;}
 void nav(String name){h.main(()->{android.view.View found=target(h.a.getWindow().getDecorView(),name);assertNotNull(name,found);assertTrue(found.performClick());});h.in.waitForIdleSync();}
 @Test public void lightDarkNavigationAndStatusUpdates()throws Exception{
  h.main(()->SetupFlow.finished(h.a));
  for(int mode=1;mode<=2;mode++){
   final int selected=mode;h.main(()->Ui.prefs(h.a).edit().putInt("theme",selected).apply());
   h.a=(MainActivity)h.in.startActivitySync(new Intent(h.in.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
   h.in.waitForIdleSync();assertEquals(selected==2,Ui.dark(h.a));
   h.screenshot(mode==1?"27_light.png":"27_dark.png");
   nav("설정");h.screenshot(mode==1?"27_light_settings.png":"27_dark_settings.png");
   nav("글자");h.screenshot(mode==1?"27_light_text.png":"27_dark_text.png");
  }
  h.main(()->{TextView label=new TextView(h.a);final int[] changes={0};label.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){changes[0]++;}public void afterTextChanged(android.text.Editable value){}});for(int i=0;i<40;i++)Ui.textIfChanged(label,"15회 중 8회");assertEquals(1,changes[0]);Ui.textIfChanged(label,"15회 중 9회");assertEquals(2,changes[0]);assertEquals("15회 중 9회",label.getText().toString());});
 }
}
