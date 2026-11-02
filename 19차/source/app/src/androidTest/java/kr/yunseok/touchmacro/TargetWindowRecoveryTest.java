package kr.yunseok.touchmacro;

import android.content.Intent;
import android.os.SystemClock;
import android.provider.Settings;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 실제 실행 취소와 개별 창 조회 취소를 구분하며 기존 회차를 보존합니다. */
@RunWith(AndroidJUnit4.class) public class TargetWindowRecoveryTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    final AtomicInteger reads=new AtomicInteger(),taps=new AtomicInteger(),captures=new AtomicInteger();
    final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
    Field sourceField,captureField;Object originalSource,originalCapture;
    @Before public void boot()throws Exception{
        h.boot();h.main(()->Ui.prefs(h.a).edit().putBoolean("hud_collapsed",false).commit());h.fixture();
        sourceField=field("windowSource");originalSource=sourceField.get(h.svc);
        captureField=field("screenshotSource");originalCapture=captureField.get(h.svc);
        h.main(()->h.entry.setOnClickListener(v->taps.incrementAndGet()));
        h.waitFor(()->h.a.getPackageName().equals(h.svc.activePackage()),4000,"검사 대상 확인 실패");
        captureField.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{
            captures.incrementAndGet();((MacroService.ScreenshotSource)originalCapture).take(executor,callback);
        });
    }
    @After public void end()throws Exception{
        h.end();release.countDown();SystemClock.sleep(150);
        sourceField.set(h.svc,originalSource);captureField.set(h.svc,originalCapture);
    }
    Field field(String name)throws Exception{Field value=MacroService.class.getDeclaredField(name);value.setAccessible(true);return value;}
    Macro macro(int rounds){Macro m=new Macro();m.rounds=rounds;m.startDelay=0;m.gap=0;m.maxMinutes=0;Step text=new Step("TEXT_TAP");text.text="안내 대상";text.match="EXACT";text.source="ACCESS";text.waitForever=true;text.immediate=true;text.after=0;m.steps.add(text);return m;}
    void start(Macro value){h.main(()->h.svc.start(value));}
    void waitHeld()throws Exception{assertTrue("제어된 창 조회가 시작되지 않았습니다",entered.await(5,TimeUnit.SECONDS));}
    RunHudTest hud(){RunHudTest value=new RunHudTest();value.h.in=h.in;value.h.auto=h.auto;value.h.a=h.a;value.h.svc=h.svc;return value;}
    void recoveryShown()throws Exception{
        h.waitFor(()->hud().hudText().contains("복구 대기")&&hud().hudText().contains("정지 가능"),2000,"실제 진행창에 복구 대기/정지 가능 표시가 없습니다");
        h.main(()->{try{assertTrue(hud().hud().isShown());}catch(Exception error){throw new RuntimeException(error);}});
        h.screenshot("PXE19_대상창_복구대기.png");
    }
    String journal()throws Exception{return ((ErrorJournal)field("errorJournal").get(h.svc)).read();}
    @Test public void canceledWindowQueryRecoversWithNewQueryAndExactlyOneTap()throws Exception{
        sourceField.set(h.svc,(MacroService.WindowSource)()->{
            if(reads.incrementAndGet()==1)throw new CancellationException("제어된 개별 창 조회 취소");
            return ((MacroService.WindowSource)originalSource).read();
        });
        start(macro(1));h.waitFor(()->!h.svc.running,7000,"취소된 창 조회 뒤 완료되지 않았습니다");
        assertEquals("유효한 실행의 조회 취소가 실행 취소로 전파됐습니다: "+h.svc.status,1,taps.get());
        assertTrue(reads.get()>=2);assertEquals(0,captures.get());assertEquals(1,h.svc.hudProgress().completed);
    }
    @Test public void slowQueryShowsRealRecoveryHudAndPhysicalStopRemainsImmediate()throws Exception{
        sourceField.set(h.svc,(MacroService.WindowSource)()->{
            reads.incrementAndGet();entered.countDown();try{release.await();}catch(InterruptedException error){Thread.currentThread().interrupt();}
            return Collections.emptyList();
        });
        start(macro(1));waitHeld();SystemClock.sleep(2300);assertTrue(h.svc.running);
        assertEquals(1,reads.get());assertEquals(0,taps.get());assertEquals(0,captures.get());recoveryShown();
        long before=SystemClock.elapsedRealtime();h.click("■");long elapsed=SystemClock.elapsedRealtime()-before;
        assertTrue("실제 정지 버튼이 1초를 넘겼습니다: "+elapsed,elapsed<1000);assertFalse(h.svc.running);
        release.countDown();SystemClock.sleep(600);assertEquals(1,reads.get());assertEquals(0,taps.get());
        android.util.Log.i("PXE19_RECOVERY_QA","physical_stop_ms="+elapsed+", reads="+reads.get()+", taps="+taps.get());
    }
    @Test public void recoveryAtEighthRoundKeepsSevenCompletedAndFinishesExactlyFifteen()throws Exception{
        sourceField.set(h.svc,(MacroService.WindowSource)()->{
            reads.incrementAndGet();if(h.svc.hudProgress().completed==7&&h.svc.hudProgress().current==8&&entered.getCount()>0){
                entered.countDown();try{release.await();}catch(InterruptedException error){Thread.currentThread().interrupt();}return Collections.emptyList();
            }
            return ((MacroService.WindowSource)originalSource).read();
        });
        start(macro(15));waitHeld();SystemClock.sleep(2200);assertTrue(h.svc.running);assertEquals(7,taps.get());
        assertEquals(7,h.svc.hudProgress().completed);assertEquals(8,h.svc.hudProgress().current);recoveryShown();
        assertTrue(hud().hudText(),hud().hudText().contains("7/15"));assertTrue(hud().hudText(),hud().hudText().contains("8회째"));
        h.main(()->h.svc.togglePause());release.countDown();SystemClock.sleep(600);assertEquals(7,taps.get());assertEquals(7,h.svc.hudProgress().completed);
        h.main(()->h.svc.togglePause());h.waitFor(()->!h.svc.running,12000,"복구 후 15회가 끝나지 않았습니다");
        assertEquals("앞선 회차가 재실행되거나 누락됐습니다",15,taps.get());assertEquals(15,h.svc.hudProgress().completed);assertEquals(0,captures.get());
    }
    @Test public void canceledQueryAfterPinnedTargetNeverTouchesOtherApplication()throws Exception{
        sourceField.set(h.svc,(MacroService.WindowSource)()->{
            if(reads.incrementAndGet()==2){entered.countDown();try{release.await();}catch(InterruptedException error){Thread.currentThread().interrupt();}throw new CancellationException("제어된 개별 창 조회 취소");}
            return ((MacroService.WindowSource)originalSource).read();
        });
        start(macro(1));waitHeld();
        h.main(()->h.a.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        // 주입한 창 소스 대신 원래 소스로 실제 활성 전환을 확정합니다.
        WindowPackages packages=(WindowPackages)field("windowPackages").get(h.svc);
        h.waitFor(()->"com.android.settings".equals(packages.resolve(((MacroService.WindowSource)originalSource).read(),h.a.getPackageName())),4000,"실제 다른 앱 활성 전환이 없습니다");
        release.countDown();SystemClock.sleep(1200);assertTrue("조회 취소로 실행이 끝났습니다",h.svc.running);
        assertEquals(0,taps.get());assertEquals(0,captures.get());assertEquals(0,h.svc.hudProgress().completed);
        h.main(()->h.svc.stop());
    }
    @Test public void permissionFailureStillStopsAndJournalIdentifiesInstalledVersion()throws Exception{
        sourceField.set(h.svc,(MacroService.WindowSource)()->{reads.incrementAndGet();throw new SecurityException("제어된 권한 거절");});
        start(macro(1));h.waitFor(()->!h.svc.running,3000,"실제 권한 거절을 무한 재시도했습니다");
        assertEquals(1,reads.get());assertEquals(0,taps.get());assertEquals(0,captures.get());
        assertTrue(h.svc.status,h.svc.status.contains("제어된 권한 거절"));
        String value=journal();assertTrue(value,value.contains("app_version=1.9.0")&&value.contains("version_code=19"));
        assertTrue(value,value.contains("SecurityException"));assertFalse("새 검색 본문이 오류 기록에 추가됐습니다",value.contains("안내 대상"));
    }
    @Test public void repeatedQueryCancellationStillHonorsExplicitOverallDeadline()throws Exception{
        sourceField.set(h.svc,(MacroService.WindowSource)()->{
            reads.incrementAndGet();entered.countDown();try{release.await();}catch(InterruptedException error){Thread.currentThread().interrupt();}
            throw new CancellationException("제어된 개별 창 조회 취소");
        });
        start(macro(1));waitHeld();field("deadline").setLong(h.svc,SystemClock.elapsedRealtime()+650);release.countDown();
        h.waitFor(()->!h.svc.running,2500,"창 조회 취소 복구가 명시 전체 기한을 무시했습니다");
        assertTrue(h.svc.status,h.svc.status.contains("최대 실행 시간"));assertEquals(0,taps.get());assertEquals(0,captures.get());
    }
}
