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
 private View panel,capture,highlight;private TextView status;private Button play;
 private WindowManager.LayoutParams params,captureParams;private final ArrayList<View> markers=new ArrayList<>();
 public boolean recording=false;private boolean once=false;private int replace=-1;private long session=0;private boolean collapsed=false, visible=true;private long lastReplayEnd=0;private int lastRecorded=-1;
 public FloatingController(MacroService s){owner=s;wm=(WindowManager)s.getSystemService(Context.WINDOW_SERVICE);}
 private Context ctx(){return Ui.themed(owner);}private int dp(float v){return Ui.dp(owner,v);}private int scaled(float v){return dp(v*Ui.prefs(owner).getInt("scale",100)/100f);}
 private WindowManager.LayoutParams layout(int w,int height,boolean touch){
  WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,height,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|(touch?0:WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.START;return p;
 }
 public void show(){if(panel!=null)return;KeepAliveService.show(owner);build();h.post(ticker);}
 public void refresh(){if(panel==null)return;removePanel();build();}
 private final Runnable ticker=new Runnable(){@Override public void run(){if(panel==null)return;if(status!=null)status.setText(caption());if(play!=null)play.setText(recording?"녹화 끝내기":!owner.running?"▶ 시작":owner.paused?"▶ 계속":"Ⅱ 일시정지");h.postDelayed(this,1000);}};
 private void build(){
  Context c=ctx();LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(scaled(7),scaled(6),scaled(7),scaled(6));box.setBackground(Ui.rounded(Ui.card(c),dp(17)));box.setElevation(dp(8));box.setAlpha(Ui.prefs(owner).getInt("opacity",95)/100f);
  if(collapsed&&!recording){Button bubble=Ui.button(c,owner.running?(owner.paused?"Ⅱ":"▶"):"NEX");box.addView(bubble,new LinearLayout.LayoutParams(scaled(48),scaled(48)));bubble.setOnClickListener(v->{collapsed=false;refresh();});bubble.setOnLongClickListener(v->{owner.stop();return true;});drag(bubble);}
  else{
   TouchLabel title=new TouchLabel(c);title.setText(recording?"NEX · 손가락 녹화":"NEX · "+owner.status);title.setTextColor(Ui.ink(c));title.setTextSize(11);box.addView(title,new LinearLayout.LayoutParams(scaled(228),scaled(39)));status=title;drag(title);
   LinearLayout r=row(box);play=button(r,recording?"녹화 끝내기":"▶ 시작",()->{if(recording){endRecording();return;}if(owner.running)owner.togglePause();else try{owner.start(Store.load(owner));}catch(Exception e){toast(e.getMessage());}});button(r,"■ 정지",()->{owner.stop();if(recording)endRecording();});button(r,recording?"취소":"— 접기",()->{if(recording)endRecording();else{collapsed=true;refresh();}});
   if(recording){TextView hint=new TextView(c);hint.setText("탭·곡선·멀티터치 → 손을 떼면 원래 앱에 재생");hint.setTextColor(Ui.muted(c));hint.setTextSize(10);box.addView(hint);}
   else{
    r=row(box);button(r,"글자",this::wordDialog);button(r,"＋ 동작",this::actionMenu);button(r,"● 녹화",()->beginRecording(false,-1));
    r=row(box);button(r,"동작 목록",this::sequenceDialog);button(r,"저장",this::saveDialog);button(r,"편집",()->{owner.stop();openApp();});
    r=row(box);button(r,"점 표시",()->{boolean on=!Ui.prefs(owner).getBoolean("markers",true);Ui.prefs(owner).edit().putBoolean("markers",on).apply();rebuildMarkers();});button(r,"설정",()->{Intent i=new Intent(owner,MainActivity.class).putExtra("settings",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);owner.startActivity(i);});button(r,"닫기",()->{owner.stop();close();});
   }
  }
  params=layout(collapsed&&!recording?scaled(62):scaled(244),WindowManager.LayoutParams.WRAP_CONTENT,true);int[] s=owner.size();params.x=Math.max(0,Math.min(s[0]-params.width,Ui.prefs(owner).getInt("panel_x",dp(12))));params.y=Math.max(dp(30),Math.min(s[1]-(collapsed?scaled(62):scaled(225)),Ui.prefs(owner).getInt("panel_y",dp(95))));
  panel=box;Ui.font(box);wm.addView(panel,params);if(!visible)panel.setVisibility(View.GONE);rebuildMarkers();
 }
 private LinearLayout row(LinearLayout b){LinearLayout r=new LinearLayout(ctx());b.addView(r);return r;}
 private Button button(LinearLayout r,String text,Runnable action){Button b=Ui.button(ctx(),text);b.setTextSize(10);b.setPadding(0,0,0,0);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,scaled(40),1);p.setMargins(scaled(2),scaled(2),scaled(2),scaled(2));r.addView(b,p);b.setOnClickListener(v->action.run());return b;}
 @android.annotation.SuppressLint("ClickableViewAccessibility") private void drag(View v){v.setOnTouchListener(new View.OnTouchListener(){float sx,sy;int ox,oy;boolean moved;public boolean onTouch(View view,MotionEvent e){if(e.getActionMasked()==MotionEvent.ACTION_DOWN){sx=e.getRawX();sy=e.getRawY();ox=params.x;oy=params.y;moved=false;return false;}if(e.getActionMasked()==MotionEvent.ACTION_MOVE){moved|=Math.abs(e.getRawX()-sx)+Math.abs(e.getRawY()-sy)>dp(5);if(moved){int[] s=owner.size();params.x=Math.max(0,Math.min(s[0]-params.width,ox+(int)(e.getRawX()-sx)));params.y=Math.max(dp(24),Math.min(s[1]-scaled(240),oy+(int)(e.getRawY()-sy)));if(panel!=null)wm.updateViewLayout(panel,params);return true;}}if(e.getActionMasked()==MotionEvent.ACTION_UP){Ui.prefs(owner).edit().putInt("panel_x",params.x).putInt("panel_y",params.y).apply();if(moved)return true;view.performClick();return true;}return false;}});}
 public boolean prepareGesture(JSONArray strokes,int width,int height){
  if(panel==null||params==null)return false;RectF box=new RectF(params.x,params.y,params.x+panel.getWidth(),params.y+panel.getHeight());
  try{for(int i=0;i<strokes.length();i++){JSONArray points=strokes.getJSONObject(i).getJSONArray("points");float minX=width,minY=height,maxX=0,maxY=0;for(int j=0;j<points.length();j++){JSONArray p=points.getJSONArray(j);float x=(float)p.getDouble(0)*width/100,y=(float)p.getDouble(1)*height/100;minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);}RectF bounds=new RectF(minX-dp(5),minY-dp(5),maxX+dp(5),maxY+dp(5));if(RectF.intersects(box,bounds)){setVisible(false);return true;}}}catch(Exception ignored){setVisible(false);return true;}return false;
 }
 public void setVisible(boolean on){visible=on;if(panel!=null)panel.setVisibility(on?View.VISIBLE:View.GONE);for(View m:markers)m.setVisibility(on?View.VISIBLE:View.GONE);if(highlight!=null)highlight.setVisibility(on?View.VISIBLE:View.GONE);}
 public boolean shown(){return panel!=null;}
 private void removePanel(){if(panel!=null){try{wm.removeView(panel);}catch(Exception ignored){}panel=null;status=null;play=null;}clearMarkers();}
 public void close(){endRecordingInternal();removePanel();if(highlight!=null){try{wm.removeView(highlight);}catch(Exception ignored){}highlight=null;}h.removeCallbacksAndMessages(null);KeepAliveService.close(owner);}
 private String caption(){return owner.getString(R.string.nex_control_status,recording?"● 녹화":"NEX",owner.status,owner.clockText());}
 public void update(){if(panel!=null){if(status!=null)status.setText(caption());if(play!=null)play.setText(recording?"녹화 끝내기":!owner.running?"▶ 시작":owner.paused?"▶ 계속":"Ⅱ 일시정지");KeepAliveService.update(owner,owner.status);}rebuildMarkers();}
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
  dialog("NEX · 글자 인식",f,"확인",()->{String txt=value.getText().toString().trim();if(txt.isEmpty())throw new IllegalArgumentException("찾을 글자를 입력하세요");Ui.prefs(owner).edit().putString("word",txt).putBoolean("word_back",back.isChecked()).apply();Step s=wordStep(owner);if(mode.getSelectedItemPosition()==2){readyEdit();Macro m=Store.load(owner);s.validate(m.steps.size()+1);m.steps.add(s);Store.save(owner,m);refresh();owner.log("글자 동작 추가됨");}else{if(owner.running)throw new IllegalArgumentException("먼저 정지하세요");if(mode.getSelectedItemPosition()==1){s.type="TEXT_CHECK";s.failure="STOP";}Macro m=new Macro();m.name="NEX 글자 모드";m.startDelay=600;m.maxMinutes=3;m.steps.add(s);owner.start(m);}});
 }
 public static Step wordStep(Context c){Step s=new Step("TEXT_TAP");s.text=Ui.prefs(c).getString("word","확인");s.source=Ui.prefs(c).getString("word_source","AUTO");s.match=Ui.prefs(c).getString("word_match","CONTAINS");s.failure=Ui.prefs(c).getBoolean("word_back",true)?"BACK_RETRY":"STOP";s.timeout=Ui.prefs(c).getInt("word_timeout",2000);s.retries=3;s.retryDelay=1000;return s;}
 private void actionMenu(){try{readyEdit();choices("동작 추가",new String[]{"화면에서 터치 위치 선택","곡선·멀티터치 한 동작 녹화","글자 찾아 터치","뒤로가기","대기 1초","홈","최근 앱"},i->{try{if(i==0){beginPoint();return;}if(i==1){beginRecording(true,-1);return;}if(i==2){wordDialog();return;}Step s=new Step(i==3?"BACK":i==4?"WAIT":i==5?"HOME":"RECENTS");if(i==4)s.duration=1000;Macro m=Store.load(owner);m.steps.add(s);Store.save(owner,m);refresh();owner.log("동작 추가");}catch(Exception e){toast(e.getMessage());}});}catch(Exception e){toast(e.getMessage());}}
 private void sequenceDialog(){try{readyEdit();Macro m=Store.load(owner);String[] names=new String[m.steps.size()];for(int i=0;i<names.length;i++)names[i]=(i+1)+" · "+m.steps.get(i).name()+"\n"+m.steps.get(i).detail();choices("현재 동작 · 눌러 편집",names,i->choices((i+1)+"번 동작",new String[]{"여기서부터 실행","위로 이동","아래로 이동","복제","사용/사용 안 함","삭제","이 위치를 다시 녹화"},w->{try{Macro now=Store.load(owner);if(i>=now.steps.size())return;if(w==0){now.startAt=i+1;owner.start(now);return;}if(w==1&&i>0)Collections.swap(now.steps,i,i-1);if(w==2&&i<now.steps.size()-1)Collections.swap(now.steps,i,i+1);if(w==3)now.steps.add(i+1,new Step(now.steps.get(i).json()));if(w==4)now.steps.get(i).enabled=!now.steps.get(i).enabled;if(w==5)now.steps.remove(i);if(w==6){beginRecording(true,i);return;}now.startAt=1;Store.save(owner,now);refresh();}catch(Exception e){toast(e.getMessage());}}));}catch(Exception e){toast(e.getMessage());}}
 private void saveDialog(){LinearLayout f=form();Macro m=Store.load(owner);EditText name=field(f,"저장 이름",m.name);dialog("현재 매크로 저장",f,"저장",()->{String n=name.getText().toString().trim();if(n.isEmpty())throw new IllegalArgumentException("이름을 입력하세요");m.name=n;Store.put(owner,m);owner.log("저장 완료");});}
 public void beginPoint(){try{readyEdit();beginRecording(true,-2);}catch(Exception e){toast(e.getMessage());}}
 public void beginRecording(boolean single,int replaceIndex){
  if(owner.running){toast("먼저 정지하세요");return;}if(recording)return;recording=true;once=single;replace=replaceIndex;session++;lastReplayEnd=0;lastRecorded=-1;clearMarkers();collapsed=false;
  if(panel==null)KeepAliveService.show(owner);removePanel();CaptureView view=new CaptureView(ctx());capture=view;WindowManager.LayoutParams p=layout(-1,-1,true);p.flags|=WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;captureParams=p;wm.addView(capture,p);build();owner.log(replace==-2?"원하는 위치를 한 번 누르세요":"녹화 중 · 손을 떼면 원래 앱에 재생");
 }
 public void endRecording(){endRecordingInternal();refresh();owner.log("녹화 완료 · 현재 매크로에 저장됨");}
 private void endRecordingInternal(){session++;recording=false;if(capture!=null){try{wm.removeView(capture);}catch(Exception ignored){}capture=null;}}
 private void captured(Step s){
  long ticket=session;Macro m=Store.load(owner);if(m.steps.size()>=500){toast("동작은 최대 500개입니다");endRecording();return;}
  if(replace>=0&&replace<m.steps.size()){m.steps.set(replace,s);lastRecorded=replace;}else{m.steps.add(s);lastRecorded=m.steps.size()-1;}Store.save(owner,m);owner.log("녹화 동작 "+m.steps.size()+"개");
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
  clearMarkers();if(panel==null||recording||!Ui.prefs(owner).getBoolean("markers",true))return;
  Macro m=Store.load(owner);int[] sz=owner.size();
  for(int i=0;i<Math.min(40,m.steps.size());i++){final int index=i;Step s=m.steps.get(i);if(!s.enabled||!Arrays.asList("TAP","DOUBLE","LONG","SWIPE","CURVE","PINCH_IN","PINCH_OUT","GESTURE").contains(s.type))continue;
   if(s.type.equals("GESTURE")&&s.strokes.length()>0){JSONArray p0=s.strokes.optJSONObject(0).optJSONArray("points").optJSONArray(0);s.x=p0.optDouble(0);s.y=p0.optDouble(1);}
   Button marker=Ui.button(ctx(),String.valueOf(i+1));marker.setTextSize(12);marker.setTextColor(Color.WHITE);marker.setBackground(Ui.rounded(0xDF2856D8,dp(24)));WindowManager.LayoutParams p=layout(dp(42),dp(42),!owner.running);p.x=(int)(s.x*sz[0]/100)-dp(21);p.y=(int)(s.y*sz[1]/100)-dp(21);
   marker.setOnClickListener(v->sequenceDialog());dragMarker(marker,p,index);wm.addView(marker,p);if(!visible)marker.setVisibility(View.GONE);markers.add(marker);
  }
 }
 @android.annotation.SuppressLint("ClickableViewAccessibility") private void dragMarker(View v,WindowManager.LayoutParams p,int index){v.setOnTouchListener(new View.OnTouchListener(){float sx,sy;int ox,oy;boolean moved;public boolean onTouch(View view,MotionEvent e){if(owner.running)return false;if(e.getActionMasked()==MotionEvent.ACTION_DOWN){sx=e.getRawX();sy=e.getRawY();ox=p.x;oy=p.y;moved=false;return true;}if(e.getActionMasked()==MotionEvent.ACTION_MOVE){moved|=Math.abs(e.getRawX()-sx)+Math.abs(e.getRawY()-sy)>dp(4);int[] z=owner.size();p.x=Math.max(-dp(21),Math.min(z[0]-dp(21),ox+(int)(e.getRawX()-sx)));p.y=Math.max(-dp(21),Math.min(z[1]-dp(21),oy+(int)(e.getRawY()-sy)));wm.updateViewLayout(view,p);return true;}if(e.getActionMasked()==MotionEvent.ACTION_UP){if(moved){Macro m=Store.load(owner);if(index<m.steps.size()){Step s=m.steps.get(index);int[] z=owner.size();double nx=(p.x+dp(21))*100.0/z[0],ny=(p.y+dp(21))*100.0/z[1];if(s.type.equals("GESTURE")){JSONArray first=s.strokes.optJSONObject(0).optJSONArray("points").optJSONArray(0);double dx=nx-first.optDouble(0),dy=ny-first.optDouble(1);for(int j=0;j<s.strokes.length();j++){JSONArray points=s.strokes.optJSONObject(j).optJSONArray("points");for(int k=0;k<points.length();k++){JSONArray point=points.optJSONArray(k);try{point.put(0,Math.max(0,Math.min(100,point.optDouble(0)+dx)));point.put(1,Math.max(0,Math.min(100,point.optDouble(1)+dy)));}catch(Exception ignored){}}}}s.x=nx;s.y=ny;Store.save(owner,m);}}else view.performClick();return true;}return true;}});}
 private void clearMarkers(){for(View v:markers)try{wm.removeView(v);}catch(Exception ignored){}markers.clear();}
 public void showHit(Rect rect,String label){
  if(highlight!=null)try{wm.removeView(highlight);}catch(Exception ignored){}
  final Rect target=new Rect(rect);final RectF targetF=new RectF(target);View v=new View(ctx()){Paint p=new Paint(3);@Override protected void onDraw(Canvas c){p.setColor(0xFF1CB79C);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));c.drawRoundRect(targetF,dp(7),dp(7),p);p.setStyle(Paint.Style.FILL);p.setTypeface(getResources().getFont(R.font.nanum_gothic_bold));p.setTextSize(dp(13));c.drawText(label,Math.max(dp(4),target.left),Math.max(dp(20),target.top-dp(5)),p);}};
  highlight=v;wm.addView(v,layout(-1,-1,false));h.postDelayed(()->{if(highlight==v){try{wm.removeView(v);}catch(Exception ignored){}highlight=null;}},2500);
 }
}
