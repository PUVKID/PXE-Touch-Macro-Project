package kr.yunseok.touchmacro;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.List;

/** 구조 토큰과 선택된 원문 구간만 AST로 바꿉니다. 자연어 규칙 파서는 아닙니다. */
public final class AiPlanParser {
    public static final String[] TOKENS={"PAD","BOS","EOS","PLAN","END","COPY","MS","SEC","MIN","HOUR","IF_PRESENT","IF_ABSENT","THEN","ELSE","END_IF","REPEAT","END_REPEAT","CALL","WATCH","REJECT","FOREVER","IN_REGION","WAIT_FOREVER","WATCH_REGION","ENTRY","ENTRY_ALL","ENTRY_REGION","ENTRY_TIMEOUT","REJECT_TEXT","NO_REJECT","BACK_TO_FIRST","NO_ENTRY","END_WATCH","DEFAULT_COOLDOWN","DEFAULT_INTERVAL","DEFAULT_ENTRY_TIMEOUT","ENTRY_DEFAULT_REGION","TAP","DOUBLE","LONG","SWIPE","WAIT","TEXT_TAP","TEXT_WAIT","TEXT_BRANCH","BACK","HOME","RECENTS","INPUT","APP","GOTO","STOP","CURVE","PINCH_IN","PINCH_OUT","MULTI_TAP","GESTURE","TEXT_CHECK","NOTIFICATIONS","F_STOP","F_SKIP","F_BACK_RETRY","F_RETRY","F_SCROLL_RETRY","F_PULL_RETRY","F_ACTION_RETRY","NEGATED","AMBIGUOUS","MISSING_REFERENCE","UNSUPPORTED","INCOMPLETE","LIMIT"};
    static final List<String> NO_ARGS=Arrays.asList("BACK","HOME","RECENTS","STOP","NOTIFICATIONS");
    static final List<String> POINTS=Arrays.asList("TAP","DOUBLE","LONG");
    static final List<String> GESTURES=Arrays.asList("SWIPE","CURVE","PINCH_IN","PINCH_OUT","MULTI_TAP","GESTURE");
    static final List<String> TEXT=Arrays.asList("TEXT_TAP","TEXT_WAIT","TEXT_CHECK");
    static final List<String> POLICIES=Arrays.asList("STOP","SKIP","BACK_RETRY","RETRY","SCROLL_RETRY","PULL_RETRY","ACTION_RETRY");
    static final List<String> REASONS=Arrays.asList("NEGATED","AMBIGUOUS","MISSING_REFERENCE","UNSUPPORTED","INCOMPLETE","LIMIT");
    private final List<Object> tokens;
    private final String source;
    private final JSONObject context;
    private int position;
    private AiPlanParser(List<Object> tokens,String source,JSONObject context){this.tokens=tokens;this.source=source;this.context=context;}
    public static JSONObject parse(List<Object> tokens,String source,JSONObject context) {
        AiTokenizer.validUnicode(source);if(tokens==null||tokens.isEmpty()||tokens.size()>512||source.codePointCount(0,source.length())>4096)throw AiJson.error("계획 상한");
        AiPlanParser parser=new AiPlanParser(tokens,source,context);Object first=parser.pop();JSONObject plan;
        if("REJECT".equals(first)){String reason=parser.word();if(!REASONS.contains(reason))throw AiJson.error("알 수 없는 거절 사유");plan=AiJson.object("schema","pxe.plan.v1","status","reject","reason",reason);}
        else if("PLAN".equals(first)){JSONArray body=parser.nodes(Arrays.asList("END"),0);parser.expect("END");plan=AiJson.object("schema","pxe.plan.v1","status","ready","body",body);}
        else throw AiJson.error("계획 시작 토큰 누락");
        parser.expect("EOS");if(parser.position!=tokens.size())throw AiJson.error("종료 뒤 토큰");return AiPlanCompiler.validateAst(plan,context);
    }
    private Object pop(){if(position>=tokens.size())throw AiJson.error("잘린 계획");return tokens.get(position++);}
    private String word(){return AiJson.string(pop());}
    private void expect(String value){if(!value.equals(pop()))throw AiJson.error("필수 토큰 누락: "+value);}
    private boolean next(String value){return position<tokens.size()&&value.equals(tokens.get(position));}
    private String copy(){JSONObject value=AiJson.obj(pop());AiJson.keys(value,"token","start","end");if(!"COPY".equals(value.opt("token")))throw AiJson.error("원문 복사 토큰 필요");int size=source.codePointCount(0,source.length()),start=AiJson.integer(value.opt("start"),0,size),end=AiJson.integer(value.opt("end"),start+1,size);return AiJson.text(AiTokenizer.substring(source,start,end));}
    private int number(){return AiNumbers.integer(copy());}
    private long duration(){String value=copy();return AiNumbers.duration(value,word());}
    private long timeDefault(String token,String key,int lo,int hi){if(next(token)){pop();return AiJson.integer(AiJson.obj(context.opt("defaults")).opt(key),lo,hi);}return duration();}
    private JSONArray nodes(List<String> stops,int depth){
        if(depth>4)throw AiJson.error("중첩 한도");JSONArray result=new JSONArray();
        while(position<tokens.size()&&tokens.get(position) instanceof String&&!stops.contains(tokens.get(position))){String op=word();JSONObject node;
            if(NO_ARGS.contains(op))node=AiJson.object("op",op);
            else if("WAIT".equals(op))node=AiJson.object("op",op,"duration_ms",duration());
            else if("INPUT".equals(op))node=AiJson.object("op",op,"text",copy());
            else if(POINTS.contains(op))node=AiJson.object("op",op,"point_ref",copy());
            else if(GESTURES.contains(op))node=AiJson.object("op",op,"gesture_ref",copy());
            else if("APP".equals(op))node=AiJson.object("op",op,"app_ref",copy());
            else if(TEXT.contains(op)){
                String text=copy(),policy=word();if(!policy.startsWith("F_")||!POLICIES.contains(policy.substring(2)))throw AiJson.error("미발견 정책 누락");policy=policy.substring(2);
                int retries=0;boolean forever=false;String action=null;
                if(!policy.equals("STOP")&&!policy.equals("SKIP")){if(next("FOREVER")){pop();forever=true;}else retries=number();if(policy.equals("ACTION_RETRY"))action=copy();}
                String region=null;if(next("IN_REGION")){pop();region=copy();}boolean wait=false;if(next("WAIT_FOREVER")){pop();wait=true;}
                node=AiJson.object("op",op,"text",text,"match","EXACT","region_ref",region,"timeout_ms",3000,"wait_forever",wait,"missing",AiJson.object("policy",policy,"retries",retries,"forever",forever,"action_ref",action));
            }else if(Arrays.asList("IF_PRESENT","IF_ABSENT","TEXT_BRANCH").contains(op)){
                String text=copy();expect("THEN");JSONArray yes=nodes(Arrays.asList("ELSE"),depth+1);expect("ELSE");JSONArray no=nodes(Arrays.asList("END_IF"),depth+1);expect("END_IF");node=AiJson.object("op","IF","text",text,"test",op.equals("IF_ABSENT")?"ABSENT":"PRESENT","then",yes,"else",no);
            }else if("REPEAT".equals(op)){int count=number();JSONArray inner=nodes(Arrays.asList("END_REPEAT"),depth+1);expect("END_REPEAT");node=AiJson.object("op",op,"count",count,"body",inner);}
            else if("CALL".equals(op))node=AiJson.object("op",op,"macro_ref",copy());
            else if("GOTO".equals(op))node=AiJson.object("op",op,"target",number());
            else if("WATCH".equals(op)){
                String text=copy(),macro=copy();int count=number();long cooldown=timeDefault("DEFAULT_COOLDOWN","watch_cooldown_ms",0,86400000),interval=timeDefault("DEFAULT_INTERVAL","watch_interval_ms",1000,86400000);JSONArray regions=new JSONArray();while(next("WATCH_REGION")){pop();regions.put(copy());}
                Object entry=null;String option=word();
                if(option.equals("ENTRY")){String entryText=copy(),regionOption=word();Object region;
                    if(regionOption.equals("ENTRY_REGION"))region=copy();else if(regionOption.equals("ENTRY_ALL"))region=null;else if(regionOption.equals("ENTRY_DEFAULT_REGION"))region=regions.length()>0?regions.opt(0):null;else throw AiJson.error("진입 영역 누락");
                    expect("ENTRY_TIMEOUT");long timeout=timeDefault("DEFAULT_ENTRY_TIMEOUT","entry_timeout_ms",100,60000);String rejection=word(),reject;
                    if(rejection.equals("REJECT_TEXT"))reject=copy();else if(rejection.equals("NO_REJECT"))reject=null;else throw AiJson.error("진입 거절 설정 누락");expect("BACK_TO_FIRST");entry=AiJson.object("text",entryText,"region_ref",region,"timeout_ms",timeout,"reject_text",reject,"failure","BACK_TO_FIRST","back_count",1);
                }else if(!option.equals("NO_ENTRY"))throw AiJson.error("진입 확인 설정 누락");expect("END_WATCH");node=AiJson.object("op",op,"text",text,"macro_ref",macro,"repetitions",count,"cooldown_ms",cooldown,"interval_ms",interval,"region_refs",regions,"entry",entry);
            }else throw AiJson.error("알 수 없는 구조 토큰: "+op);
            result.put(node);
        }
        return result;
    }
}
