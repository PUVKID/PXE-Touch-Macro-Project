package kr.yunseok.touchmacro;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 실제 설정·저장/취소·제어 버튼과 밝고 어두운 화면을 확인합니다. */
@RunWith(AndroidJUnit4.class) public class EntrySettingsUiTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    @Before public void boot()throws Exception{h.boot();h.main(()->{SetupFlow.finished(h.a);Ui.prefs(h.a).edit().putInt("theme",1).commit();WatchConfig p=new WatchConfig();p.rule.text="목록 글자";WatchStore.save(h.a,p);h.svc.controller().close();h.a.refreshSetup();});h.in.waitForIdleSync();}
    @After public void end()throws Exception{h.shell("input keyevent 4");h.end();}
    View text(View root,String value){if(root instanceof TextView&&value.contentEquals(((TextView)root).getText()))return root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View result=text(group.getChildAt(i),value);if(result!=null)return result;}}return null;}
    void invoke(String name){h.main(()->{try{java.lang.reflect.Method method=MainActivity.class.getDeclaredMethod(name);method.setAccessible(true);method.invoke(h.a);}catch(Exception error){throw new RuntimeException(error);}});h.in.waitForIdleSync();}
    @Test public void entrySettingsSaveCancelKeepListAndMacroValuesIndependent()throws Exception{
        assertNotNull("홈에서 진입 확인 설정으로 이동할 수 없습니다",h.find("진입 화면 확인"));String before=WatchStore.load(h.a).json().toString();
        invoke("entrySettings");h.setAfter("매크로를 실행할 글자","배");h.setAfter("뒤로갈 글자 · 선택","사과");h.click("진입 화면을 확인한 뒤 실행");h.click("취소");assertEquals(before,WatchStore.load(h.a).json().toString());
        invoke("entrySettings");h.setAfter("매크로를 실행할 글자","배");h.setAfter("뒤로갈 글자 · 선택","사과");h.setAfter("확인 시간 · 초","1.25");h.click("진입 화면을 확인한 뒤 실행");h.screenshot("PXE21_진입확인_간단설정.png");h.click("저장");
        WatchConfig saved=WatchStore.load(h.a);assertTrue(saved.entryCheck);assertEquals("배",saved.entryRule.text);assertEquals("사과",saved.rejectText);assertEquals(1250,saved.entryRule.timeout);assertEquals("EXACT",saved.entryRule.match);assertEquals("목록 글자",saved.rule.text);assertEquals(15,saved.repetitions);assertEquals(600000,saved.cooldownMs);
        invoke("entrySettings");h.click("영역·인식 세부 설정 보기");h.setAfter("확인 영역 위 %","20");h.setAfter("확인 영역 아래 %","80");h.click("저장");assertEquals(20,WatchStore.load(h.a).entryRule.top,0);assertEquals(0,WatchStore.load(h.a).rule.top,0);
    }
    @Test public void appAndCollapsedControllerExposePauseAndResumeForCurrentRun()throws Exception{
        Macro m=new Macro();m.startDelay=0;Step wait=new Step("WAIT");wait.duration=5000;wait.after=0;m.steps.add(wait);h.main(()->{h.svc.start(m);h.a.refreshSetup();});h.in.waitForIdleSync();
        View[] pause={null};h.main(()->pause[0]=text(h.a.getWindow().getDecorView(),"Ⅱ"));assertNotNull("앱에 일시정지 버튼이 없습니다",pause[0]);h.main(()->{assertTrue(pause[0].isShown());assertEquals("일시정지",pause[0].getContentDescription());assertTrue(pause[0].getHeight()>=Ui.dp(h.a,48));pause[0].performClick();});assertTrue(h.svc.paused);
        h.waitFor(()->text(h.a.getWindow().getDecorView(),"▶")!=null,2000,"앱의 계속 실행 문구가 없습니다");h.screenshot("PXE21_앱_일시정지.png");h.main(()->text(h.a.getWindow().getDecorView(),"▶").performClick());assertFalse(h.svc.paused);
        h.main(()->h.svc.controller().minimize());FloatingPlayTest control=new FloatingPlayTest();control.h=h;control.click("일시정지");assertTrue(h.svc.paused);h.screenshot("PXE21_접힌제어창_일시정지.png");control.click("계속 실행");assertFalse(h.svc.paused);h.main(()->h.svc.stop());
    }
    @Test public void lightDarkHomeAndEntryDialogUseActualMonochromeBlueTheme()throws Exception{
        for(int mode:new int[]{1,2}){
            h.main(()->Ui.prefs(h.a).edit().putInt("theme",mode).commit());h.a=(MainActivity)h.in.startActivitySync(new android.content.Intent(h.in.getTargetContext(),MainActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK|android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK));h.in.waitForIdleSync();String suffix=h.svc.size()[0]+"_"+(mode==1?"라이트":"다크");
            View content=((ViewGroup)h.a.findViewById(android.R.id.content)).getChildAt(0);assertEquals(mode==1?Color.WHITE:Color.parseColor("#101114"),((ColorDrawable)content.getBackground()).getColor());h.screenshot("PXE21_홈_"+suffix+".png");
            invoke("entrySettings");h.screenshot("PXE21_진입설정_"+suffix+".png");h.click("취소");
        }
    }
}
