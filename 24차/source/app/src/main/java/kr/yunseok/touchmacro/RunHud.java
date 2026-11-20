package kr.yunseok.touchmacro;
import android.content.*;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

/** 본문은 입력을 통과시키며 별도 손잡이만 이동·접기 입력을 받습니다. */
public final class RunHud {
    private final MacroService owner;private final WindowManager wm;
    private LinearLayout view;private Button handle;private TextView progress,phase,times;
    private WindowManager.LayoutParams params,handleParams;private boolean collapsed,positioned,handleRegistered;
    private final Rect avoid=new Rect();private int measuredHeight;
    private String count="",stage="",timing="",state="";
    public RunHud(MacroService owner){this.owner=owner;wm=(WindowManager)owner.getSystemService(Context.WINDOW_SERVICE);collapsed=Ui.prefs(owner).getBoolean("hud_collapsed",false);}
    private TextView line(Context c,int size,int color){TextView value=new TextView(c);value.setTextSize(size);value.setTextColor(color);value.setSingleLine(true);value.setEllipsize(TextUtils.TruncateAt.END);return value;}
    void avoid(Rect bounds){avoid.set(bounds);}
    public void update(RunProgress run,String state,String stage,String timing,boolean visible){
        count=run.counter();this.state=state;this.stage=stage;this.timing=timing;
        if(view==null){Context c=Ui.themed(owner);view=new LinearLayout(c);view.setOrientation(LinearLayout.VERTICAL);view.setPadding(Ui.dp(c,10),Ui.dp(c,7),Ui.dp(c,10),Ui.dp(c,7));view.setGravity(Gravity.CENTER_VERTICAL);view.setMinimumHeight(Ui.dp(c,48));view.setBackground(Ui.outline(c,Ui.card(c),12));
            progress=line(c,12,Ui.accent(c));view.addView(progress);phase=line(c,12,Ui.ink(c));view.addView(phase);
            times=line(c,10,Ui.muted(c));times.setSingleLine(false);times.setMaxLines(2);view.addView(times);Ui.font(view);
            handle=Ui.button(c,"");handle.setPadding(0,0,0,0);handle.setTextSize(18);handle.setTextColor(Ui.accent(c));handle.setBackground(Ui.outline(c,Ui.card(c),12));
            handle.setOnClickListener(v->{collapsed=!collapsed;Ui.prefs(owner).edit().putBoolean("hud_collapsed",collapsed).apply();render();place(true);});drag();
            params=layout(width(),WindowManager.LayoutParams.WRAP_CONTENT,false);handleParams=layout(Ui.dp(c,48),Ui.dp(c,48),true);render();place(false);
            try{wm.addView(view,params);wm.addView(handle,handleParams);handleRegistered=true;}catch(WindowManager.BadTokenException error){close();owner.overlayTokenInvalid(error);return;}
        }else{render();place(true);}visible(visible);
    }
    private WindowManager.LayoutParams layout(int width,int height,boolean touch){WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|(touch?0:WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.START;return p;}
    private int width(){return Math.min(Ui.dp(owner,collapsed?200:280),Math.max(Ui.dp(owner,48),owner.size()[0]-Ui.dp(owner,84)));}
    private void render(){String label=count.isEmpty()?state:(!state.equals("실행 중")?state+" · ":"")+count;progress.setText(owner.getString(R.string.nex_hud_count,label,""));phase.setText(stage);times.setText(timing);phase.setVisibility(collapsed?View.GONE:View.VISIBLE);times.setVisibility(collapsed?View.GONE:View.VISIBLE);view.setAlpha(Math.max(0,Math.min(100,Ui.prefs(owner).getInt("hud_opacity",96)))/100f);view.setContentDescription("실행 진행 · "+state+" · "+count+" · "+stage);handle.setText(collapsed?"▾":"▴");handle.setContentDescription("진행창 손잡이 · 끌어서 이동 · "+(collapsed?"눌러 자세히 보기":"눌러 작게 접기"));}
    private void place(boolean attached){
        int[] size=owner.size();int handleSize=Ui.dp(owner,48),gap=Ui.dp(owner,4),bodyWidth=width();
        view.measure(View.MeasureSpec.makeMeasureSpec(bodyWidth,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));measuredHeight=collapsed?handleSize:Math.max(handleSize,view.getMeasuredHeight());
        int totalWidth=bodyWidth+gap+handleSize,minY=Ui.dp(owner,24),maxY=Math.max(minY,size[1]-Ui.dp(owner,24)-measuredHeight),maxX=Math.max(0,size[0]-totalWidth);
        if(!positioned){params.x=Ui.prefs(owner).getInt("hud_x",Math.max(0,(size[0]-totalWidth)/2));params.y=Ui.prefs(owner).getInt("hud_y",Math.max(minY,size[1]-measuredHeight-Ui.dp(owner,44)));positioned=true;}
        params.x=Math.max(0,Math.min(maxX,params.x));params.y=Math.max(minY,Math.min(maxY,params.y));
        Rect grip=new Rect(params.x+bodyWidth+gap,params.y,params.x+totalWidth,params.y+handleSize);
        // 이동 손잡이가 기존 정지 제어창을 가리지 않도록 먼저 아래쪽으로 피합니다.
        if(!avoid.isEmpty()&&Rect.intersects(grip,avoid)){int below=avoid.bottom+gap;if(below<=maxY)params.y=Math.max(minY,below);else params.y=Math.max(minY,Math.min(maxY,avoid.top-handleSize-gap));}
        params.width=bodyWidth;params.height=collapsed?handleSize:WindowManager.LayoutParams.WRAP_CONTENT;handleParams.x=params.x+bodyWidth+gap;handleParams.y=params.y;
        if(attached){wm.updateViewLayout(view,params);if(handleRegistered)wm.updateViewLayout(handle,handleParams);}
    }
    @android.annotation.SuppressLint("ClickableViewAccessibility") private void drag(){handle.setOnTouchListener(new View.OnTouchListener(){float sx,sy;int ox,oy;boolean moved;final int slop=ViewConfiguration.get(owner).getScaledTouchSlop();public boolean onTouch(View v,MotionEvent event){
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:sx=event.getRawX();sy=event.getRawY();ox=params.x;oy=params.y;moved=false;return true;
            case MotionEvent.ACTION_MOVE:float dx=event.getRawX()-sx,dy=event.getRawY()-sy;moved|=dx*dx+dy*dy>slop*slop;if(moved){params.x=ox+Math.round(dx);params.y=oy+Math.round(dy);place(true);}return true;
            case MotionEvent.ACTION_UP:if(!moved)v.performClick();Ui.prefs(owner).edit().putInt("hud_x",params.x).putInt("hud_y",params.y).apply();return true;
            case MotionEvent.ACTION_CANCEL:return true;
            default:return true;
        }
    }});}
    public void resetPosition(){Ui.prefs(owner).edit().remove("hud_x").remove("hud_y").apply();positioned=false;if(view!=null)place(true);}
    private boolean intersects(View value,WindowManager.LayoutParams layout,RectF bounds){if(value==null||layout==null)return false;int width=layout.width>0?layout.width:value.getWidth(),height=layout.height>0?layout.height:value.getHeight();RectF configured=new RectF(layout.x,layout.y,layout.x+width,layout.y+height);if(RectF.intersects(bounds,configured))return true;if(value.getParent()==null)return false;int[] at=new int[2];value.getLocationOnScreen(at);return RectF.intersects(bounds,new RectF(at[0],at[1],at[0]+value.getWidth(),at[1]+value.getHeight()));}
    boolean intersects(RectF bounds){return intersects(view,params,bounds)||intersects(handle,handleParams,bounds);}
    public void visible(boolean visible){if(view!=null){view.setVisibility(visible?View.VISIBLE:View.GONE);params.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;if(view.getParent()!=null)wm.updateViewLayout(view,params);}if(handle!=null){handle.setVisibility(visible?View.VISIBLE:View.GONE);if(visible)handleParams.flags&=~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;else handleParams.flags|=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;if(handleRegistered)wm.updateViewLayout(handle,handleParams);if(!visible){if(handleRegistered)wm.removeViewImmediate(handle);if(handle.isAttachedToWindow()||handle.getParent()!=null)throw new IllegalStateException("손잡이 창 분리가 완료되지 않았습니다");handleRegistered=false;}else if(!handleRegistered){if(handle.isAttachedToWindow()||handle.getParent()!=null)throw new IllegalStateException("손잡이 창이 아직 연결되어 있습니다");try{wm.addView(handle,handleParams);handleRegistered=true;}catch(WindowManager.BadTokenException error){close();owner.overlayTokenInvalid(error);}}}}
    public void close(){if(handle!=null){if(handle.getParent()!=null)wm.removeView(handle);handleRegistered=false;handle=null;}if(view!=null){if(view.getParent()!=null)wm.removeView(view);view=null;}positioned=false;}
}
