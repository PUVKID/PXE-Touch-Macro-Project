package kr.yunseok.touchmacro;

import android.app.*;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.os.Process;
import android.view.*;
import android.view.accessibility.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 실제 납품 제품의 인식·캡처를 바꾸지 않고 한 탐지를 장시간 실행합니다. */
@RunWith(AndroidJUnit4.class)
public class LongRunOcrTest {
    private Instrumentation in;
    private UiAutomation automation;
    private MainActivity activity;
    private MacroService service;
    private AlertDialog detail;
    private CanvasTarget list,work;
    private WatchConfig watch;
    private volatile int entries,backs,rounds,detailRounds,stage;
    private volatile String problem="";
    private volatile boolean listVisible=true;
    private volatile long lastProgress;
    private int rearmPhase;
    private long rearmBegan,ocrAtBack,sequence,startElapsed,startUptime,initialGeneration;
    private final Handler main=new Handler(Looper.getMainLooper());
    private String runId,bootId;
    private File directory;

    private void ui(Runnable action){in.runOnMainSync(action);}
    private Field field(String name)throws Exception{Field field=MacroService.class.getDeclaredField(name);field.setAccessible(true);return field;}
    private long generation()throws Exception{return ((AtomicLong)field("generation").get(service)).get();}
    private ExecutionMetrics.Snapshot metrics()throws Exception{return ((ExecutionMetrics)field("executionMetrics").get(service)).snapshot(SystemClock.elapsedRealtime());}
    private String shell(String command)throws Exception{
        try(ParcelFileDescriptor descriptor=automation.executeShellCommand(command);
            FileInputStream input=new FileInputStream(descriptor.getFileDescriptor());ByteArrayOutputStream output=new ByteArrayOutputStream()){
            byte[] bytes=new byte[4096];int count;while((count=input.read(bytes))!=-1)output.write(bytes,0,count);
            return output.toString("UTF-8").trim();
        }
    }
    private void progress(){lastProgress=SystemClock.elapsedRealtime();}
    private void failFixture(String reason){if(problem.isEmpty())problem=reason;}

