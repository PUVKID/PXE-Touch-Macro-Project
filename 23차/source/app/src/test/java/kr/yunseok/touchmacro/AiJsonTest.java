package kr.yunseok.touchmacro;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class AiJsonTest {
    @Test public void deepCopyPreservesNumberTypesAndDoesNotShareMutableValues(){
        JSONObject source=AiJson.object("integer",1,"floating",1.0,"nested",new JSONArray().put(AiJson.object("value","원문")),"empty",null);
        JSONObject copied=AiJson.copy(source);assertTrue(copied.opt("integer") instanceof Integer);assertTrue(copied.opt("floating") instanceof Double);assertSame(JSONObject.NULL,copied.opt("empty"));AiJson.obj(AiJson.array(copied.opt("nested")).opt(0)).put("value","수정");assertEquals("원문",AiJson.obj(AiJson.array(source.opt("nested")).opt(0)).opt("value"));
    }
    @Test public void cyclicAndExcessivelyDeepSnapshotsAreRejected(){
        JSONObject cyclic=new JSONObject();cyclic.put("cycle",cyclic);try{AiJson.copy(cyclic);fail();}catch(IllegalArgumentException expected){}
        JSONObject root=new JSONObject(),current=root;for(int i=0;i<70;i++){JSONObject next=new JSONObject();current.put("nested",next);current=next;}try{AiJson.copy(root);fail();}catch(IllegalArgumentException expected){}
    }
    @Test public void pythonWhitespaceSetHasExactlyTwentyNineCodepointsWithoutTrimmingData(){
        int count=0;for(int cp=0;cp<=0x3000;cp++)if(AiTokenizer.pythonWhitespace(cp)){count++;assertTrue(AiTokenizer.blank(new String(Character.toChars(cp))));}assertEquals(29,count);assertFalse(AiTokenizer.blank("\u200B"));String original=" \u0085입력\u202F ";assertEquals(original,AiJson.text(original));
    }
}
