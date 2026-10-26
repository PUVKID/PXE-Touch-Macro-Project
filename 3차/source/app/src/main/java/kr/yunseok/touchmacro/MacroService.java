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
    private final AtomicLong generation=new AtomicLong();
    public volatile boolean running=false, paused=false;
    public volatile String status="준비";
    private WindowManager wm;private FloatingController controller;
    private long startedAt=0;
    private final ArrayDeque<String> logs=new ArrayDeque<>();
    private TextRecognizer recognizer;
    private int initialW,initialH;private long deadline,lastScreenshot=0;
    private boolean destroyed=false,logUpdateQueued=false;
    private void notification(){if(controller!=null)controller.show();}
    private void clearNotification(){}
    @Override protected void onServiceConnected(){current=new java.lang.ref.WeakReference<>(this);wm=(WindowManager)getSystemService(WINDOW_SERVICE);controller=new FloatingController(this);recognizer=TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());log("접근성 서비스 연결됨");}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){}
    @Override public void onInterrupt(){stop();}
    @Override public void onConfigurationChanged(android.content.res.Configuration cfg){super.onConfigurationChanged(cfg);if(running){stop();log("화면 방향 변경 · 정지");}if(controller!=null){if(controller.recording)controller.endRecording();controller.refresh();}}
    @Override public void onDestroy(){
        destroyed=true;stop();if(controller!=null)controller.close();if(recognizer!=null)recognizer.close();worker.shutdownNow();if(get()==this)current=new java.lang.ref.WeakReference<>(null);super.onDestroy();
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private void font(View v){if(v instanceof TextView)((TextView)v).setTypeface(getResources().getFont(R.font.nanum_gothic_bold));if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)font(((ViewGroup)v).getChildAt(i));}
    public synchronized String logText(){return String.join("\n",logs);}
    public synchronized void log(String s){
        String line=new java.text.SimpleDateFormat("HH:mm:ss",Locale.KOREA).format(new java.util.Date())+"  "+s;
        logs.addLast(line);while(logs.size()>150)logs.removeFirst();status=s;
        if(!logUpdateQueued){logUpdateQueued=true;ui.postDelayed(()->{synchronized(MacroService.this){getSharedPreferences("macros",0).edit().putString("last_log",String.join("\n",logs)).apply();logUpdateQueued=false;}if(controller!=null)controller.update();},250);}
    }
    public FloatingController controller(){return controller;}
    public void showControls(){ui.post(()->{if(!destroyed&&controller!=null)controller.show();});}
    public void hideControls(){ui.post(()->{if(controller!=null)controller.close();});}
    public void pickPoint(){ui.post(()->controller.beginPoint());}
    public int[] size(){android.util.DisplayMetrics m=new android.util.DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(m);return new int[]{m.widthPixels,m.heightPixels};}
    public void togglePause(){if(!running)return;paused=!paused;log(paused?"일시정지":"계속 실행");}
    public String clockText(){if(!running)return Store.load(this).steps.size()+"개 동작 · "+(controller!=null&&controller.recording?"녹화 중":"준비");long elapsed=Math.max(0,SystemClock.elapsedRealtime()-startedAt)/1000;String time=String.format(Locale.KOREA,"%02d:%02d:%02d",elapsed/3600,elapsed/60%60,elapsed%60);if(deadline>0){long rem=Math.max(0,deadline-SystemClock.elapsedRealtime())/1000;return time+" · 남은 "+String.format(Locale.KOREA,"%02d:%02d",rem/60,rem%60);}return time;}
    public void replayRecorded(Step step,Runnable complete){
        try{GestureDescription g=description(GesturePaths.forStep(step),size()[0],size()[1],0);if(!dispatchGesture(g,new GestureResultCallback(){@Override public void onCompleted(GestureDescription d){complete.run();}@Override public void onCancelled(GestureDescription d){log("녹화 제스처 재생 취소");complete.run();}},ui)){log("녹화 제스처 재생 불가");complete.run();}}catch(Exception e){log("재생 실패 · "+e.getMessage());complete.run();}
    }
    public void start(Macro original){
        if(running)return;if(controller!=null&&controller.recording)throw new IllegalStateException("녹화를 먼저 끝내세요");original.validate();
        Macro m=new Macro(original.json());long token=generation.incrementAndGet();running=true;paused=false;showControls();notification();
        worker.execute(()->run(m,token));
    }
    public void stop(){generation.incrementAndGet();running=false;paused=false;clearNotification();log("정지");}
    private void check(long token)throws Exception {
        if(token!=generation.get()||destroyed)throw new CancellationException();
        if(deadline>0&&SystemClock.elapsedRealtime()>deadline)throw new IllegalStateException("최대 실행 시간에 도달했습니다");
        if(!((PowerManager)getSystemService(POWER_SERVICE)).isInteractive())throw new IllegalStateException("화면이 꺼져 실행을 중단했습니다");
        if(((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked())throw new IllegalStateException("잠금 화면에서는 실행할 수 없습니다");
        int[] s=size();if(initialW>0&&(initialW!=s[0]||initialH!=s[1]))throw new IllegalStateException("화면 방향이 바뀌었습니다. 같은 방향에서 다시 실행하세요");
    }
    private void ready(long token)throws Exception{check(token);while(paused){Thread.sleep(70);check(token);}}
    private void sleep(long ms,long token)throws Exception{
        long remaining=ms;while(remaining>0){ready(token);long chunk=Math.min(70,remaining),before=SystemClock.elapsedRealtime();Thread.sleep(chunk);if(!paused)remaining-=Math.max(1,SystemClock.elapsedRealtime()-before);}ready(token);
    }
    private <T> T await(CompletableFuture<T> f,long token,long max)throws Exception{
        long activeElapsed=0,last=SystemClock.elapsedRealtime();
        while(true){check(token);long now=SystemClock.elapsedRealtime();if(!paused)activeElapsed+=now-last;last=now;try{return f.get(70,TimeUnit.MILLISECONDS);}catch(TimeoutException e){if(activeElapsed>max){IllegalStateException timeout=new IllegalStateException("화면 응답 시간이 초과되었습니다");f.completeExceptionally(timeout);throw timeout;}}catch(ExecutionException e){Throwable cause=e.getCause();throw new IllegalStateException(cause.getMessage(),cause);}}
    }
    private <T> T onUi(Callable<T> action,long token)throws Exception{
        ready(token);CompletableFuture<T> f=new CompletableFuture<>();Runnable task=new Runnable(){public void run(){try{if(f.isDone())return;check(token);if(paused){ui.postDelayed(this,70);return;}f.complete(action.call());}catch(Exception e){f.completeExceptionally(e);}}};ui.post(task);return await(f,token,5000);
    }
    @SuppressWarnings("deprecation") private void run(Macro m,long token){
        PowerManager.WakeLock lock=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(Ui.prefs(this).getBoolean("keep_screen",true)?PowerManager.SCREEN_DIM_WAKE_LOCK:PowerManager.PARTIAL_WAKE_LOCK,"NEX:run");
        try{
            int[] sz=size();initialW=sz[0];initialH=sz[1];startedAt=SystemClock.elapsedRealtime();deadline=m.maxMinutes==0?0:SystemClock.elapsedRealtime()+m.maxMinutes*60000L;
            lock.acquire(m.maxMinutes==0?24*60*60000L:m.maxMinutes*60000L+10000);
            log("시작까지 "+m.startDelay+"ms");sleep(m.startDelay,token);
            for(int round=1;m.rounds==0||round<=m.rounds;round++){
                int index=round==1?m.startAt-1:0;while(index<m.steps.size()){
                    ready(token);Step s=m.steps.get(index);if(!s.enabled){index++;continue;}
                    log(round+"회 · "+(index+1)+"/"+m.steps.size()+" "+s.name());
                    Integer jump=null;for(int r=0;r<s.repeat;r++){ready(token);jump=execute(s,token);sleep(jitter(s.after,s.afterJitter,600000),token);if(jump!=null)break;}
                    index=jump==null?index+1:jump;
                }
                if(m.rounds==0||round<m.rounds)sleep(m.gap,token);
            }
            if(token==generation.get())log("완료");
        }catch(CancellationException ignored){}catch(Exception e){if(token==generation.get())log("중단 · "+(e.getMessage()==null?e.toString():e.getMessage()));}
        finally{if(lock.isHeld())lock.release();if(token==generation.get()){running=false;paused=false;clearNotification();ui.post(()->{if(controller!=null)controller.update();});}}
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
                for(int attempt=0;;attempt++){
                    Hit hit=waitText(s,token);
                    if(hit!=null&&s.type.equals("TEXT_TAP")){ready(token);hit=find(s,token);}
                    if(hit!=null){
                        ready(token);final Hit foundHit=hit;log("인식 성공 · “"+hit.text+"”");if(s.type.equals("TEXT_CHECK"))onUi(()->{controller.showHit(foundHit.rect,s.text);return true;},token);
                        if(s.type.equals("TEXT_TAP")){
                            gesture(Math.max(0,Math.min(100,hit.rect.exactCenterX()*100.0/initialW+s.offsetX)),Math.max(0,Math.min(100,hit.rect.exactCenterY()*100.0/initialH+s.offsetY)),hit.rect.exactCenterX()*100.0/initialW,hit.rect.exactCenterY()*100.0/initialH,100,token);
                        }
                        if(s.type.equals("TEXT_BRANCH"))return s.found==0?null:s.found-1;
                        return null;
                    }
                    if(s.type.equals("TEXT_BRANCH"))return s.missing==0?null:s.missing-1;
                    if(attempt<s.retries&&Arrays.asList("BACK_RETRY","RETRY","SCROLL_RETRY").contains(s.failure)){if("BACK_RETRY".equals(s.failure)){log("글자 없음 · 뒤로가기 "+(attempt+1)+"/"+s.retries);global(GLOBAL_ACTION_BACK,token);}else if("SCROLL_RETRY".equals(s.failure)){log("글자 없음 · 스크롤 "+(attempt+1)+"/"+s.retries);gesture(50,75,50,30,450,token);}else log("글자 없음 · 재확인 "+(attempt+1)+"/"+s.retries);sleep(s.retryDelay,token);continue;}
                    if("SKIP".equals(s.failure)){log("글자 없음 · 다음 동작");return null;}
                    throw new IllegalStateException("글자를 찾지 못했습니다: "+s.text);
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
    private static final class Hit {final Rect rect;final String text;Hit(Rect r,String t){rect=new Rect(r);text=t;}}
    private Hit waitText(Step s,long token)throws Exception{
        long elapsed=0;do{ready(token);long before=SystemClock.elapsedRealtime();Hit h=find(s,token);if(h!=null)return h;elapsed+=SystemClock.elapsedRealtime()-before;if(elapsed>=s.timeout)return null;long delay=Math.min(600,s.timeout-elapsed);sleep(delay,token);elapsed+=delay;}while(true);
    }
    private Hit find(Step s,long token)throws Exception{
        ready(token);Hit hit=null;
        if(!s.source.equals("OCR"))hit=onUi(()->accessibleHit(s),token);
        if(hit!=null||s.source.equals("ACCESS"))return hit;
        Bitmap bitmap=screenshot(token);
        boolean processing=false;
        try {
            CompletableFuture<com.google.mlkit.vision.text.Text> result=new CompletableFuture<>();
            com.google.android.gms.tasks.Task<com.google.mlkit.vision.text.Text> task=recognizer.process(InputImage.fromBitmap(bitmap,0));
            processing=true;task.addOnSuccessListener(result::complete).addOnFailureListener(result::completeExceptionally).addOnCompleteListener(done->{if(!bitmap.isRecycled())bitmap.recycle();});
            com.google.mlkit.vision.text.Text t=await(result,token,12000);
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
        }finally{if(!processing&&!bitmap.isRecycled())bitmap.recycle();}
    }
    private boolean inside(Step s,Rect r){double x=r.exactCenterX()*100.0/initialW,y=r.exactCenterY()*100.0/initialH;return x>=s.left&&x<=s.right&&y>=s.top&&y<=s.bottom;}
    private Hit accessibleHit(Step s){
        AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return null;
        ArrayList<Hit> list=new ArrayList<>();collect(root,s,list,0);root.recycle();
        list.sort(Comparator.comparingInt((Hit a)->a.rect.top).thenComparingInt(a->a.rect.left));
        return list.size()>=s.occurrence?list.get(s.occurrence-1):null;
    }
    private void collect(AccessibilityNodeInfo n,Step s,ArrayList<Hit> out,int depth){
        if(depth>70||out.size()>500)return;
        if(n.isVisibleToUser()){
            CharSequence txt=n.getText(),desc=n.getContentDescription();String value=txt!=null?txt.toString():desc==null?"":desc.toString();
            Rect r=new Rect();n.getBoundsInScreen(r);
            if(!r.isEmpty()&&r.intersect(0,0,initialW,initialH)&&inside(s,r)&&TextRules.matches(value,s.text,s.match,s.ignoreSpaces,s.ignoreCase)){
                boolean duplicate=false;for(Hit h:out)if(h.rect.equals(r))duplicate=true;if(!duplicate)out.add(new Hit(r,value));
            }
        }
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo child=n.getChild(i);if(child!=null){collect(child,s,out,depth+1);child.recycle();}}
    }
    private Bitmap screenshot(long token)throws Exception{
        onUi(()->{controller.setVisible(false);return true;},token);
        try{
            sleep(Math.max(150,700-(SystemClock.elapsedRealtime()-lastScreenshot)),token);lastScreenshot=SystemClock.elapsedRealtime();CompletableFuture<Bitmap> f=new CompletableFuture<>();
            ui.post(()->{try{check(token);takeScreenshot(Display.DEFAULT_DISPLAY,getMainExecutor(),new TakeScreenshotCallback(){
                @Override public void onSuccess(ScreenshotResult r){
                    Bitmap copy=null;try{Bitmap hardware=Bitmap.wrapHardwareBuffer(r.getHardwareBuffer(),r.getColorSpace());if(hardware==null)throw new IllegalStateException("화면 이미지가 비었습니다");copy=hardware.copy(Bitmap.Config.ARGB_8888,false);hardware.recycle();if(!f.complete(copy))copy.recycle();}catch(Exception e){if(copy!=null)copy.recycle();f.completeExceptionally(e);}finally{r.getHardwareBuffer().close();}
                }
                @Override public void onFailure(int error){f.completeExceptionally(new IllegalStateException("OCR 화면 캡처 실패("+error+"). 보안 화면이면 화면 정보 모드를 사용하세요"));}
            });}catch(Exception e){f.completeExceptionally(e);}});
            try{return await(f,token,8000);}catch(Exception e){f.thenAccept(b->{if(b!=null&&!b.isRecycled())b.recycle();});throw e;}
        }finally{ui.post(()->{controller.setVisible(true);});}
    }
    private void input(String text,long token)throws Exception{
        boolean ok=onUi(()->{AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return false;AccessibilityNodeInfo focus=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);if(focus==null)focus=editable(root,0);boolean result=false;if(focus!=null){Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);result=focus.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args);focus.recycle();}root.recycle();return result;},token);
        if(!ok)throw new IllegalStateException("입력 칸을 먼저 터치하세요. 이 앱의 입력 칸이 접근성 입력을 지원하지 않을 수도 있습니다");
    }
    private AccessibilityNodeInfo editable(AccessibilityNodeInfo n,int depth){if(depth>60)return null;if(n.isEditable()&&n.isVisibleToUser())return AccessibilityNodeInfo.obtain(n);for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo c=n.getChild(i);if(c!=null){AccessibilityNodeInfo found=editable(c,depth+1);c.recycle();if(found!=null)return found;}}return null;}
}
