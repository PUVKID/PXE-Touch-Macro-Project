package kr.yunseok.touchmacro;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** 인용 원문은 원문 표에만 두고 신경망에는 어휘 경계와 동일값 관계를 전달합니다. */
final class AiLiteralCodec {
    static final String FORMAT="pxe-literal-slot-utf8-v1";
    static final String[] KINDS={"points","gestures","regions","macros","apps","actions"};
    static final String[] SPECIAL={"LIT","ENTITY","UNMENTIONED","REC_END","INVENTORY"};
    final AiTokenizer base;
    final String baseHash;
    AiLiteralCodec(JSONObject spec){
        AiJson.keys(spec,"format","base_format","base_tokenizer_sha256","characters","special_tokens","special_ids","normalization","offset_unit","registry_kinds","max_input","copy_contract");
        if(!FORMAT.equals(spec.opt("format"))||!"pxe-codepoint-utf8-v1".equals(spec.opt("base_format"))||!"none".equals(spec.opt("normalization"))||!"unicode-codepoint-half-open".equals(spec.opt("offset_unit"))||AiJson.integer(spec.opt("max_input"),1,4096)!=1024||!"literal-anchor-or-contiguous-unquoted-v1".equals(spec.opt("copy_contract")))throw AiJson.error("리터럴 codec 계약 불일치");
        baseHash=AiJson.string(spec.opt("base_tokenizer_sha256"));if(!baseHash.matches("[0-9a-f]{64}"))throw AiJson.error("기준 tokenizer 해시 형식");
        base=new AiTokenizer(AiJson.object("format","pxe-codepoint-utf8-v1","normalization","none","offset_unit","unicode-codepoint-half-open","characters",AiJson.array(spec.opt("characters"))));
        if(base.size()!=535)throw AiJson.error("고정 기준 어휘535 필요");
        JSONArray special=AiJson.array(spec.opt("special_tokens")),kinds=AiJson.array(spec.opt("registry_kinds"));JSONObject ids=AiJson.obj(spec.opt("special_ids"));AiJson.keys(ids,SPECIAL);
        if(special.length()!=SPECIAL.length||kinds.length()!=KINDS.length)throw AiJson.error("리터럴 어휘·등록 유형 크기");
        for(int i=0;i<SPECIAL.length;i++)if(!SPECIAL[i].equals(special.opt(i))||AiJson.integer(ids.opt(SPECIAL[i]),0,10000)!=535+i)throw AiJson.error("리터럴 특수 토큰 순서");
        for(int i=0;i<KINDS.length;i++)if(!KINDS[i].equals(kinds.opt(i)))throw AiJson.error("등록 유형 순서");
    }
    private static final class Quote {final int start,end;final String text;Quote(int a,int b,String text){start=a;end=b;this.text=text;}}
    private static ArrayList<Quote> quotes(String command){
        int[] cps=command.codePoints().toArray();ArrayList<Quote> result=new ArrayList<>();int opening=-1;boolean escaped=false;
        for(int i=0;i<cps.length;i++){int cp=cps[i];if(opening<0&&"“”«»「」『』".indexOf(cp)>=0)throw AiJson.error("ASCII 큰따옴표 외 인용 구분자");if(cp=='"'&&!escaped){if(opening<0)opening=i+1;else{String text=AiTokenizer.substring(command,opening,i);if(AiTokenizer.blank(text))throw AiJson.error("빈 인용 또는 공백 인용");result.add(new Quote(opening,i,text));opening=-1;}}escaped=cp=='\\'&&!escaped;}
        if(opening>=0)throw AiJson.error("닫히지 않은 인용");return result;
    }
    AiTokenizer.Encoded encodeRequest(String command,JSONObject context){
        AiTokenizer.validUnicode(command);if(AiTokenizer.blank(command))throw AiJson.error("명령 원문 필요");
        Object mode=context.opt("planning_mode");if(!AiJson.nil(mode)&&(!(mode instanceof String)||!mode.equals("MACRO")&&!mode.equals("WATCH")))throw AiJson.error("지원하지 않는 계획 모드");
        LinkedHashMap<String,char[]> names=new LinkedHashMap<>();
        for(int k=0;k<KINDS.length;k++){JSONObject registry=context.has(KINDS[k])?AiJson.obj(context.opt(KINDS[k])):new JSONObject();Iterator<String> keys=registry.keys();while(keys.hasNext()){String name=keys.next();AiTokenizer.validUnicode(name);if(AiTokenizer.blank(name))throw AiJson.error("등록 이름 형식");char[] mask=names.get(name);if(mask==null){mask="000000".toCharArray();names.put(name,mask);}mask[k]='1';}}
        AiTokenizer.Encoded original=base.encodeRequest(command,context);
        ArrayList<Quote> quotes=quotes(command);LinkedHashMap<String,Integer> entityIds=new LinkedHashMap<>();for(Quote quote:quotes)if(!entityIds.containsKey(quote.text))entityIds.put(quote.text,entityIds.size());
        Data data=new Data(base,command,original.ids.length,AiJson.nil(mode)?JSONObject.NULL:mode);
        for(Map.Entry<String,Integer> entry:entityIds.entrySet())data.entities.put(AiJson.object("id",entry.getValue(),"membership",names.containsKey(entry.getKey())?new String(names.get(entry.getKey())):"000000"));
        TreeMap<String,Integer> unused=new TreeMap<>();for(Map.Entry<String,char[]> entry:names.entrySet())if(!entityIds.containsKey(entry.getKey())){String mask=new String(entry.getValue());unused.put(mask,unused.getOrDefault(mask,0)+1);}
        for(Map.Entry<String,Integer> group:unused.entrySet())data.groups.put(AiJson.object("membership",group.getKey(),"count",group.getValue()));
        int cursor=0;
        for(int occurrence=0;occurrence<quotes.size();occurrence++){Quote quote=quotes.get(occurrence);data.raw(cursor,quote.start-1,occurrence);int anchor=data.ids.size();data.add(535,quote.start,quote.end,"literal",-occurrence-1);data.ascii(Integer.toString(entityIds.get(quote.text)));data.metadata(538);data.literals.put(AiJson.object("occurrence",occurrence,"entity",entityIds.get(quote.text),"anchor",anchor,"start",quote.start,"end",quote.end,"quote_start",quote.start-1,"quote_end",quote.end+1));data.units.put(AiJson.object("kind","literal","anchor",anchor,"quote_start",quote.start-1,"quote_end",quote.end+1));cursor=quote.end+1;}
        data.raw(cursor,command.codePointCount(0,command.length()),quotes.size());data.commandTokens=data.ids.size();data.metadata(AiTokenizer.SEP);data.metadata(539);
        for(int i=0;i<data.entities.length();i++){JSONObject entity=AiJson.obj(data.entities.opt(i));data.metadata(536);data.ascii(entity.opt("id")+":"+entity.opt("membership"));data.metadata(538);}
        for(int i=0;i<data.groups.length();i++){JSONObject group=AiJson.obj(data.groups.opt(i));data.metadata(537);data.ascii(group.opt("membership")+":"+group.opt("count"));data.metadata(538);}
        for(long token:base.encode("{\"planning_mode\":"+(AiJson.nil(mode)?"null":AiTokenizer.quote((String)mode))+"}").ids)data.metadata(token);
        if(data.ids.size()>1024)throw AiJson.error("새 입력1024 한도 초과. 슬롯·문맥 절단 금지");
        data.finish();return new AiTokenizer.Encoded(data.tokenIds,data.starts,data.ends,command,data.commandTokens,data);
    }
    static final class Data {
        final AiTokenizer base;final String source;final int originalTokens;final Object mode;
        final ArrayList<Long> ids=new ArrayList<>();final ArrayList<Integer> a=new ArrayList<>(),b=new ArrayList<>(),region=new ArrayList<>();final ArrayList<String> role=new ArrayList<>();
        final JSONArray literals=new JSONArray(),entities=new JSONArray(),groups=new JSONArray(),units=new JSONArray();
        long[] tokenIds;int[] starts,ends;boolean[] startKeep,endKeep;int commandTokens;
        Data(AiTokenizer base,String source,int original,Object mode){this.base=base;this.source=source;originalTokens=original;this.mode=mode;}
        void add(long id,int start,int end,String role,int region){ids.add(id);a.add(start);b.add(end);this.role.add(role);this.region.add(region);}
        void metadata(long id){add(id,-1,-1,"metadata",0);}
        void ascii(String text){for(int cp:text.codePoints().toArray()){if(cp>127)throw AiJson.error("기계 metadata는 ASCII만 허용");metadata(AiTokenizer.BYTE_BASE+cp);}}
        void raw(int start,int end,int region){if(start==end)return;AiTokenizer.Encoded encoded=base.encode(AiTokenizer.substring(source,start,end));int begin=ids.size();for(int i=0;i<encoded.ids.length;i++)add(encoded.ids[i],encoded.starts[i]+start,encoded.ends[i]+start,"raw",region);units.put(AiJson.object("kind","raw","begin",begin,"end",ids.size()));}
        void finish(){int n=ids.size();tokenIds=new long[n];starts=new int[n];ends=new int[n];startKeep=new boolean[n];endKeep=new boolean[n];for(int i=0;i<n;i++){tokenIds[i]=ids.get(i);starts[i]=a.get(i);ends[i]=b.get(i);boolean literal=role.get(i).equals("literal"),raw=role.get(i).equals("raw");startKeep[i]=literal||raw&&(i==0||!sameOffset(i-1,i));endKeep[i]=literal||raw&&(i+1==n||!sameOffset(i,i+1));}}
        boolean sameOffset(int x,int y){return a.get(x).equals(a.get(y))&&b.get(x).equals(b.get(y));}
        boolean pair(int start,int end){if(start<0||end<start||end>=ids.size()||!startKeep[start]||!endKeep[end])return false;if(role.get(start).equals("literal"))return start==end;for(int i=start;i<=end;i++)if(!role.get(i).equals("raw")||!region.get(i).equals(region.get(start)))return false;return true;}
        JSONObject json(){JSONArray id=new JSONArray(),offsets=new JSONArray(),roles=new JSONArray(),regions=new JSONArray(),start=new JSONArray(),end=new JSONArray();for(int i=0;i<ids.size();i++){id.put(ids.get(i));offsets.put(a.get(i)<0?JSONObject.NULL:new JSONArray().put(a.get(i)).put(b.get(i)));roles.put(role.get(i));regions.put(role.get(i).equals("metadata")?JSONObject.NULL:region.get(i));start.put(startKeep[i]);end.put(endKeep[i]);}return AiJson.object("ids",id,"offsets",offsets,"roles",roles,"regions",regions,"literals",literals,"entities",entities,"unmentioned_groups",groups,"command_tokens",commandTokens,"start_keep",start,"end_keep",end,"planning_mode",mode,"original_input_tokens",originalTokens);}
        String reconstruct(){StringBuilder result=new StringBuilder();for(int i=0;i<units.length();i++){JSONObject unit=AiJson.obj(units.opt(i));if(unit.opt("kind").equals("raw"))result.append(base.decode(Arrays.copyOfRange(tokenIds,(Integer)unit.opt("begin"),(Integer)unit.opt("end"))));else result.append(AiTokenizer.substring(source,(Integer)unit.opt("quote_start"),(Integer)unit.opt("quote_end")));}return result.toString();}
    }
}
