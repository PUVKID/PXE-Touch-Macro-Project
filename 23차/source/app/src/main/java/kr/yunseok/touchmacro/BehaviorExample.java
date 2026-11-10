package kr.yunseok.touchmacro;

import org.json.*;
import java.util.*;

/** 직접 시범·교정의 원문과 비실행성 행동 사전을 보존합니다. */
public final class BehaviorExample {
    public enum Type {TEXT_TAP,BACK,WAIT_TEXT,FINISH,SCROLL_UP,SCROLL_DOWN}
    public static final int MAX_ACTIONS=500,MAX_EXAMPLES=4096;
    public final String id,actionId,provenance,supersedes;
    public final Observation before,after;
    public final boolean confirmed,discarded;

    public BehaviorExample(String id,Observation before,String actionId,Observation after,String provenance,String supersedes,boolean confirmed,boolean discarded){
        this.id=text(id,80,false);this.actionId=text(actionId,80,false);
        if(before==null||after==null)throw error("행동 전후 관측이 필요합니다");
        if(!before.binding.sameSession(after.binding)||after.binding.capturedAtMs<before.binding.capturedAtMs)throw error("시범의 작업·세션·대상·설정·시간 결합 오류");
        if(!Set.of("DEMONSTRATION","CORRECTION").contains(provenance))throw error("직접 시범·사용자 교정만 정답으로 허용합니다");
        if((supersedes!=null)!=provenance.equals("CORRECTION"))throw error("교정의 원 표본 관계가 필요합니다");
        this.before=before;this.after=after;this.provenance=provenance;this.supersedes=supersedes==null?null:text(supersedes,80,false);this.confirmed=confirmed;this.discarded=discarded;
    }
    public JSONObject json(){return object("id",id,"before",before.json(),"action_id",actionId,"after",after.json(),"provenance",provenance,"supersedes",supersedes,"confirmed",confirmed,"discarded",discarded);}
    public static BehaviorExample fromJson(JSONObject o){try{keys(o,"id","before","action_id","after","provenance","supersedes","confirmed","discarded");return new BehaviorExample(string(o.get("id")),Observation.fromJson(o.getJSONObject("before")),string(o.get("action_id")),Observation.fromJson(o.getJSONObject("after")),string(o.get("provenance")),nullable(o.get("supersedes")),bool(o.get("confirmed")),bool(o.get("discarded")));}catch(JSONException e){throw error("시범 JSON 형식 오류",e);}}

