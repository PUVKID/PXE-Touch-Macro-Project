package kr.yunseok.touchmacro;

import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 실제 설정 창의 기본 인식 선택과 저장 범위, 기존 고급 값 보존을 검사합니다. */
@RunWith(AndroidJUnit4.class) public class PxeRecognitionSettingsTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    String libraryBefore,currentBefore,wordSourceBefore;

    @Before public void boot()throws Exception{
        h.boot();h.main(()->{
            h.svc.controller().close();SetupFlow.finished(h.a);
            Macro m=new Macro();m.name="인식 설정 검사";
            Step s=new Step("TEXT_TAP");s.text="설정 검사 대상";s.source="OCR";s.match="EXACT";
            s.timeout=4250;s.after=750;s.offsetX=2.5;s.offsetY=-1.5;s.repeat=2;s.retries=4;s.retryDelay=1250;
            s.left=5;s.top=10;s.right=95;s.bottom=90;m.steps.add(s);Store.save(h.a,m);Store.put(h.a,m);
            Ui.prefs(h.a).edit().putString("word_source","AUTO").commit();
            WatchConfig p=new WatchConfig();p.rule.text="목록 검사 대상";p.rule.source="AUTO";p.macroName=m.name;
            p.entryMs=1250;p.repeatGapMs=250;p.beforeBackMs=500;p.returnMs=750;p.settleMs=1000;
            p.startMs=1750;p.cooldownMs=600000;p.priorityAreas.add(new WatchArea(5,20,95,40));WatchStore.save(h.a,p);
            h.a.refreshSetup();
        });h.in.waitForIdleSync();
        libraryBefore=Store.library(h.a).get("인식 설정 검사").json().toString();
        currentBefore=Store.load(h.a).json().toString();wordSourceBefore=Ui.prefs(h.a).getString("word_source","");
    }
    @After public void end()throws Exception{h.shell("input keyevent 4");h.end();}

    void open(String method){h.main(()->{try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(method);m.setAccessible(true);m.invoke(h.a);}catch(Exception e){throw new RuntimeException(e);}});h.in.waitForIdleSync();}
    void openStep(){h.main(()->{try{java.lang.reflect.Field field=MainActivity.class.getDeclaredField("macro");field.setAccessible(true);Macro m=(Macro)field.get(h.a);java.lang.reflect.Method edit=MainActivity.class.getDeclaredMethod("edit",Step.class);edit.setAccessible(true);edit.invoke(h.a,m.steps.get(0));}catch(Exception e){throw new RuntimeException(e);}});h.in.waitForIdleSync();}
    AccessibilityNodeInfo spinner(String label){
        AccessibilityNodeInfo title=h.find(label);assertNotNull("기본 설정에서 인식 방식에 도달할 수 있어야 합니다: "+label,title);
        AccessibilityNodeInfo parent=title.getParent();title.recycle();boolean after=false;
        try{for(int i=0;i<parent.getChildCount();i++){AccessibilityNodeInfo child=parent.getChild(i);if(child==null)continue;
            if(after&&"android.widget.Spinner".contentEquals(child.getClassName()))return child;
            if(label.contentEquals(child.getText()==null?"":child.getText()))after=true;child.recycle();}}
        finally{parent.recycle();}throw new AssertionError("인식 선택 상자가 없습니다: "+label);
    }
    void choose(String label,String choice){AccessibilityNodeInfo node=spinner(label);try{assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK));}finally{node.recycle();}h.click(choice);}
    void visible(String label){AccessibilityNodeInfo node=h.find(label);try{assertNotNull("기본 설정에서 보여야 합니다: "+label,node);assertTrue("접지 않고 보여야 합니다: "+label,node.isVisibleToUser());}finally{if(node!=null)node.recycle();}}
    void untouched(){assertEquals(currentBefore,Store.load(h.a).json().toString());assertEquals(libraryBefore,Store.library(h.a).get("인식 설정 검사").json().toString());assertEquals(wordSourceBefore,Ui.prefs(h.a).getString("word_source",""));}

    // 인식 방식을 접어 숨기거나 목록 저장이 다른 모드를 덮어쓰면 실패합니다.
    @Test public void listModeIsBasicAndSaveCancelPreserveOtherModes()throws Exception{
        String before=WatchStore.load(h.a).json().toString();open("watchSettings");visible("목록 인식 방식");
        visible("목록 탐지에만 적용합니다. 실행 매크로의 개별 인식 방식은 유지됩니다.");
        h.screenshot("PXE15_목록_기본설정.png");
        choose("목록 인식 방식","화면 정보");h.click("취소");assertEquals(before,WatchStore.load(h.a).json().toString());untouched();
        open("watchSettings");choose("목록 인식 방식","화면 정보");h.click("저장");
        WatchConfig saved=WatchStore.load(h.a);assertEquals("ACCESS",saved.rule.source);
        saved.rule.source="AUTO";assertEquals(before,saved.json().toString());untouched();h.screenshot("PXE15_목록_인식설정_저장.png");
    }

    // 개별 방식과 유한 대기를 숨기거나 저장 시 보정/반복/재시도 값을 초기화하면 실패합니다.
    @Test public void stepModeAndFiniteWaitAreBasicAndAdvancedValuesSurvive()throws Exception{
        String before=Store.load(h.a).steps.get(0).json().toString();openStep();visible("검색 방식");
        visible("이 동작에만 적용합니다. 목록 탐지와 다른 동작의 인식 방식은 유지됩니다.");
        h.screenshot("PXE15_글자동작_기본설정.png");
        AccessibilityNodeInfo limit=h.find("최대 기다릴 시간 · 초, 0은 한 번 확인");assertNotNull("유한 대기를 기본에서 편집해야 합니다",limit);limit.recycle();
        choose("검색 방식","화면 정보");h.setAfter("최대 기다릴 시간 · 초, 0은 한 번 확인","5.5");
        h.click("세부 설정 보기");h.setAfter("터치 X 보정 % · -50~50","3.5");h.click("적용");
        Step saved=Store.load(h.a).steps.get(0);assertEquals("ACCESS",saved.source);assertEquals(5500,saved.timeout);assertEquals(3.5,saved.offsetX,0);
        saved.source="OCR";saved.timeout=4250;saved.offsetX=2.5;assertEquals(before,saved.json().toString());
        assertEquals(libraryBefore,Store.library(h.a).get("인식 설정 검사").json().toString());assertEquals("AUTO",WatchStore.load(h.a).rule.source);assertEquals(wordSourceBefore,Ui.prefs(h.a).getString("word_source",""));
        h.screenshot("PXE15_개별_인식설정_저장.png");
    }

    // 안내 첫 단계의 선택이 저장되지 않거나 준비 요약이 다른 방식을 표시하면 실패합니다.
    @Test public void guideSavesListModeAndShowsItInReadySummary()throws Exception{
        h.main(()->{SetupFlow.begin(h.a);h.a.refreshSetup();});h.in.waitForIdleSync();visible("목록 인식 방식");
        choose("목록 인식 방식","화면 정보");h.click("다음 · 탐지 영역 정하기");assertEquals(1,SetupFlow.stage(h.a));assertEquals("ACCESS",WatchStore.load(h.a).rule.source);
        h.main(()->{SetupFlow.stage(h.a,4);h.a.refreshSetup();});h.in.waitForIdleSync();
        AccessibilityNodeInfo summary=h.find("목록 인식 · 화면 정보");assertNotNull("준비 요약에 저장한 목록 인식 방식이 있어야 합니다",summary);summary.recycle();
        untouched();assertEquals(1250,WatchStore.load(h.a).entryMs);assertEquals(600000,WatchStore.load(h.a).cooldownMs);h.screenshot("PXE15_안내_인식요약.png");
    }

    // 즉시 진입의 기본값을 바꾸거나 취소·저장 범위·재열기 상태가 잘못되면 실패합니다.
    @Test public void listImmediateEntryIsOptionalAndSurvivesSaveOnly()throws Exception{
        String before=WatchStore.load(h.a).json().toString();assertFalse(WatchStore.load(h.a).rule.immediate);
        open("watchSettings");h.click("세부 설정 보기");
        AccessibilityNodeInfo initial=h.find("인식 즉시 들어가기");assertNotNull("목록의 즉시 진입을 세부 설정에서 선택할 수 있어야 합니다",initial);
        try{assertTrue(initial.isCheckable());assertFalse(initial.isChecked());}finally{initial.recycle();}
        AccessibilityNodeInfo help=h.find("켜면 터치 전 재확인을 생략합니다. 화면이 빠르게 바뀌면 끄세요.");assertNotNull("즉시 진입의 정확도 차이를 안내해야 합니다",help);help.recycle();
        h.click("인식 즉시 들어가기");h.click("취소");assertEquals(before,WatchStore.load(h.a).json().toString());untouched();
        open("watchSettings");h.click("세부 설정 보기");h.click("인식 즉시 들어가기");h.click("저장");
        WatchConfig saved=WatchStore.load(h.a);assertTrue(saved.rule.immediate);saved.rule.immediate=false;assertEquals(before,saved.json().toString());untouched();
        open("watchSettings");h.click("세부 설정 보기");AccessibilityNodeInfo reopened=h.find("인식 즉시 들어가기");assertNotNull(reopened);
        try{assertTrue("저장한 즉시 진입이 재열기 후 유지되어야 합니다",reopened.isChecked());}finally{reopened.recycle();}
        h.click("취소");assertTrue(WatchStore.load(h.a).rule.immediate);untouched();
    }
}
