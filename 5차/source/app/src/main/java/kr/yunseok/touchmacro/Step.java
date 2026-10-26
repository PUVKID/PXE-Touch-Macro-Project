package kr.yunseok.touchmacro;
import org.json.JSONObject;
public final class Step {
    public static final String[] TYPES = {"TAP","DOUBLE","LONG","SWIPE","WAIT","TEXT_TAP","TEXT_WAIT","TEXT_BRANCH","BACK","HOME","RECENTS","INPUT","APP","GOTO","STOP","CURVE","PINCH_IN","PINCH_OUT","MULTI_TAP","GESTURE","TEXT_CHECK","NOTIFICATIONS"};
    public static final String[] NAMES = {"좌표 터치","두 번 터치","길게 누르기","스와이프","대기","글자 찾아 터치","글자 나타날 때까지 대기","글자 조건 분기","뒤로가기","홈으로","최근 앱","문자 입력","앱 실행","동작 번호로 이동","매크로 정지","곡선 스와이프","두 손가락 축소","두 손가락 확대","동시 다중 터치","녹화 제스처","글자 인식 확인","알림 패널 열기"};
    public org.json.JSONArray strokes=new org.json.JSONArray();
    public double cx=50,cy=20,left=0,top=0,right=100,bottom=100,offsetX=0,offsetY=0;
    public int durationJitter=0,afterJitter=0;
    public String type="TAP", text="", match="CONTAINS", source="AUTO", failure="STOP", packageName="";
    public double x=50,y=50,x2=50,y2=20;
    public int duration=100,after=500,repeat=1,timeout=3000,retries=3,retryDelay=1000,found=0,missing=0,target=1,occurrence=1;
    public boolean ignoreSpaces=true,ignoreCase=true,enabled=true;
    public Step() {}
    public Step(String t){type=t;if("LONG".equals(t)) duration=800;if("SWIPE".equals(t)||"CURVE".equals(t)||t.startsWith("PINCH")) duration=450;if(t.startsWith("PINCH")){x2=5;y2=20;}if("MULTI_TAP".equals(t))text="25,50;75,50";}
    public Step(JSONObject o) {
        try{strokes=o.optJSONArray("strokes");strokes=strokes==null?new org.json.JSONArray():new org.json.JSONArray(strokes.toString());}catch(Exception e){throw new IllegalArgumentException("녹화 경로 형식을 확인하세요",e);}cx=o.optDouble("cx",50);cy=o.optDouble("cy",20);left=o.optDouble("left",0);top=o.optDouble("top",0);right=o.optDouble("right",100);bottom=o.optDouble("bottom",100);offsetX=o.optDouble("offsetX",0);offsetY=o.optDouble("offsetY",0);durationJitter=o.optInt("durationJitter",0);afterJitter=o.optInt("afterJitter",0);
        type=o.optString("type","TAP"); text=o.optString("text",""); match=o.optString("match","CONTAINS"); source=o.optString("source","AUTO"); failure=o.optString("failure","STOP");
        packageName=o.optString("packageName",""); x=o.optDouble("x",50); y=o.optDouble("y",50); x2=o.optDouble("x2",50); y2=o.optDouble("y2",20);
        duration=o.optInt("duration",100);after=o.optInt("after",500); repeat=o.optInt("repeat",1);timeout=o.optInt("timeout",3000);retries=o.optInt("retries",3);retryDelay=o.optInt("retryDelay",1000);
        found=o.optInt("found",0);missing=o.optInt("missing",0);target=o.optInt("target",1);occurrence=o.optInt("occurrence",1);
        ignoreSpaces=o.optBoolean("ignoreSpaces",true);ignoreCase=o.optBoolean("ignoreCase",true);enabled=o.optBoolean("enabled",true);
    }
    public JSONObject json() {
        JSONObject o=new JSONObject();
        try {o.put("strokes",strokes).put("cx",cx).put("cy",cy).put("left",left).put("top",top).put("right",right).put("bottom",bottom).put("offsetX",offsetX).put("offsetY",offsetY).put("durationJitter",durationJitter).put("afterJitter",afterJitter).put("type",type).put("text",text).put("match",match).put("source",source).put("failure",failure).put("packageName",packageName)
            .put("x",x).put("y",y).put("x2",x2).put("y2",y2).put("duration",duration).put("after",after).put("repeat",repeat)
            .put("timeout",timeout).put("retries",retries).put("retryDelay",retryDelay).put("found",found).put("missing",missing).put("target",target).put("occurrence",occurrence)
            .put("ignoreSpaces",ignoreSpaces).put("ignoreCase",ignoreCase).put("enabled",enabled);}catch(Exception e){throw new IllegalStateException(e);}
        return o;
    }
    public String name(){ for(int i=0;i<TYPES.length;i++)if(TYPES[i].equals(type)) return NAMES[i]; return type; }
    public boolean textAction(){return type.startsWith("TEXT_");}
    public String detail() {
        if(type.equals("GESTURE"))return strokes.length()+"개 손가락 경로";
        if(type.equals("MULTI_TAP"))return text;
        if(type.equals("CURVE"))return "곡선 · "+Times.label(duration);
        if(type.startsWith("PINCH"))return "중심 "+Math.round(x)+", "+Math.round(y)+"% · 간격 "+Math.round(x2)+" ↔ "+Math.round(y2)+"%";
        if(textAction()) return "“"+text+"” · "+("EXACT".equals(match)?"일치":"REGEX".equals(match)?"정규식":"포함")+("BACK_RETRY".equals(failure)?" · 없으면 뒤로 "+retries+"회":"")+(type.equals("TEXT_BRANCH")?" · 있음 "+(found==0?"다음":found)+" / 없음 "+(missing==0?"다음":missing):"");
        switch(type){case "TAP":case "DOUBLE":case "LONG":return Math.round(x)+"%, "+Math.round(y)+"%";case "SWIPE":return Math.round(x)+","+Math.round(y)+" → "+Math.round(x2)+","+Math.round(y2)+"%";case "WAIT":return Times.label(duration);case "INPUT":return text;case "APP":return packageName;case "GOTO":return target+"번으로 이동";default:return "완료 후 "+Times.label(after);}
    }
    public void validate(int count) {
        boolean known=false;for(String t:TYPES)if(t.equals(type))known=true;
        if(!known)throw new IllegalArgumentException("알 수 없는 동작");
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(x2)||!Double.isFinite(y2)||x<0||x>100||y<0||y>100||x2<0||x2>100||y2<0||y2>100)throw new IllegalArgumentException("좌표는 0~100%입니다");
        if(duration<1||duration>600000||after<0||after>600000||repeat<1||repeat>1000||timeout<0||timeout>600000||retries<0||retries>100||retryDelay<300||retryDelay>600000||occurrence<1||occurrence>100)throw new IllegalArgumentException("시간·반복 횟수 범위를 확인하세요");
        if((type.equals("LONG")||type.equals("SWIPE"))&&duration>59000)throw new IllegalArgumentException("길게 누르기·스와이프는 최대 59초입니다");
        if(textAction()&&text.trim().isEmpty())throw new IllegalArgumentException("찾을 글자를 입력하세요");
        if(!Double.isFinite(left)||!Double.isFinite(top)||!Double.isFinite(right)||!Double.isFinite(bottom)||left<0||top<0||right>100||bottom>100||left>=right||top>=bottom||!Double.isFinite(offsetX)||!Double.isFinite(offsetY)||Math.abs(offsetX)>50||Math.abs(offsetY)>50)throw new IllegalArgumentException("검색 영역·터치 보정을 확인하세요");
        if(durationJitter<0||durationJitter>30000||afterJitter<0||afterJitter>60000)throw new IllegalArgumentException("무작위 범위를 확인하세요");
        if((type.equals("CURVE")||type.startsWith("PINCH")||type.equals("MULTI_TAP")||type.equals("GESTURE")))GesturePaths.validate(GesturePaths.forStep(this));
        if(!java.util.Arrays.asList("CONTAINS","EXACT","REGEX").contains(match)||!java.util.Arrays.asList("AUTO","ACCESS","OCR").contains(source)||!java.util.Arrays.asList("STOP","SKIP","BACK_RETRY","RETRY","SCROLL_RETRY").contains(failure))throw new IllegalArgumentException("글자 비교 옵션을 확인하세요");
        if("REGEX".equals(match))java.util.regex.Pattern.compile(text);
        if("APP".equals(type)&&!packageName.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+"))throw new IllegalArgumentException("앱 패키지 이름을 확인하세요");
        if("GOTO".equals(type)&&(target<1||target>count))throw new IllegalArgumentException("이동할 동작 번호가 없습니다");
        if("TEXT_BRANCH".equals(type)&&(found<0||missing<0||found>count||missing>count))throw new IllegalArgumentException("분기 번호는 0(다음)~"+count+"입니다");
    }
}
