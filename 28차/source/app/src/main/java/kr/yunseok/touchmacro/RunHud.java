package kr.yunseok.touchmacro;
import android.content.*;
import android.graphics.PixelFormat;
import android.view.*;
import android.widget.*;

/** 반복 수만 보이는 이동식 위젯이며, 화살표를 눌렀을 때만 상세를 펼칩니다. */
public final class RunHud {
    private final MacroService owner;
    private final WindowManager wm;
    private LinearLayout view,details,shortcuts;
    private TextView counter,progress,times;
    private ImageButton expand;
    private Button target;
    private WindowManager.LayoutParams params;
    private boolean expanded;
    private boolean darkTheme;
    private String shortcutState="",watchState="",wordState="",activeState="";
    private int screenWidth,screenHeight;
    public RunHud(MacroService owner){this.owner=owner;wm=(WindowManager)owner.getSystemService(Context.WINDOW_SERVICE);}
    private int dp(float value){return Ui.dp(owner,value);}
    private ImageButton icon(Context c,int drawable,String description){
        ImageButton b=new ImageButton(c);b.setImageResource(drawable);b.setColorFilter(Ui.ink(c));b.setPadding(dp(12),dp(12),dp(12),dp(12));b.setBackground(Ui.ripple(c,android.graphics.Color.TRANSPARENT,10));b.setContentDescription(description);return b;
    }
    private void build(){
        Context c=Ui.themed(owner);darkTheme=Ui.dark(owner);int[] size=owner.size();screenWidth=size[0];screenHeight=size[1];expanded=Ui.prefs(owner).getBoolean("hud_expanded",false);
        view=new LinearLayout(c);view.setOrientation(LinearLayout.VERTICAL);view.setBackground(Ui.outline(c,Ui.card(c),12));view.setElevation(dp(3));
        LinearLayout header=new LinearLayout(c);header.setGravity(Gravity.CENTER_VERTICAL);view.addView(header,new LinearLayout.LayoutParams(-1,dp(44)));
        counter=new TextView(c);counter.setTextSize(13);counter.setAutoSizeTextTypeUniformWithConfiguration(10,13,1,android.util.TypedValue.COMPLEX_UNIT_SP);counter.setTextColor(Ui.ink(c));counter.setGravity(Gravity.CENTER);counter.setSingleLine(true);counter.setPadding(dp(8),0,0,0);header.addView(counter,new LinearLayout.LayoutParams(0,-1,1));drag(counter);
        expand=icon(c,R.drawable.pxe_chevron,"진행 상세 펼치기");expand.setPadding(dp(10),dp(10),dp(10),dp(10));header.addView(expand,new LinearLayout.LayoutParams(dp(40),-1));expand.setOnClickListener(v->setExpanded(!expanded));
        details=new LinearLayout(c);details.setOrientation(LinearLayout.VERTICAL);details.setPadding(dp(12),0,dp(12),dp(12));view.addView(details);
        LinearLayout state=new LinearLayout(c);state.setGravity(Gravity.CENTER_VERTICAL);details.addView(state);
        ImageView symbol=new ImageView(c);symbol.setImageResource(R.drawable.pxe_sun);symbol.setColorFilter(0xffd6a73e);symbol.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);state.addView(symbol,new LinearLayout.LayoutParams(dp(20),dp(20)));
        TextView title=new TextView(c);title.setText("현재 실행");title.setTextSize(13);title.setTextColor(Ui.ink(c));title.setPadding(dp(8),0,0,0);state.addView(title);
        progress=new TextView(c);progress.setTextSize(13);progress.setTextColor(Ui.ink(c));progress.setMaxLines(5);progress.setPadding(0,dp(8),0,dp(8));details.addView(progress);
        times=new TextView(c);times.setTextSize(11);times.setTextColor(Ui.muted(c));times.setMaxLines(3);details.addView(times);
        target=Ui.button(c,"");target.setContentDescription("인식할 글자 바로 변경");target.setTextSize(13);target.setMaxLines(2);details.addView(target,new LinearLayout.LayoutParams(-1,-2));target.setOnClickListener(v->TargetWords.showEditor(owner,this::refreshTargets));
        shortcuts=new LinearLayout(c);details.addView(shortcuts,new LinearLayout.LayoutParams(-1,-2));refreshTargets();
        Ui.font(view);params=new WindowManager.LayoutParams(width(),WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.START;params.x=Ui.prefs(owner).getInt("hud_x",dp(12));params.y=Ui.prefs(owner).getInt("hud_y",owner.size()[1]-dp(120));applyExpanded();clamp();
        try{wm.addView(view,params);}catch(WindowManager.BadTokenException error){view=null;owner.overlayTokenInvalid(error);return;}
        view.post(()->{if(view!=null){clamp();wm.updateViewLayout(view,params);}});
    }
    private int width(){return Math.min(dp(expanded?280:104),owner.size()[0]-dp(16));}
    private void clamp(){int[] size=owner.size();params.x=Math.max(0,Math.min(params.x,Math.max(0,size[0]-params.width)));int height=view==null?dp(44):Math.max(dp(44),view.getMeasuredHeight());params.y=Math.max(dp(24),Math.min(params.y,Math.max(dp(24),size[1]-height-dp(24))));}
    private void applyExpanded(){details.setVisibility(expanded?View.VISIBLE:View.GONE);expand.setRotation(expanded?270:90);expand.setContentDescription(expanded?"진행 상세 접기":"진행 상세 펼치기");}
    private void setExpanded(boolean value){if(view==null)return;expanded=value;Ui.prefs(owner).edit().putBoolean("hud_expanded",value).apply();applyExpanded();params.width=width();clamp();wm.updateViewLayout(view,params);view.post(()->{if(view!=null){clamp();wm.updateViewLayout(view,params);savePosition();}});}
    private void savePosition(){Ui.prefs(owner).edit().putInt("hud_x",params.x).putInt("hud_y",params.y).apply();}
    // 실제 창 좌표로 충돌을 판단해 자동 터치가 위젯 버튼에 전달되지 않게 합니다.
    android.graphics.RectF touchBounds(){if(view==null||view.getVisibility()!=View.VISIBLE)return null;int[] location=new int[2];view.getLocationOnScreen(location);return new android.graphics.RectF(location[0],location[1],location[0]+view.getWidth(),location[1]+view.getHeight());}
    private void fitScreen(){int[] size=owner.size();if(size[0]==screenWidth&&size[1]==screenHeight)return;screenWidth=size[0];screenHeight=size[1];params.width=width();clamp();wm.updateViewLayout(view,params);view.post(()->{if(view!=null){clamp();wm.updateViewLayout(view,params);savePosition();}});}
    private void drag(View handle){handle.setOnTouchListener(new View.OnTouchListener(){float x,y;int left,top;boolean moved;
        public boolean onTouch(View target,MotionEvent event){switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:x=event.getRawX();y=event.getRawY();left=params.x;top=params.y;moved=false;return true;
            case MotionEvent.ACTION_MOVE:float dx=event.getRawX()-x,dy=event.getRawY()-y;if(Math.hypot(dx,dy)>ViewConfiguration.get(owner).getScaledTouchSlop())moved=true;if(moved){params.x=left+Math.round(dx);params.y=top+Math.round(dy);clamp();wm.updateViewLayout(view,params);}return true;
            case MotionEvent.ACTION_UP:if(moved)savePosition();else target.performClick();return true;
            case MotionEvent.ACTION_CANCEL:return true;
            default:return false;
        }}
    });}
    private void refreshTargets(){if(shortcuts==null)return;android.content.SharedPreferences p=Ui.prefs(owner);shortcutState=p.getString("target_words","[]");watchState=p.getString("watch_config","");wordState=p.getString("word","");activeState=owner.activeTargetWord();Ui.textIfChanged(target,"글자 변경 · "+TargetWords.current(owner));TargetWords.fillShortcuts(shortcuts.getContext(),shortcuts,this::refreshTargets);if(view!=null&&params!=null)view.post(()->{if(view!=null){clamp();wm.updateViewLayout(view,params);savePosition();}});}
    private void refreshState(){if(view==null)return;Ui.textIfChanged(counter,owner.compactCounter());String description="매크로 반복 "+owner.compactCounter()+" · 끌어서 이동";if(!android.text.TextUtils.equals(counter.getContentDescription(),description))counter.setContentDescription(description);counter.setTextColor(owner.paused?Ui.muted(owner):Ui.ink(owner));android.content.SharedPreferences p=Ui.prefs(owner);if(!shortcutState.equals(p.getString("target_words","[]"))||!watchState.equals(p.getString("watch_config",""))||!wordState.equals(p.getString("word",""))||!activeState.equals(owner.activeTargetWord()))refreshTargets();}
    public void update(String line,String timing,boolean visible){
        if(view!=null&&darkTheme!=Ui.dark(owner))close();if(view==null)build();if(view==null)return;fitScreen();refreshState();Ui.textIfChanged(progress,line);Ui.textIfChanged(times,timing);float alpha=Ui.prefs(owner).getInt("hud_opacity",95)/100f;if(view.getAlpha()!=alpha)view.setAlpha(alpha);visible(visible);
    }
    public void visible(boolean visible){if(view!=null){int target=visible?View.VISIBLE:View.GONE;if(view.getVisibility()!=target)view.setVisibility(target);}}
    public void close(){if(view!=null){wm.removeView(view);view=null;}}
}
