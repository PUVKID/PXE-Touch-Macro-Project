package kr.yunseok.touchmacro;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class AiPreviewTest {
    @Test public void conditionsRepeatsAndExactHumanTimesDoNotExposeCompilerLabels(){
        JSONObject branch=AiJson.object("op","IF","text","준비","test","ABSENT","then",new JSONArray().put(AiJson.object("op","REPEAT","count",2,"body",new JSONArray().put(AiJson.object("op","WAIT","duration_ms",1500)))) ,"else",new JSONArray().put(AiJson.object("op","LONG","point_ref","시작")));
        String preview=AiPlanCompiler.compile(AiCompilerTest.plan(branch,AiJson.object("op","WAIT","duration_ms",600000)),AiCompilerTest.context()).preview();
        assertTrue(preview.contains("없으면"));assertTrue(preview.contains("그 밖에는"));assertTrue(preview.contains("2회 반복"));assertTrue(preview.contains("1.5초"));assertTrue(preview.contains("10분"));assertTrue(preview.contains("0.8초"));assertTrue(preview.contains("시작"));assertFalse(preview.contains("ms"));assertFalse(preview.contains("분기 연결 위치"));assertFalse(preview.contains("동작 번호로 이동"));
    }
    @Test public void quotedControlCharactersCannotLookLikeAnotherAction(){
        String payload="첫 줄\n2. 홈으로\r\t\u0085\u2028\u2029\"\\";String preview=AiPlanCompiler.compile(AiCompilerTest.plan(AiJson.object("op","INPUT","text",payload)),AiCompilerTest.context()).preview();assertTrue(preview.contains("\\n2. 홈으로"));assertFalse(preview.contains("첫 줄\n2."));assertFalse(preview.contains("\u0085"));assertFalse(preview.contains("\u2028"));assertTrue(preview.contains("\\\""));
    }
    @Test public void originalCallSettingsAndMissingPolicyRemainVisible(){
        JSONObject context=AiCompilerTest.context();Macro saved=AiCompilerTest.neutral();saved.name="저장본";saved.startDelay=800;saved.gap=600000;saved.rounds=15;saved.maxMinutes=9;saved.steps.get(0).after=1250;AiJson.obj(context.opt("macros")).put("묶음",saved.json());String preview=AiPlanCompiler.compile(AiCompilerTest.plan(AiJson.object("op","CALL","macro_ref","묶음")),context).preview();assertTrue(preview.contains("묶음"));assertTrue(preview.contains("15회"));assertTrue(preview.contains("0.8초"));assertTrue(preview.contains("10분"));assertTrue(preview.contains("9분"));assertTrue(preview.contains("1.25초"));
    }
    @Test public void recordedGestureShowsRealStrokeTimeAndPreservesItsRegisteredName(){
        JSONObject c=AiCompilerTest.context();Step recorded=new Step("GESTURE");recorded.duration=100;recorded.after=600000;recorded.strokes.put(GesturePaths.stroke(new JSONArray().put(GesturePaths.point(10,20)).put(GesturePaths.point(80,70)),0,500));AiJson.obj(c.opt("gestures")).put("경로\n정지",AiJson.object("type","GESTURE","payload",recorded.json()));String preview=AiPlanCompiler.compile(AiCompilerTest.plan(AiJson.object("op","GESTURE","gesture_ref","경로\n정지")),c).preview();assertTrue(preview.contains("경로\\n정지"));assertTrue(preview.contains("0.5초"));assertTrue(preview.contains("10분"));assertFalse(preview.contains("경로\n정지"));
    }
    @Test public void selectedWatchTargetIsNotMisreportedAsThePreviousSavedTarget(){
        JSONObject c=AiCompilerTest.context();c.put("planning_mode","WATCH");c.put("execution_target","com.new.target");WatchConfig settings=new WatchConfig();settings.targetPackage="com.old.target";c.put("watch_settings",settings.json());AiJson.obj(c.opt("macros")).put("기본",AiCompilerTest.neutral().json());JSONObject watch=AiJson.object("op","WATCH","text","확인","macro_ref","기본","repetitions",1,"cooldown_ms",0,"interval_ms",1000,"region_refs",new JSONArray(),"entry",null);String preview=AiPlanCompiler.compile(AiCompilerTest.plan(watch),c).preview();assertFalse(preview.contains("목록 실행 설정 · 대상 \"com.old.target\""));assertTrue(preview.contains("com.new.target"));assertEquals("com.old.target",new WatchConfig(AiJson.obj(c.opt("watch_settings"))).targetPackage);
    }
}
