package kr.yunseok.touchmacro;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
public final class Ui {
 public static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("nex_ui",0);}
 public static boolean dark(Context c){int mode=prefs(c).getInt("theme",0);return mode==2||(mode==0&&(c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);}
 public static int theme(Context c){return dark(c)?R.style.NexDark:R.style.AppTheme;}
 public static Context themed(Context c){return new android.view.ContextThemeWrapper(c,theme(c));}
 public static int bg(Context c){return Color.parseColor(dark(c)?"#0C1220":"#F6F7F9");}
 public static int card(Context c){return Color.parseColor(dark(c)?"#172133":"#FFFFFF");}
 public static int ink(Context c){return Color.parseColor(dark(c)?"#F2F6FF":"#192840");}
 public static int muted(Context c){return Color.parseColor(dark(c)?"#A6B5CB":"#596B86");}
 public static int accent(Context c){return Color.parseColor(dark(c)?"#93ACFF":"#2856D8");}
 public static int soft(Context c){return Color.parseColor(dark(c)?"#243553":"#EBEEF4");}
 public static int dp(Context c,float n){return (int)(n*c.getResources().getDisplayMetrics().density+.5f);}
 public static GradientDrawable rounded(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);return g;}
 public static void font(View v){if(v instanceof TextView)((TextView)v).setTypeface(v.getResources().getFont(R.font.nanum_gothic_bold));if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)font(((ViewGroup)v).getChildAt(i));}
 public static android.graphics.Bitmap brandBitmap(Context c){int s=dp(c,48);android.graphics.Bitmap b=android.graphics.Bitmap.createBitmap(s,s,android.graphics.Bitmap.Config.ARGB_8888);android.graphics.drawable.Drawable d=androidx.core.content.ContextCompat.getDrawable(c,R.drawable.ic_macro);d.setBounds(0,0,s,s);d.draw(new android.graphics.Canvas(b));return b;}
 public static Button button(Context c,String text){Button b=new Button(c);b.setStateListAnimator(null);b.setElevation(0);b.setText(text);b.setAllCaps(false);b.setTextColor(ink(c));b.setTextSize(13);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinimumHeight(dp(c,44));b.setPadding(dp(c,8),0,dp(c,8),0);b.setBackground(rounded(soft(c),dp(c,11)));font(b);return b;}
}
