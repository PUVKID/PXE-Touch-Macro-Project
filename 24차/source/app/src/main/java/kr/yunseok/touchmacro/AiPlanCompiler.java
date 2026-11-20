package kr.yunseok.touchmacro;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/** 완성 AST를 전체 검증한 뒤에만 기존 실행 모델로 바꿉니다. 저장은 하지 않습니다. */
public final class AiPlanCompiler {
    public static final class Compiled {
        private final JSONObject macro,watch,plan,context;
        Compiled(Macro macro,WatchConfig watch,JSONObject plan,JSONObject context){this.macro=AiJson.copy(macro.json());this.watch=watch==null?null:AiJson.copy(watch.json());this.plan=AiJson.copy(plan);this.context=AiJson.copy(context);}
        public Macro macro(){return new Macro(AiJson.copy(macro));}
        public WatchConfig watch(){return watch==null?null:new WatchConfig(AiJson.copy(watch));}
        public JSONObject plan(){return AiJson.copy(plan);}
        public String executionTarget(){Object value=context.opt("execution_target");return AiJson.nil(value)?"":AiJson.string(value);}
        public String preview(){return AiPlanPreview.render(plan,context,macro(),watch());}
    }
    private final JSONObject context;
    private final Macro output=new Macro();
    private final ArrayList<Recovery> recoveries=new ArrayList<>();
    private final ArrayList<Jump> jumps=new ArrayList<>();
    private int nodes;
    private static final class Recovery {final Step owner,action;Recovery(Step owner,Step action){this.owner=owner;this.action=action;}}
    private static final class Jump {final Step step;final int target;Jump(Step step,int target){this.step=step;this.target=target;}}
    private AiPlanCompiler(JSONObject context){this.context=AiJson.copy(context);output.name="AI 명령 계획";output.rounds=1;output.startAt=1;output.startDelay=0;output.maxMinutes=0;output.gap=0;output.adaptiveWait=false;}
    public static JSONObject validateAst(JSONObject plan,JSONObject context){
        JSONObject frozen=AiJson.copy(plan);if(!"pxe.plan.v1".equals(frozen.opt("schema")))throw AiJson.error("계획 형식");
        if("reject".equals(frozen.opt("status"))){AiJson.keys(frozen,"schema","status","reason");if(!AiPlanParser.REASONS.contains(frozen.opt("reason")))throw AiJson.error("거절 사유");}
        else{AiJson.keys(frozen,"schema","status","body");if(!"ready".equals(frozen.opt("status")))throw AiJson.error("미완성 계획");new AiPlanCompiler(context).validateBody(AiJson.array(frozen.opt("body")),0);}
        return frozen;
    }
    public static Compiled compile(JSONObject plan,JSONObject context){
        AiPlanCompiler compiler=new AiPlanCompiler(context);JSONObject frozen=AiJson.copy(plan);AiJson.keys(frozen,"schema","status", "reject".equals(frozen.opt("status"))?"reason":"body");
        if(!"pxe.plan.v1".equals(frozen.opt("schema")))throw AiJson.error("계획 형식");
        if("reject".equals(frozen.opt("status"))){String reason=AiJson.string(frozen.opt("reason"));if(!AiPlanParser.REASONS.contains(reason))throw AiJson.error("거절 사유");throw AiJson.error("명령을 해석할 수 없습니다: "+reason);}
        if(!"ready".equals(frozen.opt("status")))throw AiJson.error("미완성 계획");JSONArray body=AiJson.array(frozen.opt("body"));compiler.validateBody(body,0);
        if(body.length()==1){JSONObject node=AiJson.obj(body.opt(0));if("CALL".equals(node.opt("op")))return new Compiled(compiler.macroReference(node.opt("macro_ref")),null,frozen,compiler.context);if("WATCH".equals(node.opt("op")))return compiler.watch(node,frozen);}
        compiler.emitBody(body,true);compiler.finishRecoveries();compiler.output.validate();compiler.validateAll(compiler.output);return new Compiled(compiler.output,null,frozen,compiler.context);
    }
    private static String string(JSONObject value,String field){return AiJson.string(value.opt(field));}
    private Object reference(String kind,Object name){JSONObject registered=AiJson.obj(context.opt(kind));String key=AiJson.text(name);if(!registered.has(key))throw AiJson.error("등록되지 않은 "+kind+" 참조: "+key);return registered.opt(key);}
    private WatchArea region(Object name){JSONObject r=AiJson.obj(reference("regions",name));AiJson.keys(r,"left","top","right","bottom");double l=AiJson.coordinate(r.opt("left")),t=AiJson.coordinate(r.opt("top")),right=AiJson.coordinate(r.opt("right")),bottom=AiJson.coordinate(r.opt("bottom"));if(l>=right||t>=bottom)throw AiJson.error("영역 크기");return new WatchArea(l,t,right,bottom);}
    private Macro macroReference(Object name){JSONObject raw=AiJson.obj(reference("macros",name));JSONArray steps=AiJson.array(raw.opt("steps"));if(steps.length()<1||steps.length()>500)throw AiJson.error("저장 매크로 크기");for(String key:new String[]{"rounds","gap","startDelay","maxMinutes","startAt"})AiJson.integer(raw.opt(key),key.equals("startAt")?1:0,key.equals("rounds")?100000:key.equals("maxMinutes")?1440:key.equals("startAt")?steps.length():600000);if(raw.has("adaptiveWait"))AiJson.bool(raw.opt("adaptiveWait"));if(raw.has("name"))AiJson.string(raw.opt("name"));for(int i=0;i<steps.length();i++)snapshot(AiJson.obj(steps.opt(i)),steps.length());Macro m=new Macro(AiJson.copy(raw));validateAll(m);return m;}
    private void schemaMacroReference(Object name){AiSchemaSnapshots.macro(AiJson.obj(reference("macros",name)));}
    private static void validateAll(Macro macro){boolean enabled=false;for(Step step:macro.steps){step.validate(macro.steps.size());if(step.enabled)enabled=true;}if(!enabled)throw AiJson.error("실행할 동작 없음");macro.validate();}
    private static Step snapshot(JSONObject raw,int count){
        String type=AiJson.string(raw.opt("type"));if(!Arrays.asList(Step.TYPES).contains(type))throw AiJson.error("등록 동작 유형");AiJson.bool(raw.opt("enabled"));AiJson.integer(raw.opt("repeat"),1,1000);AiJson.integer(raw.opt("after"),0,600000);
        for(String key:new String[]{"duration","after","repeat","timeout","retries","retryDelay","found","missing","target","occurrence","recoveryTarget","durationJitter","afterJitter"})if(raw.has(key))AiJson.integer(raw.opt(key),0,600000);
        for(String key:new String[]{"enabled","ignoreSpaces","ignoreCase","waitForever","immediate","retryForever"})if(raw.has(key))AiJson.bool(raw.opt(key));
        for(String key:new String[]{"text","match","source","failure","packageName"})if(raw.has(key))AiJson.string(raw.opt(key));
        for(String key:new String[]{"x","y","x2","y2","cx","cy","left","top","right","bottom"})if(raw.has(key))AiJson.coordinate(raw.opt(key));
        for(String key:new String[]{"offsetX","offsetY"})if(raw.has(key)){Object v=raw.opt(key);if(!(v instanceof Number)||!Double.isFinite(((Number)v).doubleValue())||Math.abs(((Number)v).doubleValue())>50)throw AiJson.error("터치 보정");}
        if(AiPlanParser.POINTS.contains(type)||AiPlanParser.GESTURES.contains(type)&&!type.equals("MULTI_TAP")&&!type.equals("GESTURE")){AiJson.coordinate(raw.opt("x"));AiJson.coordinate(raw.opt("y"));}
        if(Arrays.asList("SWIPE","CURVE","PINCH_IN","PINCH_OUT").contains(type)){AiJson.coordinate(raw.opt("x2"));AiJson.coordinate(raw.opt("y2"));}
        if(type.equals("CURVE")){AiJson.coordinate(raw.opt("cx"));AiJson.coordinate(raw.opt("cy"));}
        if(Arrays.asList("WAIT","LONG","SWIPE","CURVE","PINCH_IN","PINCH_OUT","MULTI_TAP","GESTURE").contains(type))AiJson.integer(raw.opt("duration"),1,type.equals("WAIT")?600000:59000);
        if(type.equals("MULTI_TAP")||type.equals("INPUT"))AiJson.string(raw.opt("text"));if(type.equals("APP"))AiJson.string(raw.opt("packageName"));
        if(type.equals("GOTO"))AiJson.integer(raw.opt("target"),1,count);
        if(type.startsWith("TEXT_")){AiJson.text(raw.opt("text"));AiJson.integer(raw.opt("timeout"),0,600000);for(String key:new String[]{"match","source","failure"})AiJson.string(raw.opt(key));for(String key:new String[]{"waitForever","retryForever","immediate"})AiJson.bool(raw.opt(key));if(type.equals("TEXT_BRANCH")){AiJson.integer(raw.opt("found"),0,count);AiJson.integer(raw.opt("missing"),0,count);}if("ACTION_RETRY".equals(raw.opt("failure")))AiJson.integer(raw.opt("recoveryTarget"),1,count);}
        if(type.equals("GESTURE")){JSONArray strokes=AiJson.array(raw.opt("strokes"));if(strokes.length()<1||strokes.length()>20)throw AiJson.error("녹화 경로 개수");for(int i=0;i<strokes.length();i++){JSONObject stroke=AiJson.obj(strokes.opt(i));AiJson.keys(stroke,"start","duration","points");int start=AiJson.integer(stroke.opt("start"),0,58999),duration=AiJson.integer(stroke.opt("duration"),1,59000);if(start+duration>59000)throw AiJson.error("녹화 경로 시간");JSONArray points=AiJson.array(stroke.opt("points"));if(points.length()<1||points.length()>512)throw AiJson.error("녹화 경로 점 개수");for(int j=0;j<points.length();j++){JSONArray point=AiJson.array(points.opt(j));if(point.length()!=2)throw AiJson.error("녹화 좌표 형식");AiJson.coordinate(point.opt(0));AiJson.coordinate(point.opt(1));}}}
        Step result=new Step(AiJson.copy(raw));result.validate(count);return result;
    }
    private void validateBody(JSONArray body,int depth){
        if(body.length()<1||depth>4)throw AiJson.error("빈 계획 또는 중첩 상한");
        for(int i=0;i<body.length();i++){if(++nodes>128)throw AiJson.error("계획 노드 상한");JSONObject node=AiJson.obj(body.opt(i));String op=string(node,"op");
            if(AiPlanParser.NO_ARGS.contains(op))AiJson.keys(node,"op");
            else if(op.equals("WAIT")){AiJson.keys(node,"op","duration_ms");AiJson.integer(node.opt("duration_ms"),1,600000);}
            else if(op.equals("INPUT")){AiJson.keys(node,"op","text");AiJson.text(node.opt("text"));}
            else if(AiPlanParser.POINTS.contains(op)){AiJson.keys(node,"op","point_ref");JSONObject point=AiJson.obj(reference("points",node.opt("point_ref")));AiJson.keys(point,"x","y");AiJson.coordinate(point.opt("x"));AiJson.coordinate(point.opt("y"));}
            else if(AiPlanParser.GESTURES.contains(op)){AiJson.keys(node,"op","gesture_ref");JSONObject gesture=AiJson.obj(reference("gestures",node.opt("gesture_ref")));if(!op.equals(gesture.opt("type")))throw AiJson.error("제스처 유형 불일치");JSONObject payload=AiJson.obj(gesture.opt("payload"));AiSchemaSnapshots.step(payload,1);if(!op.equals(payload.opt("type")))throw AiJson.error("제스처 payload 유형 불일치");}
            else if(op.equals("APP")){AiJson.keys(node,"op","app_ref");String name=AiJson.string(reference("apps",node.opt("app_ref")));if(!name.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+"))throw AiJson.error("앱 패키지");}
            else if(AiPlanParser.TEXT.contains(op))validateText(node,op);
            else if(op.equals("IF")){AiJson.keys(node,"op","text","test","then","else");AiJson.text(node.opt("text"));if(!Arrays.asList("PRESENT","ABSENT").contains(node.opt("test")))throw AiJson.error("조건 종류");validateBody(AiJson.array(node.opt("then")),depth+1);validateBody(AiJson.array(node.opt("else")),depth+1);}
            else if(op.equals("REPEAT")){AiJson.keys(node,"op","count","body");AiJson.integer(node.opt("count"),1,1000);validateBody(AiJson.array(node.opt("body")),depth+1);}
            else if(op.equals("CALL")){AiJson.keys(node,"op","macro_ref");schemaMacroReference(node.opt("macro_ref"));}
            else if(op.equals("GOTO")){AiJson.keys(node,"op","target");AiJson.integer(node.opt("target"),1,body.length());if(depth!=0)throw AiJson.error("중첩 GOTO 미지원");}
            else if(op.equals("WATCH")){if(depth!=0||body.length()!=1||!"WATCH".equals(context.opt("planning_mode")))throw AiJson.error("목록 탐지는 목록 모드의 독립 계획만 허용합니다");validateWatch(node);}
            else throw AiJson.error("알 수 없는 동작: "+op);
        }
    }
    private void validateText(JSONObject node,String op){
        AiJson.keys(node,"op","text","match","region_ref","timeout_ms","wait_forever","missing");AiJson.text(node.opt("text"));if(!"EXACT".equals(node.opt("match")))throw AiJson.error("현재 모델은 정확히 일치만 지원합니다");if(!AiJson.nil(node.opt("region_ref")))region(node.opt("region_ref"));AiJson.integer(node.opt("timeout_ms"),0,600000);boolean wait=AiJson.bool(node.opt("wait_forever"));if(wait&&op.equals("TEXT_CHECK"))throw AiJson.error("글자 확인의 계속 기다리기 미지원");JSONObject missing=AiJson.obj(node.opt("missing"));AiJson.keys(missing,"policy","retries","forever","action_ref");String policy=string(missing,"policy");if(!AiPlanParser.POLICIES.contains(policy))throw AiJson.error("미발견 정책");int retries=AiJson.integer(missing.opt("retries"),0,100);boolean forever=AiJson.bool(missing.opt("forever")),retry=!policy.equals("STOP")&&!policy.equals("SKIP");if((forever||retries>0)&&!retry||retry&&op.equals("TEXT_CHECK")||wait&&retry)throw AiJson.error("계속 기다리기·재시도 정책 충돌");if(policy.equals("ACTION_RETRY")){JSONObject action=AiJson.obj(reference("actions",missing.opt("action_ref")));String type=AiJson.string(action.opt("type"));if(!(AiPlanParser.NO_ARGS.contains(type)||AiPlanParser.POINTS.contains(type)||AiPlanParser.GESTURES.contains(type)||type.equals("WAIT")||type.equals("INPUT")||type.equals("APP"))||type.equals("STOP"))throw AiJson.error("일반 복구 동작 필요");AiSchemaSnapshots.step(action,1);}else if(!AiJson.nil(missing.opt("action_ref")))throw AiJson.error("불필요한 복구 참조");
    }
    private void validateWatch(JSONObject node){
        AiJson.keys(node,"op","text","macro_ref","repetitions","cooldown_ms","interval_ms","region_refs","entry");AiJson.text(node.opt("text"));schemaMacroReference(node.opt("macro_ref"));AiJson.integer(node.opt("repetitions"),1,100000);AiJson.integer(node.opt("cooldown_ms"),0,86400000);int interval=AiJson.integer(node.opt("interval_ms"),1000,86400000);if(interval%1000!=0)throw AiJson.error("새로고침 간격은 정수 초");JSONArray names=AiJson.array(node.opt("region_refs"));if(names.length()>6)throw AiJson.error("우선순위 영역 상한");HashSet<String> seen=new HashSet<>();ArrayList<WatchArea> areas=new ArrayList<>();for(int i=0;i<names.length();i++){String name=AiJson.string(names.opt(i));if(!seen.add(name))throw AiJson.error("중복 영역");WatchArea area=region(name);for(WatchArea previous:areas)if(area.overlaps(previous))throw AiJson.error("목록 영역 겹침");areas.add(area);}if(!AiJson.nil(node.opt("entry"))){JSONObject entry=AiJson.obj(node.opt("entry"));AiJson.keys(entry,"text","region_ref","timeout_ms","reject_text","failure","back_count");AiJson.text(entry.opt("text"));if(!AiJson.nil(entry.opt("region_ref")))region(entry.opt("region_ref"));AiJson.integer(entry.opt("timeout_ms"),100,60000);if(!AiJson.nil(entry.opt("reject_text")))AiJson.text(entry.opt("reject_text"));if(!"BACK_TO_FIRST".equals(entry.opt("failure"))||AiJson.integer(entry.opt("back_count"),1,1)!=1)throw AiJson.error("진입 실패 복귀");}
    }
    private String source(){String source=AiJson.string(context.opt("recognition_source"));if(!Arrays.asList("AUTO","ACCESS","OCR").contains(source))throw AiJson.error("현재 인식 방식 미확인");return source;}
    private static Step simple(String type){Step step=new Step(type);step.after=0;step.repeat=1;return step;}
    private static Step label(){Step step=simple("WAIT");step.enabled=false;step.duration=1;return step;}
    private int add(Step step){if(output.steps.size()>=500)throw AiJson.error("전체 실행 동작이 500개를 넘습니다. 일부 실행은 하지 않습니다");output.steps.add(step);return output.steps.size();}
    private void emitBody(JSONArray body,boolean top){int[] entries=new int[body.length()];for(int i=0;i<body.length();i++){entries[i]=output.steps.size()+1;emit(AiJson.obj(body.opt(i)));}if(top)for(Jump jump:jumps)jump.step.target=entries[jump.target-1];}
    private void emit(JSONObject node){String op=string(node,"op");
        if(op.equals("REPEAT")){int count=AiJson.integer(node.opt("count"),1,1000);JSONArray inner=AiJson.array(node.opt("body"));Step compact=compact(inner,count);if(compact!=null){add(compact);return;}for(int i=0;i<count;i++)emitBody(inner,false);return;}
        if(op.equals("IF")){Step branch=simple("TEXT_BRANCH");branch.text=string(node,"text");branch.match="EXACT";branch.ignoreSpaces=false;branch.ignoreCase=false;branch.source=source();add(branch);int yes=output.steps.size()+1;emitBody(AiJson.array(node.opt("then")),false);Step jump=simple("GOTO");add(jump);int no=output.steps.size()+1;emitBody(AiJson.array(node.opt("else")),false);int join=add(label());jump.target=join;boolean present="PRESENT".equals(node.opt("test"));branch.found=present?yes:no;branch.missing=present?no:yes;return;}
        if(op.equals("CALL")){Macro m=macroReference(node.opt("macro_ref"));if(m.rounds!=1||m.startAt!=1||m.startDelay!=0||m.maxMinutes!=0||m.gap!=0||m.adaptiveWait)throw AiJson.error("복합 CALL은 반복1·시작1·시작대기0·제한없음·반복간0·글자맞춤끔인 매크로만 연결할 수 있습니다");int offset=output.steps.size();for(Step original:m.steps){Step step=new Step(original.json());if(step.type.equals("GOTO"))step.target+=offset;if(step.type.equals("TEXT_BRANCH")){if(step.found>0)step.found+=offset;if(step.missing>0)step.missing+=offset;}if(step.failure.equals("ACTION_RETRY"))step.recoveryTarget+=offset;add(step);}return;}
        Step step=atomic(node);if(op.equals("GOTO"))jumps.add(new Jump(step,AiJson.integer(node.opt("target"),1,128)));add(step);
    }
    private Step compact(JSONArray body,int multiplier){if(body.length()!=1)return null;JSONObject node=AiJson.obj(body.opt(0));String op=string(node,"op");if(op.equals("REPEAT")){long product=(long)multiplier*AiJson.integer(node.opt("count"),1,1000);if(product>1000)return null;return compact(AiJson.array(node.opt("body")),(int)product);}if(op.equals("CALL")||op.equals("IF")||op.equals("WATCH")||op.equals("GOTO")||op.equals("STOP")||op.startsWith("TEXT_"))return null;Step step=atomic(node);if(step.after!=0||step.afterJitter!=0||step.repeat>1000/multiplier)return null;step.repeat*=multiplier;return step;}
    private Step atomic(JSONObject node){String op=string(node,"op");Step step=simple(op);
        if(op.equals("WAIT"))step.duration=AiJson.integer(node.opt("duration_ms"),1,600000);
        else if(op.equals("INPUT"))step.text=AiJson.string(node.opt("text"));
        else if(AiPlanParser.POINTS.contains(op)){JSONObject point=AiJson.obj(reference("points",node.opt("point_ref")));step.x=AiJson.coordinate(point.opt("x"));step.y=AiJson.coordinate(point.opt("y"));}
        else if(AiPlanParser.GESTURES.contains(op)){JSONObject gesture=AiJson.obj(reference("gestures",node.opt("gesture_ref")));step=snapshot(AiJson.obj(gesture.opt("payload")),1);if(!step.enabled)throw AiJson.error("사용 안 함으로 등록된 제스처는 실행하지 않습니다");}
        else if(op.equals("APP"))step.packageName=AiJson.string(reference("apps",node.opt("app_ref")));
        else if(AiPlanParser.TEXT.contains(op)){step.text=string(node,"text");step.match="EXACT";step.ignoreSpaces=false;step.ignoreCase=false;step.source=source();step.timeout=AiJson.integer(node.opt("timeout_ms"),0,600000);step.waitForever=AiJson.bool(node.opt("wait_forever"));if(!AiJson.nil(node.opt("region_ref")))region(node.opt("region_ref")).apply(step);JSONObject missing=AiJson.obj(node.opt("missing"));step.failure=string(missing,"policy");step.retries=AiJson.integer(missing.opt("retries"),0,100);step.retryForever=AiJson.bool(missing.opt("forever"));if(step.failure.equals("ACTION_RETRY")){Step action=snapshot(AiJson.obj(reference("actions",missing.opt("action_ref"))),1);if(!action.enabled||!action.recoveryAction())throw AiJson.error("사용 가능한 일반 복구 동작 필요");recoveries.add(new Recovery(step,action));}}
        return step;
    }
    private void finishRecoveries(){if(recoveries.isEmpty())return;Step skip=simple("GOTO");add(skip);for(Recovery recovery:recoveries){recovery.owner.recoveryTarget=add(recovery.action);}skip.target=add(label());}
    private Compiled watch(JSONObject node,JSONObject plan){JSONObject raw=AiJson.obj(context.opt("watch_settings"));for(String key:new String[]{"intervalSeconds","repetitions","entryMs","settleMs","returnMs","backCount","maxMinutes","startMs","repeatGapMs","beforeBackMs","rearmMs","cooldownMs"})AiJson.integer(raw.opt(key),0,86400000);for(String key:new String[]{"entryCheck","preventDuplicate","tapFound","waitForRefresh"})AiJson.bool(raw.opt(key));for(String key:new String[]{"macroName","rejectText","targetPackage","direction"})AiJson.string(raw.opt(key));snapshot(AiJson.obj(raw.opt("rule")),1);if(AiJson.bool(raw.opt("entryCheck")))snapshot(AiJson.obj(raw.opt("entryRule")),1);JSONArray rawAreas=AiJson.array(raw.opt("priorityAreas"));for(int i=0;i<rawAreas.length();i++){JSONObject r=AiJson.obj(rawAreas.opt(i));AiJson.keys(r,"left","top","right","bottom");for(String key:new String[]{"left","top","right","bottom"})AiJson.coordinate(r.opt(key));}
        WatchConfig watch=new WatchConfig(AiJson.copy(raw));watch.rule.text=string(node,"text");watch.rule.source=source();watch.rule.match="EXACT";watch.rule.ignoreSpaces=false;watch.rule.ignoreCase=false;watch.macroName=string(node,"macro_ref");watch.repetitions=AiJson.integer(node.opt("repetitions"),1,100000);watch.cooldownMs=AiJson.integer(node.opt("cooldown_ms"),0,86400000);watch.intervalSeconds=AiJson.integer(node.opt("interval_ms"),1000,86400000)/1000;watch.priorityAreas.clear();JSONArray areas=AiJson.array(node.opt("region_refs"));for(int i=0;i<areas.length();i++)watch.priorityAreas.add(region(areas.opt(i)));if(areas.length()==0){watch.rule.left=0;watch.rule.top=0;watch.rule.right=100;watch.rule.bottom=100;}
        watch.entryCheck=!AiJson.nil(node.opt("entry"));if(watch.entryCheck){JSONObject entry=AiJson.obj(node.opt("entry"));watch.entryRule=simple("TEXT_WAIT");watch.entryRule.text=string(entry,"text");watch.entryRule.match="EXACT";watch.entryRule.source=source();watch.entryRule.ignoreSpaces=false;watch.entryRule.ignoreCase=false;watch.entryRule.timeout=AiJson.integer(entry.opt("timeout_ms"),100,60000);if(!AiJson.nil(entry.opt("region_ref")))region(entry.opt("region_ref")).apply(watch.entryRule);watch.rejectText=AiJson.nil(entry.opt("reject_text"))?"":string(entry,"reject_text");}
        Macro macro=macroReference(node.opt("macro_ref"));watch.validate();watch.prepareMacro(macro);return new Compiled(macro,watch,plan,context);
    }
    public static String support(){StringBuilder text=new StringBuilder();for(int i=0;i<Step.TYPES.length;i++){text.append(Step.NAMES[i]).append(" · ").append(Step.TYPES[i].equals("GOTO")?"타입 검증 지원 · 학습 자료 없음":Step.TYPES[i].equals("TEXT_BRANCH")?"있음/없음 조건 지원":"등록 참조 또는 명령 계획 지원").append('\n');}return text.append("정확히 일치 지원 · 포함/정규식의 새 AI 출력과 지정 복구 동작 학습은 미지원입니다. 기존 수동 기능은 그대로 사용할 수 있습니다.").toString();}
}
