package kr.yunseok.touchmacro;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.util.HashSet;

/** Android의 기본 JSON 구현이 덮어쓸 수 있는 중복 등록 키를 파싱 전에 거절합니다. */
final class AiContextJson {
    private AiContextJson(){}
    static JSONObject parse(String text){try{JSONTokener input=new JSONTokener(text);Object value=read(input);if(input.nextClean()!=0||!(value instanceof JSONObject))throw AiJson.error("문맥 객체 필요");return (JSONObject)value;}catch(IllegalArgumentException error){throw error;}catch(Exception error){throw AiJson.error("잘못된 문맥 JSON");}}
    private static Object read(JSONTokener input)throws Exception{
        char first=input.nextClean();
        if(first=='{'){JSONObject object=new JSONObject();HashSet<String> names=new HashSet<>();char next=input.nextClean();if(next=='}')return object;input.back();for(;;){if(input.nextClean()!='"')throw AiJson.error("JSON 키에는 큰따옴표 필요");String name=quoted(input);if(!names.add(name))throw AiJson.error("문맥 JSON의 중복 키");if(input.nextClean()!=':')throw AiJson.error("JSON 구분자");object.put(name,read(input));char separator=input.nextClean();if(separator=='}')return object;if(separator!=',')throw AiJson.error("JSON 객체 구분자");}}
        if(first=='['){JSONArray array=new JSONArray();char next=input.nextClean();if(next==']')return array;input.back();for(;;){array.put(read(input));char separator=input.nextClean();if(separator==']')return array;if(separator!=',')throw AiJson.error("JSON 배열 구분자");}}
        if(first=='"')return quoted(input);
        if(first==0)throw AiJson.error("잘린 JSON");input.back();Object scalar=input.nextValue();if(scalar instanceof JSONObject||scalar instanceof JSONArray||scalar instanceof String)throw AiJson.error("JSON 값 형식");return scalar;
    }
    private static String quoted(JSONTokener input)throws Exception{
        StringBuilder text=new StringBuilder();for(;;){char value=input.next();if(value=='"')return text.toString();if(value<32)throw AiJson.error("JSON 문자열의 제어문자 또는 잘림");if(value!='\\'){text.append(value);continue;}char escape=input.next();switch(escape){case '"':case '\\':case '/':text.append(escape);break;case 'b':text.append('\b');break;case 'f':text.append('\f');break;case 'n':text.append('\n');break;case 'r':text.append('\r');break;case 't':text.append('\t');break;case 'u':int cp=0;for(int i=0;i<4;i++){char h=input.next();int digit=h>='0'&&h<='9'?h-'0':h>='a'&&h<='f'?h-'a'+10:h>='A'&&h<='F'?h-'A'+10:-1;if(digit<0)throw AiJson.error("JSON unicode escape");cp=cp*16+digit;}text.append((char)cp);break;default:throw AiJson.error("JSON escape");}}
    }
}
