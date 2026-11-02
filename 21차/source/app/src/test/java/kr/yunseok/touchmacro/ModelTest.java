package kr.yunseok.touchmacro;
import org.junit.Test;
import org.json.JSONObject;
import static org.junit.Assert.*;
public class ModelTest {
 @Test public void sampleHasFiniteBackRetries(){Macro m=Store.sample();m.validate();assertEquals("BACK_RETRY",m.steps.get(0).failure);assertEquals(3,m.steps.get(0).retries);}
 @Test public void serializePreservesTextAndBranchSettings(){Macro m=new Macro();Step s=new Step("TEXT_BRANCH");s.text="구매 확인";s.found=2;s.missing=0;s.match="EXACT";s.source="OCR";s.occurrence=2;m.steps.add(s);m.steps.add(new Step("STOP"));m.rounds=0;Macro restored=new Macro(m.json());restored.validate();assertEquals(s.text,restored.steps.get(0).text);assertEquals(2,restored.steps.get(0).found);assertEquals("OCR",restored.steps.get(0).source);assertEquals(0,restored.rounds);}
 @Test(expected=IllegalArgumentException.class) public void missingBranchTargetRejected(){Step s=new Step("TEXT_BRANCH");s.text="확인";s.missing=3;s.validate(2);}
 @Test(expected=IllegalArgumentException.class) public void invalidGotoRejected(){Step s=new Step("GOTO");s.target=0;s.validate(1);}
 @Test(expected=IllegalArgumentException.class) public void blankTargetRejected(){new Step("TEXT_TAP").validate(1);}
 @Test(expected=IllegalArgumentException.class) public void nanCoordinateRejected(){Step s=new Step();s.x=Double.NaN;s.validate(1);}
 @Test(expected=IllegalArgumentException.class) public void negativeRetriesRejected(){Step s=new Step("TEXT_TAP");s.text="확인";s.retries=-1;s.validate(1);}
 @Test(expected=IllegalArgumentException.class) public void invalidSourceRejected(){Step s=new Step("TEXT_TAP");s.text="확인";s.source="ONLINE";s.validate(1);}
 @Test(expected=IllegalArgumentException.class) public void futureSchemaRejected() throws Exception {new Macro(new JSONObject("{\"schema\":9,\"steps\":[]}"));}
 @Test(expected=IllegalArgumentException.class) public void emptyMacroRejected(){new Macro().validate();}
}
