package kr.yunseok.touchmacro;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognizer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

/** 캡처 준비 콜백 하나의 지연과 원래 복구 결과를 분리해서 기록하는 외부 진단입니다. */
@RunWith(AndroidJUnit4.class)
public class ScreenResponseTimeoutDiagnosisTest {
    private final GuidedFlowTest h=new GuidedFlowTest();
    private final AtomicInteger captures=new AtomicInteger(),ocrCalls=new AtomicInteger(),ocrCompleted=new AtomicInteger(),taps=new AtomicInteger();
    private final AtomicLong firstCaptureNs=new AtomicLong(),firstOcrNs=new AtomicLong();
    private final List<Long> screenshotTimes=Collections.synchronizedList(new ArrayList<>());
    private final JSONObject evidence=new JSONObject();
    private Context context;
    private File evidenceDirectory;
    private AppDataSnapshot before;
    private Field uiField,sourceField,recognizerField;
    private Handler originalUi;
    private MacroService.ScreenshotSource originalSource;
    private TextRecognizer originalRecognizer;
    private PrepDelayHandler delayedUi;
    private long deadline,cleanupDeadline,runToken;
    private boolean sourceInstalled,recognizerInstalled,handlerInstalled,quiescent,ownsEvidenceDirectory;

    @Test public void capturePreparationTimeoutKeepsRunningAndFreshOcrTapsOnce()throws Exception {
        long started=SystemClock.elapsedRealtime();deadline=started+45000;cleanupDeadline=started+65000;
        Throwable primary=null;
        try{
            context=InstrumentationRegistry.getInstrumentation().getTargetContext();
            String runId=InstrumentationRegistry.getArguments().getString("pxe.timeout.run_id");
            assertNotNull("진단 run-id 인자가 필요합니다",runId);
            assertTrue("진단 run-id는 안전한 ASCII 1~48자여야 합니다",runId.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,47}"));
            File external=context.getExternalFilesDir(null);assertNotNull(external);
            File base=new File(external,"화면응답_114_비공개진단");
            assertFalse(Files.isSymbolicLink(base.toPath()));
            if(!base.exists())assertTrue(base.mkdirs());
            evidenceDirectory=new File(base,runId);
            assertEquals(base.getCanonicalFile(),evidenceDirectory.getCanonicalFile().getParentFile());
            assertFalse("이전 진단 증거를 덮어쓰지 않습니다",evidenceDirectory.exists());
            assertTrue("비공개 진단 폴더를 만들지 못했습니다",evidenceDirectory.mkdir());ownsEvidenceDirectory=true;
            Bundle status=new Bundle();status.putString("pxe.timeout.evidence_relative","화면응답_114_비공개진단/"+runId);
            status.putString("pxe.timeout.result_relative","화면응답_114_비공개진단/"+runId+"/진단결과.json");
            InstrumentationRegistry.getInstrumentation().sendStatus(2,status);
            put("run_id",runId);put("evidence_relative",status.getString("pxe.timeout.evidence_relative"));
            before=new AppDataSnapshot(context,new File(evidenceDirectory,"검사전"));
            before.save();
            h.boot();h.fixture();
            h.main(()->h.entry.setOnClickListener(v->taps.incrementAndGet()));
            waitFor(()->h.a.getPackageName().equals(h.svc.activePackage()),4000,"검사 앱이 활성 상태가 아닙니다");
            installObservers();
            diagnose();
        }catch(Throwable error){primary=error;put("primary_error",trace(error));}
        finally{
            primary=step(primary,()->put("before_cleanup",state()));
            primary=step(primary,()->{if(h.svc!=null)h.end();});
            primary=step(primary,()->{
                if(delayedUi!=null&&delayedUi.selected.getCount()==0)
                    waitUntil(()->delayedUi.settled(),cleanupLeft(8000),"선택 콜백의 전달 또는 future 종료·큐 제거가 완료되지 않았습니다");
            });
            primary=step(primary,()->{
                if(h.svc==null)return;
                ExecutorService worker=(ExecutorService)field(h.svc,"worker");
                worker.submit(()->{}).get(cleanupLeft(12000),TimeUnit.MILLISECONDS);
                waitUntil(()->logQuiescent()&&requestsEmpty(),cleanupLeft(5000),"작업자·캡처·OCR·운영 로그가 정리되지 않았습니다");
                quiescent=true;
            });
            primary=step(primary,()->{
                if(h.svc==null)return;
                h.main(()->{
                    try{
                        if(handlerInstalled){uiField.set(h.svc,originalUi);assertSame(originalUi,uiField.get(h.svc));}
                        if(sourceInstalled)sourceField.set(h.svc,originalSource);
                        if(recognizerInstalled)recognizerField.set(h.svc,originalRecognizer);
                    }catch(IllegalAccessException error){throw new AssertionError(error);}
                });
            });
            primary=step(primary,()->{
                if(before!=null){
                    before.captureObserved(new File(evidenceDirectory,"검사후_원복전"));
                    put("internal_data_restore_performed",false);
                }
            });
            primary=step(primary,()->{
                put("quiescent_before_restore",quiescent);
                put("after_cleanup",state());
                put("outer_force_stop_byte_restore_required",true);
                put("additional_sgd_steps",0);
                put("diagnostic_success",!evidence.has("primary_error")&&!evidence.has("cleanup_errors"));
                if(ownsEvidenceDirectory)writeJson(new File(evidenceDirectory,"진단결과.json"),evidence);
            });
        }
        throwFailure(primary);
    }

