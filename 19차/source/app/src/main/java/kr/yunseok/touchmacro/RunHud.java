package kr.yunseok.touchmacro;
import android.content.*;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

/** 진행창을 누르면 작은 횟수 표시로 접으며 캡처와 제스처 순간에는 숨깁니다. */
public final class RunHud {
    private final MacroService owner;private final WindowManager wm;
    private LinearLayout view;private TextView progress,phase,times;
    private WindowManager.LayoutParams params;private boolean collapsed;
    private String count="",stage="",timing="",state="";
    public RunHud(MacroService owner){this.owner=owner;wm=(WindowManager)owner.getSystemService(Context.WINDOW_SERVICE);collapsed=Ui.prefs(owner).getBoolean("hud_collapsed",false);}
    private TextView line(Context c,int size,int color){TextView value=new TextView(c);value.setTextSize(size);value.setTextColor(color);value.setSingleLine(true);value.setEllipsize(TextUtils.TruncateAt.END);return value;}
    public void update(RunProgress run,String state,String stage,String timing,boolean visible){
        count=run.counter();this.state=state;this.stage=stage;this.timing=timing;
        if(view==null){Context c=Ui.themed(owner);view=new LinearLayout(c);view.setOrientation(LinearLayout.VERTICAL);view.setPadding(Ui.dp(c,10),Ui.dp(c,7),Ui.dp(c,10),Ui.dp(c,7));view.setGravity(Gravity.CENTER_VERTICAL);view.setMinimumHeight(Ui.dp(c,48));view.setBackground(Ui.outline(c,Ui.card(c),12));view.setAlpha(.96f);
            progress=line(c,12,Ui.accent(c));view.addView(progress);phase=line(c,12,Ui.ink(c));view.addView(phase);
            times=line(c,10,Ui.muted(c));times.setSingleLine(false);times.setMaxLines(2);view.addView(times);Ui.font(view);
            view.setClickable(true);view.setOnClickListener(v->{collapsed=!collapsed;Ui.prefs(owner).edit().putBoolean("hud_collapsed",collapsed).apply();render();resize();});
            params=new WindowManager.LayoutParams(width(),collapsed?Ui.dp(c,48):WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);params.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;params.y=Ui.dp(c,44);render();wm.addView(view,params);
        }else render();visible(visible);
    }
    private int width(){return Math.min(Ui.dp(owner,collapsed?200:280),Math.max(Ui.dp(owner,48),owner.size()[0]-Ui.dp(owner,32)));}
    private void render(){String label=count.isEmpty()?state:(!state.equals("실행 중")?state+" · ":"")+count;progress.setText(owner.getString(R.string.nex_hud_count,label,collapsed?"▾":"▴"));phase.setText(stage);times.setText(timing);phase.setVisibility(collapsed?View.GONE:View.VISIBLE);times.setVisibility(collapsed?View.GONE:View.VISIBLE);view.setContentDescription("실행 진행 · "+state+" · "+count+" · "+stage+" · "+(collapsed?"눌러 자세히 보기":"눌러 작게 접기"));}
    private void resize(){params.width=width();params.height=collapsed?Ui.dp(owner,48):WindowManager.LayoutParams.WRAP_CONTENT;wm.updateViewLayout(view,params);}
    boolean intersects(RectF bounds){if(view==null||!view.isShown())return false;int[] at=new int[2];view.getLocationOnScreen(at);return RectF.intersects(bounds,new RectF(at[0],at[1],at[0]+view.getWidth(),at[1]+view.getHeight()));}
    public void visible(boolean visible){if(view!=null)view.setVisibility(visible?View.VISIBLE:View.GONE);}
    public void close(){if(view!=null){wm.removeView(view);view=null;}}
}