    /** 접근성 텍스트를 제공하지 않는 실제 Canvas 글자와 터치 영역입니다. */
    private final class CanvasTarget extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean listing;
        private final RectF hit=new RectF();
        private float downX,downY;
        CanvasTarget(boolean listing){super(activity);this.listing=listing;setBackgroundColor(Color.WHITE);setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            paint.setTypeface(activity.getResources().getFont(R.font.nanum_gothic_bold));}
        @Override protected void onDraw(Canvas canvas){
            super.onDraw(canvas);String value=listing?(listVisible?"검증 항목":"대상 없음"):(stage==1?"다음 단계":stage==2?"완료 단계":"화면 준비 중");
            hit.set(getWidth()*.12f,getHeight()*.36f,getWidth()*.88f,getHeight()*.62f);
            paint.setColor(0xFFE9F7F3);canvas.drawRoundRect(hit,20,20,paint);paint.setColor(Color.BLACK);
            paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(44*getResources().getDisplayMetrics().scaledDensity);
            Paint.FontMetrics font=paint.getFontMetrics();canvas.drawText(value,hit.centerX(),hit.centerY()-(font.ascent+font.descent)/2,paint);
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent event){
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN){downX=event.getX();downY=event.getY();return true;}
            if(event.getActionMasked()==MotionEvent.ACTION_UP&&Math.hypot(event.getX()-downX,event.getY()-downY)<24&&hit.contains(event.getX(),event.getY())){
                if(listing){if(!listVisible||rearmPhase!=0||detail!=null&&detail.isShowing())failFixture("중복 또는 부재 목록 터치");else openDetail();}
                else if(stage==1){stage=0;progress();invalidate();main.postDelayed(()->{stage=2;invalidate();},1500);}
                else if(stage==2){detailRounds++;rounds++;progress();if(detailRounds>15)failFixture("15회 초과 입력");stage=0;invalidate();main.postDelayed(()->{stage=detailRounds<15?1:3;invalidate();},1500);}
                else failFixture("다음 글자가 없는 동안 터치");
            }
            return true;
        }
    }

    private void openDetail(){
        entries++;detailRounds=0;stage=1;progress();work=new CanvasTarget(false);
        detail=new AlertDialog.Builder(activity).setView(work).create();detail.setCancelable(true);
        detail.setOnDismissListener(dialog->{
            if(detailRounds!=15)failFixture("완료 전 뒤로가기: "+detailRounds);
            backs++;progress();rearmPhase=1;rearmBegan=SystemClock.elapsedRealtime();
            try{ocrAtBack=metrics().ocrCount;}catch(Exception error){failFixture(error.getClass().getSimpleName());}
        });
        detail.show();detail.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        detail.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);
    }

    private Step text(String value){
        Step step=new Step("TEXT_TAP");step.text=value;step.source="OCR";step.match="EXACT";
        step.waitForever=true;step.immediate=true;step.timeout=0;step.after=0;step.duration=70;
        step.left=8;step.right=92;step.top=20;step.bottom=80;return step;
    }

    private void prepare()throws Exception{
        in=InstrumentationRegistry.getInstrumentation();automation=in.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        AccessibilityServiceInfo info=automation.getServiceInfo();info.flags|=AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;automation.setServiceInfo(info);
        assertEquals("PXE19",shell("getprop ro.boot.qemu.avd_name"));bootId=shell("cat /proc/sys/kernel/random/boot_id");
        // 계측 시작이 끊은 이전 서비스 연결을 기존 검사와 같은 경로로 다시 준비합니다.
        shell("settings put secure enabled_accessibility_services \"\"");shell("settings put secure accessibility_enabled 0");
        shell("settings put secure enabled_accessibility_services kr.yunseok.touchmacro/kr.yunseok.touchmacro.MacroService");shell("settings put secure accessibility_enabled 1");
        activity=(MainActivity)in.startActivitySync(new Intent(in.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        long until=SystemClock.elapsedRealtime()+15000;while(MacroService.get()==null&&SystemClock.elapsedRealtime()<until)SystemClock.sleep(100);
        service=MacroService.get();assertNotNull("실제 접근성 서비스 연결 실패",service);
        assertEquals(19,activity.getPackageManager().getPackageInfo(activity.getPackageName(),0).getLongVersionCode());
        ui(()->{
            try{Field testing=MainActivity.class.getDeclaredField("testing");testing.setAccessible(true);testing.setBoolean(activity,true);}catch(Exception error){throw new RuntimeException(error);}
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);list=new CanvasTarget(true);activity.setContentView(list);
        });
        in.waitForIdleSync();SystemClock.sleep(800);
        Macro macro=new Macro();macro.name="실제 OCR 연속 검증";macro.startDelay=0;macro.maxMinutes=0;macro.steps.add(text("다음 단계"));macro.steps.add(text("완료 단계"));
        watch=new WatchConfig();watch.macroName=macro.name;watch.rule=text("검증 항목");watch.rule.type="TEXT_WAIT";watch.targetPackage=activity.getPackageName();
        watch.repetitions=15;watch.preventDuplicate=true;watch.startMs=0;watch.entryMs=300;watch.repeatGapMs=0;watch.beforeBackMs=0;watch.returnMs=300;
        watch.cooldownMs=5000;watch.intervalSeconds=1;watch.settleMs=300;watch.rearmMs=1000;watch.maxMinutes=0;
        ui(()->{Store.put(activity,macro);WatchStore.save(activity,watch);WatchStore.blocked(activity,watch,false);service.startWatch(watch);});
        initialGeneration=generation();until=SystemClock.elapsedRealtime()+5000;
        while((long)field("startedAt").get(service)==0&&SystemClock.elapsedRealtime()<until)SystemClock.sleep(20);
        startElapsed=(long)field("startedAt").get(service);assertTrue(startElapsed>0);startUptime=SystemClock.uptimeMillis();progress();
        directory=new File(activity.getExternalFilesDir(null),"pxe20-"+runId);assertTrue(directory.mkdirs()||directory.isDirectory());
        writeStatus("RUNNING",0);
    }

    private synchronized void writeStatus(String result,long stopMs)throws Exception{
        long now=SystemClock.elapsedRealtime();ExecutionMetrics.Snapshot measure=metrics();
        JSONObject value=new JSONObject().put("run_id",runId).put("seq",++sequence).put("result",result)
            .put("pid",Process.myPid()).put("boot_id",bootId).put("generation",initialGeneration).put("observed_generation",generation())
            .put("started_elapsed_ms",startElapsed).put("started_uptime_ms",startUptime).put("elapsed_ms",now-startElapsed)
            .put("uptime_ms",SystemClock.uptimeMillis()).put("wall_ms",System.currentTimeMillis())
            .put("cycles",service.watchCompleted).put("rounds",rounds).put("entries",entries).put("backs",backs)
            .put("ocr_count",measure.ocrCount).put("ocr_ms",measure.ocrMs).put("capture_ms",measure.captureMs).put("step_count",measure.steps)
            .put("last_progress_elapsed_ms",lastProgress).put("errors",problem).put("requested_seconds",seconds())
            .put("running",service.running).put("watching",service.watching).put("paused",service.paused).put("watch_phase",service.watchPhase)
            .put("pss_kb",Debug.getPss()).put("java_heap_bytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())
            .put("native_heap_bytes",Debug.getNativeHeapAllocatedSize()).put("cpu_ms",Process.getElapsedCpuTime()).put("stop_ms",stopMs);
        writeAtomic(new File(directory,"status.json"),value.toString());
        writeAtomic(new File(directory,"errors.txt"),new ErrorJournal(activity.getFilesDir()).read());
        android.util.Log.i("PXE20_SOAK",value.toString());
    }

    private void writeAtomic(File target,String value)throws Exception{
        File temporary=new File(target.getParentFile(),target.getName()+".tmp");
        try(FileOutputStream stream=new FileOutputStream(temporary)){stream.write(value.getBytes(StandardCharsets.UTF_8));stream.getFD().sync();}
        if(!temporary.renameTo(target))throw new IOException("상태 원자 교체 실패");
    }
    private long seconds(){return Long.parseLong(InstrumentationRegistry.getArguments().getString("soak_seconds","86400"));}

    private long stopByActualButton()throws Exception{
        AccessibilityNodeInfo found=null;
        for(AccessibilityWindowInfo window:automation.getWindows()){
            AccessibilityNodeInfo root=window.getRoot();if(root!=null){java.util.List<AccessibilityNodeInfo> nodes=root.findAccessibilityNodeInfosByText("■");for(AccessibilityNodeInfo node:nodes){if("■".contentEquals(node.getText())&&node.isVisibleToUser()){found=AccessibilityNodeInfo.obtain(node);}node.recycle();}root.recycle();}
            if(found!=null)break;
        }
        assertNotNull("종료 경계 실제 정지 버튼 없음",found);Rect bounds=new Rect();found.getBoundsInScreen(bounds);found.recycle();
        long before=SystemClock.elapsedRealtime(),down=SystemClock.uptimeMillis();
        MotionEvent first=MotionEvent.obtain(down,down,MotionEvent.ACTION_DOWN,bounds.exactCenterX(),bounds.exactCenterY(),0);first.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        assertTrue(automation.injectInputEvent(first,true));first.recycle();SystemClock.sleep(60);
        MotionEvent last=MotionEvent.obtain(down,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,bounds.exactCenterX(),bounds.exactCenterY(),0);last.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        assertTrue(automation.injectInputEvent(last,true));last.recycle();
        long until=before+1000;while(service.running&&SystemClock.elapsedRealtime()<until)SystemClock.sleep(10);
        assertFalse("정지 1초 상한",service.running);return SystemClock.elapsedRealtime()-before;
    }

    @Test public void realOcrSingleWatchRemainsContinuous()throws Exception{
        runId=InstrumentationRegistry.getArguments().getString("run_id");assertNotNull(runId);assertTrue(runId.matches("[a-zA-Z0-9_-]+"));assertTrue(seconds()==300||seconds()==86400);
        try{
            prepare();long nextHeartbeat=SystemClock.elapsedRealtime()+30000;
            while(true){
                long now=SystemClock.elapsedRealtime();ExecutionMetrics.Snapshot measure=metrics();
                if(!service.running||!service.watching||service.paused)failFixture("탐지 실행 중단 또는 일시정지");
                if(generation()!=initialGeneration||MacroService.get()!=service)failFixture("매크로 세대 또는 서비스 교체");
                if(now-lastProgress>=300000)failFixture("실제 진행 300초 정체");
                if(!problem.isEmpty())throw new AssertionError(problem);
                if(rearmPhase==1&&service.watchPhase.equals("중복 차단 · 사라짐 대기")&&measure.ocrCount>ocrAtBack){
                    ui(()->{listVisible=false;list.invalidate();});rearmPhase=2;rearmBegan=now;
                }
                if(rearmPhase==2&&!WatchStore.blocked(activity,watch)){
                    ui(()->{listVisible=true;list.invalidate();});rearmPhase=0;progress();
                }
                if(rearmPhase==2&&now-rearmBegan>30000)throw new AssertionError("실제 부재의 중복 잠금 해제 30초 초과");
                boolean boundary=backs>=3&&entries==backs&&service.watchCompleted==backs&&service.watchPhase.equals("완료 후 쉬는 중");
                if(now-startElapsed>=seconds()*1000&&boundary){
                    assertEquals(backs*15,rounds);assertTrue(measure.ocrCount>=rounds*2);assertTrue(measure.captureMs>0);
                    long stopMs=stopByActualButton();writeStatus("PASS",stopMs);break;
                }
                if(now>=nextHeartbeat){writeStatus("RUNNING",0);nextHeartbeat=now+30000;}
                SystemClock.sleep(100);
            }
        }catch(Throwable error){
            failFixture(error.getClass().getSimpleName()+": "+String.valueOf(error.getMessage()));
            if(service!=null&&directory!=null)writeStatus("FAIL",0);
            if(error instanceof Exception)throw (Exception)error;throw (AssertionError)error;
        }finally{if(service!=null&&service.running)ui(()->service.stop());}
    }
}
