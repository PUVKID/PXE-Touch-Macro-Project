package kr.yunseok.touchmacro;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public class MainActivity extends Activity {
    private Macro macro;
    private LinearLayout page, list;private TextView state,dockProgress,dockTiming,detailTiming,detailPerformance;
    private LinearLayout executionDock;private Button pauseContinue;
    private String exportText="",exportMime="application/json";
    private int blue,ink,muted;private int tab=4;
    private boolean testing=false,shownConnected=false;
    private final Handler connectionUi=new Handler(Looper.getMainLooper());
    private final Runnable watchConnection=new Runnable(){public void run(){if(!testing&&shownConnected!=(MacroService.get()!=null))render();if(!testing)updateDock();connectionUi.postDelayed(this,250);}};
    @Override protected void onDestroy(){connectionUi.removeCallbacksAndMessages(null);if(page!=null)page.animate().cancel();super.onDestroy();}
    @Override protected void onPause(){connectionUi.removeCallbacks(watchConnection);if(page!=null)page.animate().cancel();if(!testing&&MacroService.get()!=null)MacroService.get().controller().setVisible(true);super.onPause();}
    // 현재 설치 버전을 표시해 차수별 고정 문구가 남지 않게 합니다.
    private String installedVersionLabel(){
        try{
            android.content.pm.PackageInfo info=getPackageManager().getPackageInfo(getPackageName(),0);
            return "PXE "+info.versionName+" · "+info.getLongVersionCode()+"차";
        }catch(android.content.pm.PackageManager.NameNotFoundException unavailable){return "PXE";}
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    @Override public void onCreate(Bundle b){setTheme(Ui.theme(this));super.onCreate(b);getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);blue=Ui.accent(this);ink=Ui.ink(this);muted=Ui.muted(this);macro=Store.load(this);setTaskDescription(new ActivityManager.TaskDescription("PXE",Ui.brandBitmap(this),Ui.bg(this)));if(getIntent().getBooleanExtra("watch",false))tab=4;else if(getIntent().getBooleanExtra("settings",false))tab=3;render();if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},201);}
    @Override public void onResume(){super.onResume();if(macro==null)return;if(!testing&&MacroService.get()!=null&&MacroService.get().running){if(!MacroService.get().paused)MacroService.get().togglePause();MacroService.get().log("편집 화면 · 일시정지");}if(!testing)macro=Store.load(this);
        String picked=getSharedPreferences("macros",0).getString("picked","");
        if(!picked.isEmpty()){getSharedPreferences("macros",0).edit().remove("picked").apply();try{String[] xy=picked.split(",");Step s=new Step("TAP");s.x=Double.parseDouble(xy[0]);s.y=Double.parseDouble(xy[1]);macro.steps.add(s);save();testing=false;render();edit(s);}catch(Exception e){toast("좌표를 읽지 못했습니다");}}
        else if(!testing)render();if(!testing&&SetupFlow.active(this)&&MacroService.get()!=null&&!MacroService.get().running)MacroService.get().controller().setVisible(false);connectionUi.removeCallbacks(watchConnection);connectionUi.postDelayed(watchConnection,800);
    }
    private void font(View v){Ui.font(v);}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(i.getBooleanExtra("watch",false))tab=4;else if(i.getBooleanExtra("settings",false))tab=3;macro=Store.load(this);render();}
    private void save(){Store.save(this,macro);}
    private TextView text(LinearLayout parent,String value,int sp,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(sp);t.setTextColor(color);t.setPadding(0,dp(4),0,dp(4));t.setLineSpacing(dp(sp<=14?3:2),1f);parent.addView(t);return t;}
    private Button button(LinearLayout box,String label,Runnable click){
        Button b=Ui.button(this,label);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(4),0,dp(4));box.addView(b,p);b.setOnClickListener(v->click.run());return b;
    }
    private LinearLayout row(LinearLayout box){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);box.addView(r,new LinearLayout.LayoutParams(-1,-2));return r;}
    // 좁은 화면과 큰 글꼴에서는 긴 한글 버튼을 위아래로 배치합니다.
    private boolean stackedActions(){return getResources().getConfiguration().screenWidthDp<400||getResources().getConfiguration().fontScale>1.15f;}
    private LinearLayout.LayoutParams actionSize(LinearLayout row){boolean stacked=stackedActions();row.setOrientation(stacked?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(stacked?-1:0,-2,stacked?0:1);p.setMargins(stacked?0:dp(4),dp(4),stacked?0:dp(4),dp(4));return p;}
    private void halfButton(LinearLayout r,String label,Runnable click){Button b=button(r,label,click);b.setLayoutParams(actionSize(r));}
    private LinearLayout card(LinearLayout parent){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(14));c.setBackground(Ui.outline(this,Ui.card(this),16));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(8),0,dp(8));parent.addView(c,p);return c;
    }

    private void content(View v){v.setOnApplyWindowInsetsListener((view,insets)->{if(Build.VERSION.SDK_INT>=35){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());view.setPadding(view.getPaddingLeft(),bars.top,view.getPaddingRight(),bars.bottom);}return insets;});setContentView(v);v.post(()->{WindowInsetsController bars=getWindow().getInsetsController();if(bars!=null){int mask=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;bars.setSystemBarsAppearance(Ui.dark(this)?0:mask,mask);}});}
    private void render(){
        testing=false;dockProgress=null;dockTiming=null;detailTiming=null;detailPerformance=null;executionDock=null;pauseContinue=null;blue=Ui.accent(this);ink=Ui.ink(this);muted=Ui.muted(this);
        getWindow().setStatusBarColor(Ui.bg(this));getWindow().setNavigationBarColor(Ui.bg(this));
        int lightBars=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;int barFlags=getWindow().getDecorView().getSystemUiVisibility();getWindow().getDecorView().setSystemUiVisibility(Ui.dark(this)?barFlags&~lightBars:barFlags|lightBars);
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setBackgroundColor(Ui.bg(this));
        LinearLayout header=form();header.setPadding(dp(24),dp(8),dp(24),dp(8));shell.addView(header);
        LinearLayout brand=row(header);brand.setGravity(Gravity.CENTER_VERTICAL);ImageView mark=new ImageView(this);mark.setImageResource(R.drawable.ic_macro);mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);LinearLayout.LayoutParams markSize=new LinearLayout.LayoutParams(dp(30),dp(30));markSize.setMargins(0,0,dp(10),0);brand.addView(mark,markSize);TextView logo=text(brand,"PXE",19,ink);logo.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
        boolean connected=MacroService.get()!=null;shownConnected=connected;
        state=text(brand,connected?"연결됨":"연결하기",12,connected?blue:muted);state.setGravity(Gravity.CENTER);state.setMinHeight(dp(48));state.setPadding(dp(12),0,dp(12),0);state.setBackground(Ui.ripple(this,Color.TRANSPARENT,12));state.setFocusable(true);state.setContentDescription(connected?"접근성 연결됨 · 눌러 연결 설정":"접근성 연결 필요 · 눌러 연결 설정");state.setOnClickListener(v->accessibility());
        Button shortcut=Ui.button(this,"단축창");shortcut.setTextSize(12);shortcut.setPadding(dp(12),dp(8),dp(12),dp(8));LinearLayout.LayoutParams shortcutSize=new LinearLayout.LayoutParams(-2,-2);shortcutSize.setMargins(dp(8),0,0,0);brand.addView(shortcut,shortcutSize);shortcut.setOnClickListener(v->{if(service()){save();MacroService.get().showControls();moveTaskToBack(true);}});
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);page=form();page.setPadding(dp(24),dp(12),dp(24),dp(32));scroll.addView(page);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        if(tab==0){
            text(page,"글자 찾기",24,ink);text(page,"원하는 글자를 찾아 바로 터치하세요.",13,muted);space(page,16);
            EditText word=new EditText(this);word.setText(Ui.prefs(this).getString("word","확인"));word.setHint("찾을 글자 입력");word.setSingleLine(false);word.setMaxLines(3);word.setHorizontallyScrolling(false);word.setSelectAllOnFocus(true);word.setTextSize(20);word.setTextColor(ink);word.setHintTextColor(muted);word.setPadding(dp(16),dp(16),dp(16),dp(16));word.setBackground(Ui.rounded(Ui.card(this),dp(14)));word.setMinHeight(dp(64));word.setContentDescription("찾을 글자 입력");page.addView(word,new LinearLayout.LayoutParams(-1,-2));
            word.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){Ui.prefs(MainActivity.this).edit().putString("word",s.toString()).apply();}public void afterTextChanged(android.text.Editable e){}});
            targetWordShortcuts(page);
            space(page,8);CheckBox back=check(page,"없으면 뒤로가서 다시 찾기",Ui.prefs(this).getBoolean("word_back",true));back.setTextSize(13);back.setTextColor(ink);back.setOnCheckedChangeListener((v,on)->Ui.prefs(this).edit().putBoolean("word_back",on).apply());
            space(page,16);primary(page,"글자 찾기 시작",()->quickWord(false));
            LinearLayout actions=row(page);quietHalf(actions,"미리보기",()->quickWord(true));quietHalf(actions,"인식 설정",this::recognitionSettings);
            space(page,12);quiet(page,"매크로에 이 글자 추가",()->{Step s=FloatingController.wordStep(this);try{s.validate(macro.steps.size()+1);macro.steps.add(s);save();tab=1;render();edit(s);}catch(Exception e){toast(e.getMessage());}});
            space(page,16);text(page,"실행하면 3초 뒤 시작합니다.\n다른 앱에서는 화면 위 단축창을 사용하세요.",12,muted);
        }else if(tab==4){buildWatchPage();
        }else if(tab==1){
            LinearLayout heading=row(page);TextView name=text(heading,macro.name,23,ink);name.setMinLines(2);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));quietHalfFixed(heading,"더보기",this::macroMenu);
            text(page,macro.steps.size()+"개 동작 · "+(macro.rounds==0?"무한 반복":macro.rounds+"회 반복"),12,muted);space(page,16);
            LinearLayout add=row(page);halfButton(add,"동작 추가",this::chooseStep);halfButton(add,"녹화",this::record);Button addAction=(Button)add.getChildAt(0);addAction.setBackground(Ui.ripple(this,blue,10));addAction.setTextColor(Ui.onAccent(this));
            space(page,12);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);page.addView(list);refreshList();
            space(page,12);settingRow(page,"반복 · 시간 설정",(macro.rounds==0?"무한":macro.rounds+"회")+" 반복 · 지연 시간 설정",this::settings);space(page,16);primary(page,"매크로 시작",this::start);
        }else if(tab==2){
            text(page,"저장한 매크로",23,ink);space(page,12);
            LinkedHashMap<String,Macro> all=Store.library(this);if(all.isEmpty())text(page,"저장한 매크로가 없습니다.",14,muted);
            for(String name:all.keySet())button(page,name,()->{macro=new Macro(all.get(name).json());save();tab=1;render();});
            quiet(page,"저장본 관리",this::library);
        }else{
            text(page,"설정",24,ink);text(page,"화면과 실행 환경을 설정하세요.",13,muted);space(page,16);
            settingRow(page,"화면 모드",Ui.prefs(this).getInt("theme",0)==0?"기기 설정":Ui.dark(this)?"다크":"라이트",this::themeChoice);
            settingRow(page,"화면 위 컨트롤","크기 · 투명도 · 번호 표시",this::controlSettings);
            settingRow(page,"접근성",connected?"연결됨":"연결 필요",this::accessibility);
            space(page,16);text(page,"기록과 도움말",13,muted);LinearLayout resources=new LinearLayout(this);resources.setOrientation(LinearLayout.VERTICAL);page.addView(resources);
            settingsAction(resources,"실행 기록",this::logs);settingsAction(resources,"오류 기록",this::errorLogs);settingsAction(resources,"사용법",this::help);settingsAction(resources,"인식 테스트",this::testScreen);
            space(page,12);text(page,installedVersionLabel(),11,muted);
        }
        addStatusDock(shell);addNavigation(shell);content(shell);Ui.font(shell);Ui.enter(page);
    }
    // 실행 중에는 어느 탭에서도 정지·일시정지 상태와 조작을 바로 확인할 수 있습니다.
    private void addStatusDock(LinearLayout shell){
        executionDock=new LinearLayout(this);executionDock.setOrientation(LinearLayout.VERTICAL);executionDock.setPadding(dp(20),dp(8),dp(20),dp(8));executionDock.setBackgroundColor(Ui.soft(this));shell.addView(executionDock,new LinearLayout.LayoutParams(-1,-2));
        dockProgress=text(executionDock,"",14,ink);dockProgress.setPadding(0,0,0,dp(4));dockProgress.setContentDescription("실행 상태");dockProgress.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        dockTiming=text(executionDock,"",12,muted);dockTiming.setPadding(0,0,0,0);
        LinearLayout controls=row(executionDock);controls.setTag("실행 제어");
        pauseContinue=button(controls,"일시정지",()->{MacroService svc=MacroService.get();if(svc==null||!svc.running)return;svc.togglePause();updateDock();});pauseContinue.setContentDescription("실행 일시정지 또는 계속 실행");pauseContinue.setLayoutParams(controlSize());
        Button stop=button(controls,"정지",()->{MacroService svc=MacroService.get();if(svc!=null)svc.stop();updateDock();});stop.setContentDescription("실행 정지");stop.setLayoutParams(controlSize());buttonIcon(stop,R.drawable.pxe_control_stop);
        updateDock();
    }
    private LinearLayout.LayoutParams controlSize(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.setMargins(dp(4),dp(4),dp(4),0);return p;}
    private void buttonIcon(Button b,int resource){android.graphics.drawable.Drawable icon=androidx.appcompat.content.res.AppCompatResources.getDrawable(this,resource).mutate();icon.setTint(ink);icon.setBounds(0,0,dp(18),dp(18));b.setCompoundDrawablesRelative(icon,null,null,null);b.setCompoundDrawablePadding(dp(8));}
    private void updateDock(){
        MacroService svc=MacroService.get();if(detailTiming!=null)Ui.textIfChanged(detailTiming,svc==null?"접근성을 연결하면 실행 시간이 표시됩니다.":svc.timingText());if(detailPerformance!=null)Ui.textIfChanged(detailPerformance,svc==null?"실행하면 단계별 측정이 표시됩니다.":svc.performanceText());if(executionDock==null)return;
        boolean running=svc!=null&&svc.running;executionDock.setVisibility(running?View.VISIBLE:View.GONE);if(!running)return;
        Ui.textIfChanged(dockProgress,svc.paused?"일시정지 중 · 계속 실행":"실행 중");
        Ui.textIfChanged(dockTiming,svc.progressText());
        Ui.textIfChanged(pauseContinue,svc.paused?"계속 실행":"일시정지");
        int icon=svc.paused?R.drawable.pxe_control_play:R.drawable.pxe_control_pause;
        if(!Integer.valueOf(icon).equals(pauseContinue.getTag())){buttonIcon(pauseContinue,icon);pauseContinue.setTag(icon);}
        pauseContinue.setContentDescription(svc.paused?"계속 실행":"일시정지");
    }
    private void rule(LinearLayout parent){View line=new View(this);line.setBackgroundColor(Ui.line(this));parent.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));}
    private void addNavigation(LinearLayout shell){
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);nav.setPadding(dp(8),0,dp(8),0);nav.setBackgroundColor(Ui.card(this));nav.setMinimumHeight(dp(64));rule(shell);shell.addView(nav,new LinearLayout.LayoutParams(-1,-2));
        String[] labels={"탐지","동작","글자","설정"};int[] pages={4,1,0,3},icons={R.drawable.pxe_scan,R.drawable.pxe_steps,R.drawable.pxe_text,R.drawable.pxe_settings};
        for(int i=0;i<pages.length;i++){final int chosen=pages[i];boolean active=tab==chosen||(chosen==1&&tab==2);LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);item.setContentDescription(labels[i]);item.setClickable(true);item.setFocusable(true);item.setSelected(active);item.setPadding(0,dp(8),0,dp(8));item.setMinimumHeight(dp(64));item.setBackground(Ui.ripple(this,active?Ui.soft(this):Color.TRANSPARENT,14));nav.addView(item,new LinearLayout.LayoutParams(0,-2,1));
            ImageView icon=new ImageView(this);icon.setImageResource(icons[i]);icon.setColorFilter(active?blue:muted);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);item.addView(icon,new LinearLayout.LayoutParams(dp(23),dp(23)));TextView label=text(item,labels[i],11,active?blue:muted);label.setPadding(0,dp(4),0,dp(4));label.setGravity(Gravity.CENTER);label.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);item.setOnClickListener(v->{tab=chosen;render();});}
    }
    private void actionRow(LinearLayout parent,int icon,String title,String detail,boolean selected,Runnable action){
        LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(12),dp(12),dp(12),dp(12));r.setMinimumHeight(dp(detail.isEmpty()?56:72));r.setFocusable(true);r.setSelected(selected);r.setBackground(Ui.ripple(this,selected?Ui.soft(this):Ui.card(this),12));parent.addView(r,new LinearLayout.LayoutParams(-1,-2));
        ImageView symbol=new ImageView(this);symbol.setImageResource(icon);symbol.setColorFilter(selected?blue:muted);symbol.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);LinearLayout.LayoutParams iconSize=new LinearLayout.LayoutParams(dp(26),dp(26));iconSize.setMargins(0,0,dp(16),0);r.addView(symbol,iconSize);
        LinearLayout copy=new LinearLayout(this);copy.setOrientation(LinearLayout.VERTICAL);r.addView(copy,new LinearLayout.LayoutParams(0,-2,1));TextView label=text(copy,title,14,ink);label.setPadding(0,0,0,detail.isEmpty()?0:dp(4));if(!detail.isEmpty()){TextView value=text(copy,detail,13,muted);value.setPadding(0,0,0,0);}
        ImageView arrow=new ImageView(this);arrow.setImageResource(R.drawable.pxe_chevron);arrow.setColorFilter(muted);arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);r.addView(arrow,new LinearLayout.LayoutParams(dp(20),dp(20)));r.setOnClickListener(v->action.run());r.setContentDescription(title+" · "+detail);rule(parent);
    }
    private void buildWatchPage(){
        if(SetupFlow.active(this)){new SetupWizard(this).render(page);return;}
        WatchConfig p=WatchStore.load(this);
        text(page,"목록 탐지",24,ink);text(page,"찾을 글자",12,muted);
        TextView word=text(page,p.rule.text.isEmpty()?"글자를 설정하세요":p.rule.text,20,ink);word.setContentDescription("현재 탐지할 글자");word.setPadding(dp(16),dp(14),dp(16),dp(14));word.setBackground(Ui.outline(this,Ui.card(this),14));word.setSingleLine(false);word.setHorizontallyScrolling(false);word.setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_SIMPLE);
        targetWordShortcuts(page);
        space(page,12);primary(page,"탐지 시작",this::startWatch);
        text(page,"글자나 설정을 바꾼 뒤 다시 시작할 수 있습니다.",12,muted);
        LinearLayout summary=card(page);
        actionRow(summary,R.drawable.pxe_text,"글자 · 매크로 설정",Ui.recognitionName(p.rule.source)+" · "+p.repetitions+"회 반복",true,this::watchSettings);
        actionRow(summary,R.drawable.pxe_focus,"탐지 영역",p.areaCount()+"개 영역 · 순서대로 확인",false,this::watchRegion);
        actionRow(summary,R.drawable.pxe_steps,"실행 매크로",p.macroName.isEmpty()?Store.load(this).name:p.macroName,false,this::watchSettings);
        text(summary,"완료 후 "+Times.durationLabel(p.cooldownMs)+" 대기 · 새로고침 "+p.intervalSeconds+"초",12,muted);
        LinearLayout extras=fold(page,"미리보기와 추가 설정");
        detailTiming=text(extras,"",12,muted);detailPerformance=text(extras,"",12,muted);
        quiet(extras,"인식 미리보기",()->{if(!service())return;try{Step step=new Step(p.rule.json());step.type="TEXT_CHECK";step.timeout=0;step.failure="STOP";Macro preview=new Macro();preview.name="탐지 영역 미리보기";preview.startDelay=3000;preview.maxMinutes=1;preview.steps.add(step);MacroService.get().restart(preview);moveTaskToBack(true);}catch(Exception e){toast(e.getMessage());}});
        quiet(extras,"처음부터 다시 설정",()->{SetupFlow.begin(this);render();});
        if(p.preventDuplicate)text(extras,"사라짐 확인 켜짐 · 글자가 사라진 뒤 다시 실행합니다.",12,muted);
        text(extras,MacroService.get()==null?"접근성을 연결하면 실행할 수 있습니다.":"접근성 연결됨 · 다른 앱에서 단축창으로 제어할 수 있습니다.",12,muted);
    }
    private void targetWordShortcuts(LinearLayout parent){
        space(parent,12);LinearLayout shortcuts=new LinearLayout(this);shortcuts.setOrientation(LinearLayout.VERTICAL);parent.addView(shortcuts,new LinearLayout.LayoutParams(-1,-2));TargetWords.fillShortcuts(this,shortcuts,()->render());
    }
    void refreshSetup(){macro=Store.load(this);tab=4;render();}
    void setupOutside(String kind){if(!service())return;if(MacroService.get().running||MacroService.get().controller().recording){toast("먼저 실행과 녹화를 정지하세요");return;}moveTaskToBack(true);new Handler(Looper.getMainLooper()).postDelayed(()->{if(MacroService.get()!=null)MacroService.get().controller().showSetupCoach(kind);},350);}
    private void startWatch(){if(!service())return;try{save();MacroService.get().restartWatch(WatchStore.load(this));moveTaskToBack(true);}catch(Exception e){toast(e.getMessage());}}
    private void watchRegion(){if(!service())return;if(MacroService.get().running||MacroService.get().controller().recording){toast("먼저 실행과 녹화를 정지하세요");return;}moveTaskToBack(true);new Handler(Looper.getMainLooper()).postDelayed(()->{if(MacroService.get()!=null)MacroService.get().controller().pickWatchRegion();},300);}
    private void watchSettings(){
        WatchConfig p=WatchStore.load(this);LinearLayout f=form();
        EditText word=field(f,"탐지할 글자",p.rule.text,false);
        Spinner source=spinner(f,"목록 인식 방식",Ui.recognitionNames(),selected(new String[]{"AUTO","ACCESS","OCR"},p.rule.source));
        text(f,"목록 탐지에만 적용합니다. 실행 매크로의 개별 인식 방식은 유지됩니다.",12,muted);
        text(f,"화면 정보는 앱이 제공하는 글자를 읽습니다. 이미지 속 글자는 한글 OCR을 선택하세요.",12,muted);
        ArrayList<String> keys=new ArrayList<>(),names=new ArrayList<>();keys.add("");names.add("현재 매크로 · "+macro.name);
        for(String name:Store.library(this).keySet()){keys.add(name);names.add(name);}
        if(!p.macroName.isEmpty()&&!keys.contains(p.macroName)){keys.add(p.macroName);names.add(p.macroName+" · 저장본 없음");}
        Spinner macroChoice=spinner(f,"탐지되면 실행할 매크로",names.toArray(new String[0]),keys.indexOf(p.macroName));
        EditText repetitions=field(f,"들어간 뒤 매크로 반복 · 회",Integer.toString(p.repetitions),true);
        EditText seconds=field(f,"새로고침 간격 · 초",Integer.toString(p.intervalSeconds),true);
        EditText cooldown=timeField(f,"완료 후 첫 새로고침 대기 · 초, 600은 10분",p.cooldownMs);
        CheckBox tap=check(f,"찾은 글자를 눌러 들어가기",p.tapFound);
        text(f,"반복 횟수는 여기서 한 번만 정합니다. 저장한 매크로의 전체 반복 설정과 곱하지 않습니다.",12,muted);
        LinearLayout timing=fold(f,"동작 사이 대기");
        EditText startWait=timeField(timing,"처음 시작 전 대기 · 초",p.startMs),entry=timeField(timing,"들어간 뒤 화면 대기 · 초",p.entryMs),repeatGap=timeField(timing,"매크로 한 회 사이 대기 · 초",p.repeatGapMs),beforeBack=timeField(timing,"모두 완료 후 뒤로가기 전 대기 · 초",p.beforeBackMs),backWait=timeField(timing,"뒤로가기 후 화면 대기 · 초",p.returnMs),settle=timeField(timing,"새로고침 후 화면 대기 · 초",p.settleMs);
        LinearLayout advanced=fold(f,"세부 설정");
        CheckBox immediateEntry=check(advanced,"인식 즉시 들어가기",p.rule.immediate);
        text(advanced,"켜면 터치 전 재확인을 생략합니다. 화면이 빠르게 바뀌면 끄세요.",12,muted);
        Spinner direction=spinner(advanced,"새로고침 동작",new String[]{"화면을 아래로 당기기","목록 아래로 이동하기"},p.direction.equals("PULL_DOWN")?0:1);
        CheckBox waitRefresh=check(advanced,"완료 후 새로고침부터 다시 시작",p.waitForRefresh);text(advanced,"켜면 뒤로간 뒤 설정한 초만큼 기다려 새로고침합니다. 끄면 바로 다시 탐지합니다.",12,muted);
        CheckBox duplicate=check(advanced,"같은 글자가 사라진 뒤에만 다시 실행",p.preventDuplicate);text(advanced,"켜면 새로고침 전 재실행도 막습니다. 완료한 글자가 계속 보이면 새로고침 후에도 건너뜁니다.",12,muted);
        EditText rearm=timeField(advanced,"글자 사라짐 확인 시간 · 초, 최소 1",p.rearmMs);
        Spinner match=spinner(advanced,"글자 비교",new String[]{"완전 일치 · 공백/대소문자 구분"},0);
        EditText left=field(advanced,"영역 왼쪽 %",Double.toString(p.rule.left),true),top=field(advanced,"영역 위 %",Double.toString(p.rule.top),true),right=field(advanced,"영역 오른쪽 %",Double.toString(p.rule.right),true),bottom=field(advanced,"영역 아래 %",Double.toString(p.rule.bottom),true);
        EditText count=field(advanced,"뒤로가기 횟수",Integer.toString(p.backCount),true),limit=field(advanced,"전체 제한 · 분, 0은 제한 없음",Integer.toString(p.maxMinutes),true);
        text(advanced,"저장한 매크로를 첫 동작부터 지정 횟수만큼 실행합니다. 각 동작의 개별 반복은 그대로 적용됩니다. 매크로 실행 제한이 없으면 한 번 들어가서 최대 30분까지 실행합니다.",12,muted);
        AlertDialog d=new AlertDialog.Builder(this).setIcon(R.drawable.ic_macro).setTitle("글자 · 매크로 설정").setView(wrap(f)).setPositiveButton("저장",null).setNegativeButton("취소",null).create();
        d.setOnShowListener(v->{styleDialog(d);d.getButton(-1).setOnClickListener(b->{try{
            p.rule.text=word.getText().toString().trim();p.macroName=keys.get(macroChoice.getSelectedItemPosition());p.repetitions=number(repetitions);p.intervalSeconds=number(seconds);p.direction=direction.getSelectedItemPosition()==0?"PULL_DOWN":"SCROLL_DOWN";
            p.rule.source=new String[]{"AUTO","ACCESS","OCR"}[source.getSelectedItemPosition()];p.rule.match="EXACT";p.rule.ignoreSpaces=false;p.rule.ignoreCase=false;p.rule.immediate=immediateEntry.isChecked();
            p.rule.left=decimal(left);p.rule.top=decimal(top);p.rule.right=decimal(right);p.rule.bottom=decimal(bottom);p.backCount=number(count);p.entryMs=timeValue(entry);p.settleMs=timeValue(settle);p.returnMs=timeValue(backWait);p.maxMinutes=number(limit);p.tapFound=tap.isChecked();p.waitForRefresh=waitRefresh.isChecked();
            p.preventDuplicate=duplicate.isChecked();p.startMs=timeValue(startWait);p.repeatGapMs=timeValue(repeatGap);p.beforeBackMs=timeValue(beforeBack);p.rearmMs=timeValue(rearm);p.cooldownMs=timeValue(cooldown);
            WatchStore.save(this,p);d.dismiss();render();
        }catch(Exception e){toast(e instanceof NumberFormatException?"숫자를 정확히 입력하세요":e.getMessage());}});});d.show();
    }
    private void space(LinearLayout box,int height){box.addView(new View(this),new LinearLayout.LayoutParams(1,dp(height)));}
    private LinearLayout fold(LinearLayout parent,String title){
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setVisibility(View.GONE);
        Button toggle=quiet(parent,getString(R.string.pxe_section_show,title),()->{});toggle.setOnClickListener(v->{boolean open=body.getVisibility()!=View.VISIBLE;body.setVisibility(open?View.VISIBLE:View.GONE);toggle.setText(getString(open?R.string.pxe_section_hide:R.string.pxe_section_show,title));});parent.addView(body);return body;
    }
    private Button primary(LinearLayout box,String label,Runnable action){Button b=button(box,label,action);b.setTextSize(16);b.setTextColor(Ui.onAccent(this));b.setBackground(Ui.ripple(this,blue,14));b.setMinHeight(dp(52));b.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));return b;}
    private Button quiet(LinearLayout box,String label,Runnable action){Button b=button(box,label,action);b.setBackground(Ui.ripple(this,Color.TRANSPARENT,10));b.setTextColor(muted);return b;}
    private void quietHalf(LinearLayout box,String label,Runnable action){Button b=button(box,label,action);b.setTextColor(ink);b.setTextSize(14);b.setBackground(Ui.ripple(this,Color.TRANSPARENT,10));b.setLayoutParams(actionSize(box));}
    private void quietHalfFixed(LinearLayout box,String label,Runnable action){Button b=quiet(box,label,action);b.setTextSize(14);b.setLayoutParams(new LinearLayout.LayoutParams(dp(88),-2));}
    private void settingsAction(LinearLayout box,String label,Runnable action){actionRow(box,label.equals("인식 테스트")?R.drawable.pxe_eye:label.equals("사용법")?R.drawable.pxe_help:label.equals("오류 기록")?R.drawable.pxe_warning:R.drawable.pxe_steps,label,"",false,action);}
    private void settingRow(LinearLayout box,String title,String value,Runnable action){actionRow(box,title.equals("접근성")?R.drawable.pxe_accessibility:title.equals("화면 모드")?R.drawable.pxe_sun:R.drawable.pxe_settings,title,value,false,action);}
    private void recognitionSettings(){
        LinearLayout f=form();Spinner source=spinner(f,"인식 방식",Ui.recognitionNames(),selected(new String[]{"AUTO","ACCESS","OCR"},Ui.prefs(this).getString("word_source","AUTO")));
        text(f,"글자 탭의 단독 실행과 글자 탭에서 추가하는 동작에 적용합니다. 이미 저장한 동작과 목록 탐지 설정은 유지됩니다.",12,muted);
        Spinner match=spinner(f,"비교 방식",new String[]{"완전 일치 · 공백/대소문자 구분"},0);
        text(f,"뒤로가기 재검색은 최대 3회입니다.\n검색 영역·터치 보정은 매크로 동작을 추가한 뒤 편집할 수 있습니다.",12,muted);
        show(new AlertDialog.Builder(this).setTitle("인식 설정").setView(wrap(f)).setPositiveButton("적용",(d,w)->Ui.prefs(this).edit().putString("word_source",new String[]{"AUTO","ACCESS","OCR"}[source.getSelectedItemPosition()]).putString("word_match","EXACT").apply()).setNegativeButton("취소",null),null);
    }
    private void controlSettings(){
        LinearLayout f=form();slider(f,"크기 %",75,140,"scale",100);slider(f,"컨트롤 불투명도 %",40,100,"opacity",95);slider(f,"하단 위젯 불투명도 %",40,100,"hud_opacity",95);
        CheckBox dots=check(f,"터치 위치 번호 표시",Ui.prefs(this).getBoolean("markers",true));dots.setOnCheckedChangeListener((v,on)->{Ui.prefs(this).edit().putBoolean("markers",on).apply();refreshFloating();});
        CheckBox awake=check(f,"실행 중 화면 켜짐 유지",Ui.prefs(this).getBoolean("keep_screen",true));awake.setOnCheckedChangeListener((v,on)->Ui.prefs(this).edit().putBoolean("keep_screen",on).apply());
        show(new AlertDialog.Builder(this).setTitle("화면 위 컨트롤").setView(wrap(f)).setPositiveButton("완료",null),null);
    }
    private void macroMenu(){show(new AlertDialog.Builder(this).setTitle("매크로").setItems(new String[]{"실행 설정","좌표 선택","이름으로 저장","저장한 매크로","새 매크로","파일 내보내기","파일 불러오기","정지"},(d,w)->{switch(w){case 0:settings();break;case 1:pick();break;case 2:namedSave();break;case 3:tab=2;render();break;case 4:namedSaveOrNew();break;case 5:exportMacro();break;case 6:importMacro();break;case 7:if(MacroService.get()!=null){MacroService.get().stop();if(MacroService.get().controller().recording)MacroService.get().controller().endRecording();}break;}}),null);}
    private void namedSaveOrNew(){show(new AlertDialog.Builder(this).setMessage("새 작업을 만들면 현재 편집본이 바뀝니다. 보관하려면 먼저 이름으로 저장하세요.").setPositiveButton("새로 만들기",(d,w)->{macro=new Macro();save();tab=1;render();}).setNeutralButton("이름으로 저장",(d,w)->namedSave()).setNegativeButton("취소",null),null);}
    private void refreshFloating(){if(MacroService.get()!=null)MacroService.get().controller().refresh();}
    private void themeChoice(){show(new AlertDialog.Builder(this).setTitle("라이트 / 다크").setSingleChoiceItems(new String[]{"기기 설정 따르기","라이트","다크"},Ui.prefs(this).getInt("theme",0),(d,w)->{if(!Ui.prefs(this).edit().putInt("theme",w).commit()){toast("테마 저장에 실패했습니다");return;}save();d.dismiss();refreshFloating();recreate();}).setNegativeButton("취소",null),null);}
    private void slider(LinearLayout box,String label,int min,int max,String key,int def){TextView value=text(box,label+" · "+Ui.prefs(this).getInt(key,def),12,muted);SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(Ui.prefs(this).getInt(key,def)-min);box.addView(bar);bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){refreshFloating();}public void onProgressChanged(SeekBar s,int n,boolean user){if(user){Ui.prefs(MainActivity.this).edit().putInt(key,n+min).apply();value.setText(getString(R.string.nex_slider,label,n+min));}}});}
    private void record(){if(service()){save();if(MacroService.get().running){toast("먼저 실행을 정지하세요");return;}moveTaskToBack(true);MacroService.get().controller().beginRecording(false,-1);}}
    private void quickWord(boolean preview){if(!service())return;try{Step s=FloatingController.wordStep(this);if(preview){s.type="TEXT_CHECK";s.failure="STOP";}Macro m=new Macro();m.name="PXE 글자 모드";m.startDelay=3000;m.maxMinutes=3;m.steps.add(s);m.validate();MacroService.get().restartWord(m);moveTaskToBack(true);}catch(Exception e){toast(e.getMessage());}}
    private void refreshList(){
        list.removeAllViews();if(macro.steps.isEmpty()){space(list,16);text(list,"동작을 추가하거나 화면을 녹화하세요.",14,muted);return;}
        for(int i=0;i<macro.steps.size();i++){
            final int index=i;Step step=macro.steps.get(i);LinearLayout item=card(list);LinearLayout heading=row(item);heading.setGravity(Gravity.CENTER_VERTICAL);
            TextView name=text(heading,String.format(Locale.KOREA,"%02d",i+1)+" · "+step.name()+(step.enabled?"":" · 꺼짐"),15,step.enabled?ink:muted);name.setMinLines(2);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
            Button more=Ui.button(this,"더보기");more.setTextSize(12);more.setPadding(dp(4),0,dp(4),0);more.setSingleLine(true);more.setBackground(Ui.ripple(this,Color.TRANSPARENT,10));more.setContentDescription((i+1)+"번 동작 더보기");LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(dp(64),dp(48));size.setMargins(dp(8),0,0,0);heading.addView(more,size);more.setOnClickListener(v->stepMenu(index));
            TextView description=text(item,step.detail(),13,muted);description.setPadding(0,dp(4),0,0);description.setMinLines(2);description.setMaxLines(2);description.setEllipsize(android.text.TextUtils.TruncateAt.END);
            item.setBackground(Ui.outlinedRipple(this,Ui.card(this),12));item.setMinimumHeight(dp(112));item.setFocusable(true);item.setContentDescription((i+1)+"번 동작 · "+step.name()+" · "+step.detail()+" · 눌러 편집");item.setForeground(Ui.focusRing(this));item.setOnClickListener(v->edit(step));font(item);
        }
    }
    private void small(LinearLayout r,String label,Runnable click){Button b=button(r,label,click);b.setTextSize(11);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(0,0,0,0);b.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));}
    private void stepMenu(int i){Step s=macro.steps.get(i);show(new AlertDialog.Builder(this).setTitle((i+1)+"번 동작").setItems(new String[]{"편집","위로 이동","아래로 이동","복제",s.enabled?"사용 안 함으로":"사용함으로","삭제"},(d,w)->{if(w==0){edit(s);return;}if(w==1&&i>0)Collections.swap(macro.steps,i,i-1);if(w==2&&i<macro.steps.size()-1)Collections.swap(macro.steps,i,i+1);if(w==3)macro.steps.add(i+1,new Step(s.json()));if(w==4)s.enabled=!s.enabled;if(w==5)macro.steps.remove(i);save();render();}),null);}
    private boolean service(){if(MacroService.get()!=null)return true;accessibility();return false;}
    private void accessibility(){
        show(new AlertDialog.Builder(this).setTitle("접근성 사용 안내").setMessage("터치와 스와이프 실행, 다른 앱의 화면 글자 읽기, 화면 캡처를 통한 OCR, 뒤로가기에 접근성 권한을 사용합니다.\n\nOCR 모델은 APK에 포함되어 있습니다. 화면이나 입력 내용은 서버로 전송하지 않습니다. 실행은 직접 만든 매크로로만 시작되며, 컨트롤 또는 알림의 정지 버튼으로 중단할 수 있습니다.\n\n설정에서 ‘설치된 서비스 → PXE’를 켜주세요.")
            .setPositiveButton("설정 열기",(d,w)->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).setNegativeButton("취소",null),null);
    }
    private void start(){if(!service())return;try{macro.validate();save();MacroService.get().showControls();MacroService.get().restart(macro);moveTaskToBack(true);}catch(Exception e){toast(e.getMessage());}}
    private void pick(){if(service()){if(MacroService.get().running){toast("먼저 매크로를 정지하세요");return;}save();moveTaskToBack(true);MacroService.get().pickPoint();}}
    private void chooseStep(){show(new AlertDialog.Builder(this).setTitle("추가할 동작").setItems(Step.NAMES,(d,w)->{if(macro.steps.size()>=500){toast("최대 500개입니다");return;}if(Step.TYPES[w].equals("GESTURE")){if(service()){save();moveTaskToBack(true);MacroService.get().controller().beginRecording(true,-1);}return;}Step s=new Step(Step.TYPES[w]);macro.steps.add(s);save();render();edit(s);}),null);}
    private LinearLayout form(){LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(dp(20),dp(8),dp(20),dp(12));return f;}
    private ScrollView wrap(LinearLayout f){ScrollView s=new ScrollView(this);s.setClipToPadding(false);s.setPadding(0,0,0,dp(8));s.addView(f);return s;}
    private EditText field(LinearLayout f,String label,String value,boolean numeric){
        TextView caption=text(f,label,13,muted);caption.setPadding(0,dp(12),0,dp(6));EditText e=new EditText(this);e.setId(View.generateViewId());caption.setLabelFor(e.getId());e.setText(value);e.setTextSize(15);e.setTextColor(ink);e.setHintTextColor(muted);e.setSingleLine(!label.contains("여러 줄"));e.setSelectAllOnFocus(true);
        if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);LinearLayout.LayoutParams inputSize=new LinearLayout.LayoutParams(-1,-2);inputSize.setMargins(0,0,0,dp(8));f.addView(e,inputSize);return e;
    }
    private Spinner spinner(LinearLayout f,String label,String[] labels,int selected){
        TextView caption=text(f,label,13,muted);caption.setPadding(0,dp(12),0,dp(6));Spinner s=new Spinner(this);s.setId(View.generateViewId());caption.setLabelFor(s.getId());ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,labels){@Override public View getView(int p,View v,ViewGroup parent){View out=super.getView(p,v,parent);font(out);((TextView)out).setTextColor(ink);((TextView)out).setTextSize(14);out.setMinimumHeight(dp(48));((TextView)out).setSingleLine(false);((TextView)out).setMaxLines(Integer.MAX_VALUE);out.setPadding(dp(12),dp(8),dp(12),dp(8));return out;}@Override public View getDropDownView(int p,View v,ViewGroup parent){View out=super.getDropDownView(p,v,parent);font(out);((TextView)out).setTextColor(ink);((TextView)out).setTextSize(14);out.setMinimumHeight(dp(48));out.setBackgroundColor(Ui.card(MainActivity.this));((TextView)out).setSingleLine(false);((TextView)out).setMaxLines(Integer.MAX_VALUE);out.setPadding(dp(12),dp(12),dp(12),dp(12));return out;}};a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setMinimumHeight(dp(48));s.setAdapter(a);s.setSelection(Math.max(0,selected));LinearLayout.LayoutParams selectionSize=new LinearLayout.LayoutParams(-1,-2);selectionSize.setMargins(0,0,0,dp(8));f.addView(s,selectionSize);return s;
    }
    private CheckBox check(LinearLayout f,String label,boolean value){CheckBox c=new CheckBox(this);c.setText(label);c.setTextSize(14);c.setTextColor(ink);c.setLineSpacing(dp(2),1f);c.setCompoundDrawablePadding(dp(8));c.setPadding(0,dp(4),0,dp(4));c.setChecked(value);f.addView(c);return c;}
    private int selected(String[] values,String value){for(int i=0;i<values.length;i++)if(values[i].equals(value))return i;return 0;}
    private int number(EditText e){return Integer.parseInt(e.getText().toString().trim());}
    private EditText timeField(LinearLayout f,String label,int millis){return field(f,label,Times.seconds(millis),true);}
    private int timeValue(EditText e){return Times.millis(e.getText().toString());}
    private double decimal(EditText e){return Double.parseDouble(e.getText().toString().trim());}
    private void styleDialog(AlertDialog dialog){Ui.styleDialog(dialog);}
    private void show(AlertDialog.Builder b,Runnable ready){AlertDialog d=b.create();d.setOnShowListener(x->{styleDialog(d);if(ready!=null)ready.run();});d.show();}
    private void edit(Step original){
        Step s=new Step(original.json());LinearLayout f=form();text(f,s.name(),21,ink);LinearLayout advanced=new LinearLayout(this);advanced.setOrientation(LinearLayout.VERTICAL);advanced.setVisibility(View.GONE);
        HashMap<String,EditText> fields=new HashMap<>();
        if(Arrays.asList("TAP","DOUBLE","LONG","SWIPE","CURVE","PINCH_IN","PINCH_OUT").contains(s.type)){
            text(f,"화면 전체를 100%로 계산합니다. 같은 화면 방향에서 사용하세요.",12,muted);
            fields.put("x",field(f,"X 위치 % · 0~100",Double.toString(s.x),true));fields.put("y",field(f,"Y 위치 % · 0~100",Double.toString(s.y),true));
            if(s.type.equals("SWIPE")||s.type.equals("CURVE")){fields.put("x2",field(f,"끝 X 위치 %",Double.toString(s.x2),true));fields.put("y2",field(f,"끝 Y 위치 %",Double.toString(s.y2),true));}
        }
        if(Arrays.asList("TAP","LONG","SWIPE","WAIT","CURVE","PINCH_IN","PINCH_OUT","MULTI_TAP").contains(s.type))fields.put("duration",timeField(f,"동작 시간 · 초",s.duration));
        if(s.type.equals("CURVE")){fields.put("cx",field(f,"곡선 제어점 X %",Double.toString(s.cx),true));fields.put("cy",field(f,"곡선 제어점 Y %",Double.toString(s.cy),true));}
        if(s.type.startsWith("PINCH")){fields.put("x2",field(f,"좁은 간격 · 화면 너비 %",Double.toString(s.x2),true));fields.put("y2",field(f,"넓은 간격 · 화면 너비 %",Double.toString(s.y2),true));}
        if(s.type.equals("MULTI_TAP"))fields.put("text",field(f,"동시 터치 좌표 · 25,50;75,50",s.text,false));
        if(s.type.equals("GESTURE")){text(f,s.strokes.length()+"개의 손가락 경로가 기록되어 있습니다.",13,muted);button(f,"이 동작을 다시 녹화",()->{if(service()){save();moveTaskToBack(true);MacroService.get().controller().beginRecording(true,macro.steps.indexOf(original));}});}
        Spinner matching=null,source=null,failure=null;CheckBox spaces=null,casing=null,forever=null,immediate=null;
        String[] matchValues={"EXACT"},sourceValues={"AUTO","ACCESS","OCR"},failureValues={"STOP","SKIP","BACK_RETRY","RETRY","SCROLL_RETRY"};
        if(s.textAction()){
            fields.put("text",field(f,"찾을 글자",s.text,false));
            source=spinner(f,"검색 방식",Ui.recognitionNames(),selected(sourceValues,s.source));
            text(f,"이 동작에만 적용합니다. 목록 탐지와 다른 동작의 인식 방식은 유지됩니다.",12,muted);
            fields.put("timeout",timeField(f,"최대 기다릴 시간 · 초, 0은 한 번 확인",s.timeout));
            if(s.canWaitForever()){
                forever=check(f,"글자가 나타날 때까지 계속 기다리기",s.waitForever);
                immediate=check(f,"인식 후 추가 대기 없이 다음 동작",s.immediate);
                final CheckBox nextNow=immediate;forever.setOnCheckedChangeListener((v,on)->{if(on)nextNow.setChecked(true);});
                text(f,"계속 기다리는 동안 정지할 수 있습니다. 즉시 진행은 완료 후 대기를 생략하고, 글자 터치는 터치 전 재확인도 생략합니다. 실제 인식 시간은 필요합니다.",12,muted);
            }
            matching=spinner(advanced,"글자 비교",new String[]{"완전 일치 · 공백/대소문자 구분"},0);
            spaces=check(advanced,"공백·줄바꿈 무시",false);casing=check(advanced,"영문 대소문자 무시",false);spaces.setEnabled(false);casing.setEnabled(false);text(advanced,"입력한 글자와 완전히 같을 때만 실행합니다. 포함·정규식·번역 비교는 적용하지 않습니다.",12,muted);
            fields.put("occurrence",field(advanced,"같은 글자 중 몇 번째 · 위에서 아래, 왼쪽에서 오른쪽",Integer.toString(s.occurrence),true));
            fields.put("left",field(advanced,"검색 영역 왼쪽 %",Double.toString(s.left),true));fields.put("top",field(advanced,"검색 영역 위 %",Double.toString(s.top),true));fields.put("right",field(advanced,"검색 영역 오른쪽 %",Double.toString(s.right),true));fields.put("bottom",field(advanced,"검색 영역 아래 %",Double.toString(s.bottom),true));fields.put("offsetX",field(advanced,"터치 X 보정 % · -50~50",Double.toString(s.offsetX),false));fields.put("offsetY",field(advanced,"터치 Y 보정 % · -50~50",Double.toString(s.offsetY),false));
            if(s.type.equals("TEXT_BRANCH")){
                text(advanced,"0은 다음 동작입니다. 번호는 현재 목록을 기준으로 합니다. 재정렬·삭제 후 확인하세요.",12,muted);
                fields.put("found",field(advanced,"글자 있으면 이동할 동작 번호",Integer.toString(s.found),true));fields.put("missing",field(advanced,"글자 없으면 이동할 동작 번호",Integer.toString(s.missing),true));
            }else{
                failure=spinner(advanced,"글자가 없으면",new String[]{"매크로 정지","다음 동작으로","뒤로가기 후 다시 찾기","같은 화면에서 다시 확인","위로 스크롤 후 다시 찾기"},selected(failureValues,s.failure));
                fields.put("retries",field(advanced,"최대 뒤로가기 횟수 · 0~100",Integer.toString(s.retries),true));fields.put("retryDelay",timeField(advanced,"뒤로간 뒤 화면 대기 · 초, 최소 0.3",s.retryDelay));
                text(advanced,"최대 횟수 뒤에도 글자가 없으면 중단합니다. 캡처 오류는 ‘글자 없음’으로 처리하지 않습니다.",11,muted);
            }
        }
        if(s.type.equals("INPUT"))fields.put("text",field(f,"입력할 문자 · 여러 줄 가능",s.text,false));
        if(s.type.equals("APP")){
            EditText e=field(f,"실행할 앱 패키지 이름",s.packageName,false);fields.put("packageName",e);button(f,"설치된 앱에서 선택",()->apps(e));
        }
        if(s.type.equals("GOTO")){text(f,"동작 번호는 목록을 재정렬하거나 삭제하면 달라집니다.",12,muted);fields.put("target",field(f,"이동할 동작 번호 · 1부터",Integer.toString(s.target),true));}
        if(Arrays.asList("TAP","LONG","SWIPE","CURVE","PINCH_IN","PINCH_OUT","MULTI_TAP","GESTURE","WAIT").contains(s.type)){fields.put("durationJitter",timeField(advanced,"동작 시간 무작위 범위 · ±초, 0은 사용 안 함",s.durationJitter));}fields.put("afterJitter",timeField(advanced,"완료 후 대기 무작위 범위 · ±초",s.afterJitter));
        if(!Arrays.asList("GOTO","STOP","TEXT_BRANCH").contains(s.type)){fields.put("repeat",field(advanced,"이 동작 한 세트만 반복 · 회",Integer.toString(s.repeat),true));text(advanced,"기본 1회입니다. 3으로 정하면 이 동작만 3번 하고 다음 동작으로 넘어갑니다. 전체 매크로 15회와는 별도입니다. 입력 범위는 1~1000회입니다.",12,muted);}else{text(advanced,"순서 이동과 정지는 개별 반복 대신 실행 흐름을 바꾸는 동작입니다.",12,muted);}Button detail=quiet(f,"세부 설정 보기",()->{});detail.setOnClickListener(v->{boolean open=advanced.getVisibility()!=View.VISIBLE;advanced.setVisibility(open?View.VISIBLE:View.GONE);detail.setText(open?"세부 설정 접기":"세부 설정 보기");});f.addView(advanced);fields.put("after",timeField(f,"동작 완료 후 대기 · 초",s.after));CheckBox enabled=check(f,"이 동작 사용",s.enabled);
        final Spinner fm=matching,fsrc=source,ff=failure;final CheckBox fsp=spaces,fca=casing,fforever=forever,fimmediate=immediate;
        AlertDialog d=new AlertDialog.Builder(this).setView(wrap(f)).setPositiveButton("적용",null).setNegativeButton("취소",null).create();
        d.setOnShowListener(v->{styleDialog(d);d.getButton(-1).setOnClickListener(b->{try{
            if(fields.containsKey("x")){s.x=decimal(fields.get("x"));s.y=decimal(fields.get("y"));}if(fields.containsKey("x2")){s.x2=decimal(fields.get("x2"));s.y2=decimal(fields.get("y2"));}
            if(fields.containsKey("cx")){s.cx=decimal(fields.get("cx"));s.cy=decimal(fields.get("cy"));}if(fields.containsKey("durationJitter"))s.durationJitter=timeValue(fields.get("durationJitter"));s.afterJitter=timeValue(fields.get("afterJitter"));
            if(fields.containsKey("duration"))s.duration=timeValue(fields.get("duration"));if(fields.containsKey("text"))s.text=fields.get("text").getText().toString();
            if(fm!=null){s.match=matchValues[fm.getSelectedItemPosition()];s.source=sourceValues[fsrc.getSelectedItemPosition()];s.ignoreSpaces=fsp.isChecked();s.ignoreCase=fca.isChecked();s.timeout=timeValue(fields.get("timeout"));s.occurrence=number(fields.get("occurrence"));s.left=decimal(fields.get("left"));s.top=decimal(fields.get("top"));s.right=decimal(fields.get("right"));s.bottom=decimal(fields.get("bottom"));s.offsetX=decimal(fields.get("offsetX"));s.offsetY=decimal(fields.get("offsetY"));}
            if(fforever!=null){s.waitForever=fforever.isChecked();s.immediate=fimmediate.isChecked();}
            if(ff!=null){s.failure=failureValues[ff.getSelectedItemPosition()];s.retries=number(fields.get("retries"));s.retryDelay=timeValue(fields.get("retryDelay"));}
            if(fields.containsKey("found")){s.found=number(fields.get("found"));s.missing=number(fields.get("missing"));}
            if(fields.containsKey("packageName"))s.packageName=fields.get("packageName").getText().toString().trim();if(fields.containsKey("target"))s.target=number(fields.get("target"));
            s.repeat=fields.containsKey("repeat")?number(fields.get("repeat")):1;s.after=timeValue(fields.get("after"));s.enabled=enabled.isChecked();s.validate(macro.steps.size());
            macro.steps.set(macro.steps.indexOf(original),s);save();render();d.dismiss();
        }catch(Exception e){toast(e instanceof NumberFormatException?"숫자를 정확히 입력하세요":e.getMessage());}});});d.show();
    }
    private void settings(){
        LinearLayout f=form();text(f,"실행 설정",21,ink);
        CheckBox adaptive=check(f,"다음 글자에 맞춰 진행",macro.adaptiveWait);
        text(f,"연속 글자 터치 뒤, 다음 글자가 새로 나타나면 완료 후 대기를 줄입니다. 이미 보이는 글자·수동 대기·반복 간격·무작위 대기는 유지합니다. 바로 다음 글자 터치가 같은 위치를 가리키면 사라졌다 다시 나타나야 진행합니다.",12,muted);
        EditText rounds=field(f,"전체 반복 횟수 · 0 = 무한",Integer.toString(macro.rounds),true),gap=timeField(f,"전체 반복 사이 대기 · 초",macro.gap),start=timeField(f,"시작 전 대기 · 초",macro.startDelay),max=field(f,"최대 실행 시간 분 · 0 = 제한 없음",Integer.toString(macro.maxMinutes),true);
        EditText startAt=field(f,"시작할 동작 번호",Integer.toString(macro.startAt),true);
        text(f,"화면이 꺼지거나 방향이 바뀌면 중단됩니다. 긴 실행은 화면 자동 꺼짐 시간을 직접 조정하세요.",12,muted);
        AlertDialog d=new AlertDialog.Builder(this).setView(wrap(f)).setPositiveButton("적용",null).setNegativeButton("취소",null).create();d.setOnShowListener(v->{styleDialog(d);d.getButton(-1).setOnClickListener(b->{try{int r=number(rounds),g=timeValue(gap),st=timeValue(start),mx=number(max);if(r<0||r>100000||g<0||g>600000||st<0||st>600000||mx<0||mx>1440)throw new IllegalArgumentException("범위를 확인하세요");int at=number(startAt);if(at<1||at>Math.max(1,macro.steps.size()))throw new IllegalArgumentException("시작 번호를 확인하세요");macro.startAt=at;macro.rounds=r;macro.gap=g;macro.startDelay=st;macro.maxMinutes=mx;macro.adaptiveWait=adaptive.isChecked();save();render();d.dismiss();}catch(Exception e){toast("숫자 범위를 확인하세요");}});});d.show();
    }
    private void namedSave(){
        LinearLayout f=form();EditText name=field(f,"매크로 이름",macro.name,false);AlertDialog d=new AlertDialog.Builder(this).setTitle("이름으로 저장").setView(f).setPositiveButton("저장",null).setNegativeButton("취소",null).create();d.setOnShowListener(v->{styleDialog(d);d.getButton(-1).setOnClickListener(b->{String value=name.getText().toString().trim();if(value.isEmpty()||value.length()>80){toast("이름은 1~80자입니다");return;}if(Store.library(this).containsKey(value)&&!value.equals(macro.name)){new AlertDialog.Builder(this).setMessage("같은 이름이 있습니다. 덮어쓸까요?").setPositiveButton("덮어쓰기",(a,w)->{macro.name=value;Store.put(this,macro);render();d.dismiss();}).setNegativeButton("취소",null).show();}else{macro.name=value;Store.put(this,macro);render();d.dismiss();}});});d.show();
    }
    private void library(){LinkedHashMap<String,Macro> all=Store.library(this);if(all.isEmpty()){toast("이름으로 저장한 매크로가 없습니다");return;}String[] names=all.keySet().toArray(new String[0]);show(new AlertDialog.Builder(this).setTitle("저장한 매크로").setItems(names,(d,w)->show(new AlertDialog.Builder(this).setTitle(names[w]).setItems(new String[]{"불러오기","삭제"},(a,b)->{if(b==0){macro=new Macro(all.get(names[w]).json());save();render();}else{new AlertDialog.Builder(this).setMessage("‘"+names[w]+"’ 저장본을 삭제할까요?").setPositiveButton("삭제",(x,y)->Store.remove(this,names[w])).setNegativeButton("취소",null).show();}}),null)),null);}
    private void apps(EditText input){
        Intent query=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);List<android.content.pm.ResolveInfo> result=getPackageManager().queryIntentActivities(query,0);result.sort(Comparator.comparing(r->r.loadLabel(getPackageManager()).toString()));
        String[] names=new String[result.size()];for(int i=0;i<result.size();i++)names[i]=result.get(i).loadLabel(getPackageManager())+"\n"+result.get(i).activityInfo.packageName;
        show(new AlertDialog.Builder(this).setTitle("실행할 앱").setItems(names,(d,w)->input.setText(result.get(w).activityInfo.packageName)),null);
    }
    private void exportMacro(){exportText=macro.json().toString();exportMime="application/json";Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType(exportMime).addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,macro.name.replaceAll("[\\\\/:*?\"<>|]","_")+".json");startActivityForResult(i,101);}
    private void importMacro(){startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE),102);}
    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;
        try{
            if(req==101||req==103){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IOException("파일을 열지 못했습니다");out.write(exportText.getBytes(StandardCharsets.UTF_8));}toast("저장했습니다");}
            if(req==102){try(InputStream in=getContentResolver().openInputStream(data.getData())){if(in==null)throw new IOException("파일을 열지 못했습니다");ByteArrayOutputStream buffer=new ByteArrayOutputStream();byte[] chunk=new byte[8192];int count;while((count=in.read(chunk))!=-1){buffer.write(chunk,0,count);if(buffer.size()>2*1024*1024)throw new IOException("파일이 너무 큽니다");}byte[] b=buffer.toByteArray();if(b.length>2*1024*1024)throw new IOException("파일이 너무 큽니다");Macro loaded=new Macro(new JSONObject(new String(b,StandardCharsets.UTF_8)));loaded.validate();macro=loaded;save();render();toast("불러왔습니다");}}
        }catch(Exception e){toast("파일 처리 실패 · "+e.getMessage());}
    }
    private void logs(){String log=MacroService.get()==null?getSharedPreferences("macros",0).getString("last_log","기록 없음"):MacroService.get().logText();LinearLayout f=form();text(f,log.isEmpty()?"기록 없음":log,12,ink);show(new AlertDialog.Builder(this).setTitle("최근 실행 기록").setView(wrap(f)).setPositiveButton("닫기",null).setNeutralButton("파일 저장",(d,w)->{exportText=log;startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"매크로 실행기록.txt"),103);}),null);}
    private void errorLogs(){
        try{String log=new ErrorJournal(getFilesDir()).read();LinearLayout f=form();text(f,"최근 실행 150줄과 별도로 보관합니다. 시각·오류 종류·실행 상태만 저장하며, 128KB 파일 두 개로 순환합니다.",12,muted);text(f,log.isEmpty()?"오류 기록 없음":log,12,ink);
            show(new AlertDialog.Builder(this).setTitle("지속 오류 기록").setView(wrap(f)).setPositiveButton("닫기",null).setNeutralButton("파일 저장",(d,w)->{exportText=log;startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/plain").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"PXE 오류기록.txt"),103);}),null);
        }catch(IOException e){toast("오류 기록 파일을 읽지 못했습니다 · 저장 공간을 확인하세요");}
    }
    private void help(){
      LinearLayout f=form();text(f,"PXE · 원하는 글자를 자동 터치",21,ink);
      text(f,"순서대로 설정",18,blue);text(f,"탐지 탭에서 글자 → 최대 6개 순위 영역 → 매크로 녹화 → 대기 시간 → 실행 준비를 차례대로 진행합니다. 영역을 저장하거나 녹화를 완료하면 안내로 돌아옵니다. 기존 매크로를 선택해 사용할 수도 있습니다.",14,ink);
      text(f,"순위와 새로고침",18,blue);text(f,"매번 1순위부터 6순위까지 확인하고 처음 글자가 있는 영역을 실행합니다. 1순위가 없고 2순위에 있으면 2순위를 실행합니다. 완료 후 기본 10분을 쉬고 첫 새로고침을 합니다. 그때도 같은 2순위에 글자가 있으면 다시 실행합니다. 아무 영역에도 글자가 없으면 기본 10초마다 새로고침합니다. 쉬는 시간은 분과 초로 직접 정할 수 있습니다. 쉬는 동안에는 재실행하지 않습니다.",14,ink);
      text(f,"하단 실행 표시",18,blue);text(f,"현재 순위, 매크로 반복과 동작, 대기 남은 시간, 직전 한 바퀴와 전체 경과 시간을 하단에 표시합니다. 한 바퀴는 글자를 눌러 들어간 순간부터 매크로와 뒤로가기를 마치고 목록 화면 대기를 끝낼 때까지입니다. 전체 경과 시간에는 새로고침 대기와 일시정지도 포함됩니다. 하단 표시는 터치를 통과시키고 OCR 캡처 순간에는 인식에 섞이지 않도록 잠깐 숨깁니다.",14,ink);
      text(f,"시간 입력은 초 단위",18,blue);text(f,"1은 1초, 0.5는 반 초, 1.5는 1.5초입니다. 기존 저장본과 녹화 시간도 자동으로 초 단위로 표시됩니다.",14,ink);
      text(f,"글자 탭에서 문구를 입력하세요. ‘글자 찾기 시작’을 누르면 3초 뒤 인식해 터치합니다. ‘미리보기’는 초록 테두리로 위치만 표시합니다. 다른 앱에서 쓰려면 ‘다른 앱에서 사용’을 눌러 그 앱으로 이동해 ‘글자’를 누르세요.",14,ink);
      text(f,"곡선·멀티터치 녹화",18,blue);text(f,"매크로 탭의 ‘녹화’ 또는 컨트롤 더보기의 ‘녹화’를 누릅니다. 손가락으로 탭, 곡선 스와이프, 두 손가락 확대·축소를 그리세요. 손을 떼면 원래 앱에 같은 제스처를 재생한 뒤 다음 동작을 녹화합니다. ‘녹화 끝내기’로 저장합니다. 녹화 영역에서는 제스처가 손을 뗀 뒤 전달됩니다. 동시 경로는 최대 20개, 한 제스처는 최대 59초입니다.",14,ink);
      text(f,"다른 앱 위에서 편집",18,blue);text(f,"PXE 아이콘을 누르면 바로 접고 펼칩니다. 끌면 이동하고 길게 누르면 정지합니다. 더보기에서 탐지 시작과 영역 지정, 녹화와 동작 편집을 선택합니다. 번호 점을 끌면 터치 위치가 수정됩니다. 모양은 설정 탭에서 크기·불투명도·라이트/다크를 조절하세요.",14,ink);
      text(f,"글자가 없을 때",18,blue);text(f,"글자 동작을 편집하면 정지, 건너뛰기, 뒤로가기 후 재검색, 같은 화면 재확인, 스크롤 재검색을 선택할 수 있습니다. 최대 횟수 후에도 못 찾으면 중단합니다. 검색 영역과 터치 보정도 지정할 수 있습니다. 캡처·OCR 오류는 글자 없음으로 처리하지 않습니다.",14,ink);
      text(f,"실행과 데이터",18,blue);text(f,"실행 중에는 알림의 정지 버튼도 사용할 수 있습니다. 설정의 ‘화면 켜짐 유지’는 실행 동안만 적용됩니다. 잠금 화면·화면 회전·최대 실행 시간 도달 시 중단합니다. 이미 전달된 긴 제스처는 정지 후 잠깐 마무리될 수 있습니다. 이름별 저장과 JSON 파일로 백업하세요. 앱을 삭제하면 앱 안의 저장본은 사라집니다.",13,muted);
      text(f,"한글 OCR은 기기 안에서 동작합니다. 보안 화면은 캡처가 막힐 수 있고, 폰트·화질에 따라 오인식할 수 있으므로 인식 테스트와 미리보기로 확인하세요. 문자 입력은 접근성 입력을 지원하는 입력 칸에서 가능합니다.",13,muted);show(new AlertDialog.Builder(this).setView(wrap(f)).setPositiveButton("닫기",null),null);
    }
    private void testScreen(){
        testing=true;LinearLayout box=form();box.setBackgroundColor(Ui.bg(this));content(box);
        text(box,"인식 테스트",24,ink);text(box,"아래 버튼은 화면 정보, 파란 캔버스 글자는 OCR로 테스트할 수 있습니다.",13,muted);
        TextView result=text(box,"성공 횟수: 0",20,ink);int[] taps={0};button(box,"화면 확인",()->result.setText(getString(R.string.test_taps,++taps[0])));
        View canvas=new View(this){@Override public boolean performClick(){return super.performClick();}Paint p=new Paint(3);@Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(Ui.card(MainActivity.this));p.setColor(Ui.ink(MainActivity.this));p.setTypeface(getResources().getFont(R.font.nanum_gothic_bold));p.setTextSize(dp(27));p.setTextAlign(Paint.Align.CENTER);c.drawText("테스트 확인",getWidth()/2f,getHeight()/2f+dp(9),p);}};
        box.addView(canvas,new LinearLayout.LayoutParams(-1,dp(160)));canvas.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_UP){v.performClick();result.setText(getString(R.string.ocr_taps,++taps[0]));}return true;});
        button(box,"화면 정보 테스트 실행",()->demo("ACCESS","화면 확인"));button(box,"OCR 테스트 실행",()->demo("OCR","테스트 확인"));button(box,"편집 화면으로",this::render);font(box);
    }
    private void demo(String source,String value){if(!service())return;if(MacroService.get().running){toast("먼저 정지하세요");return;}Macro m=new Macro();m.name="인식 테스트";m.startDelay=1000;Step s=new Step("TEXT_TAP");s.text=value;s.source=source;s.timeout=5000;m.steps.add(s);MacroService.get().start(m);}
    private void toast(String s){Toast.makeText(this,s==null?"설정을 확인하세요":s,Toast.LENGTH_LONG).show();}
}
