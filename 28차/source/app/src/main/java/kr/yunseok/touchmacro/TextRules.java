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
    /** OCR 요소를 원문에 순서대로 대응합니다. 생략/치환된 문자가 있으면 추정하지 않습니다. */
    public static int[] elementStarts(String line,java.util.List<String> elements){
        if(line==null||elements==null||elements.isEmpty())return null;
        int[] starts=new int[elements.size()];int cursor=0;
        for(int i=0;i<elements.size();i++){
            String part=elements.get(i);if(part==null||part.isEmpty())return null;
            int start=line.indexOf(part,cursor);if(start<0||!onlyWhitespace(line,cursor,start))return null;
            starts[i]=start;cursor=start+part.length();
        }
        return onlyWhitespace(line,cursor,line.length())?starts:null;
    }
    private static boolean onlyWhitespace(String text,int from,int to){for(int i=from;i<to;){int cp=text.codePointAt(i);if(!Character.isWhitespace(cp)&&!Character.isSpaceChar(cp))return false;i+=Character.charCount(cp);}return true;}
    /** 원문의 요소 경계에 맞는 단어/문구만 반환합니다. 공백도 원문 그대로 비교합니다. */
    public static java.util.List<int[]> elementSpans(String line,java.util.List<String> elements,String wanted){
        java.util.ArrayList<int[]> spans=new java.util.ArrayList<>();int[] starts=elementStarts(line,elements);
        if(starts==null||wanted==null||wanted.isEmpty())return spans;
        int j=0;for(int i=0;i<starts.length;i++){
            int end=starts[i]+wanted.length();j=Math.max(i,j);
            while(j<starts.length&&starts[j]+elements.get(j).length()<end)j++;
            if(j<starts.length&&starts[j]+elements.get(j).length()==end&&line.startsWith(wanted,starts[i])&&completeElement(line,starts[i],end))spans.add(new int[]{i,j});
        }
        return spans;
    }
    private static boolean wordPart(int cp){int type=Character.getType(cp);return Character.isLetterOrDigit(cp)||type==Character.NON_SPACING_MARK||type==Character.COMBINING_SPACING_MARK||type==Character.ENCLOSING_MARK||type==Character.CONNECTOR_PUNCTUATION||type==Character.FORMAT;}
    /** 독립 단어만 허용합니다. AdobePro/Adobe15/밑줄/결합문자 안의 일부는 제외합니다. */
    public static boolean completeElement(String line,int from,int to){
        if(line==null||from<0||to<=from||to>line.length())return false;
        if(from>0&&wordPart(line.codePointAt(from))&&wordPart(line.codePointBefore(from)))return false;
        return to>=line.length()||!wordPart(line.codePointBefore(to))||!wordPart(line.codePointAt(to));
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
