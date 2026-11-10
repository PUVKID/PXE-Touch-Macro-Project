package kr.yunseok.touchmacro;
import org.json.*;
import org.junit.Test;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.Assert.*;

/** Python의 실제 계약이 생성한 고정 벡터와 Android 포트의 결과를 비교합니다. */
public class AiInteropTest {
    JSONObject vectors()throws Exception{try(InputStream stream=getClass().getClassLoader().getResourceAsStream("ai_contract_vectors.json")){assertNotNull("고정 상호운용 벡터가 없습니다",stream);return new JSONObject(new String(stream.readAllBytes(),StandardCharsets.UTF_8));}}
    @Test public void actualTokenizerMatchesPythonIdsOffsetsInventoryAndByteBoundaries()throws Exception{
        JSONObject fixture=vectors();AiTokenizer tokenizer=new AiTokenizer(AiJson.obj(fixture.opt("tokenizer")));JSONArray cases=AiJson.array(fixture.opt("encodings"));
        for(int index=0;index<cases.length();index++){JSONObject row=AiJson.obj(cases.opt(index));AiTokenizer.Encoded actual=tokenizer.encodeRequest(AiJson.string(row.opt("command")),AiJson.obj(row.opt("context")));JSONArray ids=AiJson.array(row.opt("ids")),offsets=AiJson.array(row.opt("offsets"));assertEquals(ids.length(),actual.ids.length);assertEquals(row.opt("command_tokens"),actual.commandTokens);for(int i=0;i<ids.length();i++){assertEquals("행"+index+" 토큰"+i,((Number)ids.opt(i)).longValue(),actual.ids[i]);Object offset=offsets.opt(i);if(AiJson.nil(offset)){assertEquals(-1,actual.starts[i]);assertEquals(-1,actual.ends[i]);}else{JSONArray pair=AiJson.array(offset);assertEquals(pair.opt(0),actual.starts[i]);assertEquals(pair.opt(1),actual.ends[i]);}}}
    }
    @Test public void numericContractUsesTheSameAcceptedAndRejectedValues()throws Exception{
        JSONObject numeric=AiJson.obj(vectors().opt("numeric")),valid=AiJson.obj(numeric.opt("integer_valid"));Iterator<String> names=valid.keys();while(names.hasNext()){String name=names.next();assertEquals(valid.opt(name),AiNumbers.integer(name));}
        JSONArray durations=AiJson.array(numeric.opt("duration_valid"));for(int i=0;i<durations.length();i++){JSONObject row=AiJson.obj(durations.opt(i));assertEquals(((Number)row.opt("ms")).longValue(),AiNumbers.duration(AiJson.string(row.opt("text")),AiJson.string(row.opt("unit"))));}
        JSONArray invalid=AiJson.array(numeric.opt("integer_invalid"));for(int i=0;i<invalid.length();i++)try{AiNumbers.integer(AiJson.string(invalid.opt(i)));fail("잘못된 수치 수락");}catch(IllegalArgumentException expected){}
        invalid=AiJson.array(numeric.opt("duration_invalid"));for(int i=0;i<invalid.length();i++){JSONObject row=AiJson.obj(invalid.opt(i));try{AiNumbers.duration(AiJson.string(row.opt("text")),AiJson.string(row.opt("unit")));fail("잘못된 시간 수락");}catch(IllegalArgumentException expected){}}
    }
    @Test public void structuralTokensAndSourceCopyProduceTheSamePythonAst()throws Exception{
        JSONObject fixture=vectors();JSONArray vocabulary=AiJson.array(fixture.opt("output_tokens"));assertEquals(vocabulary.length(),AiPlanParser.TOKENS.length);for(int i=0;i<vocabulary.length();i++)assertEquals(vocabulary.opt(i),AiPlanParser.TOKENS[i]);JSONArray cases=AiJson.array(fixture.opt("parser"));
        for(int i=0;i<cases.length();i++){JSONObject row=AiJson.obj(cases.opt(i));JSONArray raw=AiJson.array(row.opt("tokens"));List<Object> tokens=new ArrayList<>();for(int k=0;k<raw.length();k++)tokens.add(raw.opt(k));JSONObject actual=AiPlanParser.parse(tokens,AiJson.string(row.opt("source")),AiJson.obj(row.opt("context")));assertEquals(AiJson.canonical(row.opt("ast")),AiJson.canonical(actual));}
    }
}
