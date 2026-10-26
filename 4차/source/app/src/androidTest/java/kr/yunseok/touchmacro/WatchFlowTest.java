package kr.yunseok.touchmacro;
import android.app.*;import android.content.*;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class) public class WatchFlowTest {
    Instrumentation in;UiAutomation automation;MainActivity activity;MacroService svc;
    volatile int taps=0,backs=0;AlertDialog detail;View target;Button action;
    void main(Runnable r){in.runOnMainSync(r);}
    void shell(String command)throws Exception{try(ParcelFileDescriptor fd=automation.executeShellCommand(command);java.io.FileInputStream s=new java.io.FileInputStream(fd.getFileDescriptor())){byte[] b=new byte[4096];while(s.read(b)!=-1){}}}
    void waitFor(java.util.function.BooleanSupplier condition,long timeout,String reason){long end=SystemClock.elapsedRealtime()+timeout;while(!condition.getAsBoolean()&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(80);assertTrue(reason+"\n"+svc.logText(),condition.getAsBoolean());}
    Rect bounds(View v){int[] at=new int[2];v.getLocationOnScreen(at);return new Rect(at[0],at[1],at[0]+v.getWidth(),at[1]+v.getHeight());}
    @After public void cleanup(){if(in!=null&&svc!=null)main(()->{svc.stop();if(detail!=null)detail.dismiss();});}
    @Test public void regionRefreshMacroBackRepeatAndCancellation()throws Exception{
        in=InstrumentationRegistry.getInstrumentation();automation=in.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        shell("settings put secure enabled_accessibility_services \"\"");shell("settings put secure accessibility_enabled 0");shell("settings put secure enabled_accessibility_services kr.yunseok.touchmacro/kr.yunseok.touchmacro.MacroService");shell("settings put secure accessibility_enabled 1");
        activity=(MainActivity)in.startActivitySync(new Intent(in.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        long end=SystemClock.elapsedRealtime()+12000;while(MacroService.get()==null&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(80);svc=MacroService.get();assertNotNull(svc);
        main(()->{
            try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField("testing");f.setAccessible(true);f.setBoolean(activity,true);}catch(Exception e){throw new RuntimeException(e);}
            LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);box.setBackgroundColor(Color.WHITE);
            TextView outside=new TextView(activity);outside.setText("감지 대상");outside.setTextSize(24);outside.setTextColor(Color.BLACK);box.addView(outside,new LinearLayout.LayoutParams(-1,Ui.dp(activity,180)));
            target=new View(activity){Paint p=new Paint(3);@Override protected void onDraw(Canvas c){c.drawColor(0xFFEAF0FF);p.setColor(Color.BLACK);p.setTextSize(Ui.dp(activity,28));p.setTypeface(getResources().getFont(R.font.nanum_gothic_bold));p.setTextAlign(Paint.Align.CENTER);c.drawText("감지 대상",getWidth()/2f,getHeight()/2f,p);}};
            box.addView(target,new LinearLayout.LayoutParams(-1,Ui.dp(activity,180)));
            action=new Button(activity);action.setText("상세 열기");box.addView(action,new LinearLayout.LayoutParams(-1,Ui.dp(activity,60)));action.setOnClickListener(v->{taps++;detail=new AlertDialog.Builder(activity).setTitle("상세 화면").setMessage("완료 후 뒤로가기로 닫습니다").create();detail.setOnDismissListener(d->backs++);detail.show();});
            activity.setContentView(box);svc.showControls();svc.controller().minimize();
        });in.waitForIdleSync();SystemClock.sleep(600);
        Rect[] region={null},button={null};main(()->{region[0]=bounds(target);button[0]=bounds(action);});int[] size=svc.size();
        Macro m=new Macro();m.name="탐지 검증";m.startDelay=0;m.maxMinutes=1;Step tap=new Step("TAP");tap.x=button[0].exactCenterX()*100.0/size[0];tap.y=button[0].exactCenterY()*100.0/size[1];tap.after=200;m.steps.add(tap);
        WatchConfig p=new WatchConfig();p.rule.text="감지 대상";p.rule.match="EXACT";p.rule.source="ACCESS";p.rule.left=5;p.rule.right=95;p.rule.top=(region[0].top+10)*100.0/size[1];p.rule.bottom=(region[0].bottom-10)*100.0/size[1];p.intervalSeconds=3;p.waitForRefresh=true;p.settleMs=300;p.returnMs=300;p.maxMinutes=1;
        main(()->Store.save(activity,m));main(()->svc.startWatch(p));SystemClock.sleep(4100);assertEquals("Outside-region accessibility text triggered",0,taps);main(()->svc.stop());
        // Only the canvas text is inside the region: this proves the OCR path triggers the macro.
        p.rule.source="OCR";main(()->svc.startWatch(p));waitFor(()->svc.watchCompleted>=1,30000,"No OCR-triggered macro/back cycle");assertEquals(1,taps);assertEquals(1,backs);assertTrue(svc.watching);assertTrue(svc.controller().isCollapsed());
        SystemClock.sleep(800);assertEquals("Same text immediately retriggered",1,taps);
        waitFor(()->svc.watchCompleted>=2,30000,"Did not refresh and repeat after returning");assertEquals(2,backs);assertTrue(svc.logText().contains("아래로 당겨 새로고침"));
        main(()->svc.togglePause());int before=taps;SystemClock.sleep(3300);assertEquals("Paused watcher still ran",before,taps);main(()->svc.stop());SystemClock.sleep(2500);assertEquals("Stopped watcher ran later",before,taps);assertFalse(svc.watching);
        // The requested default resumes scanning immediately after BACK, without waiting a minute.
        p.waitForRefresh=false;p.intervalSeconds=60;int beforeImmediate=taps;
        main(()->svc.startWatch(p));waitFor(()->svc.watchCompleted>=2,30000,"Immediate return/re-detect did not repeat");main(()->svc.stop());assertEquals(beforeImmediate+2,taps);int completedBacks=backs;
        // Failure must not blindly press BACK or resume detection on an unknown screen.
        Macro broken=new Macro(m.json());Step missing=new Step("TEXT_WAIT");missing.text="never-found-xyz";missing.source="ACCESS";missing.timeout=0;missing.failure="STOP";broken.steps.add(missing);
        main(()->{Store.save(activity,broken);svc.startWatch(p);});waitFor(()->!svc.running,30000,"Failed macro did not stop watcher");assertTrue(svc.status.startsWith("탐지 중단"));assertEquals("Failure unexpectedly pressed BACK",completedBacks,backs);main(()->assertTrue(detail.isShowing()));
        android.util.Log.i("NEX_WATCH_QA","PASS: region exclusion, OCR trigger, saved macro execution, automatic BACK, refresh/repeat, optional duplicate delay, immediate return/re-detect, pause/stop cancellation, failure stops without BACK, compact collapsed control");
    }
}
