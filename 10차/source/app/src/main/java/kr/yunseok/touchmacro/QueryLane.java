package kr.yunseok.touchmacro;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** A stalled platform query must never block the UI or accumulate more queries. */
final class QueryLane implements AutoCloseable {
    private final AtomicBoolean busy=new AtomicBoolean();
    private final ThreadPoolExecutor executor=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(1),r->{Thread t=new Thread(r,"PXE-screen-reader");t.setDaemon(true);return t;});
    <T> CompletableFuture<T> submit(Callable<T> action){
        CompletableFuture<T> result=new CompletableFuture<>();
        if(!busy.compareAndSet(false,true)){result.completeExceptionally(unavailable());return result;}
        try{executor.execute(()->{
            T value=null;Exception failure=null;
            try{if(!result.isDone())value=action.call();}catch(Exception e){failure=e;}finally{busy.set(false);}
            if(failure==null)result.complete(value);else result.completeExceptionally(failure);
        });}
        catch(RejectedExecutionException e){busy.set(false);result.completeExceptionally(unavailable());}
        return result;
    }
    private IllegalStateException unavailable(){return new IllegalStateException("화면 정보 응답이 아직 끝나지 않았습니다. 대상 앱을 다시 열고 재시작하세요");}
    @Override public void close(){executor.shutdownNow();}
}
