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
 public static int bg(Context c){return c.getColor(dark(c)?R.color.astryx_background_dark:R.color.astryx_background_light);}
 public static int card(Context c){return c.getColor(dark(c)?R.color.astryx_surface_dark:R.color.astryx_surface_light);}
 public static int ink(Context c){return c.getColor(dark(c)?R.color.astryx_text_dark:R.color.astryx_text_light);}
 public static int muted(Context c){return c.getColor(dark(c)?R.color.astryx_muted_dark:R.color.astryx_muted_light);}
 public static int accent(Context c){return c.getColor(dark(c)?R.color.astryx_accent_dark:R.color.astryx_accent_light);}
 public static int soft(Context c){return c.getColor(dark(c)?R.color.nex_soft_dark:R.color.nex_soft_light);}
 public static int line(Context c){return c.getColor(dark(c)?R.color.astryx_border_dark:R.color.astryx_border_light);}
 // 같은 문구를 주기적으로 다시 설정해 불필요한 배치 갱신을 만들지 않습니다.
 public static void textIfChanged(TextView view,CharSequence value){if(view!=null&&!android.text.TextUtils.equals(view.getText(),value))view.setText(value);}
 public static String[] recognitionNames(){return new String[]{"자동 · 화면 정보 → OCR","화면 정보","한글 OCR"};}
 public static String recognitionName(String source){return "ACCESS".equals(source)?"화면 정보":"OCR".equals(source)?"한글 OCR":"자동 · 화면 정보 → OCR";}
 public static GradientDrawable outline(Context c,int color,int radius){GradientDrawable g=rounded(color,dp(c,radius));g.setStroke(dp(c,1),line(c));return g;}
 // 시스템 애니메이션 설정을 따르며 동작 실행을 지연하지 않는 짧은 진입 전환입니다.
 public static void enter(View v){if(!android.animation.ValueAnimator.areAnimatorsEnabled())return;v.setAlpha(0f);v.setTranslationY(dp(v.getContext(),6));v.animate().alpha(1f).translationY(0).setDuration(180).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}
 public static int dp(Context c,float n){return (int)(n*c.getResources().getDisplayMetrics().density+.5f);}
 public static GradientDrawable rounded(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);return g;}
 // 배경과 포커스 테두리를 분리해 누름·선택·키보드 탐색을 함께 표시합니다.
 public static android.graphics.drawable.StateListDrawable controlStates(Context c,int color,int radius){return stateBackground(c,color,radius,true);}
 private static android.graphics.drawable.StateListDrawable stateBackground(Context c,int color,int radius,boolean border){
  android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
  GradientDrawable disabled=rounded(color,dp(c,radius));disabled.setAlpha(110);if(border)disabled.setStroke(dp(c,1),line(c));
  states.addState(new int[]{-android.R.attr.state_enabled},disabled);
  GradientDrawable focused=rounded(color,dp(c,radius));focused.setStroke(dp(c,2),accent(c));
  states.addState(new int[]{android.R.attr.state_focused},focused);
  GradientDrawable pressed=rounded(color==Color.TRANSPARENT?soft(c):color,dp(c,radius));if(border)pressed.setStroke(dp(c,1),accent(c));
  states.addState(new int[]{android.R.attr.state_pressed},pressed);
  GradientDrawable selected=rounded(color==Color.TRANSPARENT?soft(c):color,dp(c,radius));if(border)selected.setStroke(dp(c,1),accent(c));
  states.addState(new int[]{android.R.attr.state_selected},selected);
  states.addState(new int[]{android.R.attr.state_hovered},rounded(color==Color.TRANSPARENT?soft(c):color,dp(c,radius)));
  states.addState(new int[]{},border?outline(c,color,radius):rounded(color,dp(c,radius)));return states;
 }
 // 일반 행은 평평하게, 버튼과 입력은 경계를 표시해 같은 테두리의 반복을 줄입니다.
 public static android.graphics.drawable.RippleDrawable ripple(Context c,int color,int radius){return interaction(c,color,radius,false);}
 public static android.graphics.drawable.RippleDrawable outlinedRipple(Context c,int color,int radius){return interaction(c,color,radius,true);}
 public static android.graphics.drawable.RippleDrawable buttonBackground(Context c){return interaction(c,card(c),12,true);}
 private static android.graphics.drawable.RippleDrawable interaction(Context c,int color,int radius,boolean border){return new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(dark(c)?0x26FFFFFF:0x14000000),stateBackground(c,color,radius,border),rounded(Color.WHITE,dp(c,radius)));}
 public static android.graphics.drawable.StateListDrawable focusRing(Context c){
  android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();GradientDrawable focus=rounded(Color.TRANSPARENT,dp(c,10));focus.setStroke(dp(c,2),accent(c));states.addState(new int[]{android.R.attr.state_focused},focus);states.addState(new int[]{},rounded(Color.TRANSPARENT,dp(c,10)));return states;
 }
 public static void font(View v){if(v instanceof TextView){TextView text=(TextView)v;String label=text.getText().toString().trim();boolean symbolOnly=!label.isEmpty()&&label.matches("[Ⅱ▶■⋮↗＋]+");text.setTypeface(symbolOnly?android.graphics.Typeface.DEFAULT:v.getResources().getFont(R.font.nanum_gothic_bold));text.setIncludeFontPadding(true);text.setFallbackLineSpacing(true);text.setLineSpacing(dp(v.getContext(),2),1.05f);}if(v instanceof Button){Button b=(Button)v;b.setMinHeight(Math.max(b.getMinHeight(),dp(v.getContext(),48)));b.setForeground(focusRing(v.getContext()));b.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{muted(v.getContext()),b.getTextColors().getDefaultColor()}));}if(v instanceof CompoundButton){CompoundButton b=(CompoundButton)v;b.setMinHeight(dp(v.getContext(),48));b.setButtonTintList(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{android.R.attr.state_checked},new int[]{}},new int[]{line(v.getContext()),accent(v.getContext()),muted(v.getContext())}));}if(v instanceof SeekBar)((SeekBar)v).setMinHeight(dp(v.getContext(),48));if(v instanceof CompoundButton||v instanceof SeekBar||v instanceof Spinner){v.setForeground(focusRing(v.getContext()));}if(v instanceof ListView){ListView list=(ListView)v;list.setSelector(ripple(v.getContext(),Color.TRANSPARENT,10));}if(v instanceof EditText){EditText e=(EditText)v;Context c=e.getContext();e.setBackground(controlStates(c,card(c),12));e.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12));e.setMinHeight(dp(c,48));}if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)font(((ViewGroup)v).getChildAt(i));}
 public static int onAccent(Context c){return dark(c)?0xff101216:Color.WHITE;}
 public static void styleDialog(android.app.AlertDialog dialog){
  Window window=dialog.getWindow();if(window==null)return;Context c=dialog.getContext();font(window.getDecorView());window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
  int available=((WindowManager)c.getSystemService(Context.WINDOW_SERVICE)).getCurrentWindowMetrics().getBounds().width();window.setLayout(Math.min(dp(c,560),available-dp(c,32)),WindowManager.LayoutParams.WRAP_CONTENT);
  TextView message=dialog.findViewById(android.R.id.message);if(message!=null){message.setTextColor(muted(c));message.setTextSize(14);message.setLineSpacing(dp(c,4),1f);}
  for(int id:new int[]{android.app.AlertDialog.BUTTON_POSITIVE,android.app.AlertDialog.BUTTON_NEGATIVE,android.app.AlertDialog.BUTTON_NEUTRAL}){Button b=dialog.getButton(id);if(b==null)continue;b.setMinHeight(dp(c,48));b.setPadding(dp(c,16),dp(c,10),dp(c,16),dp(c,10));b.setTextSize(14);b.setSingleLine(false);b.setMaxLines(Integer.MAX_VALUE);b.setBackground(ripple(c,id==android.app.AlertDialog.BUTTON_POSITIVE?accent(c):Color.TRANSPARENT,12));b.setTextColor(id==android.app.AlertDialog.BUTTON_POSITIVE?onAccent(c):muted(c));}
 }
 public static android.graphics.Bitmap brandBitmap(Context c){int s=dp(c,48);android.graphics.Bitmap b=android.graphics.Bitmap.createBitmap(s,s,android.graphics.Bitmap.Config.ARGB_8888);android.graphics.drawable.Drawable d=androidx.core.content.ContextCompat.getDrawable(c,R.drawable.ic_macro);d.setBounds(0,0,s,s);d.draw(new android.graphics.Canvas(b));return b;}
 public static Button button(Context c,String text){Button b=new Button(c);b.setStateListAnimator(null);b.setElevation(0);b.setText(text);b.setAllCaps(false);b.setTextColor(ink(c));b.setTextSize(14);b.setSingleLine(false);b.setMaxLines(Integer.MAX_VALUE);b.setEllipsize(null);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinimumHeight(dp(c,48));b.setPadding(dp(c,12),dp(c,10),dp(c,12),dp(c,10));b.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{muted(c),ink(c)}));b.setBackground(buttonBackground(c));font(b);return b;}
}
