package kr.yunseok.touchmacro;

import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 정지 선행 없이 새 설정으로 시작하고 이전 비동기 결과를 폐기합니다. */
@RunWith(AndroidJUnit4.class) public class RestartRunTest {
    final GuidedFlowTest h=new GuidedFlowTest();final AtomicInteger taps=new AtomicInteger();final CountDownLatch held=new CountDownLatch(1),release=new CountDownLatch(1);Field screenField;Object originalScreen;
    @Before public void boot()throws Exception{h.boot();h.fixture();h.main(()->h.entry.setOnClickListener(v->taps.incrementAndGet()));screenField=MacroService.class.getDeclaredField("accessibilitySource");screenField.setAccessible(true);originalScreen=screenField.get(h.svc);}
    @After public void end()throws Exception{release.countDown();h.end();screenField.set(h.svc,originalScreen);}
    Macro word(String value){Macro m=new Macro();m.name="같은 이름";m.startDelay=0;m.rounds=1;Step s=new Step("TEXT_TAP");s.text=value;s.source="ACCESS";s.match="EXACT";s.waitForever=true;s.immediate=true;s.after=0;m.steps.add(s);return m;}
    void start(Macro m){h.main(()->h.svc.start(m));}
    @Test public void startDuringOldWordWaitRunsNewWordFromBeginning()throws Exception{
        start(word("이전 글자"));h.waitFor(()->h.svc.running,1000,"이전 실행이 없습니다");SystemClock.sleep(150);
        start(word("안내 대상"));h.waitFor(()->!h.svc.running,4000,"새 글자로 처음부터 시작하지 않았습니다");assertEquals(1,taps.get());assertEquals(1,h.svc.hudProgress().completed);
    }
    @Test public void invalidNewMacroKeepsOldRunAliveAndItsWord()throws Exception{
        start(word("기존 글자"));SystemClock.sleep(150);Macro invalid=word(" ");boolean[] rejected={false};h.main(()->{try{h.svc.start(invalid);}catch(IllegalArgumentException expected){rejected[0]=true;}});
        assertTrue("실행 중 시작도 새 입력을 먼저 검증해야 합니다",rejected[0]);assertTrue(h.svc.running);h.main(()->h.entry.setText("기존 글자"));h.waitFor(()->!h.svc.running,4000,"기존 실행을 취소했습니다");assertEquals(1,taps.get());
    }
    @Test public void twoQueuedStartsOnlyNewestCanInitializeOrTap()throws Exception{
        Field f=MacroService.class.getDeclaredField("worker");f.setAccessible(true);ExecutorService worker=(ExecutorService)f.get(h.svc);
        worker.execute(()->{held.countDown();try{release.await();}catch(InterruptedException error){Thread.currentThread().interrupt();}});assertTrue(held.await(2,TimeUnit.SECONDS));
        h.main(()->{h.svc.start(word("이전 글자"));h.svc.start(word("안내 대상"));});release.countDown();h.waitFor(()->!h.svc.running,4000,"이전 대기열 실행이 최신 시작을 덮었습니다");assertEquals(1,taps.get());assertEquals(1,h.svc.hudProgress().completed);
    }
    @Test public void delayedOldScreenAndFinallyCannotStopNewRunOrTapOldWord()throws Exception{
        AtomicInteger reads=new AtomicInteger();screenField.set(h.svc,(MacroService.ScreenSource)token->{if(reads.incrementAndGet()==1){held.countDown();try{release.await();}catch(InterruptedException error){Thread.currentThread().interrupt();}}return ((MacroService.ScreenSource)originalScreen).read(token);});
        start(word("안내 대상"));assertTrue(held.await(3,TimeUnit.SECONDS));start(word("새 글자"));release.countDown();SystemClock.sleep(300);assertTrue("이전 finally가 새 실행을 끝냈습니다",h.svc.running);assertEquals(0,taps.get());
        h.main(()->h.entry.setText("새 글자"));h.waitFor(()->!h.svc.running,4000,"새 실행이 유지되지 않았습니다");assertEquals(1,taps.get());assertEquals(1,h.svc.hudProgress().completed);
    }
    @Test public void watchStartWhileWatchingRestartsWithNewWordAndInvalidWatchKeepsOld()throws Exception{
        Macro m=new Macro();m.name="새 탐지 매크로";m.startDelay=0;Step wait=new Step("WAIT");wait.duration=1;wait.after=0;m.steps.add(wait);h.main(()->Store.put(h.a,m));
        WatchConfig first=new WatchConfig();first.rule.text="이전 목록";first.rule.source="ACCESS";first.rule.match="EXACT";first.macroName=m.name;first.startMs=0;first.tapFound=false;first.repetitions=1;first.beforeBackMs=3000;first.targetPackage=h.a.getPackageName();
        h.main(()->h.svc.startWatch(first));SystemClock.sleep(150);WatchConfig invalid=new WatchConfig(first.json());invalid.rule.text=" ";boolean[] rejected={false};h.main(()->{try{h.svc.startWatch(invalid);}catch(IllegalArgumentException expected){rejected[0]=true;}});assertTrue(rejected[0]);assertTrue(h.svc.running);
        WatchConfig next=new WatchConfig(first.json());next.rule.text="안내 대상";h.main(()->h.svc.startWatch(next));h.waitFor(()->h.svc.hudProgress().completed==1,4000,"탐지 시작이 새 목록 글자를 적용하지 않았습니다");assertTrue(h.svc.watching);assertEquals(0,h.svc.watchCompleted);
    }
}
