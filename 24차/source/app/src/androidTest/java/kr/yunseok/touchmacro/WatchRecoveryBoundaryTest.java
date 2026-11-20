package kr.yunseok.touchmacro;

import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

/** 목록 진입 직전의 창 조회가 복구된 경우에도 옛 글자 좌표를 누르지 않습니다. */
@RunWith(AndroidJUnit4.class) public class WatchRecoveryBoundaryTest {
    final GuidedFlowTest h=new GuidedFlowTest();final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
    final AtomicInteger windows=new AtomicInteger(),taps=new AtomicInteger();Field source;Object original;
    @Before public void boot()throws Exception{h.boot();h.fixture();source=MacroService.class.getDeclaredField("windowSource");source.setAccessible(true);original=source.get(h.svc);h.main(()->h.entry.setOnClickListener(v->taps.incrementAndGet()));}
    @After public void end()throws Exception{h.end();release.countDown();SystemClock.sleep(150);source.set(h.svc,original);}
    @Test public void recoveredLastTargetCheckDiscardsPreviouslyFoundEntry()throws Exception{
        source.set(h.svc,(MacroService.WindowSource)()->{if(windows.incrementAndGet()==3){entered.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}return Collections.emptyList();}return ((MacroService.WindowSource)original).read();});
        Macro m=new Macro();m.name="마지막 창 조회 검사";m.maxMinutes=0;Step wait=new Step("WAIT");wait.duration=100;m.steps.add(wait);
        WatchConfig p=new WatchConfig();p.rule.text="안내 대상";p.rule.match="EXACT";p.rule.source="ACCESS";p.rule.immediate=true;p.targetPackage=h.a.getPackageName();p.startMs=0;p.intervalSeconds=60;p.macroName=m.name;
        h.main(()->{Store.put(h.a,m);h.svc.startWatch(p);});assertTrue(entered.await(5,TimeUnit.SECONDS));SystemClock.sleep(1900);assertTrue(h.svc.running);
        h.main(()->h.entry.setText("이미 다른 항목"));release.countDown();SystemClock.sleep(1300);
        assertEquals("복구 전 글자 좌표로 목록에 진입했습니다",0,taps.get());assertEquals(0,h.svc.watchCompleted);assertTrue(h.svc.running);
        long before=SystemClock.elapsedRealtime();h.main(()->h.svc.stop());assertTrue(SystemClock.elapsedRealtime()-before<1000);
    }
}
