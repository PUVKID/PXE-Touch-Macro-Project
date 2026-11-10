package kr.yunseok.touchmacro;

/** 정적 계획 작성과 실제 외부 앱 입력 허가를 분리합니다. */
public final class AiStartGate {
    private final String target,own;
    private final boolean engineAppFirst;
    private boolean appPending,open;
    private String inputTarget;
    private boolean inputConsumed;
    private String previousInputTarget="";
    private boolean inputAppPending;
    private AiStartGate(String target,String own,boolean firstApp){
        if(target==null||!target.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+")||target.equals(own))throw AiJson.error("실행할 외부 앱을 선택하세요");
        this.target=target;this.own=own;this.engineAppFirst=firstApp;this.appPending=firstApp;this.inputTarget=target;
    }
    public static AiStartGate macro(Macro macro,String selected,String own){
        Step first=null;for(int index=macro.startAt-1;index<macro.steps.size();index++)if(macro.steps.get(index).enabled){first=macro.steps.get(index);break;}
        boolean app=first!=null&&first.type.equals("APP");return new AiStartGate(app?first.packageName:selected,own,app);
    }
    public static AiStartGate watch(WatchConfig watch,String selected,String own){return new AiStartGate(selected==null||selected.isEmpty()?watch.targetPackage:selected,own,false);}
    public String target(){return target;}
    public boolean engineAppFirst(){return engineAppFirst;}
    public boolean open(){return open;}
    public synchronized String inputTarget(){return inputConsumed?"":inputTarget;}
    public synchronized void inputDispatched(){inputConsumed=true;}
    public synchronized void appSucceeded(Step step){if(!step.type.equals("APP"))throw AiJson.error("앱 실행 동작이 아닙니다");if(!inputConsumed){previousInputTarget=inputTarget;inputTarget=step.packageName;inputAppPending=true;}}
    public synchronized boolean awaitingInputApp(){return !inputConsumed&&inputAppPending;}
    public synchronized boolean observeInputApp(String actual){if(!awaitingInputApp())return true;if(inputTarget.equals(actual)){inputAppPending=false;return true;}if(actual==null||actual.isEmpty()||own.equals(actual)||previousInputTarget.equals(actual))return false;throw AiJson.error("명시한 앱 전환 중 다른 앱이 활성화되어 첫 입력을 정지했습니다");}
    public boolean permitsInitialApp(Step step){return !open&&appPending&&step.type.equals("APP")&&step.packageName.equals(target);}
    public void appLaunched(Step step){if(!permitsInitialApp(step))throw AiJson.error("첫 앱 실행의 순서가 바뀌었습니다");appPending=false;}
    public boolean observe(String packageName,boolean paused){
        if(open)return true;
        if(paused||packageName==null||packageName.isEmpty()||packageName.equals(own))return false;
        if(!packageName.equals(target))throw AiJson.error("선택한 앱 대신 다른 앱이 활성화되어 입력을 시작하지 않았습니다");
        if(appPending)return false;open=true;return true;
    }
}
