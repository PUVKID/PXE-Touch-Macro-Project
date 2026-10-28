package kr.yunseok.touchmacro;

import android.content.Intent;
import android.accessibilityservice.GestureDescription;
import android.os.*;
import android.provider.Settings;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;

/** 실제 앱 전환 전·후와 제스처 전송 직전의 경계를 각각 제어합니다. */
@RunWith(AndroidJUnit4.class) public class TargetDispatchBoundaryTest {
    final AdaptiveWaitFlowTest f=new AdaptiveWaitFlowTest();
    Field uiField;Handler originalUi;
    @Before public void boot()throws Exception{f.boot();}
    @After public void end()throws Exception{if(uiField!=null&&originalUi!=null)uiField.set(f.h.svc,originalUi);f.end();}
    void settings(){f.h.main(()->f.h.a.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));}
    void report(String value){Bundle data=new Bundle();data.putString("stream","PXE16_TARGET_DIAGNOSTIC "+value+"\n");f.h.in.sendStatus(0,data);}
    static CompletableFuture<?> gestureCompletion(Runnable callback){
        if(callback==null)return null;boolean gesture=false;CompletableFuture<?> result=null;
        for(Field field:callback.getClass().getDeclaredFields())try{field.setAccessible(true);Object value=field.get(callback);if(value instanceof GestureDescription)gesture=true;if(value instanceof CompletableFuture)result=(CompletableFuture<?>)value;}catch(ReflectiveOperationException error){throw new AssertionError(error);}
        return gesture?result:null;
    }
    String permitDetails(Runnable callback)throws Exception {
        StringBuilder details=new StringBuilder("callback_class=").append(callback.getClass().getName());
        for(Field field:callback.getClass().getDeclaredFields()){
            if(field.getName().contains("permitWindow")||field.getName().contains("revision")||field.getName().contains("target")){field.setAccessible(true);Object value=field.get(callback);details.append(',').append(field.getName()).append('=').append(value instanceof String&&!value.equals(f.h.a.getPackageName())&&!value.equals("com.android.settings")?"기타":value);}
        }
        Field revision=MacroService.class.getDeclaredField("windowRevision");revision.setAccessible(true);details.append(", live_window_revision=").append(((AtomicLong)revision.get(f.h.svc)).get());return details.toString();
    }
    @Test public void confirmedSettingsBeforeReturningTheReadCannotCompleteTheTouch()throws Exception {
        f.nextAppearsAfterFirst();AtomicLong switchedAt=new AtomicLong();
        f.sourceField.set(f.h.svc,(MacroService.ScreenSource)token->{ArrayList<MacroService.Hit> actual=f.originalSource.read(token);
            if(f.reads.incrementAndGet()==3){settings();f.h.waitFor(()->"com.android.settings".equals(f.h.svc.activePackage()),3000,"실제 Settings 활성 전환이 완료되지 않았습니다");switchedAt.set(SystemClock.elapsedRealtime());}
            return actual;});
        Step next=f.text("다음 화면",0);next.waitForever=true;f.start(f.macro(true,f.text("첫 번째",1400),next));
        f.h.waitFor(()->switchedAt.get()>0,6000,"실제 전환을 확정한 snapshot 반환이 없습니다");SystemClock.sleep(500);
        report("case=confirmed_read, switched_at="+switchedAt.get()+", active="+f.h.svc.activePackage()+", first="+f.firstTaps.get()+", second="+f.secondTaps.get()+", running="+f.h.svc.running+", completed="+f.h.svc.logText().contains("2번 글자 찾아 터치 완료"));
        assertEquals(f.evidence(),0,f.secondTaps.get());assertFalse(f.evidence(),f.h.svc.logText().contains("2번 글자 찾아 터치 완료"));assertTrue(f.evidence(),f.h.svc.running);
    }
    @Test public void switchingAfterTheFinalCheckBeforeDispatchCannotTouchSettings()throws Exception {
        f.nextAppearsAfterFirst();AtomicReference<Runnable> held=new AtomicReference<>();AtomicInteger dispatches=new AtomicInteger();AtomicLong heldAt=new AtomicLong(),completedAt=new AtomicLong();
        uiField=MacroService.class.getDeclaredField("ui");uiField.setAccessible(true);originalUi=(Handler)uiField.get(f.h.svc);
        Handler controlled=new Handler(Looper.getMainLooper()){
            @Override public void dispatchMessage(Message message){
                Runnable callback=message.getCallback();Method enclosing=callback==null?null:callback.getClass().getEnclosingMethod();
                CompletableFuture<?> completion=gestureCompletion(callback);
                if(enclosing!=null&&enclosing.getName().equals("multiGesture")&&completion!=null&&dispatches.incrementAndGet()==2){heldAt.set(SystemClock.elapsedRealtime());completion.whenComplete((value,error)->completedAt.set(SystemClock.elapsedRealtime()));held.set(callback);return;}
                super.dispatchMessage(message);
            }
        };
        uiField.set(f.h.svc,controlled);assertSame("제어 Handler 교체가 반영되지 않았습니다",controlled,uiField.get(f.h.svc));
        Step next=f.text("다음 화면",0);next.waitForever=true;f.start(f.macro(true,f.text("첫 번째",1400),next));
        f.h.waitFor(()->held.get()!=null,6000,"prepare 이후 두 번째 dispatch 지점이 포착되지 않았습니다");
        report("case=held, "+permitDetails(held.get())+", future_done="+gestureCompletion(held.get()).isDone()+", completed_at="+completedAt.get());
        String before=f.h.svc.activePackage();assertEquals("전환 전 원래 대상 앱이 아닙니다",f.h.a.getPackageName(),before);
        settings();f.h.waitFor(()->"com.android.settings".equals(f.h.svc.activePackage()),4000,"dispatch 전 Settings 실제 활성이 확정되지 않았습니다");
        long releasedAt=SystemClock.elapsedRealtime();report("case=release, "+permitDetails(held.get())+", completed_before_release="+f.h.svc.logText().contains("2번 글자 찾아 터치 완료")+", future_done="+gestureCompletion(held.get()).isDone());controlled.post(held.get());
        f.h.waitFor(()->!f.h.svc.running||f.h.svc.logText().contains("2번 글자 찾아 터치 완료")||f.h.svc.progressText().contains("대상 앱"),4000,"전송 해제 후 결과가 없습니다");SystemClock.sleep(250);
        report("case=before_dispatch, held_at="+heldAt.get()+", released_at="+releasedAt+", future_completed_at="+completedAt.get()+", before="+before+", after="+f.h.svc.activePackage()+", first="+f.firstTaps.get()+", second="+f.secondTaps.get()+", running="+f.h.svc.running+", completed="+f.h.svc.logText().contains("2번 글자 찾아 터치 완료"));
        assertEquals(f.evidence(),0,f.secondTaps.get());assertFalse("다른 앱 위에서 원래 좌표 제스처가 완료됐습니다\n"+f.evidence(),f.h.svc.logText().contains("2번 글자 찾아 터치 완료"));assertTrue(f.evidence(),f.h.svc.running);
    }
}
