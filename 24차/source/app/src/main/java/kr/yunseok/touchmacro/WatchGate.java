package kr.yunseok.touchmacro;

/** Reserve before entering. Only repeated, stable absence can release the reservation. */
public final class WatchGate {
    private boolean blocked;
    private long absentSince=-1;
    private int absentScans;
    public WatchGate(boolean blocked){this.blocked=blocked;}
    public boolean blocked(){return blocked;}
    public void reserve(){blocked=true;interruptAbsence();}
    public void interruptAbsence(){absentSince=-1;absentScans=0;}
    public boolean observe(boolean present,long now,int minimumAbsenceMs){
        if(!blocked)return present;
        if(present){interruptAbsence();return false;}
        if(absentSince<0)absentSince=now;
        absentScans++;
        if(absentScans>=2&&now-absentSince>=minimumAbsenceMs){blocked=false;interruptAbsence();}
        return false;
    }
}
