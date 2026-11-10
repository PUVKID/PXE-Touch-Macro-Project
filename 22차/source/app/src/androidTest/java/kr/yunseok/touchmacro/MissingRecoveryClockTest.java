package kr.yunseok.touchmacro;

import android.accessibilityservice.AccessibilityService;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import org.json.*;
import static org.junit.Assert.*;

/** 각 글자 재시도에서도 긴 읽기 복구 뒤 정상 검색 시간을 새로 셉니다. */
@RunWith(AndroidJUnit4.class) public class MissingRecoveryClockTest {
    final EntryRecoveryClockTest clock=new EntryRecoveryClockTest();
    @Rule public org.junit.rules.TestName name=new org.junit.rules.TestName();
    final JSONArray preparation=new JSONArray();
    @Before public void boot()throws Exception{
        clock.boot();GuidedFlowTest h=clock.base.h;long deadline=SystemClock.elapsedRealtime()+3000;
        try{
            h.main(()->clock.base.detail.show());h.in.waitForIdleSync();
            while(!attachedActive()&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(80);
            assertTrue("검색시계 시험 창의 실제 연결·활성화 없음",attachedActive());
            // 실제 창이 붙은 뒤 별도 메인 실행에서 문구를 한 번만 바꿉니다.
            h.main(()->clock.base.expected.setText("정상 부재 검사"));
            while(!observed()&&SystemClock.elapsedRealtime()<deadline)SystemClock.sleep(80);
            assertTrue("검색시계 시험 창의 실제 패키지 관측 없음",observed());
        }finally{
            if(preparation.length()==0)observed();
            JSONObject data=new JSONObject();data.put("method",name.getMethodName());data.put("deadline",deadline);data.put("finished",SystemClock.elapsedRealtime());data.put("samples",preparation);
            try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(h.a.getExternalFilesDir(null),"PXE22_시계준비_"+name.getMethodName()+".json"))){out.write(data.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        }
    }
    @After public void end()throws Exception{clock.end();}
    boolean attachedActive(){if(!clock.base.detail.isShowing()||!clock.base.expected.isAttachedToWindow())return false;for(android.view.accessibility.AccessibilityWindowInfo window:clock.base.h.auto.getWindows()){try{if(window.isActive()&&window.getType()==WindowPackages.APPLICATION&&"진입 화면".contentEquals(window.getTitle()==null?"":window.getTitle()))return true;}finally{window.recycle();}}return false;}
    boolean observed(){
        try{
            GuidedFlowTest h=clock.base.h;JSONObject sample=new JSONObject();sample.put("at",SystemClock.elapsedRealtime());sample.put("service_identity",System.identityHashCode(h.svc));sample.put("current_identity",System.identityHashCode(MacroService.get()));sample.put("connection_active",h.svc.connectionActive());
            WindowPackages packages=(WindowPackages)ControlDiagnostics.field(h.svc,"windowPackages");
            JSONArray mapping=new JSONArray();synchronized(packages){java.util.Map<?,?> entries=(java.util.Map<?,?>)ControlDiagnostics.field(packages,"entries");for(java.util.Map.Entry<?,?> entry:entries.entrySet()){JSONObject row=new JSONObject();row.put("id",entry.getKey());row.put("package",ControlDiagnostics.field(entry.getValue(),"name"));row.put("time",ControlDiagnostics.field(entry.getValue(),"time"));mapping.put(row);}}sample.put("mapping",mapping);
            java.util.ArrayList<WindowPackages.Window> auto=new java.util.ArrayList<>(),service=new java.util.ArrayList<>();
            sample.put("auto_windows",windows(h.auto.getWindows(),auto));sample.put("service_windows",windows(h.svc.getWindows(),service));
            String autoName=packages.resolve(auto,h.a.getPackageName()),serviceName=packages.resolve(service,h.a.getPackageName());sample.put("auto_resolved",autoName);sample.put("service_resolved",serviceName);preparation.put(sample);
            return MacroService.get()==h.svc&&h.svc.connectionActive()&&attachedActive()&&h.a.getPackageName().equals(autoName)&&h.a.getPackageName().equals(serviceName);
        }catch(Exception error){throw new RuntimeException(error);}
    }
    JSONArray windows(java.util.List<android.view.accessibility.AccessibilityWindowInfo> source,java.util.ArrayList<WindowPackages.Window> target)throws Exception{
        JSONArray out=new JSONArray();for(android.view.accessibility.AccessibilityWindowInfo window:source){try{target.add(new WindowPackages.Window(window.getId(),window.getType(),window.isActive(),window.isFocused()));JSONObject row=new JSONObject();row.put("id",window.getId());row.put("type",window.getType());row.put("active",window.isActive());row.put("focused",window.isFocused());row.put("title",String.valueOf(window.getTitle()));android.view.accessibility.AccessibilityNodeInfo root=window.getRoot();if(root!=null){row.put("root_package",String.valueOf(root.getPackageName()));root.recycle();}out.put(row);}finally{window.recycle();}}return out;
    }
    void begin(boolean adaptive){Macro m=new Macro();m.startDelay=0;m.maxMinutes=0;m.adaptiveWait=adaptive;Step s=new Step("TEXT_TAP");s.text="없는 확인 글자";s.source="OCR";s.timeout=3000;s.failure="BACK_RETRY";s.retries=1;s.retryDelay=300;s.immediate=true;s.after=0;m.steps.add(s);clock.base.h.main(()->clock.base.h.svc.start(m));}
    void freshBudget(){clock.base.h.waitFor(()->clock.normalAt.get()>0,12000,"복구 뒤 첫 실제 OCR 없음");SystemClock.sleep(2000);assertEquals("읽기 복구가 개별 글자 검색 기한을 소비했습니다",0,clock.base.backs.get());assertEquals(0,clock.base.h.svc.hudProgress().completed);clock.base.h.waitFor(()->clock.base.backs.get()==1,6000,"정상 검색 3초 뒤 복구 동작 없음");assertTrue(SystemClock.elapsedRealtime()-clock.normalAt.get()>=3000);clock.base.h.main(()->clock.base.h.svc.stop());}
    void ocr(boolean adaptive)throws Exception{clock.proxy(true);begin(adaptive);clock.base.h.waitFor(()->clock.delayedValue.get()!=null,18000,"첫 실제 OCR 없음");clock.base.h.waitFor(()->clock.base.h.svc.logText().contains("OCR 인식 응답 지연"),14000,"12초 OCR 지연 제어 없음");clock.base.h.main(()->clock.delayed.trySetResult(clock.delayedValue.get()));freshBudget();}
    void capture(boolean adaptive)throws Exception{clock.proxy(false);clock.screenshotField.set(clock.base.h.svc,(MacroService.ScreenshotSource)(executor,callback)->{if(clock.captures.incrementAndGet()==1)clock.heldCapture=callback;else clock.originalScreenshot.take(executor,callback);});begin(adaptive);clock.base.h.waitFor(()->clock.base.h.svc.logText().contains("OCR 캡처 응답 지연"),12000,"8초 캡처 지연 제어 없음");freshBudget();}
    @Test public void legacyOcrRecoveryGetsFreshSearchBudget()throws Exception{ocr(false);}
    @Test public void adaptiveOcrRecoveryGetsFreshSearchBudget()throws Exception{ocr(true);}
    @Test public void legacyCaptureRecoveryGetsFreshSearchBudget()throws Exception{capture(false);}
    @Test public void adaptiveCaptureRecoveryGetsFreshSearchBudget()throws Exception{capture(true);}
}
