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
@RunWith(AndroidJUnit4.class) public class Sustained26Test {
    final GuidedFlowTest h=new GuidedFlowTest();
    volatile int taps;
    @Before public void boot()throws Exception{h.boot();h.fixture();h.main(()->{h.entry.setText("Adobe");h.entry.setOnClickListener(v->taps++);h.svc.controller().minimize();});h.in.waitForIdleSync();}
    @After public void end(){h.end();}
    Macro macro(){Macro m=new Macro();m.name="연속 OCR 검증";m.rounds=15;m.startDelay=0;m.gap=0;m.maxMinutes=0;Step s=new Step("TEXT_TAP");s.source="OCR";s.text="Adobe";s.match="EXACT";s.waitForever=true;s.timeout=0;s.immediate=true;s.after=0;m.steps.add(s);return m;}
    @Test public void fiveEntryOcrRepeatBackRefreshCyclesKeepSameService()throws Exception{
        final int[] entries={0},backs={0};final android.widget.Button[] action={null};
        h.main(()->{LinearLayout body=new LinearLayout(h.a);body.setPadding(40,100,40,100);action[0]=new Button(h.a);action[0].setText("실행 확인");action[0].setOnClickListener(v->taps++);body.addView(action[0],new LinearLayout.LayoutParams(-1,Ui.dp(h.a,80)));h.detail=new AlertDialog.Builder(h.a).setTitle("검사 작업 화면").setView(body).create();h.detail.setOnDismissListener(d->backs[0]++);h.entry.setOnClickListener(v->{entries[0]++;h.detail.show();});});h.in.waitForIdleSync();
        Rect[] region={null};h.main(()->region[0]=h.bounds(h.entry));int[] size=h.svc.size();
        Macro m=macro();m.name="창 전환 연속 검증";m.steps.get(0).text="실행 확인";
        WatchConfig p=new WatchConfig();p.rule.text="Adobe";p.rule.source="OCR";p.rule.match="EXACT";p.rule.immediate=true;p.rule.left=(region[0].left-2)*100.0/size[0];p.rule.top=(region[0].top-2)*100.0/size[1];p.rule.right=(region[0].right+2)*100.0/size[0];p.rule.bottom=(region[0].bottom+2)*100.0/size[1];p.targetPackage=h.a.getPackageName();p.macroName=m.name;p.startMs=0;p.repetitions=15;p.repeatGapMs=0;p.beforeBackMs=100;p.entryMs=300;p.returnMs=300;p.settleMs=300;p.intervalSeconds=1;p.cooldownMs=1000;p.preventDuplicate=false;p.waitForRefresh=true;p.maxMinutes=0;
        MacroService service=h.svc;int process=android.os.Process.myPid();long began=SystemClock.elapsedRealtime();
        h.main(()->{Store.put(h.a,m);h.svc.startWatch(p);});
        for(int cycle=1;cycle<=5;cycle++){
            final int expected=cycle;h.waitFor(()->h.svc.watchCompleted>=expected,120000,"진입/15회/뒤로/재탐지 한 바퀴 실패");assertEquals(expected,entries[0]);assertEquals(expected,backs[0]);assertEquals(expected*15,taps);assertSame(service,MacroService.get());assertEquals(process,android.os.Process.myPid());
            android.util.Log.i("PXE_CYCLE_QA","cycle="+cycle+", taps="+taps+", entries="+entries[0]+", backs="+backs[0]+", elapsedMs="+(SystemClock.elapsedRealtime()-began));
        }
        h.main(()->h.svc.stop());SystemClock.sleep(1800);assertEquals(5,entries[0]);assertEquals(5,backs[0]);assertEquals(75,taps);assertFalse(h.svc.running);
        android.util.Log.i("PXE_CYCLE_QA","PASS cycles=5, OCR actions=75, back=5, entries=5, same process/service, totalMs="+(SystemClock.elapsedRealtime()-began));
    }
    @Test public void fiveBlocksOfFifteenRealOcrTapsThenStopWhileMissing()throws Exception{
        int process=android.os.Process.myPid();MacroService service=h.svc;
        Field requestsField=MacroService.class.getDeclaredField("captureRequests");requestsField.setAccessible(true);
        Field recognitionField=MacroService.class.getDeclaredField("pendingRecognition");recognitionField.setAccessible(true);
        long began=SystemClock.elapsedRealtime();
        for(int block=1;block<=5;block++){
            final int expected=block*15;Macro m=macro();long start=SystemClock.elapsedRealtime();
            h.main(()->h.svc.start(m));h.waitFor(()->!h.svc.running,120000,"15회 OCR 반복 제한 초과");
            assertEquals("실제 터치 수",expected,taps);assertSame(service,MacroService.get());assertEquals(process,android.os.Process.myPid());
            assertEquals(0,((CaptureRequests)requestsField.get(service)).size());
            h.waitFor(()->{try{return ((AtomicReference<?>)recognitionField.get(service)).get()==null;}catch(Exception error){throw new RuntimeException(error);}},1000,"완료 OCR 미해제");
            assertTrue(service.logText().split("\n").length<=150);
            Debug.MemoryInfo memory=new Debug.MemoryInfo();Debug.getMemoryInfo(memory);
            android.util.Log.i("PXE_SUSTAINED_QA","block="+block+", taps="+taps+", ms="+(SystemClock.elapsedRealtime()-start)+", pssKiB="+memory.getTotalPss()+", javaKiB="+memory.dalvikPss+", nativeKiB="+memory.nativePss+", pid="+process);
        }
        h.main(()->h.entry.setText("어도비"));Macro missing=macro();h.main(()->h.svc.start(missing));SystemClock.sleep(3500);assertEquals(75,taps);assertTrue(h.svc.running);
        AccessibilityNodeInfo stop=h.find("■");assertNotNull("정지 버튼 없음",stop);long pressed=SystemClock.elapsedRealtime();assertTrue(stop.performAction(AccessibilityNodeInfo.ACTION_CLICK));stop.recycle();h.waitFor(()->!h.svc.running,1200,"OCR 대기 중 정지 응답 초과");
        SystemClock.sleep(1500);assertEquals("중단 뒤 늦은 결과 터치",75,taps);
        android.util.Log.i("PXE_SUSTAINED_QA","PASS taps=75, blocks=5, totalMs="+(SystemClock.elapsedRealtime()-began)+", stopMsBeforeLateWait="+(SystemClock.elapsedRealtime()-pressed-1500));
    }
}
