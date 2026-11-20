package kr.yunseok.pxe22fixture;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;

/** 별도 UID에서 실제 버튼 클릭과 스크롤을 관측하는 전용 시험 화면입니다. */
public final class FixtureActivity extends Activity {
    int clicks;
    private static boolean visualMode,visualMoved,visualDuplicate;
    private final class VisualCanvas extends View {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);int stage;
        VisualCanvas(){super(FixtureActivity.this);setContentDescription("pxe_visual_stage_0_clicks_0");}
        RectF icon(float center){float size=getWidth()*.075f,cx=getWidth()*center,cy=getHeight()*.62f;return new RectF(cx-size/2,cy-size/2,cx+size/2,cy+size/2);}
        void drawIcon(Canvas canvas,RectF rect){paint.setColor(Color.rgb(220,55,65));canvas.drawRect(rect,paint);paint.setColor(Color.rgb(40,75,220));canvas.drawRect(rect.left,rect.top,rect.left+rect.width()*.3f,rect.bottom,paint);paint.setColor(Color.rgb(250,230,100));canvas.drawRect(rect.left,rect.top,rect.right,rect.top+rect.height()*.25f,paint);}
        @Override protected void onDraw(Canvas canvas){canvas.drawColor(stage==0?Color.rgb(244,244,238):Color.rgb(24,33,62));if(stage==0){drawIcon(canvas,icon(visualMoved?.66f:.32f));if(visualDuplicate)drawIcon(canvas,icon(visualMoved?.32f:.66f));}else{paint.setColor(Color.rgb(90,140,220));canvas.drawCircle(getWidth()*.5f,getHeight()*.58f,getWidth()*.12f,paint);}}
        @Override public boolean onTouchEvent(MotionEvent event){if(event.getActionMasked()==MotionEvent.ACTION_UP&&stage==0&&icon(visualMoved?.66f:.32f).contains(event.getX(),event.getY())){clicks++;stage=1;setContentDescription("pxe_visual_stage_1_clicks_"+clicks);invalidate();performClick();}return true;}
        @Override public boolean performClick(){super.performClick();return true;}
    }

    @Override public void onCreate(Bundle state){super.onCreate(state);if(!applyVisualIntent(getIntent()))renderContent();}

    /** 명시한 시험 모드만 초기화하고 일반 앱 재열기는 현재 화면을 유지합니다. */
    @Override protected void onNewIntent(android.content.Intent intent){super.onNewIntent(intent);setIntent(intent);applyVisualIntent(intent);}

    private boolean applyVisualIntent(android.content.Intent intent){
        if(!intent.hasExtra("pxe_visual_mode"))return false;
        visualMode=intent.getBooleanExtra("pxe_visual_mode",false);
        visualMoved=intent.getBooleanExtra("pxe_visual_moved",false);
        visualDuplicate=intent.getBooleanExtra("pxe_visual_duplicate",false);
        clicks=0;renderContent();return true;
    }

    private void renderContent(){if(visualMode){setContentView(new VisualCanvas());return;}ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);TextView count=new TextView(this);count.setText(getString(R.string.fixture_taps,0));count.setTextSize(20);body.addView(count);for(int i=0;i<24;i++){Button button=new Button(this);button.setText(getString(R.string.fixture_target,i+1));button.setOnClickListener(v->{clicks++;count.setText(getString(R.string.fixture_taps,clicks));});body.addView(button,new LinearLayout.LayoutParams(-1,Math.round(100*getResources().getDisplayMetrics().density)));}scroll.addView(body);scroll.setContentDescription("fixture_scroll_0");scroll.setOnScrollChangeListener((v,x,y,oldX,oldY)->scroll.setContentDescription("fixture_scroll_"+y));setContentView(scroll);}
}
