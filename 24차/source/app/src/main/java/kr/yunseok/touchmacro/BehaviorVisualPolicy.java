package kr.yunseok.touchmacro;

import java.util.*;
import java.util.concurrent.CancellationException;
import org.json.*;

/** 확정된 시각 시범에서 실제 교차엔트로피 경사로 갱신하는 오프라인 분류기입니다. */
public final class BehaviorVisualPolicy {
    private final int outputs;
    private final double[] weights;
    private final long steps;
    public BehaviorVisualPolicy(int outputs){this(outputs,new double[outputs*(BehaviorVisual.INPUT+1)],0);}
    private BehaviorVisualPolicy(int outputs,double[] weights,long steps){if(outputs<1||outputs>500||weights==null||weights.length!=outputs*(BehaviorVisual.INPUT+1)||steps<0)throw BehaviorExample.error("시각 정책 형상 오류");for(double v:weights)BehaviorPolicy.finite(v);this.outputs=outputs;this.weights=weights.clone();this.steps=steps;}
    public int outputSize(){return outputs;}
    public long completedSteps(){return steps;}
    public double[] parameters(){return weights.clone();}
    public BehaviorVisualPolicy appendOutputs(int count){if(count<outputs||count>500)throw BehaviorExample.error("시각 정책 사전은 추가만 허용합니다");return new BehaviorVisualPolicy(count,Arrays.copyOf(weights,count*(BehaviorVisual.INPUT+1)),steps);}
    public double[] probabilities(BehaviorExample.Observation observation){double[] x=BehaviorVisual.encode(observation),p=new double[outputs];double maximum=-Double.MAX_VALUE;for(int k=0;k<outputs;k++){int offset=k*(BehaviorVisual.INPUT+1);double value=weights[offset+BehaviorVisual.INPUT];for(int i=0;i<x.length;i++)value+=weights[offset+i]*x[i];BehaviorPolicy.finite(value);p[k]=value;maximum=Math.max(maximum,value);}double total=0;for(int k=0;k<outputs;k++){p[k]=StrictMath.exp(p[k]-maximum);total+=p[k];}for(int k=0;k<outputs;k++)p[k]/=total;return p;}
    public BehaviorVisualPolicy learn(BehaviorExample.Replay replay,BehaviorExample.Registry registry,String task,int updates,BehaviorPolicy.Cancel cancel){
        if(registry.size()!=outputs||updates<1||updates>600||cancel==null||steps>Long.MAX_VALUE-updates)throw BehaviorExample.error("시각 학습 설정 오류");List<BehaviorExample> rows=new ArrayList<>();for(BehaviorExample row:replay.active(task))if(row.before.visual!=null)rows.add(row);if(rows.isEmpty())throw BehaviorExample.error("확정 시각 시범 없음");
        BehaviorVisualPolicy candidate=new BehaviorVisualPolicy(outputs,weights,steps);double[] w=candidate.weights;
        for(int step=0;step<updates;step++){
            if(cancel.cancelled())throw new CancellationException("시각 학습 후보 취소");double[] gradient=new double[w.length];
            // 고정된 전체 replay의 기울기를 합쳐 추가 시범이 예전 시범을 임의로 버리지 않게 합니다.
            for(BehaviorExample row:rows){if(cancel.cancelled())throw new CancellationException("시각 학습 후보 취소");double[] x=BehaviorVisual.encode(row.before),p=candidate.probabilities(row.before);int label=registry.indexOf(row.actionId);for(int k=0;k<outputs;k++){double d=(p[k]-(k==label?1:0))/rows.size();int offset=k*(BehaviorVisual.INPUT+1);for(int i=0;i<x.length;i++)gradient[offset+i]+=d*x[i];gradient[offset+BehaviorVisual.INPUT]+=d;}}
            for(int i=0;i<w.length;i++){w[i]-=.08*gradient[i];BehaviorPolicy.finite(w[i]);}
        }
        if(cancel.cancelled())throw new CancellationException("시각 학습 후보 취소");return new BehaviorVisualPolicy(outputs,w,steps+updates);
    }
    public JSONObject json(){java.nio.ByteBuffer buffer=java.nio.ByteBuffer.allocate(weights.length*8).order(java.nio.ByteOrder.BIG_ENDIAN);for(double value:weights){BehaviorPolicy.finite(value);buffer.putDouble(value);}return BehaviorExample.object("format","pxe.visual.softmax.v1","input",BehaviorVisual.INPUT,"outputs",outputs,"steps",steps,"weights_f64",Base64.getEncoder().encodeToString(buffer.array()));}
    public static BehaviorVisualPolicy fromJson(JSONObject o)throws JSONException{BehaviorExample.keys(o,"format","input","outputs","steps","weights_f64");if(!"pxe.visual.softmax.v1".equals(o.get("format"))||BehaviorExample.integer(o.get("input"),BehaviorVisual.INPUT,BehaviorVisual.INPUT)!=BehaviorVisual.INPUT)throw BehaviorExample.error("시각 특징 계약 오류");int k=(int)BehaviorExample.integer(o.get("outputs"),1,500);int count=k*(BehaviorVisual.INPUT+1),bytes=count*8;String encoded=BehaviorExample.string(o.get("weights_f64"));if(encoded.length()!=4*((bytes+2)/3))throw BehaviorExample.error("시각 가중치 base64 길이 오류");byte[] raw=Base64.getDecoder().decode(encoded);if(raw.length!=bytes)throw BehaviorExample.error("시각 가중치 실제 바이트 길이 오류");java.nio.ByteBuffer buffer=java.nio.ByteBuffer.wrap(raw).order(java.nio.ByteOrder.BIG_ENDIAN);double[] w=new double[count];for(int i=0;i<w.length;i++){w[i]=buffer.getDouble();BehaviorPolicy.finite(w[i]);}return new BehaviorVisualPolicy(k,w,BehaviorExample.integer(o.get("steps"),0,Long.MAX_VALUE));}
}
