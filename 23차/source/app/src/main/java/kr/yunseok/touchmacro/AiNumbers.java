package kr.yunseok.touchmacro;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/** 모델이 선택한 원문 수치만 변환합니다. 명령의 뜻은 판단하지 않습니다. */
public final class AiNumbers {
    private AiNumbers() { }
    private static final String DIGITS="일이삼사오육칠팔구";
    private static final Map<String,Integer> ONES=new LinkedHashMap<>(), TENS=new LinkedHashMap<>();
    static {
        String[] words={"하나","한","둘","두","셋","세","넷","네","다섯","여섯","일곱","여덟","아홉"};
        int[] values={1,1,2,2,3,3,4,4,5,6,7,8,9};
        for(int i=0;i<words.length;i++)ONES.put(words[i],values[i]);
        String[] tens={"열","스물","서른","마흔","쉰","예순","일흔","여든","아흔"};
        for(int i=0;i<tens.length;i++)TENS.put(tens[i],10*(i+1));
    }
    private static String raw(String text) {
        if(text==null||text.codePointCount(0,text.length())<1||text.codePointCount(0,text.length())>48)throw bad();
        for(int cp:text.codePoints().toArray())if(cp!=' '&&AiTokenizer.pythonWhitespace(cp))throw bad();
        int first=0,last=text.length();while(first<last&&text.charAt(first)==' ')first++;while(last>first&&text.charAt(last-1)==' ')last--;
        return text.substring(first,last);
    }
    public static int integer(String text) {
        String value=raw(text);if(value.isEmpty())throw bad();long result;
        if(value.matches("[0-9]{1,9}"))result=Long.parseLong(value);
        else {
            value=value.replace(" ","");Integer nativeValue=ONES.get(value);
            if(nativeValue!=null)result=nativeValue;
            else if(value.equals("스무"))result=20;
            else {
                Long found=null;
                for(Map.Entry<String,Integer> entry:TENS.entrySet()) {
                    if(value.equals(entry.getKey())) { found=(long)entry.getValue();break; }
                    if(value.startsWith(entry.getKey())) {Integer tail=ONES.get(value.substring(entry.getKey().length()));if(tail!=null){found=(long)entry.getValue()+tail;break;}}
                }
                result=found==null?sino(value):found;
            }
        }
        if(result<0||result>100000000)throw bad();return (int)result;
    }
    private static long small(String text) {
        long total=0;int pending=0,previous=10000;
        for(char c:text.toCharArray()) {
            int digit=DIGITS.indexOf(c)+1;
            if(digit>0){if(pending!=0)throw bad();pending=digit;}
            else {int unit=c=='십'?10:c=='백'?100:c=='천'?1000:0;if(unit==0||unit>=previous)throw bad();total+=(pending==0?1:pending)*unit;pending=0;previous=unit;}
        }
        return total+pending;
    }
    private static long sino(String text) {
        if(text.equals("영")||text.equals("공"))return 0;
        long total=0;int index=text.indexOf('억');
        if(index>=0){if(index!=text.lastIndexOf('억'))throw bad();String head=text.substring(0,index);total=(head.isEmpty()?1:small(head))*100000000L;text=text.substring(index+1);}
        index=text.indexOf('만');
        if(index>=0){if(index!=text.lastIndexOf('만'))throw bad();String head=text.substring(0,index);total+=(head.isEmpty()?1:small(head))*10000L+small(text.substring(index+1));}
        else total+=small(text);
        return total;
    }
    public static long duration(String text,String unit) {
        long multiplier;
        switch(unit==null?"":unit){case "MS":multiplier=1;break;case "SEC":multiplier=1000;break;case "MIN":multiplier=60000;break;case "HOUR":multiplier=3600000;break;default:throw bad();}
        String value=raw(text);long result;
        try {result=value.matches("[0-9]{1,9}\\.[0-9]{1,9}")?new BigDecimal(value).multiply(BigDecimal.valueOf(multiplier)).longValueExact():Math.multiplyExact((long)integer(value),multiplier);}
        catch(ArithmeticException error){throw bad();}
        if(result<0||result>86400000)throw bad();return result;
    }
    private static IllegalArgumentException bad(){return new IllegalArgumentException("숫자 원문·단위 또는 정확한 정수 ms 범위를 확인하세요");}
}
