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
    private final QueryLane screenReader=new QueryLane();
    private final QueryLane windowReader=new QueryLane();
    private final WindowPackages windowPackages=new WindowPackages();
    interface WindowSource {List<WindowPackages.Window> read();}
    private WindowSource windowSource=this::windowSnapshot;
    interface ScreenSource {ArrayList<Hit> read(long token)throws Exception;}
    private ScreenSource accessibilitySource=this::accessibleSnapshot;
    private volatile boolean accessibleFallback=false;
    private volatile long queryStartedAt=0;
    private final CapturePacer capturePacer=new CapturePacer(SystemClock.elapsedRealtime());
    private final CaptureRequests captureRequests=new CaptureRequests();
    private final AtomicReference<RecognitionRequest> pendingRecognition=new AtomicReference<>();
    private volatile String capturePhase="";
    private volatile String recognitionPhase="";
    interface ScreenshotSource {void take(java.util.concurrent.Executor executor,TakeScreenshotCallback callback);}
    private ScreenshotSource screenshotSource=(executor,callback)->takeScreenshot(Display.DEFAULT_DISPLAY,executor,callback);
    private final AtomicLong generation=new AtomicLong();private final AtomicLong pauseRevision=new AtomicLong();
    public volatile boolean running=false, paused=false, watching=false; public volatile int watchCompleted=0; public volatile String watchPhase=""; private volatile long watchNextRefresh=0;
    public volatile String status="준비";
    private WindowManager wm;private FloatingController controller;
    private volatile long startedAt=0,finishedAt=0,cycleStartedAt=0,lastCycleMs=0,delayUntil=0;
    private volatile String liveAction="준비";
    private volatile String lastAction="";
    private volatile int currentPriority=0;
    private long pauseStartedAt=0;
    private final ArrayDeque<String> logs=new ArrayDeque<>();
    private TextRecognizer recognizer;
    private int initialW,initialH;private long deadline;
    private volatile boolean destroyed=false,connected=false;private boolean logUpdateQueued=false;
    private ErrorJournal errorJournal;
    private void notification(){if(controller!=null)controller.show();}
    private void clearNotification(){}
    @Override protected void onServiceConnected(){if(controller!=null)controller.dispose();if(recognizer!=null)recognizer.close();connected=true;current=new java.lang.ref.WeakReference<>(this);errorJournal=new ErrorJournal(getFilesDir());wm=(WindowManager)getSystemService(WINDOW_SERVICE);controller=new FloatingController(this);recognizer=TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());log("접근성 서비스 연결됨");}
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
        if(type==AccessibilityEvent.TYPE_WINDOWS_CHANGED){if((event.getWindowChanges()&AccessibilityEvent.WINDOWS_CHANGE_REMOVED)!=0)windowPackages.remove(id,time);return;}
        if(type==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED||type==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED){CharSequence name=event.getPackageName();if(name!=null)windowPackages.observe(id,name.toString(),time);}
    }
    @Override public void onInterrupt(){stop();}
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
    private void recordError(String kind){recordError(kind,"");}
    private void recordError(String kind,String detail){
        try{if(errorJournal!=null)errorJournal.append(kind,"실행="+running+", 일시정지="+paused+", 캡처대기="+captureRequests.size()+", 인식대기="+(pendingRecognition.get()!=null)+", 단계="+(watching?watchPhase:liveAction)+(detail.isEmpty()?"":"\n"+detail));}
        catch(java.io.IOException e){log("오류 기록 파일 저장 실패 · 저장 공간을 확인하세요");}
    }
    private String errorKind(Exception error){
        for(Throwable cause=error;cause!=null;cause=cause.getCause()){
            if(cause instanceof CaptureFailure)return "OCR_CAPTURE_ERROR_"+((CaptureFailure)cause).error;
            if(cause instanceof ScreenReadFailure)return "SCREEN_INFORMATION_TIMEOUT";
        }
        String message=error.getMessage()==null?"":error.getMessage();
        if(message.contains("OCR 인식 응답"))return "OCR_RECOGNITION_TIMEOUT";
        if(message.contains("대상 앱 창 확인"))return "TARGET_WINDOW_TIMEOUT";
        return "RUN_STOP_"+error.getClass().getSimpleName();
    }
    public String activePackage(){return windowPackages.resolve(windowSource.read(),getPackageName());}
    private List<WindowPackages.Window> windowSnapshot(){
        List<AccessibilityWindowInfo> windows=getWindows();ArrayList<WindowPackages.Window> result=new ArrayList<>();
        try{for(AccessibilityWindowInfo window:windows)result.add(new WindowPackages.Window(window.getId(),window.getType(),window.isActive(),window.isFocused()));}
        finally{for(AccessibilityWindowInfo window:windows)window.recycle();}return result;
    }
    private String foregroundPackage(long token)throws Exception{
        ready(token);CompletableFuture<List<WindowPackages.Window>> result=windowReader.submit(windowSource::read);
        try{return windowPackages.resolve(await(result,token,1500,"대상 앱 창 확인 응답이 지연되었습니다. 터치하지 않고 정지합니다"),getPackageName());}
        finally{result.cancel(false);}
    }
    public boolean hasRunStats(){return startedAt>0;}
    private void resetStats(){startedAt=SystemClock.elapsedRealtime();finishedAt=0;cycleStartedAt=0;lastCycleMs=0;delayUntil=0;currentPriority=0;liveAction="시작 준비";lastAction="";}
    private void finishStats(){finishedAt=SystemClock.elapsedRealtime();delayUntil=0;}
    private String remainingText(long ms){long seconds=Math.max(0,(ms+999)/1000);return seconds>=60?(seconds/60)+"분 "+(seconds%60)+"초":seconds+"초";}
    public String progressText(){
        String phase=watching&&!watchPhase.equals("매크로 실행")?watchPhase:liveAction;
        if(watching&&currentPriority>0)phase=currentPriority+"순위 · "+phase;
        if(running&&queryStartedAt>0)phase+=" · 화면 정보 읽는 중 "+String.format(Locale.KOREA,"%.1f초",(SystemClock.elapsedRealtime()-queryStartedAt)/1000.0);
        if(running&&!capturePhase.isEmpty())phase+=" · "+capturePhase+(delayUntil>SystemClock.elapsedRealtime()?" · "+remainingText(delayUntil-SystemClock.elapsedRealtime()):"");
        if(running&&!recognitionPhase.isEmpty())phase+=" · "+recognitionPhase;
        if(!running)phase=status;if(paused)return "일시정지 · "+phase;
        boolean refreshWait=watching&&(watchPhase.equals("완료 후 쉬는 중")||watchPhase.equals("새로고침 대기")||watchPhase.equals("중복 차단 · 사라짐 대기"));
        if(refreshWait)phase+=" · 새로고침까지 "+remainingText(watchNextRefresh-SystemClock.elapsedRealtime());
        else if(running&&delayUntil>SystemClock.elapsedRealtime()&&(!watching||watchPhase.equals("매크로 실행")||watchPhase.startsWith("새로고침")||watchPhase.equals("시작 대기")||watchPhase.startsWith("뒤로가기 완료")))phase+=" · "+String.format(Locale.KOREA,"%.1f초 남음",(delayUntil-SystemClock.elapsedRealtime())/1000.0);
        return phase+(lastAction.isEmpty()?"":"\n직전: "+lastAction);
    }
    public String timingText(){long now=finishedAt>0?finishedAt:SystemClock.elapsedRealtime(),total=startedAt==0?0:Math.max(0,now-startedAt),cycle=cycleStartedAt==0?0:Math.max(0,now-cycleStartedAt);return "이번 한 바퀴 "+String.format(Locale.KOREA,"%.1f초",cycle/1000.0)+" · 직전 "+(lastCycleMs==0?"—":String.format(Locale.KOREA,"%.1f초",lastCycleMs/1000.0))+"\n전체 경과 "+(total/60000)+"분 "+(total/1000%60)+"초 · 완료 "+watchCompleted+"바퀴";}
    // 설정 안내가 숨겼던 컨트롤도 실제 실행 중에는 정지 접근을 복원합니다.
    public void showControls(){ui.post(()->{if(connectionActive()&&controller!=null){controller.show();if(running)controller.setVisible(true);}});}
    public void hideControls(){ui.post(()->{if(controller!=null)controller.close();});}
    public void pickPoint(){ui.post(()->controller.beginPoint());}
    public int[] size(){android.util.DisplayMetrics m=new android.util.DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(m);return new int[]{m.widthPixels,m.heightPixels};}
    public void togglePause(){if(!running)return;paused=!paused;if(paused)pauseStartedAt=SystemClock.elapsedRealtime();else if(pauseStartedAt>0){if(watching)watchNextRefresh+=SystemClock.elapsedRealtime()-pauseStartedAt;pauseStartedAt=0;}pauseRevision.incrementAndGet();log(paused?"일시정지":"계속 실행");}
    public String clockText(){if(watching&&!watchPhase.equals("매크로 실행")){long seconds=Math.max(0,(watchNextRefresh-SystemClock.elapsedRealtime()+999)/1000);return "탐지 "+watchCompleted+"회 · "+watchPhase+" · 새로고침 "+seconds+"초";}if(!running)return Store.load(this).steps.size()+"개 동작 · "+(controller!=null&&controller.recording?"녹화 중":"준비");long elapsed=Math.max(0,SystemClock.elapsedRealtime()-startedAt)/1000;String time=String.format(Locale.KOREA,"%02d:%02d:%02d",elapsed/3600,elapsed/60%60,elapsed%60);if(deadline>0){long rem=Math.max(0,deadline-SystemClock.elapsedRealtime())/1000;return time+" · 남은 "+String.format(Locale.KOREA,"%02d:%02d",rem/60,rem%60);}return time;}
    public void replayRecorded(Step step,Runnable complete){
        try{GestureDescription g=description(GesturePaths.forStep(step),size()[0],size()[1],0);if(!dispatchGesture(g,new GestureResultCallback(){@Override public void onCompleted(GestureDescription d){complete.run();}@Override public void onCancelled(GestureDescription d){log("녹화 제스처 재생 취소");complete.run();}},ui)){log("녹화 제스처 재생 불가");complete.run();}}catch(Exception e){log("재생 실패 · "+e.getMessage());complete.run();}
    }
    public void start(Macro original){
        if(!connectionActive())throw new IllegalStateException("PXE 접근성을 다시 켜세요");
        if(running)return;if(controller!=null&&controller.recording)throw new IllegalStateException("녹화를 먼저 끝내세요");original.validate();
        Macro m=new Macro(original.json());long token=generation.incrementAndGet();accessibleFallback=false;running=true;paused=false;showControls();notification();
        worker.execute(()->run(m,token));
    }
    public void stop(){generation.incrementAndGet();if(running)finishStats();running=false;paused=false;watching=false;watchPhase="";clearNotification();log("정지");}
    private void check(long token)throws Exception {
        if(token!=generation.get()||!connectionActive())throw new CancellationException();
        if(deadline>0&&SystemClock.elapsedRealtime()>deadline)throw new IllegalStateException("최대 실행 시간에 도달했습니다");
        if(!((PowerManager)getSystemService(POWER_SERVICE)).isInteractive())throw new IllegalStateException("화면이 꺼져 실행을 중단했습니다");
        if(((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked())throw new IllegalStateException("잠금 화면에서는 실행할 수 없습니다");
        int[] s=size();if(initialW>0&&(initialW!=s[0]||initialH!=s[1]))throw new IllegalStateException("화면 방향이 바뀌었습니다. 같은 방향에서 다시 실행하세요");
    }
    private void ready(long token)throws Exception{check(token);while(paused){Thread.sleep(70);check(token);}}
    private void sleep(long ms,long token)throws Exception{
        long remaining=ms;delayUntil=SystemClock.elapsedRealtime()+remaining;try{while(remaining>0){ready(token);delayUntil=SystemClock.elapsedRealtime()+remaining;long chunk=Math.min(70,remaining),before=SystemClock.elapsedRealtime();Thread.sleep(chunk);if(!paused)remaining-=Math.max(1,SystemClock.elapsedRealtime()-before);}ready(token);}finally{delayUntil=0;}
    }
    private <T> T await(CompletableFuture<T> f,long token,long max)throws Exception{
        return await(f,token,max,"화면 응답 시간이 초과되었습니다");
    }
    private <T> T await(CompletableFuture<T> f,long token,long max,String timeoutMessage)throws Exception{
        long activeElapsed=0,last=SystemClock.elapsedRealtime();
        while(true){check(token);long now=SystemClock.elapsedRealtime();if(!paused)activeElapsed+=now-last;last=now;try{return f.get(70,TimeUnit.MILLISECONDS);}catch(TimeoutException e){if(activeElapsed>max){IllegalStateException timeout=new IllegalStateException(timeoutMessage);f.completeExceptionally(timeout);throw timeout;}}catch(ExecutionException e){Throwable cause=e.getCause();throw new IllegalStateException(cause.getMessage(),cause);}}
    }
    private <T> T onUi(Callable<T> action,long token)throws Exception{
        ready(token);CompletableFuture<T> f=new CompletableFuture<>();Runnable task=new Runnable(){public void run(){try{if(f.isDone())return;check(token);if(paused){ui.postDelayed(this,70);return;}f.complete(action.call());}catch(Exception e){f.completeExceptionally(e);}}};ui.post(task);return await(f,token,5000);
    }
    private <T> T readScreen(Callable<T> action,long token)throws Exception{
        ready(token);queryStartedAt=SystemClock.elapsedRealtime();
        CompletableFuture<T> result=screenReader.submit(()->{check(token);return action.call();});
        try{return await(result,token,3500);}
        catch(CancellationException e){throw e;}
        catch(Exception e){throw new ScreenReadFailure("화면 정보 응답 지연 · 화면 정보 모드를 완료하지 못했습니다 ("+e.getMessage()+")",e);}
        finally{result.cancel(false);queryStartedAt=0;}
    }
    private static final class ScreenReadFailure extends IllegalStateException {ScreenReadFailure(String message,Throwable cause){super(message,cause);}}
    @SuppressWarnings("deprecation") private void run(Macro m,long token){
        PowerManager.WakeLock lock=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(Ui.prefs(this).getBoolean("keep_screen",true)?PowerManager.SCREEN_DIM_WAKE_LOCK:PowerManager.PARTIAL_WAKE_LOCK,"PXE:run");
        try{
            int[] sz=size();initialW=sz[0];initialH=sz[1];resetStats();watchCompleted=0;deadline=m.maxMinutes==0?0:SystemClock.elapsedRealtime()+m.maxMinutes*60000L;
            lock.acquire(m.maxMinutes==0?24*60*60000L:m.maxMinutes*60000L+10000);
            liveAction="시작 대기";log("시작까지 "+Times.label(m.startDelay));sleep(m.startDelay,token);
            runSequence(m,token);
            if(token==generation.get())log("완료");
        }catch(CancellationException ignored){}catch(Exception e){if(token==generation.get()){recordError(errorKind(e));log("중단 · "+(e.getMessage()==null?e.toString():e.getMessage()));}}
        finally{if(lock.isHeld())lock.release();if(token==generation.get()){finishStats();running=false;paused=false;clearNotification();ui.post(()->{if(controller!=null)controller.update();});}}
    }
    private void runSequence(Macro m,long token)throws Exception{
            for(int round=1;m.rounds==0||round<=m.rounds;round++){
                if(!watching)cycleStartedAt=SystemClock.elapsedRealtime();
                int index=round==1?m.startAt-1:0;while(index<m.steps.size()){
                    ready(token);Step s=m.steps.get(index);if(!s.enabled){index++;continue;}
                    log(round+"회 · "+(index+1)+"/"+m.steps.size()+" "+s.name());
                    Integer jump=null;for(int r=0;r<s.repeat;r++){
                        ready(token);liveAction="매크로 "+round+"/"+(m.rounds==0?"∞":m.rounds)+"회 · "+(index+1)+"번 "+s.name()+(s.repeat>1?" "+(r+1)+"/"+s.repeat:"");
                        jump=execute(s,token);lastAction=(index+1)+"번 "+s.name()+" 완료"+(s.repeat>1?" ("+(r+1)+"/"+s.repeat+")":"");log(lastAction);
                        String next=jump!=null?(jump+1)+"번으로 이동":r+1<s.repeat?"같은 동작 반복":index+1<m.steps.size()?(index+2)+"번 "+m.steps.get(index+1).name():m.rounds==0||round<m.rounds?"다음 전체 반복":watching?"뒤로가기 준비":"매크로 완료";
                        liveAction="완료 → "+next;if(s.canWaitForever()&&s.immediate)ready(token);else sleep(s.after==0&&s.afterJitter==0?0:jitter(s.after,s.afterJitter,600000),token);if(jump!=null)break;
                    }
                    index=jump==null?index+1:jump;
                }
                if(!watching){lastCycleMs=SystemClock.elapsedRealtime()-cycleStartedAt;cycleStartedAt=0;watchCompleted++;}
                if(m.rounds==0||round<m.rounds){liveAction="다음 반복 대기 · "+round+"회 완료";sleep(m.gap,token);}
            }

    }
    public void startWatch(WatchConfig original){
        if(!connectionActive())throw new IllegalStateException("PXE 접근성을 다시 켜세요");
        if(running)throw new IllegalStateException("먼저 실행을 정지하세요");
        if(controller!=null&&controller.recording)throw new IllegalStateException("녹화를 먼저 끝내세요");
        original.validate();Macro m=WatchStore.resolve(this,original);WatchConfig p=new WatchConfig(original.json());
        long token=generation.incrementAndGet();accessibleFallback=false;watching=true;running=true;paused=false;watchCompleted=0;watchPhase="시작 대기";
        showControls();ui.post(()->{if(controller!=null)controller.minimize();});
        worker.execute(()->runWatch(p,m,token));
    }
    @SuppressWarnings("deprecation") private void runWatch(WatchConfig p,Macro m,long token){
        PowerManager.WakeLock lock=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(Ui.prefs(this).getBoolean("keep_screen",true)?PowerManager.SCREEN_DIM_WAKE_LOCK:PowerManager.PARTIAL_WAKE_LOCK,"PXE:watch");
        long overall=0;
        try{
            int[] sz=size();initialW=sz[0];initialH=sz[1];resetStats();
            overall=p.maxMinutes==0?0:startedAt+p.maxMinutes*60000L;deadline=overall;
            lock.acquire(p.maxMinutes==0?24*60*60000L:p.maxMinutes*60000L+10000);
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
                        watchPhase=(area+1)+"순위 확인";Hit candidate=find(areaConfigs[area].rule,token,frame);ready(token);
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
                        ready(token);if(p.tapFound)hit=find(areaConfigs[selected].rule,token,new ScanFrame(p.targetPackage));ready(token);if(scanRevision!=pauseRevision.get())continue;
                        if(!p.targetPackage.equals(foregroundPackage(token))){for(WatchGate gate:gates)gate.interruptAbsence();continue;}
                        if(hit!=null){
                            if(p.preventDuplicate){WatchStore.blocked(this,areaConfigs[selected],true);gates[selected].reserve();}
                            currentPriority=selected+1;cycleStartedAt=SystemClock.elapsedRealtime();watchPhase="매크로 실행";liveAction="글자 터치 · 들어가기";log((selected+1)+"순위에서 발견 · "+p.rule.text+" → "+m.name+" "+p.repetitions+"회");
                            deadline=m.watchDeadline(SystemClock.elapsedRealtime(),overall);
                            if(p.tapFound){double x=hit.rect.exactCenterX()*100.0/initialW,y=hit.rect.exactCenterY()*100.0/initialH;gesture(x,y,x,y,100,token);liveAction="들어간 화면 대기";sleep(p.entryMs,token);}
                            sleep(m.startDelay,token);runSequence(m,token);
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
                sleep(Math.min(2000,remaining),token);
            }
        }catch(CancellationException ignored){}catch(Exception e){if(token==generation.get()){recordError(errorKind(e));log("탐지 중단 · "+(e.getMessage()==null?e.toString():e.getMessage()));}}
        finally{if(lock.isHeld())lock.release();if(token==generation.get()){finishStats();running=false;watching=false;paused=false;watchPhase="";ui.post(()->{if(controller!=null)controller.update();});}}
    }
    private Integer execute(Step s,long token)throws Exception{
        switch(s.type){
            case "TAP":gesture(s.x,s.y,s.x,s.y,jitter(s.duration,s.durationJitter,59000),token);break;
            case "DOUBLE":gesture(s.x,s.y,s.x,s.y,60,token);sleep(100,token);gesture(s.x,s.y,s.x,s.y,60,token);break;
            case "LONG":gesture(s.x,s.y,s.x,s.y,jitter(s.duration,s.durationJitter,59000),token);break;
            case "SWIPE":gesture(s.x,s.y,s.x2,s.y2,Math.min(59000,s.duration),token);break;
            case "CURVE":case "PINCH_IN":case "PINCH_OUT":case "MULTI_TAP":case "GESTURE":multiGesture(GesturePaths.forStep(s),s.durationJitter,token);break;
            case "NOTIFICATIONS":global(GLOBAL_ACTION_NOTIFICATIONS,token);break;
            case "WAIT":sleep(jitter(s.duration,s.durationJitter,600000),token);break;
            case "BACK":global(GLOBAL_ACTION_BACK,token);break;
            case "HOME":global(GLOBAL_ACTION_HOME,token);break;
            case "RECENTS":global(GLOBAL_ACTION_RECENTS,token);break;
            case "INPUT":input(s.text,token);break;
            case "APP":onUi(()->{Intent i=getPackageManager().getLaunchIntentForPackage(s.packageName);if(i==null)throw new IllegalStateException("앱을 찾을 수 없습니다: "+s.packageName);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);return true;},token);break;
            case "GOTO":return s.target-1;
            case "STOP":stop();throw new CancellationException();
            case "TEXT_TAP":case "TEXT_WAIT":case "TEXT_BRANCH":case "TEXT_CHECK":
                String touchTarget="";long targetWaitElapsed=0;
                for(int attempt=0;;){
                    try{
                    // 첫 탐지와 재확인, 실제 터치가 같은 앱에 속하도록 이 동작의 대상을 보존합니다.
                    if(s.type.equals("TEXT_TAP")){
                        long beforeTarget=SystemClock.elapsedRealtime();String foreground=foregroundPackage(token);
                        if(touchTarget.isEmpty()&&!foreground.isEmpty())touchTarget=foreground;
                        if(touchTarget.isEmpty()||!touchTarget.equals(foreground)){
                            liveAction="다음 글자 대기 · 대상 앱 확인 대기";sleep(100,token);targetWaitElapsed+=SystemClock.elapsedRealtime()-beforeTarget;
                            if(!s.waitForever&&targetWaitElapsed>=s.timeout)throw new IllegalStateException("대상 앱 확인 시간이 초과되어 글자 터치를 정지합니다");
                            continue;
                        }
                        targetWaitElapsed=0;
                    }
                    long scanRevision=pauseRevision.get();
                    Hit hit=waitText(s,token,touchTarget);
                    if(hit!=null&&s.type.equals("TEXT_TAP")&&!s.immediate){ready(token);hit=find(s,token,new ScanFrame(touchTarget));}
                    ready(token);if(scanRevision!=pauseRevision.get())continue;
                    // 계속 기다리기에서는 재확인 때 사라진 글자도 같은 대상 앱에서 새로 탐색합니다.
                    if(hit==null&&s.type.equals("TEXT_TAP")&&s.waitForever){liveAction="다음 글자 대기 · 재확인 글자 없음";sleep(100,token);continue;}
                    if(hit!=null){
                        ready(token);final Hit foundHit=hit;log("인식 성공 · “"+hit.text+"”");if(s.type.equals("TEXT_CHECK"))onUi(()->{controller.showHit(foundHit.rect,s.text);return true;},token);
                        if(s.type.equals("TEXT_TAP")){
                            requireRecognitionTarget(touchTarget,token);
                            gesture(Math.max(0,Math.min(100,hit.rect.exactCenterX()*100.0/initialW+s.offsetX)),Math.max(0,Math.min(100,hit.rect.exactCenterY()*100.0/initialH+s.offsetY)),hit.rect.exactCenterX()*100.0/initialW,hit.rect.exactCenterY()*100.0/initialH,100,token);
                        }
                        if(s.type.equals("TEXT_BRANCH"))return s.found==0?null:s.found-1;
                        return null;
                    }
                    if(s.type.equals("TEXT_BRANCH"))return s.missing==0?null:s.missing-1;
                    if(attempt<s.retries&&Arrays.asList("BACK_RETRY","RETRY","SCROLL_RETRY").contains(s.failure)){if("BACK_RETRY".equals(s.failure)){log("글자 없음 · 뒤로가기 "+(attempt+1)+"/"+s.retries);global(GLOBAL_ACTION_BACK,token);}else if("SCROLL_RETRY".equals(s.failure)){log("글자 없음 · 스크롤 "+(attempt+1)+"/"+s.retries);gesture(50,75,50,30,450,token);}else log("글자 없음 · 재확인 "+(attempt+1)+"/"+s.retries);sleep(s.retryDelay,token);attempt++;continue;}
                    if("SKIP".equals(s.failure)){log("글자 없음 · 다음 동작");return null;}
                    recordError("TEXT_NOT_FOUND","source="+s.source+", match="+s.match+", 영역="+s.left+","+s.top+","+s.right+","+s.bottom+", waitForever="+s.waitForever+", immediate="+s.immediate+", timeout_ms="+s.timeout);
                    throw new IllegalStateException("글자를 찾지 못했습니다: "+s.text);
                    }catch(StaleCapture|TargetChanged e){ready(token);}
                }
        }return null;
    }
    private void global(int action,long token)throws Exception{if(!onUi(()->performGlobalAction(action),token))throw new IllegalStateException("시스템 동작을 실행하지 못했습니다");}
    private long jitter(long value,int range,long max){if(range==0)return Math.max(1,value);return Math.max(1,Math.min(max,value+java.util.concurrent.ThreadLocalRandom.current().nextLong(-range,(long)range+1)));}
    private GestureDescription description(org.json.JSONArray strokes,int width,int height,int jitter)throws Exception{
        GesturePaths.validate(strokes);long base=0;for(int i=0;i<strokes.length();i++){org.json.JSONObject o=strokes.getJSONObject(i);base=Math.max(base,o.getInt("start")+o.getInt("duration"));}
        double scale=jitter==0?1:jitter(base,jitter,59000)/(double)base;GestureDescription.Builder builder=new GestureDescription.Builder();
        for(int i=0;i<strokes.length();i++){org.json.JSONObject o=strokes.getJSONObject(i);org.json.JSONArray points=o.getJSONArray("points");Path p=new Path();for(int k=0;k<points.length();k++){org.json.JSONArray a=points.getJSONArray(k);float x=(float)Math.min(width-1,a.getDouble(0)*width/100),y=(float)Math.min(height-1,a.getDouble(1)*height/100);if(k==0)p.moveTo(x,y);else p.lineTo(x,y);}long start=(long)(o.getInt("start")*scale),duration=Math.max(1,(long)(o.getInt("duration")*scale));builder.addStroke(new GestureDescription.StrokeDescription(p,start,duration));}
        return builder.build();
    }
    private void gesture(double x,double y,double x2,double y2,long duration,long token)throws Exception{
        org.json.JSONArray points=new org.json.JSONArray();points.put(GesturePaths.point(x,y));if(x!=x2||y!=y2)points.put(GesturePaths.point(x2,y2));org.json.JSONArray strokes=new org.json.JSONArray();strokes.put(GesturePaths.stroke(points,0,(int)duration));multiGesture(strokes,0,token);
    }
    private void multiGesture(org.json.JSONArray strokes,int variation,long token)throws Exception{
        boolean hidden=onUi(()->controller.prepareGesture(strokes,initialW,initialH),token);
        try{sleep(hidden?80:20,token);ready(token);GestureDescription g=description(strokes,initialW,initialH,variation);CompletableFuture<Boolean> result=new CompletableFuture<>();
            Runnable dispatch=new Runnable(){@Override public void run(){try{if(result.isDone())return;check(token);if(paused){ui.postDelayed(this,70);return;}if(!dispatchGesture(g,new GestureResultCallback(){@Override public void onCompleted(GestureDescription d){result.complete(true);}@Override public void onCancelled(GestureDescription d){result.complete(false);}},ui))result.complete(false);}catch(Exception e){result.completeExceptionally(e);}}};ui.post(dispatch);
            if(!await(result,token,65000))throw new IllegalStateException("제스처가 취소되었습니다");
        }finally{if(hidden)ui.post(()->controller.setVisible(true));}
    }
    static final class Hit {final Rect rect;final String text;Hit(Rect r,String t){rect=new Rect(r);text=t;}}
    private Hit waitText(Step s,long token,String target)throws Exception{
        long elapsed=0;
        do{
            ready(token);long before=SystemClock.elapsedRealtime(),revision=pauseRevision.get();
            if(!target.isEmpty()||s.canWaitForever()&&(s.waitForever||s.immediate)){
                String foreground=foregroundPackage(token);if(target.isEmpty()&&!foreground.isEmpty())target=foreground;
                if(target.isEmpty()||!target.equals(foreground)){if(s.type.equals("TEXT_TAP"))throw new TargetChanged();liveAction="다음 글자 대기 · 대상 앱 확인 대기";sleep(100,token);continue;}
                liveAction="다음 글자 대기 · 인식되면 바로 진행";
            }
            Hit h;
            try{h=find(s,token,new ScanFrame(target));}catch(TargetChanged e){if(s.type.equals("TEXT_TAP"))throw e;continue;}
            ready(token);
            if(revision!=pauseRevision.get())throw new StaleCapture();
            if(!target.isEmpty()&&!target.equals(foregroundPackage(token)))continue;
            if(h!=null)return h;
            elapsed+=SystemClock.elapsedRealtime()-before;if(!s.waitForever&&elapsed>=s.timeout)return null;
            long delay=s.waitForever?100:Math.min(s.immediate?100:600,s.timeout-elapsed);sleep(delay,token);elapsed+=delay;
        }while(true);
    }
    private static final class ScanFrame {
        final String targetPackage;com.google.mlkit.vision.text.Text text;ArrayList<Hit> accessible;
        ScanFrame(){this("");}ScanFrame(String targetPackage){this.targetPackage=targetPackage;}
    }
    private Hit find(Step s,long token)throws Exception{return find(s,token,new ScanFrame());}
    private Hit find(Step s,long token,ScanFrame frame)throws Exception{
        ready(token);Hit hit=null;
        if(!s.source.equals("OCR")&&(!accessibleFallback||s.source.equals("ACCESS"))){
            try{if(frame.accessible==null)frame.accessible=readScreen(()->accessibilitySource.read(token),token);hit=accessibleHit(s,frame.accessible);}
            catch(ScreenReadFailure e){
                if(s.source.equals("ACCESS"))throw e;
                accessibleFallback=true;ready(token);log("화면 정보 응답 지연 · 이 실행은 화면 OCR로 전환합니다");
            }
        }
        if(hit!=null||s.source.equals("ACCESS"))return hit;
        if(frame.text!=null)return ocrHit(s,frame.text);
        frame.text=recognizeText(token,frame.targetPackage);return ocrHit(s,frame.text);
    }
    private static final class RecognitionRequest {
        final Bitmap bitmap;final long revision;
        final CompletableFuture<com.google.mlkit.vision.text.Text> result=new CompletableFuture<>();
        private final AtomicBoolean completed=new AtomicBoolean();
        RecognitionRequest(Bitmap bitmap,long revision){this.bitmap=bitmap;this.revision=revision;}
        void complete(com.google.mlkit.vision.text.Text text,Exception error){
            if(!completed.compareAndSet(false,true))return;
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
                Bitmap bitmap=screenshot(token);RecognitionRequest request=new RecognitionRequest(bitmap,pauseRevision.get());
                try{
                    ready(token);requireRecognitionTarget(targetPackage,token);pendingRecognition.set(request);recognitionPhase="한글 OCR 인식 중 · 정지 가능";
                    com.google.android.gms.tasks.Task<com.google.mlkit.vision.text.Text> task=recognizer.process(InputImage.fromBitmap(bitmap,0));
                    task.addOnCompleteListener(getMainExecutor(),done->{
                        try{request.complete(done.isSuccessful()?done.getResult():null,done.isSuccessful()?null:done.isCanceled()?new IllegalStateException("한글 OCR 인식이 취소되었습니다"):done.getException());}
                        finally{pendingRecognition.compareAndSet(request,null);}
                    });
                }catch(Exception error){request.complete(null,error);pendingRecognition.compareAndSet(request,null);throw error;}
                try{
                    // 대기용 future만 만료시켜 원래 Task의 완료와 이미지 해제는 보존합니다.
                    com.google.mlkit.vision.text.Text text=awaitRecognition(request,targetPackage,token);
                    ready(token);if(request.revision!=pauseRevision.get())throw new StaleCapture();return text;
                }catch(Exception error){
                    if(error.getCause()!=null||!"한글 OCR 인식 응답 시간이 초과되었습니다".equals(error.getMessage()))throw error;
                    recordError("OCR_RECOGNITION_TIMEOUT");log("OCR 인식 응답 지연 · 오래된 결과를 버리고 새 화면으로 복구 대기 · 정지 가능");
                }
            }
        }finally{recognitionPhase="";}
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
                catch(CaptureTimeout e){recordError("OCR_CAPTURE_TIMEOUT");log("OCR 캡처 응답 지연 · 터치 없이 복구 대기 · 정지 가능");}
                catch(StaleCapture e){ready(token);throw e;}
                catch(Exception e){
                    CaptureFailure failure=null;for(Throwable cause=e;cause!=null;cause=cause.getCause())if(cause instanceof CaptureFailure){failure=(CaptureFailure)cause;break;}
                    if(failure==null||!recoverableCapture(failure.error))throw e;
                    recordError("OCR_CAPTURE_ERROR_"+failure.error);
                    if(failure.error==ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT)capturePacer.rejected(SystemClock.elapsedRealtime());
                    capturePhase="캡처 간격 조정 · 터치 없이 복구 대기";log(capturePhase);sleep(1000,token);
                }
            }
        }finally{capturePhase="";}
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
            sleep(150,token);CompletableFuture<Bitmap> f=new CompletableFuture<>();long revision=pauseRevision.get();
            ui.post(new Runnable(){public void run(){try{if(f.isDone())return;check(token);if(paused){ui.postDelayed(this,70);return;}
                long requestId=captureRequests.begin();if(requestId==0)throw new IllegalStateException("이전 OCR 캡처 2개가 아직 진행 중입니다");
                try{screenshotSource.take(getMainExecutor(),new TakeScreenshotCallback(){
                @Override public void onSuccess(ScreenshotResult r){
                    capturePacer.responded(SystemClock.elapsedRealtime());captureRequests.complete(requestId);
                    Bitmap copy=null;try{if(token!=generation.get()||f.isDone()){f.cancel(false);return;}if(paused||revision!=pauseRevision.get()){f.completeExceptionally(new StaleCapture());return;}Bitmap hardware=Bitmap.wrapHardwareBuffer(r.getHardwareBuffer(),r.getColorSpace());if(hardware==null)throw new IllegalStateException("화면 이미지가 비었습니다");copy=hardware.copy(Bitmap.Config.ARGB_8888,false);hardware.recycle();if(!f.complete(copy))copy.recycle();}catch(Exception e){if(copy!=null&&!copy.isRecycled())copy.recycle();f.completeExceptionally(e);}finally{r.getHardwareBuffer().close();}
                }
                @Override public void onFailure(int error){capturePacer.responded(SystemClock.elapsedRealtime());captureRequests.complete(requestId);CaptureFailure failure=new CaptureFailure(error);if(!f.completeExceptionally(failure))recordError("OCR_CAPTURE_LATE_ERROR_"+error);}
            });}catch(Exception e){capturePacer.responded(SystemClock.elapsedRealtime());captureRequests.complete(requestId);throw e;}
            }catch(Exception e){f.completeExceptionally(e);}}});
            try{return await(f,token,8000,"OCR 화면 캡처 응답 시간이 초과되었습니다");}catch(Exception e){f.thenAccept(b->{if(b!=null&&!b.isRecycled())b.recycle();});f.completeExceptionally(e);if("OCR 화면 캡처 응답 시간이 초과되었습니다".equals(e.getMessage()))throw new CaptureTimeout(e);for(Throwable cause=e;cause!=null;cause=cause.getCause())if(cause instanceof StaleCapture)throw (StaleCapture)cause;throw e;}
        }finally{ui.post(()->{if(controller!=null)controller.setCaptureMode(false);});}
    }
    static String captureError(int error){
        String reason;switch(error){case 2:reason="접근성 캡처 권한 없음 · PXE 접근성을 다시 켜세요";break;case 3:reason="캡처 간격이 너무 짧습니다";break;case 4:reason="캡처할 화면을 찾지 못했습니다";break;case 6:reason="보안 화면입니다 · 화면 정보 모드를 사용하세요";break;default:reason="화면 캡처 응답 오류 · 해당 블루스택 창과 PXE 접근성을 다시 시작하세요";}
        return "OCR 화면 캡처 실패("+error+") · "+reason;
    }
    private void input(String text,long token)throws Exception{
        boolean ok=onUi(()->{AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return false;AccessibilityNodeInfo focus=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);if(focus==null)focus=editable(root,0);boolean result=false;if(focus!=null){Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);result=focus.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args);focus.recycle();}root.recycle();return result;},token);
        if(!ok)throw new IllegalStateException("입력 칸을 먼저 터치하세요. 이 앱의 입력 칸이 접근성 입력을 지원하지 않을 수도 있습니다");
    }
    private AccessibilityNodeInfo editable(AccessibilityNodeInfo n,int depth){if(depth>60)return null;if(n.isEditable()&&n.isVisibleToUser())return AccessibilityNodeInfo.obtain(n);for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo c=n.getChild(i);if(c!=null){AccessibilityNodeInfo found=editable(c,depth+1);c.recycle();if(found!=null)return found;}}return null;}
}
