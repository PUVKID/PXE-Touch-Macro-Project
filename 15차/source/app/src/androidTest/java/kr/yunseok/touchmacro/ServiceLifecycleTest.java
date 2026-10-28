package kr.yunseok.touchmacro;

import android.content.Intent;
import android.os.Handler;
import android.os.Binder;
import android.os.SystemClock;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicInteger;

/** 연결 해제 콜백의 제어 재현과 실제 설정의 해제·재연결을 구분해 검사합니다. */
@RunWith(AndroidJUnit4.class) public class ServiceLifecycleTest {
    final GuidedFlowTest h=new GuidedFlowTest();
    @Before public void boot()throws Exception{
        MacroService previous=MacroService.get();h.boot();
        h.waitFor(()->MacroService.get()!=null&&MacroService.get()!=previous,12000,"새 접근성 서비스 연결 실패");h.svc=MacroService.get();h.fixture();
    }
    @After public void end(){h.end();}
    Macro macro(){Macro m=new Macro();m.name="접근성 연결 수명 검사";m.startDelay=0;m.maxMinutes=0;Step s=new Step("WAIT");s.duration=600000;s.after=0;m.steps.add(s);return m;}
    void start(){h.main(()->h.svc.start(macro()));h.waitFor(()->ControlDiagnostics.stopShown(h),3000,"실행 중 정지 컨트롤 표시 실패");}
    boolean shown(FloatingController c){boolean[] value={false};h.main(()->value[0]=c.shown());return value[0];}
    void oldCallbacks(FloatingController c){h.main(()->{c.show();c.refresh();c.update();});}
    void reconnect()throws Exception{
        MacroService previous=h.svc;
        h.shell("settings put secure enabled_accessibility_services \"\"");h.shell("settings put secure accessibility_enabled 0");
        h.waitFor(()->{try{return Boolean.TRUE.equals(ControlDiagnostics.field(previous,"destroyed"));}catch(Exception error){throw new RuntimeException(error);}},12000,"이전 서비스 종료 미완료");
        h.shell("settings put secure enabled_accessibility_services kr.yunseok.touchmacro/kr.yunseok.touchmacro.MacroService");h.shell("settings put secure accessibility_enabled 1");
        h.waitFor(()->MacroService.get()!=null&&MacroService.get()!=previous,12000,"새 접근성 연결 실패");h.svc=MacroService.get();h.fixture();
    }
    @Test public void unbindCallbackStopsRunAndDisposesQueuedController()throws Exception{
        start();MacroService old=h.svc;FloatingController controller=old.controller();
        // 사용자 닫기는 같은 연결에서 다시 표시할 수 있어야 합니다.
        h.main(()->{controller.close();controller.show();});assertTrue(shown(controller));
        WindowManager actual=(WindowManager)ControlDiagnostics.field(controller,"wm");AtomicInteger attempts=new AtomicInteger();
        WindowManager invalid=(WindowManager)Proxy.newProxyInstance(WindowManager.class.getClassLoader(),new Class<?>[]{WindowManager.class},(proxy,method,args)->{
            // OS 콜백 전에 토큰이 사라지는 경계를 실제 WindowManager의 잘못된 토큰으로 주입합니다.
            if(method.getName().equals("addView")){attempts.incrementAndGet();((WindowManager.LayoutParams)args[1]).token=new Binder();}
            try{return method.invoke(actual,args);}catch(InvocationTargetException error){throw error.getCause();}
        });
        Field manager=FloatingController.class.getDeclaredField("wm");manager.setAccessible(true);manager.set(controller,invalid);
        Throwable[] escaped={null};h.main(()->{try{controller.refresh();}catch(Throwable error){escaped[0]=error;}});
        assertNull("토큰 해제 경쟁이 앱 종료로 전파됐습니다: "+escaped[0],escaped[0]);
        assertFalse(old.running);assertNotSame(old,MacroService.get());assertFalse(shown(controller));
        assertTrue("토큰 오류 운영 기록이 없습니다",new ErrorJournal(h.a.getFilesDir()).read().contains("ACCESSIBILITY_OVERLAY_TOKEN_INVALID"));
        oldCallbacks(controller);assertEquals("사용할 수 없는 토큰으로 재시도했습니다",1,attempts.get());
        reconnect();start();MacroService unbound=h.svc;FloatingController retired=unbound.controller();
        Handler handler=(Handler)ControlDiagnostics.field(retired,"h");
        h.main(()->{handler.postDelayed(()->{retired.show();retired.refresh();retired.update();},100);unbound.onUnbind(new Intent(h.a,MacroService.class));});
        assertFalse("onUnbind 뒤 이전 실행이 남았습니다",unbound.running);
        assertNotSame("해제된 서비스가 현재 참조로 남았습니다",unbound,MacroService.get());
        assertFalse("onUnbind 뒤 이전 컨트롤이 남았습니다",shown(retired));
        SystemClock.sleep(600);oldCallbacks(retired);assertFalse("예약 갱신이 해제된 컨트롤을 다시 만들었습니다",shown(retired));
    }
    @Test public void actualDisableEnableRetiresOldControllerAndNewStopWorks()throws Exception{
        start();MacroService old=h.svc;FloatingController controller=old.controller();
        h.shell("settings put secure enabled_accessibility_services \"\"");h.shell("settings put secure accessibility_enabled 0");
        h.waitFor(()->MacroService.get()!=old,12000,"실제 접근성 해제 미완료");assertFalse(old.running);assertFalse(shown(controller));
        h.shell("settings put secure enabled_accessibility_services kr.yunseok.touchmacro/kr.yunseok.touchmacro.MacroService");h.shell("settings put secure accessibility_enabled 1");
        h.waitFor(()->MacroService.get()!=null&&MacroService.get()!=old,12000,"실제 접근성 재연결 실패");h.svc=MacroService.get();
        oldCallbacks(controller);assertFalse("재연결 후 이전 컨트롤이 다시 나타났습니다",shown(controller));
        h.fixture();start();AccessibilityNodeInfo stop=h.find("■");assertNotNull("재연결된 정지 노드가 없습니다",stop);assertTrue(stop.isVisibleToUser());
        long before=SystemClock.elapsedRealtime();try{assertTrue(stop.performAction(AccessibilityNodeInfo.ACTION_CLICK));}finally{stop.recycle();}
        h.waitFor(()->!h.svc.running,1000,"재연결된 실제 정지 버튼 무응답");assertTrue(SystemClock.elapsedRealtime()-before<=1000);
    }
}
