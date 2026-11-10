package kr.yunseok.touchmacro;
import org.json.JSONObject;
import java.security.MessageDigest;

/** 원본 네 파일을 한 묶음으로 결합합니다. 공개 해시는 서명 인증을 대신하지 않습니다. */
final class AiBundleManifest {
    static final String[] FILES={"export-report.json","tokenizer.json","encoder.onnx","decoder.onnx"};
    private AiBundleManifest(){}
    static void validate(JSONObject manifest){
        AiJson.keys(manifest,"format","files");if(!"pxe-ai-bundle-v1".equals(manifest.opt("format")))throw AiJson.error("AI 번들 명세 형식 불일치");
        JSONObject files=AiJson.obj(manifest.opt("files"));AiJson.keys(files,FILES);
        for(String name:FILES){JSONObject record=AiJson.obj(files.opt(name));AiJson.keys(record,"bytes","sha256");AiJson.integer(record.opt("bytes"),1,limit(name));if(!AiJson.string(record.opt("sha256")).matches("[0-9a-f]{64}"))throw AiJson.error("AI 번들 해시 형식 불일치");}
    }
    static int limit(String name){for(String allowed:FILES)if(allowed.equals(name))return name.endsWith(".json")?1024*1024:64*1024*1024;throw AiJson.error("알 수 없는 AI 번들 파일");}
    static void verify(JSONObject manifest,String name,byte[] bytes){
        validate(manifest);limit(name);JSONObject record=AiJson.obj(AiJson.obj(manifest.opt("files")).opt(name));
        try{StringBuilder hash=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))hash.append(String.format(java.util.Locale.ROOT,"%02x",b&255));if(bytes.length!=AiJson.integer(record.opt("bytes"),1,limit(name))||!hash.toString().equals(record.opt("sha256")))throw AiJson.error("AI 번들 파일 해시 불일치 · "+name);}
        catch(java.security.NoSuchAlgorithmException error){throw new IllegalStateException("SHA-256을 사용할 수 없습니다",error);}
    }
}
