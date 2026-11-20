package kr.yunseok.touchmacro;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static org.junit.Assert.*;

/** 창 식별·수락 순서와 늦은 조회의 세대 덮어쓰기를 검사합니다. */
public class WindowObservationTest {
    @Test public void changedAcceptedWindowInvalidatesExistingPermitOnlyOnce(){
        AtomicLong revision=new AtomicLong();WindowObservation observation=new WindowObservation(revision);
        assertTrue(observation.accept(1,"원래 창"));assertEquals(0,revision.get());
        assertTrue(observation.accept(2,"다른 창"));assertEquals(1,revision.get());
        assertTrue(observation.accept(3,"다른 창"));assertEquals(1,revision.get());
    }
    @Test public void olderOrDuplicateResultCannotReplaceNewerAcceptedWindow(){
        AtomicLong revision=new AtomicLong();WindowObservation observation=new WindowObservation(revision);
        assertTrue(observation.accept(1,"원래 창"));assertTrue(observation.accept(3,"다른 창"));
        assertFalse(observation.accept(2,"원래 창"));assertFalse(observation.accept(3,"원래 창"));assertEquals(1,revision.get());
        assertTrue(observation.accept(4,"다른 창"));assertEquals(1,revision.get());
    }
    @Test public void identitiesIgnoreListOrderAndInactiveWindowsButIncludeFocusAndPackage(){
        WindowPackages.Window first=new WindowPackages.Window(1,1,true,true),inactive=new WindowPackages.Window(2,1,false,false);
        String key=WindowObservation.key(Arrays.asList(first,inactive),"원래 앱");
        assertEquals(key,WindowObservation.key(Collections.singletonList(first),"원래 앱"));
        assertEquals(key,WindowObservation.key(Arrays.asList(inactive,first),"원래 앱"));
        assertNotEquals(key,WindowObservation.key(Collections.singletonList(new WindowPackages.Window(1,1,true,false)),"원래 앱"));
        assertNotEquals(key,WindowObservation.key(Collections.singletonList(first),"다른 앱"));
        assertNotEquals(key,WindowObservation.key(Collections.singletonList(new WindowPackages.Window(3,1,true,true)),"원래 앱"));
    }
    @Test public void existingEventRevisionIsNeverResetByAcceptedObservation(){
        AtomicLong revision=new AtomicLong(7);WindowObservation observation=new WindowObservation(revision);
        assertTrue(observation.accept(1,"원래 창"));assertEquals(7,revision.get());
        revision.incrementAndGet();assertTrue(observation.accept(2,"다른 창"));assertEquals(9,revision.get());
        assertTrue(observation.accept(3,"다른 창"));assertEquals(9,revision.get());
    }
    @Test public void olderSlowQueryFinishingAfterNewQueryCannotRestoreOldPermit()throws Exception{
        AtomicLong revision=new AtomicLong();WindowObservation observation=new WindowObservation(revision);observation.accept(0,"원래 창");
        CountDownLatch olderStarted=new CountDownLatch(1),newerAccepted=new CountDownLatch(1);ExecutorService worker=Executors.newSingleThreadExecutor();
        try{
            Future<Boolean> older=worker.submit(()->{olderStarted.countDown();assertTrue(newerAccepted.await(1,TimeUnit.SECONDS));return observation.accept(1,"원래 창");});
            assertTrue(olderStarted.await(1,TimeUnit.SECONDS));assertTrue(observation.accept(2,"다른 창"));newerAccepted.countDown();
            assertFalse(older.get(1,TimeUnit.SECONDS));assertEquals(1,revision.get());assertTrue(observation.accept(3,"다른 창"));assertEquals(1,revision.get());
        }finally{newerAccepted.countDown();worker.shutdownNow();}
    }
}
