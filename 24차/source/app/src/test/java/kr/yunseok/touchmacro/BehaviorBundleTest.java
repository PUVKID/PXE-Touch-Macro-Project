package kr.yunseok.touchmacro;

import org.junit.Test;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import static org.junit.Assert.*;

/** F64 이식과 계속 학습, 비실행성 묶음의 손상 거절을 검사합니다. */
public class BehaviorBundleTest {
    static BehaviorBundle.Snapshot snapshot(BehaviorPolicy policy){return new BehaviorBundle.Snapshot("task",1,BehaviorExampleTest.registry(),BehaviorExampleTest.replay(),policy);}
    static byte[] zip(String name,byte[] content)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(out,StandardCharsets.UTF_8)){ZipEntry entry=new ZipEntry(name);entry.setTime(946684800000L);entry.setMethod(ZipEntry.STORED);entry.setSize(content.length);CRC32 crc=new CRC32();crc.update(content);entry.setCrc(crc.getValue());zip.putNextEntry(entry);zip.write(content);zip.closeEntry();}return out.toByteArray();}
    @Test public void f64BitRoundtripAndPortableAdditionalLearning()throws Exception{
        var registry=BehaviorExampleTest.registry();var replay=BehaviorExampleTest.replay().append(BehaviorExampleTest.example("c0","가","back","e0"));
        BehaviorPolicy policy=new BehaviorPolicy(3,1730).learn(replay,registry,"task",64,8,0.25,BehaviorPolicyTest.NEVER);
        double[] values=policy.parameters();values[0]=-0.0;policy=policy.withParameters(values);
        byte[] archive=BehaviorBundle.write(new BehaviorBundle.Snapshot("task",1,registry,replay,policy));BehaviorBundle.Snapshot imported=BehaviorBundle.read(new ByteArrayInputStream(archive));
        assertEquals(policy.parameterHash(),imported.policy.parameterHash());assertEquals(policy.rngState(),imported.policy.rngState());assertEquals(64,imported.policy.completedSteps());
        assertEquals(Double.doubleToRawLongBits(-0.0),Double.doubleToRawLongBits(imported.policy.parameters()[0]));
        assertEquals(registry.json().toString(),imported.registry.json().toString());assertEquals(8,imported.replay.active("task").size());
        assertEquals(9,imported.replay.all().size());assertEquals("e0",imported.replay.all().get(8).supersedes);
        BehaviorPolicy left=policy.learn(replay,registry,"task",64,8,0.25,BehaviorPolicyTest.NEVER),right=imported.policy.learn(imported.replay,imported.registry,"task",64,8,0.25,BehaviorPolicyTest.NEVER);
        assertArrayEquals(left.parameters(),right.parameters(),0);assertEquals(left.rngState(),right.rngState());
        for(var row:replay.active("task"))assertEquals(left.predict(BehaviorFeatures.encode(row.before)),right.predict(BehaviorFeatures.encode(row.before)));
        assertEquals(left.parameterHash(),BehaviorBundle.read(new ByteArrayInputStream(BehaviorBundle.write(new BehaviorBundle.Snapshot("task",1,registry,replay,left)))).policy.parameterHash());
        System.out.println("PORTABLE_LEARNING "+new org.json.JSONObject().put("F64_bit_identical",true).put("bundle_bytes",archive.length).put("before_steps",policy.completedSteps()).put("after_steps_each",left.completedSteps()).put("continued_hash",left.parameterHash()));
    }
    @Test public void unknownPathsTrailingAndCorruptionRejected()throws Exception{
        for(String name:List.of("../evil","/absolute","C:/evil","unknown","folder/weights.bin")){
            byte[] bad=zip(name,new byte[]{1});assertThrows(IllegalArgumentException.class,()->BehaviorBundle.read(new ByteArrayInputStream(bad)));
        }
        byte[] valid=BehaviorBundle.write(snapshot(new BehaviorPolicy(3,1730)));
        byte[] trailing=Arrays.copyOf(valid,valid.length+1);assertThrows(IllegalArgumentException.class,()->BehaviorBundle.read(new ByteArrayInputStream(trailing)));
        byte[] truncated=Arrays.copyOf(valid,valid.length-7);assertThrows(IllegalArgumentException.class,()->BehaviorBundle.read(new ByteArrayInputStream(truncated)));
        byte[] changed=valid.clone();changed[100]^=1;assertThrows(IllegalArgumentException.class,()->BehaviorBundle.read(new ByteArrayInputStream(changed)));
        assertThrows(IllegalArgumentException.class,()->BehaviorBundle.read(new InputStream(){int count;public int read(){return count++<16777217?0:-1;}}));
    }
    @Test public void weightsAndManifestBindingCannotMix()throws Exception{
        var registry=BehaviorExampleTest.registry();
        assertThrows(IllegalArgumentException.class,()->new BehaviorBundle.Snapshot("task",1,registry,BehaviorExampleTest.replay(),new BehaviorPolicy(2,1730)));
        byte[] weights=new BehaviorPolicy(3,1730).weightBytes();byte[] bad=weights.clone();Arrays.fill(bad,bad.length-8,bad.length,(byte)0xff);
        assertThrows(IllegalArgumentException.class,()->BehaviorPolicy.readWeights(bad,1730,0,0,"0".repeat(64)));
        assertThrows(IllegalArgumentException.class,()->BehaviorPolicy.readWeights(Arrays.copyOf(weights,weights.length+1),1730,0,0,"0".repeat(64)));
    }
    @Test public void negativeStepsRejectedAtSnapshotAndWrite()throws Exception{
        BehaviorPolicy invalid=new BehaviorPolicy(3,1730);BehaviorPolicyTest.setStepsForInvariantProbe(invalid,-1);
        String counters=BehaviorPolicy.counters().toString(),hash=invalid.parameterHash();long rng=invalid.rngState();
        assertThrows(IllegalArgumentException.class,()->snapshot(invalid));
        assertEquals(counters,BehaviorPolicy.counters().toString());
        BehaviorPolicy valid=new BehaviorPolicy(3,1730);BehaviorBundle.Snapshot snapshot=snapshot(valid);
        BehaviorPolicyTest.setStepsForInvariantProbe(valid,-1);String beforeWrite=BehaviorPolicy.counters().toString();
        assertThrows(IllegalArgumentException.class,()->BehaviorBundle.write(snapshot));
        assertEquals(beforeWrite,BehaviorPolicy.counters().toString());assertEquals(hash,invalid.parameterHash());assertEquals(rng,invalid.rngState());
        assertEquals(-1,invalid.completedSteps());
    }
    @Test public void exactJsonFieldsIgnoreOrderButRejectMissingAndUnknown(){
        org.json.JSONObject ordered=BehaviorExample.object("second",2,"first",1);
        BehaviorExample.keys(ordered,"first","second");
        assertThrows(IllegalArgumentException.class,()->BehaviorExample.keys(ordered,"first"));
        assertThrows(IllegalArgumentException.class,()->BehaviorExample.keys(ordered,"first","second","third"));
        BehaviorExample.keys(new org.json.JSONObject());
    }

    static Map<String,byte[]> storedPayloads(byte[] archive)throws Exception {
        Map<String,byte[]> result=new LinkedHashMap<>();
        try(ZipInputStream in=new ZipInputStream(new ByteArrayInputStream(archive),StandardCharsets.UTF_8)){ZipEntry entry;while((entry=in.getNextEntry())!=null){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);result.put(entry.getName(),out.toByteArray());}}
        return result;
    }
    static byte[] storedArchive(Map<String,byte[]> files)throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        try(ZipOutputStream zip=new ZipOutputStream(out,StandardCharsets.UTF_8)){for(var row:files.entrySet()){ZipEntry entry=new ZipEntry(row.getKey());entry.setTime(946684800000L);entry.setMethod(ZipEntry.STORED);entry.setSize(row.getValue().length);CRC32 crc=new CRC32();crc.update(row.getValue());entry.setCrc(crc.getValue());zip.putNextEntry(entry);zip.write(row.getValue());zip.closeEntry();}}
        return out.toByteArray();
    }
    static void assertExtraFreeStoredHeaders(byte[] raw){
        ByteBuffer b=ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);int end=raw.length-22,central=b.getInt(end+16),cursor=central,local=0,count=0;
        assertEquals(0x06054b50,b.getInt(end));assertEquals(6,b.getShort(end+8)&65535);assertEquals(6,b.getShort(end+10)&65535);
        while(cursor<end){
            assertEquals(0x02014b50,b.getInt(cursor));assertEquals(0x04034b50,b.getInt(local));
            assertEquals(0x800,b.getShort(cursor+8)&65535);assertEquals(0x800,b.getShort(local+6)&65535);assertEquals(0,b.getShort(cursor+10)&65535);assertEquals(0,b.getShort(local+8)&65535);
            assertEquals(0,b.getShort(cursor+30)&65535);assertEquals(0,b.getShort(local+28)&65535);assertEquals(0,b.getShort(cursor+32)&65535);
            assertEquals(b.getShort(cursor+12),b.getShort(local+10));assertEquals(b.getShort(cursor+14),b.getShort(local+12));
            assertEquals(local,b.getInt(cursor+42));int name=b.getShort(cursor+28)&65535;assertEquals(name,b.getShort(local+26)&65535);
            local+=30+name+b.getInt(cursor+24);cursor+=46+name;count++;
        }assertEquals(6,count);assertEquals(end,cursor);assertEquals(central,local);
    }
    @Test public void timezoneOffsetsKeepStoredHeadersExtraFreeAndF64Payloads()throws Exception {
        TimeZone originalZone=TimeZone.getDefault();org.json.JSONObject counters=BehaviorPolicy.counters();
        BehaviorPolicy initial=new BehaviorPolicy(3,1730);double[] values=initial.parameters();values[0]=-0.0;values[1]=Double.longBitsToDouble(1);BehaviorPolicy policy=initial.withParameters(values);
        BehaviorBundle.Snapshot snapshot=snapshot(policy);Map<String,byte[]> originalPayloads=null;
        try{
            for(String zone:List.of("America/Los_Angeles","UTC","Asia/Seoul","Pacific/Kiritimati","Pacific/Pago_Pago")){
                TimeZone selected=TimeZone.getTimeZone(zone);assertEquals(zone,selected.getID());TimeZone.setDefault(selected);
                byte[] raw=BehaviorBundle.write(snapshot);assertExtraFreeStoredHeaders(raw);Map<String,byte[]> payloads=storedPayloads(raw);
                if(originalPayloads==null)originalPayloads=payloads;else{assertEquals(originalPayloads.keySet(),payloads.keySet());for(String name:payloads.keySet())assertArrayEquals(name,originalPayloads.get(name),payloads.get(name));}
                BehaviorBundle.Snapshot restored=BehaviorBundle.read(new ByteArrayInputStream(raw));
                assertArrayEquals(policy.weightBytes(),restored.policy.weightBytes());assertEquals(policy.parameterHash(),restored.policy.parameterHash());assertEquals(policy.rngState(),restored.policy.rngState());assertEquals(0,restored.policy.completedSteps());
                assertEquals(Double.doubleToRawLongBits(-0.0),Double.doubleToRawLongBits(restored.policy.parameters()[0]));assertEquals(1L,Double.doubleToRawLongBits(restored.policy.parameters()[1]));
                assertEquals(snapshot.registry.json().toString(),restored.registry.json().toString());assertEquals(snapshot.replay.all().size(),restored.replay.all().size());assertEquals(snapshot.dataRevision,restored.dataRevision);
            }
        }finally{TimeZone.setDefault(originalZone);}
        org.json.JSONObject after=BehaviorPolicy.counters();for(String key:List.of("optimizer_steps","forward_examples","backward_batches"))assertEquals(key,counters.getLong(key),after.getLong(key));
    }
    @Test public void storedHeaderAndManifestRejectionsRemainStrict()throws Exception {
        byte[] valid=BehaviorBundle.write(snapshot(new BehaviorPolicy(3,1730)));ByteBuffer original=ByteBuffer.wrap(valid).order(ByteOrder.LITTLE_ENDIAN);int end=valid.length-22,central=original.getInt(end+16);
        for(String defect:List.of("central_extra","local_extra","flags","method","size","count","order","crc","path")){
            byte[] bad=valid.clone();ByteBuffer b=ByteBuffer.wrap(bad).order(ByteOrder.LITTLE_ENDIAN);
            switch(defect){
                case "central_extra":b.putShort(central+30,(short)1);break;
                case "local_extra":b.putShort(28,(short)1);break;
                case "flags":b.putShort(central+8,(short)0x802);b.putShort(6,(short)0x802);break;
                case "method":b.putShort(central+10,(short)8);b.putShort(8,(short)8);break;
                case "size":b.putInt(central+20,BehaviorBundle.MAX_ENTRY_BYTES+1);b.putInt(central+24,BehaviorBundle.MAX_ENTRY_BYTES+1);break;
                case "count":b.putShort(end+8,(short)5);b.putShort(end+10,(short)5);break;
                case "order":b.putInt(central+42,1);break;
                case "crc":bad[100]^=1;break;
                case "path":byte[] name="../evil.bin".getBytes(StandardCharsets.UTF_8);assertEquals(11,name.length);System.arraycopy(name,0,bad,30,11);System.arraycopy(name,0,bad,central+46,11);break;
                default:throw new AssertionError(defect);
            }assertThrows(defect,IllegalArgumentException.class,()->BehaviorBundle.read(new ByteArrayInputStream(bad)));
        }
        Map<String,byte[]> files=storedPayloads(valid);org.json.JSONObject manifest=new org.json.JSONObject(new String(files.get("manifest.json"),StandardCharsets.UTF_8));manifest.put("unknown_field",true);files.put("manifest.json",manifest.toString().getBytes(StandardCharsets.UTF_8));
        byte[] unknown=storedArchive(files);assertThrows(IllegalArgumentException.class,()->BehaviorBundle.read(new ByteArrayInputStream(unknown)));
    }
}
