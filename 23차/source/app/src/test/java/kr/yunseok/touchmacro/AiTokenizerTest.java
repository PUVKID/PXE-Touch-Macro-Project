package kr.yunseok.touchmacro;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class AiTokenizerTest {
    private AiTokenizer tokenizer(){return new AiTokenizer(AiJson.object("format","pxe-codepoint-utf8-v1","normalization","none","offset_unit","unicode-codepoint-half-open","characters",new JSONArray().put("가").put(" ").put("😀")));}
    @Test public void codepointsAndUtf8FallbackPreserveAstralCombiningAndControls(){String text="가 😀𠀀é가\u0085\n\"\\";AiTokenizer t=tokenizer();AiTokenizer.Encoded e=t.encode(text);assertEquals(text,t.decode(e.ids));assertEquals("😀",AiTokenizer.substring(text,2,3));assertEquals(260,e.ids[0]);assertEquals(262,e.ids[2]);assertEquals(4+0xF0,e.ids[3]);assertEquals(3,e.starts[3]);assertEquals(3,e.starts[6]);try{e.tokensToSpan(4,6);fail();}catch(IllegalArgumentException expected){}assertArrayEquals(new int[]{3,4},e.tokensToSpan(3,6));}
    @Test public void pythonInventorySortAndJsonEscapingMatch(){JSONObject c=AiJson.object("points",AiJson.object("😀",1,"\uE000",1,"a\"\\\n\u0085\u2028/",1),"planning_mode","WATCH");String expected="{\"actions\":[],\"apps\":[],\"gestures\":[],\"macros\":[],\"planning_mode\":\"WATCH\",\"points\":[\"a\\\"\\\\\\n\u0085\u2028/\",\"\uE000\",\"😀\"],\"regions\":[]}";assertEquals(expected,AiTokenizer.inventory(c));AiTokenizer.Encoded e=tokenizer().encodeRequest("가",c);assertEquals(1,e.commandTokens);try{e.tokensToSpan(2,2);fail();}catch(IllegalArgumentException invalid){}}
    @Test public void invalidUnicodeBytesAndOverflowAreRejected(){AiTokenizer t=tokenizer();try{t.encode("\uD800");fail();}catch(IllegalArgumentException expected){}try{t.decode(new long[]{4+0xF0,4+0x80});fail();}catch(IllegalArgumentException expected){}try{t.encodeRequest("가".repeat(1024),new JSONObject());fail();}catch(IllegalArgumentException expected){}}
}
