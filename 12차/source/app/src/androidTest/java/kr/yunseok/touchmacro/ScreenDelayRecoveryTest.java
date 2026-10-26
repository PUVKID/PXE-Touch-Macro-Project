package kr.yunseok.touchmacro;

import android.accessibilityservice.AccessibilityService;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.os.SystemClock;
import android.provider.Settings;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** 실제 OCR/터치와 제어된 지연 주입으로 복구 및 오탭 방지를 함께 검사합니다. */
@RunWith(AndroidJUnit4.class) public class ScreenDelayRecoveryTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    final CountDownLatch release=new CountDownLatch(1),entered=new CountDownLatch(1);
    final AtomicInteger reads=new AtomicInteger(),captures=new AtomicInteger();
    Field nodeField,captureField,windowField;
    Object nodeOriginal,captureOriginal,windowOriginal;
    volatile int entries,actions;
    Rect actionBounds,region;WatchConfig profile;

    @Before public void boot()throws Exception{
        h.boot();h.fixture();
        nodeField=field("accessibilitySource");nodeOriginal=nodeField.get(h.svc);
        captureField=field("screenshotSource");captureOriginal=captureField.get(h.svc);
        windowField=field("windowSource");windowOriginal=windowField.get(h.svc);
        captureField.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{captures.incrementAndGet();((MacroService.ScreenshotSource)captureOriginal).take(executor,callback);});
        h.main(()->{
            LinearLayout body=new LinearLayout(h.a);body.setPadding(50,100,50,100);
            Button action=new Button(h.a);action.setText("기록한 동작");action.setOnClickListener(v->actions++);
            body.addView(action,new LinearLayout.LayoutParams(-1,Ui.dp(h.a,80)));
            h.detail=new AlertDialog.Builder(h.a).setTitle("실행 화면").setView(body).create();h.detail.show();
            h.action=action;h.entry.setOnClickListener(v->{entries++;h.detail.show();});
        });h.in.waitForIdleSync();
        h.main(()->{actionBounds=h.bounds(h.action);h.detail.dismiss();region=h.bounds(h.entry);});
        h.waitFor(()->h.a.getPackageName().equals(h.svc.activePackage()),4000,"대상 앱 창 확인 실패");
        int[] size=h.svc.size();Macro macro=new Macro();macro.name="화면 지연 회귀";
        Step tap=new Step("TAP");tap.x=actionBounds.exactCenterX()*100.0/size[0];tap.y=actionBounds.exactCenterY()*100.0/size[1];tap.duration=50;tap.after=10;macro.steps.add(tap);
        profile=new WatchConfig();profile.rule.text="안내 대상";profile.rule.match="EXACT";profile.rule.source="AUTO";profile.targetPackage=h.a.getPackageName();
        profile.rule.left=(region.left-2)*100.0/size[0];profile.rule.top=(region.top-2)*100.0/size[1];profile.rule.right=(region.right+2)*100.0/size[0];profile.rule.bottom=(region.bottom+2)*100.0/size[1];
        profile.startMs=0;profile.entryMs=300;profile.returnMs=300;profile.repeatGapMs=0;profile.cooldownMs=600000;profile.macroName=macro.name;
        h.main(()->Store.put(h.a,macro));
    }
    Field field(String name)throws Exception{Field f=MacroService.class.getDeclaredField(name);f.setAccessible(true);return f;}
    void blockNodes()throws Exception{
        nodeField.set(h.svc,(MacroService.ScreenSource)token->{reads.incrementAndGet();entered.countDown();release.await();return new ArrayList<>();});
    }
    void start(){h.main(()->h.svc.startWatch(profile));}
    @After public void end()throws Exception{
        h.end();release.countDown();SystemClock.sleep(150);
        if(nodeField!=null)nodeField.set(h.svc,nodeOriginal);
        if(captureField!=null)captureField.set(h.svc,captureOriginal);
        if(windowField!=null)windowField.set(h.svc,windowOriginal);
    }
    @Test public void autoTimeoutFallsBackOnceAndCompletesFifteenActions()throws Exception{
        blockNodes();start();assertTrue(entered.await(3,TimeUnit.SECONDS));
        h.waitFor(()->h.svc.watchCompleted==1,30000,"AUTO 지연 후 OCR 복구 실패");
        assertEquals(1,reads.get());assertEquals(2,captures.get());assertEquals(1,entries);assertEquals(15,actions);
        assertTrue(h.svc.logText().contains("화면 OCR로 전환"));
        release.countDown();SystemClock.sleep(800);assertEquals(1,entries);assertEquals(15,actions);assertTrue(h.svc.progressText().contains("쉬는 중"));
    }
    @Test public void ocrDoesNotReadNodesEvenForTargetPackageCheck()throws Exception{
        blockNodes();profile.rule.source="OCR";start();h.waitFor(()->h.svc.watchCompleted==1,25000,"OCR 전용 실행 실패");
        assertEquals(0,reads.get());assertEquals(1,entries);assertEquals(15,actions);
    }
    @Test public void explicitAccessFailureDoesNotBecomeMissingOrTouch()throws Exception{
        blockNodes();profile.rule.source="ACCESS";start();assertTrue(entered.await(3,TimeUnit.SECONDS));
        h.waitFor(()->!h.svc.running,6000,"화면 정보 모드 지연 정지 실패");
        assertTrue(h.svc.status.contains("화면 정보 응답 지연"));assertEquals(0,captures.get());assertEquals(0,entries);assertEquals(0,h.svc.watchCompleted);
    }
    @Test public void stopDiscardsBlockedReadAndDoesNotStartOcrLater()throws Exception{
        blockNodes();start();assertTrue(entered.await(3,TimeUnit.SECONDS));long before=SystemClock.elapsedRealtime();
        h.main(()->h.svc.stop());assertTrue(SystemClock.elapsedRealtime()-before<1000);release.countDown();SystemClock.sleep(4300);
        assertEquals(0,captures.get());assertEquals(0,entries);assertFalse(h.svc.running);
    }
    @Test public void pausePreventsFallbackAndLateReadFromTouching()throws Exception{
        blockNodes();start();assertTrue(entered.await(3,TimeUnit.SECONDS));h.main(()->h.svc.togglePause());release.countDown();SystemClock.sleep(4300);
        assertEquals(0,captures.get());assertEquals(0,entries);assertTrue(h.svc.paused);h.main(()->h.svc.stop());
    }
    @Test public void delayedWindowMetadataStopsWithoutQueuingMoreQueries()throws Exception{
        AtomicInteger attempts=new AtomicInteger();windowField.set(h.svc,(MacroService.WindowSource)()->{attempts.incrementAndGet();entered.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}return Collections.emptyList();});
        start();assertTrue(entered.await(3,TimeUnit.SECONDS));h.waitFor(()->!h.svc.running,4000,"창 메타데이터 지연이 정지하지 않음");
        start();h.waitFor(()->!h.svc.running,2000,"진행 중 조회에 작업이 쌓임");assertEquals(1,attempts.get());assertEquals(0,entries);assertEquals(0,captures.get());
    }
    @Test public void oldOcrFrameCannotTouchAnotherApplication()throws Exception{
        profile.rule.source="OCR";AtomicReference<AccessibilityService.ScreenshotResult> image=new AtomicReference<>();AtomicReference<AccessibilityService.TakeScreenshotCallback> receiver=new AtomicReference<>();
        captureField.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{
            captures.incrementAndGet();((MacroService.ScreenshotSource)captureOriginal).take(executor,new AccessibilityService.TakeScreenshotCallback(){
                @Override public void onSuccess(AccessibilityService.ScreenshotResult result){receiver.set(callback);image.set(result);}
                @Override public void onFailure(int error){callback.onFailure(error);}
            });
        });
        AccessibilityService.ScreenshotResult pending=null;
        try{
            start();h.waitFor(()->image.get()!=null,7000,"캡처 결과를 받지 못함");pending=image.getAndSet(null);
            h.main(()->h.a.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
            h.waitFor(()->{String pkg=h.svc.activePackage();return !pkg.isEmpty()&&!pkg.equals(h.a.getPackageName());},4000,"다른 앱 전환을 확인하지 못함");
            AccessibilityService.ScreenshotResult held=pending;h.main(()->receiver.get().onSuccess(held));pending=null;
            SystemClock.sleep(4500);assertEquals(0,entries);assertEquals(0,actions);assertEquals(0,h.svc.watchCompleted);assertTrue(h.svc.running);assertTrue(h.svc.progressText().contains("대상 앱 대기"));
        }finally{
            if(pending!=null)pending.getHardwareBuffer().close();
            h.main(()->{h.svc.stop();h.a.startActivity(new Intent(h.a,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));});
        }
    }
}
