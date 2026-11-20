package kr.yunseok.touchmacro;

/** 실행 루프가 확정한 완료 횟수와 현재 회차를 한 번에 전달하는 표시 전용 값입니다. */
public final class RunProgress {
    public final int total,current,completed,step,steps;
    public final String action;
    public RunProgress(int total,int current,int completed,int step,int steps,String action){
        this.total=total;this.current=current;this.completed=completed;this.step=step;this.steps=steps;this.action=action;
    }
    public String counter(){
        if(total<0)return "";
        return completed+"/"+(total==0?"∞":total)+"회 완료"+(current>completed?" · "+current+"회째":"");
    }
    public String stage(){return step>0?"동작 "+step+"/"+steps+" · "+action:"시작 준비";}
}
