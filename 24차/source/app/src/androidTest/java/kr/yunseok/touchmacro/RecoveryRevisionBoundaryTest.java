package kr.yunseok.touchmacro;

import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.*;
import android.view.*;
import android.widget.FrameLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 전달 전 실제 창 변경이 회복 입력의 유한 횟수를 소진하지 않는지 확인합니다. */
@RunWith(AndroidJUnit4.class) public class RecoveryRevisionBoundaryTest {
    final GuidedFlowTest h=new GuidedFlowTest();final AtomicInteger ups=new AtomicInteger(),dispatches=new AtomicInteger();
    final AtomicReference<Runnable> held=new AtomicReference<>();final AtomicReference<Throwable> rejected=new AtomicReference<>();
    Field sourceField,uiField;Object originalSource;Handler originalUi,controlled;AtomicLong rawRevision;
    WindowManager windows;View marker;long permitRevision,changedRevision;volatile float startY,endY;
    @Before public void boot()throws Exception{
        h.boot();h.fixture();sourceField=MacroService.class.getDeclaredField("accessibilitySource");sourceField.setAccessible(true);originalSource=sourceField.get(h.svc);
        sourceField.set(h.svc,(MacroService.ScreenSource)token->new ArrayList<>());
        Field revision=MacroService.class.getDeclaredField("windowRevision");revision.setAccessible(true);rawRevision=(AtomicLong)revision.get(h.svc);
        uiField=MacroService.class.getDeclaredField("ui");uiField.setAccessible(true);originalUi=(Handler)uiField.get(h.svc);
        controlled=new Handler(Looper.getMainLooper()){@Override public void dispatchMessage(Message message){
            Runnable callback=message.getCallback();Method enclosing=callback==null?null:callback.getClass().getEnclosingMethod();CompletableFuture<?> result=TargetDispatchBoundaryTest.gestureCompletion(callback);
            if(enclosing!=null&&enclosing.getName().equals("multiGesture")&&result!=null&&dispatches.incrementAndGet()==1){permitRevision=rawRevision.get();result.whenComplete((value,error)->rejected.set(error));held.set(callback);return;}
            super.dispatchMessage(message);
        }};
        uiField.set(h.svc,controlled);
        h.main(()->{FrameLayout frame=new FrameLayout(h.a);frame.setOnTouchListener((view,event)->{if(event.getActionMasked()==MotionEvent.ACTION_DOWN)startY=event.getRawY();if(event.getActionMasked()==MotionEvent.ACTION_UP){endY=event.getRawY();ups.incrementAndGet();}return true;});h.a.setContentView(frame);});
        h.in.waitForIdleSync();h.waitFor(()->h.a.getPackageName().equals(h.svc.activePackage()),4000,"원래 대상 앱 확인 실패");
    }
    @After public void end()throws Exception{
        Bundle data=new Bundle();Throwable error=rejected.get();data.putString("stream","PXE39_REVISION permit="+permitRevision+",changed="+changedRevision+",actual="+(rawRevision==null?-1:rawRevision.get())+",ups="+ups.get()+",dispatches="+dispatches.get()+",exception="+(error==null?"none":error.getClass().getSimpleName())+"\n");h.in.sendStatus(0,data);
        try{h.main(()->{if(marker!=null&&marker.isAttachedToWindow())windows.removeViewImmediate(marker);});}finally{if(uiField!=null&&originalUi!=null)uiField.set(h.svc,originalUi);h.end();if(sourceField!=null)sourceField.set(h.svc,originalSource);}
    }
    @Test public void sameAppWindowChangeBeforeRecoveryDispatchRetainsOneFiniteAttempt()throws Exception{
        Step step=new Step("TEXT_WAIT");step.text="없는 글자";step.source="ACCESS";step.timeout=0;step.failure="SCROLL_RETRY";step.retries=1;step.retryDelay=300;step.after=0;
        Macro macro=new Macro();macro.startDelay=0;macro.maxMinutes=0;macro.steps.add(step);h.main(()->h.svc.start(macro));
        h.waitFor(()->held.get()!=null,5000,"첫 회복 전달 지점을 포착하지 못했습니다");
        assertEquals("창 변경 전 대상 앱이 다릅니다",h.a.getPackageName(),h.svc.activePackage());
        h.main(()->{windows=(WindowManager)h.svc.getSystemService(Context.WINDOW_SERVICE);marker=new View(h.svc);WindowManager.LayoutParams layout=new WindowManager.LayoutParams(1,1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);layout.gravity=Gravity.TOP|Gravity.LEFT;windows.addView(marker,layout);});
        h.waitFor(()->rawRevision.get()!=permitRevision,3000,"실제 접근성 창 변경 이벤트가 없습니다");changedRevision=rawRevision.get();
        assertEquals("실제 창 변경 뒤 원래 대상 앱을 유지해야 합니다",h.a.getPackageName(),h.svc.activePackage());assertTrue(changedRevision>permitRevision);
        controlled.post(held.get());h.waitFor(()->ups.get()==1,5000,"동일 앱 창 변경 때문에 전달 전 회복 횟수가 소진됐습니다");
        h.main(()->h.svc.stop());assertEquals(1,ups.get());int height=h.svc.size()[1];assertEquals(height*.75,startY,2);assertEquals(height*.525,endY,2);assertEquals(height*.225,startY-endY,3);
    }
}
