package kr.yunseok.touchmacro;

import android.content.*;
import android.graphics.Rect;
import android.os.*;
import android.view.accessibility.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.regex.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** 별도로 설치한 소유 앱의 UID·실제 입력을 대조합니다. 사용자 앱은 실행하지 않습니다. */
@RunWith(AndroidJUnit4.class) public class CrossUidHudTest {
    final RunHudTest r=new RunHudTest();final GuidedFlowTest h=r.h;static final String PACKAGE="kr.yunseok.pxe22fixture";
    @Before public void boot()throws Exception{r.boot();}
    @After public void end()throws Exception{h.shell("am force-stop "+PACKAGE);r.end();}
    String shellOutput(String command)throws Exception{try(ParcelFileDescriptor fd=h.auto.executeShellCommand(command);java.io.FileInputStream in=new java.io.FileInputStream(fd.getFileDescriptor());java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toString("UTF-8");}}
    AccessibilityNodeInfo candidate(AccessibilityNodeInfo node,Rect body){if(node==null)return null;if("android.widget.Button".contentEquals(node.getClassName())){Rect bounds=new Rect();node.getBoundsInScreen(bounds);if(bounds.intersect(body)&&bounds.width()>Ui.dp(h.a,20)&&bounds.height()>Ui.dp(h.a,20))return AccessibilityNodeInfo.obtain(node);}for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i),found=candidate(child,body);if(child!=null)child.recycle();if(found!=null)return found;}return null;}
    int offset(AccessibilityNodeInfo node){if(node==null)return 0;String value=String.valueOf(node.getContentDescription());if(value.startsWith("fixture_scroll_"))return Integer.parseInt(value.substring(15));for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);int found=offset(child);if(child!=null)child.recycle();if(found>0)return found;}return 0;}
    @Test public void trustedBodyPassesOneTapAndScrollToDifferentUid()throws Exception{
        String metadata=shellOutput("cmd package list packages -U "+PACKAGE);Matcher uid=Pattern.compile("uid:(\\d+)").matcher(metadata);assertTrue("별도 시험 앱 UID 없음: "+metadata,uid.find());assertNotEquals(h.a.getApplicationInfo().uid,Integer.parseInt(uid.group(1)));r.shortStats();h.main(()->h.a.startActivity(new Intent().setComponent(new ComponentName(PACKAGE,PACKAGE+".FixtureActivity")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));h.waitFor(()->{AccessibilityNodeInfo n=h.find("터치 0");if(n==null)return false;n.recycle();return true;},4000,"별도 UID 화면 없음");Rect[] body={null};h.main(()->{try{body[0]=h.bounds(r.hud());}catch(Exception e){throw new RuntimeException(e);}});AccessibilityNodeInfo button=null;for(AccessibilityWindowInfo window:h.auto.getWindows()){try{AccessibilityNodeInfo root=window.getRoot();if(root!=null){if(PACKAGE.contentEquals(root.getPackageName()))button=candidate(root,body[0]);root.recycle();}if(button!=null)break;}finally{window.recycle();}}assertNotNull("본문 아래 실제 별도 앱 버튼 없음",button);Rect tap=new Rect();button.getBoundsInScreen(tap);button.recycle();assertTrue(tap.intersect(body[0]));h.tap(tap.exactCenterX(),tap.exactCenterY());h.waitFor(()->{AccessibilityNodeInfo node=h.find("터치 1");if(node==null)return false;node.recycle();return true;},1000,"별도 UID 버튼에 수동 터치가 통과하지 않았습니다");h.screenshot("PXE22_별도UID_본문터치1.png");h.drag(tap.exactCenterX(),tap.exactCenterY(),tap.exactCenterX(),tap.exactCenterY()-Ui.dp(h.a,180));h.waitFor(()->{for(AccessibilityWindowInfo window:h.auto.getWindows()){try{AccessibilityNodeInfo root=window.getRoot();if(root!=null){int value=PACKAGE.contentEquals(root.getPackageName())?offset(root):0;root.recycle();if(value>0)return true;}}finally{window.recycle();}}return false;},1000,"별도 UID 스크롤에 입력이 통과하지 않았습니다");h.screenshot("PXE22_별도UID_본문스크롤.png");assertTrue("터치·스크롤 중 접힘 오발",!Ui.prefs(h.a).getBoolean("hud_collapsed",false));android.util.Log.i("PXE22_CROSS_UID","main_uid="+h.a.getApplicationInfo().uid+", fixture_uid="+uid.group(1)+", tap=1, scroll>0");
    }
}
