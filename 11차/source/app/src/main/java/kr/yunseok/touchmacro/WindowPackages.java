package kr.yunseok.touchmacro;

import java.util.*;

/** 노드를 읽지 않고, 실제 활성 창과 그 창에서 받은 패키지 이벤트를 대조합니다. */
final class WindowPackages {
    static final int APPLICATION=1, ACCESSIBILITY_OVERLAY=4;
    static final class Window {
        final int id,type;final boolean active,focused;
        Window(int id,int type,boolean active,boolean focused){this.id=id;this.type=type;this.active=active;this.focused=focused;}
    }
    private static final class Entry {
        final String name;final long time;
        Entry(String name,long time){this.name=name;this.time=time;}
    }
    private final LinkedHashMap<Integer,Entry> entries=new LinkedHashMap<>();
    synchronized void observe(int id,String name,long time){
        if(id<0||name==null||name.isEmpty())return;
        Entry previous=entries.get(id);if(previous!=null&&time<=previous.time)return;
        put(id,new Entry(name,time));
    }
    synchronized void remove(int id,long time){
        Entry previous=entries.get(id);if(previous!=null&&time<previous.time)return;
        put(id,new Entry("",time));
    }
    private void put(int id,Entry entry){entries.remove(id);entries.put(id,entry);while(entries.size()>128)entries.remove(entries.keySet().iterator().next());}
    synchronized String resolve(List<Window> windows,String ownPackage){
        Window active=null,focused=null;
        for(Window window:windows){
            if(window.active){if(active!=null)return "";active=window;}
            if(window.focused&&window.type==APPLICATION){if(focused!=null)return "";focused=window;}
        }
        if(active==null)return "";
        // 자신의 단축창이 활성화됐을 때만 뒤의 입력 초점 창을 확인합니다.
        if(active.type==ACCESSIBILITY_OVERLAY){Entry overlay=entries.get(active.id);if(overlay==null||!overlay.name.equals(ownPackage))return "";active=focused;}
        if(active==null||active.type!=APPLICATION)return "";
        Entry entry=entries.get(active.id);return entry==null?"":entry.name;
    }
}
