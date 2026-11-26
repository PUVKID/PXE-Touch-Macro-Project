package kr.yunseok.touchmacro;
import org.junit.Test;import static org.junit.Assert.*;
public class StrictText28Test {
 @Test public void adobeIsLiteralOnly(){TextRules.Prepared r=TextRules.prepareExact("Adobe");assertTrue(r.matches("Adobe"));for(String x:new String[]{"어도비","ADOBE","adobe","Ａｄｏｂｅ"," Adobe","Adobe ","Ado be","Adobe Pro","Pro Adobe","Adobe\n"})assertFalse(x,r.matches(x));}
 @Test public void oldRegexSettingAcceptsLiteral(){Step s=new Step("TEXT_TAP");s.text="[";s.match="REGEX";s.validate(1);assertTrue(TextRules.prepareExact(s.text).matches("["));}
 @Test public void literalSymbolsAndEmpty(){assertTrue(TextRules.prepareExact("A+B").matches("A+B"));assertFalse(TextRules.prepareExact("A+B").matches("AAAB"));assertFalse(TextRules.prepareExact("").matches(""));assertFalse(TextRules.prepareExact("Adobe").matches(null));}
}
