package kr.yunseok.touchmacro;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import java.util.function.Consumer;

/** User-drawn screen region. No touch is replayed to the underlying app. */
public final class RegionPicker {
    private final MacroService owner;
    private final WindowManager wm;
    private View canvas,bar;
    private final RectF rect=new RectF();
    private final Consumer<RectF> accept;
    public RegionPicker(MacroService owner,Consumer<RectF> accept){this.owner=owner;this.accept=accept;wm=(WindowManager)owner.getSystemService(Context.WINDOW_SERVICE);}
    private WindowManager.LayoutParams layout(int width,int height){WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);p.gravity=Gravity.TOP|Gravity.START;return p;}
    public void show(){
        Context c=Ui.themed(owner);owner.controller().setVisible(false);
        canvas=new View(c){float x,y;final Paint paint=new Paint(3);final int[] origin=new int[2];final RectF local=new RectF();
            @Override public boolean performClick(){return super.performClick();}
            @Override public boolean onTouchEvent(android.view.MotionEvent e){
                if(e.getActionMasked()==MotionEvent.ACTION_DOWN){x=e.getRawX();y=e.getRawY();rect.set(x,y,x,y);}
                if(e.getActionMasked()==MotionEvent.ACTION_MOVE||e.getActionMasked()==MotionEvent.ACTION_UP){rect.set(Math.min(x,e.getRawX()),Math.min(y,e.getRawY()),Math.max(x,e.getRawX()),Math.max(y,e.getRawY()));if(e.getActionMasked()==MotionEvent.ACTION_UP)performClick();}
                invalidate();return true;
            }
            @Override protected void onDraw(Canvas c){super.onDraw(c);getLocationOnScreen(origin);local.set(rect);local.offset(-origin[0],-origin[1]);paint.setColor(0x77000000);paint.setStyle(Paint.Style.FILL);c.drawRect(0,0,getWidth(),local.top,paint);c.drawRect(0,local.bottom,getWidth(),getHeight(),paint);c.drawRect(0,local.top,local.left,local.bottom,paint);c.drawRect(local.right,local.top,getWidth(),local.bottom,paint);paint.setColor(0xFF93ACFF);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Ui.dp(owner,3));c.drawRect(local,paint);}
        };
        wm.addView(canvas,layout(-1,-1));
        LinearLayout box=new LinearLayout(c);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(Ui.dp(c,12),Ui.dp(c,8),Ui.dp(c,12),Ui.dp(c,8));box.setBackground(Ui.rounded(Ui.card(c),Ui.dp(c,16)));
        TextView hint=new TextView(c);hint.setText("탐지할 부분을 사각형으로 그리세요");hint.setTextColor(Ui.ink(c));hint.setTextSize(13);box.addView(hint);
        LinearLayout actions=new LinearLayout(c);box.addView(actions);Button ok=Ui.button(c,"이 영역 사용"),cancel=Ui.button(c,"취소");actions.addView(ok,new LinearLayout.LayoutParams(0,Ui.dp(c,44),1));actions.addView(cancel,new LinearLayout.LayoutParams(0,Ui.dp(c,44),1));
        cancel.setOnClickListener(v->close());ok.setOnClickListener(v->{if(rect.width()<Ui.dp(c,12)||rect.height()<Ui.dp(c,12)){Toast.makeText(owner,"손가락으로 탐지할 영역을 그려 주세요",Toast.LENGTH_SHORT).show();return;}int[] s=owner.size();RectF percent=new RectF(Math.max(0,rect.left*100/s[0]),Math.max(0,rect.top*100/s[1]),Math.min(100,rect.right*100/s[0]),Math.min(100,rect.bottom*100/s[1]));accept.accept(percent);close();Toast.makeText(owner,"탐지 영역 저장 완료",Toast.LENGTH_SHORT).show();});
        Ui.font(box);bar=box;WindowManager.LayoutParams p=layout(Ui.dp(c,300),-2);p.x=Math.max(0,(owner.size()[0]-p.width)/2);p.y=Ui.dp(c,40);wm.addView(bar,p);
    }
    public void close(){if(canvas!=null){wm.removeView(canvas);canvas=null;}if(bar!=null){wm.removeView(bar);bar=null;}owner.controller().setVisible(true);}
}
