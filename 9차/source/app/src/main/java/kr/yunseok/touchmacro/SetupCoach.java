package kr.yunseok.touchmacro;
import android.content.*;
import android.graphics.PixelFormat;
import android.view.*;
import android.widget.*;

/** Small pass-around guide: navigation remains available until the user starts drawing/recording. */
public final class SetupCoach {
    private final MacroService owner;private final WindowManager wm;private View view;
    public SetupCoach(MacroService owner){this.owner=owner;wm=(WindowManager)owner.getSystemService(Context.WINDOW_SERVICE);}
    public void show(String instruction,String label,Runnable action,Runnable back){
        close();Context c=Ui.themed(owner);LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(Ui.dp(c,10),Ui.dp(c,7),Ui.dp(c,10),Ui.dp(c,7));box.setBackground(Ui.rounded(Ui.card(c),Ui.dp(c,16)));box.setElevation(Ui.dp(c,5));
        TextView hint=new TextView(c);hint.setText(instruction);hint.setTextSize(12);hint.setTextColor(Ui.ink(c));box.addView(hint);
        LinearLayout row=new LinearLayout(c);box.addView(row);Button go=Ui.button(c,label),settings=Ui.button(c,"설정으로");go.setTextSize(12);settings.setTextSize(11);row.addView(go,new LinearLayout.LayoutParams(0,Ui.dp(c,44),1.4f));row.addView(settings,new LinearLayout.LayoutParams(0,Ui.dp(c,44),1));
        go.setOnClickListener(v->{try{action.run();}catch(Exception e){Toast.makeText(owner,e.getMessage(),Toast.LENGTH_LONG).show();}});settings.setOnClickListener(v->back.run());Ui.font(box);
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(Ui.dp(c,270),-2,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.END;p.x=Ui.dp(c,8);p.y=Ui.dp(c,36);view=box;wm.addView(view,p);
    }
    public void close(){if(view!=null){wm.removeView(view);view=null;}}
}
