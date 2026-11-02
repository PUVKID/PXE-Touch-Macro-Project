package kr.yunseok.touchmacro;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class) public class WatchPresetTest {
    android.app.Instrumentation in;MacroService svc;MainActivity activity;
    volatile int entries=0,actions=0,backs=0;
    volatile long firstBack=0,secondEntry=0;
    AlertDialog detail;Button entry,action;
    void main(Runnable r){in.runOnMainSync(r);}
    void shell(String command)throws Exception{
        try(ParcelFileDescriptor fd=in.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).executeShellCommand(command);
            java.io.FileInputStream stream=new java.io.FileInputStream(fd.getFileDescriptor())){byte[] b=new byte[4096];while(stream.read(b)!=-1){}}
    }
    void waitFor(java.util.function.BooleanSupplier condition,long timeout,String reason){
        long end=SystemClock.elapsedRealtime()+timeout;
        while(!condition.getAsBoolean()&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(60);
        assertTrue(reason+"\n"+svc.logText(),condition.getAsBoolean());
    }
    Rect bounds(View v){int[] xy=new int[2];v.getLocationOnScreen(xy);return new Rect(xy[0],xy[1],xy[0]+v.getWidth(),xy[1]+v.getHeight());}
    @After public void cleanup(){if(in!=null&&svc!=null)main(()->{svc.stop();if(detail!=null)detail.dismiss();});}
    @Test public void entersRunsExactlyFifteenBacksAndRefreshesAfterTenSeconds()throws Exception{
        in=InstrumentationRegistry.getInstrumentation();
        shell("settings put secure enabled_accessibility_services \"\"");shell("settings put secure accessibility_enabled 0");
        shell("settings put secure enabled_accessibility_services kr.yunseok.touchmacro/kr.yunseok.touchmacro.MacroService");shell("settings put secure accessibility_enabled 1");
        activity=(MainActivity)in.startActivitySync(new Intent(in.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        long end=SystemClock.elapsedRealtime()+12000;while(MacroService.get()==null&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(80);svc=MacroService.get();assertNotNull(svc);
        main(()->{
            try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField("testing");f.setAccessible(true);f.setBoolean(activity,true);}catch(Exception e){throw new RuntimeException(e);}
            LinearLayout list=new LinearLayout(activity);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(120,400,120,0);list.setBackgroundColor(Color.WHITE);
            entry=new Button(activity);entry.setText("예약 가능");list.addView(entry,new LinearLayout.LayoutParams(-1,Ui.dp(activity,70)));
            entry.setOnClickListener(v->{entries++;if(entries==2)secondEntry=SystemClock.elapsedRealtime();detail.show();});activity.setContentView(list);
            LinearLayout body=new LinearLayout(activity);body.setPadding(40,100,40,100);body.setOrientation(LinearLayout.VERTICAL);
            action=new Button(activity);action.setText("매크로 동작");action.setOnClickListener(v->actions++);body.addView(action,new LinearLayout.LayoutParams(-1,Ui.dp(activity,80)));
            detail=new AlertDialog.Builder(activity).setTitle("들어온 화면").setView(body).create();
            detail.show();svc.showControls();svc.controller().minimize();
        });in.waitForIdleSync();SystemClock.sleep(500);
        Rect[] actionRect={null},entryRect={null};main(()->{actionRect[0]=bounds(action);detail.dismiss();});in.waitForIdleSync();main(()->entryRect[0]=bounds(entry));
        main(()->detail.setOnDismissListener(d->{backs++;if(backs==1)firstBack=SystemClock.elapsedRealtime();}));
        int[] size=svc.size();Macro m=new Macro();m.name="15회 검증 매크로";m.rounds=15;m.startAt=2;m.startDelay=0;m.gap=0;m.maxMinutes=1;
        Step wait=new Step("WAIT");wait.duration=1;wait.after=0;m.steps.add(wait);
        Step tap=new Step("TAP");tap.x=actionRect[0].exactCenterX()*100/size[0];tap.y=actionRect[0].exactCenterY()*100/size[1];tap.duration=70;tap.after=30;m.steps.add(tap);
        WatchConfig p=new WatchConfig();p.cooldownMs=10000;p.preventDuplicate=false;p.repeatGapMs=0;p.rule.text="예약 가능";p.rule.source="ACCESS";p.rule.match="EXACT";p.rule.left=(entryRect[0].left-2)*100.0/size[0];p.rule.top=(entryRect[0].top-2)*100.0/size[1];p.rule.right=(entryRect[0].right+2)*100.0/size[0];p.rule.bottom=(entryRect[0].bottom+2)*100.0/size[1];p.macroName=m.name;p.entryMs=300;p.settleMs=300;p.returnMs=300;
        main(()->{Store.put(activity,m);WatchStore.save(activity,p);svc.startWatch(WatchStore.load(activity));});
        waitFor(()->svc.watchCompleted>=1,35000,"First cycle did not finish");
        assertEquals(1,entries);assertEquals("Exactly fifteen macro rounds required",15,actions);assertEquals(1,backs);assertFalse(detail.isShowing());
        SystemClock.sleep(7500);assertEquals("Entered again before ten-second refresh",1,entries);assertEquals(15,actions);
        waitFor(()->svc.watchCompleted>=2,35000,"Second refresh/entry cycle did not finish");
        main(()->svc.stop());assertEquals(2,entries);assertEquals(30,actions);assertEquals(2,backs);
        assertTrue("Refresh must wait ten seconds after returning",secondEntry-firstBack>=10000);
        assertTrue(svc.logText().contains("아래로 당겨 새로고침"));
        Macro saved=Store.library(activity).get(m.name);assertEquals(15,saved.rounds);assertEquals(2,saved.startAt);
        android.util.Log.i("PXE_PRESET_QA","PASS: chosen text tapped to enter, named macro exactly 15 rounds (not 225), BACK, full 10-second refresh wait, second entry with exactly 15 more actions, saved macro preserved");
    }
}
