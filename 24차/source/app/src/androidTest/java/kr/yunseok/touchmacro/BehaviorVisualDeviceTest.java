package kr.yunseok.touchmacro;



import android.content.SharedPreferences;

import android.graphics.Rect;

import android.os.SystemClock;

import android.view.accessibility.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import androidx.test.platform.app.InstrumentationRegistry;

import java.io.*;

import java.nio.charset.StandardCharsets;

import java.nio.file.*;

import java.util.*;

import java.util.concurrent.*;

import org.json.*;

import org.junit.Test;

import org.junit.runner.RunWith;

import static org.junit.Assert.*;



/** 별도 UID의 글자 없는 합성 아이콘을 실제 캡처·시범·학습·제스처로 확인합니다. */

@RunWith(AndroidJUnit4.class)

public final class BehaviorVisualDeviceTest {

    static final String TARGET="kr.yunseok.pxe22fixture";

    final GuidedFlowTest h=new GuidedFlowTest();

    final List<String> owned=new ArrayList<>();

    final JSONObject evidence=new JSONObject();

    File evidenceDirectory;

    BehaviorTaskStore store;

    Map<String,?> prefs;

    long started;



    BehaviorAndroidBridge bridge(){try{java.lang.reflect.Field f=MacroService.class.getDeclaredField("activeBehavior");f.setAccessible(true);return (BehaviorAndroidBridge)f.get(h.svc);}catch(Exception e){throw new AssertionError(e);}}

