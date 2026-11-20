package kr.yunseok.touchmacro;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;

/** Python snapshots.py의 AST 참조 계약입니다. Android 실행 검증과 구분합니다. */
final class AiSchemaSnapshots {
    private AiSchemaSnapshots() { }
    static void step(JSONObject raw,int count){
        String op=AiJson.string(raw.opt("type"));if(!Arrays.asList(Step.TYPES).contains(op))throw AiJson.error("등록 동작 유형");AiJson.bool(raw.opt("enabled"));AiJson.integer(raw.opt("repeat"),1,1000);AiJson.integer(raw.opt("after"),0,600000);
        if(Arrays.asList("TAP","DOUBLE","LONG","SWIPE","CURVE","PINCH_IN","PINCH_OUT").contains(op))for(String key:new String[]{"x","y"})AiJson.coordinate(raw.opt(key));
        if(Arrays.asList("SWIPE","CURVE","PINCH_IN","PINCH_OUT").contains(op))for(String key:new String[]{"x2","y2"})AiJson.coordinate(raw.opt(key));
        if(op.equals("CURVE"))for(String key:new String[]{"cx","cy"})AiJson.coordinate(raw.opt(key));
        if(Arrays.asList("WAIT","LONG","SWIPE","CURVE","PINCH_IN","PINCH_OUT","MULTI_TAP","GESTURE").contains(op))AiJson.integer(raw.opt("duration"),1,op.equals("WAIT")?600000:59000);
        if(op.equals("PINCH_IN")||op.equals("PINCH_OUT")){double x=AiJson.coordinate(raw.opt("x"));for(String key:new String[]{"x2","y2"}){double distance=AiJson.coordinate(raw.opt(key));if(x-distance<0||x+distance>100)throw AiJson.error("핀치 좌표 범위");}}
        if(op.equals("MULTI_TAP")){String[] points=AiJson.string(raw.opt("text")).split(";",-1);if(points.length<1||points.length>20)throw AiJson.error("다중 터치 개수");for(String point:points){String[] pair=point.split(",",-1);if(pair.length!=2)throw AiJson.error("다중 터치 좌표");for(String number:pair)try{AiJson.coordinate(Double.parseDouble(number));}catch(NumberFormatException error){throw AiJson.error("다중 터치 좌표 수치");}}}
        if(op.equals("GESTURE")){JSONArray strokes=AiJson.array(raw.opt("strokes"));if(strokes.length()<1||strokes.length()>20)throw AiJson.error("녹화 경로 개수");for(int i=0;i<strokes.length();i++){JSONObject stroke=AiJson.obj(strokes.opt(i));AiJson.keys(stroke,"start","duration","points");int start=AiJson.integer(stroke.opt("start"),0,58999),duration=AiJson.integer(stroke.opt("duration"),1,59000);if(start+duration>59000)throw AiJson.error("녹화 경로 시간");JSONArray points=AiJson.array(stroke.opt("points"));if(points.length()<1||points.length()>512)throw AiJson.error("경로 점 개수");for(int j=0;j<points.length();j++){JSONArray point=AiJson.array(points.opt(j));if(point.length()!=2)throw AiJson.error("경로 점 형식");AiJson.coordinate(point.opt(0));AiJson.coordinate(point.opt(1));}}}
        if(op.equals("INPUT"))AiJson.string(raw.opt("text"));
        if(op.equals("APP")&&!AiJson.string(raw.opt("packageName")).matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+"))throw AiJson.error("앱 패키지");
        if(op.equals("GOTO"))AiJson.integer(raw.opt("target"),1,count);
        if(op.startsWith("TEXT_")){
            AiJson.text(raw.opt("text"));AiJson.integer(raw.opt("timeout"),0,600000);String match=AiJson.string(raw.opt("match")),source=AiJson.string(raw.opt("source")),failure=AiJson.string(raw.opt("failure"));if(!Arrays.asList("EXACT","CONTAINS","REGEX").contains(match)||!Arrays.asList("AUTO","ACCESS","OCR").contains(source)||!AiPlanParser.POLICIES.contains(failure))throw AiJson.error("등록 글자 옵션");if(match.equals("REGEX"))java.util.regex.Pattern.compile(AiJson.string(raw.opt("text")));
            boolean wait=AiJson.bool(raw.opt("waitForever")),retry=AiJson.bool(raw.opt("retryForever"));AiJson.bool(raw.opt("immediate"));boolean canWait=op.equals("TEXT_TAP")||op.equals("TEXT_WAIT");if(wait&&(!canWait||retry)||retry&&(!canWait||failure.equals("STOP")||failure.equals("SKIP")))throw AiJson.error("등록 계속 기다리기·재시도");if(failure.equals("ACTION_RETRY"))AiJson.integer(raw.opt("recoveryTarget"),1,count);if(op.equals("TEXT_BRANCH")){AiJson.integer(raw.opt("found"),0,count);AiJson.integer(raw.opt("missing"),0,count);}
        }
    }
    static void macro(JSONObject value){
        JSONArray steps=AiJson.array(value.opt("steps"));if(steps.length()<1||steps.length()>500)throw AiJson.error("저장 매크로 크기");AiJson.integer(value.opt("rounds"),0,100000);AiJson.integer(value.opt("gap"),0,600000);AiJson.integer(value.opt("startDelay"),0,600000);AiJson.integer(value.opt("maxMinutes"),0,1440);AiJson.integer(value.opt("startAt"),1,steps.length());boolean enabled=false;
        for(int i=0;i<steps.length();i++){JSONObject raw=AiJson.obj(steps.opt(i));step(raw,steps.length());enabled|=AiJson.bool(raw.opt("enabled"));if("ACTION_RETRY".equals(raw.opt("failure"))){JSONObject action=AiJson.obj(steps.opt(AiJson.integer(raw.opt("recoveryTarget"),1,steps.length())-1));String type=AiJson.string(action.opt("type"));if(!AiJson.bool(action.opt("enabled"))||type.startsWith("TEXT_")||type.equals("GOTO")||type.equals("STOP"))throw AiJson.error("등록 복구 동작 참조");}}
        if(!enabled)throw AiJson.error("실행할 저장 동작 없음");
    }
}
