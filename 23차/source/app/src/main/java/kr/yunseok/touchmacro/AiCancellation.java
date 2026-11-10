package kr.yunseok.touchmacro;

import java.util.concurrent.CancellationException;

/** 취소 신호와 실제 추론 자원 수명은 분리합니다. */
public final class AiCancellation {
    private boolean cancelled;
    private Runnable terminate;
    private Exception terminationProblem;
    public synchronized void cancel(){if(cancelled)return;cancelled=true;if(terminate!=null)terminate.run();}
    public synchronized boolean cancelled(){return cancelled;}
    public synchronized void check(){if(cancelled||Thread.currentThread().isInterrupted())throw new CancellationException("AI 계획 생성이 취소되었습니다");}
    public synchronized void attach(Runnable callback){check();terminate=callback;}
    public synchronized void detach(){terminate=null;}
    public synchronized void terminationProblem(Exception error){terminationProblem=error;}
    public synchronized Exception terminationProblem(){return terminationProblem;}
}
