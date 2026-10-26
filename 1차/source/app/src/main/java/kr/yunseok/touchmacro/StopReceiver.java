package kr.yunseok.touchmacro;
import android.content.*;
public class StopReceiver extends BroadcastReceiver { @Override public void onReceive(Context c,Intent i){ if(MacroService.get()!=null)MacroService.get().stop(); } }
