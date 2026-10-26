package kr.yunseok.touchmacro;
import android.app.*;import android.graphics.*;import android.os.*;import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class) public class PriorityFlowTest {
 GuidedFlowTest h=new GuidedFlowTest();volatile int visits=0;volatile long firstBack=0,secondVisit=0;String[] order=new String[8];Button lower;
 @Before public void boot()throws Exception{h.boot();}
 @After public void end(){h.end();}
 @Test public void secondPriorityRepeatsAfterRefreshAndFirstPriorityWinsWhenPresent()throws Exception{
  h.fixture();h.main(()->{LinearLayout parent=(LinearLayout)h.entry.getParent();lower=new Button(h.a);lower.setText("대상 없음");LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,Ui.dp(h.a,70));lp.topMargin=Ui.dp(h.a,70);parent.addView(lower,lp);h.detail=new AlertDialog.Builder(h.a).setTitle("작업 화면").setMessage("완료 후 뒤로").create();h.detail.setOnDismissListener(d->{if(firstBack==0)firstBack=SystemClock.elapsedRealtime();});h.entry.setOnClickListener(v->visit("priority2"));lower.setOnClickListener(v->visit("priority1"));});h.in.waitForIdleSync();
  Rect[] top={null},bottom={null};h.main(()->{top[0]=h.bounds(h.entry);bottom[0]=h.bounds(lower);});int[] size=h.svc.size();
  WatchConfig p=new WatchConfig();p.rule.text="안내 대상";p.rule.source="ACCESS";p.rule.match="EXACT";p.priorityAreas.add(area(bottom[0],size));p.priorityAreas.add(area(top[0],size));p.startMs=0;p.entryMs=300;p.returnMs=300;p.settleMs=300;p.repeatGapMs=0;p.cooldownMs=2000;p.intervalSeconds=1;p.repetitions=1;p.targetPackage=h.a.getPackageName();
  Macro m=new Macro();m.name="우선순위 검증";Step wait=new Step("WAIT");wait.duration=10;wait.after=0;m.steps.add(wait);p.macroName=m.name;
  h.main(()->{Store.put(h.a,m);for(int i=0;i<p.areaCount();i++)WatchStore.blocked(h.a,p.areaConfig(i),false);h.svc.startWatch(p);});
  h.waitFor(()->h.svc.watchCompleted>=1,10000,"Second priority was not selected");assertEquals("priority2",order[0]);SystemClock.sleep(900);assertEquals("Re-executed before cooldown/refresh",1,visits);
  h.waitFor(()->h.svc.watchCompleted>=2,12000,"Same second priority did not repeat after refresh");assertEquals("priority2",order[1]);assertTrue("Post-cycle cooldown was bypassed",secondVisit-firstBack>=2000);
  h.main(()->lower.setText("안내 대상"));h.waitFor(()->h.svc.watchCompleted>=3,12000,"New first-priority text did not win");assertEquals("priority1",order[2]);h.main(()->h.svc.stop());
  android.util.Log.i("NEX_PRIORITY_QA","PASS: user-drawn bottom priority 1 and top priority 2, priority 2 selected when 1 absent, no rerun during cooldown, same priority 2 repeats after refresh, every refresh starts at priority 1, priority 1 wins once present");
 }
 void visit(String location){order[visits]=location;visits++;if(visits==2)secondVisit=SystemClock.elapsedRealtime();h.detail.show();}
 WatchArea area(Rect r,int[] size){return new WatchArea((r.left-2)*100.0/size[0],(r.top-2)*100.0/size[1],(r.right+2)*100.0/size[0],(r.bottom+2)*100.0/size[1]);}
}