    private void installObservers()throws Exception {
        uiField=declared(MacroService.class,"ui");sourceField=declared(MacroService.class,"screenshotSource");recognizerField=declared(MacroService.class,"recognizer");
        originalUi=(Handler)uiField.get(h.svc);originalSource=(MacroService.ScreenshotSource)sourceField.get(h.svc);originalRecognizer=(TextRecognizer)recognizerField.get(h.svc);
        assertSame(Looper.getMainLooper(),originalUi.getLooper());
        delayedUi=new PrepDelayHandler(originalUi.getLooper());
        h.main(()->{
            try{
                sourceField.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{
                    captures.incrementAndGet();firstCaptureNs.compareAndSet(0,SystemClock.elapsedRealtimeNanos());
                    originalSource.take(executor,new AccessibilityService.TakeScreenshotCallback(){
                        @Override public void onSuccess(AccessibilityService.ScreenshotResult result){screenshotTimes.add(result.getTimestamp());callback.onSuccess(result);}
                        @Override public void onFailure(int error){callback.onFailure(error);}
                    });
                });sourceInstalled=true;
                recognizerField.set(h.svc,Proxy.newProxyInstance(TextRecognizer.class.getClassLoader(),new Class<?>[]{TextRecognizer.class},(proxy,called,args)->{
                    Object value;try{value=called.invoke(originalRecognizer,args);}catch(InvocationTargetException error){throw error.getCause();}
                    if(called.getName().equals("process")){
                        ocrCalls.incrementAndGet();firstOcrNs.compareAndSet(0,SystemClock.elapsedRealtimeNanos());
                        @SuppressWarnings("unchecked") Task<Text> task=(Task<Text>)value;
                        task.addOnSuccessListener(Runnable::run,text->ocrCompleted.incrementAndGet());
                    }
                    return value;
                }));recognizerInstalled=true;
                uiField.set(h.svc,delayedUi);handlerInstalled=true;
            }catch(IllegalAccessException error){throw new AssertionError(error);}
        });
    }

    private void diagnose()throws Exception {
        start("안내 대상","OCR");runToken=generation();
        assertTrue("캡처 준비 onUi 콜백 지연 주입에 진입하지 않았습니다",delayedUi.selected.await(left(3000),TimeUnit.MILLISECONDS));
        assertNull("지연 주입 분류에 실패했습니다",delayedUi.injectionFailure);
        assertTrue("선택한 지연 메시지를 게시하지 못했습니다",delayedUi.postedSelected);
        // 원래 5초 예산 뒤와 지연 콜백의 6초 전달 전을 별도로 관측합니다.
        waitUntil(()->SystemClock.uptimeMillis()>=delayedUi.selectedAtUptime+5500,left(6500),"5초 예산 뒤 관측 시각에 도달하지 못했습니다");
        boolean observedBeforeLate=delayedUi.executed.getCount()!=0;
        boolean runningAfterBudget=h.svc.running;
        long tokenAfterBudget=generation();
        boolean connectedAfterBudget=h.svc.connectionActive();
        String outcomeAfterBudget=(String)field(h.svc,"runOutcome");
        boolean completedAfterBudget=!runningAfterBudget&&"완료".equals(outcomeAfterBudget)&&taps.get()==1&&captures.get()==1&&ocrCalls.get()==1&&ocrCompleted.get()==1;
        boolean validAfterBudget=connectedAfterBudget&&tokenAfterBudget==runToken&&(runningAfterBudget||completedAfterBudget);
        put("budget_acceptance_path",runningAfterBudget?"원 토큰으로 계속 실행 중":completedAfterBudget?"원 실행이 새 OCR·터치 한 번으로 이미 정상 완료":"지연만으로 무터치 종료했거나 원 실행이 무효화됨");
        put("after_original_budget",state());
        put("sample_before_delayed_delivery",observedBeforeLate);
        waitUntil(()->SystemClock.uptimeMillis()>=delayedUi.selectedWhen+250,left(2500),"원래 지연 전달 시각 뒤의 관측에 도달하지 못했습니다");
        assertTrue("선택 콜백이 전달되지도 않고 종료된 future와 함께 큐에서 제거되지도 않았습니다",delayedUi.settled());
        put("after_delayed_delivery",state());
        if(h.svc.running)waitFor(()->!h.svc.running,18000,"복구 후 새 OCR 실제 터치가 완료되지 않았습니다");
        int completedTaps=taps.get();
        long completedToken=generation();String completedOutcome=(String)field(h.svc,"runOutcome");
        put("original_timeout_red_signature",!h.svc.running&&captures.get()==0&&taps.get()==0&&delayedUi.futureError().contains("화면 응답 시간이 초과되었습니다"));
        long quietEnd=SystemClock.elapsedRealtime()+left(500);
        while(SystemClock.elapsedRealtime()<quietEnd){assertEquals("늦은 준비 콜백 뒤 중복 터치가 발생했습니다",completedTaps,taps.get());SystemClock.sleep(20);}
        put("recovery_result",state());
        // 별도의 ACCESS 무한 대기에서 실제 정지 버튼의 응답을 확인합니다.
        start("진단 화면에 없는 정지 확인 글자","ACCESS");
        waitFor(()->h.svc.running,1000,"정지 응답 확인용 작업이 시작되지 않았습니다");
        // 실행 플래그와 접근성 버튼 표시 완료는 다르므로 실제 visible 상태를 유한 대기합니다.
        put("stop_button_wait_started_ns",SystemClock.elapsedRealtimeNanos());
        boolean stopVisible=false;
        try{
            waitFor(()->{
                AccessibilityNodeInfo candidate=h.find("■");if(candidate==null)return false;
                try{return candidate.isVisibleToUser();}finally{candidate.recycle();}
            },3000,"실제 정지 버튼이 3초 안에 표시되지 않았습니다");
            stopVisible=true;
        }finally{
            put("stop_button_wait_finished_ns",SystemClock.elapsedRealtimeNanos());
            put("stop_button_visible_after_wait",stopVisible);
        }
        AccessibilityNodeInfo stop=h.find("■");assertNotNull("실제 정지 버튼이 없습니다",stop);
        long stopAt=SystemClock.elapsedRealtimeNanos();
        try{assertTrue(stop.isVisibleToUser());assertTrue(stop.performAction(AccessibilityNodeInfo.ACTION_CLICK));}finally{stop.recycle();}
        waitFor(()->!h.svc.running,1000,"정지 버튼이 1초 안에 반응하지 않았습니다");
        long stopElapsed=SystemClock.elapsedRealtimeNanos()-stopAt;put("stop_response_ns",stopElapsed);
        assertTrue("실제 정지 응답이 1초를 초과했습니다",stopElapsed<=1000000000L);
        assertEquals("정지 확인 중 추가 터치가 발생했습니다",completedTaps,taps.get());
        assertTrue("5~6초 관측 구간을 놓친 주입 검사 오류입니다",observedBeforeLate);
        assertEquals("한 개 이외의 준비 콜백을 지연했습니다",1,delayedUi.delayedCount.get());
        assertTrue("지연만으로 원 실행이 무터치 종료했거나 무효화됐습니다",validAfterBudget);
        assertEquals("선택한 준비 콜백이 원 실행의 토큰이 아닙니다",runToken,delayedUi.selectedToken);
        assertEquals("원 실행 완료 전 토큰이 바뀌었습니다",runToken,completedToken);
        assertEquals("원 실행이 정상 완료하지 않았습니다","완료",completedOutcome);
        assertEquals("실제 새 OCR의 터치가 정확히 한 번이어야 합니다",1,completedTaps);
        assertEquals("복구 후 실제 캡처가 정확히 한 번이어야 합니다",1,captures.get());
        assertEquals("실제 OCR 호출이 정확히 한 번이어야 합니다",1,ocrCalls.get());
        assertEquals("실제 OCR 성공이 정확히 한 번이어야 합니다",1,ocrCompleted.get());
        assertTrue("5초 초과 뒤의 새 캡처가 아닙니다",firstCaptureNs.get()>delayedUi.selectedAtNs+5000000000L);
        assertTrue("새 캡처 이후의 OCR이 아닙니다",firstOcrNs.get()>=firstCaptureNs.get());
    }

    private void start(String text,String source){
        Step step=new Step("TEXT_TAP");step.text=text;step.match="EXACT";step.source=source;step.timeout=0;step.waitForever=true;step.immediate=true;step.after=60000;
        Macro macro=new Macro();macro.name="화면 응답 지연 진단";macro.startDelay=0;macro.maxMinutes=0;macro.steps.add(step);h.main(()->h.svc.start(macro));
    }

    private final class PrepDelayHandler extends Handler {
        final CountDownLatch selected=new CountDownLatch(1),executed=new CountDownLatch(1);
        final AtomicInteger postedCount=new AtomicInteger(),dispatchedCount=new AtomicInteger(),trackedPending=new AtomicInteger(),maxTrackedPending=new AtomicInteger(),delayedCount=new AtomicInteger();
        volatile Message selectedMessage;
        volatile Runnable selectedCallback;
        volatile CompletableFuture<?> selectedFuture;
        volatile String callbackClass="",enclosingMethod="",matchingStack="",injectionFailure;
        volatile String futureFailure="";
        volatile boolean postedSelected,futureDoneAtLateDelivery,futureExceptionalAtLateDelivery;
        volatile long originalWhen,selectedWhen,selectedAtUptime,selectedAtNs,deliveredAtNs,selectedToken;
        PrepDelayHandler(Looper looper){super(looper);}
        @Override public boolean sendMessageAtTime(Message message,long when){
            Runnable callback=message.getCallback();boolean chosen=false;
            if(callback!=null&&selectedMessage==null){
                Method enclosing=callback.getClass().getEnclosingMethod();
                if(enclosing!=null&&enclosing.getDeclaringClass()==MacroService.class&&enclosing.getName().equals("onUi")){
                    StackTraceElement[] stack=Thread.currentThread().getStackTrace();boolean capture=false;
                    for(StackTraceElement row:stack)if(row.getClassName().equals(MacroService.class.getName())&&row.getMethodName().equals("captureOnce"))capture=true;
                    if(capture)synchronized(this){if(selectedMessage==null){
                        try{
                            CompletableFuture<?> found=null;
                            for(Field field:callback.getClass().getDeclaredFields())if(CompletableFuture.class.isAssignableFrom(field.getType())){
                                field.setAccessible(true);assertNull("준비 콜백에 future가 두 개 이상입니다",found);found=(CompletableFuture<?>)field.get(callback);
                            }
                            assertNotNull("준비 콜백의 원래 future를 확인할 수 없습니다",found);
                            selectedMessage=message;selectedCallback=callback;selectedFuture=found;
                            callbackClass=callback.getClass().getName();enclosingMethod=enclosing.toGenericString();matchingStack=Arrays.toString(stack);
                            originalWhen=when;selectedAtUptime=SystemClock.uptimeMillis();selectedAtNs=SystemClock.elapsedRealtimeNanos();selectedWhen=selectedAtUptime+6000;selectedToken=generation();when=selectedWhen;chosen=true;delayedCount.incrementAndGet();
                        }catch(Throwable error){injectionFailure=trace(error);}
                    }}
                }
            }
            int pending=trackedPending.incrementAndGet();maxTrackedPending.accumulateAndGet(pending,Math::max);postedCount.incrementAndGet();
            boolean posted=super.sendMessageAtTime(message,when);
            if(!posted)trackedPending.decrementAndGet();
            if(chosen){postedSelected=posted;selected.countDown();}
            return posted;
        }
        @Override public void dispatchMessage(Message message){
            trackedPending.decrementAndGet();dispatchedCount.incrementAndGet();
            boolean chosen=message==selectedMessage&&message.getCallback()==selectedCallback;
            if(chosen){
                deliveredAtNs=SystemClock.elapsedRealtimeNanos();futureDoneAtLateDelivery=selectedFuture.isDone();futureExceptionalAtLateDelivery=selectedFuture.isCompletedExceptionally();
                if(futureDoneAtLateDelivery)try{selectedFuture.join();}catch(CompletionException error){futureFailure=trace(error.getCause());}catch(Throwable error){futureFailure=trace(error);}
            }
            try{super.dispatchMessage(message);}finally{if(chosen)executed.countDown();}
        }
        boolean settled(){
            if(executed.getCount()==0)return true;
            return selectedMessage!=null&&postedSelected&&selectedFuture.isDone()&&deliveredAtNs==0&&!hasCallbacks(selectedCallback);
        }
        String futureError(){
            CompletableFuture<?> future=selectedFuture;
            if(future!=null&&future.isDone())try{future.join();}catch(CompletionException error){futureFailure=trace(error.getCause());}catch(Throwable error){futureFailure=trace(error);}
            return futureFailure;
        }
        JSONObject snapshot()throws Exception {
            return new JSONObject().put("callback_class",callbackClass).put("enclosing_method",enclosingMethod).put("matching_stack",matchingStack)
                .put("injection_error",injectionFailure==null?JSONObject.NULL:injectionFailure).put("original_budget_ms",5000).put("injected_delay_ms",6000)
                .put("selected_token",selectedToken).put("original_when_uptime",originalWhen).put("selected_when_uptime",selectedWhen).put("selected_at_ns",selectedAtNs).put("delivered_at_ns",deliveredAtNs)
                .put("selected_posted",postedSelected).put("delayed_count",delayedCount.get()).put("selected_dispatched",executed.getCount()==0)
                .put("future_done_at_late_delivery",futureDoneAtLateDelivery).put("future_exceptional_at_late_delivery",futureExceptionalAtLateDelivery).put("future_error_original",futureError())
                .put("future_done_now",selectedFuture!=null&&selectedFuture.isDone()).put("selected_callback_in_queue",selectedCallback!=null&&hasCallbacks(selectedCallback))
                .put("removed_after_future_done",selectedFuture!=null&&selectedFuture.isDone()&&deliveredAtNs==0&&postedSelected&&!hasCallbacks(selectedCallback))
                .put("handler_posts",postedCount.get()).put("handler_dispatches",dispatchedCount.get()).put("tracked_pending_deliveries",trackedPending.get()).put("max_tracked_pending_deliveries",maxTrackedPending.get())
                .put("queue_count_scope","이 Handler의 게시·전달 계수입니다. 제거된 메시지도 미전달 계수에 포함될 수 있으며 선택 콜백의 실제 큐 존재는 hasCallbacks로 별도 기록합니다.");
        }
    }

    private JSONObject state()throws Exception {
        JSONObject out=new JSONObject().put("captures",captures.get()).put("ocr_calls",ocrCalls.get()).put("ocr_completed",ocrCompleted.get()).put("taps",taps.get())
            .put("first_capture_ns",firstCaptureNs.get()).put("first_ocr_ns",firstOcrNs.get());
        synchronized(screenshotTimes){out.put("screenshot_timestamps",new JSONArray(screenshotTimes));}
        if(h.svc!=null)out.put("running",h.svc.running).put("token",generation()).put("status",h.svc.status).put("log_original",h.svc.logText());
        if(delayedUi!=null)out.put("delay",delayedUi.snapshot());return out;
    }
    private boolean logQuiescent(){synchronized(h.svc){try{return !((Boolean)field(h.svc,"logUpdateQueued"));}catch(Exception error){throw new AssertionError(error);}}}
    private boolean requestsEmpty(){try{return ((CaptureRequests)field(h.svc,"captureRequests")).size()==0&&((AtomicReference<?>)field(h.svc,"pendingRecognition")).get()==null;}catch(Exception error){throw new AssertionError(error);}}
    private long generation()throws Exception{return ((AtomicLong)field(h.svc,"generation")).get();}
    private long left(long cap){long value=Math.min(cap,deadline-SystemClock.elapsedRealtime());assertTrue("진단 행동 기한을 초과했습니다",value>0);return value;}
    private long cleanupLeft(long cap){long value=Math.min(cap,cleanupDeadline-SystemClock.elapsedRealtime());assertTrue("진단 원복 기한을 초과했습니다",value>0);return value;}
    private void waitFor(BooleanSupplier condition,long cap,String reason){waitUntil(condition,left(cap),reason);}
    private static void waitUntil(BooleanSupplier condition,long cap,String reason){long end=SystemClock.elapsedRealtime()+cap;while(!condition.getAsBoolean()&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(20);assertTrue(reason,condition.getAsBoolean());}
    private void put(String name,Object value)throws Exception{evidence.put(name,value);}
    private static Field declared(Class<?> type,String name)throws Exception{Field field=type.getDeclaredField(name);field.setAccessible(true);return field;}
    private static Object field(Object owner,String name)throws Exception{return declared(owner.getClass(),name).get(owner);}
    private static String trace(Throwable error){StringWriter out=new StringWriter();error.printStackTrace(new PrintWriter(out));return out.toString();}
    private interface Cleanup {void run()throws Exception;}
    private Throwable step(Throwable primary,Cleanup action){try{action.run();}catch(Throwable error){try{evidence.accumulate("cleanup_errors",trace(error));}catch(Exception ignored){error.addSuppressed(ignored);}if(primary==null)return error;if(primary!=error)primary.addSuppressed(error);}return primary;}
    private static void throwFailure(Throwable error)throws Exception{if(error instanceof Exception)throw (Exception)error;if(error instanceof Error)throw (Error)error;if(error!=null)throw new AssertionError(error);}

    /** 전체 typed map과 원문을 보존하며 원복은 외부 실행기가 앱 강제 정지 뒤 담당합니다. */
    private static final class AppDataSnapshot {
        private final Context context;
        private final File prefsDirectory,backupDirectory;
        private final boolean hadPrefsDirectory;
        private final Map<String,byte[]> prefFiles,journalFiles=new TreeMap<>();
        private final Map<String,Map<String,Object>> values=new TreeMap<>();
        private final Object journalLock;
        AppDataSnapshot(Context context,File backupDirectory)throws Exception {
            this.context=context;this.backupDirectory=backupDirectory;prefsDirectory=new File(context.getApplicationInfo().dataDir,"shared_prefs");
            hadPrefsDirectory=prefsDirectory.exists();prefFiles=readDirectory(prefsDirectory);
            Set<String> names=names(prefFiles.keySet());names.add("macros");names.add("nex_ui");
            for(String name:names)values.put(name,copy(context.getSharedPreferences(name,0).getAll()));
            journalLock=declared(ErrorJournal.class,"LOCK").get(null);
            synchronized(journalLock){for(String name:new String[]{"pxe-errors.txt","pxe-errors.previous.txt"}){File file=new File(context.getFilesDir(),name);if(file.exists())journalFiles.put(name,readFile(file));}}
        }
        void save()throws Exception {
            assertTrue(backupDirectory.mkdirs());saveFiles(new File(backupDirectory,"shared_prefs"),prefFiles);saveFiles(new File(backupDirectory,"error_journal"),journalFiles);
            JSONObject typed=new JSONObject();for(Map.Entry<String,Map<String,Object>> pref:values.entrySet()){
                JSONObject rows=new JSONObject();for(Map.Entry<String,Object> row:pref.getValue().entrySet()){
                    Object value=row.getValue();Object encoded=value instanceof Set<?>?new JSONArray(new TreeSet<>((Set<String>)value)):value;
                    rows.put(row.getKey(),new JSONObject().put("type",value.getClass().getName()).put("value",encoded));
                }typed.put(pref.getKey(),rows);
            }writeJson(new File(backupDirectory,"typed_preferences.json"),typed);
            writeJson(new File(backupDirectory,"file_manifest.json"),new JSONObject().put("shared_prefs_directory_existed",hadPrefsDirectory).put("shared_prefs",fingerprints(prefFiles)).put("error_journal",fingerprints(journalFiles)));
        }
        void captureObserved(File directory)throws Exception {
            new AppDataSnapshot(context,directory).save();
        }
        private static Set<String> names(Set<String> files){Set<String> names=new TreeSet<>();for(String file:files){String name=file.endsWith(".xml.bak")?file.substring(0,file.length()-8):file.endsWith(".xml")?file.substring(0,file.length()-4):null;if(name!=null)names.add(name);}return names;}
        private static Map<String,Object> copy(Map<String,?> original){Map<String,Object> out=new TreeMap<>();for(Map.Entry<String,?> row:original.entrySet()){Object value=row.getValue();if(value instanceof Set<?>){Set<String> strings=new HashSet<>();for(Object item:(Set<?>)value){assertTrue(item instanceof String);strings.add((String)item);}value=strings;}assertTrue("지원되지 않는 prefs 타입입니다",value instanceof String||value instanceof Integer||value instanceof Long||value instanceof Float||value instanceof Boolean||value instanceof Set<?>);out.put(row.getKey(),value);}return out;}
        private static Map<String,byte[]> readDirectory(File directory)throws Exception {Map<String,byte[]> out=new TreeMap<>();if(!directory.exists())return out;assertFalse(Files.isSymbolicLink(directory.toPath()));File[] files=directory.listFiles();assertNotNull(files);assertTrue("prefs 파일 수 상한을 초과했습니다",files.length<=128);long total=0;for(File file:files){byte[] data=readFile(file);total+=data.length;assertTrue("prefs 증거 크기 상한을 초과했습니다",total<=16*1024*1024);out.put(file.getName(),data);}return out;}
        private static byte[] readFile(File file)throws Exception {assertFalse("원복 대상 링크는 허용하지 않습니다",Files.isSymbolicLink(file.toPath()));assertTrue(file.isFile());assertTrue("원복 파일 크기 상한을 초과했습니다",file.length()<=16*1024*1024);return Files.readAllBytes(file.toPath());}
        private static void saveFiles(File directory,Map<String,byte[]> files)throws Exception {assertTrue(directory.mkdirs());for(Map.Entry<String,byte[]> row:files.entrySet())write(new File(directory,row.getKey()),row.getValue());}
        private static JSONObject fingerprints(Map<String,byte[]> files)throws Exception {JSONObject out=new JSONObject();for(Map.Entry<String,byte[]> row:files.entrySet())out.put(row.getKey(),new JSONObject().put("bytes",row.getValue().length).put("sha256",digest(row.getValue())));return out;}
    }

    private static void writeJson(File file,JSONObject value)throws Exception{write(file,value.toString(2).getBytes(StandardCharsets.UTF_8));}
    private static void write(File file,byte[] bytes)throws Exception {assertFalse(Files.isSymbolicLink(file.toPath()));try(FileOutputStream out=new FileOutputStream(file)){out.write(bytes);out.getFD().sync();}}
    private static String digest(byte[] bytes)throws Exception {StringBuilder out=new StringBuilder();for(byte value:MessageDigest.getInstance("SHA-256").digest(bytes))out.append(String.format(java.util.Locale.ROOT,"%02x",value&255));return out.toString();}
}
