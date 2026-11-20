package kr.yunseok.touchmacro;
import org.json.JSONArray;
import org.json.JSONObject;

/** 사용자 AST를 보여 주며 컴파일 연결용 동작은 사용자 명령으로 나열하지 않습니다. */
final class AiPlanPreview {
    private final JSONObject context;
    private final StringBuilder out=new StringBuilder();
    private AiPlanPreview(JSONObject context){this.context=context;}
    static String quote(Object value){return AiTokenizer.quote(AiJson.string(value)).replace("\u0085","\\u0085").replace("\u2028","\\u2028").replace("\u2029","\\u2029");}
    static String time(int value){return Times.durationLabel(value);}
    static String render(JSONObject plan,JSONObject context,Macro macro,WatchConfig watch){AiPlanPreview preview=new AiPlanPreview(context);preview.out.append("전체 명령 계획\n");preview.body(AiJson.array(plan.opt("body")),"");if(watch!=null)preview.watch(watch);else preview.settings(macro,"계획 실행 설정");String selected=AiJson.nil(context.opt("execution_target"))?"":AiJson.string(context.opt("execution_target"));try{AiStartGate gate=watch==null?AiStartGate.macro(macro,selected,"kr.yunseok.touchmacro"):AiStartGate.watch(watch,selected,"kr.yunseok.touchmacro");preview.line("","실행 대상 "+quote(gate.target()));preview.line("",gate.engineAppFirst()?"첫 앱 실행 명령을 엔진에서 그대로 실행합니다. 별도 앱 열기는 추가하지 않습니다.":"선택한 앱을 준비 동작으로 한 번 엽니다. 매크로 반복 수에는 더하지 않습니다.");preview.line("","새 대상 창이 확인된 뒤에만 계획 입력을 시작합니다.");}catch(IllegalArgumentException error){preview.line("","실행할 외부 앱 선택이 필요합니다. 미리보기만으로 실행하지 않습니다.");}return preview.out.toString();}
    private void line(String indent,String text){out.append(indent).append(text).append('\n');}
    private Object reference(String kind,Object name){return AiJson.obj(context.opt(kind)).opt(AiJson.string(name));}
    private void body(JSONArray body,String indent){
        for(int i=0;i<body.length();i++){JSONObject node=AiJson.obj(body.opt(i));String op=AiJson.string(node.opt("op")),prefix=indent+(i+1)+". ";
            if(op.equals("IF")){line(prefix,quote(node.opt("text"))+("ABSENT".equals(node.opt("test"))?"가 없으면":"가 있으면"));body(AiJson.array(node.opt("then")),indent+"    ");line(indent+"   ","그 밖에는");body(AiJson.array(node.opt("else")),indent+"    ");}
            else if(op.equals("REPEAT")){line(prefix,node.opt("count")+"회 반복");body(AiJson.array(node.opt("body")),indent+"    ");}
            else if(op.equals("CALL")){line(prefix,"매크로 "+quote(node.opt("macro_ref"))+" 실행");Macro saved=new Macro(AiJson.obj(reference("macros",node.opt("macro_ref"))));settings(saved,"저장된 실행 설정");for(int j=0;j<saved.steps.size();j++)line(indent+"    ",(j+1)+"번 · "+step(saved.steps.get(j)));}
            else if(op.equals("WATCH")){line(prefix,"목록에서 "+quote(node.opt("text"))+" 찾기");JSONArray regions=AiJson.array(node.opt("region_refs"));line(indent+"   ",regions.length()==0?"전체 화면":"우선순위 영역 "+names(regions));line(indent+"   ","매크로 "+quote(node.opt("macro_ref"))+"를 "+node.opt("repetitions")+"회 실행 · 완료 후 "+time(((Number)node.opt("cooldown_ms")).intValue())+" 대기 · "+time(((Number)node.opt("interval_ms")).intValue())+"마다 갱신");if(!AiJson.nil(node.opt("entry"))){JSONObject entry=AiJson.obj(node.opt("entry"));line(indent+"   ","진입 확인 "+quote(entry.opt("text"))+" · "+time(((Number)entry.opt("timeout_ms")).intValue())+" · "+(AiJson.nil(entry.opt("region_ref"))?"전체 화면":quote(entry.opt("region_ref"))));if(!AiJson.nil(entry.opt("reject_text")))line(indent+"   ","먼저 "+quote(entry.opt("reject_text"))+"가 있으면 되돌아가기");line(indent+"   ","확인 실패 시 뒤로 1회 후 1순위부터 다시 탐지");}else line(indent+"   ","진입 확인 끔");Macro saved=new Macro(AiJson.obj(reference("macros",node.opt("macro_ref"))));for(int j=0;j<saved.steps.size();j++)line(indent+"    ",(j+1)+"번 · "+step(saved.steps.get(j)));}
            else if(AiPlanParser.POINTS.contains(op)){Step s=new Step(op);JSONObject point=AiJson.obj(reference("points",node.opt("point_ref")));s.x=((Number)point.opt("x")).doubleValue();s.y=((Number)point.opt("y")).doubleValue();s.after=0;line(prefix,quote(node.opt("point_ref"))+" · "+step(s));}
            else if(AiPlanParser.GESTURES.contains(op))line(prefix,quote(node.opt("gesture_ref"))+" · "+step(new Step(AiJson.obj(AiJson.obj(reference("gestures",node.opt("gesture_ref"))).opt("payload")))));
            else if(op.equals("WAIT"))line(prefix,time(((Number)node.opt("duration_ms")).intValue())+" 기다리기");
            else if(op.equals("INPUT"))line(prefix,quote(node.opt("text"))+" 입력");
            else if(op.equals("APP"))line(prefix,"앱 "+quote(node.opt("app_ref"))+" 실행 ("+reference("apps",node.opt("app_ref"))+")");
            else if(AiPlanParser.TEXT.contains(op)){Step s=new Step(op);line(prefix,s.name()+" · "+quote(node.opt("text"))+" · 정확히 일치");line(indent+"   ",(AiJson.nil(node.opt("region_ref"))?"전체 화면":quote(node.opt("region_ref")))+" · "+(Boolean.TRUE.equals(node.opt("wait_forever"))?"나타날 때까지 계속 기다림":"검색 "+time(((Number)node.opt("timeout_ms")).intValue())));JSONObject missing=AiJson.obj(node.opt("missing"));String policy=AiJson.string(missing.opt("policy"));line(indent+"   ","없으면 "+policy(policy)+(AiJson.nil(missing.opt("action_ref"))?"":" "+quote(missing.opt("action_ref")))+(policy.equals("STOP")||policy.equals("SKIP")?"":Boolean.TRUE.equals(missing.opt("forever"))?" · 찾을 때까지 계속 재시도":" · 최대 "+missing.opt("retries")+"회 재시도"));if(policy.equals("ACTION_RETRY"))line(indent+"    ",step(new Step(AiJson.obj(reference("actions",missing.opt("action_ref"))))));}
            else if(op.equals("GOTO"))line(prefix,"계획의 "+node.opt("target")+"번 항목으로 이동");
            else line(prefix,new Step(op).name());
        }
    }
    private static String names(JSONArray names){StringBuilder out=new StringBuilder();for(int i=0;i<names.length();i++){if(i>0)out.append(" → ");out.append(quote(names.opt(i)));}return out.toString();}
    private static String policy(String value){switch(value){case "STOP":return "정지";case "SKIP":return "건너뛰기";case "BACK_RETRY":return "뒤로간 뒤 다시 찾기";case "RETRY":return "다시 찾기";case "SCROLL_RETRY":return "위로 쓸어 목록을 내린 뒤 다시 찾기";case "PULL_RETRY":return "아래로 당겨 새로고침 후 다시 찾기";default:return "지정 동작 후 다시 찾기";}}
    private static String step(Step s){
        String detail;
        if(s.type.equals("INPUT"))detail=quote(s.text);
        else if(s.textAction())detail=quote(s.text)+" · "+(s.match.equals("EXACT")?"정확히 일치":s.match.equals("CONTAINS")?"포함":"정규식")+" · "+s.source+" · "+(s.waitForever?"계속 기다림":"검색 "+time(s.timeout))+" · 없으면 "+policy(s.failure)+(s.retryPolicy()?s.retryForever?" · 계속 재시도":" · "+s.retries+"회 재시도":"")+(s.type.equals("TEXT_BRANCH")?" · 있음 "+s.found+"번 / 없음 "+s.missing+"번":"");
        else if(s.type.equals("APP"))detail=quote(s.packageName);
        else if(s.type.equals("WAIT"))detail=time(s.duration);
        else if(s.type.equals("LONG"))detail=s.x+", "+s.y+"% · "+time(s.duration);
        else if(s.type.equals("TAP")||s.type.equals("DOUBLE"))detail=s.x+", "+s.y+"%";
        else if(s.type.equals("SWIPE")||s.type.equals("CURVE"))detail=s.x+", "+s.y+" → "+s.x2+", "+s.y2+"% · "+time(s.duration)+(s.type.equals("CURVE")?" · 곡선점 "+s.cx+", "+s.cy+"%":"");
        else if(s.type.startsWith("PINCH"))detail="중심 "+s.x+", "+s.y+"% · 간격 "+s.x2+" ↔ "+s.y2+"% · "+time(s.duration);
        else if(s.type.equals("MULTI_TAP"))detail=quote(s.text)+" · "+time(s.duration);
        else if(s.type.equals("GESTURE")){StringBuilder paths=new StringBuilder(s.strokes.length()+"개 손가락 경로");int end=0;for(int i=0;i<s.strokes.length();i++){JSONObject stroke=AiJson.obj(s.strokes.opt(i));int start=((Number)stroke.opt("start")).intValue(),duration=((Number)stroke.opt("duration")).intValue();end=Math.max(end,start+duration);paths.append(" · ").append(i+1).append("번 시작 ").append(time(start)).append(" / 지속 ").append(time(duration));}detail=paths.append(" · 전체 ").append(time(end)).toString();}
        else if(s.type.equals("GOTO"))detail=s.target+"번 동작";else detail="";
        return (s.enabled?"":"[사용 안 함] ")+s.name()+(detail.isEmpty()?"":" · "+detail)+" · "+s.repeat+"회 · 동작 후 "+time(s.after)+(s.durationJitter>0?" · 동작시간 무작위 ±"+time(s.durationJitter):"")+(s.afterJitter>0?" · 대기 무작위 ±"+time(s.afterJitter):"");
    }
    private void settings(Macro macro,String title){line("",title+" · "+quote(macro.name));line("","전체 "+(macro.rounds==0?"무한":macro.rounds+"회")+" · 시작 "+macro.startAt+"번 · 시작 전 "+time(macro.startDelay)+" · 반복 사이 "+time(macro.gap)+" · 전체 제한 "+(macro.maxMinutes==0?"없음":macro.maxMinutes+"분")+" · 다음 글자 맞춤 "+(macro.adaptiveWait?"켬":"끔"));}
    private void watch(WatchConfig p){String selected=AiJson.nil(context.opt("execution_target"))?"":AiJson.string(context.opt("execution_target"));String target=selected.isEmpty()?p.targetPackage:selected;line("","목록 실행 설정 · 대상 "+(target.isEmpty()?"선택 필요":quote(target))+" · 중복 방지 "+(p.preventDuplicate?"켬":"끔"));line("","시작 전 "+time(p.startMs)+" · 진입 후 "+time(p.entryMs)+" · 반복 사이 "+time(p.repeatGapMs)+" · 뒤로가기 전 "+time(p.beforeBackMs)+" · 복귀 후 "+time(p.returnMs));line("","갱신 후 "+time(p.settleMs)+" · 재등장 확인 "+time(p.rearmMs)+" · 뒤로 "+p.backCount+"회 · 전체 제한 "+(p.maxMinutes==0?"없음":p.maxMinutes+"분"));line("",(p.direction.equals("PULL_DOWN")?"아래로 당겨 갱신":"위로 쓸어 목록 내림")+" · 목록 글자 터치 "+(p.tapFound?"켬":"끔")+" · 갱신까지 대기 "+(p.waitForRefresh?"켬":"끔"));}
}
