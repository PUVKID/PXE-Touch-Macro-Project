package kr.yunseok.touchmacro;
import android.content.Context;
import org.json.*;
import java.util.*;
public final class Store {
    public static String current(Context c){return c.getSharedPreferences("macros",0).getString("current","");}
    public static void save(Context c,Macro m){c.getSharedPreferences("macros",0).edit().putString("current",m.json().toString()).apply();}
    public static Macro load(Context c){try{String s=current(c);return s.isEmpty()?sample():new Macro(new JSONObject(s));}catch(Exception e){return sample();}}
    public static LinkedHashMap<String,Macro> library(Context c){
        LinkedHashMap<String,Macro> out=new LinkedHashMap<>();try{JSONObject o=new JSONObject(c.getSharedPreferences("macros",0).getString("library","{}"));Iterator<String> k=o.keys();while(k.hasNext()){String key=k.next();out.put(key,new Macro(o.getJSONObject(key)));}}catch(Exception ignored){}return out;
    }
    public static void put(Context c,Macro m){LinkedHashMap<String,Macro> all=library(c);all.put(m.name,m);writeLibrary(c,all);save(c,m);}
    public static void remove(Context c,String name){LinkedHashMap<String,Macro> all=library(c);all.remove(name);writeLibrary(c,all);}
    private static void writeLibrary(Context c,Map<String,Macro> all){JSONObject o=new JSONObject();try{for(Map.Entry<String,Macro> e:all.entrySet())o.put(e.getKey(),e.getValue().json());}catch(Exception e){throw new IllegalStateException(e);}c.getSharedPreferences("macros",0).edit().putString("library",o.toString()).apply();}
    public static Macro sample(){Macro m=new Macro();m.name="예제 · 글자 찾고 터치";Step s=new Step("TEXT_TAP");s.text="확인";s.failure="BACK_RETRY";s.retries=3;s.retryDelay=1000;s.timeout=2000;m.steps.add(s);return m;}
}
