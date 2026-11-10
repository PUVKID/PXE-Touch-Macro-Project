package kr.yunseok.touchmacro;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import static org.junit.Assert.*;

/** Python과 공유한 불변24개를 그대로 재생하며 정답을 다시 만들지 않습니다. */
@RunWith(Parameterized.class) public class AiLiteralCodecTest {
    static JSONObject vectors;
    final JSONObject row;
    public AiLiteralCodecTest(String name,JSONObject row){this.row=row;}
    @Parameterized.Parameters(name="{0}") public static Collection<Object[]> cases()throws Exception{
        byte[] bytes;try(InputStream in=AiLiteralCodecTest.class.getResourceAsStream("/literal_codec_vectors.json")){assertNotNull(in);bytes=in.readAllBytes();}
        StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
        assertEquals("2db51fdc6e4377bc37fbfa7a95a9a2a5052e71c11cd4bb6b4d2041c106b1f69c",hash.toString());
        vectors=new JSONObject(new String(bytes,StandardCharsets.UTF_8));ArrayList<Object[]> result=new ArrayList<>();JSONArray rows=vectors.getJSONArray("cases");int accepted=0;
        for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);if(row.getBoolean("accepted"))accepted++;result.add(new Object[]{row.getString("id"),row});}
        assertEquals(24,result.size());assertEquals(14,accepted);return result;
    }
    static JSONObject specification(){return AiJson.object("format","pxe-literal-slot-utf8-v1","base_format","pxe-codepoint-utf8-v1","base_tokenizer_sha256",vectors.getString("base_tokenizer_sha256"),"characters",vectors.getJSONArray("characters"),"special_tokens",vectors.getJSONArray("special_tokens"),"special_ids",vectors.getJSONObject("special_ids"),"normalization","none","offset_unit","unicode-codepoint-half-open","registry_kinds",vectors.getJSONArray("registry_kinds"),"max_input",1024,"copy_contract","literal-anchor-or-contiguous-unquoted-v1");}
    @Test public void sharedImmutableContract()throws Exception{
        AiTokenizer tokenizer=new AiTokenizer(specification());assertEquals(540,tokenizer.size());
        StringBuilder command=new StringBuilder();JSONArray cps=row.getJSONArray("command_codepoints");for(int i=0;i<cps.length();i++)command.appendCodePoint(cps.getInt(i));
        if(!row.getBoolean("accepted")){
            boolean rejected=false;try{JSONObject context=row.has("context_json")?(JSONObject)AiTokenizer.class.getMethod("parseContextJson",String.class).invoke(null,row.getString("context_json")):row.getJSONObject("context");tokenizer.encodeRequest(command.toString(),context);}catch(Exception expected){Throwable cause=expected instanceof java.lang.reflect.InvocationTargetException?expected.getCause():expected;assertTrue("입력 계약 거절이어야 합니다: "+cause,cause instanceof IllegalArgumentException||cause instanceof org.json.JSONException);rejected=true;}assertTrue(row.getString("id"),rejected);return;
        }
        AiTokenizer.Encoded encoded=tokenizer.encodeRequest(command.toString(),row.getJSONObject("context"));
        JSONObject actual=(JSONObject)encoded.getClass().getMethod("contractJson").invoke(encoded);
        assertEquals(row.getString("id"),AiJson.canonical(row.getJSONObject("expected")),AiJson.canonical(actual));
        assertEquals(command.toString(),encoded.getClass().getMethod("reconstructCommand").invoke(encoded));
        JSONArray valid=row.getJSONArray("valid_pairs");for(int i=0;i<valid.length();i++){JSONObject item=valid.getJSONObject(i);JSONArray pair=item.getJSONArray("pair"),span=item.getJSONArray("cp_span");assertArrayEquals(new int[]{span.getInt(0),span.getInt(1)},encoded.tokensToSpan(pair.getInt(0),pair.getInt(1)));assertArrayEquals(new int[]{pair.getInt(0),pair.getInt(1)},encoded.spanToTokens(span.getInt(0),span.getInt(1)));assertEquals(item.getString("raw_text"),encoded.copy(pair.getInt(0),pair.getInt(1)));}
        JSONArray invalid=row.getJSONArray("invalid_pairs");for(int i=0;i<invalid.length();i++){JSONArray pair=invalid.getJSONArray(i);boolean rejected=false;try{encoded.tokensToSpan(AiJson.integer(pair.opt(0),0,Integer.MAX_VALUE),AiJson.integer(pair.opt(1),0,Integer.MAX_VALUE));}catch(IllegalArgumentException expected){rejected=true;}assertTrue("잘못된 COPY pair "+pair,rejected);}
    }
}
