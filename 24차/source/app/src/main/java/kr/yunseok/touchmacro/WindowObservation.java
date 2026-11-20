package kr.yunseok.touchmacro;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** 실제 수락한 창 관측만 순서대로 반영해 이전 터치 허가를 폐기합니다. */
final class WindowObservation {
    private final AtomicLong revision;
    private long acceptedSequence=-1;
    private String acceptedKey;
    WindowObservation(AtomicLong revision){this.revision=revision;}
    synchronized boolean accept(long sequence,String key){
        if(sequence<=acceptedSequence)return false;
        // 키 변경과 기존 허가 세대의 증가는 같은 임계 구역에서 완료합니다.
        if(acceptedKey!=null&&!acceptedKey.equals(key))revision.incrementAndGet();
        acceptedSequence=sequence;acceptedKey=key;return true;
    }
    static String key(List<WindowPackages.Window> windows,String resolvedPackage){
        ArrayList<String> identities=new ArrayList<>();
        for(WindowPackages.Window window:windows)if(window.active||window.focused)
            identities.add(window.id+":"+window.type+":"+window.active+":"+window.focused);
        Collections.sort(identities);return resolvedPackage+"|"+String.join(";",identities);
    }
}
