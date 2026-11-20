package kr.yunseok.touchmacro;
import org.junit.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** 대기 중인 추론을 제어하는 가짜 작업은 이 검사 안에서만 사용합니다. */
public class AiPlanningSessionTest {
    private AiPlanningSession.Listener<String> listener(AtomicInteger ready,CountDownLatch delivered){return new AiPlanningSession.Listener<String>(){public void ready(long id,String value){ready.incrementAndGet();delivered.countDown();}public void failed(long id,Exception error){throw new AssertionError(error);}};}
    @Test public void cancellationTerminatesWithoutClosingTheActiveRunResource(){
        AiCancellation token=new AiCancellation();AtomicInteger termination=new AtomicInteger();token.attach(termination::incrementAndGet);token.cancel();token.cancel();assertEquals(1,termination.get());assertTrue(token.cancelled());try{token.check();fail();}catch(CancellationException expected){}token.detach();
    }
    @Test public void stoppedRequestCannotDeliverOrCommitLate() throws Exception {
        AiPlanningSession session=new AiPlanningSession(Runnable::run);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),finished=new CountDownLatch(1);AtomicInteger ready=new AtomicInteger(),actions=new AtomicInteger();
        try{long request=session.submit(()->true,token->{entered.countDown();release.await(2,TimeUnit.SECONDS);finished.countDown();return "늦은 결과";},listener(ready,new CountDownLatch(1)));assertTrue(entered.await(1,TimeUnit.SECONDS));session.cancel();release.countDown();assertTrue(finished.await(1,TimeUnit.SECONDS));assertFalse(session.commit(request,actions::incrementAndGet));assertEquals(0,actions.get());assertEquals(0,ready.get());}finally{release.countDown();session.close();}
    }
    @Test public void snapshotChangeBlocksPreviewAndCommitEvenAfterPauseResume() throws Exception {
        AiPlanningSession session=new AiPlanningSession(Runnable::run);AtomicInteger generation=new AtomicInteger();CountDownLatch preview=new CountDownLatch(1);AtomicInteger ready=new AtomicInteger(),actions=new AtomicInteger();
        try{long request=session.submit(()->generation.get()==0,token->"계획",listener(ready,preview));assertTrue(preview.await(1,TimeUnit.SECONDS));generation.addAndGet(2);assertFalse(session.commit(request,actions::incrementAndGet));assertEquals(0,actions.get());}finally{session.close();}
    }
    @Test public void onlyLatestRequestWaitsAndPreviewCommitIsOneShot() throws Exception {
        AiPlanningSession session=new AiPlanningSession(Runnable::run);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),preview=new CountDownLatch(1);AtomicInteger ready=new AtomicInteger(),actions=new AtomicInteger();
        try{session.submit(()->true,token->{entered.countDown();release.await(2,TimeUnit.SECONDS);return "이전";},listener(ready,preview));assertTrue(entered.await(1,TimeUnit.SECONDS));long latest=0;for(int i=0;i<20;i++){latest=session.submit(()->true,token->"최신",listener(ready,preview));assertTrue(session.queued()<=1);}release.countDown();assertTrue(preview.await(1,TimeUnit.SECONDS));assertEquals(1,ready.get());assertTrue(session.commit(latest,actions::incrementAndGet));assertFalse(session.commit(latest,actions::incrementAndGet));assertEquals(1,actions.get());}finally{release.countDown();session.close();}
    }
}
