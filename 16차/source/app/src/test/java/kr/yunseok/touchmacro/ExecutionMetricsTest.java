package kr.yunseok.touchmacro;

import org.junit.Test;
import static org.junit.Assert.*;

/** 단계는 하위 시간을 포함하며 완료 뒤 늦은 Task가 확정 통계를 바꾸지 못합니다. */
public class ExecutionMetricsTest {
    @Test public void totalsKeepTheirIndependentMeasurementScopes() {
        ExecutionMetrics metrics=new ExecutionMetrics();metrics.beginStep(0);metrics.endStep(900);metrics.beginStep(900);metrics.endStep(1500);
        metrics.beginOcr(10);metrics.endOcr(230);metrics.beginCapture(230);metrics.endCapture(660);
        metrics.beginConfigured(0,0);metrics.endConfigured(100);metrics.beginConfigured(100,0);metrics.endConfigured(300);
        ExecutionMetrics.Snapshot view=metrics.snapshot(1500);
        assertEquals(1500,view.stepMs);assertEquals(2,view.steps);
        assertEquals(220,view.ocrMs);assertEquals(1,view.ocrCount);
        assertEquals(430,view.captureMs);assertEquals(300,view.configuredMs);
    }
    @Test public void finishFreezesLateRecognitionAndDoesNotAffectTheNextRun() {
        ExecutionMetrics old=new ExecutionMetrics();old.beginOcr(0);old.finish(100);old.endOcr(9100);old.beginStep(0);old.endStep(200);
        ExecutionMetrics next=new ExecutionMetrics();next.beginStep(0);next.endStep(50);
        assertEquals(100,old.snapshot(9100).ocrMs);assertEquals(0,old.snapshot(9100).steps);
        assertEquals(50,next.snapshot(50).stepMs);assertEquals(0,next.snapshot(50).ocrCount);
    }
    @Test public void liveSnapshotIncludesRunningScopesButCapsLeadingDelayAtItsDeadline() {
        ExecutionMetrics metrics=new ExecutionMetrics();metrics.beginStep(100);metrics.beginOcr(200);metrics.beginConfigured(200,500);
        ExecutionMetrics.Snapshot live=metrics.snapshot(800);assertEquals(700,live.stepMs);assertEquals(600,live.ocrMs);assertEquals(300,live.configuredMs);
        metrics.finish(800);assertEquals(700,metrics.snapshot(9000).stepMs);assertEquals(300,metrics.snapshot(9000).configuredMs);
    }
}
