package kr.yunseok.touchmacro;

import org.json.JSONObject;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicLong;

/** 무작위 초기화한 F64 MLP입니다. 학습은 원본과 분리한 후보에만 적용합니다. */
public final class BehaviorPolicy {
    public interface Cancel {boolean cancelled();}
    private static final AtomicLong INSTANCES=new AtomicLong(),INITIALS=new AtomicLong(),FORWARD=new AtomicLong(),BACKWARD=new AtomicLong(),UPDATES=new AtomicLong(),DISCARDED=new AtomicLong();
    private static final long MAGIC=0x5058454248563100L;
    private final int input,hidden,outputs;
    private final double[] w1,b1,w2,b2;
    private final long seed;
    private long rng,steps;
    private String initialHash;

    public BehaviorPolicy(int outputs,long seed){this(584,32,outputs,seed);}
    BehaviorPolicy(int input,int hidden,int outputs,long seed){
        dimensions(input,hidden,outputs);this.input=input;this.hidden=hidden;this.outputs=outputs;this.seed=seed;this.rng=seed;
        w1=new double[input*hidden];b1=new double[hidden];w2=new double[hidden*outputs];b2=new double[outputs];
        double a=StrictMath.sqrt(6.0/(input+hidden)),b=StrictMath.sqrt(6.0/(hidden+outputs));
        for(int i=0;i<w1.length;i++)w1[i]=(nextDouble()*2-1)*a;for(int i=0;i<w2.length;i++)w2[i]=(nextDouble()*2-1)*b;
        INSTANCES.incrementAndGet();INITIALS.incrementAndGet();initialHash=parameterHash();
    }
    private BehaviorPolicy(BehaviorPolicy old,int k){
        dimensions(old.input,old.hidden,k);input=old.input;hidden=old.hidden;outputs=k;seed=old.seed;rng=old.rng;steps=old.steps;initialHash=old.initialHash;
        w1=old.w1.clone();b1=old.b1.clone();w2=Arrays.copyOf(old.w2,hidden*k);b2=Arrays.copyOf(old.b2,k);INSTANCES.incrementAndGet();
    }
    private static void dimensions(int d,int h,int k){if(d<1||d>584||h<1||h>32||k<1||k>500)throw BehaviorExample.error("정책 형상 상한 오류");}
    public int inputSize(){return input;}
    public int hiddenSize(){return hidden;}
    public int outputSize(){return outputs;}
    public int parameterCount(){return w1.length+b1.length+w2.length+b2.length;}
    public long completedSteps(){return steps;}
    public long rngState(){return rng;}
    public long seed(){return seed;}
    public String initialHash(){return initialHash;}
    public double[] parameters(){double[] p=new double[parameterCount()];int offset=0;for(double[] a:new double[][]{w1,b1,w2,b2}){System.arraycopy(a,0,p,offset,a.length);offset+=a.length;}return p;}
    public double[] firstLayerParameters(){double[] result=Arrays.copyOf(w1,w1.length+b1.length);System.arraycopy(b1,0,result,w1.length,b1.length);return result;}
    public double[] outputRow(int index){if(index<0||index>=outputs)throw BehaviorExample.error("출력 index 오류");double[] result=Arrays.copyOfRange(w2,index*hidden,(index+1)*hidden+1);result[hidden]=b2[index];return result;}
    public BehaviorPolicy withParameters(double[] parameters){BehaviorPolicy result=new BehaviorPolicy(this,outputs);result.assign(parameters);return result;}
    private void assign(double[] p){if(p==null||p.length!=parameterCount())throw BehaviorExample.error("가중치 길이 오류");for(double v:p)finite(v);int offset=0;for(double[] a:new double[][]{w1,b1,w2,b2}){System.arraycopy(p,offset,a,0,a.length);offset+=a.length;}}
    public BehaviorPolicy appendOutputs(int count){if(count<outputs||count>500)throw BehaviorExample.error("출력 사전은 마지막에만 추가합니다");BehaviorPolicy result=new BehaviorPolicy(this,count);double scale=StrictMath.sqrt(6.0/(hidden+count));for(int row=outputs;row<count;row++)for(int j=0;j<hidden;j++)result.w2[row*hidden+j]=(result.nextDouble()*2-1)*scale;return result;}

