package kr.yunseok.touchmacro;
import android.content.Intent;import android.graphics.*;import android.graphics.drawable.Drawable;import android.view.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class) public class PxeDesignReviewTest {
    GuidedFlowTest h=new GuidedFlowTest();
    @Before public void boot()throws Exception{h.boot();h.main(()->h.svc.controller().close());}
    @After public void end(){h.end();}
    View byDescription(View root,String value){if(value.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View found=byDescription(group.getChildAt(i),value);if(found!=null)return found;}}return null;}
    void tab(String name,int expected)throws Exception{
        Rect[] rect={null};h.main(()->{View v=byDescription(h.a.findViewById(android.R.id.content),name);assertNotNull(v);rect[0]=h.bounds(v);});h.tap(rect[0].exactCenterX(),rect[0].exactCenterY());h.in.waitForIdleSync();
        java.lang.reflect.Field selected=MainActivity.class.getDeclaredField("tab");selected.setAccessible(true);assertEquals(expected,selected.getInt(h.a));
    }
    void open(int mode){h.main(()->Ui.prefs(h.a).edit().putInt("theme",mode).commit());h.a=(MainActivity)h.in.startActivitySync(new Intent(h.in.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));h.in.waitForIdleSync();}
    @Test public void reviewBothThemesNavigationWizardAndBrand()throws Exception{
        h.main(()->{Macro m=new Macro();m.name="내 매크로";m.startDelay=0;Step tap=new Step("TAP");tap.x=50;tap.y=50;m.steps.add(tap);Step wait=new Step("WAIT");wait.duration=1000;m.steps.add(wait);Store.put(h.a,m);WatchConfig p=new WatchConfig();p.rule.text="관심 키워드";p.macroName=m.name;p.priorityAreas.add(new WatchArea(5,20,95,35));p.priorityAreas.add(new WatchArea(5,40,95,55));p.priorityAreas.add(new WatchArea(5,60,95,75));WatchStore.save(h.a,p);SetupFlow.finished(h.a);});
        open(1);assertEquals("PXE",h.a.getApplicationInfo().loadLabel(h.a.getPackageManager()).toString());assertFalse(Ui.dark(h.a));h.screenshot("PXE_라이트_탐지.png");
        tab("동작",1);h.screenshot("PXE_라이트_동작.png");tab("설정",3);h.screenshot("PXE_라이트_설정.png");tab("글자",0);h.screenshot("PXE_라이트_글자.png");
        open(2);assertTrue(Ui.dark(h.a));h.screenshot("PXE_다크_탐지.png");tab("설정",3);h.screenshot("PXE_다크_설정.png");
        h.main(()->{SetupFlow.begin(h.a);h.a.refreshSetup();});h.in.waitForIdleSync();h.screenshot("PXE_다크_첫설정.png");open(1);h.screenshot("PXE_라이트_첫설정.png");
        h.main(()->{Bitmap b=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);Drawable icon=h.a.getDrawable(R.drawable.ic_macro);icon.setBounds(0,0,512,512);icon.draw(new Canvas(b));try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(h.a.getExternalFilesDir(null),"PXE_앱아이콘.png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}catch(Exception e){throw new RuntimeException(e);}finally{b.recycle();}});
    }
}
