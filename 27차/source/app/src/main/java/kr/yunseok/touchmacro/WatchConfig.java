package kr.yunseok.touchmacro;

import org.json.JSONObject;

/** A saved detection profile. It owns no macro steps and never changes the user's macro. */
public final class WatchConfig {
    public final java.util.ArrayList<WatchArea> priorityAreas=new java.util.ArrayList<>();
    public Step rule = new Step("TEXT_WAIT");
    public String macroName = "";
    public int intervalSeconds = 10, repetitions = 15, entryMs = 1500, settleMs = 1500, returnMs = 1500, backCount = 1, maxMinutes = 0;
    public int startMs=3000,repeatGapMs=1000,beforeBackMs=0,rearmMs=2000;
    public int cooldownMs=600000;
    public String targetPackage="";
    public boolean preventDuplicate=false;
    public String direction = "PULL_DOWN";
    public boolean tapFound = true, waitForRefresh = true;
    public WatchConfig() { rule.text="확인"; rule.timeout=0; rule.failure="STOP"; }
    public WatchConfig(JSONObject o) {
        this();
        if(o.has("rule")) rule=new Step(o.optJSONObject("rule"));
        rule.type="TEXT_WAIT";rule.timeout=0;
        macroName=o.optString("macroName","");intervalSeconds=o.optInt("intervalSeconds",10);
        repetitions=o.optInt("repetitions",15);entryMs=o.optInt("entryMs",1500);
        startMs=o.optInt("startMs",3000);repeatGapMs=o.optInt("repeatGapMs",1000);beforeBackMs=o.optInt("beforeBackMs",0);rearmMs=o.optInt("rearmMs",2000);
        cooldownMs=o.optInt("cooldownMs",600000);
        org.json.JSONArray areas=o.optJSONArray("priorityAreas");if(areas!=null){if(areas.length()>6)throw new IllegalArgumentException("우선순위 영역은 최대 6개입니다");for(int i=0;i<areas.length();i++)priorityAreas.add(new WatchArea(areas.optJSONObject(i)));}
        targetPackage=o.optString("targetPackage","");preventDuplicate=o.optBoolean("preventDuplicate",false);
        settleMs=o.optInt("settleMs",1500);returnMs=o.optInt("returnMs",1500);
        backCount=o.optInt("backCount",1);maxMinutes=o.optInt("maxMinutes",0);
        direction=o.optString("direction","PULL_DOWN");tapFound=o.optBoolean("tapFound",true);waitForRefresh=o.optBoolean("waitForRefresh",true);
        // This requested workflow is the new preset; retain the old word, region and macro.
        if(!o.has("repetitions")){intervalSeconds=10;tapFound=true;waitForRefresh=true;}
    }
    public JSONObject json() {
        org.json.JSONArray areas=new org.json.JSONArray();for(WatchArea area:priorityAreas)areas.put(area.json());
        try{return new JSONObject().put("priorityAreas",areas).put("rule",rule.json()).put("macroName",macroName)
            .put("intervalSeconds",intervalSeconds).put("repetitions",repetitions).put("entryMs",entryMs).put("settleMs",settleMs).put("returnMs",returnMs)
            .put("startMs",startMs).put("repeatGapMs",repeatGapMs).put("beforeBackMs",beforeBackMs).put("rearmMs",rearmMs).put("targetPackage",targetPackage).put("preventDuplicate",preventDuplicate)
            .put("cooldownMs",cooldownMs)
            .put("backCount",backCount).put("maxMinutes",maxMinutes).put("direction",direction).put("tapFound",tapFound).put("waitForRefresh",waitForRefresh);
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    public void validate() {
        rule.validate(1);
        if(priorityAreas.size()>6)throw new IllegalArgumentException("우선순위 영역은 최대 6개입니다");
        for(int i=0;i<priorityAreas.size();i++){Step s=new Step(rule.json());priorityAreas.get(i).apply(s);s.validate(1);for(int j=0;j<i;j++)if(priorityAreas.get(i).overlaps(priorityAreas.get(j)))throw new IllegalArgumentException("우선순위 영역은 서로 겹치지 않게 그려 주세요");}
        if(intervalSeconds<1||intervalSeconds>86400)throw new IllegalArgumentException("새로고침 간격은 1~86400초입니다");
        if(repetitions<1||repetitions>100000)throw new IllegalArgumentException("매크로 반복은 1~100000회입니다");
        if(entryMs<300||entryMs>60000||settleMs<300||settleMs>60000||returnMs<300||returnMs>60000)throw new IllegalArgumentException("화면 대기는 0.3~60초입니다");
        if(startMs<0||startMs>600000||repeatGapMs<0||repeatGapMs>600000||beforeBackMs<0||beforeBackMs>600000||rearmMs<1000||rearmMs>60000)throw new IllegalArgumentException("단계 대기는 0~600초, 사라짐 확인은 1~60초입니다");
        if(cooldownMs<0||cooldownMs>86400000)throw new IllegalArgumentException("완료 후 대기는 0~86400초입니다");
        if(!targetPackage.isEmpty()&&!targetPackage.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+"))throw new IllegalArgumentException("대상 앱을 다시 지정하세요");
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
        m.rounds=repetitions;m.startAt=1;m.gap=repeatGapMs;m.startDelay=0;
        validateMacro(m);return m;
    }
    public String lockKey(){return targetPackage+"|"+rule.text+"|"+rule.match+"|"+rule.source+"|"+rule.occurrence+"|"+rule.left+","+rule.top+","+rule.right+","+rule.bottom;}
    public int areaCount(){return priorityAreas.isEmpty()?1:priorityAreas.size();}
    public WatchConfig areaConfig(int index){WatchConfig p=new WatchConfig(json());if(!priorityAreas.isEmpty()){priorityAreas.get(index).apply(p.rule);p.rule.occurrence=1;}p.priorityAreas.clear();return p;}
    public double swipeStartY(){return direction.equals("PULL_DOWN")?30:75;}
    public double swipeEndY(){return direction.equals("PULL_DOWN")?75:30;}
}
