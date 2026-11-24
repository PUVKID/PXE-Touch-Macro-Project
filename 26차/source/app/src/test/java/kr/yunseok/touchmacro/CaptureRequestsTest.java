package kr.yunseok.touchmacro;

import org.junit.Test;
import static org.junit.Assert.*;

public class CaptureRequestsTest {
    @Test public void oneLostResponseAllowsOneReserveAndBlocksFurtherRequests(){
        CaptureRequests requests=new CaptureRequests();long first=requests.begin(),reserve=requests.begin();
        assertTrue(first>0);assertTrue(reserve>first);assertEquals(0,requests.begin());assertEquals(2,requests.size());
    }
    @Test public void lateOldCompletionCannotReleaseTheOtherRequest(){
        CaptureRequests requests=new CaptureRequests();long first=requests.begin(),reserve=requests.begin();
        assertTrue(requests.complete(first));long next=requests.begin();assertTrue(next>reserve);
        assertFalse(requests.complete(first));assertEquals(2,requests.size());assertEquals(0,requests.begin());
        assertTrue(requests.complete(reserve));assertEquals(1,requests.size());assertTrue(requests.complete(next));assertEquals(0,requests.size());
    }
}
