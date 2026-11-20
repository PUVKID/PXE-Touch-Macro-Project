package kr.yunseok.touchmacro;

import android.graphics.Bitmap;
import android.os.SystemClock;
import com.google.android.gms.tasks.*;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.vision.text.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** 소유 화면에서 일시 지연·실패와 실제 권한 실패를 분리해 검사합니다. */
@RunWith(AndroidJUnit4.class) public class RecognitionRecoveryTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    final AtomicInteger reads=new AtomicInteger(),captures=new AtomicInteger(),taps=new AtomicInteger(),recognitions=new AtomicInteger();
    final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
    Field windowField,nodeField,captureField,recognizerField;
    Object originalWindow,originalNodes,originalCapture,originalRecognizer;
    TaskCompletionSource<Text> held;
    CancellationTokenSource cancellation;
    @Before public void boot()throws Exception{
        h.boot();h.fixture();
        windowField=field("windowSource");originalWindow=windowField.get(h.svc);
        nodeField=field("accessibilitySource");originalNodes=nodeField.get(h.svc);
        captureField=field("screenshotSource");originalCapture=captureField.get(h.svc);
        recognizerField=field("recognizer");originalRecognizer=recognizerField.get(h.svc);
        h.main(()->h.entry.setOnClickListener(v->taps.incrementAndGet()));
        h.waitFor(()->h.a.getPackageName().equals(h.svc.activePackage()),4000,"검사 앱 확인 실패");
        captureField.set(h.svc,(MacroService.ScreenshotSource)(executor,callback)->{captures.incrementAndGet();((MacroService.ScreenshotSource)originalCapture).take(executor,callback);});
    }
    @After public void end()throws Exception{
        h.end();release.countDown();if(held!=null)held.trySetException(new MlKitException("검사 종료",MlKitException.UNAVAILABLE));
        if(cancellation!=null)cancellation.cancel();SystemClock.sleep(150);
        windowField.set(h.svc,originalWindow);nodeField.set(h.svc,originalNodes);captureField.set(h.svc,originalCapture);recognizerField.set(h.svc,originalRecognizer);
    }
    Field field(String name)throws Exception{Field f=MacroService.class.getDeclaredField(name);f.setAccessible(true);return f;}
    void start(String source){Step s=new Step("TEXT_TAP");s.text="안내 대상";s.match="EXACT";s.source=source;s.waitForever=true;s.immediate=true;s.after=0;Macro m=new Macro();m.startDelay=0;m.maxMinutes=0;m.steps.add(s);h.main(()->h.svc.start(m));}
    void blockFirstWindow()throws Exception{
        windowField.set(h.svc,(MacroService.WindowSource)()->{
            if(reads.incrementAndGet()==1){entered.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}return Collections.emptyList();}
            return ((MacroService.WindowSource)originalWindow).read();
        });
    }
    @Test public void delayedWindowMetadataWaitsThenUsesFreshQuery()throws Exception{
        blockFirstWindow();start("ACCESS");assertTrue(entered.await(3,TimeUnit.SECONDS));SystemClock.sleep(2200);
        assertTrue("1.5초 창 조회 지연이 매크로를 종료했습니다: "+h.svc.status,h.svc.running);
        assertEquals(1,reads.get());assertEquals(0,taps.get());assertEquals(0,captures.get());
        release.countDown();h.waitFor(()->!h.svc.running,6000,"최신 창 조회 후 복구 실패");
        assertTrue("시간초과 이전 창 결과를 재사용했습니다",reads.get()>=2);assertEquals(1,taps.get());assertEquals(0,captures.get());
    }
    @Test public void stopDuringWindowRecoveryNeverQueuesOrTouches()throws Exception{
        blockFirstWindow();start("OCR");assertTrue(entered.await(3,TimeUnit.SECONDS));SystemClock.sleep(2200);
        assertTrue(h.svc.running);long before=SystemClock.elapsedRealtime();h.main(()->h.svc.stop());assertTrue(SystemClock.elapsedRealtime()-before<1000);
        release.countDown();SystemClock.sleep(700);assertEquals(1,reads.get());assertEquals(0,taps.get());assertEquals(0,captures.get());
    }
    @Test public void explicitScreenInformationWaitsWithoutOcrThenReadsFreshNodes()throws Exception{
        nodeField.set(h.svc,(MacroService.ScreenSource)token->{if(reads.incrementAndGet()==1){entered.countDown();release.await();return new ArrayList<>();}return ((MacroService.ScreenSource)originalNodes).read(token);});
        start("ACCESS");assertTrue(entered.await(3,TimeUnit.SECONDS));SystemClock.sleep(4100);
        assertTrue("화면 정보의 일시 지연이 실행을 끝냈습니다: "+h.svc.status,h.svc.running);assertEquals(0,captures.get());assertEquals(0,taps.get());assertEquals(1,reads.get());
        release.countDown();h.waitFor(()->!h.svc.running,6000,"새 화면 정보 조회 복구 실패");assertEquals(1,taps.get());assertEquals(0,captures.get());assertTrue(reads.get()>=2);
    }
    @Test public void feedbackInterruptKeepsConnectedMacroAndStopResponsive()throws Exception{
        Macro m=new Macro();m.startDelay=0;m.maxMinutes=0;Step wait=new Step("WAIT");wait.duration=4000;m.steps.add(wait);h.main(()->h.svc.start(m));
        h.waitFor(()->h.svc.running,2000,"실행 시작 실패");h.main(()->h.svc.onInterrupt());SystemClock.sleep(200);
        assertTrue("접근성 알림 중단을 연결 해제로 처리했습니다",h.svc.running);assertTrue(h.svc.connectionActive());
        long before=SystemClock.elapsedRealtime();h.main(()->h.svc.stop());assertTrue(SystemClock.elapsedRealtime()-before<1000);
    }
    void holdFirstRecognition(boolean canceled)throws Exception{
        cancellation=new CancellationTokenSource();held=canceled?new TaskCompletionSource<>(cancellation.getToken()):new TaskCompletionSource<>();
        recognizerField.set(h.svc,Proxy.newProxyInstance(TextRecognizer.class.getClassLoader(),new Class<?>[]{TextRecognizer.class},(proxy,method,args)->{
            if(!method.getName().equals("process"))return method.invoke(originalRecognizer,args);
            if(recognitions.incrementAndGet()==1){entered.countDown();return held.getTask();}
            return method.invoke(originalRecognizer,args);
        }));
    }
    Bitmap pendingBitmap()throws Exception{Object pending=((AtomicReference<?>)field("pendingRecognition").get(h.svc)).get();assertNotNull(pending);Field bitmap=pending.getClass().getDeclaredField("bitmap");bitmap.setAccessible(true);return (Bitmap)bitmap.get(pending);}
    void transientFailure(int code)throws Exception{
        holdFirstRecognition(false);start("OCR");assertTrue(entered.await(8,TimeUnit.SECONDS));Bitmap bitmap=pendingBitmap();assertFalse(bitmap.isRecycled());
        h.main(()->held.setException(new MlKitException("제어된 일시 오류",code)));
        h.waitFor(()->recognitions.get()>=2||!h.svc.running,8000,"일시 오류 후 새 OCR 없음");
        h.waitFor(()->!h.svc.running,16000,"일시 오류 후 실제 OCR 복구 실패");
        assertEquals("일시 인식 오류로 중단되었습니다: "+h.svc.status,1,taps.get());assertEquals(2,recognitions.get());assertEquals(2,captures.get());assertTrue(bitmap.isRecycled());
    }
    @Test public void unavailableRecognitionRecoversWithFreshCapture()throws Exception{transientFailure(MlKitException.UNAVAILABLE);}
    @Test public void deadlineRecognitionRecoversWithFreshCapture()throws Exception{transientFailure(MlKitException.DEADLINE_EXCEEDED);}
    @Test public void canceledTaskRecoversAndReleasesOnlyCompletedImage()throws Exception{
        holdFirstRecognition(true);start("OCR");assertTrue(entered.await(8,TimeUnit.SECONDS));Bitmap bitmap=pendingBitmap();assertFalse(bitmap.isRecycled());
        h.main(()->cancellation.cancel());h.waitFor(()->!h.svc.running,20000,"취소된 OCR 후 복구 실패");
        assertEquals(1,taps.get());assertEquals(2,recognitions.get());assertEquals(2,captures.get());assertTrue(bitmap.isRecycled());
    }
    @Test public void permissionFailureRemainsTerminalAndNeverTouches()throws Exception{
        holdFirstRecognition(false);start("OCR");assertTrue(entered.await(8,TimeUnit.SECONDS));h.main(()->held.setException(new MlKitException("제어된 권한 거절",MlKitException.PERMISSION_DENIED)));
        h.waitFor(()->!h.svc.running,3000,"권한 실패가 잘못 재시도되었습니다");assertEquals(0,taps.get());assertEquals(1,recognitions.get());assertEquals(1,captures.get());
    }
    @Test public void explicitOverallDeadlineStillStopsWindowRecovery()throws Exception{
        blockFirstWindow();start("ACCESS");assertTrue(entered.await(3,TimeUnit.SECONDS));field("deadline").setLong(h.svc,SystemClock.elapsedRealtime()+400);
        h.waitFor(()->!h.svc.running,2500,"의도된 전체 제한 시간이 무시되었습니다");assertTrue(h.svc.status,h.svc.status.contains("최대 실행 시간"));assertEquals(0,taps.get());assertEquals(0,captures.get());
    }
    @Test public void infrastructureGapCannotRearmDuplicateText()throws Exception{
        nodeField.set(h.svc,(MacroService.ScreenSource)token->{int n=reads.incrementAndGet();if(n==2){entered.countDown();release.await();return new ArrayList<>();}return n<=3?new ArrayList<>():((MacroService.ScreenSource)originalNodes).read(token);});
        Macro m=new Macro();m.name="복구 중 중복 차단 검사";Step wait=new Step("WAIT");wait.duration=100;m.steps.add(wait);
        WatchConfig p=new WatchConfig();p.rule.text="안내 대상";p.rule.source="ACCESS";p.targetPackage=h.a.getPackageName();p.preventDuplicate=true;p.rearmMs=1000;p.startMs=0;p.intervalSeconds=60;p.macroName=m.name;
        h.main(()->{Store.put(h.a,m);WatchStore.blocked(h.a,p,true);h.svc.startWatch(p);});assertTrue(entered.await(4,TimeUnit.SECONDS));SystemClock.sleep(4100);
        assertTrue(h.svc.running);release.countDown();h.waitFor(()->reads.get()>=4,5000,"복구 후 새 관측 없음");SystemClock.sleep(1000);
        assertTrue(h.svc.running);assertEquals("인식 실패 구간을 글자 사라짐으로 간주했습니다",0,taps.get());assertEquals(0,h.svc.watchCompleted);assertEquals(0,captures.get());assertTrue(WatchStore.blocked(h.a,p));
    }
    @Test public void normalFiniteMissingTextStillUsesStopPolicy()throws Exception{
        nodeField.set(h.svc,(MacroService.ScreenSource)token->new ArrayList<>());
        Step step=new Step("TEXT_TAP");step.text="없는 글자";step.source="ACCESS";step.waitForever=false;step.timeout=300;step.failure="STOP";
        Macro m=new Macro();m.startDelay=0;m.maxMinutes=0;m.steps.add(step);h.main(()->h.svc.start(m));
        h.waitFor(()->!h.svc.running,2500,"정상 유한 미발견 정책이 무시되었습니다");assertTrue(h.svc.status,h.svc.status.contains("글자를 찾지 못했습니다"));assertEquals(0,taps.get());assertEquals(0,captures.get());
    }
}
