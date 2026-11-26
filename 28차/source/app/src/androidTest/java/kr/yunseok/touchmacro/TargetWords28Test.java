package kr.yunseok.touchmacro;
import android.os.*;
import android.view.accessibility.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** 저장 단축키와 진행 위젯이 같은 글자·새 실행을 사용하는지 실제 기기로 확인합니다. */
@RunWith(AndroidJUnit4.class) public class TargetWords28Test {
    final GuidedFlowTest h=new GuidedFlowTest();final Control28Test controls=new Control28Test();
    @Before public void boot()throws Exception{h.boot();h.fixture();h.main(()->Ui.prefs(h.a).edit().putString("target_words","[]").putString("word_match","EXACT").putBoolean("hud_expanded",false).apply());}
    @After public void end(){h.end();}
    Macro waitMacro(){Macro m=new Macro();m.name="글자 전환 동작";m.startDelay=0;m.maxMinutes=0;Step s=new Step("WAIT");s.duration=100;s.after=0;m.steps.add(s);return m;}
    @Test public void savedShortcutsDeduplicateDeleteAndDoNotRewriteMacro(){
        h.main(()->{String before=Store.load(h.a).json().toString();TargetWords.add(h.a," Adobe ");TargetWords.add(h.a,"Adobe");TargetWords.add(h.a,"배");assertEquals(2,TargetWords.items(h.a).size());TargetWords.select(h.a,"Adobe");assertEquals("Adobe",WatchStore.load(h.a).rule.text);assertEquals("Adobe",Ui.prefs(h.a).getString("word",""));assertEquals(before,Store.load(h.a).json().toString());TargetWords.remove(h.a,"배");assertEquals(java.util.Collections.singletonList("Adobe"),TargetWords.items(h.a));});
    }
    @Test public void mainScreenRegistersAndSelectsTheSameHorizontalShortcut()throws Exception{
        h.main(()->SetupFlow.finished(h.a));h.a=(MainActivity)h.in.startActivitySync(new android.content.Intent(h.in.getTargetContext(),MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK|android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK));h.in.waitForIdleSync();clickDescription("글자");h.click("등록 · 관리");h.click("새 글자 등록");
        AccessibilityNodeInfo input=findDescription("등록할 글자 입력");assertNotNull(input);Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"Adobe");assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args));input.recycle();h.click("등록");clickDescription("글자 단축키 · Adobe");assertEquals("Adobe",TargetWords.current(h.a));assertEquals(java.util.Collections.singletonList("Adobe"),TargetWords.items(h.a));h.screenshot("28_revised_favorites_main.png");
    }
    @Test public void shortcutRestartsWatchingFromFirstAreaAndPreservesSettings()throws Exception{
        AtomicInteger taps=new AtomicInteger();h.main(()->{h.entry.setText("새대상");h.entry.setOnClickListener(v->taps.incrementAndGet());Macro macro=waitMacro();Store.put(h.a,macro);WatchConfig p=new WatchConfig();p.rule.text="없는대상";p.rule.source="ACCESS";p.rule.match="EXACT";p.targetPackage=h.a.getPackageName();p.macroName=macro.name;p.startMs=0;p.entryMs=300;p.cooldownMs=600000;p.intervalSeconds=19;p.repetitions=15;WatchStore.save(h.a,p);h.svc.startWatch(p);});
        h.waitFor(()->h.svc.watching,4000,"탐지 실행 시작");long old=h.svc.runRevision();h.main(()->TargetWords.select(h.a,"새대상"));assertTrue(h.svc.runRevision()>old);assertFalse(h.svc.paused);assertEquals(19,WatchStore.load(h.a).intervalSeconds);assertEquals(15,WatchStore.load(h.a).repetitions);h.waitFor(()->taps.get()==1,10000,"새 단축키 글자 실제 터치");h.main(()->h.svc.stop());assertEquals(1,taps.get());
    }
    @Test public void activeSingleWordIsShownAndShortcutReplacesItWithoutOldTap()throws Exception{
        AtomicInteger taps=new AtomicInteger();h.main(()->{h.entry.setText("단일새글자");h.entry.setOnClickListener(v->taps.incrementAndGet());WatchConfig saved=new WatchConfig();saved.rule.text="예전탐지";WatchStore.save(h.a,saved);Macro m=new Macro();m.name="PXE 글자 모드";m.startDelay=0;m.maxMinutes=0;Step word=new Step("TEXT_TAP");word.text="단일이전글자";word.source="ACCESS";word.waitForever=true;word.timeout=0;word.after=0;m.steps.add(word);h.svc.restartWord(m);assertEquals("단일이전글자",TargetWords.current(h.a));});
        long before=h.svc.runRevision();h.main(()->TargetWords.select(h.a,"단일새글자"));assertTrue(h.svc.runRevision()>before);h.waitFor(()->taps.get()==1&&!h.svc.running,10000,"단일 글자 교체 터치");SystemClock.sleep(300);assertEquals(1,taps.get());assertEquals("단일새글자",TargetWords.current(h.a));
    }
    @Test public void ordinaryMacroWithTheSameNameIsNotRestartedOrRewritten(){
        h.main(()->{Macro m=waitMacro();m.name="PXE 글자 모드";m.rounds=0;m.gap=0;Store.save(h.a,m);String before=Store.load(h.a).json().toString();h.svc.start(m);long revision=h.svc.runRevision();TargetWords.select(h.a,"보존대상");assertTrue(h.svc.running);assertEquals(revision,h.svc.runRevision());assertEquals(before,Store.load(h.a).json().toString());});
    }
    @Test public void missingTextScrollUsesActualSlowHalfDistanceGesture()throws Exception{
        long[] down={0},elapsed={0};float[] first={0},travel={0};h.main(()->{((android.view.View)h.entry.getParent()).setOnTouchListener((v,event)->{if(event.getActionMasked()==android.view.MotionEvent.ACTION_DOWN){down[0]=event.getEventTime();first[0]=event.getRawY();}if(event.getActionMasked()==android.view.MotionEvent.ACTION_UP){elapsed[0]=event.getEventTime()-down[0];travel[0]=first[0]-event.getRawY();v.performClick();}return true;});Macro m=new Macro();m.startDelay=0;m.maxMinutes=0;Step word=new Step("TEXT_TAP");word.text="없는글자";word.source="ACCESS";word.timeout=300;word.failure="SCROLL_RETRY";word.retries=1;word.retryDelay=300;word.after=0;m.steps.add(word);h.svc.start(m);});
        h.waitFor(()->elapsed[0]>0&&!h.svc.running,10000,"글자 없음 자동 스크롤");assertTrue("완만한 실제 제스처 "+elapsed[0],elapsed[0]>=700&&elapsed[0]<3000);assertEquals(h.svc.size()[1]*.225,travel[0],30);
    }
    @Test public void invalidTargetAndCancelledEditorKeepOldExecution()throws Exception{
        h.main(()->{Ui.prefs(h.a).edit().putString("word_match","REGEX").putString("word","기존").apply();WatchConfig p=new WatchConfig();p.rule.text="기존";p.rule.match="REGEX";WatchStore.save(h.a,p);Macro m=waitMacro();m.rounds=0;m.gap=0;h.svc.start(m);});long old=h.svc.runRevision();h.main(()->{try{TargetWords.select(h.a," ");fail("빈 검색어 허용");}catch(IllegalArgumentException expected){}assertEquals("기존",TargetWords.current(h.a));TargetWords.showEditor(h.svc,()->{});});assertTrue(h.svc.paused);h.click("취소");assertTrue(h.svc.running);assertFalse(h.svc.paused);assertEquals(old,h.svc.runRevision());
    }
    @Test public void expandedHudHasHorizontalShortcutsAndEditableWordWithoutPauseStop()throws Exception{
        h.main(()->{TargetWords.add(h.a,"Adobe");TargetWords.add(h.a,"배");Macro m=waitMacro();m.rounds=0;m.gap=0;h.svc.start(m);});h.waitFor(()->findDescription("진행 상세 펼치기")!=null,5000,"진행 창 표시");
        clickDescription("진행 상세 펼치기");assertNotNull(findDescription("인식할 글자 바로 변경"));assertNull(findDescription("진행 창 일시정지"));assertNull(findDescription("진행 창 정지"));AccessibilityNodeInfo chip=findDescription("글자 단축키 · 배");assertNotNull(chip);assertTrue(chip.performAction(AccessibilityNodeInfo.ACTION_CLICK));chip.recycle();assertEquals("배",TargetWords.current(h.a));
        h.screenshot("28_revised_hud_expanded.png");clickDescription("인식할 글자 바로 변경");h.waitFor(()->h.svc.paused,2000,"글자 입력 동안 일시정지");h.waitFor(()->{for(AccessibilityWindowInfo window:h.auto.getWindows()){AccessibilityNodeInfo root=window.getRoot(),field=controls.input(root);if(root!=null)root.recycle();if(field!=null){Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"사과");boolean changed=field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args);field.recycle();return changed;}}return false;},4000,"하단 글자 입력");h.click("적용");assertEquals("사과",TargetWords.current(h.a));assertFalse(h.svc.paused);clickDescription("진행 상세 접기");h.screenshot("28_revised_hud_compact.png");
    }
    AccessibilityNodeInfo findDescription(String value){for(AccessibilityWindowInfo window:h.auto.getWindows()){AccessibilityNodeInfo root=window.getRoot(),found=controls.desc(root,value);if(root!=null)root.recycle();if(found!=null)return found;}return null;}
    void clickDescription(String value){h.waitFor(()->{AccessibilityNodeInfo n=findDescription(value);if(n==null)return false;n.recycle();return true;},4000,value);AccessibilityNodeInfo node=findDescription(value);assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK));node.recycle();h.in.waitForIdleSync();}
}
