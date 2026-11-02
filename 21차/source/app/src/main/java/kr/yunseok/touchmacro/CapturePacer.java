package kr.yunseok.touchmacro;

/** 실패와 늦은 콜백을 포함한 실제 응답 시점부터 캡처 간격을 지킵니다. */
final class CapturePacer {
    private final long minimum;
    private long lastResponse,interval;
    CapturePacer(long now){this(now,500);}
    CapturePacer(long now,long minimum){
        if(minimum<=0||minimum>12000)throw new IllegalArgumentException("캡처 기본 간격을 확인하세요");
        this.minimum=minimum;lastResponse=now;interval=minimum;
    }
    synchronized long remaining(long now){return Math.max(0,lastResponse+interval-now);}
    synchronized void responded(long now){failed(now);interval=Math.max(minimum,interval/2);}
    synchronized void failed(long now){lastResponse=Math.max(lastResponse,now);}
    synchronized void rejected(long now){failed(now);interval=Math.min(12000,interval*2);}
}
