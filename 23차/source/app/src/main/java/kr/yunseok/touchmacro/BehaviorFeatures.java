package kr.yunseok.touchmacro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** OCR 원문과 직전 행동의 고정 특징이며 다음 행동을 정하는 규칙은 없습니다. */
public final class BehaviorFeatures {
    public static final String VERSION="pxe.behavior.features.v1";
    /** 모두 행동 선택 전에 측정하며 값은 아래 정규화 계약의 [-1,1]입니다. -1은 미측정입니다.
     * 0 frame_age: 현재 uptime-capturedAtMs를1000ms로 나눈 값, 최대1.
     * 1 last_dispatch_age: 직전 실제 입력 전달 후 경과/600000ms, 직전 입력 없으면0.
     * 2 session_age: 명시 실행/시범 세션 시작 후 경과/600000ms.
     * 3 same_text_observations: 현재까지 같은 OCR 원문 집합의 연속 관측 횟수/100, 첫 관측0.
     * 4 capture_retries: 현재 관측을 얻기 전 캡처 재시도 횟수/10.
     * 5 pending_captures: 선택 직전 미완료 캡처 수/2.
     * 6 pending_recognition: 선택 직전 미완료 OCR 수(0/1).
     * 7 resumed_session: 이번 세션이 사용자의 명시 재개인지(0/1).
     * 모든 분수는 [0,1]로 제한합니다. Android가 live lease에서 측정하며 after·정답·성공·교정 여부를 넣지 않습니다.
     * 이번 합성 검사에서는 여덟 값이 모든 행에서 동일한0입니다. 가드 허가를 이 값으로 대체하지 않습니다.
     */
    public static final List<String> STATE_NAMES=List.of("frame_age","last_dispatch_age","session_age","same_text_observations","capture_retries","pending_captures","pending_recognition","resumed_session");
    private BehaviorFeatures(){}
    static byte[] hash(String value){try{return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    static int bucket(byte[] hash,int size){long result=0;for(int i=0;i<8;i++)result=(result<<8)|(hash[i]&255L);return (int)Long.remainderUnsigned(result,size);}
    public static double[] previousAction(String id){BehaviorExample.text(id,80,true);double[] values=new double[64];if(!id.isEmpty()){byte[] hash=hash("pxe.behavior.prev.v1:"+id);values[bucket(hash,64)]=(hash[8]&1)==0?1:-1;}return values;}
    public static int previousCollisions(List<String> ids){Set<String> used=new HashSet<>();int result=0;for(String id:ids){double[] v=previousAction(id);String key=Arrays.toString(v);if(!used.add(key))result++;}return result;}
    public static double[] encode(BehaviorExample.Observation observation){
        if(observation==null)throw BehaviorExample.error("관측 필요");double[] result=new double[584];
        // 계층 중복은 같은 원문을 여러 번 세지 않으며 좌표는 특징에 넣지 않습니다.
        Set<String> unique=new LinkedHashSet<>();for(BehaviorExample.Token token:observation.tokens)unique.add(token.raw);
        for(String raw:unique){int[] cp=raw.codePoints().toArray();for(int i=0;i<cp.length;i++){add(result,"u:"+new String(cp,i,1));if(i+1<cp.length)add(result,"b:"+new String(cp,i,2));}}
        double norm=0;for(int i=0;i<512;i++)norm+=result[i]*result[i];norm=StrictMath.sqrt(norm);if(norm>0)for(int i=0;i<512;i++)result[i]/=norm;
        System.arraycopy(previousAction(observation.previousActionId),0,result,512,64);System.arraycopy(observation.state(),0,result,576,8);return result;
    }
    private static void add(double[] values,String source){byte[] hash=hash("pxe.behavior.ocr.v1:"+source);values[bucket(hash,512)]+=(hash[8]&1)==0?1:-1;}
    public static List<BehaviorExample.Token> deduplicate(List<BehaviorExample.Token> tokens){if(tokens==null||tokens.size()>256)throw BehaviorExample.error("OCR 후보 상한 오류");List<BehaviorExample.Token> result=new ArrayList<>();for(BehaviorExample.Token token:tokens){if(token==null)throw BehaviorExample.error("빈 OCR 후보");boolean same=false;for(var old:result)if(old.same(token)){same=true;break;}if(!same)result.add(token);}return Collections.unmodifiableList(result);}
    public static List<BehaviorExample.Token> proposeAt(List<BehaviorExample.Token> tokens,double x,double y){if(!Double.isFinite(x)||!Double.isFinite(y))throw BehaviorExample.error("탭 관측 좌표 오류");List<BehaviorExample.Token> result=new ArrayList<>();long area=Long.MAX_VALUE;for(var token:deduplicate(tokens))if(x>=token.left&&x<token.right&&y>=token.top&&y<token.bottom){if(token.area()<area){area=token.area();result.clear();}if(token.area()==area)result.add(token);}return Collections.unmodifiableList(result);}
    public static BehaviorExample.Token resolveCurrent(List<BehaviorExample.Token> tokens,String targetRaw){BehaviorExample.text(targetRaw,1024,false);List<BehaviorExample.Token> matches=new ArrayList<>();for(var token:deduplicate(tokens))if(token.raw.equals(targetRaw))matches.add(token);if(matches.isEmpty())throw BehaviorExample.error("현재 OCR에 대상 글자 없음");BehaviorExample.Token smallest=matches.get(0);for(var token:matches)if(token.area()<smallest.area())smallest=token;for(var token:matches)if(!token.contains(smallest))throw BehaviorExample.error("서로 다른 위치의 대상 글자·교차 후보가 모호합니다");return smallest;}
}
