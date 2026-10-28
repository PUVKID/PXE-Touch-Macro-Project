package kr.yunseok.touchmacro;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 실제 OBJ 면을 한 번 투영하고 상태가 바뀔 때만 다시 그리는 작은 홈 모델입니다. */
public final class PxeModelView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final List<PxeGeometry.Surface> surfaces;
    private final int accent,muted,line,edge,shadow;
    private boolean connected,running,paused;
    private int tone=-1;
    public PxeModelView(Context context){
        super(context);setTag("PXE 상태 모델");setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        boolean dark=Ui.dark(context);accent=Ui.accent(context);muted=Ui.muted(context);line=Ui.line(context);edge=dark?0x2856D7C7:0x20FFFFFF;shadow=dark?0x33000000:0x16007F78;
        try(Reader source=new InputStreamReader(context.getAssets().open("pxe_beacon.obj"),StandardCharsets.UTF_8)){surfaces=PxeGeometry.read(source).project(-.42f,-.30f);}
        catch(IOException e){throw new IllegalStateException("PXE 모델 자산을 읽지 못했습니다",e);}
        setState(false,false,false);
    }
    public void setState(boolean connected,boolean running,boolean paused){
        int next=!connected?0:running?(paused?2:3):1;
        if(tone==next)return;this.connected=connected;this.running=running;this.paused=paused;tone=next;
        setContentDescription("PXE 상태 · "+(!connected?"연결 필요":running?(paused?"일시정지":"실행 중"):"대기 중"));invalidate();
    }
    private int shade(int color,float light){return Color.rgb(Math.min(255,(int)(Color.red(color)*light)),Math.min(255,(int)(Color.green(color)*light)),Math.min(255,(int)(Color.blue(color)*light)));}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float scale=Math.min(getWidth()/3.25f,getHeight()/2.25f),cx=getWidth()*.5f,cy=getHeight()*.48f;
        paint.setStyle(Paint.Style.FILL);paint.setColor(shadow);canvas.drawOval(cx-scale*1.25f,cy+scale*.56f,cx+scale*1.25f,cy+scale*.86f,paint);
        int color=!connected||paused?muted:accent;
        for(PxeGeometry.Surface face:surfaces){
            path.rewind();for(int i=0;i<face.x.length;i++){float x=cx+face.x[i]*scale,y=cy+face.y[i]*scale;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}path.close();
            paint.setStyle(Paint.Style.FILL);paint.setColor(shade(color,face.light));canvas.drawPath(path,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(.6f,getResources().getDisplayMetrics().density*.45f));paint.setColor(edge);canvas.drawPath(path,paint);
        }
        paint.setStyle(Paint.Style.FILL);paint.setColor(running&&!paused?accent:line);canvas.drawRoundRect(cx-scale*.36f,cy+scale*.98f,cx+scale*.36f,cy+scale*1.03f,scale*.025f,scale*.025f,paint);
        // 예약 프레임이나 반복 무효화를 만들지 않아 실행·백그라운드의 모델 연산이 이어지지 않습니다.
    }
}
