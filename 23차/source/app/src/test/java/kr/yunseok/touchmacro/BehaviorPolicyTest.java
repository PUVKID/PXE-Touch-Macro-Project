package kr.yunseok.touchmacro;

import org.junit.Test;
import java.util.*;
import java.util.concurrent.CancellationException;
import static org.junit.Assert.*;

/** 고정 합성 자료의 실제 기울기와 제한된 가중치 갱신을 검사합니다. */
public class BehaviorPolicyTest {
    static final BehaviorPolicy.Cancel NEVER=()->false;
    static void exact(BehaviorPolicy policy,BehaviorExample.Replay replay){for(BehaviorExample row:replay.active("task"))assertEquals(row.id,BehaviorExampleTest.registry().indexOf(row.actionId),policy.predict(BehaviorFeatures.encode(row.before)));}

    @Test public void numericGradientTinyAllParametersAndProductionSamples(){
        for(BehaviorPolicy policy:List.of(new BehaviorPolicy(3,4,3,1730),new BehaviorPolicy(3,1730))){
            double[] x=new double[policy.inputSize()];x[0]=0.2;x[1]=-0.4;x[2]=0.1;
            double[][] batch={x};int[] labels={1};double[] gradient=policy.gradient(batch,labels).values;
            int stride=policy.inputSize()==3?1:Math.max(1,gradient.length/32);
            for(int index=0;index<gradient.length;index+=stride){
                double[] plus=policy.parameters(),minus=policy.parameters();plus[index]+=1e-6;minus[index]-=1e-6;
                double numeric=(policy.withParameters(plus).loss(batch,labels)-policy.withParameters(minus).loss(batch,labels))/(2e-6);
                double difference=Math.abs(numeric-gradient[index]),scale=Math.max(Math.abs(numeric),Math.abs(gradient[index]));
                assertTrue("gradient "+index+" "+numeric+" "+gradient[index],difference<=1e-8||difference<=1e-5*scale);
            }
        }
    }
    @Test public void dynamicKAppendPreservesOldRowsAndActualParameterCount(){
        BehaviorPolicy initial=new BehaviorPolicy(3,1730);BehaviorPolicy expanded=initial.appendOutputs(500);
        assertEquals(18720+33*500,expanded.parameterCount());
        assertEquals(281760,expanded.parameterCount()*8);
        assertArrayEquals(initial.firstLayerParameters(),expanded.firstLayerParameters(),0);
        for(int i=0;i<3;i++)assertArrayEquals(initial.outputRow(i),expanded.outputRow(i),0);
        assertEquals(3,initial.outputSize());assertEquals(500,expanded.outputSize());
        assertThrows(IllegalArgumentException.class,()->expanded.appendOutputs(501));
        BehaviorPolicy one=new BehaviorPolicy(1,1730);assertEquals(0,one.loss(new double[][]{new double[584]},new int[]{0}),0);
        assertThrows(IllegalArgumentException.class,()->one.learn(BehaviorExampleTest.replay(),BehaviorExampleTest.registry(),"task",1,8,0.25,NEVER));
    }
    @Test public void actualLearningAndCorrectionKeepPreviousValidExamples(){
        BehaviorPolicy initial=new BehaviorPolicy(3,1730);double[] before=initial.parameters();
        var replay=BehaviorExampleTest.replay();var registry=BehaviorExampleTest.registry();
        double firstLoss=initial.replayLoss(replay,registry,"task");
        BehaviorPolicy learned=initial.learn(replay,registry,"task",200,8,0.25,NEVER);
        exact(learned,replay);assertEquals(200,learned.completedSteps());assertEquals(0,initial.completedSteps());
        assertFalse(Arrays.equals(before,learned.parameters()));assertArrayEquals(before,initial.parameters(),0);
        assertTrue(learned.replayLoss(replay,registry,"task")<firstLoss);
        var corrected=replay.append(BehaviorExampleTest.example("c0","가","back","e0"));
        BehaviorPolicy after=learned.learn(corrected,registry,"task",100,8,0.25,NEVER);
        exact(after,corrected);assertEquals(300,after.completedSteps());exact(learned,replay);
        System.out.println("SYNTHETIC_LEARNING "+new org.json.JSONObject().put("seed",1730).put("first_loss",firstLoss).put("after200_loss",learned.replayLoss(replay,registry,"task")).put("after300_corrected_loss",after.replayLoss(corrected,registry,"task")).put("initial_hash",initial.parameterHash()).put("learned_hash",learned.parameterHash()).put("corrected_hash",after.parameterHash()).put("completed_steps",after.completedSteps()));
    }
    @Test public void cancelledCandidateNeverMutatesOriginal(){
        BehaviorPolicy initial=new BehaviorPolicy(3,1730);double[] before=initial.parameters();
        assertThrows(CancellationException.class,()->initial.learn(BehaviorExampleTest.replay(),BehaviorExampleTest.registry(),"task",10,8,0.25,()->true));
        long updates=BehaviorPolicy.counters().getLong("optimizer_steps");
        assertThrows(CancellationException.class,()->initial.learn(BehaviorExampleTest.replay(),BehaviorExampleTest.registry(),"task",10,8,0.25,()->BehaviorPolicy.counters().getLong("optimizer_steps")>updates));
        assertArrayEquals(before,initial.parameters(),0);assertEquals(0,initial.completedSteps());
    }
    @Test public void nonfiniteShapeAndLabelsRejected(){
        BehaviorPolicy p=new BehaviorPolicy(3,1730);double[] bad=new double[584];bad[2]=Double.NaN;
        assertThrows(IllegalArgumentException.class,()->p.predict(bad));
        assertThrows(IllegalArgumentException.class,()->p.predict(new double[3]));
        assertThrows(IllegalArgumentException.class,()->p.gradient(new double[][]{new double[584]},new int[]{3}));
        double[] parameters=p.parameters();parameters[0]=Double.POSITIVE_INFINITY;
        assertThrows(IllegalArgumentException.class,()->p.withParameters(parameters));
    }
    static BehaviorPolicy withSteps(long steps){
        BehaviorPolicy initial=new BehaviorPolicy(3,1730);
        return BehaviorPolicy.readWeights(initial.weightBytes(),1730,initial.rngState(),steps,initial.initialHash());
    }
    static void setStepsForInvariantProbe(BehaviorPolicy policy,long steps)throws Exception{
        // 외부 입력으로 만들 수 없는 손상 상태를 저장 방어 검사에만 주입합니다.
        var field=BehaviorPolicy.class.getDeclaredField("steps");field.setAccessible(true);field.setLong(policy,steps);
    }
    static void assertLearningRejectedWithoutWork(BehaviorPolicy policy){
        String counters=BehaviorPolicy.counters().toString(),hash=policy.parameterHash();long rng=policy.rngState(),steps=policy.completedSteps();
        assertThrows(IllegalArgumentException.class,()->policy.learn(BehaviorExampleTest.replay(),BehaviorExampleTest.registry(),"task",1,8,0.25,NEVER));
        assertEquals(counters,BehaviorPolicy.counters().toString());assertEquals(hash,policy.parameterHash());
        assertEquals(rng,policy.rngState());assertEquals(steps,policy.completedSteps());
    }
    @Test public void completedStepsOverflowRejectedBeforeCandidate(){
        assertLearningRejectedWithoutWork(withSteps(Long.MAX_VALUE));
    }
    @Test public void negativeCompletedStepsRejectedBeforeCandidate()throws Exception{
        BehaviorPolicy policy=new BehaviorPolicy(3,1730);setStepsForInvariantProbe(policy,-1);
        assertLearningRejectedWithoutWork(policy);
    }
    @Test public void lastValidStepRoundtripsAndFurtherLearningRejected()throws Exception{
        BehaviorPolicy original=withSteps(Long.MAX_VALUE-1);String hash=original.parameterHash();long rng=original.rngState();
        BehaviorPolicy last=original.learn(BehaviorExampleTest.replay(),BehaviorExampleTest.registry(),"task",1,8,0.25,NEVER);
        assertEquals(Long.MAX_VALUE,last.completedSteps());assertEquals(Long.MAX_VALUE-1,original.completedSteps());
        assertEquals(hash,original.parameterHash());assertEquals(rng,original.rngState());assertNotEquals(hash,last.parameterHash());
        byte[] archive=BehaviorBundle.write(BehaviorBundleTest.snapshot(last));
        BehaviorPolicy imported=BehaviorBundle.read(new java.io.ByteArrayInputStream(archive)).policy;
        assertEquals(Long.MAX_VALUE,imported.completedSteps());assertEquals(last.parameterHash(),imported.parameterHash());assertEquals(last.rngState(),imported.rngState());
        assertLearningRejectedWithoutWork(last);assertLearningRejectedWithoutWork(imported);
        String output=System.getProperty("behavior.boundary.output");
        if(output!=null)java.nio.file.Files.write(java.nio.file.Path.of(output),archive);
        System.out.println("STEP_BOUNDARY "+BehaviorExample.object("metadata_before",original.completedSteps(),"metadata_after",last.completedSteps(),"physical_optimizer_steps",1,"original_hash",hash,"after_hash",last.parameterHash(),"bundle_sha256",BehaviorPolicy.hex(BehaviorPolicy.digest(archive)),"bundle_bytes",archive.length,"import_rng_identical",true));
    }
}
