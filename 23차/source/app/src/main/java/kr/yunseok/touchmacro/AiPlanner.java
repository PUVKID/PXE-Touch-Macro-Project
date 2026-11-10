package kr.yunseok.touchmacro;

import android.content.res.AssetManager;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 번들 FP32 자체 모델을 CPU에서만 실행합니다. 외부 통신·규칙 의미 파서는 없습니다. */
public final class AiPlanner {
    private final AssetManager assets;
    public AiPlanner(AssetManager assets){this.assets=assets;}
    public JSONObject plan(String command,JSONObject context,AiCancellation cancellation) throws Exception {
        cancellation.check();JSONObject bundle=json(read("bundle.json",1024*1024));AiBundleManifest.validate(bundle);JSONObject report=json(bundled("export-report.json",bundle));
        if(!"FP32".equals(report.opt("format"))||!Boolean.TRUE.equals(report.opt("allclose"))||!Boolean.TRUE.equals(report.opt("argmax_equal")))throw AiJson.error("동결된 FP32 정합 보고서가 없습니다");
        JSONArray tokens=AiJson.array(report.opt("output_tokens"));if(tokens.length()!=AiPlanParser.TOKENS.length)throw AiJson.error("출력 어휘 크기 불일치");for(int i=0;i<tokens.length();i++)if(!AiPlanParser.TOKENS[i].equals(tokens.opt(i)))throw AiJson.error("출력 어휘 순서 불일치");
        byte[] tokenizerBytes=bundled("tokenizer.json",bundle);AiTokenizer tokenizer=new AiTokenizer(json(tokenizerBytes));AiCodecContract.validateReport(report,tokenizer,tokenizerBytes);JSONObject config=AiJson.obj(report.opt("config"));
        if(AiJson.integer(config.opt("input_vocab"),1,10000)!=tokenizer.size()||AiJson.integer(config.opt("output_vocab"),1,10000)!=AiPlanParser.TOKENS.length||AiJson.integer(config.opt("max_input"),1,4096)!=1024||AiJson.integer(config.opt("max_output"),1,4096)!=512)throw AiJson.error("모델·토크나이저 계약 불일치");
        AiTokenizer.Encoded input=tokenizer.encodeRequest(command,context);cancellation.check();
        byte[] encoderBytes=verified("encoder.onnx",report,bundle),decoderBytes=verified("decoder.onnx",report,bundle);cancellation.check();
        OrtEnvironment environment=OrtEnvironment.getEnvironment();environment.setTelemetry(false);
        try(OrtSession.RunOptions run=new OrtSession.RunOptions()){
            cancellation.attach(()->{try{run.setTerminate(true);}catch(Exception error){cancellation.terminationProblem(error);}});
            try(OrtSession.SessionOptions options=new OrtSession.SessionOptions()){
                options.setIntraOpNumThreads(4);options.setInterOpNumThreads(1);
                try(OrtSession encoder=environment.createSession(encoderBytes,options);OrtSession decoder=environment.createSession(decoderBytes,options)){
                    cancellation.check();boolean[][] sourceKeep=new boolean[1][input.ids.length],copyKeep=new boolean[][]{input.copyKeep.clone()};Arrays.fill(sourceKeep[0],true);
                    try(OnnxTensor source=OnnxTensor.createTensor(environment,new long[][]{input.ids});OnnxTensor keep=OnnxTensor.createTensor(environment,sourceKeep);OnnxTensor copy=OnnxTensor.createTensor(environment,copyKeep)){
                        Map<String,OnnxTensor> encoderInput=new HashMap<>();encoderInput.put("source",source);encoderInput.put("source_keep",keep);
                        try(OrtSession.Result encoded=encoder.run(encoderInput,run)){
                            OnnxTensor memory=(OnnxTensor)encoded.get(0);ArrayList<Long> prefix=new ArrayList<>();prefix.add(1L);ArrayList<Object> generated=new ArrayList<>();
                            for(int step=0;step<511;step++){
                                cancellation.check();long[][] target=new long[1][prefix.size()];for(int i=0;i<prefix.size();i++)target[0][i]=prefix.get(i);
                                try(OnnxTensor targetTensor=OnnxTensor.createTensor(environment,target)){
                                    Map<String,OnnxTensor> decoderInput=new HashMap<>();decoderInput.put("memory",memory);decoderInput.put("target",targetTensor);decoderInput.put("source_keep",keep);decoderInput.put("copy_keep",copy);
                                    try(OrtSession.Result decoded=decoder.run(decoderInput,run)){
                                        cancellation.check();float[][][] logits=(float[][][])decoded.get(0).getValue();int last=prefix.size()-1,identifier=argmax(logits[0][last],null);String token=AiPlanParser.TOKENS[identifier];
                                        if(token.equals("COPY")){
                                            float[] starts=((float[][][])decoded.get(1).getValue())[0][last],ends=((float[][][])decoded.get(2).getValue())[0][last];int[] span=AiCopySelection.span(input,starts,ends);generated.add(AiJson.object("token","COPY","start",span[0],"end",span[1]));
                                        }else generated.add(token);
                                        prefix.add((long)identifier);
                                        if(token.equals("EOS")){JSONObject plan=AiPlanParser.parse(generated,command,context);cancellation.check();return plan;}
                                    }
                                }
                            }
                            throw AiJson.error("모델 출력이 잘렸습니다. 일부 동작은 실행하지 않습니다");
                        }
                    }
                }
            }finally{cancellation.detach();}
        }
    }
    private static int argmax(float[] values,boolean[] allowed){return AiCopySelection.argmax(values,allowed);}
    private byte[] bundled(String name,JSONObject bundle)throws IOException{byte[] bytes=read(name,AiBundleManifest.limit(name));AiBundleManifest.verify(bundle,name,bytes);return bytes;}
    private byte[] verified(String name,JSONObject report,JSONObject bundle) throws Exception {JSONObject record=AiJson.obj(AiJson.obj(report.opt("files")).opt(name));byte[] bytes=bundled(name,bundle);if(AiJson.integer(record.opt("bytes"),1,64*1024*1024)!=bytes.length||!hex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(record.opt("sha256")))throw AiJson.error("번들 모델 해시 불일치");return bytes;}
    private byte[] read(String name,int limit) throws IOException {
        try(InputStream stream=assets.open("ai/"+name);ByteArrayOutputStream output=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int count;while((count=stream.read(buffer))!=-1){if(output.size()>limit-count)throw new IOException("AI 번들 파일 크기 상한 초과");output.write(buffer,0,count);}return output.toByteArray();
        }catch(java.io.FileNotFoundException error){throw new IOException("검증된 자체 모델 번들이 아직 준비되지 않았습니다",error);}
    }
    private static JSONObject json(byte[] bytes) throws Exception{return new JSONObject(StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString());}
    private static String hex(byte[] bytes){StringBuilder value=new StringBuilder();for(byte b:bytes)value.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return value.toString();}
}
