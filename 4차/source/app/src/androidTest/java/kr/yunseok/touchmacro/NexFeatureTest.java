package kr.yunseok.touchmacro;
import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;import androidx.test.ext.junit.runners.AndroidJUnit4;import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public class NexFeatureTest {
 Instrumentation in;MainActivity activity;MacroService svc;UiAutomation automation;
 void main(Runnable r){in.runOnMainSync(r);}
 void shell(String command)throws Exception{try(ParcelFileDescriptor fd=automation.executeShellCommand(command);java.io.FileInputStream s=new java.io.FileInputStream(fd.getFileDescriptor())){byte[] b=new byte[4096];while(s.read(b)!=-1){}}}
 void screen(){main(()->{try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("testScreen");m.setAccessible(true);m.invoke(activity);}catch(Exception e){throw new RuntimeException(e);}});SystemClock.sleep(300);}
 TextView find(View v,String text){if(v instanceof TextView&&((TextView)v).getText().toString().equals(text))return (TextView)v;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){TextView r=find(g.getChildAt(i),text);if(r!=null)return r;}}return null;}
 boolean has(String text){boolean[] result={false};main(()->result[0]=find(activity.getWindow().getDecorView(),text)!=null);return result[0];}
 Rect bounds(View v){int[] location=new int[2];v.getLocationOnScreen(location);return new Rect(location[0],location[1],location[0]+v.getWidth(),location[1]+v.getHeight());}
 Rect canvas(){Rect[] out={null};main(()->{ViewGroup content=activity.findViewById(android.R.id.content);ViewGroup box=(ViewGroup)content.getChildAt(0);out[0]=bounds(box.getChildAt(4));});return out[0];}
 Step word(String type,String source,String text){Step s=new Step(type);s.text=text;s.source=source;s.match="EXACT";s.timeout=0;s.after=100;return s;}
 Macro macro(Step... steps){Macro m=new Macro();m.startDelay=100;m.maxMinutes=2;for(Step s:steps)m.steps.add(s);return m;}
 void start(Macro m){main(()->svc.start(m));}
 void done(){long end=SystemClock.elapsedRealtime()+20000;while(svc.running&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);assertFalse("Run hung: "+svc.logText(),svc.running);SystemClock.sleep(200);}
 void complete(){done();assertEquals(svc.logText(),"완료",svc.status);}
 void single(long down,int action,float x,float y){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);assertTrue(automation.injectInputEvent(e,true));e.recycle();SystemClock.sleep(80);}
 void multi(long down,int action,int count,float x,float y,float x2,float y2){
  MotionEvent.PointerProperties[] p=new MotionEvent.PointerProperties[count];MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[count];
  for(int i=0;i<count;i++){p[i]=new MotionEvent.PointerProperties();p[i].id=i;p[i].toolType=MotionEvent.TOOL_TYPE_FINGER;coords[i]=new MotionEvent.PointerCoords();coords[i].x=i==0?x:x2;coords[i].y=i==0?y:y2;coords[i].pressure=1;coords[i].size=.08f;}
  MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,count,p,coords,0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,0);assertTrue(automation.injectInputEvent(e,true));e.recycle();SystemClock.sleep(80);
 }
 @Test public void newTextModesGesturesAndRecorder()throws Exception{
  in=InstrumentationRegistry.getInstrumentation();automation=in.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
  shell("settings put secure enabled_accessibility_services \"\"");shell("settings put secure accessibility_enabled 0");shell("settings put secure enabled_accessibility_services kr.yunseok.touchmacro/kr.yunseok.touchmacro.MacroService");shell("settings put secure accessibility_enabled 1");
  activity=(MainActivity)in.startActivitySync(new Intent(in.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
  long end=SystemClock.elapsedRealtime()+12000;while(MacroService.get()==null&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(100);svc=MacroService.get();assertNotNull(svc);
  main(()->{Ui.prefs(activity).edit().putInt("panel_y",svc.size()[1]-650).putBoolean("markers",true).putBoolean("keep_screen",true).apply();svc.showControls();});SystemClock.sleep(200);
  screen();Step excluded=word("TEXT_TAP","ACCESS","화면 확인");excluded.right=1;excluded.bottom=1;excluded.failure="SKIP";start(macro(excluded));complete();assertTrue("Region exclusion still clicked",has("성공 횟수: 0"));
  screen();start(macro(word("TEXT_CHECK","OCR","테스트 확인")));complete();assertTrue("Text preview performed a tap",has("성공 횟수: 0"));
  screen();Rect area=canvas();Rect[] target={null};main(()->target[0]=bounds(find(activity.getWindow().getDecorView(),"화면 확인")));Step offset=word("TEXT_TAP","ACCESS","화면 확인");offset.offsetY=(area.exactCenterY()-target[0].exactCenterY())*100.0/svc.size()[1];start(macro(offset));complete();assertTrue("Offset did not hit canvas",has("OCR 성공 횟수: 1"));
  screen();Step retry=word("TEXT_WAIT","ACCESS","없는문자X");retry.failure="RETRY";retry.retries=1;retry.retryDelay=300;start(macro(retry));done();assertTrue(svc.status.startsWith("중단"));assertTrue(svc.logText().contains("재확인 1/1"));assertTrue(has("성공 횟수: 0"));
  screen();area=canvas();int[] z=svc.size();Step curve=new Step("CURVE");curve.x=(area.left+40)*100.0/z[0];curve.y=area.exactCenterY()*100.0/z[1];curve.x2=(area.right-40)*100.0/z[0];curve.y2=curve.y;curve.cx=50;curve.cy=(area.top+20)*100.0/z[1];curve.duration=350;curve.after=100;
  start(macro(curve));complete();assertTrue("Curve did not reach app",has("OCR 성공 횟수: 1"));
  screen();Step pinch=new Step("PINCH_OUT");pinch.y=area.exactCenterY()*100.0/z[1];pinch.x2=5;pinch.y2=12;pinch.duration=350;pinch.after=100;start(macro(pinch));complete();assertTrue("Pinch did not reach app",has("OCR 성공 횟수: 1"));
  screen();Step multiTap=new Step("MULTI_TAP");multiTap.text="45,"+pinch.y+";55,"+pinch.y;multiTap.after=100;start(macro(multiTap));complete();assertTrue("Multi tap did not reach app",has("OCR 성공 횟수: 1"));
  screen();main(()->{Store.save(activity,new Macro());svc.controller().beginPoint();});in.waitForIdleSync();SystemClock.sleep(900);long down=SystemClock.uptimeMillis();single(down,MotionEvent.ACTION_DOWN,area.exactCenterX(),area.exactCenterY());single(down,MotionEvent.ACTION_UP,area.exactCenterX(),area.exactCenterY());SystemClock.sleep(800);assertFalse("Coordinate capture did not finish; status="+svc.status,svc.controller().recording);Macro picked=Store.load(activity);assertEquals(1,picked.steps.size());assertEquals("TAP",picked.steps.get(0).type);assertTrue("Coordinate selection replayed a tap",has("성공 횟수: 0"));
  screen();main(()->{Store.save(activity,new Macro());svc.controller().beginRecording(false,-1);});in.waitForIdleSync();SystemClock.sleep(900);down=SystemClock.uptimeMillis();single(down,MotionEvent.ACTION_DOWN,area.left+80,area.exactCenterY());single(down,MotionEvent.ACTION_MOVE,area.exactCenterX(),area.top+50);single(down,MotionEvent.ACTION_MOVE,area.right-80,area.exactCenterY());single(down,MotionEvent.ACTION_UP,area.right-80,area.exactCenterY());SystemClock.sleep(1200);Macro recorded=Store.load(activity);assertEquals(1,recorded.steps.size());assertEquals("GESTURE",recorded.steps.get(0).type);assertTrue(recorded.steps.get(0).strokes.getJSONObject(0).getJSONArray("points").length()>2);assertTrue("Recorded curve was not replayed",has("OCR 성공 횟수: 1"));main(()->svc.controller().endRecording());
  screen();main(()->{Store.save(activity,new Macro());svc.controller().beginRecording(true,-1);});in.waitForIdleSync();SystemClock.sleep(900);down=SystemClock.uptimeMillis();float x=area.exactCenterX()-60,x2=area.exactCenterX()+60,y=area.exactCenterY();
  multi(down,MotionEvent.ACTION_DOWN,1,x,y,x2,y);multi(down,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,x,y,x2,y);multi(down,MotionEvent.ACTION_MOVE,2,x-40,y,x2+40,y);multi(down,MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,x-40,y,x2+40,y);multi(down,MotionEvent.ACTION_UP,1,x-40,y,x2+40,y);SystemClock.sleep(1300);
  assertFalse("Single recording did not finish",svc.controller().recording);Macro multiRecord=Store.load(activity);assertEquals(1,multiRecord.steps.size());assertEquals(2,multiRecord.steps.get(0).strokes.length());assertTrue("Recorded multi-touch was not replayed",has("OCR 성공 횟수: 1"));
  Step home=new Step("HOME");home.after=100;Step wait=new Step("WAIT");wait.duration=1000;wait.after=0;start(macro(home,wait));complete();assertTrue(svc.controller().shown());
  main(()->{assertTrue(Ui.prefs(activity).edit().putInt("theme",2).commit());svc.controller().refresh();});assertTrue(Ui.dark(activity));
  android.util.Log.i("NEX_QA","PASS: text region, OCR preview, text offset, retry, curve, pinch, multitap, point capture, curve recording/replay, multi-touch recording/replay, foreground outside activity, dark palette");
 }
}
