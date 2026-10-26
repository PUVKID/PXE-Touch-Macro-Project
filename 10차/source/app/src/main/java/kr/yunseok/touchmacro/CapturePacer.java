package kr.yunseok.touchmacro;

/** Pace from the actual response, including failures and late callbacks. */
final class CapturePacer {
    private long lastResponse,interval=2000;
    CapturePacer(long now){lastResponse=now;}
    synchronized long remaining(long now){return Math.max(0,lastResponse+interval-now);}
    synchronized void responded(long now){lastResponse=Math.max(lastResponse,now);}
    synchronized void rejected(long now){responded(now);interval=Math.min(12000,interval*2);}
}
