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
    private WindowManager wm;private View overlay,picker;private TouchLabel badge;private Button play;
    private WindowManager.LayoutParams overlayParams;
    private final ArrayDeque<String> logs=new ArrayDeque<>();
    private TextRecognizer recognizer;
    private int initialW,initialH;private long deadline,lastScreenshot=0;
    private boolean destroyed=false;
    private void notification(){NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);nm.createNotificationChannel(new NotificationChannel("run","매크로 실행",NotificationManager.IMPORTANCE_LOW));Intent stop=new Intent(this,StopReceiver.class);PendingIntent pi=PendingIntent.getBroadcast(this,1,stop,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent open=PendingIntent.getActivity(this,2,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);nm.notify(10,new Notification.Builder(this,"run").setSmallIcon(R.drawable.ic_macro).setContentTitle("윤석 터치매크로 실행 중").setContentText("누르면 편집 화면 · 정지 버튼으로 중단").setContentIntent(open).setOngoing(true).addAction(new Notification.Action.Builder(null,"정지",pi).build()).build());}
    private void clearNotification(){((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(10);}

    @Override protected void onServiceConnected(){current=new java.lang.ref.WeakReference<>(this);wm=(WindowManager)getSystemService(WINDOW_SERVICE);recognizer=TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());log("접근성 서비스 연결됨");}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){}
    @Override public void onInterrupt(){stop();}
    @Override public void onDestroy(){
        destroyed=true;stop();hideControls();closePicker();if(recognizer!=null)recognizer.close();worker.shutdownNow();if(get()==this)current=new java.lang.ref.WeakReference<>(null);super.onDestroy();
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private void font(View v){if(v instanceof TextView)((TextView)v).setTypeface(getResources().getFont(R.font.nanum_gothic_bold));if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)font(((ViewGroup)v).getChildAt(i));}
    public synchronized String logText(){return String.join("\n",logs);}
    public synchronized void log(String s){
        String line=new java.text.SimpleDateFormat("HH:mm:ss",Locale.KOREA).format(new java.util.Date())+"  "+s;
        logs.addLast(line);while(logs.size()>150)logs.removeFirst();status=s;
        getSharedPreferences("macros",0).edit().putString("last_log",String.join("\n",logs)).apply();
        ui.post(()->{if(badge!=null)badge.setText(s);if(play!=null)play.setText(!running?"시작":paused?"계속":"일시정지");});
    }
    private Button control(LinearLayout box,String text,Runnable action){
        Button b=new Button(this);b.setText(text);b.setTextSize(11);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(3),0,dp(3),0);box.addView(b,new LinearLayout.LayoutParams(dp(63),dp(44)));b.setOnClickListener(v->action.run());return b;
    }
    public void showControls(){ui.post(()->{
        if(destroyed||overlay!=null)return;
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(6),dp(4),dp(6),dp(4));box.setBackgroundColor(Color.rgb(235,240,255));
        badge=new TouchLabel(this);badge.setText(status);badge.setTextColor(Color.rgb(24,38,75));badge.setTextSize(11);badge.setMaxLines(2);box.addView(badge,new LinearLayout.LayoutParams(dp(260),dp(35)));
        LinearLayout buttons=new LinearLayout(this);box.addView(buttons);
        play=control(buttons,"시작",()->{if(running){paused=!paused;log(paused?"일시정지":"계속 실행");}else{try{start(Store.load(this));}catch(Exception e){log(e.getMessage());}}});
        control(buttons,"정지",this::stop);control(buttons,"좌표",()->{if(running){log("먼저 정지한 뒤 좌표를 선택하세요");return;}pickPoint();});
        control(buttons,"편집",()->{stop();Intent i=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);startActivity(i);});
        overlayParams=new WindowManager.LayoutParams(dp(272),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        overlayParams.gravity=Gravity.TOP|Gravity.START;overlayParams.x=dp(12);overlayParams.y=dp(80);overlay=box;font(box);
        badge.setOnTouchListener(new View.OnTouchListener(){float sx,sy;int ox,oy;public boolean onTouch(View v,android.view.MotionEvent e){if(e.getAction()==0){sx=e.getRawX();sy=e.getRawY();ox=overlayParams.x;oy=overlayParams.y;return true;}if(e.getAction()==2){overlayParams.x=Math.max(0,Math.min(size()[0]-dp(272),ox+(int)(e.getRawX()-sx)));overlayParams.y=Math.max(0,Math.min(size()[1]-dp(90),oy+(int)(e.getRawY()-sy)));if(overlay!=null)wm.updateViewLayout(overlay,overlayParams);return true;}if(e.getAction()==MotionEvent.ACTION_UP)v.performClick();return true;}});
        wm.addView(overlay,overlayParams);
    });}
    public void hideControls(){ui.post(()->{if(overlay!=null){try{wm.removeView(overlay);}catch(Exception ignored){}overlay=null;badge=null;play=null;}});}
    public int[] size(){android.util.DisplayMetrics m=new android.util.DisplayMetrics();wm.getDefaultDisplay().getRealMetrics(m);return new int[]{m.widthPixels,m.heightPixels};}
    public void pickPoint(){ui.post(()->{
        if(picker!=null)return;if(overlay!=null)overlay.setVisibility(View.GONE);
        LinearLayout pane=new TouchPane(this);pane.setOrientation(LinearLayout.VERTICAL);pane.setBackgroundColor(0x182856D8);
        TextView title=new TextView(this);title.setText("좌표 선택\n원하는 곳을 한 번 누르세요. 실제 터치는 실행되지 않습니다.");title.setTextSize(15);title.setTextColor(Color.WHITE);title.setPadding(dp(16),dp(32),dp(16),dp(8));title.setBackgroundColor(0xDE18306A);pane.addView(title);
        Button cancel=new Button(this);cancel.setText("취소");pane.addView(cancel,new LinearLayout.LayoutParams(-1,dp(48)));cancel.setOnClickListener(v->closePicker());
        pane.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_UP){v.performClick();int[] sz=size();double x=e.getRawX()*100.0/sz[0],y=e.getRawY()*100.0/sz[1];getSharedPreferences("macros",0).edit().putString("picked",String.format(Locale.US,"%.4f,%.4f",x,y)).apply();closePicker();Intent i=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);startActivity(i);}return true;});
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.START;picker=pane;font(pane);wm.addView(picker,p);
    });}
    private void closePicker(){ui.post(()->{if(picker!=null){try{wm.removeView(picker);}catch(Exception ignored){}picker=null;}if(overlay!=null)overlay.setVisibility(View.VISIBLE);});}
    public void start(Macro original){
        if(running)return;original.validate();
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
    private void run(Macro m,long token){
        PowerManager.WakeLock lock=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"YunseokMacro:run");
        try{
            int[] sz=size();initialW=sz[0];initialH=sz[1];deadline=m.maxMinutes==0?0:SystemClock.elapsedRealtime()+m.maxMinutes*60000L;
            lock.acquire(m.maxMinutes==0?24*60*60000L:m.maxMinutes*60000L+10000);
            log("시작까지 "+m.startDelay+"ms");sleep(m.startDelay,token);
            for(int round=1;m.rounds==0||round<=m.rounds;round++){
                int index=0;while(index<m.steps.size()){
                    ready(token);Step s=m.steps.get(index);if(!s.enabled){index++;continue;}
                    log(round+"회 · "+(index+1)+"/"+m.steps.size()+" "+s.name());
                    Integer jump=null;for(int r=0;r<s.repeat;r++){ready(token);jump=execute(s,token);sleep(s.after,token);if(jump!=null)break;}
                    index=jump==null?index+1:jump;
                }
                if(m.rounds==0||round<m.rounds)sleep(m.gap,token);
            }
            if(token==generation.get())log("완료");
        }catch(CancellationException ignored){}catch(Exception e){if(token==generation.get())log("중단 · "+(e.getMessage()==null?e.toString():e.getMessage()));}
        finally{if(lock.isHeld())lock.release();if(token==generation.get()){running=false;paused=false;clearNotification();ui.post(()->{if(play!=null)play.setText("시작");});}}
    }
    private Integer execute(Step s,long token)throws Exception{
        switch(s.type){
            case "TAP":gesture(s.x,s.y,s.x,s.y,100,token);break;
            case "DOUBLE":gesture(s.x,s.y,s.x,s.y,60,token);sleep(100,token);gesture(s.x,s.y,s.x,s.y,60,token);break;
            case "LONG":gesture(s.x,s.y,s.x,s.y,Math.min(59000,s.duration),token);break;
            case "SWIPE":gesture(s.x,s.y,s.x2,s.y2,Math.min(59000,s.duration),token);break;
            case "WAIT":sleep(s.duration,token);break;
            case "BACK":global(GLOBAL_ACTION_BACK,token);break;
            case "HOME":global(GLOBAL_ACTION_HOME,token);break;
            case "RECENTS":global(GLOBAL_ACTION_RECENTS,token);break;
            case "INPUT":input(s.text,token);break;
            case "APP":onUi(()->{Intent i=getPackageManager().getLaunchIntentForPackage(s.packageName);if(i==null)throw new IllegalStateException("앱을 찾을 수 없습니다: "+s.packageName);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);return true;},token);break;
            case "GOTO":return s.target-1;
            case "STOP":stop();throw new CancellationException();
            case "TEXT_TAP":case "TEXT_WAIT":case "TEXT_BRANCH":
                for(int attempt=0;;attempt++){
                    Hit hit=waitText(s,token);
                    if(hit!=null){
                        ready(token);
                        if(s.type.equals("TEXT_TAP")){
                            // Re-read after a pause so we never touch stale screen coordinates.
                            hit=find(s,token);if(hit==null)throw new IllegalStateException("터치 직전에 글자가 사라졌습니다");
                            gesture(hit.rect.exactCenterX()*100.0/initialW,hit.rect.exactCenterY()*100.0/initialH,hit.rect.exactCenterX()*100.0/initialW,hit.rect.exactCenterY()*100.0/initialH,100,token);
                        }
                        if(s.type.equals("TEXT_BRANCH"))return s.found==0?null:s.found-1;
                        return null;
                    }
                    if(s.type.equals("TEXT_BRANCH"))return s.missing==0?null:s.missing-1;
                    if("BACK_RETRY".equals(s.failure)&&attempt<s.retries){log("글자 없음 · 뒤로가기 "+(attempt+1)+"/"+s.retries);global(GLOBAL_ACTION_BACK,token);sleep(s.retryDelay,token);continue;}
                    if("SKIP".equals(s.failure)){log("글자 없음 · 다음 동작");return null;}
                    throw new IllegalStateException("글자를 찾지 못했습니다: "+s.text);
                }
        }return null;
    }
    private void global(int action,long token)throws Exception{if(!onUi(()->performGlobalAction(action),token))throw new IllegalStateException("시스템 동작을 실행하지 못했습니다");}
    private void gesture(double x,double y,double x2,double y2,long duration,long token)throws Exception{
        onUi(()->{if(overlay!=null)overlay.setVisibility(View.GONE);return true;},token);
        try {sleep(80,token);ready(token);CompletableFuture<Boolean> f=new CompletableFuture<>();
        Runnable dispatch=new Runnable(){public void run(){try{check(token);if(paused){ui.postDelayed(this,70);return;}
            Path p=new Path();float ax=(float)Math.min(initialW-1,x*initialW/100),ay=(float)Math.min(initialH-1,y*initialH/100);p.moveTo(ax,ay);
            if(x!=x2||y!=y2)p.lineTo((float)Math.min(initialW-1,x2*initialW/100),(float)Math.min(initialH-1,y2*initialH/100));
            GestureDescription g=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(p,0,Math.max(1,duration))).build();
            if(!dispatchGesture(g,new GestureResultCallback(){@Override public void onCompleted(GestureDescription g){f.complete(true);}@Override public void onCancelled(GestureDescription g){f.complete(false);}},ui))f.complete(false);
        }catch(Exception e){f.completeExceptionally(e);}}};ui.post(dispatch);
        if(!await(f,token,duration+5000))throw new IllegalStateException("터치가 취소되었습니다. 화면 상태를 확인하세요");
        }finally{ui.post(()->{if(overlay!=null)overlay.setVisibility(View.VISIBLE);});}
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
                for(com.google.mlkit.vision.text.Text.Line line:b.getLines()){
                    boolean elementMatch=false;
                    for(com.google.mlkit.vision.text.Text.Element e:line.getElements()){
                        if(e.getBoundingBox()!=null&&TextRules.matches(e.getText(),s.text,s.match,s.ignoreSpaces,s.ignoreCase)){candidates.add(new Hit(e.getBoundingBox(),e.getText()));elementMatch=true;}
                    }
                    if(!elementMatch&&line.getBoundingBox()!=null&&TextRules.matches(line.getText(),s.text,s.match,s.ignoreSpaces,s.ignoreCase))candidates.add(new Hit(line.getBoundingBox(),line.getText()));
                }
                if(candidates.isEmpty()&&b.getBoundingBox()!=null&&TextRules.matches(b.getText(),s.text,s.match,s.ignoreSpaces,s.ignoreCase))candidates.add(new Hit(b.getBoundingBox(),b.getText()));
            }
            candidates.sort(Comparator.comparingInt((Hit a)->a.rect.top).thenComparingInt(a->a.rect.left));
            return candidates.size()>=s.occurrence?candidates.get(s.occurrence-1):null;
        }finally{if(!processing&&!bitmap.isRecycled())bitmap.recycle();}
    }
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
            if(!r.isEmpty()&&r.intersect(0,0,initialW,initialH)&&TextRules.matches(value,s.text,s.match,s.ignoreSpaces,s.ignoreCase)){
                boolean duplicate=false;for(Hit h:out)if(h.rect.equals(r))duplicate=true;if(!duplicate)out.add(new Hit(r,value));
            }
        }
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo child=n.getChild(i);if(child!=null){collect(child,s,out,depth+1);child.recycle();}}
    }
    private Bitmap screenshot(long token)throws Exception{
        onUi(()->{if(overlay!=null)overlay.setVisibility(View.GONE);return true;},token);
        try{
            sleep(Math.max(150,700-(SystemClock.elapsedRealtime()-lastScreenshot)),token);lastScreenshot=SystemClock.elapsedRealtime();CompletableFuture<Bitmap> f=new CompletableFuture<>();
            ui.post(()->{try{check(token);takeScreenshot(Display.DEFAULT_DISPLAY,getMainExecutor(),new TakeScreenshotCallback(){
                @Override public void onSuccess(ScreenshotResult r){
                    Bitmap copy=null;try{Bitmap hardware=Bitmap.wrapHardwareBuffer(r.getHardwareBuffer(),r.getColorSpace());if(hardware==null)throw new IllegalStateException("화면 이미지가 비었습니다");copy=hardware.copy(Bitmap.Config.ARGB_8888,false);hardware.recycle();if(!f.complete(copy))copy.recycle();}catch(Exception e){if(copy!=null)copy.recycle();f.completeExceptionally(e);}finally{r.getHardwareBuffer().close();}
                }
                @Override public void onFailure(int error){f.completeExceptionally(new IllegalStateException("OCR 화면 캡처 실패("+error+"). 보안 화면이면 화면 정보 모드를 사용하세요"));}
            });}catch(Exception e){f.completeExceptionally(e);}});
            try{return await(f,token,8000);}catch(Exception e){f.thenAccept(b->{if(b!=null&&!b.isRecycled())b.recycle();});throw e;}
        }finally{ui.post(()->{if(overlay!=null)overlay.setVisibility(View.VISIBLE);});}
    }
    private void input(String text,long token)throws Exception{
        boolean ok=onUi(()->{AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return false;AccessibilityNodeInfo focus=root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);if(focus==null)focus=editable(root,0);boolean result=false;if(focus!=null){Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);result=focus.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args);focus.recycle();}root.recycle();return result;},token);
        if(!ok)throw new IllegalStateException("입력 칸을 먼저 터치하세요. 이 앱의 입력 칸이 접근성 입력을 지원하지 않을 수도 있습니다");
    }
    private AccessibilityNodeInfo editable(AccessibilityNodeInfo n,int depth){if(depth>60)return null;if(n.isEditable()&&n.isVisibleToUser())return AccessibilityNodeInfo.obtain(n);for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo c=n.getChild(i);if(c!=null){AccessibilityNodeInfo found=editable(c,depth+1);c.recycle();if(found!=null)return found;}}return null;}
}
