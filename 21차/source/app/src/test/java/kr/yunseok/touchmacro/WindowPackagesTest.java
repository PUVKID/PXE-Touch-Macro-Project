package kr.yunseok.touchmacro;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class WindowPackagesTest {
    final WindowPackages packages=new WindowPackages();
    WindowPackages.Window app(int id){return new WindowPackages.Window(id,1,true,true);}
    @Test public void requiresPackageEventForActualWindow(){packages.observe(10,"target",100);assertEquals("target",packages.resolve(Arrays.asList(app(10)),"own"));assertEquals("",packages.resolve(Arrays.asList(app(11)),"own"));}
    @Test public void anotherApplicationNeverUsesPreviousPackage(){packages.observe(10,"target",100);packages.observe(11,"other",101);assertEquals("other",packages.resolve(Arrays.asList(app(11),new WindowPackages.Window(10,1,false,false)),"own"));}
    @Test public void removedWindowCannotReuseOldPackage(){packages.observe(10,"target",100);packages.remove(10,110);assertEquals("",packages.resolve(Arrays.asList(app(10)),"own"));packages.observe(10,"other",111);assertEquals("other",packages.resolve(Arrays.asList(app(10)),"own"));}
    @Test public void lateContentEventCannotReviveRemovedWindow(){packages.observe(10,"target",100);packages.remove(10,110);packages.observe(10,"target",105);packages.observe(10,"target",110);assertEquals("",packages.resolve(Arrays.asList(app(10)),"own"));}
    @Test public void lateRemovalCannotEraseNewWindow(){packages.observe(10,"other",120);packages.remove(10,110);assertEquals("other",packages.resolve(Arrays.asList(app(10)),"own"));}
    @Test public void systemDialogAndInputMethodAreNotTarget(){packages.observe(10,"target",100);for(int type:new int[]{2,3,5})assertEquals("",packages.resolve(Arrays.asList(new WindowPackages.Window(20,type,true,false),new WindowPackages.Window(10,1,false,true)),"own"));}
    @Test public void onlyKnownOwnOverlayMayUseFocusedApplication(){packages.observe(10,"target",100);packages.observe(20,"own",100);List<WindowPackages.Window> windows=Arrays.asList(new WindowPackages.Window(20,4,true,false),new WindowPackages.Window(10,1,false,true));assertEquals("target",packages.resolve(windows,"own"));assertEquals("",packages.resolve(windows,"other-service"));}
    @Test public void missingOrAmbiguousActiveWindowFailsClosed(){packages.observe(10,"target",100);assertEquals("",packages.resolve(Collections.emptyList(),"own"));assertEquals("",packages.resolve(Arrays.asList(app(10),app(11)),"own"));assertEquals("",packages.resolve(Arrays.asList(new WindowPackages.Window(10,1,false,true)),"own"));}
}