    private static final class Forward {final double[] h,z,p;Forward(double[] h,double[] z,double[] p){this.h=h;this.z=z;this.p=p;}}
    private Forward forward(double[] x){if(x==null||x.length!=input)throw BehaviorExample.error("정책 입력 형상 오류");for(double v:x)finite(v);double[] h=new double[hidden],z=new double[outputs],p=new double[outputs];
        for(int j=0;j<hidden;j++){double sum=b1[j];for(int i=0;i<input;i++)sum+=w1[j*input+i]*x[i];h[j]=StrictMath.tanh(sum);finite(h[j]);}
        double maximum=-Double.MAX_VALUE;for(int k=0;k<outputs;k++){double sum=b2[k];for(int j=0;j<hidden;j++)sum+=w2[k*hidden+j]*h[j];z[k]=sum;finite(sum);maximum=Math.max(maximum,sum);}
        double total=0;for(int k=0;k<outputs;k++){p[k]=StrictMath.exp(z[k]-maximum);total+=p[k];}finite(total);if(total<=0)throw BehaviorExample.error("softmax 정규화 오류");for(int k=0;k<outputs;k++)p[k]/=total;
        FORWARD.incrementAndGet();return new Forward(h,z,p);
    }
    public double[] probabilities(double[] x){return forward(x).p;}
    public int predict(double[] x){double[] p=probabilities(x);int best=0;for(int i=1;i<p.length;i++)if(p[i]>p[best])best=i;return best;}
    public static final class Gradient {public final double loss;public final double[] values;Gradient(double loss,double[] values){this.loss=loss;this.values=values;}}
    public double loss(double[][] x,int[] labels){return compute(x,labels,false).loss;}
    public Gradient gradient(double[][] x,int[] labels){return compute(x,labels,true);}
    private Gradient compute(double[][] x,int[] labels,boolean backward){if(x==null||labels==null||x.length==0||x.length>4096||x.length!=labels.length)throw BehaviorExample.error("학습 배치 형상 오류");double[] gradient=new double[parameterCount()];double loss=0;
        int b1at=w1.length,w2at=b1at+b1.length,b2at=w2at+w2.length;
        for(int row=0;row<x.length;row++){int label=labels[row];if(label<0||label>=outputs)throw BehaviorExample.error("등록 범위 밖 정답");Forward f=forward(x[row]);double maximum=Arrays.stream(f.z).max().getAsDouble(),sum=0;for(double z:f.z)sum+=StrictMath.exp(z-maximum);loss+=(maximum-f.z[label]+StrictMath.log(sum))/x.length;
            if(backward){double[] dh=new double[hidden];for(int k=0;k<outputs;k++){double dz=(f.p[k]-(k==label?1:0))/x.length;gradient[b2at+k]+=dz;for(int j=0;j<hidden;j++){gradient[w2at+k*hidden+j]+=dz*f.h[j];dh[j]+=w2[k*hidden+j]*dz;}}
                for(int j=0;j<hidden;j++){double dz=dh[j]*(1-f.h[j]*f.h[j]);gradient[b1at+j]+=dz;for(int i=0;i<input;i++)gradient[j*input+i]+=dz*x[row][i];}}
        }
        finite(loss);for(double value:gradient)finite(value);if(backward)BACKWARD.incrementAndGet();return new Gradient(loss,gradient);
    }
    public double replayLoss(BehaviorExample.Replay replay,BehaviorExample.Registry registry,String taskId){List<BehaviorExample> rows=rows(replay,registry,taskId);double sum=0;for(BehaviorExample row:rows)sum+=loss(new double[][]{BehaviorFeatures.encode(row.before)},new int[]{registry.indexOf(row.actionId)})/rows.size();return sum;}
    private List<BehaviorExample> rows(BehaviorExample.Replay replay,BehaviorExample.Registry registry,String taskId){if(input!=584||hidden!=32||replay==null||registry==null||registry.size()!=outputs)throw BehaviorExample.error("정책·행동 사전 형상 결합 오류");List<BehaviorExample> rows=replay.active(taskId);if(rows.isEmpty())throw BehaviorExample.error("확정한 시범 없음");for(BehaviorExample row:rows){registry.indexOf(row.actionId);if(!row.before.previousActionId.isEmpty())registry.indexOf(row.before.previousActionId);}return rows;}
    public BehaviorPolicy learn(BehaviorExample.Replay replay,BehaviorExample.Registry registry,String taskId,int updates,int batchSize,double lr,Cancel cancellation){
        if(outputs<2||updates<1||updates>600||batchSize<1||batchSize>32||!Double.isFinite(lr)||lr<=0||lr>1||cancellation==null)throw BehaviorExample.error("학습 설정·다중 행동·취소 계약 오류");
        if(steps<0||steps>Long.MAX_VALUE-updates)throw BehaviorExample.error("완료 학습 단계의 비음수·덧셈 범위 오류");
        List<BehaviorExample> rows=rows(replay,registry,taskId);check(cancellation);BehaviorPolicy candidate=new BehaviorPolicy(this,outputs);
        try{for(int step=0;step<updates;step++){check(cancellation);double[][] x=new double[batchSize][];int[] y=new int[batchSize];for(int i=0;i<batchSize;i++){check(cancellation);BehaviorExample row=rows.get(candidate.nextInt(rows.size()));x[i]=BehaviorFeatures.encode(row.before);y[i]=registry.indexOf(row.actionId);}Gradient gradient=candidate.gradient(x,y);check(cancellation);double[] parameters=candidate.parameters();for(int i=0;i<parameters.length;i++){parameters[i]-=lr*gradient.values[i];finite(parameters[i]);}candidate.assign(parameters);candidate.steps++;UPDATES.incrementAndGet();}check(cancellation);return candidate;
        }catch(CancellationException e){DISCARDED.addAndGet(candidate.steps-steps);throw e;}
    }
    private static void check(Cancel cancellation){if(cancellation.cancelled())throw new CancellationException("시범 학습 후보 취소");}
    private long nextLong(){long z=(rng+=0x9E3779B97F4A7C15L);z=(z^(z>>>30))*0xBF58476D1CE4E5B9L;z=(z^(z>>>27))*0x94D049BB133111EBL;return z^(z>>>31);}
    private double nextDouble(){return (nextLong()>>>11)*0x1.0p-53;}
    private int nextInt(int bound){long bits,value;do{bits=nextLong()>>>1;value=bits%bound;}while(bits-value+(bound-1)<0);return (int)value;}
    static void finite(double value){if(!Double.isFinite(value))throw BehaviorExample.error("비유한 정책 값");}
    public byte[] weightBytes(){try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeLong(MAGIC);out.writeInt(input);out.writeInt(hidden);out.writeInt(outputs);for(double value:parameters()){finite(value);out.writeLong(Double.doubleToRawLongBits(value));}out.flush();return bytes.toByteArray();}catch(IOException e){throw new IllegalStateException(e);}}
    public String parameterHash(){return hex(digest(weightBytes()));}
    static byte[] digest(byte[] raw){try{return MessageDigest.getInstance("SHA-256").digest(raw);}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    static String hex(byte[] raw){StringBuilder s=new StringBuilder();for(byte b:raw)s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    public static BehaviorPolicy readWeights(byte[] raw,long seed,long rng,long steps,String initialHash){try{if(raw==null||raw.length<20||raw.length>300000||steps<0||initialHash==null||!initialHash.matches("[0-9a-f]{64}"))throw BehaviorExample.error("가중치 header·학습 상태 오류");DataInputStream in=new DataInputStream(new ByteArrayInputStream(raw));if(in.readLong()!=MAGIC)throw BehaviorExample.error("가중치 magic 오류");int d=in.readInt(),h=in.readInt(),k=in.readInt();dimensions(d,h,k);long count=(long)d*h+h+(long)h*k+k;if(raw.length!=20+8*count)throw BehaviorExample.error("잘리거나 남는 가중치 바이트");BehaviorPolicy p=new BehaviorPolicy(d,h,k,seed);double[] parameters=new double[(int)count];for(int i=0;i<parameters.length;i++)parameters[i]=Double.longBitsToDouble(in.readLong());p.assign(parameters);p.rng=rng;p.steps=steps;p.initialHash=initialHash;return p;}catch(IOException e){throw BehaviorExample.error("가중치 읽기 오류",e);}}
    public static JSONObject counters(){return BehaviorExample.object("model_instances",INSTANCES.get(),"random_initializations",INITIALS.get(),"forward_examples",FORWARD.get(),"backward_batches",BACKWARD.get(),"optimizer_steps",UPDATES.get(),"discarded_candidate_optimizer_steps",DISCARDED.get());}
}
