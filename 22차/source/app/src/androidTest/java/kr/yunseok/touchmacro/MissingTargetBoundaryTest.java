package kr.yunseok.touchmacro;
import android.content.Intent;
import android.provider.Settings;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.ArrayList;
import java.util.concurrent.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 글자 대기의 원래 부재 결과가 다른 UID의 뒤로가기로 이어지지 않아야 합니다. */
@RunWith(AndroidJUnit4.class) public class MissingTargetBoundaryTest {
 final MissingRetryFlowTest base=new MissingRetryFlowTest();final GuidedFlowTest h=base.h;final CountDownLatch held=new CountDownLatch(1),release=new CountDownLatch(1);
 @Before public void boot()throws Exception{base.boot();h.waitFor(()->h.a.getPackageName().equals(h.svc.activePackage()),3000,"원래 대상 앱 관측 없음");}
 @After public void end()throws Exception{release.countDown();h.shell("input keyevent 4");base.end();}
 @Test public void waitingStepNeverBindsAnOldAbsenceToTheNewForegroundUid()throws Exception{
  base.source.set(h.svc,(MacroService.ScreenSource)token->{held.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}return new ArrayList<>();});Step s=new Step("TEXT_WAIT");s.text="없는 글자";s.source="ACCESS";s.timeout=0;s.failure="BACK_RETRY";s.retries=1;s.retryDelay=300;s.after=0;Macro m=base.macro(s);base.start(m);assertTrue(held.await(3,TimeUnit.SECONDS));h.main(()->h.a.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));h.waitFor(()->"com.android.settings".equals(h.svc.activePackage()),3000,"다른 UID의 실제 활성 전환 없음");release.countDown();h.waitFor(()->!h.svc.running||!"com.android.settings".equals(h.svc.activePackage()),3000,"대상 확인 거절 또는 외부 복구 관측 없음");assertEquals("원래 글자 부재를 다른 UID의 뒤로가기로 실행했습니다","com.android.settings",h.svc.activePackage());assertFalse("다른 앱에서 복구 로그가 발생했습니다",h.svc.logText().contains("글자 없음 · 뒤로가기"));assertFalse(h.svc.running);assertEquals(0,h.svc.hudProgress().completed);assertEquals(0,base.taps.get());
 }
}
