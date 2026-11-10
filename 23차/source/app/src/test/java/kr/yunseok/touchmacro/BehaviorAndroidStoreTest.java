package kr.yunseok.touchmacro;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

/** 합성 저장 묶음만 읽으며 개인 자료·새 학습·추론을 사용하지 않습니다. */
public class BehaviorAndroidStoreTest {
    @Rule public TemporaryFolder folder=new TemporaryFolder();
    @Test public void offGatePrecedesReadingAndCreatingFiles()throws Exception{
        BehaviorTaskStore store=new BehaviorTaskStore(folder.getRoot());
        InputStream neverRead=new InputStream(){public int read(){throw new AssertionError("OFF에서는 읽기0");}};
        try{store.importNew(neverRead,"작업","example.target",false,()->true);fail();}catch(IllegalStateException expected){}
        assertEquals(0,Objects.requireNonNull(folder.getRoot().list()).length);
    }
    @Test public void malformedImportPreservesOriginalTaskBytes()throws Exception{
        BehaviorTaskStore store=new BehaviorTaskStore(folder.getRoot());BehaviorTaskStore.Task t=store.create("원 작업","example.target",true);
        Path current=new File(new File(new File(folder.getRoot(),"behavior"),t.storageId),"current.json").toPath();byte[] original=Files.readAllBytes(current);
        try{store.importNew(new ByteArrayInputStream(new byte[]{1,2,3}),"실패","example.target",true,()->true);fail();}catch(IllegalArgumentException expected){}
        assertArrayEquals(original,Files.readAllBytes(current));assertEquals(1,store.list().size());
    }
    @Test public void syntheticImportAndCancelledCommitKeepOriginalRevision()throws Exception{
        String source=System.getProperty("pxe.behavior.fixture");byte[] bytes;
        // 외부 경로 지정을 유지하고 기본 회귀 검사는 동봉한 검토 자원을 읽습니다.
        try(InputStream input=source==null?BehaviorAndroidStoreTest.class.getResourceAsStream("/합성학습_64단계.pxebehavior"):Files.newInputStream(Path.of(source))){
            assertNotNull("검토한 합성 검사 자원 필요",input);bytes=input.readAllBytes();
        }
        assertEquals("검토한 합성 자원의 해시가 다릅니다","f4d4ab78593d43b09bf753cfe647d25f4b33534391fe70f18f4802496badfbb5",BehaviorPolicy.hex(BehaviorPolicy.digest(bytes)));
        BehaviorBundle.Snapshot sample=BehaviorBundle.read(new ByteArrayInputStream(bytes));
        String target=sample.replay.all().get(0).before.binding.targetPackage;
        BehaviorTaskStore store=new BehaviorTaskStore(folder.getRoot());BehaviorTaskStore.Task imported=store.importNew(new ByteArrayInputStream(bytes),"합성 검사",target,true,()->true);
        BehaviorTaskStore.Task loaded=store.load(imported.storageId,true);assertEquals(64,loaded.snapshot.policy.completedSteps());assertEquals(sample.taskId,loaded.taskId);
        Path current=new File(new File(new File(folder.getRoot(),"behavior"),loaded.storageId),"current.json").toPath();byte[] original=Files.readAllBytes(current);
        BehaviorBundle.Snapshot next=new BehaviorBundle.Snapshot(loaded.taskId,loaded.revision+1,loaded.registry(),loaded.replay(),loaded.snapshot.policy);
        try(BehaviorTaskStore.Prepared p=store.prepare(loaded,next,true)){try{store.commit(p,()->false);fail();}catch(IOException expected){}}
        assertArrayEquals(original,Files.readAllBytes(current));assertEquals(1,store.list().size());
        assertEquals(0,BehaviorPolicy.counters().getLong("optimizer_steps"));assertEquals(0,BehaviorPolicy.counters().getLong("forward_examples"));
    }
    @AfterClass public static void counters(){System.out.println("BEHAVIOR_ANDROID_CPU_COUNTERS="+BehaviorPolicy.counters());System.out.println("CPU="+Runtime.getRuntime().availableProcessors()+" HEAP_MAX="+Runtime.getRuntime().maxMemory());}
}
