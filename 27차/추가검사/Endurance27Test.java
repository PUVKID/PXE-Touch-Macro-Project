package kr.yunseok.touchmacro;

import android.os.*;
import android.app.AlertDialog;
import android.graphics.Rect;
import android.widget.*;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** 같은 서비스 연결에서 반복 OCR·제스처와 마지막 중단을 연속 확인합니다. */
@RunWith(AndroidJUnit4.class) public class Endurance27Test {
    final GuidedFlowTest h=new GuidedFlowTest();
    volatile int taps;
    @Before public void boot()throws Exception{h.boot();h.fixture();h.main(()->{h.entry.setText("Adobe");h.entry.setOnClickListener(v->taps++);h.svc.controller().minimize();});h.in.waitForIdleSync();}
    @After public void end(){h.end();}
    Macro macro(){Macro m=new Macro();m.name="연속 OCR 검증";m.rounds=15;m.startDelay=0;m.gap=0;m.maxMinutes=0;Step s=new Step("TEXT_TAP");s.source="OCR";s.text="Adobe";s.match="EXACT";s.waitForever=true;s.timeout=0;s.immediate=true;s.after=0;m.steps.add(s);return m;}
    @Test public void oneHourRealOcrEntryRepeatBackRefreshWithoutRestart()throws Exception{
        final int[] entries={0},backs={0};final android.widget.Button[] action={null};
        h.main(()->{LinearLayout body=new LinearLayout(h.a);body.setPadding(40,100,40,100);action[0]=new Button(h.a);action[0].setText("실행 확인");action[0].setOnClickListener(v->taps++);body.addView(action[0],new LinearLayout.LayoutParams(-1,Ui.dp(h.a,80)));h.detail=new AlertDialog.Builder(h.a).setTitle("검사 작업 화면").setView(body).create();h.detail.setOnDismissListener(d->backs[0]++);h.entry.setOnClickListener(v->{entries[0]++;h.detail.show();});});h.in.waitForIdleSync();
        Rect[] region={null};h.main(()->region[0]=h.bounds(h.entry));int[] size=h.svc.size();
        Macro m=macro();m.name="창 전환 연속 검증";m.steps.get(0).text="실행 확인";
        WatchConfig p=new WatchConfig();p.rule.text="Adobe";p.rule.source="OCR";p.rule.match="EXACT";p.rule.immediate=true;p.rule.left=(region[0].left-2)*100.0/size[0];p.rule.top=(region[0].top-2)*100.0/size[1];p.rule.right=(region[0].right+2)*100.0/size[0];p.rule.bottom=(region[0].bottom+2)*100.0/size[1];p.targetPackage=h.a.getPackageName();p.macroName=m.name;p.startMs=0;p.repetitions=15;p.repeatGapMs=0;p.beforeBackMs=100;p.entryMs=300;p.returnMs=300;p.settleMs=300;p.intervalSeconds=1;p.cooldownMs=1000;p.preventDuplicate=false;p.waitForRefresh=true;p.maxMinutes=0;
        MacroService service=h.svc;int process=android.os.Process.myPid();long began=SystemClock.elapsedRealtime();
        java.nio.file.Files.write(new java.io.File(h.a.getExternalFilesDir(null),"pxe27-errors-before.txt").toPath(),new ErrorJournal(h.a.getFilesDir()).read().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        h.main(()->{Store.put(h.a,m);h.svc.startWatch(p);});
        java.io.File trace=new java.io.File(h.a.getExternalFilesDir(null),"pxe27-endurance.jsonl");
        java.nio.file.Files.write(trace.toPath(),new byte[0]);
        Field requestsField=MacroService.class.getDeclaredField("captureRequests");requestsField.setAccessible(true);
        Field recognitionField=MacroService.class.getDeclaredField("pendingRecognition");recognitionField.setAccessible(true);
        int completed=0;
        for(int cycle=1;;cycle++){
            final int expected=cycle;h.waitFor(()->h.svc.watchCompleted>=expected,120000,"진입/15회/뒤로/재탐지 한 바퀴 실패");assertEquals(expected,entries[0]);assertEquals(expected,backs[0]);assertEquals(expected*15,taps);assertSame(service,MacroService.get());assertEquals(process,android.os.Process.myPid());
            completed=cycle;
            Debug.MemoryInfo memory=new Debug.MemoryInfo();Debug.getMemoryInfo(memory);
            org.json.JSONObject record=new org.json.JSONObject();record.put("cycle",cycle);record.put("taps",taps);record.put("entries",entries[0]);record.put("backs",backs[0]);record.put("elapsedMs",SystemClock.elapsedRealtime()-began);record.put("pid",process);record.put("pssKiB",memory.getTotalPss());record.put("javaKiB",memory.dalvikPss);record.put("nativeKiB",memory.nativePss);record.put("pendingCapture",((CaptureRequests)requestsField.get(service)).size());record.put("pendingRecognition",((AtomicReference<?>)recognitionField.get(service)).get()!=null);
            assertTrue(((CaptureRequests)requestsField.get(service)).size()<=2);assertTrue(service.running);assertTrue(service.logText().split("\\n").length<=150);
            try(java.io.FileOutputStream stream=new java.io.FileOutputStream(trace,true)){stream.write((record.toString()+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));stream.getFD().sync();}
            android.util.Log.i("PXE_ENDURANCE_QA",record.toString());
            if(record.getLong("elapsedMs")>=3600000L)break;
        }
        h.main(()->h.svc.stop());SystemClock.sleep(1800);assertEquals(completed,entries[0]);assertEquals(completed,backs[0]);assertEquals(completed*15,taps);assertFalse(h.svc.running);
        java.nio.file.Files.write(new java.io.File(h.a.getExternalFilesDir(null),"pxe27-errors-after.txt").toPath(),new ErrorJournal(h.a.getFilesDir()).read().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        android.util.Log.i("PXE_ENDURANCE_QA","PASS cycles="+completed+", taps="+taps+", same process/service, totalMs="+(SystemClock.elapsedRealtime()-began));
    }
}
