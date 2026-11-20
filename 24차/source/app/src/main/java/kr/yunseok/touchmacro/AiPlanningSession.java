package kr.yunseok.touchmacro;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 실행 중 하나와 최신 대기 하나만 보관하며 오래된 결과를 적용하지 않습니다. */
public final class AiPlanningSession implements AutoCloseable {
    public interface Guard {boolean valid();}
    public interface Work<T> {T run(AiCancellation cancellation) throws Exception;}
    public interface Listener<T> {void ready(long request,T result);void failed(long request,Exception error);}
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(1),r->{Thread t=new Thread(r,"pxe-ai");t.setDaemon(true);return t;});
    private final Executor delivery;
    private long revision;
    private boolean closed;
    private AiCancellation cancellation;
    private Guard guard;
    public AiPlanningSession(Executor delivery){this.delivery=delivery;}
    public synchronized <T> long submit(Guard guard,Work<T> work,Listener<T> listener) {
        if(closed)throw AiJson.error("계획 화면이 닫혔습니다");
        cancel();long request=revision;AiCancellation token=new AiCancellation();this.cancellation=token;this.guard=guard;
        worker.getQueue().clear();worker.execute(()->{
            try {token.check();T value=work.run(token);token.check();delivery.execute(()->{synchronized(AiPlanningSession.this){if(current(request))listener.ready(request,value);}});}
            catch(Exception error){delivery.execute(()->{synchronized(AiPlanningSession.this){if(current(request))listener.failed(request,error);}});}
        });return request;
    }
    public synchronized boolean current(long request){return !closed&&request==revision&&cancellation!=null&&!cancellation.cancelled()&&guard!=null&&guard.valid();}
    public synchronized boolean commit(long request,Runnable action){if(!current(request))return false;action.run();cancel();return true;}
    public synchronized void cancel(){revision++;if(cancellation!=null)cancellation.cancel();cancellation=null;guard=null;worker.getQueue().clear();}
    public synchronized int queued(){return worker.getQueue().size();}
    @Override public synchronized void close(){if(closed)return;cancel();closed=true;worker.shutdownNow();}
}
