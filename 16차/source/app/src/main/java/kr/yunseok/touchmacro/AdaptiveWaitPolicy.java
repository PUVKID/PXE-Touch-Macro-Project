package kr.yunseok.touchmacro;

/** 추가 화면 조회 없이 확정된 인접 글자 조건에만 적응 대기를 적용합니다. */
final class AdaptiveWaitPolicy {
    private AdaptiveWaitPolicy(){}
    static boolean eligible(Macro macro,Step current,Step next) {
        return macro.adaptiveWait&&"TEXT_TAP".equals(current.type)&&current.repeat==1
            &&next!=null&&next.enabled&&next.canWaitForever()&&current.source.equals(next.source)
            &&current.left==next.left&&current.top==next.top&&current.right==next.right&&current.bottom==next.bottom;
    }
    static long timeoutElapsed(long started,long leadingUntil,long now) {
        return Math.max(0,now-Math.max(started,leadingUntil));
    }
    static final class Reappearance {
        private int missing;
        private boolean armed;
        boolean observe(boolean present) {
            if(armed)return present;
            if(!present){missing++;if(missing>=2)armed=true;return false;}
            missing=0;return false;
        }
        void interrupt(){missing=0;armed=false;}
    }
}
