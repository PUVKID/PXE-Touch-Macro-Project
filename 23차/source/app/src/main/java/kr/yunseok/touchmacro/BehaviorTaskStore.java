package kr.yunseok.touchmacro;

import org.json.*;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** 매크로 SharedPreferences와 분리한 개인 작업·불변 revision 저장소입니다. */
public final class BehaviorTaskStore {
    private final File root;
    public static final class Task {
        public final String storageId,taskId,name,target;
        public final long revision;
        public final BehaviorBundle.Snapshot snapshot;
        private Task(String storage,String task,String name,String target,long revision,BehaviorBundle.Snapshot snapshot){this.storageId=storage;this.taskId=task;this.name=name;this.target=target;this.revision=revision;this.snapshot=snapshot;}
        public BehaviorExample.Registry registry(){return snapshot==null?new BehaviorExample.Registry():snapshot.registry;}
        public BehaviorExample.Replay replay(){return snapshot==null?new BehaviorExample.Replay():snapshot.replay;}
    }
    public static final class Prepared implements AutoCloseable {
        private final Task prior,next;private final File data,meta,audit;private boolean committed;
        private Prepared(Task prior,Task next,File data,File meta,File audit){this.prior=prior;this.next=next;this.data=data;this.meta=meta;this.audit=audit;}
        @Override public void close(){if(!committed){data.delete();meta.delete();audit.delete();}}
    }
    public BehaviorTaskStore(File privateDirectory){root=new File(privateDirectory,"behavior");}
    public static void requireEnabled(boolean enabled){if(!enabled)throw new IllegalStateException("AI 기능을 먼저 켜세요");}
    private File directory(String id)throws IOException{if(id==null||!id.matches("[a-f0-9-]{36}"))throw new IOException("개인 작업 식별자 오류");File d=new File(root,id);if(Files.isSymbolicLink(root.toPath())||Files.isSymbolicLink(d.toPath()))throw new IOException("연결된 저장 경로는 사용하지 않습니다");return d;}
    private static byte[] read(File file,int cap)throws IOException{
        if(Files.isSymbolicLink(file.toPath()))throw new IOException("연결된 파일은 읽지 않습니다");
        try(InputStream in=new FileInputStream(file)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(n==0||out.size()+n>cap)throw new IOException("개인 파일 크기 상한");out.write(buf,0,n);}return out.toByteArray();}
    }
    private static JSONObject object(File file)throws IOException{try{return new JSONObject(StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(read(file,65536))).toString());}catch(Exception e){throw new IOException("개인 작업 정보 손상",e);}}
    private static void write(File path,byte[] bytes)throws IOException{try(FileOutputStream out=new FileOutputStream(path)){out.write(bytes);out.getFD().sync();}}
    private static void move(File from,File to)throws IOException{Files.move(from.toPath(),to.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
    private Task metadata(String id)throws IOException{try{JSONObject m=object(new File(directory(id),"current.json"));return new Task(id,m.getString("task"),m.getString("name"),m.getString("target"),m.getLong("revision"),null);}catch(JSONException e){throw new IOException("개인 작업 정보의 필드 오류",e);}}
    public List<Task> list()throws IOException{
        List<Task> tasks=new ArrayList<>();if(!root.exists())return tasks;File[] dirs=root.listFiles();if(dirs==null)throw new IOException("개인 작업 목록을 읽지 못했습니다");
        for(File d:dirs)if(d.isDirectory()&&d.getName().matches("[a-f0-9-]{36}"))tasks.add(metadata(d.getName()));return Collections.unmodifiableList(tasks);
    }
    public Task create(String name,String target,boolean enabled)throws IOException{
        requireEnabled(enabled);return create(name,target,()->true);
    }
    public Task create(String name,String target,BooleanSupplier live)throws IOException{
        validateName(name,target);synchronized(BehaviorTaskStore.class){if(!live.getAsBoolean())throw new IOException("새 작업 요청이 취소되었습니다");String id=UUID.randomUUID().toString();File d=directory(id);if(!d.mkdirs())throw new IOException("개인 작업 폴더를 만들지 못했습니다");File temp=new File(d,"new.json");try{Task t=new Task(id,id,name,target,0,null);write(temp,meta(t,"").toString().getBytes(StandardCharsets.UTF_8));if(!live.getAsBoolean())throw new IOException("새 작업 요청이 취소되었습니다");move(temp,new File(d,"current.json"));return t;}catch(IOException error){temp.delete();d.delete();throw error;}}
    }
    private static void validateName(String name,String target)throws IOException{if(name==null||name.trim().isEmpty()||name.codePointCount(0,name.length())>80||target==null||!target.matches("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+"))throw new IOException("작업 이름·대상 앱을 확인하세요");}
    private static JSONObject meta(Task t,String file)throws IOException{try{return new JSONObject().put("task",t.taskId).put("name",t.name).put("target",t.target).put("revision",t.revision).put("bundle",file);}catch(JSONException e){throw new IOException("개인 작업 정보 구성 실패",e);}}
    public Task load(String id,boolean enabled)throws IOException{
        requireEnabled(enabled);Task m=metadata(id);JSONObject data=object(new File(directory(id),"current.json"));String name;try{name=data.getString("bundle");}catch(JSONException e){throw new IOException("개인 묶음 경로 필드 오류",e);}if(name.isEmpty())return m;
        if(!name.matches("r[0-9]+-[a-f0-9-]{36}\\.pxeb"))throw new IOException("개인 revision 경로 오류");
        BehaviorBundle.Snapshot snapshot;try(InputStream in=new ByteArrayInputStream(read(new File(directory(id),name),BehaviorBundle.MAX_BYTES))){snapshot=BehaviorBundle.read(in);}
        validateTarget(m.target,snapshot);if(!m.taskId.equals(snapshot.taskId)||m.revision!=snapshot.dataRevision)throw new IOException("개인 작업 revision 결합 오류");return new Task(id,m.taskId,m.name,m.target,m.revision,snapshot);
    }
    private static void validateTarget(String target,BehaviorBundle.Snapshot snapshot)throws IOException{
        for(BehaviorExample row:snapshot.replay.all())if(!target.equals(row.before.binding.targetPackage)||!target.equals(row.after.binding.targetPackage))throw new IOException("다른 대상 앱의 시범이 섞여 있습니다");
    }
    public Prepared prepare(Task prior,BehaviorBundle.Snapshot snapshot,boolean enabled)throws IOException{
        return prepare(prior,snapshot,enabled,"[]".getBytes(StandardCharsets.UTF_8));
    }
    public Prepared prepare(Task prior,BehaviorBundle.Snapshot snapshot,boolean enabled,byte[] envelope)throws IOException{
        requireEnabled(enabled);if(snapshot==null||!prior.taskId.equals(snapshot.taskId)||snapshot.dataRevision!=prior.revision+1)throw new IOException("새 개인 revision 오류");validateTarget(prior.target,snapshot);
        if(envelope==null||envelope.length>1024*1024)throw new IOException("전달 원문 크기 상한");
        byte[] bytes=BehaviorBundle.write(snapshot);Task next=new Task(prior.storageId,prior.taskId,prior.name,prior.target,snapshot.dataRevision,snapshot);File d=directory(prior.storageId);String name="r"+next.revision+"-"+UUID.randomUUID()+".pxeb";
        File data=new File(d,name),meta=new File(d,"pending-"+UUID.randomUUID()+".json"),audit=new File(d,name+".delivery.json");try{write(data,bytes);write(audit,envelope);write(meta,meta(next,name).put("delivery",audit.getName()).toString().getBytes(StandardCharsets.UTF_8));return new Prepared(prior,next,data,meta,audit);}catch(IOException|JSONException e){data.delete();meta.delete();audit.delete();throw new IOException("개인 revision 준비 실패",e);}
    }
    /** 큰 쓰기·검증을 끝낸 뒤 허가 검사와 작은 포인터 교체만 실행합니다. */
    public Task commit(Prepared pending,BooleanSupplier live)throws IOException{
        synchronized(BehaviorTaskStore.class){if(!live.getAsBoolean()||metadata(pending.prior.storageId).revision!=pending.prior.revision)throw new IOException("설정·작업·실행 상태가 바뀌어 저장하지 않았습니다");move(pending.meta,new File(directory(pending.prior.storageId),"current.json"));pending.committed=true;return pending.next;}
    }
    public Task importNew(InputStream input,String name,String target,boolean enabled,BooleanSupplier live)throws IOException{
        requireEnabled(enabled);validateName(name,target);
        InputStream guarded=new FilterInputStream(input){@Override public int read(byte[] b,int off,int len)throws IOException{if(!live.getAsBoolean())throw new IOException("가져오기가 취소되었습니다");return super.read(b,off,len);}@Override public int read()throws IOException{if(!live.getAsBoolean())throw new IOException("가져오기가 취소되었습니다");return super.read();}};
        BehaviorBundle.Snapshot source=BehaviorBundle.read(guarded);validateTarget(target,source);if(!live.getAsBoolean())throw new IOException("가져오기가 취소되었습니다");
        long revision;try{revision=Math.addExact(source.dataRevision,1);}catch(ArithmeticException e){throw new IOException("더 이상 증가할 수 없는 작업 revision",e);}
        BehaviorBundle.Snapshot snapshot=new BehaviorBundle.Snapshot(source.taskId,revision,source.registry,source.replay,source.policy);byte[] bytes=BehaviorBundle.write(snapshot);
        String storage=UUID.randomUUID().toString();Task task=new Task(storage,source.taskId,name,target,revision,snapshot);File d=directory(storage);if(!d.mkdirs())throw new IOException("개인 작업 폴더를 만들지 못했습니다");
        File bundle=new File(d,"r"+revision+"-"+UUID.randomUUID()+".pxeb"),pending=new File(d,"new.json");boolean committed=false;
        try{write(bundle,bytes);write(pending,meta(task,bundle.getName()).toString().getBytes(StandardCharsets.UTF_8));
            // 파싱·큰 쓰기 후에만 새 목록 포인터를 공개합니다. 기존 작업을 덮지 않습니다.
            synchronized(BehaviorTaskStore.class){if(!live.getAsBoolean())throw new IOException("가져오기가 취소되었습니다");move(pending,new File(d,"current.json"));committed=true;}return task;
        }finally{if(!committed){bundle.delete();pending.delete();d.delete();}}
    }
}
