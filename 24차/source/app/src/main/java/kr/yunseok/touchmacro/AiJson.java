package kr.yunseok.touchmacro;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

/** AI 입력에서는 JSONObject의 문자열·숫자 자동 보정을 사용하지 않습니다. */
final class AiJson {
    private AiJson() { }
    static JSONObject object(Object... pairs) {
        try {JSONObject value=new JSONObject();for(int i=0;i<pairs.length;i+=2)value.put((String)pairs[i],pairs[i+1]==null?JSONObject.NULL:pairs[i+1]);return value;}
        catch(Exception error){throw new IllegalArgumentException("계획 JSON 생성 실패",error);}
    }
    static JSONObject copy(JSONObject value){return (JSONObject)copyValue(value,0,new java.util.IdentityHashMap<>());}
    private static Object copyValue(Object value,int depth,java.util.IdentityHashMap<Object,Boolean> visiting){
        if(depth>64)throw error("JSON 중첩 상한");
        if(nil(value))return JSONObject.NULL;
        if(value instanceof String){AiTokenizer.validUnicode((String)value);return value;}
        if(value instanceof Boolean)return value;
        if(value instanceof Byte||value instanceof Short||value instanceof Integer||value instanceof Long||value instanceof java.math.BigInteger||value instanceof java.math.BigDecimal)return value;
        if(value instanceof Double||value instanceof Float){if(!Double.isFinite(((Number)value).doubleValue()))throw error("비유한 JSON 수치");return value;}
        if(!(value instanceof JSONObject)&&!(value instanceof JSONArray))throw error("지원하지 않는 JSON 값");
        if(visiting.put(value,Boolean.TRUE)!=null)throw error("순환 JSON 참조");
        try{
            if(value instanceof JSONObject){JSONObject source=(JSONObject)value,target=new JSONObject();Iterator<String> names=source.keys();while(names.hasNext()){String key=names.next();AiTokenizer.validUnicode(key);target.put(key,copyValue(source.opt(key),depth+1,visiting));}return target;}
            JSONArray source=(JSONArray)value,target=new JSONArray();for(int i=0;i<source.length();i++)target.put(copyValue(source.opt(i),depth+1,visiting));return target;
        }catch(IllegalArgumentException exception){throw exception;}
        catch(Exception exception){throw new IllegalArgumentException("계획 JSON 복사 실패",exception);}
        finally{visiting.remove(value);}
    }
    static void keys(JSONObject value,String... expected) {
        HashSet<String> actual=new HashSet<>();Iterator<String> names=value.keys();while(names.hasNext())actual.add(names.next());
        if(!actual.equals(new HashSet<>(Arrays.asList(expected))))throw error("필수 필드 또는 알 수 없는 필드");
    }
    static JSONObject obj(Object value){if(!(value instanceof JSONObject))throw error("객체 필요");return (JSONObject)value;}
    static JSONArray array(Object value){if(!(value instanceof JSONArray))throw error("목록 필요");return (JSONArray)value;}
    static String string(Object value){if(!(value instanceof String))throw error("문자열 필요");return (String)value;}
    static String text(Object value){String text=string(value);int length=text.codePointCount(0,text.length());if(AiTokenizer.blank(text)||length>1024)throw error("문구 길이");AiTokenizer.validUnicode(text);return text;}
    static int integer(Object value,int lo,int hi) {
        if(!(value instanceof Byte||value instanceof Short||value instanceof Integer||value instanceof Long))throw error("정수 필요");
        long number=((Number)value).longValue();if(number<lo||number>hi)throw error("정수 범위");return (int)number;
    }
    static boolean bool(Object value){if(!(value instanceof Boolean))throw error("논리값 필요");return (Boolean)value;}
    static double coordinate(Object value){if(!(value instanceof Number))throw error("좌표 수치 필요");double number=((Number)value).doubleValue();if(!Double.isFinite(number)||number<0||number>100)throw error("좌표 범위");return number;}
    static boolean nil(Object value){return value==null||value==JSONObject.NULL;}
    static IllegalArgumentException error(String detail){return new IllegalArgumentException("AI 계획을 실행하지 않았습니다 · "+detail);}
    static String canonical(Object value){
        if(nil(value))return "null";
        if(value instanceof JSONObject){JSONObject object=(JSONObject)value;java.util.ArrayList<String> names=new java.util.ArrayList<>();Iterator<String> iterator=object.keys();while(iterator.hasNext())names.add(iterator.next());names.sort(AiTokenizer.CODEPOINT_ORDER);StringBuilder out=new StringBuilder("{");for(int i=0;i<names.size();i++){if(i>0)out.append(',');String name=names.get(i);out.append(AiTokenizer.quote(name)).append(':').append(canonical(object.opt(name)));}return out.append('}').toString();}
        if(value instanceof JSONArray){JSONArray array=(JSONArray)value;StringBuilder out=new StringBuilder("[");for(int i=0;i<array.length();i++){if(i>0)out.append(',');out.append(canonical(array.opt(i)));}return out.append(']').toString();}
        if(value instanceof String)return AiTokenizer.quote((String)value);
        if(value instanceof Boolean||value instanceof Number)return value.toString();
        throw error("지원하지 않는 문맥 값");
    }
}
