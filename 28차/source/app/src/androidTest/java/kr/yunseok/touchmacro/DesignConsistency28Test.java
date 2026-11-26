package kr.yunseok.touchmacro;
import android.content.*;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class) public class DesignConsistency28Test {
 final GuidedFlowTest h=new GuidedFlowTest();float original;
 @Before public void boot()throws Exception{h.boot();original=h.a.getResources().getConfiguration().fontScale;h.main(()->SetupFlow.finished(h.a));}
 @After public void end()throws Exception{h.end();h.shell("settings put system font_scale "+original);}
 Object field(Object o,String name){try{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(Exception e){throw new AssertionError(e);}}
 void set(Object o,String name,Object value){try{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}catch(Exception e){throw new AssertionError(e);}}
 void render(){try{Method m=MainActivity.class.getDeclaredMethod("render");m.setAccessible(true);m.invoke(h.a);}catch(Exception e){throw new AssertionError(e);}}
 Button button(View v,String desc){if(v instanceof Button&&desc.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return (Button)v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){Button b=button(((ViewGroup)v).getChildAt(i),desc);if(b!=null)return b;}return null;}
 Rect ink(Button b){Bitmap bmp=Bitmap.createBitmap(b.getWidth(),b.getHeight(),Bitmap.Config.ARGB_8888);b.getForeground().setBounds(0,0,bmp.getWidth(),bmp.getHeight());b.getForeground().draw(new Canvas(bmp));Rect r=new Rect(bmp.getWidth(),bmp.getHeight(),0,0);for(int y=0;y<bmp.getHeight();y++)for(int x=0;x<bmp.getWidth();x++)if(Color.alpha(bmp.getPixel(x,y))>200){r.left=Math.min(r.left,x);r.top=Math.min(r.top,y);r.right=Math.max(r.right,x+1);r.bottom=Math.max(r.bottom,y+1);}bmp.recycle();return r;}
 @Test public void actionRowsAndControllerHaveConsistentGeometry()throws Exception{
  for(float scale:new float[]{1f,1.3f}){h.shell("settings put system font_scale "+scale);android.os.SystemClock.sleep(300);
   for(int theme=1;theme<=2;theme++){final int mode=theme;h.main(()->Ui.prefs(h.a).edit().putInt("theme",mode).apply());h.a=(MainActivity)h.in.startActivitySync(new Intent(h.in.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
    h.main(()->{Macro macro=new Macro();for(String type:new String[]{"TAP","TEXT_WAIT","SWIPE","BACK"}){Step step=new Step(type);step.text="Adobe 매우 긴 설명 문구를 포함한 글자 조건";macro.steps.add(step);}set(h.a,"macro",macro);set(h.a,"tab",1);render();});h.in.waitForIdleSync();
    h.main(()->{LinearLayout list=(LinearLayout)field(h.a,"list");assertEquals(4,list.getChildCount());int min=Integer.MAX_VALUE,max=0,width=list.getWidth();for(int i=0;i<4;i++){View row=list.getChildAt(i);assertEquals("동작 칸 너비",width,row.getWidth());min=Math.min(min,row.getHeight());max=Math.max(max,row.getHeight());Button more=button(row,(i+1)+"번 동작 더보기");assertNotNull(more);assertEquals("더보기 한 줄",1,more.getLineCount());}assertTrue("칸 높이 최대차 1px: "+min+"~"+max,max-min<=1);});h.screenshot("28_neutral_"+theme+"_"+scale+"_actions.png");
    if(scale==1f){h.main(()->{FloatingController c=h.svc.controller();set(c,"collapsed",false);c.show();c.refresh();});h.in.waitForIdleSync();h.main(()->{View panel=(View)field(h.svc.controller(),"panel");Button play=button(panel,"글자 탐지 시작"),stop=button(panel,"정지");assertNotNull(play);assertNotNull(stop);assertEquals(play.getWidth(),stop.getWidth());assertEquals(play.getHeight(),stop.getHeight());assertEquals("",play.getText().toString());assertEquals("",stop.getText().toString());Rect p=ink(play),s=ink(stop);assertTrue("아이콘 가로 크기",Math.abs(p.width()-s.width())<=2);assertTrue("아이콘 세로 크기",Math.abs(p.height()-s.height())<=2);});h.screenshot("28_neutral_"+theme+"_controls.png");h.main(()->h.svc.controller().close());}
   }
  }
 }
}
