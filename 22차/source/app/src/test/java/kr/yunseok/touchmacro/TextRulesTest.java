package kr.yunseok.touchmacro;
import org.junit.Test;
import static org.junit.Assert.*;
public class TextRulesTest {
 @Test public void koreanSpaceAndExact(){assertTrue(TextRules.matches("  구매\n 확인  ","구매확인","EXACT",true,true));assertFalse(TextRules.matches("구매 확인 취소","구매 확인","EXACT",false,true));}
 @Test public void containsAndCase(){assertTrue(TextRules.matches("BUY NOW","buy","CONTAINS",false,true));assertFalse(TextRules.matches("BUY NOW","buy","CONTAINS",false,false));}
 @Test public void neverMatchEmptyTarget(){assertFalse(TextRules.matches("삭제","","CONTAINS",true,true));assertFalse(TextRules.matches("삭제","  ","REGEX",true,true));}
 @Test public void unicodeNormalization(){assertTrue(TextRules.matches("ＡＢＣ","abc","EXACT",false,true));assertTrue(TextRules.matches("확인","확인","EXACT",false,false));}
 @Test public void regexDoesNotDestroyExpression(){assertTrue(TextRules.matches("확인123","확인\\d+","REGEX",true,true));assertFalse(TextRules.matches("확인","확인\\d+","REGEX",true,true));}
 @Test(expected=java.util.regex.PatternSyntaxException.class) public void invalidRegexFails(){TextRules.matches("확인","[","REGEX",true,true);}
}
