package kr.yunseok.touchmacro;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
public final class TextRules {
    private TextRules() {}
    public static String normalize(String value, boolean spaces, boolean ignoreCase) {
        String normalized=Normalizer.normalize(value==null?"":value,Normalizer.Form.NFKC);
        if(spaces){
            StringBuilder compact=new StringBuilder(normalized.length());
            normalized.codePoints().filter(c->!Character.isWhitespace(c)&&!Character.isSpaceChar(c)&&c!=0x200B&&c!=0xFEFF).forEach(compact::appendCodePoint);
            normalized=compact.toString();
        }
        return ignoreCase?normalized.toLowerCase(Locale.ROOT):normalized;
    }
    /** 탐지 한 번에서 같은 검색어의 정규화와 정규식 컴파일을 반복하지 않습니다. */
    public static Prepared prepare(String wanted,String mode,boolean spaces,boolean ignoreCase){
        return new Prepared(wanted,mode,spaces,ignoreCase);
    }
    /** 실행 시 저장된 구형 비교 옵션보다 원문 완전 일치를 우선합니다. */
    public static Prepared prepareExact(String wanted){return new Prepared(wanted,"LITERAL",false,false);}
    public static boolean matches(String actual,String wanted,String mode,boolean spaces,boolean ignoreCase){
        return prepare(wanted,mode,spaces,ignoreCase).matches(actual);
    }
    public static final class Prepared {
        private final String wanted,mode;
        private final boolean spaces,ignoreCase,valid;
        private final Pattern expression;
        private Prepared(String raw,String mode,boolean spaces,boolean ignoreCase){
            this.mode=mode;this.spaces=spaces;this.ignoreCase=ignoreCase;
            boolean supplied=raw!=null&&!raw.trim().isEmpty();
            expression=supplied&&"REGEX".equals(mode)?Pattern.compile(raw,ignoreCase?Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE:0):null;
            wanted=supplied&&! "REGEX".equals(mode)?("LITERAL".equals(mode)?raw:normalize(raw,spaces,ignoreCase)):"";
            valid=supplied&&("REGEX".equals(mode)||!wanted.isEmpty());
        }
        public boolean matches(String actual){
            if(!valid)return false;
            if("LITERAL".equals(mode))return wanted.equals(actual);
            String displayed=normalize(actual,spaces,ignoreCase);
            if(expression!=null)return expression.matcher(displayed).find();
            return "EXACT".equals(mode)?displayed.equals(wanted):displayed.contains(wanted);
        }
    }
}
