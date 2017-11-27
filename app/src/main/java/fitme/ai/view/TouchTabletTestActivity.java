package fitme.ai.view;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import fitme.ai.R;
import fitme.ai.utils.L;

/**
 * Created by hongy on 2017/11/22.
 */

public class TouchTabletTestActivity extends Activity implements GestureDetector.OnGestureListener,GestureDetector.OnDoubleTapListener{

    private GestureDetectorCompat gestureDetectorCompat;
    private TextView textView;
    private EditText editText;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.touch_layout);
        gestureDetectorCompat = new GestureDetectorCompat(this,this);
        gestureDetectorCompat.setOnDoubleTapListener(this);
        textView = (TextView) findViewById(R.id.tv_touch);
        textView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetectorCompat.onTouchEvent(event);
                return true;
            }
        });
    }

    //监听手势
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        L.i("SingleTapConfirmed");
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        L.i("DoubleTap");
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        L.i("DoubleTapEvent");
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        L.i("onDown");
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        L.i("onShowPress");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        L.i("onSingleTapUp");
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        L.i("onLongPress");
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

        L.i("onFling");
        return false;
    }
}
