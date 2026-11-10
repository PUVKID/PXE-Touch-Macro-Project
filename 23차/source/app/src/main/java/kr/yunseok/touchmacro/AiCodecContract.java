package kr.yunseok.touchmacro;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 실제 어휘·복사 의미와 export 보고서의 파일 결합을 확인합니다. */
final class AiCodecContract {
    private AiCodecContract(){}
    static String sha256(byte[] bytes){try{StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return hash.toString();}catch(java.security.NoSuchAlgorithmException error){throw new IllegalStateException(error);}}
    static JSONObject fingerprint(AiTokenizer tokenizer){
        JSONObject special=new JSONObject();JSONArray kinds=new JSONArray();if(tokenizer.literalCodec()){special=AiJson.object("LIT",535,"ENTITY",536,"UNMENTIONED",537,"REC_END",538,"INVENTORY",539);for(String kind:AiLiteralCodec.KINDS)kinds.put(kind);}
        return AiJson.object("format",tokenizer.literalCodec()?AiLiteralCodec.FORMAT:"pxe-codepoint-utf8-v1","input_vocab",tokenizer.size(),
            "characters_sha256",sha256(AiJson.canonical(tokenizer.characters()).getBytes(StandardCharsets.UTF_8)),
            "base_tokenizer_sha256",tokenizer.literalCodec()?tokenizer.literal.baseHash:null,"special_ids",special,"registry_kinds",kinds,
            "normalization","none","offset_unit","unicode-codepoint-half-open","max_input",1024,
            "copy_contract",tokenizer.literalCodec()?"literal-anchor-or-contiguous-unquoted-v1":"legacy-codepoint-span-v1",
            "output_vocab",AiPlanParser.TOKENS.length,"output_tokens_sha256",sha256(AiJson.canonical(new JSONArray(java.util.Arrays.asList(AiPlanParser.TOKENS))).getBytes(StandardCharsets.UTF_8)));
    }
    static void validateReport(JSONObject report,AiTokenizer tokenizer,byte[] tokenizerBytes){
        if(tokenizer.literalCodec()||report.has("input_contract")){
            JSONObject actual=AiJson.obj(report.opt("input_contract")),expected=fingerprint(tokenizer);AiJson.keys(actual,"format","input_vocab","characters_sha256","base_tokenizer_sha256","special_ids","registry_kinds","normalization","offset_unit","max_input","copy_contract","output_vocab","output_tokens_sha256");
            if(!AiJson.canonical(expected).equals(AiJson.canonical(actual)))throw AiJson.error("모델과 입력 codec fingerprint 불일치");
        }
        if(tokenizer.literalCodec()){
            String hash=sha256(tokenizerBytes);JSONObject file=AiJson.obj(AiJson.obj(report.opt("files")).opt("tokenizer.json"));AiJson.keys(file,"bytes","sha256");
            if(!hash.equals(report.opt("tokenizer_sha256"))||!hash.equals(file.opt("sha256"))||AiJson.integer(file.opt("bytes"),1,1024*1024)!=tokenizerBytes.length)throw AiJson.error("리터럴 tokenizer 실제 파일 hash 결합 불일치");
        }
    }
}
