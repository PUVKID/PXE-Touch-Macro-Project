package kr.yunseok.touchmacro;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** 느린 플랫폼 조회는 한 개만 유지하며 UI를 막거나 추가 조회를 쌓지 않습니다. */
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
    static final class Busy extends IllegalStateException {Busy(){super("이전 화면 정보 응답을 기다리는 중입니다");}}
    private IllegalStateException unavailable(){return new Busy();}
    @Override public void close(){executor.shutdownNow();}
}