    /** 기기별 실시간 lease의 대체물이 아닌 표본 출처 정보입니다. */
    public static final class Binding {
        public final String taskId,sessionId,targetPackage;
        public final long settingsRevision,dataRevision,windowRevision,capturedAtMs;
        public final int width,height;
        public Binding(String taskId,String sessionId,String targetPackage,long settingsRevision,long dataRevision,long windowRevision,long capturedAtMs,int width,int height){
            this.taskId=text(taskId,80,false);this.sessionId=text(sessionId,80,false);this.targetPackage=text(targetPackage,255,false);
            if(!targetPackage.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+")||settingsRevision<0||dataRevision<0||windowRevision<0||capturedAtMs<0||width<1||height<1||width>32768||height>32768)throw error("관측 출처 범위 오류");
            this.settingsRevision=settingsRevision;this.dataRevision=dataRevision;this.windowRevision=windowRevision;this.capturedAtMs=capturedAtMs;this.width=width;this.height=height;
        }
        boolean sameSession(Binding b){return taskId.equals(b.taskId)&&sessionId.equals(b.sessionId)&&targetPackage.equals(b.targetPackage)&&settingsRevision==b.settingsRevision&&dataRevision==b.dataRevision;}
        JSONObject json(){return object("task_id",taskId,"session_id",sessionId,"target_package",targetPackage,"settings_revision",settingsRevision,"data_revision",dataRevision,"window_revision",windowRevision,"captured_at_ms",capturedAtMs,"width",width,"height",height);}
        static Binding fromJson(JSONObject o)throws JSONException{keys(o,"task_id","session_id","target_package","settings_revision","data_revision","window_revision","captured_at_ms","width","height");return new Binding(string(o.get("task_id")),string(o.get("session_id")),string(o.get("target_package")),integer(o.get("settings_revision"),0,Long.MAX_VALUE),integer(o.get("data_revision"),0,Long.MAX_VALUE),integer(o.get("window_revision"),0,Long.MAX_VALUE),integer(o.get("captured_at_ms"),0,Long.MAX_VALUE),(int)integer(o.get("width"),1,32768),(int)integer(o.get("height"),1,32768));}
    }
    public static final class Token {
        public final String raw,level;
        public final int left,top,right,bottom;
        public Token(String raw,int left,int top,int right,int bottom,String level){
            this.raw=text(raw,1024,true);if(raw.isEmpty()||left<0||top<0||right<=left||bottom<=top||right>32768||bottom>32768||!Set.of("ELEMENT","LINE","BLOCK").contains(level))throw error("OCR 후보 형식 오류");
            this.left=left;this.top=top;this.right=right;this.bottom=bottom;this.level=level;
        }
        long area(){return (long)(right-left)*(bottom-top);}
        boolean same(Token t){return raw.equals(t.raw)&&left==t.left&&top==t.top&&right==t.right&&bottom==t.bottom;}
        boolean contains(Token t){return left<=t.left&&top<=t.top&&right>=t.right&&bottom>=t.bottom;}
        JSONObject json(){return object("raw",raw,"left",left,"top",top,"right",right,"bottom",bottom,"level",level,"raw_start_cp",0,"raw_end_cp",raw.codePointCount(0,raw.length()));}
        static Token fromJson(JSONObject o)throws JSONException{keys(o,"raw","left","top","right","bottom","level","raw_start_cp","raw_end_cp");String raw=string(o.get("raw"));if(integer(o.get("raw_start_cp"),0,0)!=0||integer(o.get("raw_end_cp"),0,1024)!=raw.codePointCount(0,raw.length()))throw error("OCR 원문 전체 코드포인트 범위 오류");return new Token(raw,(int)integer(o.get("left"),0,32768),(int)integer(o.get("top"),0,32768),(int)integer(o.get("right"),0,32768),(int)integer(o.get("bottom"),0,32768),string(o.get("level")));}
    }
    public static final class Observation {
        public final Binding binding;
        public final List<Token> tokens;
        public final String previousActionId;
        private final double[] state;
        /** state의 각 원천·정규화는 BehaviorFeatures.STATE_NAMES와 해당 API 주석에 고정합니다. */
        public Observation(Binding binding,List<Token> tokens,String previousActionId,double[] state){
            if(binding==null||tokens==null||tokens.size()>256||state==null||state.length!=8)throw error("관측 크기 오류");
            for(Token token:tokens)if(token==null||token.right>binding.width||token.bottom>binding.height)throw error("화면 밖 OCR 후보");
            for(double value:state)if(!Double.isFinite(value)||value< -1||value>1)throw error("관측 특징 범위 오류");
            this.binding=binding;this.tokens=Collections.unmodifiableList(new ArrayList<>(tokens));this.previousActionId=text(previousActionId,80,true);this.state=state.clone();
        }
        public double[] state(){return state.clone();}
        public JSONObject json(){try{JSONArray rows=new JSONArray();for(Token token:tokens)rows.put(token.json());JSONArray values=new JSONArray();for(double value:state)values.put(value);return object("binding",binding.json(),"tokens",rows,"previous_action_id",previousActionId,"state",values);}catch(JSONException e){throw error("관측 JSON 생성 오류",e);}}
        public static Observation fromJson(JSONObject o){try{keys(o,"binding","tokens","previous_action_id","state");JSONArray a=o.getJSONArray("tokens"),v=o.getJSONArray("state");if(a.length()>256||v.length()!=8)throw error("관측 배열 크기 오류");List<Token> rows=new ArrayList<>();for(int i=0;i<a.length();i++)rows.add(Token.fromJson(a.getJSONObject(i)));double[] state=new double[8];for(int i=0;i<8;i++)state[i]=real(v.get(i));return new Observation(Binding.fromJson(o.getJSONObject("binding")),rows,string(o.get("previous_action_id")),state);}catch(JSONException e){throw error("관측 JSON 오류",e);}}
    }
    public static final class Action {
        public final String id,targetRaw;
        public final Type type;
        public final int timeoutMs;
        public final boolean waitForever;
        public Action(String id,Type type,String targetRaw,int timeoutMs,boolean waitForever){
            this.id=text(id,80,false);if(type==null)throw error("행동 종류 필요");boolean target=type==Type.TEXT_TAP||type==Type.WAIT_TEXT;
            if(target)text(targetRaw,1024,false);else if(targetRaw!=null)throw error("이 행동은 글자 payload를 받지 않습니다");
            if(timeoutMs<0||timeoutMs>600000||type!=Type.WAIT_TEXT&&(timeoutMs!=0||waitForever))throw error("대기 payload 범위 오류");
            this.type=type;this.targetRaw=targetRaw;this.timeoutMs=timeoutMs;this.waitForever=waitForever;
        }
        public JSONObject json(){return object("id",id,"type",type.name(),"target_raw",targetRaw,"timeout_ms",timeoutMs,"wait_forever",waitForever);}
        public static Action fromJson(JSONObject o){try{keys(o,"id","type","target_raw","timeout_ms","wait_forever");return new Action(string(o.get("id")),Type.valueOf(string(o.get("type"))),nullable(o.get("target_raw")),(int)integer(o.get("timeout_ms"),0,600000),bool(o.get("wait_forever")));}catch(JSONException e){throw error("행동 JSON 오류",e);}}
    }
    public static final class Registry {
        private final List<Action> actions;
        public Registry(){actions=List.of();}
        private Registry(List<Action> actions){this.actions=Collections.unmodifiableList(actions);}
        public int size(){return actions.size();}
        public Action get(int index){return actions.get(index);}
        public int indexOf(String id){for(int i=0;i<size();i++)if(actions.get(i).id.equals(id))return i;throw error("등록하지 않은 행동 ID");}
        public Registry append(Action action){if(action==null||size()>=MAX_ACTIONS)throw error("행동 사전 최대500개");for(Action old:actions)if(old.id.equals(action.id))throw error("중복 행동 ID");List<Action> result=new ArrayList<>(actions);result.add(action);return new Registry(result);}
        public JSONArray json(){try{JSONArray a=new JSONArray();for(int i=0;i<size();i++)a.put(get(i).json().put("index",i));return a;}catch(JSONException e){throw error("사전 JSON 생성 오류",e);}}
        public static Registry fromJson(JSONArray array){try{if(array.length()>MAX_ACTIONS)throw error("행동 사전 상한 초과");Registry result=new Registry();for(int i=0;i<array.length();i++){JSONObject row=new JSONObject(array.getJSONObject(i).toString());if(integer(row.get("index"),0,499)!=i)throw error("행동 사전 순서 변경");row.remove("index");result=result.append(Action.fromJson(row));}return result;}catch(JSONException e){throw error("사전 JSON 오류",e);}}
    }
    public static final class Replay {
        private final List<BehaviorExample> rows;
        public Replay(){rows=List.of();}
        private Replay(List<BehaviorExample> rows){this.rows=Collections.unmodifiableList(rows);}
        public List<BehaviorExample> all(){return rows;}
        public Replay append(BehaviorExample row){if(row==null||rows.size()>=MAX_EXAMPLES)throw error("시범 버퍼 상한 초과");BehaviorExample parent=null;for(BehaviorExample old:rows){if(old.id.equals(row.id))throw error("중복 표본 ID");if(old.id.equals(row.supersedes))parent=old;if(row.supersedes!=null&&row.confirmed&&!row.discarded&&old.confirmed&&!old.discarded&&row.supersedes.equals(old.supersedes))throw error("같은 부모의 상충 교정; 마지막 교정을 교정하세요");}
            if(row.supersedes!=null&&(parent==null||!parent.before.binding.taskId.equals(row.before.binding.taskId)))throw error("알 수 없거나 다른 작업의 교정 부모");
            List<BehaviorExample> result=new ArrayList<>(rows);result.add(row);return new Replay(result);}
        public List<BehaviorExample> active(String taskId){Set<String> replaced=new HashSet<>();for(BehaviorExample row:rows)if(row.confirmed&&!row.discarded&&row.supersedes!=null)replaced.add(row.supersedes);List<BehaviorExample> result=new ArrayList<>();for(BehaviorExample row:rows)if(row.confirmed&&!row.discarded&&!replaced.contains(row.id)&&row.before.binding.taskId.equals(taskId))result.add(row);return Collections.unmodifiableList(result);}
    }
    static JSONObject object(Object... pairs){try{JSONObject out=new JSONObject();for(int i=0;i<pairs.length;i+=2)out.put((String)pairs[i],pairs[i+1]==null?JSONObject.NULL:pairs[i+1]);return out;}catch(JSONException e){throw error("JSON 객체 생성 오류",e);}}
    static void keys(JSONObject o,String... keys){if(o==null)throw error("필수/미지원 JSON 필드");Set<String> actual=new HashSet<>();Iterator<String> names=o.keys();while(names.hasNext())actual.add(names.next());if(!actual.equals(new HashSet<>(Arrays.asList(keys))))throw error("필수/미지원 JSON 필드");}
    static String string(Object value){if(!(value instanceof String))throw error("문자열 자료형 필요");return (String)value;}
    static String nullable(Object value){return value==JSONObject.NULL?null:string(value);}
    static boolean bool(Object value){if(!(value instanceof Boolean))throw error("bool 자료형 필요");return (Boolean)value;}
    static long integer(Object value,long minimum,long maximum){if(!(value instanceof Integer||value instanceof Long||value instanceof Short||value instanceof Byte))throw error("엄격한 정수 자료형 필요");long v=((Number)value).longValue();if(v<minimum||v>maximum)throw error("정수 범위 초과");return v;}
    static double real(Object value){if(!(value instanceof Number))throw error("실수 자료형 필요");double v=((Number)value).doubleValue();if(!Double.isFinite(v))throw error("유한한 숫자 필요");return v;}
    static String text(String value,int maximum,boolean allowBlank){if(value==null||!allowBlank&&value.codePoints().allMatch(cp->Character.isWhitespace(cp)||Character.isSpaceChar(cp)||cp==0x85)||value.codePointCount(0,value.length())>maximum)throw error("문자열 길이·내용 범위 오류");for(int i=0;i<value.length();i++){char c=value.charAt(i);if(Character.isHighSurrogate(c)){if(i+1>=value.length()||!Character.isLowSurrogate(value.charAt(++i)))throw error("고립 surrogate 거절");}else if(Character.isLowSurrogate(c))throw error("고립 surrogate 거절");}return value;}
    static IllegalArgumentException error(String message){return new IllegalArgumentException(message);}
    static IllegalArgumentException error(String message,Throwable error){return new IllegalArgumentException(message,error);}
}
