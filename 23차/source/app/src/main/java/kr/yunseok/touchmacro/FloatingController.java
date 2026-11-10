package kr.yunseok.touchmacro;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
public final class FloatingController {
 private final MacroService owner;private final WindowManager wm;private final Handler h=new Handler(Looper.getMainLooper());
 private View panel,capture,highlight;private RegionPicker regionPicker;private TextView status;private Button play;private SetupCoach coach;private boolean guidedRecording=false;private RunHud hud;
 private WindowManager.LayoutParams params,captureParams;private final ArrayList<View> markers=new ArrayList<>();private boolean captureMode=false,builtRunning=false,hudGestureHidden=false,gestureHidPanel=false;
 public boolean recording=false;private boolean once=false;private int replace=-1;private long session=0;private boolean collapsed=true, visible=true,disposed=false;private long lastReplayEnd=0;private int lastRecorded=-1;
 public FloatingController(MacroService s){owner=s;wm=(WindowManager)s.getSystemService(Context.WINDOW_SERVICE);}
 private Context ctx(){return Ui.themed(owner);}private int dp(float v){return Ui.dp(owner,v);}private int scaled(float v){return dp(v*Ui.prefs(owner).getInt("scale",100)/100f);}
 private WindowManager.LayoutParams layout(int w,int height,boolean touch){
  WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,height,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|(touch?0:WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.START;return p;
 }
 private GestureLease gestureLease;
 private BehaviorGestureLease behaviorGestureLease;private boolean behaviorShieldRegistered,behaviorBarRegistered;
 static final class BehaviorGestureLease {
  final FloatingController controller;final BehaviorAndroidBridge bridge;View shield,bar;boolean released;
  BehaviorGestureLease(FloatingController controller,BehaviorAndroidBridge bridge){this.controller=controller;this.bridge=bridge;}
 }
 private BehaviorAndroidBridge behavior;private View behaviorShield,behaviorBar;private TextView behaviorStatus;private WindowManager.LayoutParams behaviorShieldParams,behaviorBarParams;
 static final class GestureLease {final boolean waitForOverlay;GestureLease(boolean waitForOverlay){this.waitForOverlay=waitForOverlay;}}
 private boolean usable(){return !disposed&&owner.connectionActive();}
 public void show(){if(!usable()||panel!=null)return;KeepAliveService.show(owner);build();if(panel!=null)h.post(ticker);}
 public void refresh(){if(!usable()||panel==null)return;removePanel();build();}
 private final Runnable ticker=new Runnable(){@Override public void run(){if(!usable()||panel==null)return;if(builtRunning!=owner.running)refresh();if(!usable()||panel==null)return;if(status!=null)status.setText(caption());updatePlay();updateHud();h.postDelayed(this,250);}};
 private void updatePlay(){if(play!=null){play.setText(recording?"완료":!owner.running?"▶":owner.paused?"▶":"Ⅱ");play.setTextSize(recording?10:17);play.setContentDescription(recording?"녹화 완료":!owner.running?"글자 탐지 시작":owner.paused?"계속 실행":"일시정지");}}
 private void startDetection(){
  if(!Ui.prefs(owner).contains("watch_config")){toast("찾을 글자·영역·매크로를 먼저 설정하세요");openSettings(true);return;}
  owner.startWatch(WatchStore.load(owner));
 }
 private void updateHud(){if(panel==null)return;if(hud==null){if(hudGestureHidden)return;if(owner.running||owner.hasRunStats())hud=new RunHud(owner);}if(hud!=null){hud.avoid(panel==null||params==null?new Rect():new Rect(params.x,params.y,params.x+panel.getWidth(),params.y+panel.getHeight()));hud.update(owner.hudProgress(),owner.hudState(),owner.hudStage(),owner.hudTimingText(),visible&&!captureMode&&!recording&&!hudGestureHidden);}}
 public void showSetupCoach(String kind){
  if(owner.running||recording)throw new IllegalStateException("먼저 실행과 녹화를 정지하세요");
  show();if(coach!=null)coach.close();coach=new SetupCoach(owner);setVisible(false);
  Runnable back=()->{closeCoach();openWizard();};
  if(kind.equals("REGION"))coach.show("글자가 보이는 대상 앱의 목록 화면을 열고 누르세요.","영역 그리기",()->{
   closeCoach();regionPicker=new RegionPicker(owner,r->{WatchConfig p=WatchStore.load(owner);int index=SetupFlow.areaIndex(owner);String pkg=requireTargetPackage();if(p.priorityAreas.size()>1&&!p.targetPackage.isEmpty()&&!p.targetPackage.equals(pkg))throw new IllegalArgumentException("다른 영역과 같은 대상 앱에서 지정하세요");WatchArea area=new WatchArea(r.left,r.top,r.right,r.bottom);if(index<0||index>p.priorityAreas.size()||index>=6)throw new IllegalArgumentException("영역 순서를 다시 선택하세요");if(index==p.priorityAreas.size())p.priorityAreas.add(area);else p.priorityAreas.set(index,area);p.priorityAreas.get(0).apply(p.rule);p.targetPackage=pkg;WatchStore.save(owner,p);SetupFlow.stage(owner,1);h.postDelayed(this::openWizard,150);},()->showSetupCoach("REGION"));regionPicker.show();
  },back);
  else if(kind.equals("RECORD"))coach.show("글자를 직접 눌러 들어간 뒤, 안에서 할 동작만 녹화하세요.","녹화 시작",()->{SetupFlow.beginDraft(owner);closeCoach();guidedRecording=true;beginRecording(false,-1);},back);
  else coach.show("대상 앱의 목록 화면을 열고 시작하세요. 중복 방지가 적용됩니다.","이 화면에서 시작",()->{
   WatchConfig p=WatchStore.load(owner);String pkg=requireTargetPackage();if(!p.targetPackage.isEmpty()&&!p.targetPackage.equals(pkg))throw new IllegalStateException("영역을 지정했던 대상 앱으로 이동하세요");owner.startWatch(p);SetupFlow.finished(owner);closeCoach();
  },back);
 }
 private void closeCoach(){if(coach!=null){coach.close();coach=null;}setVisible(true);}
 private String requireTargetPackage(){String pkg=owner.activePackage();if(pkg.isEmpty())throw new IllegalStateException("대상 앱을 확인하지 못했습니다. 취소 후 대상 앱을 한 번 터치하고 다시 시도하세요");return pkg;}
 private void openWizard(){owner.startActivity(new Intent(owner,MainActivity.class).putExtra("watch",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP));}
 public void cancelRegion(){if(regionPicker!=null){regionPicker.close();regionPicker=null;}}
 public void pickWatchRegion(){if(owner.running||recording){toast("먼저 실행과 녹화를 정지하세요");return;}SetupFlow.stage(owner,1);openWizard();}
 public void minimize(){if(recording)return;collapsed=true;refresh();}
 public boolean isCollapsed(){return collapsed;}
 private void build(){
  if(!usable())return;
  builtRunning=owner.running;
  Context c=ctx();LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(0,0,0,0);box.setBackground(Ui.outline(c,Ui.card(c),14));box.setElevation(dp(2));box.setAlpha(Ui.prefs(owner).getInt("opacity",95)/100f);
  LinearLayout r=row(box);
  ImageButton logo=new ImageButton(c);logo.setImageResource(R.drawable.ic_macro);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setPadding(scaled(4),scaled(4),scaled(4),scaled(4));logo.setBackgroundColor(Color.TRANSPARENT);
  logo.setContentDescription(collapsed?"PXE 컨트롤 펼치기 · 길게 눌러 정지":"PXE 컨트롤 바로 접기");r.addView(logo,new LinearLayout.LayoutParams(scaled(48),scaled(48)));
  logo.setOnClickListener(v->{if(recording){toast("녹화를 먼저 끝내세요");return;}collapsed=!collapsed;refresh();});logo.setOnLongClickListener(v->{owner.stop();return true;});drag(logo);
  if(!collapsed||recording){
   play=button(r,recording?"완료":!owner.running?"▶":owner.paused?"▶":"Ⅱ",()->{if(recording){endRecording();return;}if(owner.running)owner.togglePause();else try{startDetection();}catch(Exception e){toast(e.getMessage());}});
   updatePlay();play.setTextColor(Ui.accent(c));
   Button stop=button(r,"■",()->{owner.stop();if(recording)endRecording();});stop.setContentDescription("정지");stop.setTextSize(17);
   Button more=button(r,"⋮",()->{if(recording){endRecording();return;}moreMenu();});more.setTextSize(22);more.setContentDescription(recording?"녹화 끝내기":"컨트롤 더보기");
  }
  if(collapsed&&!recording&&owner.running){play=button(r,"",owner::togglePause);updatePlay();play.setTextColor(Ui.accent(c));Button stop=button(r,"■",owner::stop);stop.setContentDescription("정지");stop.setTextSize(17);stop.setTextColor(Ui.accent(c));}
  params=layout(collapsed&&!recording?scaled(owner.running?144:48):scaled(192),scaled(48),true);int[] s=owner.size();params.x=Math.max(0,Math.min(s[0]-params.width,Ui.prefs(owner).getInt("panel_x",dp(6))));params.y=Math.max(dp(30),Math.min(s[1]-scaled(60),Ui.prefs(owner).getInt("panel_y",dp(95))));
  panel=box;Ui.font(box);
  // 시스템의 토큰 제거가 해제 콜백보다 먼저 도착하면 해당 연결을 종료합니다.
  try{wm.addView(panel,params);}catch(WindowManager.BadTokenException error){panel=null;status=null;play=null;owner.overlayTokenInvalid(error);return;}
  if(!visible)panel.setVisibility(View.GONE);rebuildMarkers();
 }
 private void openSettings(boolean watch){Intent i=new Intent(owner,MainActivity.class).putExtra(watch?"watch":"settings",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);owner.startActivity(i);}
 private void moreMenu(){choices("PXE",new String[]{"글자 탐지 시작","화면에서 탐지 영역 지정","탐지 설정","글자 찾아 터치","동작 / 녹화","실행 상태","앱 설정","컨트롤 닫기"},i->{switch(i){
  case 0:try{startDetection();}catch(Exception e){toast(e.getMessage());}break;
  case 1:pickWatchRegion();break;case 2:openSettings(true);break;case 3:wordDialog();break;
  case 4:toolsMenu();break;case 5:choices(owner.status+" · "+owner.clockText(),new String[]{"닫기"},n->{});break;
  case 6:openSettings(false);break;case 7:owner.stop();close();break;}});
 }
 private void toolsMenu(){choices("동작 / 녹화",new String[]{"동작 추가","녹화","동작 목록","저장","편집 화면","점 표시 켜기 / 끄기","글자 탐지 없이 매크로만 실행"},i->{switch(i){
  case 0:actionMenu();break;case 1:beginRecording(false,-1);break;case 2:sequenceDialog();break;case 3:saveDialog();break;
  case 4:owner.stop();openApp();break;case 5:Ui.prefs(owner).edit().putBoolean("markers",!Ui.prefs(owner).getBoolean("markers",true)).apply();rebuildMarkers();break;
  case 6:try{owner.start(Store.load(owner));minimize();}catch(Exception e){toast(e.getMessage());}break;}});
 }
 private LinearLayout row(LinearLayout b){LinearLayout r=new LinearLayout(ctx());b.addView(r);return r;}
 private Button button(LinearLayout r,String text,Runnable action){Button b=Ui.button(ctx(),text);b.setTextSize(10);b.setPadding(0,0,0,0);b.setBackgroundColor(Color.TRANSPARENT);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,scaled(48),1);p.setMargins(0,0,0,0);r.addView(b,p);b.setOnClickListener(v->action.run());return b;}
 @android.annotation.SuppressLint("ClickableViewAccessibility") private void drag(View v){v.setOnTouchListener(new View.OnTouchListener(){float sx,sy;int ox,oy;boolean moved;public boolean onTouch(View view,MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN){sx=e.getRawX();sy=e.getRawY();ox=params.x;oy=params.y;moved=false;return false;}if(e.getActionMasked()==MotionEvent.ACTION_MOVE){moved|=Math.abs(e.getRawX()-sx)+Math.abs(e.getRawY()-sy)>dp(5);if(moved){view.cancelLongPress();int[] s=owner.size();params.x=Math.max(0,Math.min(s[0]-params.width,ox+(int)(e.getRawX()-sx)));params.y=Math.max(dp(24),Math.min(s[1]-Math.max(scaled(44),panel==null?scaled(44):panel.getHeight()),oy+(int)(e.getRawY()-sy)));if(panel!=null)wm.updateViewLayout(panel,params);return true;}}if(e.getActionMasked()==MotionEvent.ACTION_UP){Ui.prefs(owner).edit().putInt("panel_x",params.x).putInt("panel_y",params.y).apply();if(moved)return true;return false;}return false;}});}
 public GestureLease prepareGesture(JSONArray strokes,int width,int height){
  RectF box=panel==null||params==null?new RectF():new RectF(params.x,params.y,params.x+panel.getWidth(),params.y+panel.getHeight());boolean panelHit=false,hudHit=false;
  // 모든 경로의 실제 창 좌표를 먼저 확인하며, HUD 생성도 전달 종료까지 막습니다.
  try{for(int i=0;i<strokes.length();i++){JSONArray points=strokes.getJSONObject(i).getJSONArray("points");float minX=width,minY=height,maxX=0,maxY=0;for(int j=0;j<points.length();j++){JSONArray p=points.getJSONArray(j);float x=(float)p.getDouble(0)*width/100,y=(float)p.getDouble(1)*height/100;minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);}RectF bounds=new RectF(minX-dp(5),minY-dp(5),maxX+dp(5),maxY+dp(5));panelHit|=RectF.intersects(box,bounds);hudHit|=hud!=null&&hud.intersects(bounds);}}
  catch(Exception ignored){panelHit=true;}
  GestureLease lease=new GestureLease(panelHit||hudHit);gestureLease=lease;hudGestureHidden=true;if(hud!=null)hud.visible(false);
  gestureHidPanel|=panelHit;if(panelHit)setVisible(false);return lease;
 }
 public void restoreGesture(GestureLease lease){if(lease==null||gestureLease!=lease)return;restoreGesture();}
 public void restoreGesture(){gestureLease=null;boolean restorePanel=gestureHidPanel;gestureHidPanel=false;hudGestureHidden=false;if(!usable())return;if(restorePanel)setVisible(true);else updateHud();}
 public void setVisible(boolean on){visible=on;boolean display=on&&behavior==null;if(panel!=null)panel.setVisibility(display?View.VISIBLE:View.GONE);for(View m:markers)m.setVisibility(display&&!captureMode?View.VISIBLE:View.GONE);if(highlight!=null)highlight.setVisibility(display&&!captureMode?View.VISIBLE:View.GONE);if(hud!=null)hud.visible(display&&!captureMode&&!recording&&!hudGestureHidden);}
 public void setCaptureMode(boolean on){captureMode=on;setVisible(visible);}
 public void resetHudPosition(){Ui.prefs(owner).edit().remove("hud_x").remove("hud_y").apply();if(hud!=null)hud.resetPosition();}
 public boolean shown(){return panel!=null;}
 private void removePanel(){if(panel!=null){try{wm.removeView(panel);}catch(Exception ignored){}panel=null;status=null;play=null;}clearMarkers();}
 public void close(){closeBehavior(behavior);if(coach!=null){coach.close();coach=null;}if(hud!=null){hud.close();hud=null;}if(regionPicker!=null){regionPicker.close();regionPicker=null;}endRecordingInternal();removePanel();if(highlight!=null){try{wm.removeView(highlight);}catch(Exception ignored){}highlight=null;}h.removeCallbacksAndMessages(null);KeepAliveService.close(owner);}
 // 일반 닫기와 달리 연결이 끝난 컨트롤은 다시 표시하지 않습니다.
 void dispose(){disposed=true;close();}
 void showBehavior(BehaviorAndroidBridge bridge){
  if(!usable()||!bridge.live())return;closeBehavior(behavior);if(behaviorShield!=null||behaviorBar!=null)throw new IllegalStateException("이전 AI 창을 정리하지 못했습니다");behavior=bridge;setVisible(false);
  behaviorShield=new View(ctx()){
   float x,y;boolean armed,moved;
   @Override public boolean performClick(){return super.performClick();}
   @Override public boolean onTouchEvent(MotionEvent e){
    if(e.getActionMasked()==MotionEvent.ACTION_DOWN){x=e.getRawX();y=e.getRawY();armed=bridge.teaching&&!bridge.busy.get()&&!owner.paused&&bridge.live();moved=false;}
    if(e.getPointerCount()!=1||Math.abs(e.getRawX()-x)+Math.abs(e.getRawY()-y)>dp(12))moved=true;
    if(e.getActionMasked()==MotionEvent.ACTION_CANCEL)armed=false;
    if(e.getActionMasked()==MotionEvent.ACTION_UP){performClick();if(armed&&!moved){armed=false;bridge.tapped(x,y);}}return true;
   }
  };
  behaviorShieldParams=layout(-1,-1,true);if(Ui.prefs(owner).getBoolean("keep_screen",true))behaviorShieldParams.flags|=WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
  LinearLayout bar=form();bar.setPadding(dp(4),0,dp(4),0);bar.setBackground(Ui.outline(owner,Ui.card(owner),10));
  LinearLayout controls=row(bar);button(controls,"Ⅱ / ▶",owner::togglePause);Button stop=button(controls,"■ 중단",owner::stop);stop.setContentDescription("AI 시범과 실행 즉시 중단");
  ScrollView details=new ScrollView(ctx());LinearLayout content=form();content.setPadding(0,0,0,0);details.addView(content);bar.addView(details,new LinearLayout.LayoutParams(-1,0,1));
  behaviorStatus=new TextView(ctx());behaviorStatus.setText("AI · 제목을 끌어 이동");behaviorStatus.setTextSize(11);behaviorStatus.setTextColor(Ui.ink(owner));behaviorStatus.setMinHeight(dp(32));behaviorStatus.setMaxLines(2);behaviorStatus.setEllipsize(android.text.TextUtils.TruncateAt.END);content.addView(behaviorStatus);
  if(bridge.teaching){LinearLayout actions=row(content);button(actions,"뒤로",()->bridge.choose(BehaviorExample.Type.BACK));button(actions,"위로",()->bridge.choose(BehaviorExample.Type.SCROLL_UP));button(actions,"아래로",()->bridge.choose(BehaviorExample.Type.SCROLL_DOWN));button(actions,"글자대기",()->bridge.choose(BehaviorExample.Type.WAIT_TEXT));button(row(content),"시범 완료 · 확인하고 학습",bridge::finishTeaching);}
  behaviorBar=bar;int[] screen=owner.size();int height=Math.min(screen[1],dp(192));behaviorBarParams=layout(Math.min(screen[0],dp(280)),height,true);behaviorBarParams.x=0;behaviorBarParams.y=Math.min(dp(28),Math.max(0,screen[1]-height));Ui.font(bar);
  behaviorStatus.setClickable(true);
  behaviorStatus.setOnTouchListener(new View.OnTouchListener(){
   float x,y;int ox,oy;boolean armed,dragged;
   @Override public boolean onTouch(View view,MotionEvent e){
    int action=e.getActionMasked();
    if(action==MotionEvent.ACTION_DOWN){x=e.getRawX();y=e.getRawY();ox=behaviorBarParams.x;oy=behaviorBarParams.y;armed=behavior==bridge;dragged=false;return true;}
    if(action==MotionEvent.ACTION_CANCEL){armed=false;return true;}
    if(!armed)return true;
    if(e.getPointerCount()!=1||Math.abs(e.getRawX()-x)+Math.abs(e.getRawY()-y)>android.view.ViewConfiguration.get(ctx()).getScaledTouchSlop())dragged=true;
    if(action==MotionEvent.ACTION_MOVE&&behavior==bridge){int[] s=owner.size();behaviorBarParams.x=Math.max(0,Math.min(s[0]-behaviorBar.getWidth(),ox+(int)(e.getRawX()-x)));behaviorBarParams.y=Math.max(0,Math.min(s[1]-behaviorBar.getHeight(),oy+(int)(e.getRawY()-y)));wm.updateViewLayout(behaviorBar,behaviorBarParams);}
    // 짧은 탭만 접근성 클릭으로 한 번 알리고 드래그의 손 뗌은 클릭으로 처리하지 않습니다.
    if(action==MotionEvent.ACTION_UP){boolean click=behavior==bridge&&!dragged;armed=false;if(click)view.performClick();}
    return true;
   }
  });
  try{wm.addView(behaviorShield,behaviorShieldParams);behaviorShieldRegistered=true;wm.addView(behaviorBar,behaviorBarParams);behaviorBarRegistered=true;}
  catch(RuntimeException error){closeBehavior(bridge);if(error instanceof WindowManager.BadTokenException)owner.overlayTokenInvalid((WindowManager.BadTokenException)error);else throw error;}
 }
 // 분리·복원은 UI 스레드에서 직렬화하며 이전 실행의 수명은 재사용하지 않습니다.
 private static boolean behaviorWindowPresent(View view){return view!=null&&(view.isAttachedToWindow()||view.getParent()!=null);}
 private boolean removeBehaviorWindow(View view,boolean registered){
  if(view==null)return true;
  try{if(registered||behaviorWindowPresent(view))wm.removeViewImmediate(view);if(behaviorWindowPresent(view))throw new IllegalStateException("AI 창 분리가 완료되지 않았습니다");return true;}
  catch(RuntimeException error){if(error instanceof IllegalArgumentException&&!behaviorWindowPresent(view))return true;owner.behaviorOverlayFailure(error);return false;}
 }
 void closeBehavior(BehaviorAndroidBridge bridge){
  if(behavior!=bridge)return;behavior=null;if(behaviorGestureLease!=null)behaviorGestureLease.released=true;behaviorGestureLease=null;
  if(removeBehaviorWindow(behaviorBar,behaviorBarRegistered)){behaviorBar=null;behaviorBarRegistered=false;}
  if(removeBehaviorWindow(behaviorShield,behaviorShieldRegistered)){behaviorShield=null;behaviorShieldRegistered=false;}
  behaviorStatus=null;if(usable())setVisible(true);
 }
 void behaviorMessage(BehaviorAndroidBridge bridge,String message){if(behavior==bridge&&behaviorStatus!=null)behaviorStatus.setText(message+" · 끌어 이동");}
 void behaviorReady(BehaviorAndroidBridge bridge){if(behavior==bridge&&behaviorGestureLease==null&&!behaviorShieldRegistered)owner.behaviorOverlayFailure(new IllegalStateException("AI 입력 차단창이 복원되지 않았습니다"));}
 void behaviorCapture(BehaviorAndroidBridge bridge){if(behavior!=bridge)throw new java.util.concurrent.CancellationException();if(behaviorGestureLease!=null||!behaviorShieldRegistered)throw new IllegalStateException("AI 입력 전달 정리가 끝나지 않았습니다");setVisible(false);}
 private boolean currentBehaviorLease(BehaviorGestureLease lease){return lease!=null&&lease.controller==this&&behaviorGestureLease==lease&&behavior==lease.bridge&&behaviorShield==lease.shield&&behaviorBar==lease.bar&&owner.controller()==this&&usable()&&lease.bridge.live();}
 void prepareBehaviorGesture(BehaviorGestureLease lease){
  if(lease==null||lease.controller!=this||lease.released||behavior!=lease.bridge||owner.controller()!=this||!usable()||!lease.bridge.live())throw new java.util.concurrent.CancellationException();
  if(behaviorGestureLease!=null||!behaviorShieldRegistered||!behaviorBarRegistered)throw new IllegalStateException("AI 입력 차단창의 수명이 겹쳤습니다");
  lease.shield=behaviorShield;lease.bar=behaviorBar;behaviorGestureLease=lease;
  // 입력 중에는 제어창과 정지·일시정지 버튼을 그대로 둡니다.
  try{wm.removeViewImmediate(lease.shield);}finally{if(!behaviorWindowPresent(lease.shield))behaviorShieldRegistered=false;}
  if(behaviorWindowPresent(lease.shield))throw new IllegalStateException("AI 입력 차단창 분리가 완료되지 않았습니다");
 }
 void restoreBehaviorGesture(BehaviorGestureLease lease){
  if(lease==null||lease.released)return;lease.released=true;
  boolean current=currentBehaviorLease(lease);if(behaviorGestureLease==lease)behaviorGestureLease=null;if(!current)return;
  try{
   if(!behaviorShieldRegistered){
    wm.addView(lease.shield,behaviorShieldParams);behaviorShieldRegistered=true;
    // 같은 제어창을 마지막에 다시 등록하여 가림막 위의 순서·위치·리스너를 보존합니다.
    try{wm.removeViewImmediate(lease.bar);}finally{if(!behaviorWindowPresent(lease.bar))behaviorBarRegistered=false;}
    if(behaviorWindowPresent(lease.bar))throw new IllegalStateException("AI 제어창 재배치 분리가 완료되지 않았습니다");
    wm.addView(lease.bar,behaviorBarParams);behaviorBarRegistered=true;
   }
  }catch(RuntimeException error){closeBehavior(lease.bridge);throw new IllegalStateException("AI 입력 차단창 복원에 실패했습니다",error);}
 }
 Rect behaviorBounds(BehaviorAndroidBridge bridge){if(behavior!=bridge||behaviorBar==null)throw new java.util.concurrent.CancellationException();return new Rect(behaviorBarParams.x,behaviorBarParams.y,behaviorBarParams.x+behaviorBar.getWidth(),behaviorBarParams.y+behaviorBar.getHeight());}
 private String caption(){return owner.getString(R.string.nex_control_status,recording?"● 녹화":"PXE",owner.status,owner.clockText());}
 public void update(){if(!usable())return;if(panel!=null){if(builtRunning!=owner.running)refresh();if(!usable())return;if(status!=null)status.setText(caption());updatePlay();KeepAliveService.update(owner,owner.status);}updateHud();rebuildMarkers();if(captureMode)setVisible(visible);}
 private void openApp(){owner.startActivity(new Intent(owner,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP));}
 private void toast(String message){Toast.makeText(owner,message==null?"설정을 확인하세요":message,Toast.LENGTH_LONG).show();}
 private AlertDialog dialog(String title,View body,String positive,Runnable action){
  AlertDialog d=new AlertDialog.Builder(ctx()).setTitle(title).setView(body).setPositiveButton(positive,null).setNegativeButton("취소",null).create();d.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);d.setOnShowListener(v->{Ui.font(d.getWindow().getDecorView());d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x->{try{action.run();d.dismiss();}catch(Exception e){toast(e.getMessage());}});});d.show();return d;
 }
 private void choices(String title,String[] values,java.util.function.IntConsumer choose){AlertDialog d=new AlertDialog.Builder(ctx()).setTitle(title).setItems(values,(a,w)->choose.accept(w)).setNegativeButton("닫기",null).create();d.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);d.setOnShowListener(v->Ui.font(d.getWindow().getDecorView()));d.show();}
 private LinearLayout form(){LinearLayout f=new LinearLayout(ctx());f.setOrientation(LinearLayout.VERTICAL);f.setPadding(dp(18),dp(8),dp(18),dp(12));return f;}
 private EditText field(LinearLayout f,String hint,String value){EditText e=new EditText(ctx());e.setHint(hint);e.setText(value);e.setTextColor(Ui.ink(owner));e.setHintTextColor(Ui.muted(owner));f.addView(e);return e;}
 private void readyEdit(){if(owner.running||recording)throw new IllegalStateException("먼저 실행·녹화를 정지하세요");}
 public void wordDialog(){
  if(recording){toast("녹화를 먼저 끝내세요");return;}
  LinearLayout f=form();EditText value=field(f,"찾을 글자",Ui.prefs(owner).getString("word","확인"));CheckBox back=new CheckBox(ctx());back.setText("글자 없으면 뒤로가기 후 최대 3회 재검색");back.setChecked(Ui.prefs(owner).getBoolean("word_back",true));f.addView(back);Spinner mode=new Spinner(ctx());mode.setAdapter(new ArrayAdapter<String>(ctx(),android.R.layout.simple_spinner_dropdown_item,new String[]{"인식하고 터치","인식 위치 미리보기","현재 동작 목록에 추가"}));f.addView(mode);
  dialog("PXE · 글자 인식",f,"확인",()->{String txt=value.getText().toString().trim();if(txt.isEmpty())throw new IllegalArgumentException("찾을 글자를 입력하세요");Ui.prefs(owner).edit().putString("word",txt).putBoolean("word_back",back.isChecked()).apply();Step s=wordStep(owner);if(mode.getSelectedItemPosition()==2){readyEdit();Macro m=Store.load(owner);s.validate(m.steps.size()+1);m.steps.add(s);Store.save(owner,m);refresh();owner.log("글자 동작 추가됨");}else{if(mode.getSelectedItemPosition()==1){s.type="TEXT_CHECK";s.failure="STOP";}Macro m=new Macro();m.name="PXE 글자 모드";m.startDelay=600;m.maxMinutes=3;m.steps.add(s);owner.start(m);}});
 }
 public static Step wordStep(Context c){Step s=new Step("TEXT_TAP");s.text=Ui.prefs(c).getString("word","확인");s.source=Ui.prefs(c).getString("word_source","AUTO");s.match=Ui.prefs(c).getString("word_match","CONTAINS");s.failure=Ui.prefs(c).getBoolean("word_back",true)?"BACK_RETRY":"STOP";s.timeout=Ui.prefs(c).getInt("word_timeout",2000);s.retries=3;s.retryDelay=1000;return s;}
 private void actionMenu(){try{readyEdit();choices("동작 추가",new String[]{"화면에서 터치 위치 선택","곡선·멀티터치 한 동작 녹화","글자 찾아 터치","뒤로가기","대기 1초","홈","최근 앱"},i->{try{if(i==0){beginPoint();return;}if(i==1){beginRecording(true,-1);return;}if(i==2){wordDialog();return;}Step s=new Step(i==3?"BACK":i==4?"WAIT":i==5?"HOME":"RECENTS");if(i==4)s.duration=1000;Macro m=Store.load(owner);m.steps.add(s);Store.save(owner,m);refresh();owner.log("동작 추가");}catch(Exception e){toast(e.getMessage());}});}catch(Exception e){toast(e.getMessage());}}
 private void sequenceDialog(){try{readyEdit();Macro m=Store.load(owner);String[] names=new String[m.steps.size()];for(int i=0;i<names.length;i++)names[i]=(i+1)+" · "+m.steps.get(i).name()+"\n"+m.steps.get(i).detail();choices("현재 동작 · 눌러 편집",names,i->choices((i+1)+"번 동작",new String[]{"여기서부터 실행","위로 이동","아래로 이동","복제","사용/사용 안 함","삭제","이 위치를 다시 녹화"},w->{try{Macro now=Store.load(owner);if(i>=now.steps.size())return;if(w==0){now.startAt=i+1;owner.start(now);return;}if(w==1&&i>0)Collections.swap(now.steps,i,i-1);if(w==2&&i<now.steps.size()-1)Collections.swap(now.steps,i,i+1);if(w==3)now.steps.add(i+1,new Step(now.steps.get(i).json()));if(w==4)now.steps.get(i).enabled=!now.steps.get(i).enabled;if(w==5)now.steps.remove(i);if(w==6){beginRecording(true,i);return;}now.startAt=1;Store.save(owner,now);refresh();}catch(Exception e){toast(e.getMessage());}}));}catch(Exception e){toast(e.getMessage());}}
 private void saveDialog(){LinearLayout f=form();Macro m=Store.load(owner);EditText name=field(f,"저장 이름",m.name);dialog("현재 매크로 저장",f,"저장",()->{String n=name.getText().toString().trim();if(n.isEmpty())throw new IllegalArgumentException("이름을 입력하세요");m.name=n;Store.put(owner,m);owner.log("저장 완료");});}
 public void beginPoint(){try{readyEdit();beginRecording(true,-2);}catch(Exception e){toast(e.getMessage());}}
 public void beginRecording(boolean single,int replaceIndex){
  if(owner.running){toast("먼저 정지하세요");return;}if(recording)return;recording=true;if(hud!=null)hud.visible(false);once=single;replace=replaceIndex;session++;lastReplayEnd=0;lastRecorded=-1;clearMarkers();collapsed=false;
  if(panel==null)KeepAliveService.show(owner);removePanel();CaptureView view=new CaptureView(ctx());capture=view;WindowManager.LayoutParams p=layout(-1,-1,true);p.flags|=WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;captureParams=p;wm.addView(capture,p);build();owner.log(replace==-2?"원하는 위치를 한 번 누르세요":"녹화 중 · 손을 떼면 원래 앱에 재생");
 }
 public void endRecording(){boolean guided=guidedRecording;guidedRecording=false;endRecordingInternal();refresh();owner.log("녹화 완료 · 현재 매크로에 저장됨");if(guided){try{SetupFlow.finishDraft(owner);openWizard();}catch(Exception e){toast("녹화할 동작이 없습니다. 다시 녹화하세요");showSetupCoach("RECORD");}}}
 private void endRecordingInternal(){session++;recording=false;if(capture!=null){try{wm.removeView(capture);}catch(Exception ignored){}capture=null;}}
 private void captured(Step s){
  long ticket=session;Macro m=Store.load(owner);if(m.steps.size()>=500){toast("동작은 최대 500개입니다");endRecording();return;}
  if(replace>=0&&replace<m.steps.size()){m.steps.set(replace,s);lastRecorded=replace;}else{m.steps.add(s);lastRecorded=m.steps.size()-1;}Store.save(owner,m);if(guidedRecording)SetupFlow.captureDraft(owner,m);owner.log("녹화 동작 "+m.steps.size()+"개");
  if(replace==-2){endRecording();return;}
  if(capture!=null){captureParams.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;wm.updateViewLayout(capture,captureParams);capture.setVisibility(View.GONE);}if(panel!=null){params.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;wm.updateViewLayout(panel,params);panel.setVisibility(View.GONE);}
  h.postDelayed(()->{if(session!=ticket||!recording)return;owner.replayRecorded(s,()->{if(session!=ticket||!recording)return;lastReplayEnd=SystemClock.uptimeMillis();if(once){endRecording();return;}if(capture!=null){captureParams.flags&=~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;wm.updateViewLayout(capture,captureParams);capture.setVisibility(View.VISIBLE);}if(panel!=null){params.flags&=~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;wm.updateViewLayout(panel,params);panel.setVisibility(View.VISIBLE);}});},180);
 }
 private final class CaptureView extends View {
  final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final Path drawing=new Path();final LinkedHashMap<Integer,Track> active=new LinkedHashMap<>();final ArrayList<Track> ended=new ArrayList<>();long begin=0;boolean failed=false;
  CaptureView(Context c){super(c);}
  final class Track {JSONArray points=new JSONArray();int start;int duration;void add(float x,float y){int[] s=owner.size();if(points.length()>=512){JSONArray reduced=new JSONArray();for(int i=0;i<points.length();i+=2)reduced.put(points.optJSONArray(i));reduced.put(points.optJSONArray(points.length()-1));points=reduced;}points.put(GesturePaths.point(Math.max(0,Math.min(100,x*100.0/s[0])),Math.max(0,Math.min(100,y*100.0/s[1]))));}}
  @Override public boolean performClick(){return super.performClick();}
  @Override public boolean onTouchEvent(MotionEvent e){
   int action=e.getActionMasked(),index=e.getActionIndex();long now=e.getEventTime();
   if(action==MotionEvent.ACTION_DOWN){if(lastReplayEnd>0&&lastRecorded>=0&&!once){Macro m=Store.load(owner);if(lastRecorded<m.steps.size()){m.steps.get(lastRecorded).after=(int)Math.max(0,Math.min(600000,now-lastReplayEnd));Store.save(owner,m);}}active.clear();ended.clear();failed=false;begin=now;}
   if(now-begin>=59000){failed=true;toast("한 제스처는 최대 59초입니다");}
   float ox=e.getRawX()-e.getX(),oy=e.getRawY()-e.getY();
   if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){Track t=new Track();t.start=(int)(now-begin);t.add(e.getX(index)+ox,e.getY(index)+oy);active.put(e.getPointerId(index),t);}
   for(int i=0;i<e.getPointerCount();i++){Track t=active.get(e.getPointerId(i));if(t!=null){for(int j=0;j<e.getHistorySize();j++)t.add(e.getHistoricalX(i,j)+ox,e.getHistoricalY(i,j)+oy);t.add(e.getX(i)+ox,e.getY(i)+oy);}}
   if(action==MotionEvent.ACTION_POINTER_UP||action==MotionEvent.ACTION_UP){Track t=active.remove(e.getPointerId(index));if(t!=null){t.duration=Math.max(1,(int)(now-begin)-t.start);ended.add(t);}}
   if(action==MotionEvent.ACTION_CANCEL){active.clear();ended.clear();failed=true;}
   if(action==MotionEvent.ACTION_UP){performClick();if(!failed){try{Step s;if(replace==-2){s=new Step("TAP");JSONArray p=ended.get(0).points.getJSONArray(0);s.x=p.getDouble(0);s.y=p.getDouble(1);}else{s=new Step("GESTURE");for(Track t:ended)s.strokes.put(GesturePaths.stroke(t.points,t.start,t.duration));GesturePaths.validate(s.strokes);}s.after=350;captured(s);}catch(Exception ex){toast(ex.getMessage());}}active.clear();ended.clear();}
   invalidate();return true;
  }
  private void drawTrack(Canvas c,Track t){drawing.reset();float w=getWidth(),height=getHeight();for(int i=0;i<t.points.length();i++){JSONArray a=t.points.optJSONArray(i);float x=(float)a.optDouble(0)*w/100,y=(float)a.optDouble(1)*height/100;if(i==0)drawing.moveTo(x,y);else drawing.lineTo(x,y);}c.drawPath(drawing,paint);}
  @Override protected void onDraw(Canvas c){super.onDraw(c);paint.setColor(Ui.accent(owner));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(3));for(Track t:ended)drawTrack(c,t);for(Track t:active.values())drawTrack(c,t);}
 }
 public void rebuildMarkers(){
  clearMarkers();if(panel==null||recording||collapsed||owner.watching||!Ui.prefs(owner).getBoolean("markers",true))return;
  Macro m=Store.load(owner);int[] sz=owner.size();
  for(int i=0;i<Math.min(40,m.steps.size());i++){final int index=i;Step s=m.steps.get(i);if(!s.enabled||!Arrays.asList("TAP","DOUBLE","LONG","SWIPE","CURVE","PINCH_IN","PINCH_OUT","GESTURE").contains(s.type))continue;
   if(s.type.equals("GESTURE")&&s.strokes.length()>0){JSONArray p0=s.strokes.optJSONObject(0).optJSONArray("points").optJSONArray(0);s.x=p0.optDouble(0);s.y=p0.optDouble(1);}
   Button marker=Ui.button(ctx(),String.valueOf(i+1));marker.setTextSize(12);marker.setTextColor(Ui.dark(owner)?Ui.bg(owner):Color.WHITE);marker.setContentDescription((i+1)+"번 터치 위치 · 끌어서 이동");marker.setBackground(Ui.rounded((Ui.accent(owner)&0x00FFFFFF)|0xDF000000,dp(24)));WindowManager.LayoutParams p=layout(dp(48),dp(48),!owner.running);p.x=(int)(s.x*sz[0]/100)-dp(24);p.y=(int)(s.y*sz[1]/100)-dp(24);
   marker.setOnClickListener(v->sequenceDialog());dragMarker(marker,p,index);wm.addView(marker,p);if(!visible)marker.setVisibility(View.GONE);markers.add(marker);
  }
 }
 @android.annotation.SuppressLint("ClickableViewAccessibility") private void dragMarker(View v,WindowManager.LayoutParams p,int index){v.setOnTouchListener(new View.OnTouchListener(){float sx,sy;int ox,oy;boolean moved;public boolean onTouch(View view,MotionEvent e){if(owner.running)return false;if(e.getActionMasked()==MotionEvent.ACTION_DOWN){sx=e.getRawX();sy=e.getRawY();ox=p.x;oy=p.y;moved=false;return true;}if(e.getActionMasked()==MotionEvent.ACTION_MOVE){moved|=Math.abs(e.getRawX()-sx)+Math.abs(e.getRawY()-sy)>dp(4);int[] z=owner.size();p.x=Math.max(-dp(24),Math.min(z[0]-dp(24),ox+(int)(e.getRawX()-sx)));p.y=Math.max(-dp(24),Math.min(z[1]-dp(24),oy+(int)(e.getRawY()-sy)));wm.updateViewLayout(view,p);return true;}if(e.getActionMasked()==MotionEvent.ACTION_UP){if(moved){Macro m=Store.load(owner);if(index<m.steps.size()){Step s=m.steps.get(index);int[] z=owner.size();double nx=(p.x+dp(24))*100.0/z[0],ny=(p.y+dp(24))*100.0/z[1];if(s.type.equals("GESTURE")){JSONArray first=s.strokes.optJSONObject(0).optJSONArray("points").optJSONArray(0);double dx=nx-first.optDouble(0),dy=ny-first.optDouble(1);for(int j=0;j<s.strokes.length();j++){JSONArray points=s.strokes.optJSONObject(j).optJSONArray("points");for(int k=0;k<points.length();k++){JSONArray point=points.optJSONArray(k);try{point.put(0,Math.max(0,Math.min(100,point.optDouble(0)+dx)));point.put(1,Math.max(0,Math.min(100,point.optDouble(1)+dy)));}catch(Exception ignored){}}}}s.x=nx;s.y=ny;Store.save(owner,m);}}else view.performClick();return true;}return true;}});}
 private void clearMarkers(){for(View v:markers)try{wm.removeView(v);}catch(Exception ignored){}markers.clear();}
 public void showHit(Rect rect,String label){
  if(highlight!=null)try{wm.removeView(highlight);}catch(Exception ignored){}
  final Rect target=new Rect(rect);final RectF targetF=new RectF(target);View v=new View(ctx()){Paint p=new Paint(3);final int[] origin=new int[2];@Override protected void onDraw(Canvas c){getLocationOnScreen(origin);c.save();c.translate(-origin[0],-origin[1]);p.setColor(Ui.accent(owner));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));c.drawRoundRect(targetF,dp(7),dp(7),p);p.setStyle(Paint.Style.FILL);p.setTypeface(getResources().getFont(R.font.nanum_gothic_bold));p.setTextSize(dp(13));c.drawText(label,Math.max(dp(4),target.left),Math.max(dp(20),target.top-dp(5)),p);c.restore();}};
  highlight=v;wm.addView(v,layout(-1,-1,false));h.postDelayed(()->{if(highlight==v){try{wm.removeView(v);}catch(Exception ignored){}highlight=null;}},2500);
 }
}
