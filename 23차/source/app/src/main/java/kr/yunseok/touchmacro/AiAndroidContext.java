package kr.yunseok.touchmacro;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/** 사용자가 저장한 등록 값만 복사하며 화면 OCR·입력 칸 내용은 읽지 않습니다. */
public final class AiAndroidContext {
    private AiAndroidContext() { }
    public static JSONObject capture(Context activity,Macro current,String mode){
        return capture(activity,current,mode,"");
    }
    public static JSONObject capture(Context activity,Macro current,String mode,String executionTarget){
        if(!mode.equals("MACRO")&&!mode.equals("WATCH"))throw AiJson.error("계획 문맥 모드");
        JSONObject points=new JSONObject(),gestures=new JSONObject(),regions=new JSONObject(),actions=new JSONObject(),macros=new JSONObject(),apps=new JSONObject();
        LinkedHashMap<String,Macro> library=Store.library(activity);for(String name:library.keySet())put(macros,name,library.get(name).json());String currentName="현재 편집본";while(macros.has(currentName))currentName+=" · 현재";put(macros,currentName,current.json());
        for(int i=0;i<current.steps.size();i++){Step step=current.steps.get(i);String name=(i+1)+"번 동작";
            if(step.enabled&&AiPlanParser.POINTS.contains(step.type))put(points,name,AiJson.object("x",step.x,"y",step.y));
            if(step.enabled&&AiPlanParser.GESTURES.contains(step.type))put(gestures,name,AiJson.object("type",step.type,"payload",step.json()));
            if(step.enabled&&step.recoveryAction())put(actions,name,step.json());
            if(step.enabled&&step.type.equals("APP"))put(apps,name,step.packageName);
            if(step.textAction())put(regions,(i+1)+"번 검색 영역",rectangle(step));
        }
        String raw=Ui.prefs(activity).getString("watch_config",null);WatchConfig watch;
        try{watch=raw==null?new WatchConfig():new WatchConfig(new JSONObject(raw));watch.validate();}
        catch(Exception error){throw AiJson.error("저장된 목록 설정을 확인하세요. 설정을 대신 바꾸지 않았습니다");}
        for(int i=0;i<watch.priorityAreas.size();i++)put(regions,"탐지 영역"+(i+1),watch.priorityAreas.get(i).json());
        put(regions,"목록 검색 영역",rectangle(watch.rule));if(watch.entryCheck)put(regions,"진입 확인 영역",rectangle(watch.entryRule));
        String source=mode.equals("WATCH")?watch.rule.source:Ui.prefs(activity).getString("word_source","OCR");if(!java.util.Arrays.asList("AUTO","ACCESS","OCR").contains(source))throw AiJson.error("현재 인식 방식을 확인하세요");
        if(!executionTarget.isEmpty()&&(!executionTarget.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+")||executionTarget.equals(activity.getPackageName())))throw AiJson.error("실행할 외부 앱을 선택하세요");
        return AiJson.object("planning_mode",mode,"recognition_source",source,"execution_target",executionTarget,"points",points,"gestures",gestures,"regions",regions,"macros",macros,"apps",apps,"actions",actions,"defaults",AiJson.object("watch_cooldown_ms",watch.cooldownMs,"watch_interval_ms",watch.intervalSeconds*1000,"entry_timeout_ms",watch.entryRule.timeout),"watch_settings",watch.json(),"current_macro",current.json(),"stored_current_macro",Store.current(activity));
    }
    public static LinkedHashMap<String,String> executionTargets(Context context){
        java.util.TreeMap<String,String> sorted=new java.util.TreeMap<>(AiTokenizer.CODEPOINT_ORDER);
        for(ResolveInfo info:context.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)){
            String pkg=info.activityInfo.packageName;if(pkg.equals(context.getPackageName()))continue;String label=info.loadLabel(context.getPackageManager()).toString();sorted.put(AiPlanPreview.quote(label)+" · "+pkg,pkg);
        }
        String saved=WatchStore.load(context).targetPackage;if(!saved.isEmpty()&&!saved.equals(context.getPackageName())&&!sorted.containsValue(saved))sorted.put("저장한 대상 · "+saved,saved);
        return new LinkedHashMap<>(sorted);
    }
    private static JSONObject rectangle(Step step){return AiJson.object("left",step.left,"top",step.top,"right",step.right,"bottom",step.bottom);}
    private static void put(JSONObject target,String key,Object value){try{target.put(key,value);}catch(Exception error){throw AiJson.error("등록 문맥 복사 실패");}}
    public static String describe(JSONObject context){
        StringBuilder text=new StringBuilder(context.opt("planning_mode").equals("WATCH")?"목록 탐지 문맥\n":"매크로 동작 문맥\n");text.append("인식 방식 ").append(context.opt("recognition_source")).append(" · 미설정 글자 방식은 한글 OCR\n");
        for(String kind:new String[]{"points","gestures","regions","macros","apps","actions"}){String title=kind.equals("points")?"좌표":kind.equals("gestures")?"제스처":kind.equals("regions")?"영역":kind.equals("macros")?"매크로":kind.equals("apps")?"앱":"복구 동작";JSONObject values=AiJson.obj(context.opt(kind));ArrayList<String> names=new ArrayList<>();java.util.Iterator<String> iterator=values.keys();while(iterator.hasNext())names.add(iterator.next());names.sort(AiTokenizer.CODEPOINT_ORDER);text.append(title).append(": ").append(names.isEmpty()?"등록 없음":String.join(" · ",names)).append('\n');}
        text.append("등록된 이름을 명령에 적어 주세요. 여기·저기 같은 미등록 위치는 실행하지 않습니다. 화면 글자는 명령으로 읽지 않습니다.");return text.toString();
    }
}
