package kr.yunseok.touchmacro;
import android.app.*;
import android.content.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

/** 한 번에 한 단계만 표시하고 외부 앱 작업 후 같은 설정 흐름으로 돌아옵니다. */
public final class SetupWizard {
    private final MainActivity a;
    public SetupWizard(MainActivity a){this.a=a;}
    private TextView text(LinearLayout box,String value,int size){TextView v=new TextView(a);v.setText(value);v.setTextSize(size);v.setTextColor(size<=14?Ui.muted(a):Ui.ink(a));v.setLineSpacing(Ui.dp(a,3),1);v.setPadding(0,Ui.dp(a,8),0,Ui.dp(a,8));box.addView(v);return v;}
    private EditText field(LinearLayout box,String label,String value,boolean numeric){text(box,label,12);EditText e=new EditText(a);e.setText(value);e.setTextSize(16);e.setTextColor(Ui.ink(a));e.setSingleLine(true);e.setSelectAllOnFocus(true);if(numeric)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,Ui.dp(a,54));lp.setMargins(0,0,0,Ui.dp(a,10));box.addView(e,lp);return e;}
    private EditText time(LinearLayout box,String label,int ms){return field(box,label+" · 초",Times.seconds(ms),true);}
    private void button(LinearLayout box,String label,Runnable action,boolean primary){Button b=Ui.button(a,label);b.setTextSize(14);if(primary){b.setBackground(Ui.rounded(Ui.accent(a),Ui.dp(a,8)));b.setTextColor(Ui.dark(a)?0xFF101828:0xFFFFFFFF);}LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,Ui.dp(a,52));lp.setMargins(0,Ui.dp(a,8),0,0);box.addView(b,lp);b.setOnClickListener(v->{try{action.run();}catch(Exception e){Toast.makeText(a,e.getMessage()==null?"입력값을 확인하세요":e.getMessage(),Toast.LENGTH_LONG).show();}});}
    private int integer(EditText e){try{return Integer.parseInt(e.getText().toString().trim());}catch(Exception x){throw new IllegalArgumentException("횟수와 새로고침 간격은 정수로 입력하세요");}}
    private int millis(EditText e){return Times.millis(e.getText().toString());}
    private void next(int n){SetupFlow.stage(a,n);a.refreshSetup();}
    public void render(LinearLayout page){
        int step=SetupFlow.stage(a);WatchConfig p=WatchStore.load(a);
        String[] titles={"어떤 글자를 찾을까요?","어디에서 찾을까요?","들어간 뒤 할 동작 녹화","횟수와 대기 시간","준비가 끝났어요"};
        LinearLayout progress=new LinearLayout(a);LinearLayout.LayoutParams progressSize=new LinearLayout.LayoutParams(-1,Ui.dp(a,5));progressSize.setMargins(0,Ui.dp(a,4),0,Ui.dp(a,20));page.addView(progress,progressSize);
        for(int i=0;i<5;i++){View bar=new View(a);bar.setBackground(Ui.rounded(i<=step?Ui.accent(a):Ui.soft(a),Ui.dp(a,3)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);if(i<4)lp.setMargins(0,0,Ui.dp(a,5),0);progress.addView(bar,lp);}
        text(page,(step+1)+" / 5  ·  새로운 작업",11).setTextColor(Ui.accent(a));text(page,titles[step],29);
        if(step==0){
            text(page,"찾을 글자를 입력하면 다음 단계에서 화면의 위치를 정합니다.",13);
            EditText word=field(page,"찾을 글자",p.rule.text,false);
            button(page,"다음 · 탐지 영역 정하기",()->{p.rule.text=word.getText().toString().trim();WatchStore.save(a,p);next(1);},true);
        }else if(step==1){
            text(page,"네모 영역을 최대 6개까지 순서대로 그리세요. 매번 1순위부터 확인해 처음 글자가 있는 영역을 실행합니다. 영역은 서로 겹치지 않게 지정하세요.",14);
            for(int i=0;i<p.priorityAreas.size();i++){final int index=i;button(page,(i+1)+"순위 영역 다시 지정",()->{SetupFlow.areaIndex(a,index);a.setupOutside("REGION");},false);}
            if(p.priorityAreas.size()<6)button(page,(p.priorityAreas.size()+1)+"순위 영역 추가",()->{SetupFlow.areaIndex(a,p.priorityAreas.size());a.setupOutside("REGION");},p.priorityAreas.isEmpty());
            if(!p.priorityAreas.isEmpty()){
                button(page,"다음 · 매크로 녹화",()->next(2),true);
                button(page,"마지막 순위 영역 삭제",()->{p.priorityAreas.remove(p.priorityAreas.size()-1);WatchStore.save(a,p);a.refreshSetup();},false);
            }else if(p.rule.left!=0||p.rule.top!=0||p.rule.right!=100||p.rule.bottom!=100)button(page,"기존 영역을 1순위로 사용",()->{p.priorityAreas.add(new WatchArea(p.rule.left,p.rule.top,p.rule.right,p.rule.bottom));WatchStore.save(a,p);a.refreshSetup();},false);
        }else if(step==2){
            text(page,"대상 앱에서 글자를 직접 눌러 들어간 뒤 ‘녹화 시작’을 누르세요. 안에서 할 동작을 한 세트만 녹화하고 ‘완료’를 누르면 자동 저장됩니다.",14);
            text(page,"들어가는 터치와 마지막 뒤로가기는 앱이 처리합니다. 녹화에는 넣지 마세요.",13);
            EditText name=field(page,"작업 이름",SetupFlow.name(a),false);
            button(page,SetupFlow.pending(a)?"임시 녹화 이어서 하기":"대상 앱에서 녹화 준비",()->{SetupFlow.name(a,name.getText().toString());a.setupOutside("RECORD");},true);
            if(SetupFlow.pending(a))button(page,"임시 녹화 취소",()->{SetupFlow.cancelDraft(a);a.refreshSetup();},false);
            button(page,"저장한 매크로 사용",()->chooseMacro(p),false);
        }else if(step==3){
            text(page,"단계 사이에 기다릴 시간을 직접 정하세요. 0.5는 반 초, 1.5는 1.5초입니다.",13);
            EditText repeats=field(page,"매크로 전체 반복 · 회",Integer.toString(p.repetitions),true);
            EditText entry=time(page,"글자 터치 → 매크로 시작",p.entryMs);
            EditText gap=time(page,"매크로 한 회 → 다음 회",p.repeatGapMs);
            EditText back=time(page,"매크로 모두 완료 → 뒤로가기",p.beforeBackMs);
            EditText returned=time(page,"뒤로가기 → 목록 화면 대기",p.returnMs);
            LinearLayout cooldownRow=new LinearLayout(a);page.addView(cooldownRow);LinearLayout minutesBox=new LinearLayout(a),secondsBox=new LinearLayout(a);minutesBox.setOrientation(LinearLayout.VERTICAL);secondsBox.setOrientation(LinearLayout.VERTICAL);cooldownRow.addView(minutesBox,new LinearLayout.LayoutParams(0,-2,1));cooldownRow.addView(secondsBox,new LinearLayout.LayoutParams(0,-2,1));
            EditText cooldownMinutes=field(minutesBox,"완료 후 쉬는 시간 · 분",Integer.toString(p.cooldownMs/60000),true),cooldownSeconds=time(secondsBox,"추가",p.cooldownMs%60000);
            EditText interval=field(page,"그 이후 새로고침 간격 · 초",Integer.toString(p.intervalSeconds),true);
            EditText settle=time(page,"새로고침 → 글자 탐지",p.settleMs);
            EditText start=time(page,"시작 버튼 → 첫 탐지",p.startMs);
            text(page,"완료 후에는 정한 시간을 쉬고 새로고침하기 전까지 다시 누르지 않습니다. 새로고침 후에는 1순위부터 다시 확인하며, 같은 2순위에 글자가 있어도 다시 실행합니다.",13);
            button(page,"다음 · 실행 준비",()->{p.repetitions=integer(repeats);p.entryMs=millis(entry);p.repeatGapMs=millis(gap);p.beforeBackMs=millis(back);p.returnMs=millis(returned);p.cooldownMs=Times.minutesAndSeconds(integer(cooldownMinutes),cooldownSeconds.getText().toString());p.intervalSeconds=integer(interval);p.settleMs=millis(settle);p.startMs=millis(start);p.tapFound=true;p.waitForRefresh=true;p.preventDuplicate=false;WatchStore.save(a,p);WatchStore.resolve(a,p);next(4);},true);
        }else{
            text(page,"1~"+p.areaCount()+"순위에서 “"+p.rule.text+"” 발견 → 들어가기\n"+p.macroName+" · "+p.repetitions+"회 반복\n뒤로가기 → "+Times.durationLabel(p.cooldownMs)+" 쉬기\n첫 새로고침 후 "+p.intervalSeconds+"초 간격 새로고침",16);
            text(page,"쉬는 동안에는 재실행하지 않습니다. 새로고침 후에는 1순위부터 다시 확인하고, 같은 영역의 글자도 다시 실행합니다.",13);
            text(page,"대상 앱의 목록 화면으로 돌아간 뒤 작은 안내창에서 시작하세요. 하단에 현재 단계와 한 바퀴·전체 시간이 표시됩니다.",13);
            button(page,"대상 앱으로 이동해서 시작",()->{WatchStore.resolve(a,p);a.setupOutside("START");},true);
            button(page,"설정을 저장하고 안내 닫기",()->{SetupFlow.finished(a);a.refreshSetup();},false);
        }
        if(step>0)button(page,"이전 단계",()->next(step-1),false);
    }
    private void chooseMacro(WatchConfig p){
        if(SetupFlow.pending(a))throw new IllegalStateException("먼저 임시 녹화를 마치거나 취소하세요");
        LinkedHashMap<String,Macro> all=Store.library(a);ArrayList<String> keys=new ArrayList<>();keys.add("");keys.addAll(all.keySet());ArrayList<String> labels=new ArrayList<>();labels.add("현재 매크로 · "+Store.load(a).name);labels.addAll(all.keySet());
        AlertDialog d=new AlertDialog.Builder(a).setTitle("사용할 매크로").setItems(labels.toArray(new String[0]),(dialog,index)->{try{p.macroName=keys.get(index);WatchStore.resolve(a,p);if(p.macroName.isEmpty()){Macro current=Store.load(a);if(Store.library(a).containsKey(current.name))current.name=current.name+" · 안내용 "+System.currentTimeMillis();Store.put(a,current);p.macroName=current.name;}WatchStore.save(a,p);next(3);}catch(Exception e){Toast.makeText(a,e.getMessage(),Toast.LENGTH_LONG).show();}}).setNegativeButton("취소",null).create();d.setOnShowListener(v->Ui.font(d.getWindow().getDecorView()));d.show();
    }
}
