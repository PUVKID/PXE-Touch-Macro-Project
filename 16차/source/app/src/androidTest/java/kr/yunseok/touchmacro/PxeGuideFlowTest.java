package kr.yunseok.touchmacro;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.*;import org.junit.runner.RunWith;

/** 새 작업 상태에서 실제 영역 지정과 녹화 안내를 검사합니다. */
@RunWith(AndroidJUnit4.class) public class PxeGuideFlowTest {
    GuidedFlowTest flow=new GuidedFlowTest();
    @Before public void boot()throws Exception{flow.boot();flow.main(()->WatchStore.save(flow.a,new WatchConfig()));}
    @After public void end(){flow.end();}
    @Test public void completeRedesignedGuideWithRealRegionAndRecording()throws Exception{flow.regionRecordingAutomaticallyReturnAndSaveDelays();}
}
