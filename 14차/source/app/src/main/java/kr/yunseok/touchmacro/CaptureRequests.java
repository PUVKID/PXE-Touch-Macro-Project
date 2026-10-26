package kr.yunseok.touchmacro;

import java.util.HashSet;

/** 취소할 수 없는 캡처를 서비스 수명 동안 최대 두 개로 제한합니다. */
final class CaptureRequests {
    private final HashSet<Long> pending=new HashSet<>();
    private long nextId=0;
    synchronized long begin(){
        if(pending.size()>=2)return 0;
        long id=++nextId;pending.add(id);return id;
    }
    synchronized boolean complete(long id){return pending.remove(id);}
    synchronized int size(){return pending.size();}
}
