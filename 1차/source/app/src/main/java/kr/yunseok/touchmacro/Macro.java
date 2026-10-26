package kr.yunseok.touchmacro;
import org.json.*;
import java.util.ArrayList;
public final class Macro {
    public String name="내 매크로";
    public int rounds=1, gap=1000, startDelay=3000, maxMinutes=30;
    public ArrayList<Step> steps=new ArrayList<>();
    public Macro(){}
    public Macro(JSONObject o) {
        if(o.optInt("schema",1)!=1)throw new IllegalArgumentException("지원하지 않는 파일 버전");
        name=o.optString("name","내 매크로");rounds=o.optInt("rounds",1);gap=o.optInt("gap",1000);startDelay=o.optInt("startDelay",3000);maxMinutes=o.optInt("maxMinutes",30);
        JSONArray a=o.optJSONArray("steps");if(a==null||a.length()>500)throw new IllegalArgumentException("동작 목록은 최대 500개입니다");
        for(int i=0;i<a.length();i++) steps.add(new Step(a.optJSONObject(i)==null?new JSONObject():a.optJSONObject(i)));
    }
    public JSONObject json() {
        JSONObject o=new JSONObject();JSONArray a=new JSONArray();for(Step s:steps)a.put(s.json());
        try {o.put("schema",1).put("name",name).put("rounds",rounds).put("gap",gap).put("startDelay",startDelay).put("maxMinutes",maxMinutes).put("steps",a);}catch(Exception e){throw new IllegalStateException(e);}return o;
    }
    public void validate(){
        if(steps.isEmpty()||steps.size()>500)throw new IllegalArgumentException("동작을 1~500개 추가하세요");
        if(rounds<0||rounds>100000||gap<0||gap>600000||startDelay<0||startDelay>600000||maxMinutes<0||maxMinutes>1440)throw new IllegalArgumentException("실행 설정의 범위를 확인하세요");
        for(Step s:steps)s.validate(steps.size());
    }
}
