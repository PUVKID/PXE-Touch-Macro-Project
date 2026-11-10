package kr.yunseok.touchmacro;

import android.graphics.Rect;
import android.os.SystemClock;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** 단일 서비스 worker에서 관측·시범·자동 정책 실행을 연결합니다. */
public final class BehaviorAndroidBridge {
    final MacroService service;
    final BehaviorTaskStore store;
    volatile BehaviorTaskStore.Task task;
    final long token,settings;
    final long initialPause;
    final int maxMinutes;
    final boolean teaching;
    final String sessionId=UUID.randomUUID().toString();
    final BehaviorAndroidSession session;
    final AtomicBoolean busy=new AtomicBoolean(true);
    private volatile boolean cancelled;
    private BehaviorExample.Registry registry;
    private String previous="";
    private boolean waitLabel;
    private long lastDispatch,started;
    private Set<String> lastTexts=Set.of();private int sameTextCount;
    private final org.json.JSONArray deliveries=new org.json.JSONArray();
    private static boolean delivered(String result){return "COMPLETED".equals(result)||"ACCEPTED_GLOBAL".equals(result)||"OBSERVED_WAIT".equals(result);}
    private void delivery(BehaviorExample.Action action,String result,Frame frame)throws org.json.JSONException{deliveries.put(new org.json.JSONObject().put("session",sessionId).put("action",action.id).put("result",result).put("before_uptime",frame.observation.binding.capturedAtMs).put("result_uptime",SystemClock.uptimeMillis()).put("generation",frame.stamp.generation).put("pause",frame.stamp.pause).put("window",frame.stamp.window).put("content",frame.stamp.content));}
    // 표시 이름만 바꾸고 저장된 행동·전달 결과 식별자는 그대로 유지합니다.
    static String actionName(BehaviorExample.Type type){
        switch(type){
            case TEXT_TAP:return "글자 터치";
            case BACK:return "뒤로가기";
            case WAIT_TEXT:return "글자 대기";
            case FINISH:return "완료";
            case SCROLL_UP:return "위로 스크롤";
            case SCROLL_DOWN:return "아래로 스크롤";
            default:return "동작";
        }
    }
    private static String deliveryName(String result){
        if("COMPLETED".equals(result))return "완료";
        if("ACCEPTED_GLOBAL".equals(result))return "시스템 요청 수락";
        if("OBSERVED_WAIT".equals(result))return "글자 관측";
        if("CANCELLED".equals(result))return "취소";
        if("REJECTED".equals(result))return "거절";
        return "결과 미확인";
    }
    static final class Frame {
        final BehaviorExample.Observation observation;final BehaviorAndroidSession.Stamp stamp;final Rect excluded;final String application;
        Frame(BehaviorExample.Observation observation,BehaviorAndroidSession.Stamp stamp,Rect excluded,String application){this.observation=observation;this.stamp=stamp;this.excluded=new Rect(excluded);this.application=application;}
    }
    BehaviorAndroidBridge(MacroService service,BehaviorTaskStore.Task task,long token,long settings,boolean teaching,String correction,int maxMinutes){
        this.service=service;this.task=task;this.token=token;this.settings=settings;this.teaching=teaching;this.maxMinutes=maxMinutes;initialPause=service.behaviorPauseRevision();store=new BehaviorTaskStore(service.getFilesDir());registry=task.registry();session=new BehaviorAndroidSession(task.taskId,task.replay(),correction);
    }
    boolean live(){return !cancelled&&service.behaviorLive(this);}
    void check(){if(!live())throw new CancellationException("시범 실행 상태가 바뀌었습니다");}
    void cancel(){cancelled=true;session.cancel();}
    void begin(){try{check();started=SystemClock.uptimeMillis();service.behaviorInitialize(this);service.behaviorUi(this,()->service.controller().showBehavior(this));service.behaviorLaunch(this);if(teaching){message("글자를 한 번 누르세요 · 원 매크로 저장 없음");busy.set(false);}else runPolicy();}catch(Exception e){fail(e);}}
    private void submit(Checked operation){if(!teaching||!live()||!busy.compareAndSet(false,true))return;service.behaviorExecute(()->{try{for(;;){check();try{operation.run();break;}catch(Exception e){if(!service.behaviorPauseStale(e))throw e;service.behaviorWait(this);}}}catch(Exception e){fail(e);}finally{busy.set(false);if(live())service.behaviorUiLater(this,()->service.controller().behaviorReady(this));}});}
    interface Checked {void run()throws Exception;}
    void tapped(float x,float y){submit(()->{
        Frame a=frame();session.observeBefore(a.observation);List<BehaviorExample.Token> proposed=BehaviorFeatures.proposeAt(a.observation.tokens,x,y);
        if(proposed.size()!=1)throw new IllegalArgumentException("누른 곳의 글자가 없거나 모호합니다. 제어창을 옮기고 다시 가르쳐 주세요");
        String raw=proposed.get(0).raw;BehaviorExample.Type type=waitLabel?BehaviorExample.Type.WAIT_TEXT:BehaviorExample.Type.TEXT_TAP;waitLabel=false;
        BehaviorExample.Action action=registered(type,raw);Frame b=frame();BehaviorFeatures.resolveCurrent(b.observation.tokens,raw);check();
        String delivery=service.behaviorDispatch(this,action,b);delivery(action,delivery,b);
        session.stage(b.observation,action.id,delivery);if(delivered(delivery)){previous=action.id;if(!"OBSERVED_WAIT".equals(delivery))lastDispatch=SystemClock.uptimeMillis();}message("시범 "+session.draftCount()+"개 · "+org.json.JSONObject.quote(raw)+" · 전달 "+deliveryName(delivery)+" · 정답 미확정");
    });}
    void choose(BehaviorExample.Type type){if(type==BehaviorExample.Type.WAIT_TEXT){if(!live()||busy.get())return;waitLabel=true;message("기다릴 글자를 누르세요 · 최대10초");return;}submit(()->{Frame a=frame();session.observeBefore(a.observation);BehaviorExample.Action action=registered(type,null);Frame b=frame();String delivery=service.behaviorDispatch(this,action,b);delivery(action,delivery,b);session.stage(b.observation,action.id,delivery);if(delivered(delivery)){previous=action.id;if(!"OBSERVED_WAIT".equals(delivery))lastDispatch=SystemClock.uptimeMillis();}message(actionName(type)+" · 전달 "+deliveryName(delivery)+" · 정답 미확정");});}
    private BehaviorExample.Action registered(BehaviorExample.Type type,String raw){
        for(int i=0;i<registry.size();i++){BehaviorExample.Action a=registry.get(i);if(a.type==type&&Objects.equals(a.targetRaw,raw))return a;}
        BehaviorExample.Action action=new BehaviorExample.Action(UUID.randomUUID().toString(),type,raw,type==BehaviorExample.Type.WAIT_TEXT?10000:0,false);registry=registry.append(action);return action;
    }
    void finishTeaching(){submit(()->{
        Frame finalFrame=frame();session.observeBefore(finalFrame.observation);
        BehaviorExample.Action finish=registered(BehaviorExample.Type.FINISH,null);
        session.stageFinish(finalFrame.observation,finish.id);
        BehaviorExample.Replay replay=session.finish(finalFrame.observation,true);
        check();message("직접 시범 확인됨 · 학습 후보 계산 중 · 중단 가능");
        BehaviorPolicy old=task.snapshot==null?null:task.snapshot.policy;
        BehaviorPolicy policy=old==null?new BehaviorPolicy(registry.size(),new java.security.SecureRandom().nextLong()):old.outputSize()==registry.size()?old:old.appendOutputs(registry.size());
        if(registry.size()>1)policy=policy.learn(replay,registry,task.taskId,64,8,.05,()->!live());
        check();BehaviorBundle.Snapshot candidate=new BehaviorBundle.Snapshot(task.taskId,task.revision+1,registry,replay,policy);
        try(BehaviorTaskStore.Prepared pending=store.prepare(task,candidate,true,deliveries.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))){service.behaviorCommit(this,()->task=store.commit(pending,this::live));}
        service.behaviorFinished(this,"완료","시범과 학습 후보 저장됨 · 완료는 업무 성공 보증이 아닙니다",null);
    });}
    private void runPolicy()throws Exception{
        if(task.snapshot==null||task.snapshot.policy.completedSteps()==0)throw new IllegalStateException("확정 시범과 학습된 작업이 필요합니다");
        while(live()){
            try{
            Frame a=frame();BehaviorExample.Action action=select(a.observation);if(action==null)continue;Frame b=frame();BehaviorExample.Action fresh=select(b.observation);
            if(fresh==null||!action.id.equals(fresh.id)){message("새 화면 재확인 · 입력 없이 기다리는 중 · 고치기로 보완 가능");continue;}
            if(action.type==BehaviorExample.Type.FINISH){service.behaviorFinished(this,"완료","배운 시범의 완료 지점 · 업무 성공은 사용자가 확인하세요",null);return;}
            String result=service.behaviorDispatch(this,action,b);delivery(action,result,b);if(!delivered(result))throw new IllegalStateException("입력이 완료되지 않아 재전송하지 않았습니다 · "+deliveryName(result));
            previous=action.id;if(!"OBSERVED_WAIT".equals(result))lastDispatch=SystemClock.uptimeMillis();
            }catch(Exception error){if(!service.behaviorPauseStale(error))throw error;service.behaviorWait(this);}
        }
    }
    private BehaviorExample.Action select(BehaviorExample.Observation observation){
        check();String supported=BehaviorAndroidSession.supportedAction(task.snapshot.replay,task.taskId,task.target,observation);
        if(BehaviorAndroidSession.readiness(supported,null,-1)==BehaviorAndroidSession.Readiness.WAIT_SUPPORT){message("배운 글자 단서 대기 · 입력 없음 · 중단/고치기 가능");return null;}
        int index=registry.indexOf(supported);double[] p=task.snapshot.policy.probabilities(BehaviorFeatures.encode(observation));
        if(BehaviorAndroidSession.readiness(supported,p,index)!=BehaviorAndroidSession.Readiness.READY){message("확신 기준 대기 · 입력 없음 · 중단/고치기 가능");return null;}
        BehaviorExample.Action action=registry.get(index);if(action.type==BehaviorExample.Type.TEXT_TAP||action.type==BehaviorExample.Type.WAIT_TEXT)try{BehaviorFeatures.resolveCurrent(observation.tokens,action.targetRaw);}catch(IllegalArgumentException ambiguous){message("현재 유일한 글자 대기 · 입력 없음 · 제어창/교정 확인");return null;}return action;
    }
    Frame frame()throws Exception{
        check();Frame frame=service.behaviorSnapshot(this,previous,started,lastDispatch,sameTextCount);
        Set<String> texts=BehaviorAndroidSession.texts(frame.observation);sameTextCount=texts.equals(lastTexts)?sameTextCount+1:0;lastTexts=texts;return frame;
    }
    void message(String text){service.behaviorUiLater(this,()->service.controller().behaviorMessage(this,text));}
    private void fail(Exception error){if(live()){service.behaviorFinished(this,"중단","시범/실행 중단 · "+error.getMessage(),error);}else cancel();}
}
