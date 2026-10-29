package kr.yunseok.touchmacro;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class QueryLaneTest {
    @Test public void cancelledSlowReadDoesNotQueueOrRunNewReads()throws Exception{
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger calls=new AtomicInteger();
        try(QueryLane lane=new QueryLane()){
            CompletableFuture<Integer> slow=lane.submit(()->{entered.countDown();release.await();return 7;});
            assertTrue(entered.await(1,TimeUnit.SECONDS));slow.cancel(false);
            for(int i=0;i<100;i++){
                try{lane.submit(()->calls.incrementAndGet()).get(1,TimeUnit.SECONDS);fail("Busy reader accepted more work");}catch(ExecutionException expected){}
            }
            assertEquals(0,calls.get());release.countDown();
            long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);boolean recovered=false;
            while(System.nanoTime()<until){try{assertEquals(Integer.valueOf(9),lane.submit(()->9).get(1,TimeUnit.SECONDS));recovered=true;break;}catch(ExecutionException e){Thread.yield();}}
            assertTrue(recovered);assertTrue(slow.isCancelled());
        }finally{release.countDown();}
    }
    @Test public void sequentialReadsDoNotRaceExecutorCompletion()throws Exception{
        try(QueryLane lane=new QueryLane()){for(int i=0;i<1000;i++){final int n=i;assertEquals(Integer.valueOf(n),lane.submit(()->n).get(2,TimeUnit.SECONDS));}}
    }
    @Test public void closedReaderRejectsWithoutExecuting()throws Exception{
        QueryLane lane=new QueryLane();lane.close();
        try{lane.submit(()->{fail("Executed after close");return 1;}).get(1,TimeUnit.SECONDS);fail("Accepted after close");}catch(ExecutionException expected){}
    }
}
