package kr.yunseok.touchmacro;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

/** Python과 같은 원문 codepoint 구간과 UTF-8 byte fallback을 사용합니다. */
public final class AiTokenizer {
    public static final int PAD=0,BOS=1,EOS=2,SEP=3,BYTE_BASE=4,CHAR_BASE=260;
    private final String[] characters;
    final AiLiteralCodec literal;
    private final HashMap<Integer,Integer> ids=new HashMap<>();
    public AiTokenizer(JSONObject specification) {
        if(AiLiteralCodec.FORMAT.equals(specification.opt("format"))){literal=new AiLiteralCodec(specification);characters=new String[0];return;}
        literal=null;
        if(!"pxe-codepoint-utf8-v1".equals(specification.opt("format"))||!"none".equals(specification.opt("normalization"))||!"unicode-codepoint-half-open".equals(specification.opt("offset_unit")))throw AiJson.error("토크나이저 계약 불일치");
        JSONArray values=AiJson.array(specification.opt("characters"));characters=new String[values.length()];
        for(int i=0;i<values.length();i++) {String c=AiJson.string(values.opt(i));validUnicode(c);if(c.codePointCount(0,c.length())!=1||ids.put(c.codePointAt(0),i+CHAR_BASE)!=null)throw AiJson.error("중복 또는 잘못된 문자 어휘");characters[i]=c;}
    }
    public int size(){return literal==null?CHAR_BASE+characters.length:540;}
    JSONArray characters(){return literal==null?new JSONArray(Arrays.asList(characters)):literal.base.characters();}
    public boolean literalCodec(){return literal!=null;}
    public static JSONObject parseContextJson(String text){return AiContextJson.parse(text);}
    public static void validUnicode(String value) {
        for(int i=0;i<value.length();i++){char c=value.charAt(i);if(Character.isHighSurrogate(c)){if(i+1>=value.length()||!Character.isLowSurrogate(value.charAt(++i)))throw AiJson.error("잘못된 유니코드 원문");}else if(Character.isLowSurrogate(c))throw AiJson.error("잘못된 유니코드 원문");}
    }
    public static boolean pythonWhitespace(int cp){return cp>=9&&cp<=13||cp>=0x1c&&cp<=0x20||cp==0x85||cp==0xa0||cp==0x1680||cp>=0x2000&&cp<=0x200a||cp==0x2028||cp==0x2029||cp==0x202f||cp==0x205f||cp==0x3000;}
    public static boolean blank(String value){for(int cp:value.codePoints().toArray())if(!pythonWhitespace(cp))return false;return true;}
    public static final class Encoded {
        public final long[] ids;
        public final int[] starts,ends;
        public final String command;
        public final int commandTokens;
        public final boolean[] startKeep,endKeep,copyKeep;
        private final AiLiteralCodec.Data literal;
        Encoded(long[] ids,int[] starts,int[] ends,String command,int commandTokens){this(ids,starts,ends,command,commandTokens,null);}
        Encoded(long[] ids,int[] starts,int[] ends,String command,int commandTokens,AiLiteralCodec.Data literal){this.ids=ids;this.starts=starts;this.ends=ends;this.command=command;this.commandTokens=commandTokens;this.literal=literal;startKeep=new boolean[ids.length];endKeep=new boolean[ids.length];copyKeep=new boolean[ids.length];for(int i=0;i<ids.length;i++){startKeep[i]=literal==null?i<commandTokens&&(i==0||starts[i-1]!=starts[i]):literal.startKeep[i];endKeep[i]=literal==null?i<commandTokens&&(i+1==commandTokens||ends[i+1]!=ends[i]):literal.endKeep[i];copyKeep[i]=literal==null?i<commandTokens:startKeep[i]||endKeep[i];}}
        public boolean[] endMask(int start){if(start<0||start>=ids.length||!startKeep[start])throw AiJson.error("유효하지 않은 COPY 시작");boolean[] result=new boolean[ids.length];for(int end=start;end<ids.length;end++)result[end]=literal==null?endKeep[end]:literal.pair(start,end);return result;}
        public JSONObject contractJson(){if(literal==null)throw AiJson.error("리터럴 codec 입력이 아닙니다");return literal.json();}
        public String reconstructCommand(){return literal==null?command:literal.reconstruct();}
        public int[] spanToTokens(int start,int end){
            if(start<0||end<=start||end>command.codePointCount(0,command.length()))throw AiJson.error("원문 COPY 범위 오류");
            if(literal!=null)for(int i=0;i<literal.literals.length();i++){JSONObject quoted=AiJson.obj(literal.literals.opt(i));if((Integer)quoted.opt("start")==start&&(Integer)quoted.opt("end")==end){int anchor=(Integer)quoted.opt("anchor");return new int[]{anchor,anchor};}}
            int first=-1,last=-1;for(int i=0;i<ids.length;i++)if(starts[i]>=start&&ends[i]<=end&&starts[i]>=0&&(literal==null||literal.role.get(i).equals("raw"))){if(first<0)first=i;last=i;}
            if(first>=0){int[] span=tokensToSpan(first,last);if(span[0]==start&&span[1]==end)return new int[]{first,last};}throw AiJson.error("인용 일부·구분자·문맥 또는 불일치 COPY");
        }
        public int[] tokensToSpan(int start,int end){
            if(literal!=null){if(!literal.pair(start,end))throw AiJson.error("COPY 전체 구간·UTF8·리터럴 경계 오류");return new int[]{starts[start],ends[end]};}
            if(start<0||end<start||end>=commandTokens||starts[start]<0||ends[end]<0||start>0&&starts[start-1]==starts[start]||end+1<commandTokens&&ends[end+1]==ends[end])throw AiJson.error("UTF-8 문자 중간 또는 문맥 복사 금지");
            return new int[]{starts[start],ends[end]};
        }
        public String copy(int start,int end){int[] span=tokensToSpan(start,end);return substring(command,span[0],span[1]);}
    }
    public Encoded encode(String value){
        if(literal!=null)return literal.base.encode(value);
        validUnicode(value);ArrayList<Long> encoded=new ArrayList<>();ArrayList<Integer> starts=new ArrayList<>(),ends=new ArrayList<>();int position=0;
        for(int offset=0;offset<value.length();position++) {int cp=value.codePointAt(offset);offset+=Character.charCount(cp);Integer id=ids.get(cp);
            if(id!=null){encoded.add((long)id);starts.add(position);ends.add(position+1);}
            else for(byte b:new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8)){encoded.add((long)BYTE_BASE+(b&255));starts.add(position);ends.add(position+1);}
        }
        long[] result=new long[encoded.size()];int[] a=new int[result.length],b=new int[result.length];for(int i=0;i<result.length;i++){result[i]=encoded.get(i);a[i]=starts.get(i);b[i]=ends.get(i);}
        return new Encoded(result,a,b,value,result.length);
    }
    public Encoded encodeRequest(String command,JSONObject context){
        if(literal!=null)return literal.encodeRequest(command,context);
        if(command.codePointCount(0,command.length())>4096)throw AiJson.error("원문 길이 상한");
        Encoded input=encode(command),suffix=encode(inventory(context));int length=input.ids.length+1+suffix.ids.length;
        if(length>1024||blank(command))throw AiJson.error("명령·등록 문맥이 모델 입력 상한을 넘거나 비어 있습니다");
        long[] tokens=Arrays.copyOf(input.ids,length);tokens[input.ids.length]=SEP;System.arraycopy(suffix.ids,0,tokens,input.ids.length+1,suffix.ids.length);
        int[] starts=Arrays.copyOf(input.starts,length),ends=Arrays.copyOf(input.ends,length);Arrays.fill(starts,input.ids.length,length,-1);Arrays.fill(ends,input.ids.length,length,-1);
        return new Encoded(tokens,starts,ends,command,input.ids.length);
    }
    public String decode(long[] tokens) {
        if(literal!=null)return literal.base.decode(tokens);
        StringBuilder result=new StringBuilder();ByteArrayOutputStream pending=new ByteArrayOutputStream();
        for(long token:tokens) {
            if(token>=BYTE_BASE&&token<CHAR_BASE)pending.write((int)token-BYTE_BASE);
            else if(token>=CHAR_BASE&&token<size()){flush(result,pending);result.append(characters[(int)token-CHAR_BASE]);}
            else throw AiJson.error("원문에 특수·알 수 없는 토큰");
        }
        flush(result,pending);return result.toString();
    }
    private static void flush(StringBuilder result,ByteArrayOutputStream pending){
        if(pending.size()==0)return;
        try {result.append(StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(pending.toByteArray())));}
        catch(CharacterCodingException error){throw AiJson.error("잘못된 UTF-8 byte열");}pending.reset();
    }
    public static String substring(String source,int start,int end){
        validUnicode(source);int length=source.codePointCount(0,source.length());if(start<0||end<=start||end>length)throw AiJson.error("원문 복사 범위");return source.substring(source.offsetByCodePoints(0,start),source.offsetByCodePoints(0,end));
    }
    public static final Comparator<String> CODEPOINT_ORDER=(a,b)->{int x=0,y=0;while(x<a.length()&&y<b.length()){int ca=a.codePointAt(x),cb=b.codePointAt(y);if(ca!=cb)return Integer.compare(ca,cb);x+=Character.charCount(ca);y+=Character.charCount(cb);}return Integer.compare(a.length()-x,b.length()-y);};
    public static String inventory(JSONObject context){
        ArrayList<String> keys=new ArrayList<>();for(String key:new String[]{"points","gestures","regions","macros","apps","actions"})if(!context.has(key)||context.opt(key) instanceof JSONObject)keys.add(key);keys.add("planning_mode");keys.sort(CODEPOINT_ORDER);
        StringBuilder value=new StringBuilder("{");boolean first=true;
        for(String key:keys){if(!first)value.append(',');first=false;value.append(quote(key)).append(':');if(key.equals("planning_mode")){Object mode=context.opt(key);value.append(AiJson.nil(mode)?"null":quote(AiJson.string(mode)));continue;}
            JSONObject registered=context.has(key)?AiJson.obj(context.opt(key)):new JSONObject();ArrayList<String> names=new ArrayList<>();Iterator<String> iterator=registered.keys();while(iterator.hasNext()){String name=iterator.next();validUnicode(name);names.add(name);}names.sort(CODEPOINT_ORDER);value.append('[');for(int i=0;i<names.size();i++){if(i>0)value.append(',');value.append(quote(names.get(i)));}value.append(']');
        }
        return value.append('}').toString();
    }
    public static String quote(String text){
        validUnicode(text);StringBuilder result=new StringBuilder("\"");for(int cp:text.codePoints().toArray()){switch(cp){case '"':result.append("\\\"");break;case '\\':result.append("\\\\");break;case '\b':result.append("\\b");break;case '\t':result.append("\\t");break;case '\n':result.append("\\n");break;case '\f':result.append("\\f");break;case '\r':result.append("\\r");break;default:if(cp<32)result.append(String.format(java.util.Locale.ROOT,"\\u%04x",cp));else result.appendCodePoint(cp);}}return result.append('"').toString();
    }
}
