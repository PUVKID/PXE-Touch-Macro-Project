package kr.yunseok.touchmacro;
import android.content.Context;
import org.json.JSONObject;
public final class WatchStore {
    public static WatchConfig load(Context c){try{return new WatchConfig(new JSONObject(Ui.prefs(c).getString("watch_config","{}")));}catch(Exception e){return new WatchConfig();}}
    public static void save(Context c,WatchConfig p){p.validate();Ui.prefs(c).edit().putString("watch_config",p.json().toString()).apply();}
    public static Macro resolve(Context c,WatchConfig p){
        Macro m=p.macroName.isEmpty()?Store.load(c):Store.library(c).get(p.macroName);
        if(m==null)throw new IllegalArgumentException("선택한 매크로가 없습니다. 탐지 설정에서 다시 선택하세요");
        p.validateMacro(m);return new Macro(m.json());
    }
}
