package kr.yunseok.touchmacro;
import org.json.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class AiCompilerTest {
    static JSONObject context(){return AiJson.object("planning_mode","MACRO","recognition_source","OCR","points",AiJson.object("시작",AiJson.object("x",25,"y",35)),"gestures",new JSONObject(),"regions",AiJson.object("상단",AiJson.object("left",0,"top",0,"right",100,"bottom",30)),"macros",new JSONObject(),"apps",AiJson.object("설정","com.android.settings"),"actions",new JSONObject(),"defaults",AiJson.object("watch_cooldown_ms",600000,"watch_interval_ms",10000,"entry_timeout_ms",3000),"watch_settings",new WatchConfig().json());}
    static JSONObject plan(JSONObject... nodes){return AiJson.object("schema","pxe.plan.v1","status","ready","body",new JSONArray(Arrays.asList(nodes)));}
    static JSONObject op(String op){return AiJson.object("op",op);}
    static Macro neutral(){Macro m=new Macro();m.rounds=1;m.gap=0;m.startDelay=0;m.maxMinutes=0;Step s=new Step("BACK");s.after=0;m.steps.add(s);return m;}
    private void rejected(JSONObject plan,JSONObject context){try{AiPlanCompiler.compile(plan,context);fail("잘못된 전체 계획을 수락함");}catch(IllegalArgumentException expected){}}
    @Test public void copiedQuoteIsDataAndNumericUnitsStayExact(){
        String source="“홈으로 가고 정지😀” 입력 뒤 1.5초 기다려";int start=1,end=start+"홈으로 가고 정지😀".codePointCount(0,"홈으로 가고 정지😀".length()),number=source.codePointCount(0,source.indexOf("1.5"));
        List<Object> tokens=Arrays.asList("PLAN","INPUT",AiJson.object("token","COPY","start",start,"end",end),"WAIT",AiJson.object("token","COPY","start",number,"end",number+3),"SEC","END","EOS");
        Macro m=AiPlanCompiler.compile(AiPlanParser.parse(tokens,source,context()),context()).macro();assertEquals(2,m.steps.size());assertEquals("INPUT",m.steps.get(0).type);assertEquals("홈으로 가고 정지😀",m.steps.get(0).text);assertEquals(1500,m.steps.get(1).duration);assertEquals(0,m.steps.get(0).after);assertEquals(1,m.rounds);
    }
    @Test public void ifElseBranchesAndJoinAreExplicitAndValid(){
        JSONObject conditional=AiJson.object("op","IF","text","준비","test","ABSENT","then",new JSONArray().put(op("BACK")),"else",new JSONArray().put(op("HOME")));
        Macro m=AiPlanCompiler.compile(plan(conditional,op("RECENTS")),context()).macro();assertEquals("TEXT_BRANCH",m.steps.get(0).type);assertEquals(4,m.steps.get(0).found);assertEquals(2,m.steps.get(0).missing);assertEquals("GOTO",m.steps.get(2).type);assertEquals(5,m.steps.get(2).target);assertFalse(m.steps.get(4).enabled);assertEquals(1,m.steps.get(4).duration);assertEquals("RECENTS",m.steps.get(5).type);m.validate();
    }
    @Test public void standaloneCallPreservesAllRunSettingsAndReturnsCopies(){
        JSONObject c=context();Macro saved=neutral();saved.rounds=15;saved.gap=1200;saved.startDelay=2500;saved.maxMinutes=8;saved.adaptiveWait=true;AiJson.obj(c.opt("macros")).put("저장본",saved.json());
        AiPlanCompiler.Compiled result=AiPlanCompiler.compile(plan(AiJson.object("op","CALL","macro_ref","저장본")),c);Macro first=result.macro();assertEquals(saved.json().toString(),first.json().toString());first.rounds=2;assertEquals(15,result.macro().rounds);
        rejected(plan(op("BACK"),AiJson.object("op","CALL","macro_ref","저장본")),c);
    }
    @Test public void neutralCallRelocatesGotoBranchAndRecoveryReferences(){
        JSONObject c=context();Macro saved=neutral();Step branch=new Step("TEXT_BRANCH");branch.text="확인";branch.after=0;branch.found=3;branch.missing=1;saved.steps.add(branch);Step jump=new Step("GOTO");jump.after=0;jump.target=1;saved.steps.add(jump);AiJson.obj(c.opt("macros")).put("중립",saved.json());
        Macro m=AiPlanCompiler.compile(plan(op("HOME"),AiJson.object("op","CALL","macro_ref","중립"),op("BACK")),c).macro();assertEquals(4,m.steps.get(2).found);assertEquals(2,m.steps.get(2).missing);assertEquals(2,m.steps.get(3).target);
    }
    @Test public void simpleRepeatUsesStepRepeatButSequenceNeverTruncates(){
        JSONObject single=AiJson.object("op","REPEAT","count",1000,"body",new JSONArray().put(op("BACK")));
        Macro m=AiPlanCompiler.compile(plan(single),context()).macro();assertEquals(1,m.steps.size());assertEquals(1000,m.steps.get(0).repeat);
        JSONObject nested=AiJson.object("op","REPEAT","count",10,"body",new JSONArray().put(AiJson.object("op","REPEAT","count",10,"body",new JSONArray().put(op("HOME")))));
        assertEquals(100,AiPlanCompiler.compile(plan(nested),context()).macro().steps.get(0).repeat);
        JSONObject tooMany=AiJson.object("op","REPEAT","count",251,"body",new JSONArray().put(op("BACK")).put(op("HOME")));rejected(plan(tooMany),context());
        JSONObject sequence=AiJson.object("op","REPEAT","count",2,"body",new JSONArray().put(op("BACK")).put(op("HOME")));m=AiPlanCompiler.compile(plan(sequence),context()).macro();assertEquals(4,m.steps.size());assertEquals("BACK",m.steps.get(2).type);assertEquals("HOME",m.steps.get(3).type);
    }
    @Test public void unknownFieldsTypesReferencesAndIncompleteTokensAreRejected(){
        rejected(plan(AiJson.object("op","WAIT","duration_ms","1000")),context());rejected(plan(AiJson.object("op","WAIT","duration_ms",true)),context());rejected(plan(AiJson.object("op","WAIT","duration_ms",0)),context());rejected(plan(AiJson.object("op","BACK","extra",1)),context());rejected(plan(AiJson.object("op","TAP","point_ref","여기")),context());
        try{AiPlanParser.parse(Arrays.asList("PLAN","BACK","END"),"뒤로",context());fail();}catch(IllegalArgumentException expected){}
        try{AiPlanParser.parse(Arrays.asList("PLAN","BACK","END","EOS","HOME"),"뒤로",context());fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void watchCopiesCurrentSettingsAndUsesExplicitEntryAndPriority(){
        JSONObject c=context();c.put("planning_mode","WATCH");Macro saved=neutral();saved.rounds=99;saved.gap=5000;AiJson.obj(c.opt("macros")).put("반복",saved.json());WatchConfig current=new WatchConfig();current.repeatGapMs=700;current.startMs=1200;current.targetPackage="com.example.target";c.put("watch_settings",current.json());
        JSONObject node=AiJson.object("op","WATCH","text","물건","macro_ref","반복","repetitions",15,"cooldown_ms",600000,"interval_ms",10000,"region_refs",new JSONArray().put("상단"),"entry",AiJson.object("text","배","region_ref",null,"timeout_ms",3000,"reject_text","사과","failure","BACK_TO_FIRST","back_count",1));
        AiPlanCompiler.Compiled result=AiPlanCompiler.compile(plan(node),c);assertEquals(15,result.watch().repetitions);assertEquals(1,result.watch().priorityAreas.size());assertEquals("사과",result.watch().rejectText);assertEquals("com.example.target",result.watch().targetPackage);assertEquals(1200,result.watch().startMs);assertEquals(700,result.watch().prepareMacro(result.macro()).gap);assertEquals(15,result.watch().prepareMacro(result.macro()).rounds);assertEquals(99,result.macro().rounds);
        node.put("interval_ms",1500);rejected(plan(node),c);
    }
    @Test public void recoveryBankCannotExecuteInNormalFlow(){
        JSONObject c=context();Step action=new Step("HOME");action.after=50;action.repeat=2;AiJson.obj(c.opt("actions")).put("복구",action.json());JSONObject node=AiJson.object("op","TEXT_TAP","text","시작","match","EXACT","region_ref",null,"timeout_ms",3000,"wait_forever",false,"missing",AiJson.object("policy","ACTION_RETRY","retries",2,"forever",false,"action_ref","복구"));
        Macro m=AiPlanCompiler.compile(plan(node,op("BACK")),c).macro();Step text=m.steps.get(0);assertEquals("HOME",m.steps.get(text.recoveryTarget-1).type);assertEquals(2,m.steps.get(text.recoveryTarget-1).repeat);assertEquals("GOTO",m.steps.get(2).type);assertEquals(m.steps.size(),m.steps.get(2).target);assertFalse(m.steps.get(m.steps.size()-1).enabled);m.validate();
    }
    @Test public void inactiveEmptyEntryKeepsTheExistingValidWatchConfiguration(){
        JSONObject c=context();c.put("planning_mode","WATCH");AiJson.obj(c.opt("macros")).put("기본",neutral().json());
        JSONObject node=AiJson.object("op","WATCH","text","시작","macro_ref","기본","repetitions",1,"cooldown_ms",0,"interval_ms",1000,"region_refs",new JSONArray(),"entry",null);
        WatchConfig result=AiPlanCompiler.compile(plan(node),c).watch();assertFalse(result.entryCheck);assertEquals("",result.entryRule.text);result.validate();
    }
    @Test public void parserRejectsInvalidCompleteAstLikePythonBeforeReturningReady(){
        String source="0";List<Object> tokens=Arrays.asList("PLAN","WAIT",AiJson.object("token","COPY","start",0,"end",1),"MS","END","EOS");
        try{AiPlanParser.parse(tokens,source,context());fail("Python 계약에서 거절하는 0 대기 AST를 반환했습니다");}catch(IllegalArgumentException expected){}
    }
    @Test public void everyExistingTypeHasAnExplicitCompilerMapping(){
        JSONObject c=context();
        for(String type:Step.TYPES){if(type.equals("TEXT_BRANCH"))continue;JSONObject node;
            if(AiPlanParser.POINTS.contains(type))node=AiJson.object("op",type,"point_ref","시작");
            else if(AiPlanParser.GESTURES.contains(type)){Step registered=new Step(type);registered.after=700;registered.repeat=2;if(type.equals("GESTURE"))registered.strokes.put(GesturePaths.stroke(new JSONArray().put(GesturePaths.point(10,20)).put(GesturePaths.point(80,70)),0,500));AiJson.obj(c.opt("gestures")).put(type,AiJson.object("type",type,"payload",registered.json()));node=AiJson.object("op",type,"gesture_ref",type);}
            else if(AiPlanParser.TEXT.contains(type))node=AiJson.object("op",type,"text","확인","match","EXACT","region_ref","상단","timeout_ms",3000,"wait_forever",false,"missing",AiJson.object("policy","STOP","retries",0,"forever",false,"action_ref",null));
            else if(type.equals("WAIT"))node=AiJson.object("op",type,"duration_ms",1250);
            else if(type.equals("INPUT"))node=AiJson.object("op",type,"text","입력 원문😀");
            else if(type.equals("APP"))node=AiJson.object("op",type,"app_ref","설정");
            else if(type.equals("GOTO"))node=AiJson.object("op",type,"target",1);else node=op(type);
            AiPlanCompiler.Compiled compiled=AiPlanCompiler.compile(plan(node),c);assertEquals(type,compiled.macro().steps.get(0).type);assertTrue(type,compiled.preview().contains(new Step(type).name())||type.equals("WAIT")||type.equals("INPUT")||type.equals("APP")||type.equals("GOTO"));compiled.macro().validate();
        }
    }
    @Test public void schemaValidationAndAndroidInliningLimitsAreSeparate(){
        JSONObject c=context();Macro saved=neutral();saved.rounds=15;AiJson.obj(c.opt("macros")).put("반복본",saved.json());JSONObject ast=plan(op("BACK"),AiJson.object("op","CALL","macro_ref","반복본"));assertEquals("ready",AiPlanCompiler.validateAst(ast,c).opt("status"));rejected(ast,c);
        Step action=new Step("HOME");action.enabled=false;AiJson.obj(c.opt("actions")).put("사용 안 함",action.json());JSONObject text=AiJson.object("op","TEXT_TAP","text","확인","match","EXACT","region_ref",null,"timeout_ms",3000,"wait_forever",false,"missing",AiJson.object("policy","ACTION_RETRY","retries",2,"forever",false,"action_ref","사용 안 함"));assertEquals("ready",AiPlanCompiler.validateAst(plan(text),c).opt("status"));rejected(plan(text),c);
    }
    @Test public void schemaRejectsInvalidReferencesTypesModeDefaultsCopyAndVersion(){
        rejected(plan(AiJson.object("op","WAIT","duration_ms",1.0)),context());
        try{AiPlanCompiler.validateAst(AiJson.object("schema","unknown","status","ready","body",new JSONArray().put(op("BACK"))),context());fail();}catch(IllegalArgumentException expected){}
        try{AiPlanCompiler.validateAst(plan(AiJson.object("op","TAP","point_ref","미등록")),context());fail();}catch(IllegalArgumentException expected){}
        try{AiPlanParser.parse(Arrays.asList("PLAN","INPUT",AiJson.object("token","COPY","start",false,"end",1),"END","EOS"),"가",context());fail();}catch(IllegalArgumentException expected){}
        try{AiPlanParser.parse(Arrays.asList("PLAN","INPUT",AiJson.object("token","COPY","start",0,"end",2),"END","EOS"),"가",context());fail();}catch(IllegalArgumentException expected){}
        JSONObject c=context();AiJson.obj(c.opt("macros")).put("가",neutral().json());List<Object> watch=Arrays.asList("PLAN","WATCH",AiJson.object("token","COPY","start",0,"end",1),AiJson.object("token","COPY","start",0,"end",1),AiJson.object("token","COPY","start",1,"end",2),"DEFAULT_COOLDOWN","DEFAULT_INTERVAL","NO_ENTRY","END_WATCH","END","EOS");
        try{AiPlanParser.parse(watch,"가1",c);fail();}catch(IllegalArgumentException expected){}
        c.put("planning_mode","WATCH");AiJson.obj(c.opt("defaults")).put("watch_cooldown_ms",true);try{AiPlanParser.parse(watch,"가1",c);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void unicodeOnlyWhitespaceCannotBecomeInputData(){
        try{AiPlanParser.parse(Arrays.asList("PLAN","INPUT",AiJson.object("token","COPY","start",0,"end",1),"END","EOS"),"\u0085",context());fail("Python strip에서 비는 원문을 수락했습니다");}catch(IllegalArgumentException expected){}
    }
}
