package kr.yunseok.touchmacro;

import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** 원 captureOnce 준비 콜백 하나만 늦추며 메인 스레드를 막지 않는 검사 경계입니다. */
final class UiPreparationDelay implements AutoCloseable {
    private final GuidedFlowTest h;
    private final Field field;
    private final Handler original,controlled;
    private final CountDownLatch selected=new CountDownLatch(1);
    final AtomicInteger delayedCount=new AtomicInteger();
    private volatile Runnable callback;
    private volatile CompletableFuture<?> future;
    volatile long selectedAtNs,selectedAtUptime;

    UiPreparationDelay(GuidedFlowTest owner)throws Exception {
        h=owner;field=MacroService.class.getDeclaredField("ui");field.setAccessible(true);original=(Handler)field.get(h.svc);
        controlled=new Handler(original.getLooper()){
            @Override public boolean sendMessageAtTime(Message message,long when){
                Runnable job=message.getCallback();boolean chosen=false;
                if(job!=null&&callback==null){
                    Method method=job.getClass().getEnclosingMethod();boolean capture=false;
                    if(method!=null&&method.getDeclaringClass()==MacroService.class&&method.getName().equals("onUi")){
                        for(StackTraceElement row:Thread.currentThread().getStackTrace())if(row.getClassName().equals(MacroService.class.getName())&&row.getMethodName().equals("captureOnce"))capture=true;
                    }
                    if(capture)synchronized(UiPreparationDelay.this){if(callback==null){
                        try{
                            for(Field item:job.getClass().getDeclaredFields())if(CompletableFuture.class.isAssignableFrom(item.getType())){item.setAccessible(true);assertNull(future);future=(CompletableFuture<?>)item.get(job);}
                            assertNotNull("원 준비 future를 확인할 수 없습니다",future);
                        }catch(IllegalAccessException error){throw new AssertionError(error);}
                        callback=job;selectedAtNs=SystemClock.elapsedRealtimeNanos();selectedAtUptime=SystemClock.uptimeMillis();when=selectedAtUptime+6000;chosen=true;delayedCount.incrementAndGet();
                    }}
                }
                boolean posted=super.sendMessageAtTime(message,when);
                if(chosen){assertTrue("준비 지연 작업을 게시하지 못했습니다",posted);selected.countDown();}
                return posted;
            }
        };
        try{h.main(()->set(controlled));}
        catch(RuntimeException|Error error){try{h.main(()->set(original));}catch(RuntimeException|Error restore){error.addSuppressed(restore);}throw error;}
    }
    void awaitSelected()throws Exception{assertTrue("captureOnce의 첫 onUi 준비를 선택하지 못했습니다",selected.await(3,TimeUnit.SECONDS));assertEquals(1,delayedCount.get());}
    void awaitLateBoundary(){h.waitFor(()->SystemClock.uptimeMillis()>=selectedAtUptime+6250,8000,"지연 콜백의 원 전달 시각 뒤를 관측하지 못했습니다");}
    void assertSettled(){h.waitFor(()->future!=null&&future.isDone()&&!controlled.hasCallbacks(callback),1000,"준비 future 종료·큐 제거 또는 지연 전달이 완료되지 않았습니다");}
    private void set(Handler handler){try{field.set(h.svc,handler);assertSame(handler,field.get(h.svc));}catch(IllegalAccessException error){throw new AssertionError(error);}}
    @Override public void close()throws Exception {
        try{
            h.main(()->h.svc.stop());
            Field worker=MacroService.class.getDeclaredField("worker");worker.setAccessible(true);
            ((ExecutorService)worker.get(h.svc)).submit(()->{}).get(1500,TimeUnit.MILLISECONDS);
            if(callback!=null)assertSettled();
        }finally{h.main(()->set(original));}
    }
}
