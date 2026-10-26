package kr.yunseok.touchmacro;

import org.json.JSONObject;

/** A saved detection profile. It owns no macro steps and never changes the user's macro. */
public final class WatchConfig {
    public Step rule = new Step("TEXT_WAIT");
    public String macroName = "";
    public int intervalSeconds = 10, repetitions = 15, entryMs = 1500, settleMs = 1500, returnMs = 1500, backCount = 1, maxMinutes = 0;
    public String direction = "PULL_DOWN";
    public boolean tapFound = true, waitForRefresh = true;
    public WatchConfig() { rule.text="확인"; rule.timeout=0; rule.failure="STOP"; }
    public WatchConfig(JSONObject o) {
        this();
        if(o.has("rule")) rule=new Step(o.optJSONObject("rule"));
        rule.type="TEXT_WAIT";rule.timeout=0;
        macroName=o.optString("macroName","");intervalSeconds=o.optInt("intervalSeconds",10);
        repetitions=o.optInt("repetitions",15);entryMs=o.optInt("entryMs",1500);
        settleMs=o.optInt("settleMs",1500);returnMs=o.optInt("returnMs",1500);
        backCount=o.optInt("backCount",1);maxMinutes=o.optInt("maxMinutes",0);
        direction=o.optString("direction","PULL_DOWN");tapFound=o.optBoolean("tapFound",true);waitForRefresh=o.optBoolean("waitForRefresh",true);
        // This requested workflow is the new preset; retain the old word, region and macro.
        if(!o.has("repetitions")){intervalSeconds=10;tapFound=true;waitForRefresh=true;}
    }
    public JSONObject json() {
        try{return new JSONObject().put("rule",rule.json()).put("macroName",macroName)
            .put("intervalSeconds",intervalSeconds).put("repetitions",repetitions).put("entryMs",entryMs).put("settleMs",settleMs).put("returnMs",returnMs)
            .put("backCount",backCount).put("maxMinutes",maxMinutes).put("direction",direction).put("tapFound",tapFound).put("waitForRefresh",waitForRefresh);
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    public void validate() {
        rule.validate(1);
        if(intervalSeconds<1||intervalSeconds>86400)throw new IllegalArgumentException("새로고침 간격은 1~86400초입니다");
        if(repetitions<1||repetitions>100000)throw new IllegalArgumentException("매크로 반복은 1~100000회입니다");
        if(entryMs<300||entryMs>60000||settleMs<300||settleMs>60000||returnMs<300||returnMs>60000)throw new IllegalArgumentException("화면 대기는 0.3~60초입니다");
        if(backCount<1||backCount>10)throw new IllegalArgumentException("뒤로가기는 1~10회입니다");
        if(maxMinutes<0||maxMinutes>1440)throw new IllegalArgumentException("탐지 제한 시간은 0~1440분입니다");
        if(!direction.equals("PULL_DOWN")&&!direction.equals("SCROLL_DOWN"))throw new IllegalArgumentException("새로고침 방향을 확인하세요");
    }
    public void validateMacro(Macro m) {
        m.validate();
        boolean active=false;for(Step s:m.steps)if(s.enabled)active=true;
        if(!active)throw new IllegalArgumentException("실행할 동작을 한 개 이상 켜 주세요");
    }
    public Macro prepareMacro(Macro original) {
        validate();
        Macro m=new Macro(original.json());
        m.rounds=repetitions;m.startAt=1;
        validateMacro(m);return m;
    }
    public double swipeStartY(){return direction.equals("PULL_DOWN")?30:75;}
    public double swipeEndY(){return direction.equals("PULL_DOWN")?75:30;}
}
