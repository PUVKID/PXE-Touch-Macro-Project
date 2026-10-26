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
    private LinearLayout page, list;private TextView state;
    private String exportText="",exportMime="application/json";
    private int blue,ink,muted;private int tab=0;
    private boolean testing=false,shownConnected=false;
    private final Handler connectionUi=new Handler(Looper.getMainLooper());
    private final Runnable watchConnection=new Runnable(){public void run(){if(!testing&&shownConnected!=(MacroService.get()!=null))render();connectionUi.postDelayed(this,800);}};
    @Override protected void onPause(){connectionUi.removeCallbacks(watchConnection);super.onPause();}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    @Override public void onCreate(Bundle b){setTheme(Ui.theme(this));super.onCreate(b);blue=Ui.accent(this);ink=Ui.ink(this);muted=Ui.muted(this);macro=Store.load(this);if(getIntent().getBooleanExtra("settings",false))tab=3;render();if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},201);}
    @Override public void onResume(){super.onResume();if(macro==null)return;if(!testing&&MacroService.get()!=null&&MacroService.get().running){MacroService.get().paused=true;MacroService.get().log("편집 화면 · 일시정지");}if(!testing)macro=Store.load(this);
        String picked=getSharedPreferences("macros",0).getString("picked","");
        if(!picked.isEmpty()){getSharedPreferences("macros",0).edit().remove("picked").apply();try{String[] xy=picked.split(",");Step s=new Step("TAP");s.x=Double.parseDouble(xy[0]);s.y=Double.parseDouble(xy[1]);macro.steps.add(s);save();testing=false;render();edit(s);}catch(Exception e){toast("좌표를 읽지 못했습니다");}}
        else if(!testing)render();connectionUi.removeCallbacks(watchConnection);connectionUi.postDelayed(watchConnection,800);
    }
    private void font(View v){if(v instanceof TextView)((TextView)v).setTypeface(getResources().getFont(R.font.nanum_gothic_bold));if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)font(((ViewGroup)v).getChildAt(i));}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);if(i.getBooleanExtra("settings",false))tab=3;macro=Store.load(this);render();}
    private void save(){Store.save(this,macro);}
    private TextView text(LinearLayout parent,String value,int sp,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(sp);t.setTextColor(color);t.setPadding(0,dp(5),0,dp(5));parent.addView(t);return t;}
    private Button button(LinearLayout box,String label,Runnable click){
        Button b=Ui.button(this,label);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.setMargins(0,dp(3),0,dp(3));box.addView(b,p);b.setOnClickListener(v->click.run());return b;
    }
    private LinearLayout row(LinearLayout box){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);box.addView(r,new LinearLayout.LayoutParams(-1,-2));return r;}
    private void halfButton(LinearLayout r,String label,Runnable click){Button b=button(r,label,click);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(52),1);p.setMargins(dp(2),dp(2),dp(2),dp(2));b.setLayoutParams(p);}
    private LinearLayout card(LinearLayout parent){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(10),dp(14),dp(10));android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(Ui.card(this));bg.setCornerRadius(dp(16));c.setBackground(bg);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(8));parent.addView(c,p);return c;
    }
    private void content(View v){v.setOnApplyWindowInsetsListener((view,insets)->{if(Build.VERSION.SDK_INT>=35){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());view.setPadding(view.getPaddingLeft(),bars.top,view.getPaddingRight(),bars.bottom);}return insets;});setContentView(v);}
    private void render(){
        testing=false;blue=Ui.accent(this);ink=Ui.ink(this);muted=Ui.muted(this);
        getWindow().setStatusBarColor(Ui.bg(this));getWindow().setNavigationBarColor(Ui.bg(this));
        LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setBackgroundColor(Ui.bg(this));
        LinearLayout header=form();header.setPadding(dp(20),dp(10),dp(20),dp(8));shell.addView(header);text(header,"NEX",30,ink);text(header,"NEXT ACTION · 2차 / 2.0",10,blue);
        LinearLayout nav=row(header);String[] labels={"글자","동작","보관","설정"};for(int i=0;i<labels.length;i++){final int chosen=i;Button btn=Ui.button(this,labels[i]);btn.setTextColor(tab==i?Color.WHITE:ink);btn.setBackground(Ui.rounded(tab==i?Color.rgb(40,86,216):Ui.soft(this),dp(10)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1);p.setMargins(dp(2),0,dp(2),0);nav.addView(btn,p);btn.setOnClickListener(v->{tab=chosen;render();});}
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);page=form();page.setPadding(dp(18),dp(4),dp(18),dp(20));scroll.addView(page);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        boolean connected=MacroService.get()!=null;shownConnected=connected;LinearLayout ready=card(page);state=text(ready,connected?"● 접근성 연결 · 다른 앱에서 실행 가능":"○ 접근성 서비스를 켜 주세요",12,connected?Color.rgb(23,166,137):blue);if(!connected)button(ready,"접근성 켜기",this::accessibility);
        if(tab==0){
            LinearLayout hero=card(page);text(hero,"원하는 글자를 찾아서 터치",21,ink);text(hero,"버튼·화면 글자·이미지 속 한글을 인식합니다.",12,muted);
            EditText word=field(hero,"찾을 글자",Ui.prefs(this).getString("word","확인"),false);word.setTextSize(22);word.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){Ui.prefs(MainActivity.this).edit().putString("word",s.toString()).apply();}public void afterTextChanged(android.text.Editable e){}});
            CheckBox back=check(hero,"없으면 뒤로가서 최대 3회 다시 확인",Ui.prefs(this).getBoolean("word_back",true));back.setOnCheckedChangeListener((v,on)->Ui.prefs(this).edit().putBoolean("word_back",on).apply());
            Spinner source=spinner(hero,"인식 방식",new String[]{"자동 · 화면 정보 + 한글 OCR","화면 정보","OCR · 이미지/게임 글자"},selected(new String[]{"AUTO","ACCESS","OCR"},Ui.prefs(this).getString("word_source","AUTO")));source.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> v){}public void onItemSelected(android.widget.AdapterView<?> v,View item,int pos,long id){Ui.prefs(MainActivity.this).edit().putString("word_source",new String[]{"AUTO","ACCESS","OCR"}[pos]).apply();}});
            Spinner matching=spinner(hero,"비교 방식",new String[]{"포함","완전 일치","정규식"},selected(new String[]{"CONTAINS","EXACT","REGEX"},Ui.prefs(this).getString("word_match","CONTAINS")));matching.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> v){}public void onItemSelected(android.widget.AdapterView<?> v,View item,int pos,long id){Ui.prefs(MainActivity.this).edit().putString("word_match",new String[]{"CONTAINS","EXACT","REGEX"}[pos]).apply();}});
            LinearLayout r=row(hero);halfButton(r,"인식 미리보기",()->quickWord(true));halfButton(r,"글자 모드 실행",()->quickWord(false));
            button(hero,"＋ 이 글자 동작을 매크로에 추가",()->{Step s=FloatingController.wordStep(this);try{s.validate(macro.steps.size()+1);macro.steps.add(s);save();tab=1;render();edit(s);}catch(Exception e){toast(e.getMessage());}});
            LinearLayout tip=card(page);text(tip,"다른 앱 위에서도 글자 모드",17,ink);text(tip,"아래 ‘화면 위 컨트롤’을 띄운 뒤 원하는 앱으로 이동하세요. 컨트롤의 ‘글자’ 버튼에서 문구를 입력하고 바로 실행할 수 있습니다. 인식 미리보기는 초록 테두리로 위치만 보여줍니다.",13,muted);
            button(tip,"글자·OCR 테스트 화면",this::testScreen);
        }else if(tab==1){
            LinearLayout config=card(page);text(config,macro.name,19,ink);text(config,macro.steps.size()+"개 동작 · "+(macro.rounds==0?"무한 반복":macro.rounds+"회 반복")+" · "+macro.startAt+"번부터 시작",12,muted);
            LinearLayout r=row(config);halfButton(r,"＋ 동작",this::chooseStep);halfButton(r,"실행 설정",this::settings);
            r=row(config);halfButton(r,"화면에서 녹화",this::record);halfButton(r,"좌표 선택",this::pick);
            list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);page.addView(list);refreshList();
            button(page,"현재 매크로 실행",this::start);
        }else if(tab==2){
            LinearLayout box=card(page);text(box,"내 매크로",22,ink);LinearLayout r=row(box);halfButton(r,"이름으로 저장",this::namedSave);halfButton(r,"새 매크로",()->{namedSaveOrNew();});
            LinkedHashMap<String,Macro> all=Store.library(this);if(all.isEmpty())text(box,"저장한 매크로가 없습니다.",13,muted);for(String name:all.keySet())button(box,name,()->{macro=new Macro(all.get(name).json());save();tab=1;render();});
            r=row(box);halfButton(r,"JSON 내보내기",this::exportMacro);halfButton(r,"JSON 불러오기",this::importMacro);button(box,"저장본 관리 / 삭제",this::library);
        }else{
            LinearLayout look=card(page);text(look,"화면과 컨트롤",22,ink);button(look,"모드 · "+(Ui.prefs(this).getInt("theme",0)==0?"기기 설정":Ui.dark(this)?"다크":"라이트"),this::themeChoice);
            slider(look,"화면 위 컨트롤 크기 %",75,140,"scale",100);slider(look,"화면 위 컨트롤 불투명도 %",40,100,"opacity",95);
            CheckBox dots=check(look,"터치 위치 번호 표시 · 최대 40개",Ui.prefs(this).getBoolean("markers",true));dots.setOnCheckedChangeListener((v,on)->{Ui.prefs(this).edit().putBoolean("markers",on).apply();refreshFloating();});
            CheckBox awake=check(look,"실행 중 화면 켜짐 유지",Ui.prefs(this).getBoolean("keep_screen",true));awake.setOnCheckedChangeListener((v,on)->Ui.prefs(this).edit().putBoolean("keep_screen",on).apply());
            LinearLayout tools=card(page);text(tools,"도움말과 실행 기록",19,ink);button(tools,"접근성 설정",this::accessibility);button(tools,"실행 기록",this::logs);button(tools,"NEX 사용법",this::help);button(tools,"인식 테스트",this::testScreen);
            text(tools,"한글 OCR·나눔고딕 Bold 내장\n화면과 입력은 기기 안에서 처리합니다.",12,muted);
        }
        LinearLayout dock=form();dock.setPadding(dp(18),dp(4),dp(18),dp(10));dock.setBackgroundColor(Ui.card(this));LinearLayout r=row(dock);halfButton(r,"화면 위 컨트롤",()->{if(service()){save();MacroService.get().showControls();moveTaskToBack(true);}});halfButton(r,"■ 정지",()->{if(MacroService.get()!=null){MacroService.get().stop();if(MacroService.get().controller().recording)MacroService.get().controller().endRecording();}toast("정지");});shell.addView(dock);content(shell);Ui.font(shell);
    }
    private void namedSaveOrNew(){show(new AlertDialog.Builder(this).setMessage("새 작업을 만들면 현재 편집본이 바뀝니다. 보관하려면 먼저 이름으로 저장하세요.").setPositiveButton("새로 만들기",(d,w)->{macro=new Macro();save();tab=1;render();}).setNeutralButton("이름으로 저장",(d,w)->namedSave()).setNegativeButton("취소",null),null);}
    private void refreshFloating(){if(MacroService.get()!=null)MacroService.get().controller().refresh();}
    private void themeChoice(){show(new AlertDialog.Builder(this).setTitle("라이트 / 다크").setSingleChoiceItems(new String[]{"기기 설정 따르기","라이트","다크"},Ui.prefs(this).getInt("theme",0),(d,w)->{if(!Ui.prefs(this).edit().putInt("theme",w).commit()){toast("테마 저장에 실패했습니다");return;}save();d.dismiss();refreshFloating();recreate();}).setNegativeButton("취소",null),null);}
    private void slider(LinearLayout box,String label,int min,int max,String key,int def){TextView value=text(box,label+" · "+Ui.prefs(this).getInt(key,def),12,muted);SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(Ui.prefs(this).getInt(key,def)-min);box.addView(bar);bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){refreshFloating();}public void onProgressChanged(SeekBar s,int n,boolean user){if(user){Ui.prefs(MainActivity.this).edit().putInt(key,n+min).apply();value.setText(getString(R.string.nex_slider,label,n+min));}}});}
    private void record(){if(service()){save();if(MacroService.get().running){toast("먼저 실행을 정지하세요");return;}moveTaskToBack(true);MacroService.get().controller().beginRecording(false,-1);}}
    private void quickWord(boolean preview){if(!service())return;try{Step s=FloatingController.wordStep(this);if(preview){s.type="TEXT_CHECK";s.failure="STOP";}Macro m=new Macro();m.name="NEX 글자 모드";m.startDelay=3000;m.maxMinutes=3;m.steps.add(s);m.validate();MacroService.get().start(m);moveTaskToBack(true);}catch(Exception e){toast(e.getMessage());}}
    private void refreshList(){
        list.removeAllViews();if(macro.steps.isEmpty()){text(list,"아직 동작이 없습니다. ‘+ 동작 추가’를 눌러 시작하세요.",14,muted);return;}
        for(int i=0;i<macro.steps.size();i++){
            final int index=i;Step s=macro.steps.get(i);LinearLayout c=card(list);TextView name=text(c,String.format(Locale.KOREA,"%02d  %s%s",i+1,s.name(),s.enabled?"":" · 사용 안 함"),16,s.enabled?ink:muted);text(c,s.detail(),12,muted);text(c,"반복 "+s.repeat+"회 · 완료 후 "+s.after+"ms",11,muted);name.setOnClickListener(v->edit(s));
            LinearLayout r=row(c);small(r,"편집",()->edit(s));small(r,"↑",()->{if(index>0){Collections.swap(macro.steps,index,index-1);save();refreshList();}});small(r,"↓",()->{if(index<macro.steps.size()-1){Collections.swap(macro.steps,index,index+1);save();refreshList();}});small(r,"더보기",()->stepMenu(index));font(c);
        }
    }
    private void small(LinearLayout r,String label,Runnable click){Button b=button(r,label,click);b.setTextSize(11);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(0,0,0,0);b.setLayoutParams(new LinearLayout.LayoutParams(0,dp(43),1));}
    private void stepMenu(int i){Step s=macro.steps.get(i);show(new AlertDialog.Builder(this).setTitle((i+1)+"번 동작").setItems(new String[]{"복제",s.enabled?"사용 안 함으로":"사용함으로","삭제"},(d,w)->{if(w==0)macro.steps.add(i+1,new Step(s.json()));if(w==1)s.enabled=!s.enabled;if(w==2)macro.steps.remove(i);save();render();}),null);}
    private boolean service(){if(MacroService.get()!=null)return true;accessibility();return false;}
    private void accessibility(){
        show(new AlertDialog.Builder(this).setTitle("접근성 사용 안내").setMessage("터치와 스와이프 실행, 다른 앱의 화면 글자 읽기, 화면 캡처를 통한 OCR, 뒤로가기에 접근성 권한을 사용합니다.\n\nOCR 모델은 APK에 포함되어 있습니다. 화면이나 입력 내용은 서버로 전송하지 않습니다. 실행은 직접 만든 매크로로만 시작되며, 컨트롤 또는 알림의 정지 버튼으로 중단할 수 있습니다.\n\n설정에서 ‘설치된 서비스 → NEX’를 켜주세요.")
            .setPositiveButton("설정 열기",(d,w)->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).setNegativeButton("취소",null),null);
    }
    private void start(){if(!service())return;try{macro.validate();save();MacroService.get().showControls();MacroService.get().start(macro);moveTaskToBack(true);}catch(Exception e){toast(e.getMessage());}}
    private void pick(){if(service()){if(MacroService.get().running){toast("먼저 매크로를 정지하세요");return;}save();moveTaskToBack(true);MacroService.get().pickPoint();}}
    private void chooseStep(){show(new AlertDialog.Builder(this).setTitle("추가할 동작").setItems(Step.NAMES,(d,w)->{if(macro.steps.size()>=500){toast("최대 500개입니다");return;}if(Step.TYPES[w].equals("GESTURE")){if(service()){save();moveTaskToBack(true);MacroService.get().controller().beginRecording(true,-1);}return;}Step s=new Step(Step.TYPES[w]);macro.steps.add(s);save();render();edit(s);}),null);}
    private LinearLayout form(){LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(dp(20),dp(8),dp(20),dp(12));return f;}
    private ScrollView wrap(LinearLayout f){ScrollView s=new ScrollView(this);s.addView(f);return s;}
    private EditText field(LinearLayout f,String label,String value,boolean numeric){
        text(f,label,12,muted);EditText e=new EditText(this);e.setText(value);e.setTextSize(15);e.setTextColor(ink);e.setHintTextColor(muted);e.setSingleLine(!label.contains("여러 줄"));e.setSelectAllOnFocus(true);
        if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);f.addView(e,new LinearLayout.LayoutParams(-1,-2));return e;
    }
    private Spinner spinner(LinearLayout f,String label,String[] labels,int selected){
        text(f,label,12,muted);Spinner s=new Spinner(this);ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,labels){@Override public View getView(int p,View v,ViewGroup parent){View out=super.getView(p,v,parent);font(out);return out;}@Override public View getDropDownView(int p,View v,ViewGroup parent){View out=super.getDropDownView(p,v,parent);font(out);return out;}};a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(a);s.setSelection(Math.max(0,selected));f.addView(s,new LinearLayout.LayoutParams(-1,dp(48)));return s;
    }
    private CheckBox check(LinearLayout f,String label,boolean value){CheckBox c=new CheckBox(this);c.setText(label);c.setChecked(value);f.addView(c);return c;}
    private int selected(String[] values,String value){for(int i=0;i<values.length;i++)if(values[i].equals(value))return i;return 0;}
    private int number(EditText e){return Integer.parseInt(e.getText().toString().trim());}
    private double decimal(EditText e){return Double.parseDouble(e.getText().toString().trim());}
    private void show(AlertDialog.Builder b,Runnable ready){AlertDialog d=b.create();d.setOnShowListener(x->{font(d.getWindow().getDecorView());if(ready!=null)ready.run();});d.show();}
    private void edit(Step original){
        Step s=new Step(original.json());LinearLayout f=form();text(f,s.name(),21,ink);
        HashMap<String,EditText> fields=new HashMap<>();
        if(Arrays.asList("TAP","DOUBLE","LONG","SWIPE","CURVE","PINCH_IN","PINCH_OUT").contains(s.type)){
            text(f,"화면 전체를 100%로 계산합니다. 같은 화면 방향에서 사용하세요.",12,muted);
            fields.put("x",field(f,"X 위치 % · 0~100",Double.toString(s.x),true));fields.put("y",field(f,"Y 위치 % · 0~100",Double.toString(s.y),true));
            if(s.type.equals("SWIPE")||s.type.equals("CURVE")){fields.put("x2",field(f,"끝 X 위치 %",Double.toString(s.x2),true));fields.put("y2",field(f,"끝 Y 위치 %",Double.toString(s.y2),true));}
        }
        if(Arrays.asList("TAP","LONG","SWIPE","WAIT","CURVE","PINCH_IN","PINCH_OUT","MULTI_TAP").contains(s.type))fields.put("duration",field(f,"동작 시간 ms · 1000 = 1초",Integer.toString(s.duration),true));
        if(s.type.equals("CURVE")){fields.put("cx",field(f,"곡선 제어점 X %",Double.toString(s.cx),true));fields.put("cy",field(f,"곡선 제어점 Y %",Double.toString(s.cy),true));}
        if(s.type.startsWith("PINCH")){fields.put("x2",field(f,"좁은 간격 · 화면 너비 %",Double.toString(s.x2),true));fields.put("y2",field(f,"넓은 간격 · 화면 너비 %",Double.toString(s.y2),true));}
        if(s.type.equals("MULTI_TAP"))fields.put("text",field(f,"동시 터치 좌표 · 25,50;75,50",s.text,false));
        if(s.type.equals("GESTURE")){text(f,s.strokes.length()+"개의 손가락 경로가 기록되어 있습니다.",13,muted);button(f,"이 동작을 다시 녹화",()->{if(service()){save();moveTaskToBack(true);MacroService.get().controller().beginRecording(true,macro.steps.indexOf(original));}});}
        Spinner matching=null,source=null,failure=null;CheckBox spaces=null,casing=null;
        String[] matchValues={"CONTAINS","EXACT","REGEX"},sourceValues={"AUTO","ACCESS","OCR"},failureValues={"STOP","SKIP","BACK_RETRY","RETRY","SCROLL_RETRY"};
        if(s.textAction()){
            fields.put("text",field(f,"찾을 글자",s.text,false));
            matching=spinner(f,"글자 비교",new String[]{"포함","완전 일치","정규식"},selected(matchValues,s.match));
            source=spinner(f,"검색 방식",new String[]{"자동 · 화면 정보 → OCR","화면 정보만 · 빠름","OCR만 · 이미지 속 글자"},selected(sourceValues,s.source));
            spaces=check(f,"공백·줄바꿈 무시",s.ignoreSpaces);casing=check(f,"영문 대소문자 무시",s.ignoreCase);
            fields.put("occurrence",field(f,"같은 글자 중 몇 번째 · 위에서 아래, 왼쪽에서 오른쪽",Integer.toString(s.occurrence),true));
            fields.put("left",field(f,"검색 영역 왼쪽 %",Double.toString(s.left),true));fields.put("top",field(f,"검색 영역 위 %",Double.toString(s.top),true));fields.put("right",field(f,"검색 영역 오른쪽 %",Double.toString(s.right),true));fields.put("bottom",field(f,"검색 영역 아래 %",Double.toString(s.bottom),true));fields.put("offsetX",field(f,"터치 X 보정 % · -50~50",Double.toString(s.offsetX),false));fields.put("offsetY",field(f,"터치 Y 보정 % · -50~50",Double.toString(s.offsetY),false));
            fields.put("timeout",field(f,"각 화면에서 기다릴 최대 시간 ms · 0 = 한 번 확인",Integer.toString(s.timeout),true));
            if(s.type.equals("TEXT_BRANCH")){
                text(f,"0은 다음 동작입니다. 번호는 현재 목록을 기준으로 합니다. 재정렬·삭제 후 확인하세요.",12,muted);
                fields.put("found",field(f,"글자 있으면 이동할 동작 번호",Integer.toString(s.found),true));fields.put("missing",field(f,"글자 없으면 이동할 동작 번호",Integer.toString(s.missing),true));
            }else{
                failure=spinner(f,"글자가 없으면",new String[]{"매크로 정지","다음 동작으로","뒤로가기 후 다시 찾기","같은 화면에서 다시 확인","위로 스크롤 후 다시 찾기"},selected(failureValues,s.failure));
                fields.put("retries",field(f,"최대 뒤로가기 횟수 · 0~100",Integer.toString(s.retries),true));fields.put("retryDelay",field(f,"뒤로간 뒤 화면 안정 대기 ms · 최소 300",Integer.toString(s.retryDelay),true));
                text(f,"최대 횟수 뒤에도 글자가 없으면 중단합니다. 캡처 오류는 ‘글자 없음’으로 처리하지 않습니다.",11,muted);
            }
        }
        if(s.type.equals("INPUT"))fields.put("text",field(f,"입력할 문자 · 여러 줄 가능",s.text,false));
        if(s.type.equals("APP")){
            EditText e=field(f,"실행할 앱 패키지 이름",s.packageName,false);fields.put("packageName",e);button(f,"설치된 앱에서 선택",()->apps(e));
        }
        if(s.type.equals("GOTO")){text(f,"동작 번호는 목록을 재정렬하거나 삭제하면 달라집니다.",12,muted);fields.put("target",field(f,"이동할 동작 번호 · 1부터",Integer.toString(s.target),true));}
        if(Arrays.asList("TAP","LONG","SWIPE","CURVE","PINCH_IN","PINCH_OUT","MULTI_TAP","GESTURE","WAIT").contains(s.type)){fields.put("durationJitter",field(f,"동작 시간 무작위 범위 ±ms · 0=사용 안 함",Integer.toString(s.durationJitter),true));}fields.put("afterJitter",field(f,"완료 후 대기 무작위 범위 ±ms",Integer.toString(s.afterJitter),true));
        fields.put("repeat",field(f,"이 동작 반복 횟수 · 1~1000",Integer.toString(s.repeat),true));fields.put("after",field(f,"동작 완료 후 대기 ms",Integer.toString(s.after),true));CheckBox enabled=check(f,"이 동작 사용",s.enabled);
        final Spinner fm=matching,fsrc=source,ff=failure;final CheckBox fsp=spaces,fca=casing;
        AlertDialog d=new AlertDialog.Builder(this).setView(wrap(f)).setPositiveButton("적용",null).setNegativeButton("취소",null).create();
        d.setOnShowListener(v->{font(d.getWindow().getDecorView());d.getButton(-1).setOnClickListener(b->{try{
            if(fields.containsKey("x")){s.x=decimal(fields.get("x"));s.y=decimal(fields.get("y"));}if(fields.containsKey("x2")){s.x2=decimal(fields.get("x2"));s.y2=decimal(fields.get("y2"));}
            if(fields.containsKey("cx")){s.cx=decimal(fields.get("cx"));s.cy=decimal(fields.get("cy"));}if(fields.containsKey("durationJitter"))s.durationJitter=number(fields.get("durationJitter"));s.afterJitter=number(fields.get("afterJitter"));
            if(fields.containsKey("duration"))s.duration=number(fields.get("duration"));if(fields.containsKey("text"))s.text=fields.get("text").getText().toString();
            if(fm!=null){s.match=matchValues[fm.getSelectedItemPosition()];s.source=sourceValues[fsrc.getSelectedItemPosition()];s.ignoreSpaces=fsp.isChecked();s.ignoreCase=fca.isChecked();s.timeout=number(fields.get("timeout"));s.occurrence=number(fields.get("occurrence"));s.left=decimal(fields.get("left"));s.top=decimal(fields.get("top"));s.right=decimal(fields.get("right"));s.bottom=decimal(fields.get("bottom"));s.offsetX=decimal(fields.get("offsetX"));s.offsetY=decimal(fields.get("offsetY"));}
            if(ff!=null){s.failure=failureValues[ff.getSelectedItemPosition()];s.retries=number(fields.get("retries"));s.retryDelay=number(fields.get("retryDelay"));}
            if(fields.containsKey("found")){s.found=number(fields.get("found"));s.missing=number(fields.get("missing"));}
            if(fields.containsKey("packageName"))s.packageName=fields.get("packageName").getText().toString().trim();if(fields.containsKey("target"))s.target=number(fields.get("target"));
            s.repeat=number(fields.get("repeat"));s.after=number(fields.get("after"));s.enabled=enabled.isChecked();s.validate(macro.steps.size());
            macro.steps.set(macro.steps.indexOf(original),s);save();render();d.dismiss();
        }catch(Exception e){toast(e instanceof NumberFormatException?"숫자를 정확히 입력하세요":e.getMessage());}});});d.show();
    }
    private void settings(){
        LinearLayout f=form();text(f,"실행 설정",21,ink);
        EditText rounds=field(f,"전체 반복 횟수 · 0 = 무한",Integer.toString(macro.rounds),true),gap=field(f,"전체 반복 사이 대기 ms",Integer.toString(macro.gap),true),start=field(f,"시작 전 대기 ms",Integer.toString(macro.startDelay),true),max=field(f,"최대 실행 시간 분 · 0 = 제한 없음",Integer.toString(macro.maxMinutes),true);
        EditText startAt=field(f,"시작할 동작 번호",Integer.toString(macro.startAt),true);
        text(f,"화면이 꺼지거나 방향이 바뀌면 중단됩니다. 긴 실행은 화면 자동 꺼짐 시간을 직접 조정하세요.",12,muted);
        AlertDialog d=new AlertDialog.Builder(this).setView(wrap(f)).setPositiveButton("적용",null).setNegativeButton("취소",null).create();d.setOnShowListener(v->{font(d.getWindow().getDecorView());d.getButton(-1).setOnClickListener(b->{try{int r=number(rounds),g=number(gap),st=number(start),mx=number(max);if(r<0||r>100000||g<0||g>600000||st<0||st>600000||mx<0||mx>1440)throw new IllegalArgumentException("범위를 확인하세요");int at=number(startAt);if(at<1||at>Math.max(1,macro.steps.size()))throw new IllegalArgumentException("시작 번호를 확인하세요");macro.startAt=at;macro.rounds=r;macro.gap=g;macro.startDelay=st;macro.maxMinutes=mx;save();render();d.dismiss();}catch(Exception e){toast("숫자 범위를 확인하세요");}});});d.show();
    }
    private void namedSave(){
        LinearLayout f=form();EditText name=field(f,"매크로 이름",macro.name,false);AlertDialog d=new AlertDialog.Builder(this).setTitle("이름으로 저장").setView(f).setPositiveButton("저장",null).setNegativeButton("취소",null).create();d.setOnShowListener(v->{font(d.getWindow().getDecorView());d.getButton(-1).setOnClickListener(b->{String value=name.getText().toString().trim();if(value.isEmpty()||value.length()>80){toast("이름은 1~80자입니다");return;}if(Store.library(this).containsKey(value)&&!value.equals(macro.name)){new AlertDialog.Builder(this).setMessage("같은 이름이 있습니다. 덮어쓸까요?").setPositiveButton("덮어쓰기",(a,w)->{macro.name=value;Store.put(this,macro);render();d.dismiss();}).setNegativeButton("취소",null).show();}else{macro.name=value;Store.put(this,macro);render();d.dismiss();}});});d.show();
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
    private void help(){
      LinearLayout f=form();text(f,"NEX · 원하는 글자를 자동 터치",21,ink);
      text(f,"글자 탭에서 문구를 입력하세요. ‘글자 모드 실행’을 누르면 3초 뒤 인식해 터치합니다. ‘인식 미리보기’는 초록 테두리로 위치만 표시합니다. 다른 앱에서 쓰려면 ‘화면 위 컨트롤’을 띄우고 그 앱으로 이동해 ‘글자’를 누르세요.",14,ink);
      text(f,"곡선·멀티터치 녹화",18,blue);text(f,"동작 탭의 ‘화면에서 녹화’ 또는 컨트롤의 ‘녹화’를 누릅니다. 손가락으로 탭, 곡선 스와이프, 두 손가락 확대·축소를 그리세요. 손을 떼면 원래 앱에 같은 제스처를 재생한 뒤 다음 동작을 녹화합니다. ‘녹화 끝내기’로 저장합니다. 녹화 영역에서는 제스처가 손을 뗀 뒤 전달됩니다. 동시 경로는 최대 20개, 한 제스처는 최대 59초입니다.",14,ink);
      text(f,"다른 앱 위에서 편집",18,blue);text(f,"컨트롤을 끌어 이동하고 ‘접기’로 작게 줄일 수 있습니다. 번호 점을 끌면 터치 위치가 수정됩니다. ‘동작 목록’에서 순서·복제·사용 여부·삭제·시작 번호를 바꿉니다. 글자 동작 추가와 저장도 가능합니다. 모양은 설정 탭에서 크기·불투명도·라이트/다크를 조절하세요.",14,ink);
      text(f,"글자가 없을 때",18,blue);text(f,"글자 동작을 편집하면 정지, 건너뛰기, 뒤로가기 후 재검색, 같은 화면 재확인, 스크롤 재검색을 선택할 수 있습니다. 최대 횟수 후에도 못 찾으면 중단합니다. 검색 영역과 터치 보정도 지정할 수 있습니다. 캡처·OCR 오류는 글자 없음으로 처리하지 않습니다.",14,ink);
      text(f,"실행과 데이터",18,blue);text(f,"실행 중에는 알림의 정지 버튼도 사용할 수 있습니다. 설정의 ‘화면 켜짐 유지’는 실행 동안만 적용됩니다. 잠금 화면·화면 회전·최대 실행 시간 도달 시 중단합니다. 이미 전달된 긴 제스처는 정지 후 잠깐 마무리될 수 있습니다. 이름별 저장과 JSON 파일로 백업하세요. 앱을 삭제하면 앱 안의 저장본은 사라집니다.",13,muted);
      text(f,"한글 OCR은 기기 안에서 동작합니다. 보안 화면은 캡처가 막힐 수 있고, 폰트·화질에 따라 오인식할 수 있으므로 인식 테스트와 미리보기로 확인하세요. 문자 입력은 접근성 입력을 지원하는 입력 칸에서 가능합니다.",13,muted);show(new AlertDialog.Builder(this).setView(wrap(f)).setPositiveButton("닫기",null),null);
    }
    private void testScreen(){
        testing=true;LinearLayout box=form();box.setBackgroundColor(Ui.bg(this));content(box);
        text(box,"인식 테스트",24,ink);text(box,"아래 버튼은 화면 정보, 파란 캔버스 글자는 OCR로 테스트할 수 있습니다.",13,muted);
        TextView result=text(box,"성공 횟수: 0",20,ink);int[] taps={0};button(box,"화면 확인",()->result.setText(getString(R.string.test_taps,++taps[0])));
        View canvas=new View(this){@Override public boolean performClick(){return super.performClick();}Paint p=new Paint(3);@Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(Color.rgb(232,240,255));p.setColor(Color.rgb(20,40,85));p.setTypeface(getResources().getFont(R.font.nanum_gothic_bold));p.setTextSize(dp(27));p.setTextAlign(Paint.Align.CENTER);c.drawText("테스트 확인",getWidth()/2f,getHeight()/2f+dp(9),p);}};
        box.addView(canvas,new LinearLayout.LayoutParams(-1,dp(160)));canvas.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_UP){v.performClick();result.setText(getString(R.string.ocr_taps,++taps[0]));}return true;});
        button(box,"화면 정보 테스트 실행",()->demo("ACCESS","화면 확인"));button(box,"OCR 테스트 실행",()->demo("OCR","테스트 확인"));button(box,"편집 화면으로",this::render);font(box);
    }
    private void demo(String source,String value){if(!service())return;if(MacroService.get().running){toast("먼저 정지하세요");return;}Macro m=new Macro();m.name="인식 테스트";m.startDelay=1000;Step s=new Step("TEXT_TAP");s.text=value;s.source=source;s.timeout=5000;m.steps.add(s);MacroService.get().start(m);}
    private void toast(String s){Toast.makeText(this,s==null?"설정을 확인하세요":s,Toast.LENGTH_LONG).show();}
}
