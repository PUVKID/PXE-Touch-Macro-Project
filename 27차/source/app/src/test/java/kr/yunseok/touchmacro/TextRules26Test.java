package kr.yunseok.touchmacro;
import org.junit.Test;
import static org.junit.Assert.*;
public class TextRules26Test {
 @Test public void englishBrandNeverMatchesKoreanPronunciation(){for(String mode:new String[]{"EXACT","CONTAINS","REGEX"})for(boolean spaces:new boolean[]{true,false})for(boolean ignoreCase:new boolean[]{true,false}){assertFalse(mode,TextRules.matches("어도비","Adobe",mode,spaces,ignoreCase));assertFalse(mode,TextRules.matches("Adobe","어도비",mode,spaces,ignoreCase));assertTrue(mode,TextRules.matches("Adobe","Adobe",mode,spaces,ignoreCase));}}
 @Test public void exactEnglishDoesNotAcceptLongerOrTranslatedLabels(){assertFalse(TextRules.matches("Adobe 어도비","Adobe","EXACT",true,true));assertFalse(TextRules.matches("AdobeX","Adobe","EXACT",true,true));assertTrue(TextRules.matches("ADOBE","Adobe","EXACT",true,true));assertFalse(TextRules.matches("ADOBE","Adobe","EXACT",true,false));}
 @Test public void normalizedBlankNeverMatches(){for(String value:new String[]{"\u3000","\u00A0","\u200B","\uFEFF"," \u200B\n"}){assertFalse(value,TextRules.matches("결제",value,"CONTAINS",true,true));assertFalse(value,TextRules.matches("",value,"EXACT",true,true));}}
 @Test public void invisibleOcrSpacingIsOptional(){assertTrue(TextRules.matches("확\u200B인\uFEFF","확인","EXACT",true,true));assertFalse(TextRules.matches("확\u200B인","확인","EXACT",false,true));}
 @Test public void unicodeSpaceAndComposedHangul(){assertTrue(TextRules.matches("확\u3000인\u00A0완료","확인완료","EXACT",true,true));}
 @Test public void preparedRuleKeepsComparison(){TextRules.Prepared rule=TextRules.prepare("확인","EXACT",true,false);assertTrue(rule.matches("확 인"));assertFalse(rule.matches("확인 취소"));assertFalse(rule.matches(null));}
 @Test public void regexSyntaxAndSpacesRemain(){assertTrue(TextRules.prepare("확인\\d+","REGEX",true,true).matches("확인123"));assertTrue(TextRules.prepare("A B","REGEX",false,true).matches("a b"));}
 @Test public void falseSpaceOptionPreservesTarget(){assertTrue(TextRules.matches("\u3000","\u3000","EXACT",false,false));assertFalse(TextRules.matches("내용","\u3000","CONTAINS",false,false));}
}
