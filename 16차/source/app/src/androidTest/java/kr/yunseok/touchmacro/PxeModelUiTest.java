package kr.yunseok.touchmacro;

import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 실제 홈의 입체 모델과 주 동작·상태·정지의 독립 배치를 검사합니다. */
@RunWith(AndroidJUnit4.class) public class PxeModelUiTest {
    final PxeRedesignTest base=new PxeRedesignTest();
    @Before public void boot()throws Exception{base.boot();}
    @After public void end(){base.end();}
    View model(View root){if("PXE 상태 모델".equals(root.getTag()))return root;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){View found=model(g.getChildAt(i));if(found!=null)return found;}}return null;}
    View model(){return model(base.h.a.findViewById(android.R.id.content));}
    @Test public void homeUsesRealNativeGeometryWithoutCoveringKeywordOrStart()throws Exception {
        base.h.main(()->{View v=model();assertNotNull("홈에 실제 기하 모델이 없습니다",v);assertEquals("kr.yunseok.touchmacro.PxeModelView",v.getClass().getName());assertFalse(v.isClickable());assertTrue(v.getWidth()>=Ui.dp(base.h.a,80));assertTrue(v.getHeight()<=Ui.dp(base.h.a,112));
            View word=base.text(base.h.a.findViewById(android.R.id.content),WatchStore.load(base.h.a).rule.text);assertNotNull(word);assertFalse(Rect.intersects(base.h.bounds(v),base.h.bounds(word)));
            Bitmap image=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);v.draw(new Canvas(image));int painted=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if(Color.alpha(image.getPixel(x,y))>0)painted++;assertTrue("정점과 면을 실제로 그려야 합니다",painted>image.getWidth()*image.getHeight()/12);image.recycle();
            ScrollView sc=base.scroll(base.h.a.findViewById(android.R.id.content));sc.fullScroll(View.FOCUS_DOWN);
        });base.h.in.waitForIdleSync();base.h.waitFor(()->{final boolean[] visible={false};base.h.main(()->{TextView start=base.text(base.h.a.findViewById(android.R.id.content),"탐지 시작");Rect bounds=new Rect();visible[0]=start.getGlobalVisibleRect(bounds)&&bounds.height()==start.getHeight();});return visible[0];},2000,"주 버튼이 가려졌습니다");
        base.h.screenshot("PXE16_모델_홈.png");
    }
    @Test public void modelFollowsLivePauseAndStopWithoutReplacingHome()throws Exception {
        final View[] original={null};base.h.main(()->{original[0]=model();assertNotNull("홈에 실제 기하 모델이 없습니다",original[0]);Macro m=new Macro();m.startDelay=0;m.maxMinutes=1;Step wait=new Step("WAIT");wait.duration=10000;m.steps.add(wait);base.h.svc.start(m);base.h.svc.togglePause();});
        base.h.waitFor(()->{final boolean[] ready={false};base.h.main(()->ready[0]=String.valueOf(model().getContentDescription()).contains("일시정지"));return ready[0];},2000,"모델이 실제 일시정지 상태를 반영하지 않았습니다");
        base.h.main(()->{assertSame(original[0],model());try{assertEquals(base.h.svc.getClass().getMethod("performanceText").invoke(base.h.svc),((TextView)base.described("실행 측정")).getText().toString());}catch(ReflectiveOperationException e){throw new AssertionError("실제 실행 측정 연결이 없습니다",e);}base.h.svc.stop();});base.h.waitFor(()->{final boolean[] ready={false};base.h.main(()->ready[0]=String.valueOf(model().getContentDescription()).contains("대기"));return ready[0];},2000,"정지 뒤 대기 상태가 반영되지 않았습니다");
        base.h.main(()->assertSame(original[0],model()));
    }
    @Test public void themeAndMotionDisabledKeepReadableNativeHome()throws Exception {
        float old=android.provider.Settings.Global.getFloat(base.h.a.getContentResolver(),android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f);
        try{base.h.shell("settings put global animator_duration_scale 0");base.h.waitFor(()->!android.animation.ValueAnimator.areAnimatorsEnabled(),3000,"시스템 모션 꺼짐 적용 실패");
            for(int theme:new int[]{1,2}){base.h.main(()->{Ui.prefs(base.h.a).edit().putInt("theme",theme).commit();base.h.a.refreshSetup();});base.h.in.waitForIdleSync();base.h.main(()->{assertNotNull("홈에 실제 기하 모델이 없습니다",model());assertEquals(1f,base.privateView(base.h.a,"page").getAlpha(),0f);assertNotNull(base.described("실행 상태"));assertTrue(base.h.bounds(base.described("탐지")).height()>=Ui.dp(base.h.a,48));});base.h.screenshot("PXE16_모델_"+(theme==1?"라이트":"다크")+"_모션꺼짐.png");}
        }finally{base.h.shell("settings put global animator_duration_scale "+old);}
    }
}
