package kr.yunseok.touchmacro;
import android.app.*;import android.accessibilityservice.AccessibilityServiceInfo;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;import android.view.accessibility.*;import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class) public class GuidedFlowTest {
 Instrumentation in;UiAutomation auto;MainActivity a;MacroService svc;AlertDialog detail;
 volatile int entries,actions,backs;Button entry,action;
 void main(Runnable r){in.runOnMainSync(r);}
 void shell(String command)throws Exception{try(ParcelFileDescriptor fd=auto.executeShellCommand(command);java.io.FileInputStream s=new java.io.FileInputStream(fd.getFileDescriptor())){byte[] b=new byte[4096];while(s.read(b)!=-1){}}}
 void waitFor(java.util.function.BooleanSupplier test,long ms,String reason){long end=SystemClock.elapsedRealtime()+ms;while(!test.getAsBoolean()&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(80);assertTrue(reason+(svc==null?"":"\n"+svc.logText()),test.getAsBoolean());}
 @Before public void boot()throws Exception{
  in=InstrumentationRegistry.getInstrumentation();auto=in.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);AccessibilityServiceInfo info=auto.getServiceInfo();info.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;auto.setServiceInfo(info);
  shell("settings put secure enabled_accessibility_services \"\"");shell("settings put secure accessibility_enabled 0");shell("settings put secure enabled_accessibility_services kr.yunseok.touchmacro/kr.yunseok.touchmacro.MacroService");shell("settings put secure accessibility_enabled 1");
  a=(MainActivity)in.startActivitySync(new Intent(in.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));waitFor(()->MacroService.get()!=null,12000,"No accessibility service");svc=MacroService.get();
 }
 @After public void end(){if(svc!=null)main(()->{svc.stop();if(svc.controller().recording)svc.controller().endRecording();if(detail!=null)detail.dismiss();});}
 AccessibilityNodeInfo search(AccessibilityNodeInfo n,String value){if(n==null)return null;if(value.contentEquals(n.getText()==null?"":n.getText()))return AccessibilityNodeInfo.obtain(n);for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo child=n.getChild(i),found=search(child,value);if(child!=null)child.recycle();if(found!=null)return found;}return null;}
 AccessibilityNodeInfo find(String text){for(AccessibilityWindowInfo w:auto.getWindows()){AccessibilityNodeInfo root=w.getRoot(),found=search(root,text);if(root!=null)root.recycle();if(found!=null)return found;}return null;}
 void click(String text){waitFor(()->{AccessibilityNodeInfo n=find(text);if(n==null)return false;n.recycle();return true;},5000,"Missing UI: "+text);AccessibilityNodeInfo n=find(text);assertNotNull(n);assertTrue(n.performAction(AccessibilityNodeInfo.ACTION_CLICK));n.recycle();SystemClock.sleep(200);}
 void setAfter(String label,String value){AccessibilityNodeInfo n=find(label);assertNotNull(label,n);AccessibilityNodeInfo parent=n.getParent();boolean next=false,done=false;for(int i=0;i<parent.getChildCount();i++){AccessibilityNodeInfo child=parent.getChild(i);if(next&&"android.widget.EditText".contentEquals(child.getClassName())){Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value);assertTrue(child.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args));done=true;child.recycle();break;}if(label.contentEquals(child.getText()==null?"":child.getText()))next=true;child.recycle();}parent.recycle();n.recycle();assertTrue(label,done);}
 void touch(float x,float y,int action,long down){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);assertTrue(auto.injectInputEvent(e,true));e.recycle();}
 void tap(float x,float y){long t=SystemClock.uptimeMillis();touch(x,y,MotionEvent.ACTION_DOWN,t);SystemClock.sleep(80);touch(x,y,MotionEvent.ACTION_UP,t);}
 void drag(float x,float y,float endX,float endY){long t=SystemClock.uptimeMillis();touch(x,y,MotionEvent.ACTION_DOWN,t);for(int i=1;i<=10;i++){SystemClock.sleep(30);touch(x+(endX-x)*i/10,y+(endY-y)*i/10,MotionEvent.ACTION_MOVE,t);}touch(endX,endY,MotionEvent.ACTION_UP,t);}
 Rect bounds(View v){int[] xy=new int[2];v.getLocationOnScreen(xy);return new Rect(xy[0],xy[1],xy[0]+v.getWidth(),xy[1]+v.getHeight());}
 void fixture(){main(()->{try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField("testing");f.setAccessible(true);f.setBoolean(a,true);}catch(Exception e){throw new RuntimeException(e);}LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(120,600,120,0);box.setBackgroundColor(Color.WHITE);entry=new Button(a);entry.setText("안내 대상");box.addView(entry,new LinearLayout.LayoutParams(-1,Ui.dp(a,70)));a.setContentView(box);});in.waitForIdleSync();}
 void screenshot(String name)throws Exception{Bitmap b=auto.takeScreenshot();assertNotNull(b);try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(a.getExternalFilesDir(null),name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}finally{b.recycle();}}

 @Test public void regionRecordingAutomaticallyReturnAndSaveDelays()throws Exception{
  main(()->{SetupFlow.begin(a);a.refreshSetup();});setAfter("찾을 글자","안내 대상");click("다음 · 탐지 영역 정하기");assertEquals(1,SetupFlow.stage(a));
  fixture();main(()->svc.controller().showSetupCoach("REGION"));click("영역 그리기");drag(120,650,950,1050);click("이 영역 사용");waitFor(()->!WatchStore.load(a).priorityAreas.isEmpty(),4000,"Region did not save");
  WatchConfig region=WatchStore.load(a);assertEquals(120*100.0/svc.size()[0],region.rule.left,.1);assertEquals(1050*100.0/svc.size()[1],region.rule.bottom,.1);
  click("다음 · 매크로 녹화");waitFor(()->find("작업 이름")!=null,5000,"Guide did not reopen after region");screenshot("guide_record.png");
  String original=Store.load(a).json().toString();fixture();main(()->{entry.setOnClickListener(v->actions++);svc.controller().showSetupCoach("RECORD");});click("녹화 시작");assertTrue(svc.controller().recording);
  Rect[] box={null};main(()->box[0]=bounds(entry));tap(box[0].exactCenterX(),box[0].exactCenterY());waitFor(()->actions==1,5000,"Recorded tap was not replayed");click("완료");
  waitFor(()->SetupFlow.stage(a)==3,5000,"Recording did not advance");assertFalse(SetupFlow.pending(a));assertEquals(original,Store.load(a).json().toString());WatchConfig selected=WatchStore.load(a);assertEquals(1,Store.library(a).get(selected.macroName).steps.size());
  waitFor(()->find("매크로 전체 반복 · 회")!=null,5000,"Delay guide missing");
  setAfter("글자 터치 → 매크로 시작 · 초","1.25");setAfter("매크로 한 회 → 다음 회 · 초","0.1");
  // Remaining fields live below the fold. Scroll the guide, rather than bypassing its save path.
  drag(850,1700,850,650);SystemClock.sleep(250);setAfter("매크로 모두 완료 → 뒤로가기 · 초","0.25");setAfter("뒤로가기 → 목록 화면 대기 · 초","0.3");
  drag(850,1700,850,650);SystemClock.sleep(250);setAfter("완료 후 쉬는 시간 · 분","10");setAfter("추가 · 초","0");setAfter("그 이후 새로고침 간격 · 초","2");setAfter("새로고침 → 글자 탐지 · 초","0.3");setAfter("시작 버튼 → 첫 탐지 · 초","0");
  click("다음 · 실행 준비");waitFor(()->SetupFlow.stage(a)==4,4000,"Delay save did not advance");WatchConfig saved=WatchStore.load(a);assertEquals(1250,saved.entryMs);assertEquals(100,saved.repeatGapMs);assertEquals(250,saved.beforeBackMs);assertEquals(300,saved.returnMs);assertEquals(2,saved.intervalSeconds);assertEquals(300,saved.settleMs);assertEquals(0,saved.startMs);assertFalse(saved.preventDuplicate);assertEquals(600000,saved.cooldownMs);screenshot("guide_ready.png");
  android.util.Log.i("NEX_GUIDE_QA","PASS: word entry, real drawn region, automatic return, real recorded/replayed gesture, automatic named save, original macro restored, second-based delays saved, guide ready");
 }
 @Test public void duplicateLockSurvivesRefreshAndRestartUntilStableAbsence()throws Exception{
  fixture();main(()->{LinearLayout body=new LinearLayout(a);body.setPadding(40,100,40,100);action=new Button(a);action.setText("매크로 동작");action.setOnClickListener(v->actions++);body.addView(action,new LinearLayout.LayoutParams(-1,Ui.dp(a,80)));detail=new AlertDialog.Builder(a).setTitle("작업 화면").setView(body).create();detail.show();});in.waitForIdleSync();Rect[] tapAt={null},roi={null};main(()->{tapAt[0]=bounds(action);detail.dismiss();roi[0]=bounds(entry);detail.setOnDismissListener(d->backs++);entry.setOnClickListener(v->{entries++;detail.show();});});
  int[] size=svc.size();Macro m=new Macro();m.name="중복 차단 검증";m.rounds=15;m.startDelay=9000;Step s=new Step("TAP");s.duration=70;s.after=30;s.x=tapAt[0].exactCenterX()*100/size[0];s.y=tapAt[0].exactCenterY()*100/size[1];m.steps.add(s);
  WatchConfig p=new WatchConfig();p.preventDuplicate=true;p.cooldownMs=2000;p.rule.text="안내 대상";p.rule.source="ACCESS";p.rule.match="EXACT";p.rule.left=(roi[0].left-2)*100.0/size[0];p.rule.top=(roi[0].top-2)*100.0/size[1];p.rule.right=(roi[0].right+2)*100.0/size[0];p.rule.bottom=(roi[0].bottom+2)*100.0/size[1];p.targetPackage=a.getPackageName();p.macroName=m.name;p.startMs=0;p.repeatGapMs=0;p.beforeBackMs=200;p.entryMs=300;p.returnMs=300;p.settleMs=300;p.intervalSeconds=2;p.rearmMs=1000;
  main(()->{Store.put(a,m);WatchStore.blocked(a,p,false);svc.startWatch(p);});waitFor(()->svc.watchCompleted==1,15000,"First cycle failed");assertEquals(15,actions);assertEquals(1,backs);assertTrue(WatchStore.blocked(a,p));SystemClock.sleep(6500);assertEquals("Repeated same visible text after refresh",1,entries);assertTrue(svc.watching);assertTrue(svc.timingText().contains("직전"));screenshot("run_status.png");
  main(()->{svc.stop();svc.startWatch(p);});SystemClock.sleep(3500);assertEquals("Restart bypassed persisted lock",1,entries);
  main(()->entry.setText("대상 없음"));waitFor(()->!WatchStore.blocked(a,p),12000,"Stable absence did not release lock");assertEquals(1,entries);
  main(()->entry.setText("안내 대상"));waitFor(()->svc.watchCompleted==1,15000,"Reappearance did not execute");assertEquals(2,entries);assertEquals(30,actions);assertEquals(2,backs);assertTrue(svc.timingText().contains("전체 경과"));assertTrue(svc.timingText().contains("완료 1바퀴"));main(()->svc.stop());String frozen=svc.timingText();SystemClock.sleep(400);assertEquals(frozen,svc.timingText());
  android.util.Log.i("NEX_DUPLICATE_QA","PASS: exact 15 actions, refresh wait, same text blocked across multiple refreshes and restart, stable absence required, reappearance triggers next 15, live HUD and cycle/total timers, stop freezes totals");
 }
}
