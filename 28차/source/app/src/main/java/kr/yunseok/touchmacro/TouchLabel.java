package kr.yunseok.touchmacro; import android.content.Context; import android.widget.TextView; // The app uses a platform Activity and platform Material theme, without AppCompat.
@android.annotation.SuppressLint("AppCompatCustomView") public class TouchLabel extends TextView { public TouchLabel(Context c){super(c);} @Override public boolean performClick(){return super.performClick();} }
