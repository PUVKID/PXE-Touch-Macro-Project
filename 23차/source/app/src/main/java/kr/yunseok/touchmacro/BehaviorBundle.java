package kr.yunseok.touchmacro;

import org.json.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;

/** 개인 학습 상태를 비실행성·크기 제한 ZIP_STORED 묶음으로 옮깁니다. 파일을 추출하지 않습니다. */
public final class BehaviorBundle {
    public static final int MAX_BYTES=16777216,MAX_ENTRY_BYTES=8388608;
    private static final String FORMAT="pxe.behavior.bundle.v1";
    private static final Set<String> FILES=Set.of("manifest.json","weights.bin","actions.json","demonstrations.jsonl","corrections.jsonl","training-state.json");
    private BehaviorBundle(){}
    public static final class Snapshot {
        public final String taskId;
        public final long dataRevision;
        public final BehaviorExample.Registry registry;
        public final BehaviorExample.Replay replay;
        public final BehaviorPolicy policy;
        public Snapshot(String taskId,long dataRevision,BehaviorExample.Registry registry,BehaviorExample.Replay replay,BehaviorPolicy policy){
            this.taskId=BehaviorExample.text(taskId,80,false);if(dataRevision<0||registry==null||replay==null||policy==null||policy.completedSteps()<0||policy.inputSize()!=584||policy.hiddenSize()!=32||policy.outputSize()!=registry.size())throw BehaviorExample.error("묶음 모델·사전·revision·학습 단계 결합 오류");
            for(BehaviorExample row:replay.all()){if(!taskId.equals(row.before.binding.taskId)||row.before.binding.dataRevision>dataRevision)throw BehaviorExample.error("다른 작업·미래 revision의 시범");registry.indexOf(row.actionId);if(!row.before.previousActionId.isEmpty())registry.indexOf(row.before.previousActionId);if(!row.after.previousActionId.isEmpty())registry.indexOf(row.after.previousActionId);}
            this.dataRevision=dataRevision;this.registry=registry;this.replay=replay;this.policy=policy;
        }
    }
    public static byte[] write(Snapshot snapshot){
        if(snapshot==null||snapshot.policy.completedSteps()<0)throw BehaviorExample.error("저장할 묶음·비음수 학습 단계 필요");Map<String,byte[]> files=new LinkedHashMap<>();files.put("weights.bin",snapshot.policy.weightBytes());files.put("actions.json",utf8(snapshot.registry.json().toString()));
        StringBuilder demonstrations=new StringBuilder(),corrections=new StringBuilder();JSONArray order=new JSONArray(),active=new JSONArray();
        for(BehaviorExample row:snapshot.replay.all()){order.put(row.id);(row.provenance.equals("CORRECTION")?corrections:demonstrations).append(row.json()).append('\n');}
        for(BehaviorExample row:snapshot.replay.active(snapshot.taskId))active.put(row.id);
        files.put("demonstrations.jsonl",utf8(demonstrations.toString()));files.put("corrections.jsonl",utf8(corrections.toString()));
        JSONObject state=BehaviorExample.object("format",FORMAT,"features",BehaviorFeatures.VERSION,"dtype","F64","input",584,"hidden",32,"K",snapshot.registry.size(),"seed",snapshot.policy.seed(),"rng",String.format(Locale.ROOT,"%016x",snapshot.policy.rngState()),"steps",snapshot.policy.completedSteps(),"initial_hash",snapshot.policy.initialHash(),"replay_order",order,"active_ids",active,"data_revision",snapshot.dataRevision,"task_id",snapshot.taskId);
        files.put("training-state.json",utf8(state.toString()));JSONObject entries=new JSONObject();
        for(var entry:files.entrySet()){if(entry.getValue().length>MAX_ENTRY_BYTES)throw BehaviorExample.error("묶음 파일 크기 상한");try{entries.put(entry.getKey(),BehaviorExample.object("bytes",entry.getValue().length,"sha256",BehaviorPolicy.hex(BehaviorPolicy.digest(entry.getValue()))));}catch(JSONException e){throw BehaviorExample.error("묶음 파일 선언 JSON 생성 오류",e);}}
        files.put("manifest.json",utf8(BehaviorExample.object("format",FORMAT,"files",entries).toString()));
        // 2000년 UTC 고정 시각은 시간대 변환 뒤에도 DOS 범위 안이므로 확장 timestamp를 만들지 않습니다.
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(bytes,StandardCharsets.UTF_8)){for(var entry:files.entrySet()){ZipEntry item=new ZipEntry(entry.getKey());item.setTime(946684800000L);item.setMethod(ZipEntry.STORED);item.setSize(entry.getValue().length);CRC32 crc=new CRC32();crc.update(entry.getValue());item.setCrc(crc.getValue());zip.putNextEntry(item);zip.write(entry.getValue());zip.closeEntry();if(bytes.size()>MAX_BYTES)throw BehaviorExample.error("전체 묶음 크기 상한");}}byte[] raw=bytes.toByteArray();if(raw.length>MAX_BYTES)throw BehaviorExample.error("전체 묶음 크기 상한");return raw;}catch(IOException e){throw BehaviorExample.error("묶음 저장 오류",e);}
    }
    public static Snapshot read(InputStream input){
        if(input==null)throw BehaviorExample.error("가져올 입력 필요");try{ByteArrayOutputStream data=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int count;while((count=input.read(buffer))!=-1){if(count==0)throw BehaviorExample.error("진행하지 않는 묶음 stream");if(data.size()+count>MAX_BYTES)throw BehaviorExample.error("stream 실제 읽기 상한 초과");data.write(buffer,0,count);}Map<String,byte[]> files=readStoredZip(data.toByteArray());
            JSONObject manifest=json(files.get("manifest.json"));BehaviorExample.keys(manifest,"format","files");if(!FORMAT.equals(BehaviorExample.string(manifest.get("format"))))throw BehaviorExample.error("묶음 format 오류");JSONObject declarations=manifest.getJSONObject("files");Set<String> declared=new HashSet<>(FILES);declared.remove("manifest.json");BehaviorExample.keys(declarations,declared.toArray(new String[0]));
            for(String name:declared){JSONObject record=declarations.getJSONObject(name);BehaviorExample.keys(record,"bytes","sha256");if(BehaviorExample.integer(record.get("bytes"),0,MAX_ENTRY_BYTES)!=files.get(name).length||!BehaviorPolicy.hex(BehaviorPolicy.digest(files.get(name))).equals(BehaviorExample.string(record.get("sha256"))))throw BehaviorExample.error("묶음 실제 크기·hash 불일치");}
            JSONObject state=json(files.get("training-state.json"));BehaviorExample.keys(state,"format","features","dtype","input","hidden","K","seed","rng","steps","initial_hash","replay_order","active_ids","data_revision","task_id");
            if(!FORMAT.equals(state.get("format"))||!BehaviorFeatures.VERSION.equals(state.get("features"))||!"F64".equals(state.get("dtype"))||BehaviorExample.integer(state.get("input"),584,584)!=584||BehaviorExample.integer(state.get("hidden"),32,32)!=32)throw BehaviorExample.error("묶음 특징·dtype·형상 계약 불일치");
            BehaviorExample.Registry registry=BehaviorExample.Registry.fromJson(array(files.get("actions.json")));if(BehaviorExample.integer(state.get("K"),1,500)!=registry.size())throw BehaviorExample.error("출력 사전 K 불일치");
            Map<String,BehaviorExample> examples=new LinkedHashMap<>();readRows(files.get("demonstrations.jsonl"),"DEMONSTRATION",examples);readRows(files.get("corrections.jsonl"),"CORRECTION",examples);
            JSONArray order=state.getJSONArray("replay_order");if(order.length()!=examples.size())throw BehaviorExample.error("replay 순서 길이 불일치");BehaviorExample.Replay replay=new BehaviorExample.Replay();Set<String> used=new HashSet<>();
            for(int i=0;i<order.length();i++){String id=BehaviorExample.string(order.get(i));BehaviorExample row=examples.get(id);if(row==null||!used.add(id))throw BehaviorExample.error("replay 중복·미등록 ID");replay=replay.append(row);}
            String taskId=BehaviorExample.string(state.get("task_id"));JSONArray active=new JSONArray();for(BehaviorExample row:replay.active(taskId))active.put(row.id);if(!active.toString().equals(state.getJSONArray("active_ids").toString()))throw BehaviorExample.error("교정·활성 replay 상태 불일치");
            String rng=BehaviorExample.string(state.get("rng"));if(!rng.matches("[0-9a-f]{16}"))throw BehaviorExample.error("RNG 상태 형식 오류");
            BehaviorPolicy policy=BehaviorPolicy.readWeights(files.get("weights.bin"),BehaviorExample.integer(state.get("seed"),Long.MIN_VALUE,Long.MAX_VALUE),Long.parseUnsignedLong(rng,16),BehaviorExample.integer(state.get("steps"),0,Long.MAX_VALUE),BehaviorExample.string(state.get("initial_hash")));
            return new Snapshot(taskId,BehaviorExample.integer(state.get("data_revision"),0,Long.MAX_VALUE),registry,replay,policy);
        }catch(IOException|JSONException|BufferUnderflowException|IndexOutOfBoundsException e){throw BehaviorExample.error("손상되거나 지원하지 않는 학습 묶음",e);}
    }
    private static byte[] utf8(String source){return source.getBytes(StandardCharsets.UTF_8);}
    private static String decode(byte[] raw){try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(raw)).toString();}catch(CharacterCodingException e){throw BehaviorExample.error("묶음 UTF-8 오류",e);}}
    private static Object value(byte[] raw){try{JSONTokener parser=new JSONTokener(decode(raw));Object value=parser.nextValue();if(parser.nextClean()!=0)throw BehaviorExample.error("JSON 뒤 추가 데이터");return value;}catch(JSONException e){throw BehaviorExample.error("묶음 JSON 해석 오류",e);}}
    private static JSONObject json(byte[] raw){Object value=value(raw);if(!(value instanceof JSONObject))throw BehaviorExample.error("JSON object 필요");return (JSONObject)value;}
    private static JSONArray array(byte[] raw){Object value=value(raw);if(!(value instanceof JSONArray))throw BehaviorExample.error("JSON array 필요");return (JSONArray)value;}
    private static void readRows(byte[] raw,String provenance,Map<String,BehaviorExample> result){String text=decode(raw);if(text.isEmpty())return;if(!text.endsWith("\n"))throw BehaviorExample.error("JSONL 마지막 LF 필요");String[] rows=text.split("\n",-1);for(int i=0;i<rows.length-1;i++){BehaviorExample row=BehaviorExample.fromJson(json(utf8(rows[i])));if(!provenance.equals(row.provenance)||result.putIfAbsent(row.id,row)!=null||result.size()>4096)throw BehaviorExample.error("시범·교정 중복·출처·상한 오류");}}

    /** 제한된 STORED ZIP만 허용하며 선언과 실제 local/central/끝 위치를 모두 대조합니다. */
    private static Map<String,byte[]> readStoredZip(byte[] raw){
        if(raw.length<22)throw BehaviorExample.error("잘린 ZIP");ByteBuffer b=ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);int end=raw.length-22;
        if(b.getInt(end)!=0x06054b50||u16(b,end+4)!=0||u16(b,end+6)!=0||u16(b,end+8)!=6||u16(b,end+10)!=6||u16(b,end+20)!=0)throw BehaviorExample.error("ZIP 끝·다중disk·개수·trailing 오류");
        long centralSize=u32(b,end+12),centralOffset=u32(b,end+16);if(centralOffset+centralSize!=end||centralOffset<0||centralOffset>end)throw BehaviorExample.error("ZIP central 범위 오류");int cursor=(int)centralOffset,local=0;Map<String,byte[]> result=new LinkedHashMap<>();
        while(cursor<end){if(cursor+46>end||b.getInt(cursor)!=0x02014b50)throw BehaviorExample.error("ZIP central header 오류");int flags=u16(b,cursor+8),method=u16(b,cursor+10),nameLength=u16(b,cursor+28),extra=u16(b,cursor+30),comment=u16(b,cursor+32);long crc=u32(b,cursor+16),size=u32(b,cursor+24),compressed=u32(b,cursor+20),offset=u32(b,cursor+42);
            if((flags&~0x800)!=0||method!=0||extra!=0||comment!=0||u16(b,cursor+34)!=0||size!=compressed||size>MAX_ENTRY_BYTES||nameLength<1||nameLength>80||cursor+46+nameLength>end||offset!=local)throw BehaviorExample.error("미지원 ZIP 압축·extra·경로·크기·순서");
            String name=decode(Arrays.copyOfRange(raw,cursor+46,cursor+46+nameLength));if(!FILES.contains(name)||result.containsKey(name))throw BehaviorExample.error("ZIP 미지원 경로·중복 entry");
            if(local+30>centralOffset||b.getInt(local)!=0x04034b50||u16(b,local+6)!=flags||u16(b,local+8)!=method||u32(b,local+14)!=crc||u32(b,local+18)!=size||u32(b,local+22)!=size||u16(b,local+26)!=nameLength||u16(b,local+28)!=0)throw BehaviorExample.error("ZIP local 선언 불일치");
            long body=(long)local+30+nameLength,next=body+size;if(next>centralOffset||!name.equals(decode(Arrays.copyOfRange(raw,local+30,(int)body))))throw BehaviorExample.error("ZIP 실제 payload 범위 불일치");byte[] bytes=Arrays.copyOfRange(raw,(int)body,(int)next);CRC32 actual=new CRC32();actual.update(bytes);if(actual.getValue()!=crc)throw BehaviorExample.error("ZIP 실제 CRC 불일치");result.put(name,bytes);local=(int)next;cursor+=46+nameLength;
        }
        if(cursor!=end||local!=centralOffset||!result.keySet().equals(FILES))throw BehaviorExample.error("ZIP 남는 데이터·파일 목록 불일치");return result;
    }
    private static int u16(ByteBuffer b,int offset){return b.getShort(offset)&0xffff;}
    private static long u32(ByteBuffer b,int offset){return b.getInt(offset)&0xffffffffL;}
}
