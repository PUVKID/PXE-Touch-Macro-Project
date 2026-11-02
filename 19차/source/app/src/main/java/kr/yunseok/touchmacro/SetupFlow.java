package kr.yunseok.touchmacro;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

/** Durable setup progress and a separate recording draft; existing macros are preserved. */
public final class SetupFlow {
    private SetupFlow(){}
    private static SharedPreferences prefs(Context c){return Ui.prefs(c);}
    public static boolean active(Context c){return prefs(c).getBoolean("setup_active",!prefs(c).getBoolean("setup_completed",false));}
    public static int stage(Context c){return Math.max(0,Math.min(4,prefs(c).getInt("setup_stage",0)));}
    public static void stage(Context c,int n){write(prefs(c).edit().putBoolean("setup_active",true).putInt("setup_stage",n));}
    public static void begin(Context c){stage(c,0);}
    public static void finished(Context c){write(prefs(c).edit().putBoolean("setup_active",false).putBoolean("setup_completed",true));}
    public static String name(Context c){return prefs(c).getString("setup_name","내 반복 작업");}
    public static int areaIndex(Context c){return prefs(c).getInt("setup_area_index",0);}
    public static void areaIndex(Context c,int index){write(prefs(c).edit().putInt("setup_area_index",index));}
    public static void name(Context c,String name){if(name.trim().isEmpty()||name.length()>70)throw new IllegalArgumentException("작업 이름을 1~70자로 입력하세요");write(prefs(c).edit().putString("setup_name",name.trim()));}
    public static boolean pending(Context c){return prefs(c).contains("setup_backup");}
    public static void beginDraft(Context c){
        if(pending(c)){try{Store.save(c,new Macro(new JSONObject(prefs(c).getString("setup_draft",""))));return;}catch(Exception e){throw new IllegalStateException("임시 녹화를 읽지 못했습니다. 녹화 취소 후 다시 시작하세요",e);}}
        Macro previous=Store.load(c),draft=new Macro();String base=name(c),unique=base;int n=2;
        while(Store.library(c).containsKey(unique))unique=base+" ("+(n++)+")";
        draft.name=unique;draft.startDelay=0;draft.rounds=1;
        write(prefs(c).edit().putString("setup_backup",previous.json().toString()).putString("setup_draft",draft.json().toString()));
        Store.save(c,draft);
    }
    public static void captureDraft(Context c,Macro m){if(pending(c))write(prefs(c).edit().putString("setup_draft",m.json().toString()));}
    public static void finishDraft(Context c){
        if(!pending(c))throw new IllegalStateException("먼저 녹화를 시작하세요");
        Macro recorded=Store.load(c);recorded.validate();Store.put(c,recorded);
        WatchConfig p=WatchStore.load(c);p.macroName=recorded.name;WatchStore.save(c,p);
        restore(c);stage(c,3);
    }
    public static void cancelDraft(Context c){restore(c);stage(c,2);}
    private static void restore(Context c){
        if(!pending(c))return;
        try{Store.save(c,new Macro(new JSONObject(prefs(c).getString("setup_backup",""))));}catch(Exception e){throw new IllegalStateException("이전 매크로를 복구하지 못했습니다",e);}
        write(prefs(c).edit().remove("setup_backup").remove("setup_draft"));
    }
    private static void write(SharedPreferences.Editor e){if(!e.commit())throw new IllegalStateException("설정 저장에 실패했습니다");}
}
