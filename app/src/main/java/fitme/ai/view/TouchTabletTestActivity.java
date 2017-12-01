package fitme.ai.view;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import fitme.ai.R;
import fitme.ai.utils.L;
import fitme.ai.utils.PhoneUtils;


/**
 * Created by hongy on 2017/11/22.
 */

public class TouchTabletTestActivity extends Activity {

    private ImageView imageView;

    //中心点
    private int centerPointHeight;
    private int centerPointWidth;

    //顺时针，逆时针滑动事件
    private int slideCount = 0;
    //夹角
    private int firAngle = 0;
    private int secAngle = 0;
    //触碰点与中心点的夹角
    private int includedAngle = 0;     //0-360
    //是否正在滑动
    private boolean isSliding = false;
    private int clockwiseCount = 0;  //顺时针滑动触发次数
    private int anticlickwiseCount = 0;   //逆时针滑动触发次数


    //双击事件
    private int count = 0;
    private long firClick = 0;
    private long secClick = 0;
    //两次点击时间间隔，单位毫秒
    private final int interval = 1000;

    //长按事件
    private int nowTime;
    private Handler handler = new Handler();
    private Runnable task = new Runnable() {
        @Override
        public void run() {
            if (nowTime <= 3) {
                nowTime += 1;
                handler.postDelayed(this, 1000);
            }else if (nowTime>3){
                nowTime = 0;
                if (!isSliding){
                    L.i("触发长按事件");
                }
                //TODO 长按事件

            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);//去掉标题栏
        //this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//去掉信息栏
        setContentView(R.layout.touch_layout);
        //获取中心点
        int height = Integer.valueOf(PhoneUtils.getValueFromPhone(this).getScreenHeight());
        int width = Integer.valueOf(PhoneUtils.getValueFromPhone(this).getScreenWidth());

        centerPointHeight = height/2;
        centerPointWidth = width/2;
        L.i("Height:"+height+"  width:"+width+"中心点："+centerPointWidth+" "+centerPointHeight);

        imageView = (ImageView) findViewById(R.id.tv_touch);
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        nowTime = 0;
                        handler.removeCallbacks(task);
                        handler.post(task);
                        //双击事件
                        count++;
                        if (1 == count) {
                            firClick = System.currentTimeMillis();
                        } else if (2 == count) {
                            secClick = System.currentTimeMillis();
                            if (secClick - firClick < interval) {
                                //触发双击
                                if (!isSliding){
                                    L.i("触发双击事件");
                                }
                                count = 0;
                                firClick = 0;
                            } else {
                                firClick = secClick;
                                count = 1;
                            }
                            secClick = 0;
                        }

                        break;
                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(task);
                        //L.i("up");
                        isSliding = false;
                        clockwiseCount = 0;
                        anticlickwiseCount = 0;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int RawX = (int)event.getRawX();
                        int RawY = (int)event.getRawY();
                        int angle = (int) Math.toDegrees(Math.atan((double)(RawX-centerPointWidth)/(double) (centerPointHeight-RawY)));
                        if (RawX-centerPointWidth>0&&RawY-centerPointHeight<0){
                            //第一象限
                            includedAngle = angle;
                        }else if (RawX-centerPointWidth<0&&RawY-centerPointHeight<0){
                            includedAngle = angle+360;
                        }else if (RawX-centerPointWidth<0&&RawY-centerPointHeight>0){
                            includedAngle = angle+180;
                        }else if (RawX-centerPointWidth>0&&RawY-centerPointHeight>0){
                            includedAngle = angle+180;
                        }
                       // L.i("tan比值："+includedAngle);
                        slideCount++;
                        if (1 == slideCount) {
                            firAngle = includedAngle;
                        } else if (10 == slideCount) {
                            secAngle = includedAngle;
                            if (firAngle<secAngle){
                                clockwiseCount++;
                                if (clockwiseCount>2){
                                    L.i("触发顺时针滑动事件");
                                    isSliding = true;
                                }
                                anticlickwiseCount = 0;
                                firAngle = 0;
                            }else if (firAngle>secAngle){
                                anticlickwiseCount++;
                                if (anticlickwiseCount>2){
                                    L.i("逆时针");
                                    isSliding = true;
                                }
                                clockwiseCount = 0;
                                firAngle = 0;
                            }
                            secAngle = 0;
                            slideCount = 0;
                            //触发了调节音量，就不再触发其他事件

                        }
                        break;

                }
                return true;
            }
        });
    }

}
