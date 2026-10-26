package kr.yunseok.touchmacro;

/** 실패와 늦은 콜백을 포함한 실제 응답 시점부터 캡처 간격을 지킵니다. */
final class CapturePacer {
    private long lastResponse,interval=2000;
    CapturePacer(long now){lastResponse=now;}
    synchronized long remaining(long now){return Math.max(0,lastResponse+interval-now);}
    synchronized void responded(long now){lastResponse=Math.max(lastResponse,now);}
    synchronized void rejected(long now){responded(now);interval=Math.min(12000,interval*2);}
}