    AccessibilityNodeInfo description(AccessibilityNodeInfo node,String prefix){if(node==null)return null;CharSequence value=node.getContentDescription();if(value!=null&&value.toString().startsWith(prefix))return AccessibilityNodeInfo.obtain(node);for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i),found=description(child,prefix);if(child!=null)child.recycle();if(found!=null)return found;}return null;}

    AccessibilityNodeInfo canvas(){for(AccessibilityWindowInfo window:h.auto.getWindows()){if(window.getType()!=AccessibilityWindowInfo.TYPE_APPLICATION)continue;AccessibilityNodeInfo root=window.getRoot();if(root==null)continue;try{if(TARGET.contentEquals(root.getPackageName())){AccessibilityNodeInfo result=description(root,"pxe_visual_stage_");if(result!=null)return result;}}finally{root.recycle();}}return null;}

    int count(){AccessibilityNodeInfo node=canvas();if(node==null)return -1;try{String s=node.getContentDescription().toString();return Integer.parseInt(s.substring(s.lastIndexOf('_')+1));}finally{node.recycle();}}

    private CaptureTrace captureTrace;
    /** 기존 캡처 통로만 관측하며 원래 전달과 버퍼 수명은 바꾸지 않습니다. */
    private final class CaptureTrace implements MacroService.ScreenshotSource {
        final MacroService service;
        final java.lang.reflect.Field sourceField,nextId;
        final java.lang.reflect.Method activeTime;
        final Map<String,java.lang.reflect.Field> fields=new HashMap<>();
        final MacroService.ScreenshotSource original;
        final JSONArray rows=new JSONArray();
        volatile boolean closed;
        int requests,successes,failures,sourceErrors,omitted,observationErrors;
        String observationErrorType="";
        CaptureTrace(MacroService service)throws Exception{
            this.service=service;sourceField=MacroService.class.getDeclaredField("screenshotSource");sourceField.setAccessible(true);original=(MacroService.ScreenshotSource)sourceField.get(service);
            for(String name:new String[]{"generation","pauseRevision","windowRevision","behaviorContentRevision","behaviorScreenshotAt","pendingRecognition","captureRequests","executionMetrics"}){java.lang.reflect.Field f=MacroService.class.getDeclaredField(name);f.setAccessible(true);fields.put(name,f);}
            nextId=CaptureRequests.class.getDeclaredField("nextId");nextId.setAccessible(true);activeTime=MacroService.class.getDeclaredMethod("activeTime");activeTime.setAccessible(true);
        }
        void install()throws Exception{sourceField.set(service,this);}
        void close()throws Exception{closed=true;Object current=sourceField.get(service);if(current==this)sourceField.set(service,original);else if(current!=original)throw new IllegalStateException("캡처 관측 소유권이 바뀌었습니다");}
        void observationError(Throwable error){synchronized(this){if(closed)return;observationErrors++;String type=error.getClass().getName();observationErrorType=type.length()>128?type.substring(0,128):type;}}
        JSONObject state()throws Exception{
            JSONObject out=new JSONObject().put("uptime_ms",SystemClock.uptimeMillis());
            for(String name:new String[]{"generation","pauseRevision","windowRevision","behaviorContentRevision"})out.put(name,((java.util.concurrent.atomic.AtomicLong)fields.get(name).get(service)).get());
            out.put("screenshot_at",fields.get("behaviorScreenshotAt").getLong(service)).put("paused",service.paused);
            CaptureRequests pending=(CaptureRequests)fields.get("captureRequests").get(service);synchronized(pending){out.put("capture_next_id",nextId.getLong(pending)).put("capture_pending",pending.size());}
            out.put("ocr_pending",((java.util.concurrent.atomic.AtomicReference<?>)fields.get("pendingRecognition").get(service)).get()!=null);return out;
        }
        void event(String kind,int request,long timestamp,int code){
            if(closed)return;
            try{JSONObject row=state().put("event",kind).put("request",request).put("native_timestamp",timestamp).put("failure_code",code);
                synchronized(this){if(closed)return;JSONArray candidate=new JSONArray(rows.toString());candidate.put(row);if(rows.length()>=32||candidate.toString().getBytes(StandardCharsets.UTF_8).length>6144){omitted++;return;}rows.put(row);}
            }catch(Throwable error){observationError(error);}
        }
        long timestamp(android.accessibilityservice.AccessibilityService.ScreenshotResult result){try{return result.getTimestamp();}catch(Throwable error){observationError(error);return -1;}}
        @Override public void take(java.util.concurrent.Executor executor,android.accessibilityservice.AccessibilityService.TakeScreenshotCallback callback){
            final int request;synchronized(this){if(closed){request=0;}else request=++requests;}
            if(request==0){original.take(executor,callback);return;}event("request",request,-1,-1);
            try{original.take(executor,new android.accessibilityservice.AccessibilityService.TakeScreenshotCallback(){
                @Override public void onSuccess(android.accessibilityservice.AccessibilityService.ScreenshotResult result){
                    synchronized(CaptureTrace.this){if(!closed)successes++;}long time=timestamp(result);event("success_before",request,time,-1);
                    try{callback.onSuccess(result);}finally{event("success_after",request,time,-1);}
                }
                @Override public void onFailure(int code){
                    synchronized(CaptureTrace.this){if(!closed)failures++;}event("failure_before",request,-1,code);
                    try{callback.onFailure(code);}finally{event("failure_after",request,-1,code);}
                }
            });}catch(RuntimeException|Error error){synchronized(this){if(!closed)sourceErrors++;}event("source_error",request,-1,-1);throw error;}
        }
        JSONObject snapshot(){
            JSONObject out;
            try{out=state();ExecutionMetrics metrics=(ExecutionMetrics)fields.get("executionMetrics").get(service);ExecutionMetrics.Snapshot value=metrics.snapshot((Long)activeTime.invoke(service));out.put("ocr_count",value.ocrCount).put("ocr_ms",value.ocrMs).put("capture_ms",value.captureMs).put("step_ms",value.stepMs).put("steps",value.steps).put("configured_ms",value.configuredMs);}
            catch(Throwable error){observationError(error);out=new JSONObject();}
            try{synchronized(this){JSONArray copy=new JSONArray(rows.toString());int removed=0;out.put("requests",requests).put("successes",successes).put("failures",failures).put("source_errors",sourceErrors).put("observation_errors",observationErrors).put("observation_error_type",observationErrorType).put("omitted",omitted).put("events",copy);
                    while(out.toString().getBytes(StandardCharsets.UTF_8).length>8192&&copy.length()>0){copy.remove(copy.length()-1);out.put("omitted",omitted+(++removed));}return out;}}
            catch(Throwable error){observationError(error);return new JSONObject();}
        }
    }
    private void installCaptureTrace()throws Exception{captureTrace=new CaptureTrace(h.svc);h.main(()->{try{captureTrace.install();}catch(Exception error){throw new AssertionError(error);}});}
    private void restoreCaptureTrace(){if(captureTrace!=null)h.main(()->{try{captureTrace.close();}catch(Exception error){throw new AssertionError(error);}});}

    final JSONArray observations=new JSONArray();
    boolean fixedTextPresent(AccessibilityNodeInfo root,String text){AccessibilityNodeInfo found=h.search(root,text);if(found==null)return false;found.recycle();return true;}
    /** 자사 코드가 있는 작업 스레드만 고정 메타데이터와 제한된 스택으로 관측합니다. */
    JSONArray fixedWorkerStacks(){
        JSONArray rows=new JSONArray();
        try{
            Thread observer=Thread.currentThread();
            for(Map.Entry<Thread,StackTraceElement[]> entry:Thread.getAllStackTraces().entrySet()){
                Thread thread=entry.getKey();if(thread==observer)continue;
                StackTraceElement[] stack=entry.getValue();boolean own=false;
                for(StackTraceElement frame:stack)if(frame.getClassName().startsWith("kr.yunseok.touchmacro.")){own=true;break;}
                if(!own)continue;
                JSONArray frames=new JSONArray();
                for(int index=0;index<Math.min(20,stack.length);index++){
                    StackTraceElement frame=stack[index];String fixed=frame.getClassName()+"#"+frame.getMethodName()+":"+frame.getLineNumber();
                    frames.put(fixed.length()>256?fixed.substring(0,256):fixed);
                }
                JSONObject row=new JSONObject().put("thread_id",thread.getId()).put("state",thread.getState().name()).put("frames",frames);
                JSONArray candidate=new JSONArray(rows.toString());candidate.put(row);
                if(candidate.toString().getBytes(StandardCharsets.UTF_8).length>8192)continue;
                rows=candidate;if(rows.length()>=8)break;
            }
        }catch(Throwable ignored){}
        return rows;
    }
    /** 개인 화면 문구를 보존하지 않고 고정 시험 표식과 수치만 관측합니다. */
    void tracePreparation(String phase){
        JSONObject row=new JSONObject();long from=SystemClock.elapsedRealtimeNanos();
        try{
            row.put("phase",phase).put("elapsed_start_ns",from).put("uptime_ms",SystemClock.uptimeMillis());
            if("검사 실패".equals(phase)){row.put("worker_stacks",fixedWorkerStacks());if(captureTrace!=null){JSONObject capture=captureTrace.snapshot();row.put("capture_trace",capture);evidence.put("capture_trace",capture);}}
            MacroService service=h.svc;BehaviorAndroidBridge owner=service==null?null:bridge();
            row.put("service_present",service!=null).put("service_same",service!=null&&service==MacroService.get());
            if(service!=null)row.put("connected",service.connectionActive()).put("generation",service.behaviorGeneration()).put("running",service.running).put("paused",service.paused).put("settings_revision",MacroService.behaviorSettings(service)).put("enabled",MacroService.behaviorEnabled(service));
            row.put("bridge_present",owner!=null);
            if(owner!=null)row.put("bridge_busy",owner.busy.get()).put("bridge_live",owner.live()).put("bridge_generation",owner.token).put("bridge_settings",owner.settings).put("bridge_teaching",owner.teaching).put("bridge_target_is_fixture",TARGET.equals(owner.task.target));
            JSONArray windows=new JSONArray();boolean target3=false,visualCanvas=false,scrollCanvas=false;int canvasCount=-1,textCount=-1;
            if(h.auto!=null)for(AccessibilityWindowInfo window:h.auto.getWindows()){
                AccessibilityNodeInfo root=null;
                try{
                    root=window.getRoot();JSONObject entry=new JSONObject().put("id",window.getId()).put("type",window.getType()).put("layer",window.getLayer()).put("active",window.isActive()).put("focused",window.isFocused()).put("root_present",root!=null);
                    if(root!=null){String pkg=String.valueOf(root.getPackageName());String kind=TARGET.equals(pkg)?"fixture":h.a!=null&&h.a.getPackageName().equals(pkg)?"pxe":"other";entry.put("fixed_package_kind",kind);
                        if("fixture".equals(kind)){
                            boolean found=fixedTextPresent(root,"별도 앱 대상 3");entry.put("fixed_target3_present",found);target3|=found;
                            AccessibilityNodeInfo canvas=description(root,"pxe_visual_stage_");
                            if(canvas!=null)try{String value=String.valueOf(canvas.getContentDescription());java.util.regex.Matcher match=java.util.regex.Pattern.compile("pxe_visual_stage_([0-9]+)_clicks_([0-9]+)").matcher(value);if(match.matches()){visualCanvas=true;canvasCount=Integer.parseInt(match.group(2));entry.put("fixed_visual_stage",Integer.parseInt(match.group(1))).put("fixed_visual_clicks",canvasCount);}}finally{canvas.recycle();}
                            AccessibilityNodeInfo scroll=description(root,"fixture_scroll_");if(scroll!=null){scrollCanvas=true;scroll.recycle();}entry.put("fixed_scroll_present",scrollCanvas);
                            for(int count=0;count<=30;count++)if(fixedTextPresent(root,"터치 "+count)){textCount=count;entry.put("fixed_text_counter",count);break;}
                        }else if("pxe".equals(kind))entry.put("fixed_image_button_present",fixedTextPresent(root,"이미지 터치")).put("fixed_wait_button_present",fixedTextPresent(root,"글자대기"));
                    }
                    windows.put(entry);
                }finally{if(root!=null)root.recycle();window.recycle();}
            }
            row.put("windows",windows).put("fixed_target3_present",target3).put("fixed_visual_present",visualCanvas).put("fixed_scroll_present",scrollCanvas).put("fixed_canvas_counter",canvasCount).put("fixed_text_counter",textCount);
        }catch(Throwable error){try{row.put("observation_error_class",error.getClass().getName());}catch(JSONException ignored){}}
        try{row.put("elapsed_end_ns",SystemClock.elapsedRealtimeNanos());if(observations.length()<32)observations.put(row);evidence.put("preparation_observations",observations);android.os.Bundle status=new android.os.Bundle();status.putString("pxe.visual.trace",row.toString());InstrumentationRegistry.getInstrumentation().sendStatus(2,status);}catch(Throwable ignored){}
    }

    void configure(boolean moved,boolean duplicate)throws Exception{tracePreparation("구성 전");h.shell("am start -W --activity-single-top -n "+TARGET+"/.FixtureActivity --ez pxe_visual_mode true --ez pxe_visual_moved "+moved+" --ez pxe_visual_duplicate "+duplicate);tracePreparation("구성 명령 반환");h.waitFor(()->count()==0,6000,"합성 시각 화면 초기화 실패");tracePreparation("구성 준비 관측 완료");}

    Rect canvasBounds(){AccessibilityNodeInfo node=canvas();assertNotNull(node);try{Rect r=new Rect();node.getBoundsInScreen(r);return r;}finally{node.recycle();}}

    String visualStatus(){final String[] value={""};h.main(()->{try{java.lang.reflect.Field field=FloatingController.class.getDeclaredField("behaviorStatus");field.setAccessible(true);android.widget.TextView view=(android.widget.TextView)field.get(h.svc.controller());if(view!=null)value[0]=view.getText().toString();}catch(Exception error){throw new AssertionError(error);}});return value[0];}

    void physicalButton(String text){AccessibilityNodeInfo node=h.find(text);assertNotNull("실제 버튼 없음: "+text,node);Rect r=new Rect();try{assertTrue(node.isVisibleToUser());node.getBoundsInScreen(r);}finally{node.recycle();}assertFalse(r.isEmpty());h.tap(r.exactCenterX(),r.exactCenterY());h.in.waitForIdleSync();}
    /** 실제 제목 드래그로 제어창을 원래 중앙 스크롤 경로 밖으로 옮깁니다. */
    void moveBehaviorBarAboveScroll(BehaviorAndroidBridge owner)throws Exception{
        Rect title=new Rect(),before=new Rect(),after=new Rect();android.graphics.RectF path=new android.graphics.RectF();
        h.main(()->{
            assertTrue(owner.live());assertFalse(owner.busy.get());
            try{java.lang.reflect.Field field=FloatingController.class.getDeclaredField("behaviorStatus");field.setAccessible(true);android.widget.TextView view=(android.widget.TextView)field.get(h.svc.controller());assertNotNull(view);assertTrue(view.isShown());title.set(h.bounds(view));before.set(h.svc.controller().behaviorBounds(owner));}
            catch(ReflectiveOperationException error){throw new AssertionError(error);}
        });
        assertFalse(title.isEmpty());h.drag(title.exactCenterX(),title.exactCenterY(),title.exactCenterX(),title.exactCenterY()-before.top);h.in.waitForIdleSync();
        h.main(()->{
            after.set(h.svc.controller().behaviorBounds(owner));int[] screen=h.svc.size();
            path.set(screen[0]*.50f,screen[1]*.28f,screen[0]*.50f+1,screen[1]*.72f+1);
            assertEquals("제어창 제목 드래그가 상단에 도달하지 않았습니다",0,after.top);
            assertFalse("제어창이 원래 중앙 스크롤 경로와 겹칩니다",android.graphics.RectF.intersects(path,new android.graphics.RectF(after)));
        });
        evidence.put("scroll_bar_drag",new JSONObject().put("before",new JSONArray().put(before.left).put(before.top).put(before.right).put(before.bottom)).put("after",new JSONArray().put(after.left).put(after.top).put(after.right).put(after.bottom)).put("path",new JSONArray().put(path.left).put(path.top).put(path.right).put(path.bottom)).put("overlap",false));
    }


    void writeEvidence()throws Exception{if(evidenceDirectory==null)return;evidence.put("elapsed_ms",SystemClock.elapsedRealtime()-started);try(FileOutputStream out=new FileOutputStream(new File(evidenceDirectory,"result.json"))){out.write(evidence.toString(2).getBytes(StandardCharsets.UTF_8));out.getFD().sync();}}

    BehaviorTaskStore.Task demonstrate(BehaviorTaskStore.Task task,int expectedRevision)throws Exception{

        configure(false,false);h.main(()->h.svc.startBehavior(task,true,null));tracePreparation("시범 시작 반환");h.waitFor(()->bridge()!=null&&!bridge().busy.get()&&count()==0,10000,"시각 시범 준비 실패");tracePreparation("시각 시범 준비 관측 완료");BehaviorAndroidBridge owner=bridge();Rect bounds=canvasBounds();

        physicalButton("이미지 터치");h.tap(bounds.left+bounds.width()*.32f,bounds.top+bounds.height()*.62f);

        h.waitFor(()->count()==1&&!owner.busy.get()&&owner.session.draftCount()==1,20000,"실제 아이콘 터치가 한 번 전달되지 않았습니다");

        java.lang.reflect.Field pending=BehaviorAndroidSession.class.getDeclaredField("pendingBefore");pending.setAccessible(true);BehaviorExample.Observation observed=(BehaviorExample.Observation)pending.get(owner.session);assertNotNull(observed);assertNotNull("서비스의 실제 캡처에서 시각 특징 없음",observed.visual);for(BehaviorExample.Token token:observed.tokens)assertFalse("합성 아이콘 화면 내부에 OCR 글자가 생겼습니다",Rect.intersects(bounds,new Rect(token.left,token.top,token.right,token.bottom)));

        long captured=observed.binding.capturedAtMs;assertTrue(captured>0);assertTrue(SystemClock.uptimeMillis()-captured<15000);

        physicalButton("시범 완료 · 확인하고 학습");h.waitFor(()->!h.svc.running&&bridge()==null,25000,"실제 시각 학습·저장이 끝나지 않았습니다");BehaviorTaskStore.Task saved=store.load(task.storageId,true);assertEquals(expectedRevision,saved.revision);assertNotNull(saved.snapshot.visualPolicy);assertEquals(128L*expectedRevision,saved.snapshot.visualPolicy.completedSteps());assertEquals("시각 시범이 구형 글자 MLP를 덮었습니다",0,saved.snapshot.policy.completedSteps());assertEquals(1,count());return saved;

    }

    @Test public void twoIconDemonstrationsLearnMoveFinishAndAmbiguityStopsWithoutInput()throws Throwable{

        started=SystemClock.elapsedRealtime();Throwable failure=null;String run=InstrumentationRegistry.getArguments().getString("pxe_run_id");assertNotNull("독립 실행 식별자 pxe_run_id 필요",run);assertTrue(run.matches("[A-Za-z0-9_-]{1,64}"));

        try{

            h.boot();installCaptureTrace();prefs=new HashMap<>(Ui.prefs(h.a).getAll());File directory=new File(h.a.getExternalFilesDir(null),"visual-24-"+run);assertFalse(directory.exists());assertTrue(directory.mkdir());evidenceDirectory=directory;evidence.put("run_id",run).put("test","actual_screenshot_ocr_visual_policy_gesture");

            h.main(()->h.svc.setBehaviorEnabled(true));store=new BehaviorTaskStore(h.a.getFilesDir());BehaviorTaskStore.Task task=store.create("합성 시각 시범 검사",TARGET,true);owned.add(task.storageId);

            BehaviorTaskStore.Task once=demonstrate(task,1),twice=demonstrate(once,2);assertEquals(4,twice.replay().active(twice.taskId).size());evidence.put("confirmed_demonstrations",2).put("visual_optimizer_steps",twice.snapshot.visualPolicy.completedSteps());

            byte[] bundle=BehaviorBundle.write(twice.snapshot);BehaviorBundle.Snapshot copied=BehaviorBundle.read(new ByteArrayInputStream(bundle));assertArrayEquals(twice.snapshot.visualPolicy.parameters(),copied.visualPolicy.parameters(),0);evidence.put("bundle_bytes",bundle.length).put("visual_weights_sha256",BehaviorPolicy.hex(BehaviorPolicy.digest(twice.snapshot.visualPolicy.json().toString().getBytes(StandardCharsets.UTF_8))));

            configure(true,false);h.main(()->h.svc.startBehavior(twice,false,null));tracePreparation("자동 수행 시작 반환");h.waitFor(()->!h.svc.running&&bridge()==null,20000,"이동한 아이콘 자동 수행·완료 실패");assertEquals("이동한 현재 아이콘을 정확히 한 번 터치해야 합니다",1,count());SystemClock.sleep(1000);assertEquals("완료 이후 추가 입력",1,count());evidence.put("automatic_moved_icon_clicks",1).put("post_finish_extra_clicks",0);h.screenshot("visual24-"+run+"-finished.png");

            configure(false,true);h.main(()->h.svc.startBehavior(twice,false,null));tracePreparation("자동 수행 시작 반환");h.waitFor(()->bridge()!=null&&h.svc.running,6000,"중복 화면 자동 대기 시작 실패");h.waitFor(()->visualStatus().contains("유일한 이미지 대상 대기"),15000,"실제 중복 이미지 거절 상태를 관측하지 못했습니다");assertEquals("같은 아이콘 두 개에서 임의 터치",0,count());assertTrue(h.svc.running);

            long stop=SystemClock.elapsedRealtime();physicalButton("■ 중단");h.waitFor(()->!h.svc.running&&bridge()==null,1000,"실제 정지 버튼 응답 지연");long stopMs=SystemClock.elapsedRealtime()-stop;SystemClock.sleep(1000);assertEquals("중단 이후 늦은 시각 결과 터치",0,count());evidence.put("duplicate_icon_clicks",0).put("stop_ms",stopMs).put("post_stop_extra_clicks",0).put("passed",true);

        }catch(Throwable error){failure=error;tracePreparation("검사 실패");try{evidence.put("passed",false).put("error",error.toString());}catch(Exception ignored){}}

        finally{

            try{restoreCaptureTrace();}catch(Throwable restore){if(failure==null)failure=restore;else failure.addSuppressed(restore);}try{if(h.svc!=null){h.end();CompletableFuture<Boolean> drained=new CompletableFuture<>();h.svc.behaviorExecute(()->drained.complete(true));drained.get(6000,TimeUnit.MILLISECONDS);}if(h.a!=null&&prefs!=null)restorePrefs();if(h.a!=null)removeOwned();if(h.auto!=null)h.shell("am start -W --activity-single-top -n "+TARGET+"/.FixtureActivity --ez pxe_visual_mode false");evidence.put("local_cleanup",true);writeEvidence();}catch(Throwable cleanup){if(failure==null)failure=cleanup;else failure.addSuppressed(cleanup);}

        }

        if(failure!=null)throw failure;

    }

    AccessibilityNodeInfo targetText(String text){for(AccessibilityWindowInfo window:h.auto.getWindows()){AccessibilityNodeInfo root=window.getRoot();if(root==null)continue;try{if(TARGET.contentEquals(root.getPackageName())){AccessibilityNodeInfo node=h.search(root,text);if(node!=null)return node;}}finally{root.recycle();}}return null;}
    boolean flag(BehaviorAndroidBridge owner,String name){boolean[] value={false};h.main(()->{try{java.lang.reflect.Field field=BehaviorAndroidBridge.class.getDeclaredField(name);field.setAccessible(true);value[0]=field.getBoolean(owner);}catch(Exception error){throw new AssertionError(error);}});return value[0];}
    BehaviorExample.Action pendingAction(BehaviorAndroidBridge owner)throws Exception{java.lang.reflect.Field pending=BehaviorAndroidSession.class.getDeclaredField("pendingAction");pending.setAccessible(true);String id=(String)pending.get(owner.session);java.lang.reflect.Field field=BehaviorAndroidBridge.class.getDeclaredField("registry");field.setAccessible(true);BehaviorExample.Registry registry=(BehaviorExample.Registry)field.get(owner);return registry.get(registry.indexOf(id));}
    @Test public void visualThenWaitDoesNotTouchAndReverseSelectionUsesVisual()throws Throwable{
        started=SystemClock.elapsedRealtime();Throwable failure=null;String run=InstrumentationRegistry.getArguments().getString("pxe_run_id");assertNotNull(run);assertTrue(run.matches("[A-Za-z0-9_-]{1,64}"));
        try{
            h.boot();installCaptureTrace();prefs=new HashMap<>(Ui.prefs(h.a).getAll());File directory=new File(h.a.getExternalFilesDir(null),"visual-mode24-"+run);assertFalse(directory.exists());assertTrue(directory.mkdir());evidenceDirectory=directory;evidence.put("run_id",run).put("test","actual_wait_visual_selection_modes");
            h.main(()->h.svc.setBehaviorEnabled(true));store=new BehaviorTaskStore(h.a.getFilesDir());BehaviorTaskStore.Task task=store.create("합성 시범 모드 검사",TARGET,true);owned.add(task.storageId);h.shell("am start -W --activity-single-top -n "+TARGET+"/.FixtureActivity --ez pxe_visual_mode false");h.main(()->h.svc.startBehavior(task,true,null));tracePreparation("시범 시작 반환");h.waitFor(()->bridge()!=null&&!bridge().busy.get(),10000,"시범 준비 실패");tracePreparation("선택 모드 시범 준비 관측 완료");BehaviorAndroidBridge owner=bridge();
            physicalButton("이미지 터치");physicalButton("글자대기");assertFalse(flag(owner,"visualLabel"));assertTrue(flag(owner,"waitLabel"));AccessibilityNodeInfo button=targetText("별도 앱 대상 3");assertNotNull(button);Rect bounds=new Rect();try{button.getBoundsInScreen(bounds);}finally{button.recycle();}CompletableFuture<BehaviorAndroidBridge.Frame> probe=new CompletableFuture<>();h.svc.behaviorExecute(()->{try{probe.complete(owner.frame());}catch(Throwable error){probe.completeExceptionally(error);}});BehaviorAndroidBridge.Frame actual=probe.get(15000,TimeUnit.MILLISECONDS);BehaviorExample.Token unique=null;for(BehaviorExample.Token candidate:actual.observation.tokens)if(bounds.contains((candidate.left+candidate.right)/2,(candidate.top+candidate.bottom)/2)){try{BehaviorExample.Token resolved=BehaviorFeatures.resolveCurrent(actual.observation.tokens,candidate.raw);List<BehaviorExample.Token> proposed=BehaviorFeatures.proposeAt(actual.observation.tokens,(resolved.left+resolved.right)/2d,(resolved.top+resolved.bottom)/2d);if(proposed.size()==1&&proposed.get(0).raw.equals(resolved.raw)){unique=resolved;break;}}catch(IllegalArgumentException ambiguous){}}assertNotNull("실제 OCR의 유일한 대기 글자 후보가 없습니다",unique);h.tap((unique.left+unique.right)/2f,(unique.top+unique.bottom)/2f);
            h.waitFor(()->!owner.busy.get()&&owner.session.draftCount()==1,20000,"실제 OCR 글자 대기 시범 완료 실패");assertEquals(BehaviorExample.Type.WAIT_TEXT,pendingAction(owner).type);AccessibilityNodeInfo zero=targetText("터치 0");assertNotNull("글자 대기가 실제 터치를 전달했습니다",zero);zero.recycle();java.lang.reflect.Field deliveries=BehaviorAndroidBridge.class.getDeclaredField("deliveries");deliveries.setAccessible(true);JSONArray waitDeliveries=new JSONArray(deliveries.get(owner).toString());assertEquals(1,waitDeliveries.length());assertEquals("OBSERVED_WAIT",waitDeliveries.getJSONObject(0).getString("result"));evidence.put("wait_actual_target_clicks",0).put("wait_delivery","OBSERVED_WAIT");
            // 처리 중 거절된 선택은 이미 선택한 대기 모드를 바꾸지 않습니다.
            physicalButton("글자대기");h.main(()->owner.busy.set(true));try{physicalButton("이미지 터치");physicalButton("뒤로");assertFalse(flag(owner,"visualLabel"));assertTrue(flag(owner,"waitLabel"));}finally{h.main(()->owner.busy.set(false));}
            physicalButton("이미지 터치");assertTrue(flag(owner,"visualLabel"));assertFalse(flag(owner,"waitLabel"));configure(false,false);Rect canvas=canvasBounds();h.tap(canvas.left+canvas.width()*.32f,canvas.top+canvas.height()*.62f);h.waitFor(()->count()==1&&!owner.busy.get()&&owner.session.draftCount()==2,20000,"역순 선택의 실제 이미지 터치 실패");assertEquals(BehaviorExample.Type.VISUAL_TAP,pendingAction(owner).type);evidence.put("reverse_visual_target_clicks",1);
            moveBehaviorBarAboveScroll(owner);physicalButton("글자대기");physicalButton("아래로");h.waitFor(()->!owner.busy.get()&&owner.session.draftCount()==3,20000,"수락된 일반 동작 완료 실패");assertFalse(flag(owner,"visualLabel"));assertFalse(flag(owner,"waitLabel"));assertEquals(1,count());physicalButton("■ 중단");h.waitFor(()->!h.svc.running&&bridge()==null,1000,"모드 검사 정지 실패");evidence.put("busy_rejected_mode_preserved",true).put("accepted_scroll_mode_cleared",true).put("passed",true);
        }catch(Throwable error){failure=error;tracePreparation("검사 실패");try{evidence.put("passed",false).put("error",error.toString());}catch(Exception ignored){}}
        finally{try{restoreCaptureTrace();}catch(Throwable restore){if(failure==null)failure=restore;else failure.addSuppressed(restore);}try{if(h.svc!=null){h.end();CompletableFuture<Boolean> drained=new CompletableFuture<>();h.svc.behaviorExecute(()->drained.complete(true));drained.get(6000,TimeUnit.MILLISECONDS);}if(h.a!=null&&prefs!=null)restorePrefs();if(h.a!=null)removeOwned();if(h.auto!=null)h.shell("am start -W --activity-single-top -n "+TARGET+"/.FixtureActivity --ez pxe_visual_mode false");evidence.put("local_cleanup",true);writeEvidence();}catch(Throwable cleanup){if(failure==null)failure=cleanup;else failure.addSuppressed(cleanup);}}
        if(failure!=null)throw failure;
    }
    private void restorePrefs(){SharedPreferences.Editor editor=Ui.prefs(h.a).edit().clear();for(var entry:prefs.entrySet()){String k=entry.getKey();Object v=entry.getValue();if(v instanceof String)editor.putString(k,(String)v);else if(v instanceof Boolean)editor.putBoolean(k,(Boolean)v);else if(v instanceof Integer)editor.putInt(k,(Integer)v);else if(v instanceof Long)editor.putLong(k,(Long)v);else if(v instanceof Float)editor.putFloat(k,(Float)v);else if(v instanceof Set){Set<String> values=new HashSet<>();for(Object x:(Set<?>)v)values.add((String)x);editor.putStringSet(k,values);}else throw new AssertionError("미지원 설정 자료형");}assertTrue(editor.commit());}

    private void removeOwned()throws Exception{Path root=new File(h.a.getFilesDir(),"behavior").toPath().toAbsolutePath().normalize();for(String id:owned){assertTrue(id.matches("[a-f0-9-]{36}"));Path directory=root.resolve(id).normalize();assertTrue(directory.startsWith(root)&&!directory.equals(root));if(!Files.exists(directory))continue;try(java.util.stream.Stream<Path> paths=Files.walk(directory)){List<Path> all=new ArrayList<>();paths.forEach(all::add);all.sort(Comparator.reverseOrder());for(Path p:all){assertFalse(Files.isSymbolicLink(p));Files.delete(p);}}}}

}

