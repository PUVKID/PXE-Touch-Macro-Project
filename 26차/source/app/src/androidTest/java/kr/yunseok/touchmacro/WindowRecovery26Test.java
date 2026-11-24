package kr.yunseok.touchmacro;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;import org.junit.runner.RunWith;
import java.lang.reflect.*;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** 지연된 창 응답의 대기·취소·실제 오류 구분을 확인합니다. */
@RunWith(AndroidJUnit4.class) public class WindowRecovery26Test {
 final GuidedFlowTest h=new GuidedFlowTest();
 Field source;Object original;
 @Before public void boot()throws Exception{h.boot();source=MacroService.class.getDeclaredField("windowSource");source.setAccessible(true);original=source.get(h.svc);}
 @After public void end()throws Exception{source.set(h.svc,original);h.end();}
 long token()throws Exception{Field f=MacroService.class.getDeclaredField("generation");f.setAccessible(true);return ((AtomicLong)f.get(h.svc)).get();}
 String read(long token)throws Exception{Method m=MacroService.class.getDeclaredMethod("foregroundPackage",long.class);m.setAccessible(true);try{return (String)m.invoke(h.svc,token);}catch(InvocationTargetException e){throw (Exception)e.getCause();}}
 void source(MacroService.WindowSource value)throws Exception{source.set(h.svc,value);}
 @Test public void delayedWindowRecoversWithoutTimeout()throws Exception{
  AtomicInteger calls=new AtomicInteger();source(()->{calls.incrementAndGet();try{Thread.sleep(1900);}catch(InterruptedException e){Thread.currentThread().interrupt();}return Collections.emptyList();});
  assertEquals("",read(token()));assertEquals(1,calls.get());
 }
 @Test public void stopCancelsPendingWaitAndNoExtraQueries()throws Exception{
  CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger calls=new AtomicInteger();AtomicReference<Throwable> failure=new AtomicReference<>();
  source(()->{calls.incrementAndGet();entered.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}return Collections.emptyList();});
  long current=token();Thread task=new Thread(()->{try{read(current);failure.set(new AssertionError("정지 후 응답을 사용했습니다"));}catch(Throwable e){failure.set(e);}});
  try{task.start();assertTrue(entered.await(2,TimeUnit.SECONDS));h.main(()->h.svc.stop());task.join(1200);assertFalse(task.isAlive());assertTrue(String.valueOf(failure.get()),failure.get() instanceof CancellationException);assertEquals(1,calls.get());}finally{release.countDown();task.join(2000);}
 }
 @Test public void actualQueryFailureRemainsVisible()throws Exception{
  source(()->{throw new IllegalStateException("검사용 실제 조회 실패");});
  try{read(token());fail("조회 오류를 숨겼습니다");}catch(IllegalStateException e){assertEquals("검사용 실제 조회 실패",e.getMessage());}
 }
 @Test public void finiteWindowWaitKeepsConfiguredLimit()throws Exception{
  CountDownLatch release=new CountDownLatch(1);source(()->{try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}return Collections.emptyList();});
  Field f=MacroService.class.getDeclaredField("windowWaitLimit");f.setAccessible(true);@SuppressWarnings("unchecked") ThreadLocal<Long> limit=(ThreadLocal<Long>)f.get(h.svc);
  Method clock=MacroService.class.getDeclaredMethod("activeTime");clock.setAccessible(true);limit.set(200L);
  try{read(token());fail("유한 제한을 넘겨 기다렸습니다");}catch(IllegalStateException e){assertTrue(e.getMessage().contains("설정한 글자 대기 시간"));}finally{limit.remove();release.countDown();}
 }
 @Test public void pauseDiscardsOldResponseAndResumeReadsAgain()throws Exception{
  CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger calls=new AtomicInteger();AtomicReference<Throwable> failure=new AtomicReference<>();
  source(()->{if(calls.incrementAndGet()==1){entered.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}}return Collections.emptyList();});
  long current=token();Thread task=new Thread(()->{try{read(current);}catch(Throwable e){failure.set(e);}});
  try{h.main(()->h.svc.running=true);task.start();assertTrue(entered.await(2,TimeUnit.SECONDS));h.main(()->h.svc.togglePause());release.countDown();Thread.sleep(200);assertTrue(task.isAlive());h.main(()->h.svc.togglePause());task.join(2500);assertFalse(task.isAlive());assertNull(failure.get());assertEquals(2,calls.get());}finally{release.countDown();h.main(()->h.svc.stop());task.join(2000);}
 }
}
