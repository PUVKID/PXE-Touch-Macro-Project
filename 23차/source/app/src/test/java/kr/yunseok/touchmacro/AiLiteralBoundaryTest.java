package kr.yunseok.touchmacro;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static org.junit.Assert.*;

/** 추가 경계와 제어 logits만 검사하며 실제 모델 의미 예측으로 집계하지 않습니다. */
public class AiLiteralBoundaryTest {
    static AiTokenizer tokenizer;
    @BeforeClass public static void setup()throws Exception{AiLiteralCodecTest.cases();tokenizer=new AiTokenizer(AiLiteralCodecTest.specification());}
    static void rejected(Runnable operation){try{operation.run();fail("거절해야 합니다");}catch(IllegalArgumentException expected){}}
    @Test public void sameValueAndAllSixMembershipDoNotTransmitRawNames(){
        JSONObject context=new JSONObject();for(String kind:AiLiteralCodec.KINDS)try{context.put(kind,AiJson.object("비밀😀",new JSONObject()));}catch(Exception error){throw new AssertionError(error);}
        AiTokenizer.Encoded input=tokenizer.encodeRequest("\"비밀😀\" 다음 \"비밀😀\" 입력",context);JSONObject value=input.contractJson();JSONArray literals=value.getJSONArray("literals"),entities=value.getJSONArray("entities");assertEquals(2,literals.length());assertEquals(1,entities.length());assertEquals("111111",entities.getJSONObject(0).getString("membership"));assertEquals(0,literals.getJSONObject(1).getInt("entity"));assertEquals(0,value.getJSONArray("unmentioned_groups").length());
        AiTokenizer.Encoded renamed=tokenizer.encodeRequest("\"다른é\" 다음 \"다른é\" 입력",renameAll("다른é"));assertArrayEquals(input.ids,renamed.ids);
    }
    static JSONObject renameAll(String name){JSONObject context=new JSONObject();for(String kind:AiLiteralCodec.KINDS)try{context.put(kind,AiJson.object(name,new JSONObject()));}catch(Exception error){throw new AssertionError(error);}return context;}
    @Test public void unmentionedGroupsAreSortedMasksAndCountDistinctNames(){
        JSONObject context=AiJson.object("points",AiJson.object("공통",0,"여분",0),"actions",AiJson.object("공통",0),"gestures",AiJson.object("추가",0));JSONObject encoded=tokenizer.encodeRequest("홈으로",context).contractJson();JSONArray groups=encoded.getJSONArray("unmentioned_groups");assertEquals(3,groups.length());assertEquals("010000",groups.getJSONObject(0).getString("membership"));assertEquals("100000",groups.getJSONObject(1).getString("membership"));assertEquals("100001",groups.getJSONObject(2).getString("membership"));
    }
    @Test public void entireIntervalCannotCrossQuoteOrContext(){
        AiTokenizer.Encoded input=tokenizer.encodeRequest("12 \"가\" 34",new JSONObject());int anchor=input.contractJson().getJSONArray("literals").getJSONObject(0).getInt("anchor");assertTrue(input.startKeep[0]);assertFalse(input.endMask(0)[anchor]);rejected(()->input.tokensToSpan(0,anchor));rejected(()->input.tokensToSpan(anchor,anchor+1));rejected(()->input.tokensToSpan(input.commandTokens,input.commandTokens));boolean[] end=input.endMask(anchor);int count=0;for(boolean value:end)if(value)count++;assertEquals(1,count);assertTrue(end[anchor]);
    }
    @Test public void rawCpSpansCannotCopyQuoteDelimiterOrPartialLiteral(){AiTokenizer.Encoded input=tokenizer.encodeRequest("\"가나다\" 입력",new JSONObject());assertEquals("가나다",input.copy(input.spanToTokens(1,4)[0],input.spanToTokens(1,4)[1]));rejected(()->input.spanToTokens(1,2));rejected(()->input.spanToTokens(0,4));rejected(()->input.spanToTokens(2,9));}
    @Test public void fallbackCharacterOnlyAllowsWholeUtf8Boundary(){
        String unknown=new String(Character.toChars(0x10ffff));AiTokenizer.Encoded input=tokenizer.encodeRequest(unknown+" 입력",new JSONObject());assertEquals(4,input.ends[0]==1&&input.ends[3]==1?4:0);assertTrue(input.startKeep[0]);assertFalse(input.startKeep[1]);assertFalse(input.endKeep[2]);assertTrue(input.endKeep[3]);assertTrue(input.copyKeep[0]);assertFalse(input.copyKeep[1]);assertFalse(input.copyKeep[2]);assertTrue(input.copyKeep[3]);assertEquals(unknown,input.copy(0,3));rejected(()->input.tokensToSpan(0,2));
    }
    @Test public void predictedStartChoosesItsOwnEndMaskAndRawCpSpan(){
        String source="\"가\" 다음 \"나😀\" 입력";AiTokenizer.Encoded input=tokenizer.encodeRequest(source,new JSONObject());JSONObject literal=input.contractJson().getJSONArray("literals").getJSONObject(1);int anchor=literal.getInt("anchor");float[] start=new float[input.ids.length],end=new float[input.ids.length];start[anchor]=30;end[end.length-1]=100;int[] span=AiCopySelection.span(input,start,end);assertArrayEquals(new int[]{literal.getInt("start"),literal.getInt("end")},span);assertEquals("나😀",AiTokenizer.substring(source,span[0],span[1]));
    }
    @Test public void pointerShapesAndNonfiniteAreRejected(){AiTokenizer.Encoded input=tokenizer.encodeRequest("홈으로",new JSONObject());rejected(()->AiCopySelection.span(input,new float[1],new float[1]));float[] scores=new float[input.ids.length];scores[0]=Float.NaN;rejected(()->AiCopySelection.span(input,scores,new float[input.ids.length]));rejected(()->AiCopySelection.argmax(new float[]{1},new boolean[]{false}));}
    @Test public void contextDuplicateNestedKeysAreRejectedAndEscapesPreserved(){rejected(()->AiTokenizer.parseContextJson("{\"points\":{\"가\":{},\"가\":{}}}"));JSONObject context=AiTokenizer.parseContextJson("{\"points\":{\"x\\n\\u0085😀\":{}}}");assertTrue(context.getJSONObject("points").has("x\n\u0085😀"));assertEquals("\"/\u2028\u0085\\n\"",AiTokenizer.quote("/\u2028\u0085\n"));}
    @Test public void isolatedSurrogatesAndWhitespaceNamesAreRejected(){rejected(()->tokenizer.encodeRequest("\ud800 입력",new JSONObject()));rejected(()->tokenizer.encodeRequest("홈으로",AiJson.object("points",AiJson.object("\udfff",0))));rejected(()->tokenizer.encodeRequest("홈으로",AiJson.object("points",AiJson.object("\u0085\u202f",0))));}
    @Test public void codecFingerprintMatchesPythonAndLegacy535(){
        JSONObject reference;try(java.io.InputStream in=getClass().getResourceAsStream("/literal_input_contract.json")){assertNotNull(in);reference=new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));}catch(Exception error){throw new AssertionError(error);}
        assertEquals(AiJson.canonical(reference.getJSONObject("literal")),AiJson.canonical(AiCodecContract.fingerprint(tokenizer)));assertEquals(AiJson.canonical(reference.getJSONObject("legacy")),AiJson.canonical(AiCodecContract.fingerprint(tokenizer.literal.base)));assertEquals(535,tokenizer.literal.base.size());
    }
    @Test public void reportBindsFingerprintAndActualTokenizerBytes(){
        byte[] raw=AiJson.canonical(AiLiteralCodecTest.specification()).getBytes(StandardCharsets.UTF_8);String hash=AiCodecContract.sha256(raw);JSONObject report=AiJson.object("input_contract",AiCodecContract.fingerprint(tokenizer),"tokenizer_sha256",hash,"files",AiJson.object("tokenizer.json",AiJson.object("bytes",raw.length,"sha256",hash)));AiCodecContract.validateReport(report,tokenizer,raw);
        JSONObject altered=AiJson.copy(report);try{altered.getJSONObject("input_contract").put("copy_contract","legacy-codepoint-span-v1");}catch(Exception error){throw new AssertionError(error);}rejected(()->AiCodecContract.validateReport(altered,tokenizer,raw));byte[] changed=raw.clone();changed[0]^=1;rejected(()->AiCodecContract.validateReport(report,tokenizer,changed));JSONObject missing=AiJson.copy(report);missing.remove("tokenizer_sha256");rejected(()->AiCodecContract.validateReport(missing,tokenizer,raw));JSONObject extra=AiJson.copy(report);try{extra.getJSONObject("input_contract").put("extra",1);}catch(Exception error){throw new AssertionError(error);}rejected(()->AiCodecContract.validateReport(extra,tokenizer,raw));
    }
}
