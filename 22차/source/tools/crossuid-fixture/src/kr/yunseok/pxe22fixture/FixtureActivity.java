package kr.yunseok.pxe22fixture;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;

/** 별도 UID에서 실제 버튼 클릭과 스크롤을 관측하는 전용 시험 화면입니다. */
public final class FixtureActivity extends Activity {
    int clicks;
    @Override public void onCreate(Bundle state){super.onCreate(state);ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);TextView count=new TextView(this);count.setText(getString(R.string.fixture_taps,0));count.setTextSize(20);body.addView(count);for(int i=0;i<24;i++){Button button=new Button(this);button.setText(getString(R.string.fixture_target,i+1));button.setOnClickListener(v->{clicks++;count.setText(getString(R.string.fixture_taps,clicks));});body.addView(button,new LinearLayout.LayoutParams(-1,Math.round(100*getResources().getDisplayMetrics().density)));}scroll.addView(body);scroll.setContentDescription("fixture_scroll_0");scroll.setOnScrollChangeListener((v,x,y,oldX,oldY)->scroll.setContentDescription("fixture_scroll_"+y));setContentView(scroll);}
}
