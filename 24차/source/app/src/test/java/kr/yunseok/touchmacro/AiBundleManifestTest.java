package kr.yunseok.touchmacro;
import org.json.JSONObject;
import org.junit.Test;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import static org.junit.Assert.*;

public class AiBundleManifestTest {
    private static final String[] NAMES={"export-report.json","tokenizer.json","encoder.onnx","decoder.onnx"};
    private static String hash(byte[] data)throws Exception{StringBuilder out=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(data))out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();}
    private static JSONObject manifest()throws Exception{JSONObject files=new JSONObject();for(String name:NAMES)files.put(name,AiJson.object("bytes",2,"sha256",hash(new byte[]{1,2})));return AiJson.object("format","pxe-ai-bundle-v1","files",files);}
    private static void verify(JSONObject manifest,String name,byte[] data)throws Exception{try{Class.forName("kr.yunseok.touchmacro.AiBundleManifest").getDeclaredMethod("verify",JSONObject.class,String.class,byte[].class).invoke(null,manifest,name,data);}catch(InvocationTargetException failure){if(failure.getCause() instanceof IllegalArgumentException)throw (IllegalArgumentException)failure.getCause();throw failure;}}
    private static void reject(JSONObject manifest,String name,byte[] data)throws Exception{try{verify(manifest,name,data);fail("잘못된 번들 수락");}catch(IllegalArgumentException expected){}}
    @Test public void everyOriginalFileIsBoundToItsExactBytes()throws Exception{JSONObject manifest=manifest();for(String name:NAMES)verify(manifest,name,new byte[]{1,2});}
    @Test public void sameSizeTokenizerReplacementAndModelChangesAreRejected()throws Exception{JSONObject manifest=manifest();reject(manifest,"tokenizer.json",new byte[]{2,1});reject(manifest,"encoder.onnx",new byte[]{1});reject(manifest,"decoder.onnx",new byte[]{1,2,3});reject(manifest,"export-report.json",new byte[]{2,1});}
    @Test public void manifestStructureAndIntegerTypesAreStrict()throws Exception{JSONObject bad=manifest();AiJson.obj(bad.opt("files")).remove("tokenizer.json");reject(bad,"encoder.onnx",new byte[]{1,2});bad=manifest();AiJson.obj(bad.opt("files")).put("extra",new JSONObject());reject(bad,"encoder.onnx",new byte[]{1,2});bad=manifest();AiJson.obj(AiJson.obj(bad.opt("files")).opt("encoder.onnx")).put("bytes",2.0);reject(bad,"encoder.onnx",new byte[]{1,2});bad=manifest();bad.put("format","other");reject(bad,"encoder.onnx",new byte[]{1,2});}
}
