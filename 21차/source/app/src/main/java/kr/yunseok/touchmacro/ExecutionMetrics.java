package kr.yunseok.touchmacro;

/** 활성 시간만 측정합니다. 단계에는 OCR·캡처가 포함되므로 서로 더하지 않습니다. */
final class ExecutionMetrics {
    private final long[] totals=new long[4],began={-1,-1,-1,-1},limits=new long[4];
    private int steps,ocrCount;
    private boolean finished;
    private void begin(int index,long now,long limit){if(!finished){began[index]=now;limits[index]=limit;}}
    private long duration(int index,long now){return began[index]<0?0:Math.max(0,(limits[index]>0?Math.min(now,limits[index]):now)-began[index]);}
    private void end(int index,long now){if(!finished&&began[index]>=0){totals[index]+=duration(index,now);began[index]=-1;if(index==0)steps++;if(index==1)ocrCount++;}}
    synchronized void beginStep(long now){begin(0,now,0);}
    synchronized void endStep(long now){end(0,now);}
    synchronized void beginOcr(long now){begin(1,now,0);}
    synchronized void endOcr(long now){end(1,now);}
    synchronized void beginCapture(long now){begin(2,now,0);}
    synchronized void endCapture(long now){end(2,now);}
    synchronized void beginConfigured(long now,long limit){begin(3,now,limit);}
    synchronized void endConfigured(long now){end(3,now);}
    synchronized void finish(long now){if(finished)return;for(int i=0;i<4;i++){totals[i]+=duration(i,now);began[i]=-1;}finished=true;}
    synchronized Snapshot snapshot(long now){return new Snapshot(totals[0]+duration(0,now),steps,totals[1]+duration(1,now),ocrCount,totals[2]+duration(2,now),totals[3]+duration(3,now));}
    static final class Snapshot {
        final long stepMs,ocrMs,captureMs,configuredMs;
        final int steps,ocrCount;
        Snapshot(long stepMs,int steps,long ocrMs,int ocrCount,long captureMs,long configuredMs){this.stepMs=stepMs;this.steps=steps;this.ocrMs=ocrMs;this.ocrCount=ocrCount;this.captureMs=captureMs;this.configuredMs=configuredMs;}
    }
}
