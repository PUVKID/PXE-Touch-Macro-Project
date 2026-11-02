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
 public static int bg(Context c){return Color.parseColor(dark(c)?"#101B24":"#FFFFFF");}
 public static int card(Context c){return Color.parseColor(dark(c)?"#162630":"#FFFFFF");}
 public static int ink(Context c){return Color.parseColor(dark(c)?"#F5FAFA":"#101B24");}
 public static int muted(Context c){return Color.parseColor(dark(c)?"#B4C5CD":"#536675");}
 public static int accent(Context c){return Color.parseColor(dark(c)?"#56D7C7":"#007F78");}
 public static int soft(Context c){return Color.parseColor(dark(c)?"#203B42":"#E9F7F3");}
 public static int line(Context c){return Color.parseColor(dark(c)?"#31444E":"#DCE5E8");}
 public static String[] recognitionNames(){return new String[]{"자동 · 화면 정보 → OCR","화면 정보","한글 OCR"};}
 public static String recognitionName(String source){return "ACCESS".equals(source)?"화면 정보":"OCR".equals(source)?"한글 OCR":"자동 · 화면 정보 → OCR";}
 public static GradientDrawable outline(Context c,int color,int radius){GradientDrawable g=rounded(color,dp(c,radius));g.setStroke(dp(c,1),line(c));return g;}
 // 시스템 애니메이션 설정을 따르며 동작 실행을 지연하지 않는 짧은 진입 전환입니다.
 public static void enter(View v){if(!android.animation.ValueAnimator.areAnimatorsEnabled())return;v.setAlpha(0f);v.setTranslationY(dp(v.getContext(),6));v.animate().alpha(1f).translationY(0).setDuration(180).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}
 public static int dp(Context c,float n){return (int)(n*c.getResources().getDisplayMetrics().density+.5f);}
 public static GradientDrawable rounded(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);return g;}
 public static android.graphics.drawable.RippleDrawable ripple(Context c,int color,int radius){return new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(dark(c)?0x26FFFFFF:0x14000000),rounded(color,dp(c,radius)),null);}
 public static void font(View v){if(v instanceof TextView){((TextView)v).setTypeface(v.getResources().getFont(R.font.nanum_gothic_bold));((TextView)v).setIncludeFontPadding(false);}if(v instanceof CompoundButton)((CompoundButton)v).setMinHeight(dp(v.getContext(),48));if(v instanceof SeekBar)((SeekBar)v).setMinHeight(dp(v.getContext(),48));if(v instanceof EditText){EditText e=(EditText)v;Context c=e.getContext();e.setBackground(outline(c,soft(c),8));e.setPadding(dp(c,14),dp(c,14),dp(c,14),dp(c,14));e.setMinHeight(dp(c,50));}if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)font(((ViewGroup)v).getChildAt(i));}
 public static android.graphics.Bitmap brandBitmap(Context c){int s=dp(c,48);android.graphics.Bitmap b=android.graphics.Bitmap.createBitmap(s,s,android.graphics.Bitmap.Config.ARGB_8888);android.graphics.drawable.Drawable d=androidx.core.content.ContextCompat.getDrawable(c,R.drawable.ic_macro);d.setBounds(0,0,s,s);d.draw(new android.graphics.Canvas(b));return b;}
 public static Button button(Context c,String text){Button b=new Button(c);b.setStateListAnimator(null);b.setElevation(0);b.setText(text);b.setAllCaps(false);b.setTextColor(ink(c));b.setTextSize(13);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinimumHeight(dp(c,48));b.setPadding(dp(c,12),0,dp(c,12),0);android.content.res.ColorStateList ripple=android.content.res.ColorStateList.valueOf(dark(c)?0x26FFFFFF:0x14000000);b.setBackground(new android.graphics.drawable.RippleDrawable(ripple,outline(c,Color.TRANSPARENT,8),null));font(b);return b;}
}
