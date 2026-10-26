package kr.yunseok.touchmacro;
import android.content.*;
import android.graphics.PixelFormat;
import android.view.*;
import android.widget.*;

/** 하단 상태창은 터치를 가리지 않으며 OCR 캡처 순간에만 숨깁니다. */
public final class RunHud {
    private final MacroService owner;private final WindowManager wm;private LinearLayout view;private TextView progress,times;
    public RunHud(MacroService owner){this.owner=owner;wm=(WindowManager)owner.getSystemService(Context.WINDOW_SERVICE);}
    public void update(String line,String timing,boolean visible){
        if(view==null){Context c=Ui.themed(owner);view=new LinearLayout(c);view.setOrientation(LinearLayout.VERTICAL);view.setPadding(Ui.dp(c,16),Ui.dp(c,11),Ui.dp(c,16),Ui.dp(c,11));view.setBackground(Ui.rounded(Ui.card(c),Ui.dp(c,22)));view.setAlpha(.96f);
            progress=new TextView(c);progress.setTextSize(12);progress.setTextColor(Ui.ink(c));progress.setMaxLines(2);view.addView(progress);
            times=new TextView(c);times.setTextSize(10);times.setTextColor(Ui.muted(c));times.setMaxLines(2);view.addView(times);Ui.font(view);
            WindowManager.LayoutParams p=new WindowManager.LayoutParams(Math.min(Ui.dp(c,390),Math.max(Ui.dp(c,200),owner.size()[0]-Ui.dp(c,32))),-2,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT);p.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;p.y=Ui.dp(c,44);wm.addView(view,p);
        }
        progress.setText(line);times.setText(timing);visible(visible);
    }
    public void visible(boolean visible){if(view!=null)view.setVisibility(visible?View.VISIBLE:View.GONE);}
    public void close(){if(view!=null){wm.removeView(view);view=null;}}
}
