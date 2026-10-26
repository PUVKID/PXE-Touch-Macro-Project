package kr.yunseok.touchmacro;
import android.content.Context;
import org.json.JSONObject;
public final class WatchStore {
    public static boolean blocked(Context c,WatchConfig p){return Ui.prefs(c).getStringSet("watch_locks",java.util.Collections.emptySet()).contains(p.lockKey())||Ui.prefs(c).getString("watch_lock","").equals(p.lockKey());}
    public static void blocked(Context c,WatchConfig p,boolean value){
        android.content.SharedPreferences.Editor e=Ui.prefs(c).edit();java.util.HashSet<String> locks=new java.util.HashSet<>(Ui.prefs(c).getStringSet("watch_locks",java.util.Collections.emptySet()));
        String old=Ui.prefs(c).getString("watch_lock","");if(!old.isEmpty())locks.add(old);
        if(value)locks.add(p.lockKey());else locks.remove(p.lockKey());e.putStringSet("watch_locks",locks).remove("watch_lock");
        if(!e.commit())throw new IllegalStateException("중복 방지 상태를 저장하지 못했습니다");
    }
    public static WatchConfig load(Context c){try{return new WatchConfig(new JSONObject(Ui.prefs(c).getString("watch_config","{}")));}catch(Exception e){return new WatchConfig();}}
    public static void save(Context c,WatchConfig p){p.validate();Ui.prefs(c).edit().putString("watch_config",p.json().toString()).apply();}
    public static Macro resolve(Context c,WatchConfig p){
        Macro m=p.macroName.isEmpty()?Store.load(c):Store.library(c).get(p.macroName);
        if(m==null)throw new IllegalArgumentException("선택한 매크로가 없습니다. 탐지 설정에서 다시 선택하세요");
        return p.prepareMacro(m);
    }
}
