package kr.yunseok.touchmacro;
import android.accessibilityservice.*;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.*;
import android.widget.*;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
public class MacroService extends AccessibilityService {
    private static volatile java.lang.ref.WeakReference<MacroService> current=new java.lang.ref.WeakReference<>(null);
    public static MacroService get(){return current.get();}
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ThreadLocal<AiStartGate> firstAiInput=new ThreadLocal<>();
    interface AiInputBoundaryObserver {void reached(String phase)throws Exception;}
    private AiInputBoundaryObserver aiInputBoundaryObserver;
    private void observeAiInputBoundary(String phase)throws Exception{AiStartGate gate=firstAiInput.get();if(gate!=null&&!gate.inputTarget().isEmpty()&&aiInputBoundaryObserver!=null)aiInputBoundaryObserver.reached(phase);}
    private final QueryLane screenReader=new QueryLane();
    private final QueryLane windowReader=new QueryLane();
    private final WindowPackages windowPackages=new WindowPackages();
    interface WindowSource {List<WindowPackages.Window> read();}
    private WindowSource windowSource=this::windowSnapshot;
    interface ScreenSource {ArrayList<Hit> read(long token)throws Exception;}
    private ScreenSource accessibilitySource=this::accessibleSnapshot;
    private volatile boolean accessibleFallback=false;
    private volatile long queryStartedAt=0;
    private final CapturePacer capturePacer=new CapturePacer(SystemClock.elapsedRealtime(),Build.VERSION.SDK_INT<=30?1100:500);
    private final CaptureRequests captureRequests=new CaptureRequests();
    private final AtomicReference<RecognitionRequest> pendingRecognition=new AtomicReference<>();
    private long recognitionRetryDelay=250;
    private volatile String capturePhase="";
    private volatile String recognitionPhase="";
    private volatile String recognitionStage="",requestedSource="",actualSource="",recognitionNote="";
    private volatile String targetWindowRecovery="";
    private String installedVersion="app_version=unknown, version_code=unknown";
    interface ScreenshotSource {void take(java.util.concurrent.Executor executor,TakeScreenshotCallback callback);}
    private ScreenshotSource screenshotSource=(executor,callback)->takeScreenshot(Display.DEFAULT_DISPLAY,executor,callback);
    private final AtomicLong generation=new AtomicLong();private final AtomicLong pauseRevision=new AtomicLong();private final AtomicLong windowRevision=new AtomicLong();
    private AiCancellation pendingAi;
    private volatile BehaviorAndroidBridge activeBehavior;
    private final AtomicLong behaviorContentRevision=new AtomicLong();
    private volatile long behaviorScreenshotAt;
    /** 정적 계획의 연결·실행·일시정지 상태입니다. 창 세대는 진단값이며 입력 허가는 새 실행에서 받습니다. */
    public static final class AiLease {
        private final MacroService service;private final FloatingController controller;private final long generation,pause,window;
        private AiLease(MacroService service){this.service=service;this.controller=service.controller;generation=service.generation.get();pause=service.pauseRevision.get();window=service.windowRevision.get();}
    }
    public synchronized AiLease captureAiLease(){if(!connectionActive())throw new IllegalStateException("실행하려면 PXE 접근성을 연결하세요");if(controller!=null&&controller.recording)throw new IllegalStateException("녹화를 먼저 끝내세요");return new AiLease(this);}
    public synchronized boolean validAiLease(AiLease lease){return lease!=null&&lease.service==this&&lease.controller==controller&&connectionActive()&&(controller==null||!controller.recording)&&lease.generation==generation.get()&&lease.pause==pauseRevision.get();}
    public synchronized void attachAi(AiLease lease,AiCancellation cancellation){cancellation.check();if(!validAiLease(lease))throw new CancellationException("AI 계획의 실행 상태가 바뀌었습니다");invalidateAi();pendingAi=cancellation;}
    private void invalidateAi(){if(pendingAi!=null){pendingAi.cancel();pendingAi=null;}}
    public synchronized void commitAi(AiLease lease,AiPlanCompiler.Compiled compiled){
        Macro macro=compiled.macro();WatchConfig watch=compiled.watch();
        if(watch==null)macro.validate();else{watch.validate();watch.prepareMacro(macro);}
        AiStartGate gate=watch==null?AiStartGate.macro(macro,compiled.executionTarget(),getPackageName()):AiStartGate.watch(watch,compiled.executionTarget(),getPackageName());
        if(getPackageManager().getLaunchIntentForPackage(gate.target())==null)throw new IllegalArgumentException("선택한 실행 앱을 열 수 없습니다. 대상 앱을 다시 선택하세요");
        if(!validAiLease(lease))throw new CancellationException("실행·일시정지·대상 또는 접근성 연결이 바뀌어 계획을 적용하지 않았습니다");
        if(watch==null)start(macro,gate);else{watch.targetPackage=gate.target();startPreparedWatch(watch,watch.prepareMacro(macro),gate);}
    }
    private final AtomicLong windowQuerySequence=new AtomicLong();
    private final AtomicLong readRecoveryRevision=new AtomicLong();
    private volatile String missingRecovery="";
    private final WindowObservation observedWindow=new WindowObservation(windowRevision);
    public volatile boolean running=false, paused=false, watching=false; public volatile int watchCompleted=0; public volatile String watchPhase=""; private volatile long watchNextRefresh=0;
    public volatile String status="준비";
    private WindowManager wm;private FloatingController controller;
    private volatile long startedAt=0,finishedAt=0,cycleStartedAt=0,lastCycleMs=0,delayUntil=0;
    private volatile String liveAction="준비";
    private volatile String lastAction="";
    private volatile RunProgress runProgress=new RunProgress(-1,0,0,0,0,"");
    private volatile String runOutcome="준비";
    private volatile int currentPriority=0;
    private volatile long pauseStartedAt=0,pausedTotalMs=0;
    private volatile ExecutionMetrics executionMetrics=new ExecutionMetrics();
    private final ArrayDeque<String> logs=new ArrayDeque<>();
    private TextRecognizer recognizer;
    private int initialW,initialH;private long deadline;
    private volatile boolean destroyed=false,connected=false;private boolean logUpdateQueued=false;
    private ErrorJournal errorJournal;
    private void notification(){if(controller!=null)controller.show();}
    private void clearNotification(){}
    @Override protected void onServiceConnected(){if(controller!=null)controller.dispose();if(recognizer!=null)recognizer.close();connected=true;current=new java.lang.ref.WeakReference<>(this);errorJournal=new ErrorJournal(getFilesDir());installedVersion=readInstalledVersion();wm=(WindowManager)getSystemService(WINDOW_SERVICE);controller=new FloatingController(this);recognizer=TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());log("접근성 서비스 연결됨");}
    boolean connectionActive(){return connected&&!destroyed&&get()==this;}
    private void releaseConnection(String reason){
        connected=false;if(get()==this)current=new java.lang.ref.WeakReference<>(null);stop();if(controller!=null)controller.dispose();if(!reason.isEmpty())log(reason);
    }
    @Override public boolean onUnbind(Intent intent){releaseConnection("접근성 연결 해제됨");return super.onUnbind(intent);}
    void overlayTokenInvalid(WindowManager.BadTokenException error){
        recordError("ACCESSIBILITY_OVERLAY_TOKEN_INVALID",error.toString());releaseConnection("접근성 창 연결을 사용할 수 없어 정지 · PXE 접근성을 다시 켜세요");
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(event==null)return;
        int type=event.getEventType(),id=event.getWindowId();long time=event.getEventTime();
        // 창 전환 뒤 대기열에 남은 글자 터치 허가를 메인 스레드에서 기다림 없이 폐기합니다.
        if(type==AccessibilityEvent.TYPE_WINDOWS_CHANGED||type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)windowRevision.incrementAndGet();
        BehaviorAndroidBridge behavior=activeBehavior;
        if(behavior!=null&&event.getPackageName()!=null&&behavior.task.target.contentEquals(event.getPackageName())&&(type==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED||type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED))behaviorContentRevision.incrementAndGet();
        if(type==AccessibilityEvent.TYPE_WINDOWS_CHANGED){if((event.getWindowChanges()&AccessibilityEvent.WINDOWS_CHANGE_REMOVED)!=0)windowPackages.remove(id,time);return;}
        if(type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED||type==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED){CharSequence name=event.getPackageName();if(name!=null)windowPackages.observe(id,name.toString(),time);}
    }
    @Override public void onInterrupt(){
        // 피드백 중단은 접근성 연결 해제가 아니며, 전달한 제스처의 결과도 바꾸지 않습니다.
        if(connectionActive()&&running){recordError("ACCESSIBILITY_FEEDBACK_INTERRUPTED");log("접근성 알림 중단 · 현재 실행은 계속됩니다");}
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration cfg){super.onConfigurationChanged(cfg);if(running){stop();log("화면 방향 변경 · 정지");}if(controller!=null){controller.cancelRegion();if(controller.recording)controller.endRecording();controller.refresh();}}
    @Override public void onDestroy(){
        destroyed=true;releaseConnection("");if(recognizer!=null)recognizer.close();worker.shutdownNow();screenReader.close();windowReader.close();super.onDestroy();
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private void font(View v){if(v instanceof TextView)((TextView)v).setTypeface(getResources().getFont(R.font.nanum_gothic_bold));if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)font(((ViewGroup)v).getChildAt(i));}
    public synchronized String logText(){return String.join("\n",logs);}
    public synchronized void log(String s){
        String line=new java.text.SimpleDateFormat("HH:mm:ss",Locale.KOREA).format(new java.util.Date())+"  "+s;
        logs.addLast(line);while(logs.size()>150)logs.removeFirst();status=s;
        if(!logUpdateQueued){logUpdateQueued=true;ui.postDelayed(()->{synchronized(MacroService.this){getSharedPreferences("macros",0).edit().putString("last_log",String.join("\n",logs)).apply();logUpdateQueued=false;}if(connectionActive()&&controller!=null)controller.update();},250);}
    }
    public FloatingController controller(){return controller;}
    private void recordError(String kind){recordError(kind,recognitionDetail());}
    private String readInstalledVersion(){
        try{android.content.pm.PackageInfo info=getPackageManager().getPackageInfo(getPackageName(),0);return "app_version="+info.versionName+", version_code="+info.getLongVersionCode();}
        catch(android.content.pm.PackageManager.NameNotFoundException error){return "app_version=unknown, version_code=unknown";}
    }
    private void recordError(String kind,Throwable error){
        StringBuilder types=new StringBuilder();int count=0;
        // 원문 글자·예외 메시지 대신 제한된 예외 타입과 실제 빌드만 기록합니다.
        for(Throwable cause=error;cause!=null&&count++<6;cause=cause.getCause()){if(types.length()>0)types.append('>');types.append(cause.getClass().getSimpleName());}
        recordError(kind,"error_types="+types+(recognitionDetail().isEmpty()?"":", "+recognitionDetail()));
    }
    private void recognitionStage(String stage){recognitionStage=stage;requestedSource="";actualSource="";recognitionNote="";}
    private String recognitionDetail(){return requestedSource.isEmpty()?"":"recognition_stage="+recognitionStage+", requested_source="+requestedSource+", actual_source="+actualSource;}
    private String recognitionSummary(){return actualSource.isEmpty()?"":recognitionStage+" · "+("ACCESS".equals(actualSource)?"화면 정보":"한글 OCR")+("AUTO".equals(requestedSource)?" (자동)":"")+(recognitionNote.isEmpty()?"":" · "+recognitionNote);}
    private String recognitionFailure(Exception error){String message=error.getMessage()==null?error.toString():error.getMessage();String summary=recognitionSummary();return message+(summary.isEmpty()?"":" · "+summary);}
    private void recordError(String kind,String detail){
        try{if(errorJournal!=null)errorJournal.append(kind,installedVersion+(detail.isEmpty()?"":"\n"+detail)+"\n실행="+running+", 일시정지="+paused+", 캡처대기="+captureRequests.size()+", 인식대기="+(pendingRecognition.get()!=null)+", 단계="+(watching?watchPhase:liveAction));}
        catch(java.io.IOException e){log("오류 기록 파일 저장 실패 · 저장 공간을 확인하세요");}
    }
    private String errorKind(Exception error){
        for(Throwable cause=error;cause!=null;cause=cause.getCause()){
            if(cause instanceof CaptureFailure)return "OCR_CAPTURE_ERROR_"+((CaptureFailure)cause).error;
            if(cause instanceof ScreenReadFailure)return "SCREEN_INFORMATION_TIMEOUT";
            if(cause instanceof MlKitException)return "OCR_RECOGNITION_ERROR_"+((MlKitException)cause).getErrorCode();
            if(cause instanceof RecognitionCanceled)return "OCR_RECOGNITION_CANCELED";
        }
        String message=error.getMessage()==null?"":error.getMessage();
        if(message.contains("OCR 인식 응답"))return "OCR_RECOGNITION_TIMEOUT";
        if(message.contains("대상 앱 창 확인"))return "TARGET_WINDOW_TIMEOUT";
        return "RUN_STOP_"+error.getClass().getSimpleName();
    }
    public String activePackage(){
        long sequence=windowQuerySequence.incrementAndGet();List<WindowPackages.Window> windows=windowSource.read();String resolved=windowPackages.resolve(windows,getPackageName());
        return observedWindow.accept(sequence,WindowObservation.key(windows,resolved))?resolved:"";
    }
    private List<WindowPackages.Window> windowSnapshot(){
        List<AccessibilityWindowInfo> windows=getWindows();ArrayList<WindowPackages.Window> result=new ArrayList<>();
        try{for(AccessibilityWindowInfo window:windows)result.add(new WindowPackages.Window(window.getId(),window.getType(),window.isActive(),window.isFocused()));}
        finally{for(AccessibilityWindowInfo window:windows)window.recycle();}return result;
    }
    private synchronized boolean acceptForegroundObservation(long token,long sequence,long revision,String key){
        if(token!=generation.get()||!connectionActive()||!running)throw new CancellationException();
        if(deadline>0&&SystemClock.elapsedRealtime()>deadline)throw new IllegalStateException("최대 실행 시간에 도달했습니다");
        if(paused||revision!=pauseRevision.get())return false;
        return observedWindow.accept(sequence,key);
    }
    private synchronized void markReadRecovery(long token)throws Exception{
        // 현재 worker만 복구를 표시하며 늦은 Task·캡처 콜백은 새 실행의 시간을 바꾸지 않습니다.
        check(token);readRecoveryRevision.incrementAndGet();
    }
    private String foregroundPackage(long token)throws Exception{
        boolean recovering=false;
        try{for(;;){
            ready(token);long sequence=windowQuerySequence.incrementAndGet(),revision=pauseRevision.get();CompletableFuture<List<WindowPackages.Window>> result=windowReader.submit(windowSource::read);
            try{
                List<WindowPackages.Window> windows=await(result,token,1500,"대상 앱 창 확인 응답이 지연되었습니다");
                ready(token);String foreground=windowPackages.resolve(windows,getPackageName());
                // 취소·지연 결과와 더 새로운 조회보다 늦은 결과는 관측 세대를 덮지 않습니다.
                if(acceptForegroundObservation(token,sequence,revision,WindowObservation.key(windows,foreground))){if(recovering)log("대상 화면 응답 복구됨 · 새 화면으로 다시 확인합니다");return foreground;}
            }catch(Exception error){
                // 실행 정지·연결 해제·전체 기한은 개별 조회의 취소와 구분합니다.
                check(token);if(!running)throw new CancellationException();
                if(!queryCanRecover(error)&&!windowQueryCanceled(error))throw error;
                targetWindowRecovery="대상 화면 확인 · 응답 복구 대기 · 정지 가능";
                if(!recovering){markReadRecovery(token);pauseRevision.incrementAndGet();recordError(windowQueryCanceled(error)?"TARGET_WINDOW_QUERY_CANCELED":"TARGET_WINDOW_TIMEOUT",error);log("대상 화면 응답 복구 대기 · 터치 없이 기다리는 중 · 정지 가능");recovering=true;}
            }finally{result.cancel(false);}
            sleep(250,token);
        }}finally{if(token==generation.get())targetWindowRecovery="";}
    }
    private static boolean queryCanRecover(Throwable error){for(Throwable cause=error;cause!=null;cause=cause.getCause())if(cause instanceof QueryLane.Busy||cause instanceof AwaitTimeout)return true;return false;}
    private static boolean windowQueryCanceled(Throwable error){for(Throwable cause=error;cause!=null;cause=cause.getCause())if(cause instanceof CancellationException)return true;return false;}
    public boolean hasRunStats(){return startedAt>0;}
    private synchronized long activeTime(){long now=SystemClock.elapsedRealtime();return now-pausedTotalMs-(paused&&pauseStartedAt>0?Math.max(0,now-pauseStartedAt):0);}
    private synchronized boolean resetStats(long token){
        // 이전 실행의 제한 시간은 확인하지 않고, 정지한 큐 작업의 초기화부터 차단합니다.
        if(token!=generation.get()||!connectionActive()||!running)return false;
        startedAt=SystemClock.elapsedRealtime();finishedAt=0;cycleStartedAt=0;lastCycleMs=0;delayUntil=0;currentPriority=0;liveAction="시작 준비";lastAction="";
        if(!paused)pauseStartedAt=0;else if(pauseStartedAt==0)pauseStartedAt=startedAt;
        pausedTotalMs=0;executionMetrics=new ExecutionMetrics();accessibleFallback=false;recognitionStage("");return true;
    }
    private void finishStats(){executionMetrics.finish(activeTime());finishedAt=SystemClock.elapsedRealtime();delayUntil=0;}
    public String performanceText(){ExecutionMetrics.Snapshot measured=executionMetrics.snapshot(activeTime());return String.format(Locale.KOREA,"단계 %d회 · %.1f초 (하위 시간 포함)\nOCR %d회 · %.1f초 · 캡처·간격 %.1f초\n설정 대기 %.1f초",measured.steps,measured.stepMs/1000.0,measured.ocrCount,measured.ocrMs/1000.0,measured.captureMs/1000.0,measured.configuredMs/1000.0);}
    private String remainingText(long ms){long seconds=Math.max(0,(ms+999)/1000);return seconds>=60?(seconds/60)+"분 "+(seconds%60)+"초":seconds+"초";}
    public String progressText(){
        String phase=watching&&!watchPhase.equals("매크로 실행")?watchPhase:liveAction;
        if(running&&!targetWindowRecovery.isEmpty())phase=targetWindowRecovery;
        if(watching&&currentPriority>0)phase=currentPriority+"순위 · "+phase;
        if(running&&queryStartedAt>0)phase+=" · 화면 정보 읽는 중 "+String.format(Locale.KOREA,"%.1f초",(SystemClock.elapsedRealtime()-queryStartedAt)/1000.0);
        if(running&&!capturePhase.isEmpty())phase+=" · "+capturePhase+(delayUntil>SystemClock.elapsedRealtime()?" · "+remainingText(delayUntil-SystemClock.elapsedRealtime()):"");
        if(running&&!recognitionPhase.isEmpty())phase+=" · "+recognitionPhase;
        if(running&&!actualSource.isEmpty())phase+=" · "+recognitionSummary();
        if(!running)phase=status;if(paused)return "일시정지 · "+phase;
        boolean refreshWait=watching&&(watchPhase.equals("완료 후 쉬는 중")||watchPhase.equals("새로고침 대기")||watchPhase.equals("중복 차단 · 사라짐 대기"));
        if(refreshWait)phase+=" · 새로고침까지 "+remainingText(watchNextRefresh-SystemClock.elapsedRealtime());
        else if(running&&delayUntil>SystemClock.elapsedRealtime()&&(!watching||watchPhase.equals("매크로 실행")||watchPhase.startsWith("새로고침")||watchPhase.equals("시작 대기")||watchPhase.startsWith("뒤로가기 완료")))phase+=" · "+String.format(Locale.KOREA,"%.1f초 남음",(delayUntil-SystemClock.elapsedRealtime())/1000.0);
        return phase+(lastAction.isEmpty()?"":"\n직전: "+lastAction);
    }
    public RunProgress hudProgress(){return runProgress;}
    private synchronized void displayProgress(long token,RunProgress value){if(token==generation.get()&&running&&connectionActive())runProgress=value;}
    private synchronized void displayOutcome(long token,String value){if(token==generation.get()&&running&&connectionActive())runOutcome=value;}
    public String hudState(){
        if(!running)return runOutcome;if(paused)return "일시정지";
        if(!targetWindowRecovery.isEmpty())return "복구 대기";
        if(watching&&!watchPhase.equals("매크로 실행"))return watchPhase.equals("완료 후 쉬는 중")||watchPhase.equals("새로고침 대기")||watchPhase.equals("중복 차단 · 사라짐 대기")?"대기":"탐지 중";
        return "실행 중";
    }
    public String hudStage(){
        if(!running)return status;
        if(!targetWindowRecovery.isEmpty())return targetWindowRecovery;
        if(!missingRecovery.isEmpty())return missingRecovery;
        if(watching&&!watchPhase.equals("매크로 실행")){
            boolean waiting=watchPhase.equals("완료 후 쉬는 중")||watchPhase.equals("새로고침 대기")||watchPhase.equals("중복 차단 · 사라짐 대기");
            return waiting?"새로고침까지 "+remainingText(watchNextRefresh-SystemClock.elapsedRealtime()):watchPhase;
        }
        if(!capturePhase.isEmpty())return "화면 준비 · "+runProgress.stage();
        if(!recognitionPhase.isEmpty()||queryStartedAt>0)return "글자 인식 · "+runProgress.stage();
        return runProgress.step>0?runProgress.stage():liveAction;
    }
    public String timingText(){long now=finishedAt>0?finishedAt:SystemClock.elapsedRealtime(),total=startedAt==0?0:Math.max(0,now-startedAt),cycle=cycleStartedAt==0?0:Math.max(0,now-cycleStartedAt);return "이번 한 바퀴 "+String.format(Locale.KOREA,"%.1f초",cycle/1000.0)+" · 직전 "+(lastCycleMs==0?"—":String.format(Locale.KOREA,"%.1f초",lastCycleMs/1000.0))+"\n전체 경과 "+(total/60000)+"분 "+(total/1000%60)+"초 · 완료 "+watchCompleted+"바퀴";}
    public String hudTimingText(){long now=finishedAt>0?finishedAt:SystemClock.elapsedRealtime(),total=startedAt==0?0:Math.max(0,now-startedAt),cycle=cycleStartedAt==0?0:Math.max(0,now-cycleStartedAt);return "이번 "+String.format(Locale.KOREA,"%.1f초",cycle/1000.0)+" · 직전 "+(lastCycleMs==0?"—":String.format(Locale.KOREA,"%.1f초",lastCycleMs/1000.0))+"\n전체 "+(total/60000)+"분 "+(total/1000%60)+"초 · "+watchCompleted+"바퀴 완료";}
    // 설정 안내가 숨겼던 컨트롤도 실제 실행 중에는 정지 접근을 복원합니다.
    public void showControls(){ui.post(()->{if(connectionActive()&&controller!=null){controller.show();if(running)controller.setVisible(true);}});}
    public void hideControls(){ui.post(()->{if(controller!=null)controller.close();});}
    public void pickPoint(){ui.post(()->controller.beginPoint());}
    public int[] size(){android.util.DisplayMetrics m=new android.util.DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(m);return new int[]{m.widthPixels,m.heightPixels};}
    public synchronized void togglePause(){if(!running)return;invalidateAi();paused=!paused;if(paused)pauseStartedAt=SystemClock.elapsedRealtime();else if(pauseStartedAt>0){long duration=SystemClock.elapsedRealtime()-pauseStartedAt;pausedTotalMs+=duration;if(watching)watchNextRefresh+=duration;pauseStartedAt=0;}pauseRevision.incrementAndGet();log(paused?"일시정지":"계속 실행");}
    public String clockText(){if(watching&&!watchPhase.equals("매크로 실행")){long seconds=Math.max(0,(watchNextRefresh-SystemClock.elapsedRealtime()+999)/1000);return "탐지 "+watchCompleted+"회 · "+watchPhase+" · 새로고침 "+seconds+"초";}if(!running)return Store.load(this).steps.size()+"개 동작 · "+(controller!=null&&controller.recording?"녹화 중":"준비");long elapsed=Math.max(0,SystemClock.elapsedRealtime()-startedAt)/1000;String time=String.format(Locale.KOREA,"%02d:%02d:%02d",elapsed/3600,elapsed/60%60,elapsed%60);if(deadline>0){long rem=Math.max(0,deadline-SystemClock.elapsedRealtime())/1000;return time+" · 남은 "+String.format(Locale.KOREA,"%02d:%02d",rem/60,rem%60);}return time;}
    public void replayRecorded(Step step,Runnable complete){
        try{GestureDescription g=description(GesturePaths.forStep(step),size()[0],size()[1],0);if(!dispatchGesture(g,new GestureResultCallback(){@Override public void onCompleted(GestureDescription d){complete.run();}@Override public void onCancelled(GestureDescription d){log("녹화 제스처 재생 취소");complete.run();}},ui)){log("녹화 제스처 재생 불가");complete.run();}}catch(Exception e){log("재생 실패 · "+e.getMessage());complete.run();}
    }
    public synchronized void start(Macro original){
        start(original,null);
    }
    private void start(Macro original,AiStartGate gate){
        if(!connectionActive())throw new IllegalStateException("PXE 접근성을 다시 켜세요");
        if(controller!=null&&controller.recording)throw new IllegalStateException("녹화를 먼저 끝내세요");original.validate();
        Macro m=new Macro(original.json());long token=replaceRun();runProgress=new RunProgress(m.rounds,0,0,0,m.steps.size(),"");runOutcome="실행 중";accessibleFallback=false;running=true;paused=false;showControls();notification();
        worker.execute(()->run(m,token,gate));
    }
    private long replaceRun(){
        invalidateAi();
        cancelBehavior();
        // 새 입력 검증을 마친 뒤에만 이전 실행을 폐기합니다. 실제 미완료 OCR·캡처는 유지합니다.
        if(running)finishStats();long token=generation.incrementAndGet();pauseRevision.incrementAndGet();
        watching=false;watchPhase="";watchCompleted=0;currentPriority=0;cycleStartedAt=0;deadline=0;initialW=0;initialH=0;
        pauseStartedAt=0;pausedTotalMs=0;delayUntil=0;queryStartedAt=0;capturePhase="";recognitionPhase="";targetWindowRecovery="";missingRecovery="";recognitionRetryDelay=250;
        ui.post(()->{if(token==generation.get()&&connectionActive()&&controller!=null){controller.setCaptureMode(false);controller.restoreGesture();}});
        return token;
    }
    public synchronized void stop(){
        invalidateAi();
        cancelBehavior();
        long token=generation.incrementAndGet();if(running)finishStats();runOutcome="정지";running=false;paused=false;watching=false;watchPhase="";clearNotification();log("정지");
        // 정지 세대가 숨김을 복원하며, 그 뒤 시작한 새 실행의 화면 상태는 바꾸지 않습니다.
        ui.post(()->{if(token==generation.get()&&connectionActive()&&controller!=null){controller.setCaptureMode(false);controller.restoreGesture();}});
    }
    public static boolean behaviorEnabled(Context context){return Ui.prefs(context).getBoolean("behavior_enabled",false);}
    public static long behaviorSettings(Context context){return Ui.prefs(context).getLong("behavior_settings_revision",0);}
    public synchronized void startBehavior(BehaviorTaskStore.Task task,boolean teaching,String correction){
        BehaviorTaskStore.requireEnabled(behaviorEnabled(this));
        if(!connectionActive())throw new IllegalStateException("접근성을 연결하세요");
        if(controller==null||controller.recording)throw new IllegalStateException("녹화를 먼저 끝내세요");
        if(task==null||task.target.equals(getPackageName())||getPackageManager().getLaunchIntentForPackage(task.target)==null)throw new IllegalArgumentException("실행할 대상 앱을 선택하세요");
        if(!teaching&&(task.snapshot==null||task.snapshot.policy.completedSteps()==0))throw new IllegalArgumentException("먼저 직접 시범을 확정해 학습하세요");
        int maxMinutes=Store.load(this).maxMinutes;BehaviorAndroidSession.deadline(SystemClock.elapsedRealtime(),maxMinutes);
        long token=replaceRun();runOutcome="실행 중";running=true;paused=false;watching=false;
        BehaviorAndroidBridge bridge=new BehaviorAndroidBridge(this,task,token,behaviorSettings(this),teaching,correction,maxMinutes);activeBehavior=bridge;showControls();worker.execute(bridge::begin);
    }
    private void cancelBehavior(){BehaviorAndroidBridge old=activeBehavior;activeBehavior=null;if(old!=null)old.cancel();ui.post(()->{if(controller!=null)controller.closeBehavior(old);});}
    synchronized boolean behaviorLive(BehaviorAndroidBridge bridge){return activeBehavior==bridge&&generation.get()==bridge.token&&running&&connectionActive()&&behaviorEnabled(this)&&behaviorSettings(this)==bridge.settings;}
    void behaviorExecute(Runnable job){worker.execute(job);}
    long behaviorPauseRevision(){return pauseRevision.get();}
    long behaviorGeneration(){return generation.get();}
    synchronized void setBehaviorEnabled(boolean enabled){invalidateAi();synchronized(BehaviorTaskStore.class){Ui.prefs(this).edit().putBoolean("behavior_enabled",enabled).putLong("behavior_settings_revision",behaviorSettings(this)+1).apply();}if(activeBehavior!=null)stop();}
    boolean behaviorPauseStale(Throwable error){for(Throwable cause=error;cause!=null;cause=cause.getCause())if(cause instanceof StaleCapture)return true;return false;}
    void behaviorWait(BehaviorAndroidBridge bridge)throws Exception{bridge.check();ready(bridge.token);}
    void behaviorUiLater(BehaviorAndroidBridge bridge,Runnable job){ui.post(()->{if(behaviorLive(bridge))job.run();});}
    void behaviorUi(BehaviorAndroidBridge bridge,Runnable job)throws Exception{onUi(()->{bridge.check();job.run();return true;},bridge.token);}
    void behaviorInitialize(BehaviorAndroidBridge bridge)throws Exception{
        if(!resetStats(bridge.token))throw new CancellationException();int[] dimensions=size();initialW=dimensions[0];initialH=dimensions[1];
        // 기존 매크로의 사용자 최대시간만 읽기 전용으로 적용하며0은 무제한입니다.
        deadline=BehaviorAndroidSession.deadline(SystemClock.elapsedRealtime(),bridge.maxMinutes);ready(bridge.token);liveAction=bridge.teaching?"AI 시범":"AI 자동 수행";
    }
    void behaviorLaunch(BehaviorAndroidBridge bridge)throws Exception{
        onUi(()->{bridge.check();Intent intent=getPackageManager().getLaunchIntentForPackage(bridge.task.target);if(intent==null)throw new IllegalStateException("대상 앱을 열 수 없습니다");startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));return true;},bridge.token);
        for(;;){bridge.check();ready(bridge.token);String pkg=foregroundPackage(bridge.token);if(pkg.equals(bridge.task.target))return;if(!pkg.isEmpty()&&!pkg.equals(getPackageName()))throw new TargetChanged();sleep(100,bridge.token);}
    }
    private String behaviorTarget(BehaviorAndroidBridge bridge)throws Exception{
        boolean recovering=false;for(;;){
        bridge.check();ready(bridge.token);long sequence=windowQuerySequence.incrementAndGet(),revision=pauseRevision.get();
        CompletableFuture<List<WindowPackages.Window>> waiting=windowReader.submit(windowSource::read);
        try{List<WindowPackages.Window> windows=await(waiting,bridge.token,1500,"대상 앱 창 확인 응답이 지연되었습니다");String pkg=windowPackages.resolve(windows,getPackageName());
            if(paused||revision!=pauseRevision.get())continue;
            if(!acceptForegroundObservation(bridge.token,sequence,revision,WindowObservation.key(windows,pkg)))continue;
            BehaviorAndroidSession.TargetReadiness targetReadiness=BehaviorAndroidSession.targetReadiness(pkg,bridge.task.target);
            if(targetReadiness==BehaviorAndroidSession.TargetReadiness.WAIT_TARGET){bridge.message("대상 앱 확인 대기 · 입력 없음 · 중단 가능");sleep(250,bridge.token);continue;}
            if(targetReadiness==BehaviorAndroidSession.TargetReadiness.REJECT_TARGET)throw new TargetChanged();
            int identity=-1;for(WindowPackages.Window window:windows)if(window.type==WindowPackages.APPLICATION&&(window.focused||window.active)){if(identity!=-1&&identity!=window.id)throw new TargetChanged();identity=window.id;}
            if(identity<0)throw new TargetChanged();if(recovering)log("AI 대상 화면 응답 복구 · 이전 허가 폐기");return pkg+":"+identity;
        }catch(Exception error){check(bridge.token);bridge.check();if(!queryCanRecover(error)&&!windowQueryCanceled(error))throw error;
            if(!recovering){markReadRecovery(bridge.token);pauseRevision.incrementAndGet();recordError(windowQueryCanceled(error)?"TARGET_WINDOW_QUERY_CANCELED":"TARGET_WINDOW_TIMEOUT",error);recovering=true;}
            bridge.message("대상 창 응답 복구 대기 · 입력 없음 · 중단 가능");
        }finally{waiting.cancel(false);}
        sleep(250,bridge.token);
        }
    }
    private BehaviorAndroidSession.Stamp behaviorStamp(BehaviorAndroidBridge bridge){return new BehaviorAndroidSession.Stamp(generation.get(),pauseRevision.get(),windowRevision.get(),behaviorContentRevision.get(),behaviorSettings(this),bridge.task.revision,bridge.task.target,behaviorLive(bridge),paused);}
    BehaviorAndroidBridge.Frame behaviorSnapshot(BehaviorAndroidBridge bridge,String previous,long started,long lastDispatch,int sameTextCount)throws Exception{
        for(;;){bridge.check();ready(bridge.token);
            Rect excluded=onUi(()->{bridge.check();controller.behaviorCapture(bridge);return controller.behaviorBounds(bridge);},bridge.token);if(excluded.isEmpty()){sleep(70,bridge.token);continue;}
            String application=behaviorTarget(bridge);BehaviorAndroidSession.Stamp before=behaviorStamp(bridge);long requested=SystemClock.uptimeMillis();behaviorScreenshotAt=0;
            com.google.mlkit.vision.text.Text text;
            try{text=recognizeText(bridge.token,bridge.task.target);}catch(StaleCapture stale){ready(bridge.token);continue;}
            String afterApplication=behaviorTarget(bridge);BehaviorAndroidSession.Stamp after=behaviorStamp(bridge);
            if(!before.matches(after)||!application.equals(afterApplication)||behaviorScreenshotAt<requested)continue;
            List<BehaviorExample.Token> tokens=new ArrayList<>();
            for(com.google.mlkit.vision.text.Text.TextBlock block:text.getTextBlocks()){
                behaviorToken(tokens,block.getText(),block.getBoundingBox(),"BLOCK",excluded);
                for(com.google.mlkit.vision.text.Text.Line line:block.getLines()){
                    behaviorToken(tokens,line.getText(),line.getBoundingBox(),"LINE",excluded);
                    for(com.google.mlkit.vision.text.Text.Element element:line.getElements())behaviorToken(tokens,element.getText(),element.getBoundingBox(),"ELEMENT",excluded);
                }
            }
            long now=SystemClock.uptimeMillis();double[] state={Math.min(1,Math.max(0,now-behaviorScreenshotAt)/1000d),lastDispatch==0?0:Math.min(1,(now-lastDispatch)/600000d),Math.min(1,(now-started)/600000d),Math.min(1,sameTextCount/100d),-1,captureRequests.size()/2d,pendingRecognition.get()==null?0:1,pauseRevision.get()>bridge.initialPause?1:0};
            BehaviorExample.Binding binding=new BehaviorExample.Binding(bridge.task.taskId,bridge.sessionId,bridge.task.target,bridge.settings,bridge.task.revision,after.window,behaviorScreenshotAt,initialW,initialH);
            return new BehaviorAndroidBridge.Frame(new BehaviorExample.Observation(binding,tokens,previous,state),after,excluded,application);
        }
    }
    private void behaviorToken(List<BehaviorExample.Token> tokens,String raw,Rect rect,String level,Rect excluded){
        if(rect==null||rect.isEmpty()||raw.isEmpty()||Rect.intersects(rect,excluded))return;
        if(tokens.size()>=256)throw new IllegalStateException("OCR 후보가256개를 넘어 전체 관측을 거절했습니다");tokens.add(new BehaviorExample.Token(raw,rect.left,rect.top,rect.right,rect.bottom,level));
    }
    void behaviorOverlayFailure(RuntimeException error){recordError("AI_BEHAVIOR_OVERLAY_CLEANUP",error);log("AI 창 정리 실패 · 접근성 연결 상태를 확인하세요");}
    private void restoreBehaviorGesture(FloatingController controls,FloatingController.BehaviorGestureLease lease,Exception primary)throws Exception{
        CompletableFuture<Boolean> restored=new CompletableFuture<>();
        // 정지·일시정지 상태도 정리해야 하므로 입력용 onUi의 ready 대기를 사용하지 않습니다.
        if(!ui.post(()->{try{controls.restoreBehaviorGesture(lease);restored.complete(true);}catch(RuntimeException error){restored.completeExceptionally(error);}}))restored.completeExceptionally(new IllegalStateException("AI 창 복원 작업을 전달하지 못했습니다"));
        try{restored.get(5000,TimeUnit.MILLISECONDS);}
        catch(Exception error){if(error instanceof InterruptedException)Thread.currentThread().interrupt();recordError("AI_BEHAVIOR_SHIELD_RESTORE",error);if(primary!=null)primary.addSuppressed(error);else throw error;}
    }
    String behaviorDispatch(BehaviorAndroidBridge bridge,BehaviorExample.Action action,BehaviorAndroidBridge.Frame frame)throws Exception{
        bridge.check();ready(bridge.token);if(!frame.stamp.matches(behaviorStamp(bridge)))throw new StaleCapture();
        if(action.type==BehaviorExample.Type.WAIT_TEXT){long until=SystemClock.elapsedRealtime()+action.timeoutMs;for(;;){BehaviorAndroidBridge.Frame fresh=bridge.frame();try{BehaviorFeatures.resolveCurrent(fresh.observation.tokens,action.targetRaw);return "OBSERVED_WAIT";}catch(IllegalArgumentException missing){if(!action.waitForever&&SystemClock.elapsedRealtime()>=until)throw missing;sleep(100,bridge.token);}}}
        FloatingController controls=controller;if(controls==null)throw new CancellationException();
        FloatingController.BehaviorGestureLease lease=new FloatingController.BehaviorGestureLease(controls,bridge);Exception primary=null;
        try{
            onUi(()->{bridge.check();controls.prepareBehaviorGesture(lease);return true;},bridge.token);
            String application=behaviorTarget(bridge);BehaviorAndroidSession.Stamp permit=behaviorStamp(bridge);
            // 차단창을 실제 분리한 뒤 같은 application 창·내용·실행을 다시 결합합니다.
            if(!frame.application.equals(application)||!frame.stamp.sameExceptWindow(permit))throw new StaleCapture();
            Rect currentBounds=onUi(()->controller.behaviorBounds(bridge),bridge.token);if(!currentBounds.equals(frame.excluded))throw new StaleCapture();
            if(action.type==BehaviorExample.Type.BACK){return onUi(()->{bridge.check();if(!permit.matches(behaviorStamp(bridge)))throw new StaleCapture();return performGlobalAction(GLOBAL_ACTION_BACK)?"ACCEPTED_GLOBAL":"REJECTED";},bridge.token);}
            double x=50,y=action.type==BehaviorExample.Type.SCROLL_UP?75:28,x2=50,y2=action.type==BehaviorExample.Type.SCROLL_UP?52.5:72;int duration=action.type==BehaviorExample.Type.SCROLL_UP?450:500;
            if(action.type==BehaviorExample.Type.TEXT_TAP){BehaviorExample.Token hit=BehaviorFeatures.resolveCurrent(frame.observation.tokens,action.targetRaw);Rect rect=new Rect(hit.left,hit.top,hit.right,hit.bottom);if(Rect.intersects(rect,currentBounds))throw new IllegalStateException("제어창이 대상을 가립니다. 창을 옮겨 주세요");x=x2=rect.exactCenterX()*100/initialW;y=y2=rect.exactCenterY()*100/initialH;duration=70;}
            else if(action.type!=BehaviorExample.Type.SCROLL_UP&&action.type!=BehaviorExample.Type.SCROLL_DOWN)throw new IllegalArgumentException("직접 전달할 수 없는 행동");
            RectF pathBounds=new RectF((float)Math.min(x,x2)*initialW/100,(float)Math.min(y,y2)*initialH/100,(float)Math.max(x,x2)*initialW/100+1,(float)Math.max(y,y2)*initialH/100+1);if(RectF.intersects(pathBounds,new RectF(currentBounds)))throw new IllegalStateException("제어창이 입력 경로를 가립니다. 창을 옮겨 주세요");
            org.json.JSONArray points=new org.json.JSONArray().put(GesturePaths.point(x,y));if(x!=x2||y!=y2)points.put(GesturePaths.point(x2,y2));GestureDescription description=description(new org.json.JSONArray().put(GesturePaths.stroke(points,0,duration)),initialW,initialH,0);
            CompletableFuture<String> result=new CompletableFuture<>();ui.post(()->{try{synchronized(MacroService.this){if(result.isDone())return;bridge.check();check(bridge.token);if(!permit.matches(behaviorStamp(bridge)))throw new StaleCapture();if(!dispatchGesture(description,new GestureResultCallback(){@Override public void onCompleted(GestureDescription d){result.complete("COMPLETED");}@Override public void onCancelled(GestureDescription d){result.complete("CANCELLED");}},ui))result.complete("REJECTED");}}catch(Exception error){result.completeExceptionally(error);}});
            return await(result,bridge.token,65000);
        }catch(Exception error){primary=error;throw error;}finally{restoreBehaviorGesture(controls,lease,primary);}
    }
    void behaviorCommit(BehaviorAndroidBridge bridge,BehaviorAndroidBridge.Checked commit)throws Exception{for(;;){bridge.check();ready(bridge.token);synchronized(this){bridge.check();if(paused)continue;commit.run();return;}}}
    void behaviorFinished(BehaviorAndroidBridge bridge,String outcome,String message,Exception error){
        synchronized(this){
            // 현재 실행의 종료만 먼저 기록하고, 늦은 이전 결과는 로그에도 반영하지 않습니다.
            if(!behaviorLive(bridge))return;
            runOutcome=outcome;
            if(error!=null)recordError(errorKind(error),error);
            log(message);finishStats();running=false;paused=false;activeBehavior=null;bridge.cancel();
            ui.post(()->{if(controller!=null){controller.closeBehavior(bridge);controller.update();}});
        }
    }
    private void check(long token)throws Exception {
        if(token!=generation.get()||!connectionActive())throw new CancellationException();
        if(deadline>0&&SystemClock.elapsedRealtime()>deadline)throw new IllegalStateException("최대 실행 시간에 도달했습니다");
        if(!((PowerManager)getSystemService(POWER_SERVICE)).isInteractive())throw new IllegalStateException("화면이 꺼져 실행을 중단했습니다");
        if(((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked())throw new IllegalStateException("잠금 화면에서는 실행할 수 없습니다");
        int[] s=size();if(initialW>0&&(initialW!=s[0]||initialH!=s[1]))throw new IllegalStateException("화면 방향이 바뀌었습니다. 같은 방향에서 다시 실행하세요");
    }
    private void ready(long token)throws Exception{check(token);while(paused){Thread.sleep(70);check(token);}}
    private void sleep(long ms,long token)throws Exception{
        long remaining=ms;delayUntil=SystemClock.elapsedRealtime()+remaining;try{while(remaining>0){ready(token);delayUntil=SystemClock.elapsedRealtime()+remaining;long chunk=Math.min(70,remaining),before=SystemClock.elapsedRealtime();Thread.sleep(chunk);if(!paused)remaining-=Math.max(1,SystemClock.elapsedRealtime()-before);}ready(token);}finally{if(token==generation.get())delayUntil=0;}
    }
    private void configuredSleep(long ms,long token)throws Exception{ExecutionMetrics measured=executionMetrics;measured.beginConfigured(activeTime(),0);try{sleep(ms,token);}finally{measured.endConfigured(activeTime());}}
    private <T> T await(CompletableFuture<T> f,long token,long max)throws Exception{
        return await(f,token,max,"화면 응답 시간이 초과되었습니다");
    }
    private <T> T await(CompletableFuture<T> f,long token,long max,String timeoutMessage)throws Exception{
        long activeElapsed=0,last=SystemClock.elapsedRealtime();
        while(true){check(token);long now=SystemClock.elapsedRealtime();if(!paused)activeElapsed+=now-last;last=now;try{return f.get(70,TimeUnit.MILLISECONDS);}catch(TimeoutException e){if(activeElapsed>max){AwaitTimeout timeout=new AwaitTimeout(timeoutMessage);f.completeExceptionally(timeout);throw timeout;}}catch(ExecutionException e){Throwable cause=e.getCause();throw new IllegalStateException(cause.getMessage(),cause);}}
    }
    private static final class AwaitTimeout extends IllegalStateException {AwaitTimeout(String message){super(message);}}
    private <T> T onUi(Callable<T> action,long token)throws Exception{
        ready(token);CompletableFuture<T> f=new CompletableFuture<>();Runnable task=new Runnable(){public void run(){try{if(f.isDone())return;check(token);if(paused){ui.postDelayed(this,70);return;}f.complete(action.call());}catch(Exception e){f.completeExceptionally(e);}}};ui.post(task);return await(f,token,5000);
    }
    private <T> T readScreen(Callable<T> action,long token)throws Exception{
        ready(token);queryStartedAt=SystemClock.elapsedRealtime();
        CompletableFuture<T> result=screenReader.submit(()->{check(token);return action.call();});
        try{return await(result,token,3500);}
        catch(CancellationException e){throw e;}
        catch(Exception e){throw new ScreenReadFailure("화면 정보 응답 지연 · 화면 정보 모드를 완료하지 못했습니다 ("+e.getMessage()+")",e);}
        finally{result.cancel(false);if(token==generation.get())queryStartedAt=0;}
    }
    private static final class ScreenReadFailure extends IllegalStateException {ScreenReadFailure(String message,Throwable cause){super(message,cause);}}
    private void run(Macro m,long token){run(m,token,null);}
    @SuppressWarnings("deprecation") private void run(Macro m,long token,AiStartGate gate){
        PowerManager.WakeLock lock=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(Ui.prefs(this).getBoolean("keep_screen",true)?PowerManager.SCREEN_DIM_WAKE_LOCK:PowerManager.PARTIAL_WAKE_LOCK,"PXE:run");
        try{
            if(!resetStats(token))return;firstAiInput.set(gate);int[] sz=size();initialW=sz[0];initialH=sz[1];watchCompleted=0;deadline=m.maxMinutes==0?0:SystemClock.elapsedRealtime()+m.maxMinutes*60000L;
            lock.acquire(m.maxMinutes==0?24*60*60000L:m.maxMinutes*60000L+10000);
            if(gate!=null&&!gate.engineAppFirst())launchAiTarget(gate,token);
            liveAction="시작 대기";log("시작까지 "+Times.label(m.startDelay));configuredSleep(m.startDelay,token);
            runSequence(m,token,gate);
            if(token==generation.get()){displayOutcome(token,"완료");log("완료");}
        }catch(CancellationException ignored){}catch(Exception e){if(token==generation.get()){displayOutcome(token,"중단");recordError(errorKind(e),e);log("중단 · "+recognitionFailure(e));}}
        finally{firstAiInput.remove();if(lock.isHeld())lock.release();synchronized(this){if(token==generation.get()){finishStats();running=false;paused=false;clearNotification();ui.post(()->{if(controller!=null)controller.update();});}}}
    }
    private void launchAiTarget(AiStartGate gate,long token)throws Exception{
        // 사용자가 미리보기에서 확인한 대상 앱 열기만 준비 동작으로 전달합니다.
        liveAction="AI 실행 대상 앱 열기";
        onUi(()->{Intent intent=getPackageManager().getLaunchIntentForPackage(gate.target());if(intent==null)throw new IllegalStateException("선택한 대상 앱을 열 수 없습니다");intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(intent);return true;},token);
    }
    private void awaitAiTarget(AiStartGate gate,long token)throws Exception{
        while(!gate.open()){
            ready(token);long revision=pauseRevision.get();liveAction="선택한 앱의 새 화면 확인 · 입력 없이 대기 · 정지 가능";
            String foreground=foregroundPackage(token);ready(token);
            // 일시정지·재개나 조회 복구 이전의 관측은 첫 입력 허가로 사용하지 않습니다.
            if(revision!=pauseRevision.get())continue;
            if(gate.observe(foreground,paused))return;
            sleep(100,token);
        }
    }
    private void runSequence(Macro m,long token)throws Exception{
        runSequence(m,token,null);
    }
    private void runSequence(Macro m,long token,AiStartGate gate)throws Exception{
            for(int round=1;m.rounds==0||round<=m.rounds;round++){
                displayProgress(token,new RunProgress(m.rounds,round,round-1,0,m.steps.size(),""));
                if(!watching)cycleStartedAt=SystemClock.elapsedRealtime();
                TextTransition pending=null;int index=round==1?m.startAt-1:0;while(index<m.steps.size()){
                    ready(token);Step s=m.steps.get(index);if(!s.enabled){index++;continue;}
                    displayProgress(token,new RunProgress(m.rounds,round,round-1,index+1,m.steps.size(),s.name()));
                    log(round+"회 · "+(index+1)+"/"+m.steps.size()+" "+s.name());
                    Integer jump=null;for(int r=0;r<s.repeat;r++){
                        if(gate!=null&&!gate.open()&&!gate.permitsInitialApp(s))awaitAiTarget(gate,token);
                        ready(token);liveAction="매크로 "+round+"/"+(m.rounds==0?"∞":m.rounds)+"회 · "+(index+1)+"번 "+s.name()+(s.repeat>1?" "+(r+1)+"/"+s.repeat:"");
                        Step nextStep=nextEnabled(m,index);StepExecution execution=new StepExecution(m,m.adaptiveWait,AdaptiveWaitPolicy.eligible(m,s,nextStep)?nextStep:null,r==0&&pending!=null&&pending.next==s?pending:null);pending=null;
                        boolean initialApp=gate!=null&&gate.permitsInitialApp(s);
                        recognitionStage(s.textAction()?"실행 동작 "+(index+1):"");ExecutionMetrics measured=executionMetrics;measured.beginStep(activeTime());try{jump=execute(s,token,execution);}finally{measured.endStep(activeTime());}ready(token);
                        if(initialApp){gate.appLaunched(s);awaitAiTarget(gate,token);}
                        recognitionStage("");lastAction=(index+1)+"번 "+s.name()+" 완료"+(s.repeat>1?" ("+(r+1)+"/"+s.repeat+")":"");log(lastAction);
                        String next=jump!=null?(jump+1)+"번으로 이동":r+1<s.repeat?"같은 동작 반복":index+1<m.steps.size()?(index+2)+"번 "+m.steps.get(index+1).name():m.rounds==0||round<m.rounds?"다음 전체 반복":watching?"뒤로가기 준비":"매크로 완료";
                        liveAction="완료 → "+next;pending=execution.outgoing;
                        if(s.canWaitForever()&&s.immediate)ready(token);
                        else if(pending!=null&&pending.early){pending.leadingUntil=activeTime()+s.after;ready(token);}
                        else configuredSleep(s.after==0&&s.afterJitter==0?0:jitter(s.after,s.afterJitter,600000),token);
                        if(jump!=null)break;
                    }
                    index=jump==null?index+1:jump;
                }
                if(!watching){lastCycleMs=SystemClock.elapsedRealtime()-cycleStartedAt;cycleStartedAt=0;watchCompleted++;}
                displayProgress(token,new RunProgress(m.rounds,0,round,0,m.steps.size(),""));
                if(m.rounds==0||round<m.rounds){liveAction="다음 반복 대기 · "+round+"회 완료";configuredSleep(m.gap,token);}
            }

    }
    private Step nextEnabled(Macro macro,int index){for(int i=index+1;i<macro.steps.size();i++)if(macro.steps.get(i).enabled)return macro.steps.get(i);return null;}
    private static final class TextTransition {
        final Step next;final String target;final long token;final boolean early;final AdaptiveWaitPolicy.Reappearance gate;
        long revision,leadingUntil;
        TextTransition(Step next,String target,long token,long revision,boolean early,boolean same){this.next=next;this.target=target;this.token=token;this.revision=revision;this.early=early;gate=same?new AdaptiveWaitPolicy.Reappearance():null;}
    }
    private static final class StepExecution {
        final Macro macro;final boolean adaptive;final Step next;TextTransition incoming,outgoing;
        StepExecution(Macro macro,boolean adaptive,Step next,TextTransition incoming){this.macro=macro;this.adaptive=adaptive;this.next=next;this.incoming=incoming;}
    }
    public synchronized void startWatch(WatchConfig original){
        if(!connectionActive())throw new IllegalStateException("PXE 접근성을 다시 켜세요");
        if(controller!=null&&controller.recording)throw new IllegalStateException("녹화를 먼저 끝내세요");
        original.validate();Macro m=WatchStore.resolve(this,original);WatchConfig p=new WatchConfig(original.json());
        startPreparedWatch(p,m);
    }
    private void startPreparedWatch(WatchConfig p,Macro m){
        startPreparedWatch(p,m,null);
    }
    private void startPreparedWatch(WatchConfig p,Macro m,AiStartGate gate){
        if(!connectionActive())throw new IllegalStateException("PXE 접근성을 다시 켜세요");
        if(controller!=null&&controller.recording)throw new IllegalStateException("녹화를 먼저 끝내세요");
        p.validate();p.validateMacro(m);
        long token=replaceRun();runProgress=new RunProgress(m.rounds,0,0,0,m.steps.size(),"");runOutcome="실행 중";accessibleFallback=false;watching=true;running=true;paused=false;watchCompleted=0;watchPhase="시작 대기";
        showControls();ui.post(()->{if(token==generation.get()&&connectionActive()&&controller!=null)controller.minimize();});
        worker.execute(()->runWatch(p,m,token,gate));
    }
    private void runWatch(WatchConfig p,Macro m,long token){runWatch(p,m,token,null);}
    @SuppressWarnings("deprecation") private void runWatch(WatchConfig p,Macro m,long token,AiStartGate aiStartGate){
        PowerManager.WakeLock lock=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(Ui.prefs(this).getBoolean("keep_screen",true)?PowerManager.SCREEN_DIM_WAKE_LOCK:PowerManager.PARTIAL_WAKE_LOCK,"PXE:watch");
        long overall=0;
        try{
            if(!resetStats(token))return;firstAiInput.set(aiStartGate);int[] sz=size();initialW=sz[0];initialH=sz[1];
            overall=p.maxMinutes==0?0:startedAt+p.maxMinutes*60000L;deadline=overall;
            lock.acquire(p.maxMinutes==0?24*60*60000L:p.maxMinutes*60000L+10000);
            if(aiStartGate!=null){launchAiTarget(aiStartGate,token);awaitAiTarget(aiStartGate,token);}
            log("탐지 시작까지 "+Times.label(p.startMs)+" · 원하는 앱으로 이동하세요");sleep(p.startMs,token);
            watchNextRefresh=SystemClock.elapsedRealtime()+p.intervalSeconds*1000L;
            log("글자 탐지 중 · "+p.rule.text);boolean armed=true;
            WatchGate[] gates=new WatchGate[p.areaCount()];WatchConfig[] areaConfigs=new WatchConfig[p.areaCount()];
            for(int i=0;i<gates.length;i++){areaConfigs[i]=p.areaConfig(i);gates[i]=new WatchGate(p.preventDuplicate&&WatchStore.blocked(this,areaConfigs[i]));}
            long observedRevision=pauseRevision.get();
            while(true){
                ready(token);
                if(observedRevision!=pauseRevision.get()){for(WatchGate gate:gates)gate.interruptAbsence();observedRevision=pauseRevision.get();}
                String foreground=foregroundPackage(token);
                if(observedRevision!=pauseRevision.get()){for(WatchGate gate:gates)gate.interruptAbsence();observedRevision=pauseRevision.get();}
                if(p.targetPackage.isEmpty()&&!foreground.isEmpty())p.targetPackage=foreground;
                if(foreground.isEmpty()||!p.targetPackage.equals(foreground)){for(WatchGate gate:gates)gate.interruptAbsence();watchPhase=foreground.isEmpty()?"대상 앱 확인 대기 · 대상 앱을 한 번 터치하세요":"대상 앱 대기";sleep(500,token);continue;}
                if(SystemClock.elapsedRealtime()>=watchNextRefresh){
                    long refreshAt=SystemClock.elapsedRealtime();watchPhase="새로고침";log("탐지 · "+(p.direction.equals("PULL_DOWN")?"아래로 당겨 새로고침":"목록 아래로 스크롤"));
                    gesture(50,p.swipeStartY(),50,p.swipeEndY(),500,token);lastAction="목록 새로고침 완료";watchPhase="새로고침 완료 · 화면 대기";sleep(p.settleMs,token);
                    watchNextRefresh=refreshAt+p.intervalSeconds*1000L;armed=true;
                }
                if(armed){
                    try{
                    watchPhase="글자 찾는 중";ScanFrame frame=new ScanFrame(p.targetPackage);
                    long scanRevision=pauseRevision.get();Hit hit=null;int selected=-1;boolean anyBlocked=false;
                    for(int area=0;area<gates.length;area++){
                        watchPhase=(area+1)+"순위 확인";recognitionStage("목록 탐지 "+(area+1)+"순위");Hit candidate=find(areaConfigs[area].rule,token,frame);ready(token);
                        lastAction=(area+1)+"순위 · "+(candidate==null?"글자 없음":"글자 발견");
                        if(scanRevision!=pauseRevision.get())break;
                        WatchGate gate=gates[area];boolean wasBlocked=gate.blocked();
                        boolean allowed=!p.preventDuplicate||gate.observe(candidate!=null,SystemClock.elapsedRealtime(),p.rearmMs);
                        if(p.preventDuplicate&&wasBlocked&&!gate.blocked()){WatchStore.blocked(this,areaConfigs[area],false);log((area+1)+"순위 글자 사라짐 확인 · 다시 나타나면 실행");}
                        anyBlocked|=gate.blocked();if(allowed&&candidate!=null){hit=candidate;selected=area;break;}
                    }
                    ready(token);if(scanRevision!=pauseRevision.get()){for(WatchGate gate:gates)gate.interruptAbsence();continue;}
                    if(!p.targetPackage.equals(foregroundPackage(token))){for(WatchGate gate:gates)gate.interruptAbsence();continue;}
                    watchPhase=anyBlocked?"중복 차단 · 사라짐 대기":"글자 찾는 중";
                    if(hit!=null){
                        ready(token);if(p.tapFound&&!areaConfigs[selected].rule.immediate)hit=find(areaConfigs[selected].rule,token,new ScanFrame(p.targetPackage));ready(token);if(scanRevision!=pauseRevision.get())continue;
                        if(!p.targetPackage.equals(foregroundPackage(token))){for(WatchGate gate:gates)gate.interruptAbsence();continue;}
                        ready(token);if(scanRevision!=pauseRevision.get()){for(WatchGate gate:gates)gate.interruptAbsence();continue;}
                        if(hit!=null){
                            if(p.preventDuplicate){WatchStore.blocked(this,areaConfigs[selected],true);gates[selected].reserve();}
                            currentPriority=selected+1;cycleStartedAt=SystemClock.elapsedRealtime();watchPhase="매크로 실행";liveAction="글자 터치 · 들어가기";log((selected+1)+"순위에서 발견 · "+p.rule.text+" → "+m.name+" "+p.repetitions+"회");
                            displayProgress(token,new RunProgress(m.rounds,0,0,0,m.steps.size(),""));
                            deadline=m.watchDeadline(SystemClock.elapsedRealtime(),overall);
                            if(p.tapFound){double x=hit.rect.exactCenterX()*100.0/initialW,y=hit.rect.exactCenterY()*100.0/initialH;gesture(x,y,x,y,100,token);liveAction="들어간 화면 대기";sleep(p.entryMs,token);}
                            if(!entryAllowsMacro(p,token)){
                                deadline=overall;cycleStartedAt=0;currentPriority=0;armed=true;watchNextRefresh=SystemClock.elapsedRealtime()+p.intervalSeconds*1000L;
                                for(WatchGate gate:gates)gate.interruptAbsence();watchPhase="1순위부터 다시 탐지";continue;
                            }
                            configuredSleep(m.startDelay,token);runSequence(m,token);
                            lastAction="매크로 "+p.repetitions+"회 완료";liveAction="전체 반복 완료 → 뒤로가기 대기";sleep(p.beforeBackMs,token);
                            ready(token);deadline=overall;watchPhase="뒤로가기";log("매크로 완료 · 탐지 화면으로 뒤로가기");
                            for(int i=0;i<p.backCount;i++){global(GLOBAL_ACTION_BACK,token);lastAction="뒤로가기 "+(i+1)+"/"+p.backCount+"회 완료";watchPhase="뒤로가기 완료 · 목록 대기";sleep(p.returnMs,token);}
                            lastCycleMs=SystemClock.elapsedRealtime()-cycleStartedAt;cycleStartedAt=0;currentPriority=0;
                            boolean waitRefresh=p.waitForRefresh||p.preventDuplicate;watchCompleted++;armed=!waitRefresh;if(waitRefresh)watchNextRefresh=SystemClock.elapsedRealtime()+p.cooldownMs;
                            watchPhase=waitRefresh?"완료 후 쉬는 중":"글자 찾는 중";log("탐지 "+watchCompleted+"회 완료 · 한 바퀴 "+String.format(Locale.KOREA,"%.1f초",lastCycleMs/1000.0)+" · "+(waitRefresh?"다음 새로고침 대기":"바로 다시 탐지"));
                        }
                    }
                    }catch(StaleCapture|TargetChanged e){for(WatchGate gate:gates)gate.interruptAbsence();continue;}
                }
                long remaining=Math.max(1,watchNextRefresh-SystemClock.elapsedRealtime());
                sleep(Math.min(armed?100:2000,remaining),token);
            }
        }catch(CancellationException ignored){}catch(Exception e){if(token==generation.get()){displayOutcome(token,"중단");recordError(errorKind(e),e);log("탐지 중단 · "+recognitionFailure(e));}}
        finally{firstAiInput.remove();if(lock.isHeld())lock.release();synchronized(this){if(token==generation.get()){finishStats();running=false;watching=false;paused=false;watchPhase="";ui.post(()->{if(controller!=null)controller.update();});}}}
    }
    private boolean entryAllowsMacro(WatchConfig p,long token)throws Exception{
        if(!p.entryCheck)return true;
        Step expected=new Step(p.entryRule.json()),reject=new Step(p.entryRule.json());reject.text=p.rejectText;
        long started=activeTime(),recovery=readRecoveryRevision.get();
        for(;;){
            try{
                ready(token);long revision=pauseRevision.get();watchPhase="진입 확인 · 글자 확인 중";recognitionStage("진입 확인");
                requireRecognitionTarget(p.targetPackage,token);ScanFrame frame=new ScanFrame(p.targetPackage);
                boolean rejected=!p.rejectText.trim().isEmpty()&&find(reject,token,frame)!=null;
                Hit accepted=rejected?null:find(expected,token,frame);ready(token);requireRecognitionTarget(p.targetPackage,token);
                if(revision!=pauseRevision.get())throw new StaleCapture();
                if(recovery!=readRecoveryRevision.get()){recovery=readRecoveryRevision.get();started=activeTime();}
                if(!rejected&&accepted!=null){watchPhase="매크로 실행";recognitionStage("");return true;}
                if(rejected||activeTime()-started>=expected.timeout){
                    watchPhase="진입 확인 실패 · 목록으로 복귀";log(rejected?"되돌아갈 글자 확인 · 매크로 없이 목록으로 복귀":"확인 글자 없음 · 매크로 없이 목록으로 복귀");
                    int returned=0;
                    while(returned<p.backCount){
                        try{
                            ready(token);long backRevision=returned==0?revision:pauseRevision.get(),permit=windowRevision.get();requireRecognitionTarget(p.targetPackage,token);
                            if(backRevision!=pauseRevision.get())throw new StaleCapture();if(permit!=windowRevision.get())throw new TargetChanged();
                            AiStartGate firstBack=firstAiInput.get();if(!onUi(()->{if(backRevision!=pauseRevision.get())throw new StaleCapture();if(permit!=windowRevision.get())throw new TargetChanged();boolean sent=performGlobalAction(GLOBAL_ACTION_BACK);if(sent&&firstBack!=null)firstBack.inputDispatched();return sent;},token))throw new IllegalStateException("시스템 동작을 실행하지 못했습니다");
                            returned++;sleep(p.returnMs,token);
                        }catch(Exception error){
                            boolean stale=false;for(Throwable cause=error;cause!=null;cause=cause.getCause())if(cause instanceof StaleCapture||cause instanceof TargetChanged){stale=true;break;}
                            if(!stale||returned==0)throw error;
                            // 첫 복귀 뒤에는 수행한 횟수를 보존하고 대상 확인 후 남은 복귀만 계속합니다.
                            watchPhase="진입 확인 실패 · 남은 복귀 대기";sleep(100,token);
                        }
                    }
                    recognitionStage("");return false;
                }
                sleep(Math.min(100,Math.max(1,expected.timeout-(activeTime()-started))),token);
            }catch(Exception error){
                boolean stale=false;for(Throwable cause=error;cause!=null;cause=cause.getCause())if(cause instanceof StaleCapture||cause instanceof TargetChanged){stale=true;break;}
                if(!stale)throw error;
                // 읽기 복구·일시정지·대상 이탈은 정상 부재가 아니며 확인 시간을 새로 시작합니다.
                started=activeTime();watchPhase="진입 확인 · 새 화면 대기";sleep(100,token);
            }
        }
    }
    private Integer execute(Step s,long token,StepExecution execution)throws Exception{
        switch(s.type){
            case "TAP":gesture(s.x,s.y,s.x,s.y,jitter(s.duration,s.durationJitter,59000),token);break;
            case "DOUBLE":gesture(s.x,s.y,s.x,s.y,60,token);sleep(100,token);gesture(s.x,s.y,s.x,s.y,60,token);break;
            case "LONG":gesture(s.x,s.y,s.x,s.y,jitter(s.duration,s.durationJitter,59000),token);break;
            case "SWIPE":gesture(s.x,s.y,s.x2,s.y2,Math.min(59000,s.duration),token);break;
            case "CURVE":case "PINCH_IN":case "PINCH_OUT":case "MULTI_TAP":case "GESTURE":multiGesture(GesturePaths.forStep(s),s.durationJitter,token);break;
            case "NOTIFICATIONS":global(GLOBAL_ACTION_NOTIFICATIONS,token);break;
            case "WAIT":configuredSleep(jitter(s.duration,s.durationJitter,600000),token);break;
            case "BACK":global(GLOBAL_ACTION_BACK,token);break;
            case "HOME":global(GLOBAL_ACTION_HOME,token);break;
            case "RECENTS":global(GLOBAL_ACTION_RECENTS,token);break;
            case "INPUT":input(s.text,token);break;
            case "APP":onUi(()->{Intent i=getPackageManager().getLaunchIntentForPackage(s.packageName);if(i==null)throw new IllegalStateException("앱을 찾을 수 없습니다: "+s.packageName);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);return true;},token);AiStartGate appBoundary=firstAiInput.get();if(appBoundary!=null)appBoundary.appSucceeded(s);break;
            case "GOTO":return s.target-1;
            case "STOP":synchronized(this){if(token==generation.get())stop();}throw new CancellationException();
            case "TEXT_TAP":case "TEXT_WAIT":case "TEXT_BRANCH":case "TEXT_CHECK":
                TextTransition incoming=execution.incoming;String touchTarget=incoming==null?"":incoming.target;long targetWaitElapsed=0;
                for(long attempt=0;;){
                    try{
                    // 첫 탐지와 재확인, 실제 터치가 같은 앱에 속하도록 이 동작의 대상을 보존합니다.
                    if(s.type.equals("TEXT_TAP")||s.retryPolicy()||execution.adaptive&&s.canWaitForever()){
                        long beforeTarget=execution.adaptive?activeTime():SystemClock.elapsedRealtime();String foreground=foregroundPackage(token);
                        if(touchTarget.isEmpty()&&!foreground.isEmpty())touchTarget=foreground;
                        if(touchTarget.isEmpty()||!touchTarget.equals(foreground)){
                            liveAction="다음 글자 대기 · 대상 앱 확인 대기";if(incoming!=null&&incoming.leadingUntil>0){invalidateTransition(incoming,token);beforeTarget=activeTime();}sleep(100,token);targetWaitElapsed+=(execution.adaptive?activeTime():SystemClock.elapsedRealtime())-beforeTarget;
                            if(!s.waitForever&&targetWaitElapsed>=s.timeout)throw new IllegalStateException("대상 앱 확인 시간이 초과되어 글자 터치를 정지합니다");
                            continue;
                        }
                        targetWaitElapsed=0;
                    }
                    long scanRevision=pauseRevision.get();
                    TextObservation observation=waitText(s,token,touchTarget,execution);Hit hit=observation.hit;ScanFrame confirmed=observation.frame;
                    if(hit!=null&&s.type.equals("TEXT_TAP")&&!s.immediate){ready(token);confirmed=new ScanFrame(touchTarget);hit=find(s,token,confirmed);}
                    ready(token);if(scanRevision!=pauseRevision.get())continue;
                    // 계속 기다리기에서는 재확인 때 사라진 글자도 같은 대상 앱에서 새로 탐색합니다.
                    if(hit==null&&s.type.equals("TEXT_TAP")&&s.waitForever){liveAction="다음 글자 대기 · 재확인 글자 없음";sleep(100,token);continue;}
                    if(hit!=null){
                        ready(token);setMissingRecovery(token,"");final Hit foundHit=hit;log("인식 성공 · “"+hit.text+"”");if(s.type.equals("TEXT_CHECK"))onUi(()->{controller.showHit(foundHit.rect,s.text);return true;},token);
                        if(s.type.equals("TEXT_TAP")){
                            TextTransition outgoing=prepareTransition(s,execution.next,hit,confirmed,touchTarget,token,scanRevision);
                            requireRecognitionTarget(touchTarget,token);
                            gesture(Math.max(0,Math.min(100,hit.rect.exactCenterX()*100.0/initialW+s.offsetX)),Math.max(0,Math.min(100,hit.rect.exactCenterY()*100.0/initialH+s.offsetY)),hit.rect.exactCenterX()*100.0/initialW,hit.rect.exactCenterY()*100.0/initialH,100,token,touchTarget,scanRevision);
                            if(scanRevision==pauseRevision.get())execution.outgoing=outgoing;
                        }
                        if(s.type.equals("TEXT_BRANCH"))return s.found==0?null:s.found-1;
                        return null;
                    }
                    if(s.type.equals("TEXT_BRANCH"))return s.missing==0?null:s.missing-1;
                    if(s.retryPolicy()&&(s.retryForever||attempt<s.retries)){
                        ready(token);if(touchTarget.isEmpty())touchTarget=foregroundPackage(token);if(touchTarget.isEmpty())throw new TargetChanged();requireRecognitionTarget(touchTarget,token);
                        if(scanRevision!=pauseRevision.get())throw new StaleCapture();
                        long number=attempt==Long.MAX_VALUE?Long.MAX_VALUE:attempt+1;
                        String action="BACK_RETRY".equals(s.failure)?"뒤로가기":"SCROLL_RETRY".equals(s.failure)?"위로 스크롤":"PULL_RETRY".equals(s.failure)?"아래로 당겨 새로고침":"ACTION_RETRY".equals(s.failure)?s.recoveryTarget+"번 "+execution.macro.steps.get(s.recoveryTarget-1).name():"같은 화면에서 다시 확인";
                        attempt=number;setMissingRecovery(token,"“"+s.text+"” 없음 · "+action+" · "+number+(s.retryForever?"번째 · 계속 재시도":"/"+s.retries));log("글자 없음 · "+action+" "+number+(s.retryForever?" · 계속 재시도":"/"+s.retries));
                        if("BACK_RETRY".equals(s.failure))recoveryGlobal(GLOBAL_ACTION_BACK,touchTarget,token);
                        else if("SCROLL_RETRY".equals(s.failure))recoveryGesture(50,75,50,52.5,450,touchTarget,token);
                        else if("PULL_RETRY".equals(s.failure))recoveryGesture(50,28,50,72,500,touchTarget,token);
                        else if("ACTION_RETRY".equals(s.failure))runRecoveryAction(execution.macro.steps.get(s.recoveryTarget-1),touchTarget,token);
                        // 복구로 화면이 바뀐 뒤 이전 선행 조건·좌표·프레임은 재사용하지 않습니다.
                        invalidateTransition(incoming,token);incoming=null;execution.incoming=null;execution.outgoing=null;
                        sleep(s.retryDelay,token);continue;
                    }
                    if("SKIP".equals(s.failure)){log("글자 없음 · 다음 동작");return null;}
                    recordError("TEXT_NOT_FOUND","source="+s.source+", match="+s.match+", 영역="+s.left+","+s.top+","+s.right+","+s.bottom+", waitForever="+s.waitForever+", immediate="+s.immediate+", timeout_ms="+s.timeout+", "+recognitionDetail());
                    throw new IllegalStateException("글자를 찾지 못했습니다: "+s.text);
                    }catch(StaleCapture|TargetChanged e){invalidateTransition(incoming,token);ready(token);}
                }
        }return null;
    }
    private synchronized void setMissingRecovery(long token,String value){if(token==generation.get()&&running&&connectionActive())missingRecovery=value;}
    private <T> T recoveryUi(String target,long token,Callable<T> action)throws Exception{
        for(;;){
            ready(token);long revision=pauseRevision.get();requireRecognitionTarget(target,token);long permit=windowRevision.get();
            if(revision!=pauseRevision.get())continue;
            try{return onUi(()->{if(revision!=pauseRevision.get())throw new StaleCapture();if(permit!=windowRevision.get())throw new TargetChanged();return action.call();},token);}
            catch(Exception error){boolean beforeDispatch=false;for(Throwable cause=error;cause!=null;cause=cause.getCause()){if(cause instanceof TargetChanged||cause instanceof StaleCapture)beforeDispatch=true;}if(!beforeDispatch)throw error;ready(token);requireRecognitionTarget(target,token);}
        }
    }
    private void recoveryGlobal(int action,String target,long token)throws Exception{AiStartGate gate=firstAiInput.get();boolean initial=gate!=null&&!gate.inputTarget().isEmpty();if(initial&&!gate.inputTarget().equals(target))throw new TargetChanged();boolean sent=initial?firstAiUi(()->performGlobalAction(action),token):recoveryUi(target,token,()->performGlobalAction(action));if(!sent)throw new IllegalStateException("시스템 동작을 실행하지 못했습니다");}
    private void recoveryGesture(double x,double y,double x2,double y2,long duration,String target,long token)throws Exception{
        AiStartGate gate=firstAiInput.get();String firstTarget=gate==null?"":gate.inputTarget();if(!firstTarget.isEmpty()&&!firstTarget.equals(target))throw new TargetChanged();
        for(;;){try{ready(token);gesture(x,y,x2,y2,duration,token,target,pauseRevision.get());return;}catch(StaleCapture|TargetChanged beforeDispatch){ready(token);requireRecognitionTarget(target,token);}}
    }
    private void recoveryMultiGesture(Step action,String target,long token)throws Exception{
        AiStartGate gate=firstAiInput.get();String firstTarget=gate==null?"":gate.inputTarget();if(!firstTarget.isEmpty()&&!firstTarget.equals(target))throw new TargetChanged();
        // StaleCapture와 TargetChanged는 전달 전 허가 거절만 뜻합니다. 완료·취소 callback은 재전송하지 않습니다.
        for(;;){try{ready(token);multiGesture(GesturePaths.forStep(action),action.durationJitter,token,target,pauseRevision.get());return;}catch(StaleCapture|TargetChanged beforeDispatch){ready(token);requireRecognitionTarget(target,token);}}
    }
    private void runRecoveryAction(Step action,String target,long token)throws Exception{
        if(!action.enabled||!action.recoveryAction())throw new IllegalStateException("다시 찾기 전에 실행할 일반 동작을 확인하세요");
        for(int repetition=0;repetition<action.repeat;repetition++){
            ready(token);requireRecognitionTarget(target,token);
            switch(action.type){
                case "TAP":case "LONG":recoveryGesture(action.x,action.y,action.x,action.y,jitter(action.duration,action.durationJitter,59000),target,token);break;
                case "DOUBLE":recoveryGesture(action.x,action.y,action.x,action.y,60,target,token);sleep(100,token);recoveryGesture(action.x,action.y,action.x,action.y,60,target,token);break;
                case "SWIPE":recoveryGesture(action.x,action.y,action.x2,action.y2,Math.min(59000,action.duration),target,token);break;
                case "CURVE":case "PINCH_IN":case "PINCH_OUT":case "MULTI_TAP":case "GESTURE":recoveryMultiGesture(action,target,token);break;
                case "BACK":recoveryGlobal(GLOBAL_ACTION_BACK,target,token);break;
                case "HOME":recoveryGlobal(GLOBAL_ACTION_HOME,target,token);break;
                case "RECENTS":recoveryGlobal(GLOBAL_ACTION_RECENTS,target,token);break;
                case "NOTIFICATIONS":recoveryGlobal(GLOBAL_ACTION_NOTIFICATIONS,target,token);break;
                case "WAIT":configuredSleep(jitter(action.duration,action.durationJitter,600000),token);break;
                case "INPUT":input(action.text,token,target);break;
                case "APP":recoveryUi(target,token,()->{Intent intent=getPackageManager().getLaunchIntentForPackage(action.packageName);if(intent==null)throw new IllegalStateException("앱을 찾을 수 없습니다: "+action.packageName);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(intent);return true;});AiStartGate recoveryApp=firstAiInput.get();if(recoveryApp!=null)recoveryApp.appSucceeded(action);break;
                default:throw new IllegalStateException("복구에는 일반 동작만 사용할 수 있습니다");
            }
            configuredSleep(action.after==0&&action.afterJitter==0?0:jitter(action.after,action.afterJitter,600000),token);
        }
    }
    private void awaitAiInputApp(AiStartGate gate,long token)throws Exception{
        if(gate==null)return;
        while(gate.awaitingInputApp()){ready(token);long revision=pauseRevision.get();String foreground=foregroundPackage(token);ready(token);if(revision!=pauseRevision.get())continue;if(gate.observeInputApp(foreground))return;liveAction="명시한 앱의 새 화면 확인 · 첫 입력 없이 대기";sleep(100,token);}
    }
    private AiFirstInputPermit freshAiInput(AiStartGate gate,long token,long revision)throws Exception{
        if(gate==null||gate.inputTarget().isEmpty())return null;
        awaitAiInputApp(gate,token);
        // 첫 준비 조회는 관측 키의 변경만 반영합니다. 이 결과만으로 입력하지 않습니다.
        requireRecognitionTarget(gate.inputTarget(),token);if(revision!=pauseRevision.get()||paused)throw new StaleCapture();
        // 자체 HUD 숨김 등 준비 이벤트가 끝난 뒤의 새 조회만 실제 입력 허가로 사용합니다.
        AiFirstInputPermit permit=new AiFirstInputPermit(gate.inputTarget(),token,revision,windowRevision.get());String foreground=foregroundPackage(token);
        if(revision!=pauseRevision.get()||paused)throw new StaleCapture();
        if(!permit.confirm(foreground,generation.get(),pauseRevision.get(),windowRevision.get(),paused))throw new TargetChanged();return permit;
    }
    private void requireAiDispatch(AiFirstInputPermit permit)throws Exception{if(permit!=null&&!permit.permitsDispatch(generation.get(),pauseRevision.get(),windowRevision.get(),paused)){if(permit.pauseChanged(pauseRevision.get(),paused))throw new StaleCapture();throw new TargetChanged();}}
    private <T> T firstAiUi(Callable<T> action,long token)throws Exception{
        AiStartGate gate=firstAiInput.get();if(gate==null||gate.inputTarget().isEmpty())return onUi(action,token);
        for(;;){ready(token);long revision=pauseRevision.get();
            try{AiFirstInputPermit permit=freshAiInput(gate,token,revision);return onUi(()->{requireAiDispatch(permit);T result=action.call();if(Boolean.TRUE.equals(result)){permit.dispatched();gate.inputDispatched();}return result;},token);}
            catch(Exception error){boolean stale=false;for(Throwable cause=error;cause!=null;cause=cause.getCause())if(cause instanceof StaleCapture)stale=true;if(!stale)throw error;ready(token);}
        }
    }
    private void global(int action,long token)throws Exception{if(!firstAiUi(()->performGlobalAction(action),token))throw new IllegalStateException("시스템 동작을 실행하지 못했습니다");}
    private long jitter(long value,int range,long max){if(range==0)return Math.max(1,value);return Math.max(1,Math.min(max,value+java.util.concurrent.ThreadLocalRandom.current().nextLong(-range,(long)range+1)));}
    private GestureDescription description(org.json.JSONArray strokes,int width,int height,int jitter)throws Exception{
        GesturePaths.validate(strokes);long base=0;for(int i=0;i<strokes.length();i++){org.json.JSONObject o=strokes.getJSONObject(i);base=Math.max(base,o.getInt("start")+o.getInt("duration"));}
        double scale=jitter==0?1:jitter(base,jitter,59000)/(double)base;GestureDescription.Builder builder=new GestureDescription.Builder();
        for(int i=0;i<strokes.length();i++){org.json.JSONObject o=strokes.getJSONObject(i);org.json.JSONArray points=o.getJSONArray("points");Path p=new Path();for(int k=0;k<points.length();k++){org.json.JSONArray a=points.getJSONArray(k);float x=(float)Math.min(width-1,a.getDouble(0)*width/100),y=(float)Math.min(height-1,a.getDouble(1)*height/100);if(k==0)p.moveTo(x,y);else p.lineTo(x,y);}long start=(long)(o.getInt("start")*scale),duration=Math.max(1,(long)(o.getInt("duration")*scale));builder.addStroke(new GestureDescription.StrokeDescription(p,start,duration));}
        return builder.build();
    }
    private void gesture(double x,double y,double x2,double y2,long duration,long token)throws Exception{
        for(;;){try{gesture(x,y,x2,y2,duration,token,"",0);return;}catch(StaleCapture beforeDispatch){AiStartGate gate=firstAiInput.get();if(gate==null||gate.inputTarget().isEmpty())throw beforeDispatch;ready(token);}}
    }
    private void gesture(double x,double y,double x2,double y2,long duration,long token,String target,long revision)throws Exception{
        org.json.JSONArray points=new org.json.JSONArray();points.put(GesturePaths.point(x,y));if(x!=x2||y!=y2)points.put(GesturePaths.point(x2,y2));org.json.JSONArray strokes=new org.json.JSONArray();strokes.put(GesturePaths.stroke(points,0,(int)duration));multiGesture(strokes,0,token,target,revision);
    }
    private void multiGesture(org.json.JSONArray strokes,int variation,long token)throws Exception{
        for(;;){try{multiGesture(strokes,variation,token,"",0);return;}catch(StaleCapture beforeDispatch){AiStartGate gate=firstAiInput.get();if(gate==null||gate.inputTarget().isEmpty())throw beforeDispatch;ready(token);}}
    }
    private void multiGesture(org.json.JSONArray strokes,int variation,long token,String target,long revision)throws Exception{
        AiStartGate firstGate=firstAiInput.get();String firstTarget=firstGate==null?"":firstGate.inputTarget();boolean initial=!firstTarget.isEmpty();
        if(initial&&!target.isEmpty()&&!firstTarget.equals(target))throw new TargetChanged();
        final String boundTarget=initial?firstTarget:target;final long boundRevision=initial&&target.isEmpty()?pauseRevision.get():revision;
        FloatingController.GestureLease lease=onUi(()->controller.prepareGesture(strokes,initialW,initialH),token);
        try{sleep(lease.waitForOverlay?80:20,token);ready(token);GestureDescription g=description(strokes,initialW,initialH,variation);CompletableFuture<Boolean> result=new CompletableFuture<>();
            observeAiInputBoundary("PREPARED");final long beforeQueryWindow=windowRevision.get();
            final AiFirstInputPermit firstPermit=initial?freshAiInput(firstGate,token,boundRevision):null;
            final long permitWindow=firstPermit==null?beforeQueryWindow:firstPermit.window();observeAiInputBoundary("CONFIRMED");
            if(!boundTarget.isEmpty()){
                if(boundRevision!=pauseRevision.get())throw new StaleCapture();
                if(!initial)requireRecognitionTarget(boundTarget,token);
                if(boundRevision!=pauseRevision.get())throw new StaleCapture();
                if(permitWindow!=windowRevision.get())throw new TargetChanged();
            }
            // 창 조회는 위 worker에서 끝내고, 실제 전달 직전에는 세대 숫자만 대조합니다.
            Runnable dispatch=new Runnable(){@Override public void run(){try{if(result.isDone())return;check(token);if(firstPermit!=null&&aiInputBoundaryObserver!=null)aiInputBoundaryObserver.reached("DISPATCH");requireAiDispatch(firstPermit);if(!boundTarget.isEmpty()){if(paused||boundRevision!=pauseRevision.get())throw new StaleCapture();if(permitWindow!=windowRevision.get())throw new TargetChanged();}if(paused){ui.postDelayed(this,70);return;}if(!dispatchGesture(g,new GestureResultCallback(){@Override public void onCompleted(GestureDescription d){result.complete(true);}@Override public void onCancelled(GestureDescription d){result.complete(false);}},ui))result.complete(false);else if(firstPermit!=null){firstPermit.dispatched();firstGate.inputDispatched();}}catch(Exception e){result.completeExceptionally(e);}}};ui.post(dispatch);
            try{if(!await(result,token,65000))throw new IllegalStateException("제스처가 취소되었습니다");}
            catch(Exception error){for(Throwable cause=error;cause!=null;cause=cause.getCause()){if(cause instanceof TargetChanged)throw (TargetChanged)cause;if(cause instanceof StaleCapture)throw (StaleCapture)cause;}throw error;}
        }finally{ui.post(()->{if(token==generation.get()&&connectionActive()&&controller!=null)controller.restoreGesture(lease);});}
    }
    static final class Hit {final Rect rect;final String text;Hit(Rect r,String t){rect=new Rect(r);text=t;}}
    private static final class TextObservation {
        final Hit hit;final ScanFrame frame;
        TextObservation(Hit hit,ScanFrame frame){this.hit=hit;this.frame=frame;}
    }
    private TextTransition prepareTransition(Step current,Step next,Hit hit,ScanFrame frame,String target,long token,long revision){
        if(next==null||frame==null||target.isEmpty()||revision!=pauseRevision.get()||token!=generation.get())return null;
        Hit nextHit=null;boolean known=false;
        if("OCR".equals(next.source)&&frame.text!=null){nextHit=ocrHit(next,frame.text);known=true;}
        else if("ACCESS".equals(next.source)&&frame.accessible!=null){nextHit=accessibleHit(next,frame.accessible);known=true;}
        else if("AUTO".equals(next.source)&&frame.accessible!=null){nextHit=accessibleHit(next,frame.accessible);if(nextHit!=null)known=true;else if(frame.text!=null){nextHit=ocrHit(next,frame.text);known=true;}}
        // 이미 있던 다른 조건과 아직 평가하지 않은 인식 방식을 새 화면의 증거로 쓰지 않습니다.
        boolean same=nextHit!=null&&"TEXT_TAP".equals(next.type)&&nextHit.rect.equals(hit.rect);
        boolean early=known&&nextHit==null&&current.afterJitter==0&&!current.immediate&&current.after>0;
        return same||early?new TextTransition(next,target,token,revision,early,same):null;
    }
    private void invalidateTransition(TextTransition transition,long token)throws Exception{
        if(transition==null)return;long remaining=Math.max(0,transition.leadingUntil-activeTime());transition.leadingUntil=0;
        if(transition.gate!=null)transition.gate.interrupt();transition.revision=pauseRevision.get();
        if(remaining>0)configuredSleep(remaining,token);
    }
    private TextObservation waitText(Step s,long token,String target,StepExecution execution)throws Exception{
        if(!execution.adaptive||!s.canWaitForever())return waitTextLegacy(s,token,target);
        TextTransition incoming=execution.incoming;long started=activeTime();
        long leadingUntil=incoming==null?0:incoming.leadingUntil,recovery=readRecoveryRevision.get();ExecutionMetrics measured=executionMetrics;
        if(leadingUntil>activeTime())measured.beginConfigured(activeTime(),leadingUntil);
        try{
            while(true){
                ready(token);long revision=pauseRevision.get();
                if(incoming!=null&&incoming.revision!=revision){measured.endConfigured(activeTime());invalidateTransition(incoming,token);leadingUntil=0;started=activeTime();}
                if(s.canWaitForever()||!target.isEmpty()){
                    String foreground=foregroundPackage(token);if(target.isEmpty()&&!foreground.isEmpty())target=foreground;
                    if(target.isEmpty()||!target.equals(foreground))throw new TargetChanged();
                    liveAction=leadingUntil>activeTime()?"다음 글자 조건 대기 · 준비되면 진행":"다음 글자 대기 · 인식되면 바로 진행";
                }
                ScanFrame frame=new ScanFrame(target);Hit hit=find(s,token,frame);ready(token);
                if(revision!=pauseRevision.get())throw new StaleCapture();
                if(!target.isEmpty()&&!target.equals(foregroundPackage(token)))throw new TargetChanged();
                if(incoming!=null&&incoming.gate!=null&&!incoming.gate.observe(hit!=null))hit=null;
                if(hit!=null)return new TextObservation(hit,frame);
                long now=activeTime();if(recovery!=readRecoveryRevision.get()){recovery=readRecoveryRevision.get();started=now;}long elapsed=AdaptiveWaitPolicy.timeoutElapsed(started,leadingUntil,now);
                if(!s.waitForever&&now>=leadingUntil&&elapsed>=s.timeout)return new TextObservation(null,frame);
                long remaining=now<leadingUntil?leadingUntil-now:s.waitForever?100:s.timeout-elapsed;
                sleep(Math.min(100,Math.max(1,remaining)),token);
            }
        }finally{measured.endConfigured(activeTime());}
    }
    private TextObservation waitTextLegacy(Step s,long token,String target)throws Exception{
        long elapsed=0,recovery=readRecoveryRevision.get();
        do{
            ready(token);long before=SystemClock.elapsedRealtime(),revision=pauseRevision.get();
            if(!target.isEmpty()||s.canWaitForever()&&(s.waitForever||s.immediate)){
                String foreground=foregroundPackage(token);if(target.isEmpty()&&!foreground.isEmpty())target=foreground;
                if(target.isEmpty()||!target.equals(foreground)){if(s.type.equals("TEXT_TAP")||s.retryPolicy())throw new TargetChanged();liveAction="다음 글자 대기 · 대상 앱 확인 대기";sleep(100,token);continue;}
                liveAction="다음 글자 대기 · 인식되면 바로 진행";
            }
            Hit h;
            ScanFrame frame=new ScanFrame(target);try{h=find(s,token,frame);}catch(TargetChanged e){if(s.type.equals("TEXT_TAP")||s.retryPolicy())throw e;continue;}
            ready(token);
            if(revision!=pauseRevision.get())throw new StaleCapture();
            if(!target.isEmpty()&&!target.equals(foregroundPackage(token))){if(s.retryPolicy())throw new TargetChanged();continue;}
            if(h!=null)return new TextObservation(h,frame);
            if(recovery!=readRecoveryRevision.get()){recovery=readRecoveryRevision.get();elapsed=0;before=SystemClock.elapsedRealtime();}
            elapsed+=SystemClock.elapsedRealtime()-before;if(!s.waitForever&&elapsed>=s.timeout)return new TextObservation(null,frame);
            long delay=s.waitForever?100:Math.min(s.immediate?100:600,s.timeout-elapsed);sleep(delay,token);elapsed+=delay;
        }while(true);
    }
    private static final class ScanFrame {
        final String targetPackage;com.google.mlkit.vision.text.Text text;ArrayList<Hit> accessible;
        ScanFrame(){this("");}ScanFrame(String targetPackage){this.targetPackage=targetPackage;}
    }
    private Hit find(Step s,long token)throws Exception{return find(s,token,new ScanFrame());}
    private Hit find(Step s,long token,ScanFrame frame)throws Exception{
        ready(token);requestedSource=s.source;actualSource="";recognitionNote="";Hit hit=null;
        if(!s.source.equals("OCR")&&(!accessibleFallback||s.source.equals("ACCESS"))){
            actualSource="ACCESS";
            try{
                boolean recovering=false;
                while(frame.accessible==null){
                    try{frame.accessible=readScreen(()->accessibilitySource.read(token),token);}
                    catch(ScreenReadFailure error){
                        if(!s.source.equals("ACCESS")||!queryCanRecover(error))throw error;
                        if(!recovering){markReadRecovery(token);pauseRevision.incrementAndGet();recordError("SCREEN_INFORMATION_TIMEOUT");log("화면 정보 응답 복구 대기 · OCR 전환 없이 기다립니다 · 정지 가능");recovering=true;}
                        sleep(250,token);
                    }
                }
                if(recovering)log("화면 정보 응답 복구됨 · 새 화면으로 다시 확인합니다");
                hit=accessibleHit(s,frame.accessible);if(frame.accessible.isEmpty())recognitionNote="읽을 수 있는 화면 글자 없음";
            }
            catch(ScreenReadFailure e){
                if(s.source.equals("ACCESS"))throw e;
                accessibleFallback=true;ready(token);log("화면 정보 응답 지연 · 이 실행은 화면 OCR로 전환합니다");
            }
        }
        if(hit!=null||s.source.equals("ACCESS"))return hit;
        actualSource="OCR";recognitionNote="";
        if(frame.text!=null)return ocrHit(s,frame.text);
        frame.text=recognizeText(token,frame.targetPackage);return ocrHit(s,frame.text);
    }
    private static final class RecognitionRequest {
        final Bitmap bitmap;final long revision;
        final ExecutionMetrics measured;final java.util.function.LongSupplier clock;
        final CompletableFuture<com.google.mlkit.vision.text.Text> result=new CompletableFuture<>();
        private final AtomicBoolean completed=new AtomicBoolean();
        RecognitionRequest(Bitmap bitmap,long revision,ExecutionMetrics measured,java.util.function.LongSupplier clock){this.bitmap=bitmap;this.revision=revision;this.measured=measured;this.clock=clock;}
        void complete(com.google.mlkit.vision.text.Text text,Exception error){
            if(!completed.compareAndSet(false,true))return;
            measured.endOcr(clock.getAsLong());
            if(!bitmap.isRecycled())bitmap.recycle();
            if(error==null)result.complete(text);else result.completeExceptionally(error);
        }
    }
    private void requireRecognitionTarget(String targetPackage,long token)throws Exception{
        if(!targetPackage.isEmpty()&&!targetPackage.equals(foregroundPackage(token)))throw new TargetChanged();
    }
    private com.google.mlkit.vision.text.Text awaitRecognition(RecognitionRequest request,String targetPackage,long token)throws Exception{
        CompletableFuture<com.google.mlkit.vision.text.Text> waiting=request.result.thenApply(value->value);
        long activeElapsed=0,last=SystemClock.elapsedRealtime(),nextTargetCheck=0;
        while(true){
            check(token);long now=SystemClock.elapsedRealtime();if(!paused)activeElapsed+=now-last;last=now;
            // 실제 OCR은 유지한 채 대상 창이 바뀌면 결과를 버리고 바깥 대기로 돌아갑니다.
            if(!paused&&now>=nextTargetCheck){requireRecognitionTarget(targetPackage,token);nextTargetCheck=SystemClock.elapsedRealtime()+250;}
            try{return waiting.get(70,TimeUnit.MILLISECONDS);}
            catch(TimeoutException e){if(activeElapsed>12000){IllegalStateException timeout=new IllegalStateException("한글 OCR 인식 응답 시간이 초과되었습니다");waiting.completeExceptionally(timeout);throw timeout;}}
            catch(ExecutionException e){Throwable cause=e.getCause();throw new IllegalStateException(cause.getMessage(),cause);}
        }
    }
    private com.google.mlkit.vision.text.Text recognizeText(long token,String targetPackage)throws Exception{
        long scanRevision=pauseRevision.get();
        try{
            while(true){
                ready(token);
                // 중단 후 다시 실행해도 끝나지 않은 인식 뒤에 새 이미지를 쌓지 않습니다.
                while(pendingRecognition.get()!=null){requireRecognitionTarget(targetPackage,token);recognitionPhase="OCR 인식 응답 복구 대기 · 계속 미응답이면 접근성을 껐다 켜세요 · 정지 가능";sleep(100,token);}
                if(scanRevision!=pauseRevision.get())throw new StaleCapture();
                requireRecognitionTarget(targetPackage,token);
                try{
                Bitmap bitmap=screenshot(token);RecognitionRequest request=new RecognitionRequest(bitmap,pauseRevision.get(),executionMetrics,this::activeTime);
                try{
                    ready(token);requireRecognitionTarget(targetPackage,token);pendingRecognition.set(request);recognitionPhase="한글 OCR 인식 중 · 정지 가능";
                    request.measured.beginOcr(activeTime());com.google.android.gms.tasks.Task<com.google.mlkit.vision.text.Text> task=recognizer.process(InputImage.fromBitmap(bitmap,0));
                    task.addOnCompleteListener(getMainExecutor(),done->{
                        try{request.complete(done.isSuccessful()?done.getResult():null,done.isSuccessful()?null:done.isCanceled()?new RecognitionCanceled():done.getException());}
                        finally{pendingRecognition.compareAndSet(request,null);}
                    });
                }catch(Exception error){request.complete(null,error);pendingRecognition.compareAndSet(request,null);throw error;}
                // 대기용 future만 만료시켜 원래 Task의 완료와 이미지 해제는 보존합니다.
                com.google.mlkit.vision.text.Text text=awaitRecognition(request,targetPackage,token);
                ready(token);if(request.revision!=pauseRevision.get())throw new StaleCapture();recognitionRetryDelay=250;return text;
                }catch(Exception error){
                    if(error.getCause()==null&&"한글 OCR 인식 응답 시간이 초과되었습니다".equals(error.getMessage())){
                        markReadRecovery(token);
                        recordError("OCR_RECOGNITION_TIMEOUT");log("OCR 인식 응답 지연 · 오래된 결과를 버리고 새 화면으로 복구 대기 · 정지 가능");
                    }else{
                        if(!recognitionCanRecover(error))throw error;
                        // 읽지 못한 화면은 글자 부재가 아니므로 중복 해제와 이전 좌표를 폐기합니다.
                        markReadRecovery(token);pauseRevision.incrementAndGet();recordError(errorKind(error));
                        recognitionPhase="OCR 일시 오류 복구 대기 · 새 화면으로 재시도 · 정지 가능";log(recognitionPhase);long delay=recognitionRetryDelay;recognitionRetryDelay=Math.min(2000,recognitionRetryDelay*2);sleep(delay,token);
                        // 다음 순위와 재확인도 복구 전의 부재·좌표를 공유하지 않도록 새 탐색을 시작합니다.
                        throw new StaleCapture();
                    }
                }
            }
        }finally{if(token==generation.get())recognitionPhase="";}
    }
    private static final class RecognitionCanceled extends IllegalStateException {RecognitionCanceled(){super("한글 OCR 인식 작업이 취소되었습니다");}}
    private static boolean recognitionCanRecover(Throwable error){
        for(Throwable cause=error;cause!=null;cause=cause.getCause()){
            if(cause instanceof RecognitionCanceled)return true;
            if(cause instanceof MlKitException){int code=((MlKitException)cause).getErrorCode();return code==MlKitException.UNAVAILABLE||code==MlKitException.DEADLINE_EXCEEDED;}
        }return false;
    }
    private Hit ocrHit(Step s,com.google.mlkit.vision.text.Text t){
            ArrayList<Hit> candidates=new ArrayList<>();
            for(com.google.mlkit.vision.text.Text.TextBlock b:t.getTextBlocks()){
                int beforeBlock=candidates.size();
                for(com.google.mlkit.vision.text.Text.Line line:b.getLines()){
                    boolean elementMatch=false;
                    for(com.google.mlkit.vision.text.Text.Element e:line.getElements()){
                        if(e.getBoundingBox()!=null&&TextRules.matches(e.getText(),s.text,s.match,s.ignoreSpaces,s.ignoreCase)){candidates.add(new Hit(e.getBoundingBox(),e.getText()));elementMatch=true;}
                    }
                    if(!elementMatch&&line.getBoundingBox()!=null&&TextRules.matches(line.getText(),s.text,s.match,s.ignoreSpaces,s.ignoreCase))candidates.add(new Hit(line.getBoundingBox(),line.getText()));
                }
                if(candidates.size()==beforeBlock&&b.getBoundingBox()!=null&&TextRules.matches(b.getText(),s.text,s.match,s.ignoreSpaces,s.ignoreCase))candidates.add(new Hit(b.getBoundingBox(),b.getText()));
            }
            candidates.removeIf(a->!inside(s,a.rect));
            candidates.sort(Comparator.comparingInt((Hit a)->a.rect.top).thenComparingInt(a->a.rect.left));
            return candidates.size()>=s.occurrence?candidates.get(s.occurrence-1):null;
    }
    private boolean inside(Step s,Rect r){double x=r.exactCenterX()*100.0/initialW,y=r.exactCenterY()*100.0/initialH;return x>=s.left&&x<=s.right&&y>=s.top&&y<=s.bottom;}
    private Hit accessibleHit(Step s,ArrayList<Hit> snapshot){
        ArrayList<Hit> list=new ArrayList<>();
        for(Hit h:snapshot)if(inside(s,h.rect)&&TextRules.matches(h.text,s.text,s.match,s.ignoreSpaces,s.ignoreCase)){
            boolean duplicate=false;for(Hit other:list)if(other.rect.equals(h.rect)){duplicate=true;break;}if(!duplicate)list.add(h);
        }
        list.sort(Comparator.comparingInt((Hit a)->a.rect.top).thenComparingInt(a->a.rect.left));
        return list.size()>=s.occurrence?list.get(s.occurrence-1):null;
    }
    private ArrayList<Hit> accessibleSnapshot(long token)throws Exception{
        long until=SystemClock.elapsedRealtime()+2500;int[] visited={0};
        AccessibilityNodeInfo root=getRootInActiveWindow();ArrayList<Hit> list=new ArrayList<>();
        if(root==null)return list;
        try{collect(root,list,0,visited,until,token);return list;}finally{root.recycle();}
    }
    private void collect(AccessibilityNodeInfo n,ArrayList<Hit> out,int depth,int[] visited,long until,long token)throws Exception{
        if(token!=generation.get()||destroyed)throw new CancellationException();
        if(depth>70||++visited[0]>2000||SystemClock.elapsedRealtime()>until)throw new IllegalStateException("화면 항목이 너무 많거나 응답이 느립니다");
        if(n.isVisibleToUser()){
            CharSequence txt=n.getText(),desc=n.getContentDescription();String value=txt!=null?txt.toString():desc==null?"":desc.toString();
            Rect r=new Rect();n.getBoundsInScreen(r);
            if(!value.isEmpty()&&!r.isEmpty()&&r.intersect(0,0,initialW,initialH))out.add(new Hit(r,value));
        }
        int children=n.getChildCount();for(int i=0;i<children;i++){
            if(SystemClock.elapsedRealtime()>until||token!=generation.get())throw new IllegalStateException("화면 정보 읽기를 마치지 못했습니다");
            AccessibilityNodeInfo child=n.getChild(i);if(child!=null){try{collect(child,out,depth+1,visited,until,token);}finally{child.recycle();}}
        }
    }
    private Bitmap screenshot(long token)throws Exception{
        ExecutionMetrics measured=executionMetrics;measured.beginCapture(activeTime());
        try{
            for(int retry=0;;retry++){
                ready(token);capturePhase=retry==0?"OCR 캡처 간격 대기":"OCR 복구 대기 · 재시도 "+retry;
                boolean reportedBlocked=false;
                while(captureRequests.size()>=2){
                    capturePhase="OCR 복구 대기 · 캡처 2개 무응답 · 접근성을 껐다 켜세요 · 정지 가능";
                    if(!reportedBlocked){recordError("OCR_CAPTURE_UNRESPONSIVE");log(capturePhase);reportedBlocked=true;}
                    sleep(100,token);
                }
                long wait;while((wait=capturePacer.remaining(SystemClock.elapsedRealtime()))>0)sleep(wait,token);
                try{capturePhase=captureRequests.size()==0?"OCR 화면 캡처 중":"OCR 예비 캡처 중 · 이전 1개 무응답 · 정지 가능";return captureOnce(token);}
                catch(CaptureTimeout e){markReadRecovery(token);recordError("OCR_CAPTURE_TIMEOUT");log("OCR 캡처 응답 지연 · 터치 없이 복구 대기 · 정지 가능");}
                catch(StaleCapture e){ready(token);throw e;}
                catch(Exception e){
                    CaptureFailure failure=null;for(Throwable cause=e;cause!=null;cause=cause.getCause())if(cause instanceof CaptureFailure){failure=(CaptureFailure)cause;break;}
                    if(failure==null||!recoverableCapture(failure.error))throw e;
                    markReadRecovery(token);
                    recordError("OCR_CAPTURE_ERROR_"+failure.error);
                    if(failure.error==ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT)capturePacer.rejected(SystemClock.elapsedRealtime());
                    capturePhase="캡처 간격 조정 · 터치 없이 복구 대기";log(capturePhase);sleep(1000,token);
                }
            }
        }finally{if(token==generation.get())capturePhase="";measured.endCapture(activeTime());}
    }
    private boolean recoverableCapture(int error){return error==ERROR_TAKE_SCREENSHOT_INTERNAL_ERROR||error==ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT||error==ERROR_TAKE_SCREENSHOT_INVALID_DISPLAY;}
    private static final class CaptureTimeout extends IllegalStateException {CaptureTimeout(Throwable cause){super("OCR 화면 캡처 응답 시간이 초과되었습니다",cause);}}
    private static final class TargetChanged extends IllegalStateException {TargetChanged(){super("대상 앱이 바뀌어 이전 화면 인식을 버립니다");}}
    private static final class StaleCapture extends IllegalStateException {StaleCapture(){super("일시정지 이전 화면은 버리고 다시 확인합니다");}}
    private static final class CaptureFailure extends IllegalStateException {
        final int error;CaptureFailure(int error){super(captureError(error));this.error=error;}
    }
    private Bitmap captureOnce(long token)throws Exception{
        onUi(()->{controller.setCaptureMode(true);return true;},token);
        try{
            sleep(150,token);CompletableFuture<Bitmap> f=new CompletableFuture<>();long revision=pauseRevision.get();String captureDetail=recognitionDetail();
            ui.post(new Runnable(){public void run(){try{if(f.isDone())return;check(token);if(paused){ui.postDelayed(this,70);return;}
                long requestId=captureRequests.begin();if(requestId==0)throw new IllegalStateException("이전 OCR 캡처 2개가 아직 진행 중입니다");
                try{screenshotSource.take(getMainExecutor(),new TakeScreenshotCallback(){
                @Override public void onSuccess(ScreenshotResult r){
                    capturePacer.responded(SystemClock.elapsedRealtime());captureRequests.complete(requestId);
                    Bitmap copy=null;try{if(token!=generation.get()||f.isDone()){f.cancel(false);return;}if(paused||revision!=pauseRevision.get()){f.completeExceptionally(new StaleCapture());return;}Bitmap hardware=Bitmap.wrapHardwareBuffer(r.getHardwareBuffer(),r.getColorSpace());if(hardware==null)throw new IllegalStateException("화면 이미지가 비었습니다");copy=hardware.copy(Bitmap.Config.ARGB_8888,false);hardware.recycle();if(activeBehavior!=null&&activeBehavior.token==token)behaviorScreenshotAt=r.getTimestamp();if(!f.complete(copy))copy.recycle();}catch(Exception e){if(copy!=null&&!copy.isRecycled())copy.recycle();f.completeExceptionally(e);}finally{r.getHardwareBuffer().close();}
                }
                @Override public void onFailure(int error){capturePacer.failed(SystemClock.elapsedRealtime());captureRequests.complete(requestId);CaptureFailure failure=new CaptureFailure(error);if(!f.completeExceptionally(failure)&&token==generation.get())recordError("OCR_CAPTURE_LATE_ERROR_"+error,captureDetail);}
            });}catch(Exception e){capturePacer.failed(SystemClock.elapsedRealtime());captureRequests.complete(requestId);throw e;}
            }catch(Exception e){f.completeExceptionally(e);}}});
            try{return await(f,token,8000,"OCR 화면 캡처 응답 시간이 초과되었습니다");}catch(Exception e){f.thenAccept(b->{if(b!=null&&!b.isRecycled())b.recycle();});f.completeExceptionally(e);if("OCR 화면 캡처 응답 시간이 초과되었습니다".equals(e.getMessage()))throw new CaptureTimeout(e);for(Throwable cause=e;cause!=null;cause=cause.getCause())if(cause instanceof StaleCapture)throw (StaleCapture)cause;throw e;}
        }finally{ui.post(()->{if(token==generation.get()&&connectionActive()&&controller!=null)controller.setCaptureMode(false);});}
    }
    static String captureError(int error){
        String reason;switch(error){case 2:reason="접근성 캡처 권한 없음 · PXE 접근성을 다시 켜세요";break;case 3:reason="캡처 간격이 너무 짧습니다";break;case 4:reason="캡처할 화면을 찾지 못했습니다";break;case 6:reason="보안 화면으로 보고되어 캡처가 거부되었습니다 · 화면 정보도 앱이 제공하는 글자만 읽을 수 있습니다";break;default:reason="화면 캡처 응답 오류 · 해당 블루스택 창과 PXE 접근성을 다시 시작하세요";}
        return "OCR 화면 캡처 실패("+error+") · "+reason;
    }
    private void input(String text,long token)throws Exception{input(text,token,"");}
    private void input(String text,long token,String target)throws Exception{
        AiStartGate gate=firstAiInput.get();awaitAiInputApp(gate,token);String firstTarget=gate==null?"":gate.inputTarget();if(!firstTarget.isEmpty()&&!target.isEmpty()&&!firstTarget.equals(target))throw new TargetChanged();final String boundTarget=firstTarget.isEmpty()?target:firstTarget;
        Callable<Boolean> writeText=()->{AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return false;if(!boundTarget.isEmpty()&&!boundTarget.contentEquals(root.getPackageName())){root.recycle();throw new TargetChanged();}AccessibilityNodeInfo focus=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);if(focus==null)focus=editable(root,0);boolean result=false;if(focus!=null){Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);result=focus.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args);focus.recycle();}root.recycle();return result;};boolean ok=boundTarget.isEmpty()?onUi(writeText,token):recoveryUi(boundTarget,token,writeText);if(ok&&!firstTarget.isEmpty())gate.inputDispatched();
        if(!ok)throw new IllegalStateException("입력 칸을 먼저 터치하세요. 이 앱의 입력 칸이 접근성 입력을 지원하지 않을 수도 있습니다");
    }
    private AccessibilityNodeInfo editable(AccessibilityNodeInfo n,int depth){if(depth>60)return null;if(n.isEditable()&&n.isVisibleToUser())return AccessibilityNodeInfo.obtain(n);for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo c=n.getChild(i);if(c!=null){AccessibilityNodeInfo found=editable(c,depth+1);c.recycle();if(found!=null)return found;}}return null;}
}
