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
    private final int blue=Color.rgb(40,86,216),ink=Color.rgb(23,39,71),muted=Color.rgb(92,108,133);
    private boolean testing=false;
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    @Override public void onCreate(Bundle b){super.onCreate(b);macro=Store.load(this);render();if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},201);}
    @Override public void onResume(){super.onResume();if(macro==null)return;
        String picked=getSharedPreferences("macros",0).getString("picked","");
        if(!picked.isEmpty()){getSharedPreferences("macros",0).edit().remove("picked").apply();try{String[] xy=picked.split(",");Step s=new Step("TAP");s.x=Double.parseDouble(xy[0]);s.y=Double.parseDouble(xy[1]);macro.steps.add(s);save();testing=false;render();edit(s);}catch(Exception e){toast("좌표를 읽지 못했습니다");}}
        else if(!testing)render();
    }
    private void font(View v){if(v instanceof TextView)((TextView)v).setTypeface(getResources().getFont(R.font.nanum_gothic_bold));if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)font(((ViewGroup)v).getChildAt(i));}
    private void save(){Store.save(this,macro);}
    private TextView text(LinearLayout parent,String value,int sp,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(sp);t.setTextColor(color);t.setPadding(0,dp(5),0,dp(5));parent.addView(t);return t;}
    private Button button(LinearLayout box,String label,Runnable click){
        Button b=new Button(this);b.setText(label);b.setTextSize(13);b.setAllCaps(false);b.setTextColor(blue);b.setMinHeight(dp(46));box.addView(b,new LinearLayout.LayoutParams(-1,-2));b.setOnClickListener(v->click.run());return b;
    }
    private LinearLayout row(LinearLayout box){LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);box.addView(r,new LinearLayout.LayoutParams(-1,-2));return r;}
    private void halfButton(LinearLayout r,String label,Runnable click){Button b=button(r,label,click);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(52),1);p.setMargins(dp(2),dp(2),dp(2),dp(2));b.setLayoutParams(p);}
    private LinearLayout card(LinearLayout parent){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(14),dp(10),dp(14),dp(10));android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(16));c.setBackground(bg);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(6),0,dp(8));parent.addView(c,p);return c;
    }
    private void content(View v){v.setOnApplyWindowInsetsListener((view,insets)->{if(Build.VERSION.SDK_INT>=35){android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());view.setPadding(view.getPaddingLeft(),bars.top,view.getPaddingRight(),bars.bottom);}return insets;});setContentView(v);}
    private void render(){
        testing=false;ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(243,246,251));page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(18),dp(18),dp(18),dp(28));scroll.addView(page);content(scroll);
        text(page,"YUNSEOK · TOUCH MACRO",11,blue);text(page,"윤석 터치매크로",25,ink);text(page,"동작을 추가하고, 원하는 화면에서 실행하세요.",13,muted);
        LinearLayout setup=card(page);boolean connected=MacroService.get()!=null;state=text(setup,connected?"● 접근성 연결됨 · 실행 준비 완료":"○ 최초 설정 · 접근성 서비스를 켜주세요",14,connected?Color.rgb(10,129,103):blue);
        LinearLayout r=row(setup);halfButton(r,connected?"접근성 설정":"접근성 켜기",this::accessibility);halfButton(r,"사용법",this::help);
        LinearLayout config=card(page);text(config,macro.name,19,ink);text(config,macro.steps.size()+"개 동작 · "+(macro.rounds==0?"무한 반복":macro.rounds+"회 반복")+" · 시작 대기 "+macro.startDelay+"ms",12,muted);
        r=row(config);halfButton(r,"실행 설정",this::settings);halfButton(r,"이름으로 저장",this::namedSave);
        r=row(config);halfButton(r,"저장 목록",this::library);halfButton(r,"새 매크로",()->new AlertDialog.Builder(this).setMessage("새 매크로를 만듭니다. 현재 작업은 먼저 이름으로 저장해 두세요.").setPositiveButton("새로 만들기",(d,w)->{macro=new Macro();save();render();}).setNegativeButton("취소",null).show());
        r=row(page);halfButton(r,"+ 동작 추가",this::chooseStep);halfButton(r,"화면에서 좌표 선택",this::pick);
        list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);page.addView(list);refreshList();
        LinearLayout run=card(page);Button start=button(run,"▶ 실행 · 시작 대기 "+macro.startDelay+"ms",this::start);start.setTextColor(Color.WHITE);android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(blue);bg.setCornerRadius(dp(10));start.setBackground(bg);
        text(run,"시작 대기 시간은 실행 설정에서 바꿀 수 있습니다. 컨트롤의 상태 부분을 끌어 위치를 옮기세요.",11,muted);
        r=row(run);halfButton(r,"컨트롤만 띄우기",()->{if(service()){save();MacroService.get().showControls();moveTaskToBack(true);}});halfButton(r,"정지 / 컨트롤 닫기",()->{if(MacroService.get()!=null){MacroService.get().stop();MacroService.get().hideControls();}toast("정지");});
        LinearLayout tools=card(page);r=row(tools);halfButton(r,"파일 내보내기",this::exportMacro);halfButton(r,"파일 불러오기",this::importMacro);r=row(tools);halfButton(r,"실행 기록",this::logs);halfButton(r,"인식 테스트 화면",this::testScreen);
        text(page,"한글 OCR 내장 · 화면과 입력 내용은 기기 안에서 처리\nAndroid 11 이상 · 화면을 켜 둔 상태에서 사용",11,muted);font(page);
    }
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
        show(new AlertDialog.Builder(this).setTitle("접근성 사용 안내").setMessage("터치와 스와이프 실행, 다른 앱의 화면 글자 읽기, 화면 캡처를 통한 OCR, 뒤로가기에 접근성 권한을 사용합니다.\n\nOCR 모델은 APK에 포함되어 있습니다. 화면이나 입력 내용은 서버로 전송하지 않습니다. 실행은 직접 만든 매크로로만 시작되며, 컨트롤 또는 알림의 정지 버튼으로 중단할 수 있습니다.\n\n설정에서 ‘설치된 서비스 → 윤석 터치매크로’를 켜주세요.")
            .setPositiveButton("설정 열기",(d,w)->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))).setNegativeButton("취소",null),null);
    }
    private void start(){if(!service())return;try{macro.validate();save();MacroService.get().showControls();MacroService.get().start(macro);moveTaskToBack(true);}catch(Exception e){toast(e.getMessage());}}
    private void pick(){if(service()){if(MacroService.get().running){toast("먼저 매크로를 정지하세요");return;}save();moveTaskToBack(true);MacroService.get().pickPoint();}}
    private void chooseStep(){show(new AlertDialog.Builder(this).setTitle("추가할 동작").setItems(Step.NAMES,(d,w)->{if(macro.steps.size()>=500){toast("최대 500개입니다");return;}Step s=new Step(Step.TYPES[w]);macro.steps.add(s);save();render();edit(s);}),null);}
    private LinearLayout form(){LinearLayout f=new LinearLayout(this);f.setOrientation(LinearLayout.VERTICAL);f.setPadding(dp(20),dp(8),dp(20),dp(12));return f;}
    private ScrollView wrap(LinearLayout f){ScrollView s=new ScrollView(this);s.addView(f);return s;}
    private EditText field(LinearLayout f,String label,String value,boolean numeric){
        text(f,label,12,muted);EditText e=new EditText(this);e.setText(value);e.setTextSize(15);e.setSingleLine(!label.contains("여러 줄"));e.setSelectAllOnFocus(true);
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
        if(Arrays.asList("TAP","DOUBLE","LONG","SWIPE").contains(s.type)){
            text(f,"화면 전체를 100%로 계산합니다. 같은 화면 방향에서 사용하세요.",12,muted);
            fields.put("x",field(f,"X 위치 % · 0~100",Double.toString(s.x),true));fields.put("y",field(f,"Y 위치 % · 0~100",Double.toString(s.y),true));
            if(s.type.equals("SWIPE")){fields.put("x2",field(f,"끝 X 위치 %",Double.toString(s.x2),true));fields.put("y2",field(f,"끝 Y 위치 %",Double.toString(s.y2),true));}
        }
        if(Arrays.asList("LONG","SWIPE","WAIT").contains(s.type))fields.put("duration",field(f,"동작 시간 ms · 1000 = 1초",Integer.toString(s.duration),true));
        Spinner matching=null,source=null,failure=null;CheckBox spaces=null,casing=null;
        String[] matchValues={"CONTAINS","EXACT","REGEX"},sourceValues={"AUTO","ACCESS","OCR"},failureValues={"STOP","SKIP","BACK_RETRY"};
        if(s.textAction()){
            fields.put("text",field(f,"찾을 글자",s.text,false));
            matching=spinner(f,"글자 비교",new String[]{"포함","완전 일치","정규식"},selected(matchValues,s.match));
            source=spinner(f,"검색 방식",new String[]{"자동 · 화면 정보 → OCR","화면 정보만 · 빠름","OCR만 · 이미지 속 글자"},selected(sourceValues,s.source));
            spaces=check(f,"공백·줄바꿈 무시",s.ignoreSpaces);casing=check(f,"영문 대소문자 무시",s.ignoreCase);
            fields.put("occurrence",field(f,"같은 글자 중 몇 번째 · 위에서 아래, 왼쪽에서 오른쪽",Integer.toString(s.occurrence),true));
            fields.put("timeout",field(f,"각 화면에서 기다릴 최대 시간 ms · 0 = 한 번 확인",Integer.toString(s.timeout),true));
            if(s.type.equals("TEXT_BRANCH")){
                text(f,"0은 다음 동작입니다. 번호는 현재 목록을 기준으로 합니다. 재정렬·삭제 후 확인하세요.",12,muted);
                fields.put("found",field(f,"글자 있으면 이동할 동작 번호",Integer.toString(s.found),true));fields.put("missing",field(f,"글자 없으면 이동할 동작 번호",Integer.toString(s.missing),true));
            }else{
                failure=spinner(f,"글자가 없으면",new String[]{"매크로 정지","다음 동작으로","뒤로가기 후 다시 찾기 · 최종 실패 시 정지"},selected(failureValues,s.failure));
                fields.put("retries",field(f,"최대 뒤로가기 횟수 · 0~100",Integer.toString(s.retries),true));fields.put("retryDelay",field(f,"뒤로간 뒤 화면 안정 대기 ms · 최소 300",Integer.toString(s.retryDelay),true));
                text(f,"최대 횟수 뒤에도 글자가 없으면 중단합니다. 캡처 오류는 ‘글자 없음’으로 처리하지 않습니다.",11,muted);
            }
        }
        if(s.type.equals("INPUT"))fields.put("text",field(f,"입력할 문자 · 여러 줄 가능",s.text,false));
        if(s.type.equals("APP")){
            EditText e=field(f,"실행할 앱 패키지 이름",s.packageName,false);fields.put("packageName",e);button(f,"설치된 앱에서 선택",()->apps(e));
        }
        if(s.type.equals("GOTO")){text(f,"동작 번호는 목록을 재정렬하거나 삭제하면 달라집니다.",12,muted);fields.put("target",field(f,"이동할 동작 번호 · 1부터",Integer.toString(s.target),true));}
        fields.put("repeat",field(f,"이 동작 반복 횟수 · 1~1000",Integer.toString(s.repeat),true));fields.put("after",field(f,"동작 완료 후 대기 ms",Integer.toString(s.after),true));CheckBox enabled=check(f,"이 동작 사용",s.enabled);
        final Spinner fm=matching,fsrc=source,ff=failure;final CheckBox fsp=spaces,fca=casing;
        AlertDialog d=new AlertDialog.Builder(this).setView(wrap(f)).setPositiveButton("적용",null).setNegativeButton("취소",null).create();
        d.setOnShowListener(v->{font(d.getWindow().getDecorView());d.getButton(-1).setOnClickListener(b->{try{
            if(fields.containsKey("x")){s.x=decimal(fields.get("x"));s.y=decimal(fields.get("y"));}if(fields.containsKey("x2")){s.x2=decimal(fields.get("x2"));s.y2=decimal(fields.get("y2"));}
            if(fields.containsKey("duration"))s.duration=number(fields.get("duration"));if(fields.containsKey("text"))s.text=fields.get("text").getText().toString();
            if(fm!=null){s.match=matchValues[fm.getSelectedItemPosition()];s.source=sourceValues[fsrc.getSelectedItemPosition()];s.ignoreSpaces=fsp.isChecked();s.ignoreCase=fca.isChecked();s.timeout=number(fields.get("timeout"));s.occurrence=number(fields.get("occurrence"));}
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
        text(f,"화면이 꺼지거나 방향이 바뀌면 중단됩니다. 긴 실행은 화면 자동 꺼짐 시간을 직접 조정하세요.",12,muted);
        AlertDialog d=new AlertDialog.Builder(this).setView(wrap(f)).setPositiveButton("적용",null).setNegativeButton("취소",null).create();d.setOnShowListener(v->{font(d.getWindow().getDecorView());d.getButton(-1).setOnClickListener(b->{try{int r=number(rounds),g=number(gap),st=number(start),mx=number(max);if(r<0||r>100000||g<0||g>600000||st<0||st>600000||mx<0||mx>1440)throw new IllegalArgumentException("범위를 확인하세요");macro.rounds=r;macro.gap=g;macro.startDelay=st;macro.maxMinutes=mx;save();render();d.dismiss();}catch(Exception e){toast("숫자 범위를 확인하세요");}});});d.show();
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
    private void help(){LinearLayout f=form();text(f,"처음 시작하기",20,ink);text(f,"1. 접근성 설정에서 윤석 터치매크로를 켭니다.\n2. 동작을 추가하고 이름으로 저장합니다.\n3. ‘컨트롤만 띄우기’를 누르고 자동화할 앱으로 갑니다.\n4. 화면 위 ‘시작’을 누릅니다.\n5. 일시정지/계속, 정지 버튼으로 제어합니다. 실행 중 알림에서도 정지할 수 있습니다.",14,ink);
        text(f,"글자 없으면 뒤로가기",18,blue);text(f,"‘글자 찾아 터치’ → 찾을 글자 입력 → ‘글자가 없으면: 뒤로가기 후 다시 찾기’. 각 화면에서 지정한 시간만큼 찾고, 없으면 뒤로 간 뒤 다시 확인합니다. 최대 횟수를 넘기면 정지합니다.\n\n화면 정보는 일반 버튼·라벨에 빠릅니다. 게임이나 이미지 속 글자는 OCR을 사용하세요. 자동 모드는 화면 정보에서 못 찾으면 OCR로 확인합니다. 동일한 글자의 순서는 위→아래, 왼쪽→오른쪽입니다.",14,ink);
        text(f,"좌표와 조건",18,blue);text(f,"좌표는 화면 전체 너비/높이의 비율(%)입니다. ‘화면에서 좌표 선택’은 실제 터치 없이 위치만 기록합니다. 캡처 후 좌표 터치 동작이 추가됩니다. 스와이프는 시작·끝 좌표를 입력하세요.\n\n글자 조건 분기는 있을 때와 없을 때 각각 동작 번호로 이동합니다. 0은 다음입니다. 동작을 정렬·삭제하면 번호를 다시 확인하세요. 전체 반복 0은 무한입니다.",14,ink);
        text(f,"사용 시 알아두기",18,blue);text(f,"화면을 켜 두고 사용하세요. 잠금 화면·회전·시간 제한은 실행을 중단합니다. 다른 손가락 터치가 매크로 제스처를 취소할 수 있습니다. 이미 전달된 긴 제스처는 정지 후 짧게 마무리될 수 있습니다.\n\n보안 화면은 캡처가 차단될 수 있습니다. OCR은 폰트·화질·레이아웃에 따라 오인식할 수 있으므로 테스트 후 사용하세요. 이 앱에서 화면·문자 데이터를 서버로 보내는 코드나 인터넷 권한을 추가하지 않았습니다.\n\nAndroid 13 이상에서 접근성 설정이 막히면 앱 정보 우측 메뉴의 ‘제한된 설정 허용’을 확인하세요.",13,muted);show(new AlertDialog.Builder(this).setView(wrap(f)).setPositiveButton("닫기",null),null);}
    private void testScreen(){
        testing=true;LinearLayout box=form();box.setBackgroundColor(Color.WHITE);content(box);
        text(box,"인식 테스트",24,ink);text(box,"아래 버튼은 화면 정보, 파란 캔버스 글자는 OCR로 테스트할 수 있습니다.",13,muted);
        TextView result=text(box,"성공 횟수: 0",20,ink);int[] taps={0};button(box,"화면 확인",()->result.setText(getString(R.string.test_taps,++taps[0])));
        View canvas=new View(this){@Override public boolean performClick(){return super.performClick();}Paint p=new Paint(3);@Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(Color.rgb(232,240,255));p.setColor(Color.rgb(20,40,85));p.setTypeface(getResources().getFont(R.font.nanum_gothic_bold));p.setTextSize(dp(27));p.setTextAlign(Paint.Align.CENTER);c.drawText("테스트 확인",getWidth()/2f,getHeight()/2f+dp(9),p);}};
        box.addView(canvas,new LinearLayout.LayoutParams(-1,dp(160)));canvas.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_UP){v.performClick();result.setText(getString(R.string.ocr_taps,++taps[0]));}return true;});
        button(box,"화면 정보 테스트 실행",()->demo("ACCESS","화면 확인"));button(box,"OCR 테스트 실행",()->demo("OCR","테스트 확인"));button(box,"편집 화면으로",this::render);font(box);
    }
    private void demo(String source,String value){if(!service())return;if(MacroService.get().running){toast("먼저 정지하세요");return;}Macro m=new Macro();m.name="인식 테스트";m.startDelay=1000;Step s=new Step("TEXT_TAP");s.text=value;s.source=source;s.timeout=5000;m.steps.add(s);MacroService.get().start(m);}
    private void toast(String s){Toast.makeText(this,s==null?"설정을 확인하세요":s,Toast.LENGTH_LONG).show();}
}
