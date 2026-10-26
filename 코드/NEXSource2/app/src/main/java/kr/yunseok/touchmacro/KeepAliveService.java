package kr.yunseok.touchmacro;
import android.app.*;
import android.content.*;
import android.os.IBinder;
public class KeepAliveService extends Service {
 private static final int ID=20;
 public static Notification note(Context c,String status){
  NotificationManager nm=(NotificationManager)c.getSystemService(NOTIFICATION_SERVICE);nm.createNotificationChannel(new NotificationChannel("nex_control","NEX 화면 위 컨트롤",NotificationManager.IMPORTANCE_LOW));
  PendingIntent open=PendingIntent.getActivity(c,20,new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  PendingIntent stop=PendingIntent.getBroadcast(c,21,new Intent(c,StopReceiver.class).setAction("STOP"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  PendingIntent close=PendingIntent.getBroadcast(c,22,new Intent(c,StopReceiver.class).setAction("CLOSE"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  return new Notification.Builder(c,"nex_control").setSmallIcon(R.drawable.ic_macro).setContentTitle("NEX · 화면 위 컨트롤").setContentText(status).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).addAction(new Notification.Action.Builder(null,"정지",stop).build()).addAction(new Notification.Action.Builder(null,"컨트롤 닫기",close).build()).build();
 }
 public static void show(Context c){c.startForegroundService(new Intent(c,KeepAliveService.class));}
 public static void update(Context c,String s){((NotificationManager)c.getSystemService(NOTIFICATION_SERVICE)).notify(ID,note(c,s));}
 public static void close(Context c){c.stopService(new Intent(c,KeepAliveService.class));}
 @Override public int onStartCommand(Intent i,int flags,int startId){startForeground(ID,note(this,"준비 · 알림에서 정지할 수 있습니다"));return START_NOT_STICKY;}
 @Override public IBinder onBind(Intent i){return null;}
 @Override public void onDestroy(){stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
}
