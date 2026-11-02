package kr.yunseok.touchmacro;

import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.lang.reflect.Field;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 실제 하단 진행창의 크기와 터치 접기, 실행 중 복원을 검사합니다. */
@RunWith(AndroidJUnit4.class) public class RunHudTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    @Before public void boot()throws Exception{h.boot();h.main(()->Ui.prefs(h.a).edit().putBoolean("hud_collapsed",false).commit());h.fixture();}
    @After public void end(){h.end();}
    Object field(Object owner,String name)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    View hud()throws Exception{Object hud=field(h.svc.controller(),"hud");return hud==null?null:(View)field(hud,"view");}
    String text(View view){if(view instanceof TextView)return ((TextView)view).getText().toString();StringBuilder out=new StringBuilder();if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)out.append(text(((ViewGroup)view).getChildAt(i))).append('\n');return out.toString();}
    String hudText(){String[] value={""};h.main(()->{try{value[0]=text(hud());}catch(Exception e){throw new RuntimeException(e);}});return value[0];}
    void shot(String name)throws Exception{
        String prefix="w"+h.svc.size()[0]+"_f"+Math.round(h.a.getResources().getConfiguration().fontScale*100)+"_";
        h.screenshot(prefix+name);org.json.JSONObject data=new org.json.JSONObject();
        h.main(()->{try{View view=hud();data.put("screen_width",h.svc.size()[0]);data.put("screen_height",h.svc.size()[1]);data.put("density",h.a.getResources().getDisplayMetrics().density);data.put("font_scale",h.a.getResources().getConfiguration().fontScale);data.put("bounds",h.bounds(view).toShortString());org.json.JSONArray lines=new org.json.JSONArray();for(int i=0;i<((ViewGroup)view).getChildCount();i++){TextView line=(TextView)((ViewGroup)view).getChildAt(i);org.json.JSONObject entry=new org.json.JSONObject();entry.put("text",line.getText().toString());entry.put("shown",line.isShown());entry.put("lines",line.getLineCount());entry.put("first_line_ellipsis",line.getLayout()==null?-1:line.getLayout().getEllipsisCount(0));entry.put("bounds",h.bounds(line).toShortString());lines.put(entry);}data.put("text_views",lines);}catch(Exception e){throw new RuntimeException(e);}});
        try(java.io.FileOutputStream file=new java.io.FileOutputStream(new java.io.File(h.a.getExternalFilesDir(null),prefix+name.replace(".png",".json")))){file.write(data.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    }
    void begin(){Macro m=new Macro();m.rounds=15;m.startDelay=0;m.gap=0;Step s=new Step("WAIT");s.duration=5000;s.after=0;m.steps.add(s);h.main(()->h.svc.start(m));h.waitFor(()->h.svc.hasRunStats(),3000,"실행 초기화 없음");}
    @Test public void bottomHudIsSmallAndTappingCollapsesWithoutResetOnUpdates()throws Exception{
        begin();h.waitFor(()->{try{return hud()!=null&&hud().isShown();}catch(Exception e){return false;}},4000,"하단 진행창 없음");
        View[] view={null};Rect[] initial={null};h.main(()->{try{view[0]=hud();initial[0]=h.bounds(view[0]);}catch(Exception e){throw new RuntimeException(e);}});
        assertTrue("기본 폭이 280dp를 넘습니다: "+initial[0].width(),initial[0].width()<=Ui.dp(h.a,280));
        assertTrue("하단 창 터치 접기가 없습니다",view[0].isClickable());
        assertTrue("실제 시작 완료 횟수 0/15가 없습니다: "+hudText(),hudText().contains("0/15"));
        h.tap(initial[0].exactCenterX(),initial[0].exactCenterY());h.in.waitForIdleSync();SystemClock.sleep(400);
        Rect[] collapsed={null};h.main(()->collapsed[0]=h.bounds(view[0]));
        assertTrue("접힘 폭이 200dp를 넘습니다",collapsed[0].width()<=Ui.dp(h.a,200));
        assertEquals("접힘 터치 높이",Ui.dp(h.a,48),collapsed[0].height());
        shot("PXE18_진행창_접힘.png");
        h.main(()->h.svc.controller().setCaptureMode(true));assertFalse(view[0].isShown());h.main(()->h.svc.controller().setCaptureMode(false));assertTrue(view[0].isShown());
        h.main(()->h.svc.togglePause());SystemClock.sleep(300);assertTrue(hudText().contains("일시정지"));
        Rect[] after={null};h.main(()->after[0]=h.bounds(view[0]));assertEquals(collapsed[0].height(),after[0].height());
        h.tap(after[0].exactCenterX(),after[0].exactCenterY());h.in.waitForIdleSync();assertTrue(view[0].getHeight()>collapsed[0].height());
        shot("PXE18_진행창_펼침.png");
        h.main(()->h.svc.stop());SystemClock.sleep(350);assertTrue(hudText().contains("정지"));
    }
    @Test public void fifteenRoundsShowCompletedSevenAndCurrentEightThenExactlyFifteen()throws Exception{
        final int[] taps={0};Rect[] at={null};h.main(()->{h.entry.setOnClickListener(v->taps[0]++);at[0]=h.bounds(h.entry);});int[] size=h.svc.size();
        Macro m=new Macro();m.rounds=15;m.startDelay=0;m.gap=100;Step tap=new Step("TAP");tap.x=at[0].exactCenterX()*100/size[0];tap.y=at[0].exactCenterY()*100/size[1];tap.duration=50;tap.after=0;m.steps.add(tap);Step wait=new Step("WAIT");wait.duration=300;wait.after=0;m.steps.add(wait);
        h.main(()->h.svc.start(m));h.waitFor(()->h.svc.hudProgress().completed==7&&h.svc.hudProgress().current==8,15000,"실제7회완료/8회째진입없음");h.main(()->h.svc.togglePause());int frozen=h.svc.hudProgress().completed;assertEquals(7,frozen);SystemClock.sleep(400);assertEquals(frozen,h.svc.hudProgress().completed);assertTrue(hudText(),hudText().contains("7/15"));assertTrue(hudText(),hudText().contains("8회째"));assertTrue(hudText().contains("일시정지"));shot("PXE18_7회완료_8회째.png");
        h.main(()->h.svc.togglePause());h.waitFor(()->!h.svc.running,15000,"15회완료안됨");assertEquals(15,taps[0]);assertEquals(15,h.svc.hudProgress().completed);assertEquals(0,h.svc.hudProgress().current);SystemClock.sleep(350);assertTrue(hudText(),hudText().contains("15/15"));assertTrue(hudText().contains("완료"));shot("PXE18_15회완료.png");
        h.main(()->{h.svc.start(m);h.svc.stop();h.svc.start(m);h.svc.togglePause();});SystemClock.sleep(400);assertEquals(0,h.svc.hudProgress().completed);assertEquals(0,h.svc.hudProgress().current);assertTrue(h.svc.paused);assertEquals("일시정지",h.svc.hudState());
    }
    @Test public void watchCountsRoundsSeparatelyAndResetsOnlyOnNextEntry()throws Exception{
        int[] entries={0},actions={0},secondEntryCompleted={-1};Button[] action={null};AlertDialog[] dialog={null};Rect[] inside={null},entry={null};
        h.main(()->{LinearLayout body=new LinearLayout(h.a);body.setPadding(40,100,40,100);action[0]=new Button(h.a);action[0].setText("실제 동작");action[0].setOnClickListener(v->actions[0]++);body.addView(action[0],new LinearLayout.LayoutParams(-1,Ui.dp(h.a,70)));dialog[0]=new AlertDialog.Builder(h.a).setView(body).create();dialog[0].show();});h.in.waitForIdleSync();
        h.main(()->{inside[0]=h.bounds(action[0]);dialog[0].dismiss();entry[0]=h.bounds(h.entry);h.entry.setOnClickListener(v->{entries[0]++;if(entries[0]==2)secondEntryCompleted[0]=h.svc.hudProgress().completed;dialog[0].show();});});
        int[] size=h.svc.size();Macro m=new Macro();m.name="진행 횟수 검증";m.startDelay=0;Step tap=new Step("TAP");tap.x=inside[0].exactCenterX()*100/size[0];tap.y=inside[0].exactCenterY()*100/size[1];tap.duration=40;tap.after=0;m.steps.add(tap);WatchConfig p=new WatchConfig();p.macroName=m.name;p.repetitions=15;p.rule.text="안내 대상";p.rule.source="ACCESS";p.rule.match="EXACT";p.targetPackage=h.a.getPackageName();p.startMs=0;p.entryMs=500;p.repeatGapMs=0;p.beforeBackMs=100;p.returnMs=300;p.cooldownMs=1200;p.intervalSeconds=2;p.preventDuplicate=false;p.waitForRefresh=true;p.settleMs=300;
        h.main(()->{Store.put(h.a,m);h.svc.startWatch(p);});try{h.waitFor(()->h.svc.watchCompleted==1,15000,"첫탐지바퀴미완료");assertEquals(15,actions[0]);assertEquals(15,h.svc.hudProgress().completed);assertEquals(1,h.svc.watchCompleted);assertEquals("대기",h.svc.hudState());shot("PXE18_완료후_새로고침대기.png");h.waitFor(()->entries[0]==2,12000,"새탐지진입없음");assertEquals(0,secondEntryCompleted[0]);assertEquals(0,h.svc.hudProgress().completed);}finally{h.main(()->{h.svc.stop();dialog[0].dismiss();});}
    }
    Button fullScreenTarget(int[] taps){Button[] target={null};h.main(()->{FrameLayout body=new FrameLayout(h.a);target[0]=new Button(h.a);target[0].setText("하단 실제 대상");target[0].setOnClickListener(v->taps[0]++);body.addView(target[0],new FrameLayout.LayoutParams(-1,-1));h.a.setContentView(body);});h.in.waitForIdleSync();return target[0];}
    void shortStats(){Macro m=new Macro();m.startDelay=0;Step s=new Step("WAIT");s.duration=1;s.after=0;m.steps.add(s);h.main(()->h.svc.start(m));h.waitFor(()->h.svc.hasRunStats()&&!h.svc.running,4000,"기존통계생성안됨");SystemClock.sleep(300);}
    @Test public void automaticTapUnderHudReachesAppAndRestoresChosenSize()throws Exception{
        int[] taps={0};Button button=fullScreenTarget(taps);shortStats();View view=hud();Rect[] box={null};h.main(()->box[0]=h.bounds(view));int[] size=h.svc.size();Macro m=new Macro();m.startDelay=0;Step tap=new Step("TAP");tap.x=box[0].exactCenterX()*100/size[0];tap.y=box[0].exactCenterY()*100/size[1];tap.duration=70;tap.after=0;m.steps.add(tap);h.main(()->h.svc.start(m));h.waitFor(()->!h.svc.running,5000,"하단자동터치미완료");assertEquals("HUD가대상터치를가로챘습니다",1,taps[0]);h.waitFor(view::isShown,2000,"HUD복원실패");assertFalse(Ui.prefs(h.a).getBoolean("hud_collapsed",false));
        Rect[] target={null};h.main(()->target[0]=h.bounds(h.a.findViewById(android.R.id.content)));h.tap(target[0].left+Ui.dp(h.a,30),target[0].top+Ui.dp(h.a,200));h.waitFor(()->taps[0]==2,1000,"HUD밖수동터치가통과하지않습니다");assertEquals(2,taps[0]);
    }
    @Test public void recordedReplayCannotBeInterceptedByOldHud()throws Exception{
        int[] taps={0};fullScreenTarget(taps);shortStats();View view=hud();Rect[] box={null};long end=SystemClock.elapsedRealtime()+5000;h.main(()->{box[0]=h.bounds(view);h.svc.controller().beginRecording(true,-1);});assertFalse("녹화시작에이전HUD가남았습니다",view.isShown());h.waitFor(()->{boolean[] ready={false};h.main(()->{try{View capture=(View)field(h.svc.controller(),"capture");ready[0]=capture!=null&&capture.isShown()&&capture.isLaidOut()&&capture.getWidth()>0&&capture.getHeight()>0;}catch(Exception e){throw new RuntimeException(e);}});return ready[0];},Math.max(1,end-SystemClock.elapsedRealtime()),"녹화입력창준비안됨");h.tap(box[0].exactCenterX(),box[0].exactCenterY());h.waitFor(()->!h.svc.controller().recording,Math.max(1,end-SystemClock.elapsedRealtime()),"녹화재생미완료");assertEquals("녹화재생터치가HUD에가로채졌습니다",1,taps[0]);h.waitFor(view::isShown,2000,"녹화완료후HUD복원실패");
    }
    @Test public void bothThemesAndLongStoppedMessageStayInsideSmallViewport()throws Exception{
        shortStats();for(int mode:new int[]{1,2}){int theme=mode;h.main(()->{h.svc.controller().close();Ui.prefs(h.a).edit().putInt("theme",theme).putBoolean("hud_collapsed",false).commit();h.svc.controller().show();h.svc.log("정지 · "+"아주 긴 상태 안내 ".repeat(80));});SystemClock.sleep(400);View view=hud();Rect[] rect={null};h.main(()->rect[0]=h.bounds(view));assertTrue(rect[0].width()<=Ui.dp(h.a,280));assertTrue(rect[0].left>=0);assertTrue(rect[0].right<=h.svc.size()[0]);assertTrue(view.getHeight()>=Ui.dp(h.a,48));shot("PXE18_"+(mode==1?"라이트":"다크")+"_긴안내.png");h.tap(rect[0].exactCenterX(),rect[0].exactCenterY());h.in.waitForIdleSync();assertEquals(Ui.dp(h.a,48),view.getHeight());shot("PXE18_"+(mode==1?"라이트":"다크")+"_작은표시.png");}
    }
}
