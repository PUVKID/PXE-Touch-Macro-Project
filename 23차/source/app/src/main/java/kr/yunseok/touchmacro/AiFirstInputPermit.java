package kr.yunseok.touchmacro;

/** 준비가 끝난 뒤 새 대상 조회와 최초 입력 전달 사이의 세대를 결합합니다. */
final class AiFirstInputPermit {
    private final String target;
    private final long generation,pause,window;
    private boolean confirmed,consumed;
    public AiFirstInputPermit(String target,long generation,long pause,long window){if(target==null||target.isEmpty())throw AiJson.error("첫 입력 대상이 없습니다");this.target=target;this.generation=generation;this.pause=pause;this.window=window;}
    private boolean same(long generation,long pause,long window,boolean paused){return !consumed&&!paused&&this.generation==generation&&this.pause==pause&&this.window==window;}
    public synchronized boolean confirm(String actual,long generation,long pause,long window,boolean paused){confirmed=target.equals(actual)&&same(generation,pause,window,paused);return confirmed;}
    public synchronized boolean permitsDispatch(long generation,long pause,long window,boolean paused){return confirmed&&same(generation,pause,window,paused);}
    public boolean pauseChanged(long pause,boolean paused){return paused||this.pause!=pause;}
    public long window(){return window;}
    public synchronized void dispatched(){if(!confirmed||consumed)throw AiJson.error("첫 입력 허가를 재사용할 수 없습니다");consumed=true;}
}
