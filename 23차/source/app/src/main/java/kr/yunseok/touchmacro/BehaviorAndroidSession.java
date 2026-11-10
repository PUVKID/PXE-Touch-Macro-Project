package kr.yunseok.touchmacro;

import java.util.*;
import java.util.concurrent.CancellationException;

/** 실시간 허가와 직접 시범의 확정을 분리하는 작은 세션입니다. */
public final class BehaviorAndroidSession {
    public static final double MIN_PROBABILITY=.80,MIN_MARGIN=.20;
    public enum Readiness {READY,WAIT_SUPPORT,WAIT_CONFIDENCE}
    public static boolean enabledRevision(boolean enabled,long expected,long current){return enabled&&expected==current;}
    public enum TargetReadiness {WAIT_TARGET,CHECK_WINDOW,REJECT_TARGET}
    public static TargetReadiness targetReadiness(String observed,String expected){return observed.isEmpty()?TargetReadiness.WAIT_TARGET:observed.equals(expected)?TargetReadiness.CHECK_WINDOW:TargetReadiness.REJECT_TARGET;}
    public static Readiness readiness(String supported,double[] probabilities,int expected){return supported==null?Readiness.WAIT_SUPPORT:confident(probabilities,expected)?Readiness.READY:Readiness.WAIT_CONFIDENCE;}
    public static long deadline(long now,int maxMinutes){if(maxMinutes<0||maxMinutes>1440)throw new IllegalArgumentException("기존 최대 실행 시간 범위 오류");return maxMinutes==0?0:Math.addExact(now,maxMinutes*60000L);}
    public static final class Stamp {
        public final long generation,pause,window,content,settings,model;
        public final String target;
        public final boolean enabled,paused;
        public Stamp(long generation,long pause,long window,long content,long settings,long model,String target,boolean enabled,boolean paused){
            this.generation=generation;this.pause=pause;this.window=window;this.content=content;this.settings=settings;this.model=model;this.target=target;this.enabled=enabled;this.paused=paused;
        }
        public boolean matches(Stamp other){return other!=null&&enabled&&other.enabled&&!paused&&!other.paused&&generation==other.generation&&pause==other.pause&&window==other.window&&content==other.content&&settings==other.settings&&model==other.model&&target.equals(other.target);}
        public boolean sameExceptWindow(Stamp other){return other!=null&&new Stamp(generation,pause,other.window,content,settings,model,target,enabled,paused).matches(other);}
    }
    private final String taskId,correction;
    private final BehaviorExample.Replay original;
    private BehaviorExample.Replay confirmed;
    private final List<BehaviorExample> drafts=new ArrayList<>();
    private BehaviorExample.Observation pendingBefore;
    private String pendingAction,pendingDelivery;
    private boolean cancelled,finished;
    public BehaviorAndroidSession(String taskId,BehaviorExample.Replay original,String correction){this.taskId=Objects.requireNonNull(taskId);this.original=Objects.requireNonNull(original);this.confirmed=original;this.correction=correction;}
    private void check(){if(cancelled||finished)throw new CancellationException("시범 세션이 끝났습니다");}
    public synchronized void observeBefore(BehaviorExample.Observation after){check();closePending(after);}
    private void closePending(BehaviorExample.Observation after){
        if(pendingBefore==null)return;
        String parent=drafts.isEmpty()?correction:null;
        drafts.add(new BehaviorExample(UUID.randomUUID().toString(),pendingBefore,pendingAction,after,parent==null?"DEMONSTRATION":"CORRECTION",parent,false,false));
        pendingBefore=null;pendingAction=null;pendingDelivery=null;
    }
    public synchronized void stage(BehaviorExample.Observation before,String action,String delivery){
        check();if(pendingBefore!=null)throw new IllegalStateException("이전 행동의 다음 관측이 필요합니다");
        // OS 전달 결과는 정답이 아닙니다. 취소·거절된 입력은 시범 정답에 포함하지 않습니다.
        if(!"COMPLETED".equals(delivery)&&!"ACCEPTED_GLOBAL".equals(delivery)&&!"OBSERVED_WAIT".equals(delivery))return;
        pendingBefore=before;pendingAction=action;pendingDelivery=delivery;
    }
    public synchronized void stageFinish(BehaviorExample.Observation before,String action){check();if(pendingBefore!=null)throw new IllegalStateException("마지막 행동의 후속 관측이 필요합니다");pendingBefore=before;pendingAction=action;pendingDelivery="USER_FINISH";}
    public synchronized BehaviorExample.Replay finish(BehaviorExample.Observation after,boolean userConfirmed){
        check();if(!userConfirmed)throw new IllegalArgumentException("사용자의 시범 완료 확인이 필요합니다");closePending(after);
        BehaviorExample.Replay result=original;
        for(BehaviorExample row:drafts)result=result.append(new BehaviorExample(row.id,row.before,row.actionId,row.after,row.provenance,row.supersedes,true,false));
        confirmed=result;finished=true;return result;
    }
    public synchronized BehaviorExample.Replay confirmed(){return confirmed;}
    public synchronized void cancel(){cancelled=true;pendingBefore=null;drafts.clear();}
    public synchronized int draftCount(){return drafts.size()+(pendingBefore==null?0:1);}
    /** 직접 반복 시범의 안정 원문과 다른 행동의 금지 원문을 검사합니다. 확률과 live 허가는 별도입니다. */
    public static String supportedAction(BehaviorExample.Replay replay,String task,String target,BehaviorExample.Observation current){
        if(!task.equals(current.binding.taskId)||!target.equals(current.binding.targetPackage))return null;
        Map<String,List<Set<String>>> groups=new LinkedHashMap<>();
        for(BehaviorExample row:replay.active(task)){
            BehaviorExample.Observation before=row.before;
            if(target.equals(before.binding.targetPackage)&&before.previousActionId.equals(current.previousActionId))groups.computeIfAbsent(row.actionId,key->new ArrayList<>()).add(texts(before));
        }
        Map<String,Set<String>> stable=new LinkedHashMap<>(),seen=new LinkedHashMap<>();
        for(var group:groups.entrySet()){
            Set<String> all=new HashSet<>(),intersection=new HashSet<>(group.getValue().get(0));for(Set<String> words:group.getValue()){all.addAll(words);intersection.retainAll(words);}seen.put(group.getKey(),all);
            if(group.getValue().size()>=2&&!intersection.isEmpty())stable.put(group.getKey(),intersection);
        }
        Set<String> currentWords=texts(current);String result=null;
        for(var candidate:stable.entrySet()){
            if(!currentWords.containsAll(candidate.getValue()))continue;
            Set<String> forbidden=new HashSet<>(),distinct=new HashSet<>(candidate.getValue());
            for(var other:groups.entrySet())if(!other.getKey().equals(candidate.getKey())){
                distinct.removeAll(seen.get(other.getKey()));Set<String> otherStable=stable.get(other.getKey());if(otherStable!=null)for(String raw:otherStable)if(!seen.get(candidate.getKey()).contains(raw))forbidden.add(raw);
            }
            if(groups.size()>1&&distinct.isEmpty()||!Collections.disjoint(forbidden,currentWords))continue;
            if(result!=null)return null;result=candidate.getKey();
        }
        return result;
    }
    public static Set<String> texts(BehaviorExample.Observation observation){Set<String> result=new HashSet<>();for(BehaviorExample.Token token:observation.tokens)result.add(token.raw);return Collections.unmodifiableSet(result);}
    public static boolean confident(double[] probabilities,int expected){
        if(probabilities==null||probabilities.length==0||expected<0||expected>=probabilities.length)return false;
        int best=0;double second=0,sum=0;
        for(int i=0;i<probabilities.length;i++){double p=probabilities[i];if(!Double.isFinite(p)||p<0||p>1)return false;sum+=p;if(p>probabilities[best])best=i;}
        if(Math.abs(sum-1)>1e-9)return false;
        for(int i=0;i<probabilities.length;i++)if(i!=best)second=Math.max(second,probabilities[i]);
        return best==expected&&probabilities[best]>=MIN_PROBABILITY&&probabilities[best]-second>=MIN_MARGIN;
    }
}
