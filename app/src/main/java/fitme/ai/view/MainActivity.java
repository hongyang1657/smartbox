package fitme.ai.view;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SynthesizerListener;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.param.family.BLFamilyModuleInfo;
import cn.com.broadlink.sdk.result.account.BLLoginResult;
import cn.com.broadlink.sdk.result.family.BLAllFamilyInfoResult;
import cn.com.broadlink.sdk.result.family.BLFamilyIdListGetResult;
import fitme.ai.FileConfig;
import fitme.ai.MyApplication;
import fitme.ai.R;
import fitme.ai.bean.DeviceBean;
import fitme.ai.bean.MessageGet;
import fitme.ai.bean.Music;
import fitme.ai.bean.YeelightDeviceBean;
import fitme.ai.bean.YeelightStripeIpAndPortBean;
import fitme.ai.model.BLControl;
import fitme.ai.model.BLControlConstants;
import fitme.ai.model.YeelightControl;
import fitme.ai.service.MusicPlayerService;
import fitme.ai.setting.api.ApiManager;
import fitme.ai.utils.JsonPraser;
import fitme.ai.utils.L;
import fitme.ai.utils.Mac;
import fitme.ai.utils.NetworkStateUtil;
import fitme.ai.utils.SignAndEncrypt;
import fitme.ai.utils.WordsToVoice;
import fitme.ai.view.impl.IGetYeelight;
import okhttp3.ResponseBody;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sai.commnpkg.DirectorBaseMsg;
import org.sai.commnpkg.saiAPI_wrap;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    //UI相关
    private ImageView ivLight1,ivLight2,ivLight3,ivLight4,ivLight5,ivLight6,ivLight7
            ,ivLight8,ivLight9,ivLight10,ivLight11,ivLight12;
    private List<ImageView> ivLightList = new LinkedList<>();
    private Button bt1,bt2,bt3,bt4,bt5,bt6,bt7,bt8;

    private MusicReceiver musicReceiver = null;


    private static final int TIMER = 1;   //跑马灯计时器
    private static final int CLEAR_ALL = 2;  //熄灭所有灯
    private int timer = 0;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case TIMER:
                    lightUp(timer);
                    Log.i("result", "handleMessage: "+timer);
                    if (timer==12){
                        timer = 0;
                    }
                    break;
                case CLEAR_ALL:
                    clearAllLightToColor(R.drawable.cycler_shape_gray);
                    break;
                default:
                    break;
            }
        }
    };

    //讯飞TTS
    private WordsToVoice wordsToVoice;
    private MyApplication app;

    //播放短音效
    private SoundPool soundPool;
    private int soundid;

    //音量控制
    private AudioManager mAudioManager;
    private int maxVolume;
    private int currentVolume;

    //博联控制类
    private BLControl blControl;

    //yeelight
    private Map<String,YeelightStripeIpAndPortBean> yeelightStripeMap;
    private int bright;
    //yeelight灯带的控制相关参数
    //yeelight灯控制回调
    private IGetYeelight iGetYeelight = new IGetYeelight() {
        @Override
        public void getInfoList(List<YeelightDeviceBean> templist) {
            L.i("yellight控制中心传递过来的数据集合长度为:" + templist.size());
            for (int i = 0; i < templist.size(); i++) {
                YeelightDeviceBean yeelightDeviceBean = templist.get(i);
                String location = yeelightDeviceBean.getLocation();//yeelight搜索之后的当前设备的基本信息包括IP地址以及端口号
//                 int bright;//亮度
//                 String ct;//饱和度
//                 String hue;//颜色
                String hue = yeelightDeviceBean.getHue();//颜色值0-360
                String ct = yeelightDeviceBean.getCt();//饱和度0-4800
                int bright = yeelightDeviceBean.getBright();//亮度0-100
                //yeelight://172.16.12.101:55443
                String ip = location.split(":")[1].replace("//", "").trim();
                String port = location.split(":")[2].trim();
                YeelightStripeIpAndPortBean yeelightStripeIpAndPortBean = new YeelightStripeIpAndPortBean();
                yeelightStripeIpAndPortBean.setIp(ip);
                yeelightStripeIpAndPortBean.setPort(port);
                yeelightStripeIpAndPortBean.setHue(hue);
                yeelightStripeIpAndPortBean.setBright(bright);
                yeelightStripeIpAndPortBean.setCt(ct);
                yeelightStripeIpAndPortBean.setId(yeelightDeviceBean.getId());
                yeelightStripeIpAndPortBean.setModel(yeelightDeviceBean.getModel().trim());
//                    yeelightStripeIpAndPortBeenlist.add(yeelightStripeIpAndPortBean);
                yeelightStripeMap.put(yeelightDeviceBean.getId().trim(), yeelightStripeIpAndPortBean);
            }
        }

        @Override
        public void getIpAndDevice(YeelightDeviceBean yeelightDeviceBean) {
            int brightnew = yeelightDeviceBean.getBright();
            bright = brightnew;
            L.i("yeelight亮度："+bright);
        }

        @Override
        public void getResponse(String value) {
            L.i("调用yeelight控制方法之后的返回结果回调：" + value);
        }
    };


    private static final String TAG = "SoundAi";

    private static final float WAKE_UP_THRESHOLD_VALUE = 0.55F;
    private static final int TIMEOUT_MILLIS = 15000, INTERVAL = 1000;
    private static final int VAD_END = 1,
            VAD_START_TIME_OUT = 13,
            ASR_ERROR = 3;

    private TextView asrTxv,
            lastAsrTxv,
            vadTxv,
            countTv,
            fitme_result;

    private BaseCallBack baseCallBack;
    private saiAPI_wrap saiAPI_wrap;
    private TimeCounter timeCounter;

    private volatile String wakeUpId;
    private volatile String lastAsrStr;
    private volatile int wakeUpCount;
    private volatile int asrCount;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            int what = message.what;
            switch (what) {
                case 0:
                    //获取ASR
                    String asr = message.obj.toString();

                    break;
                case 1:
                    //唤醒成功
                    float angle = (float) message.obj;
                    int a = (int) angle;
                    L.i("唤醒角度："+angle);
                    wakeUpByAngle(a);
                    handler.sendEmptyMessageDelayed(CLEAR_ALL,3000);
                    //播放唤醒声
                    soundPool.play(soundid, 1.0f, 1.0f, 0, 0, 1.0f);
                    //mWkpCountTxv.setText(message.arg1 + "");
                    break;
                case 2:

                    break;
                default:
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        asrTxv = (TextView) findViewById(R.id.asr_result);
        fitme_result = (TextView) findViewById(R.id.fitme_result);
        lastAsrTxv = (TextView) findViewById(R.id.last_asr_result);
        vadTxv = (TextView) findViewById(R.id.vad_result);
        countTv = (TextView) findViewById(R.id.count);

        //root
        String apkRoot = "chmod 777 "+getPackageCodePath();

        L.i("root_hy"+SystemManager.RootCommand(apkRoot));

        initUI();
        wordsToVoice = new WordsToVoice(MainActivity.this);
        app = (MyApplication) getApplication();
        new BLControl.LoginWithoutNameTask(app).execute();    //初始化博联
        blControl = new BLControl();
        yeelightStripeMap = new HashMap<String, YeelightStripeIpAndPortBean>();
        YeelightControl.getInstance(app,iGetYeelight).searchDevice();    //初始化Yeelight
        //绑定设备
        bindDevice();

        //测试，博联云云对接
        broadlinkAcount();

        //初始化短音效
        soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        soundid = soundPool.load(MainActivity.this, R.raw.wake_up, 1);
        //音量控制,初始化定义
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        //最大音量
        maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        L.i("最大音量"+maxVolume);


        //注册广播接收器
        musicReceiver=new MusicReceiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction("fitme.ai.service.MusicPlayerService");
        MainActivity.this.registerReceiver(musicReceiver,filter);

        if (FileConfig.checkConfigFile(getApplicationContext())) {
            baseCallBack = new BaseCallBack();
            saiAPI_wrap = new saiAPI_wrap();

            //
            saiAPI_wrap.enable_wave_commun(true);
            DirectorBaseMsg directorBaseMsg = new DirectorBaseMsg();


            int initApi = saiAPI_wrap.init_system(WAKE_UP_THRESHOLD_VALUE, "/sdcard/sai_config", baseCallBack);
            int start = saiAPI_wrap.start_service();
            Log.d(TAG, "onCreate: " + "initApi=" + initApi + "," + "start=" + start);

            timeCounter = new TimeCounter(TIMEOUT_MILLIS, INTERVAL);
        } else {
            Log.e(TAG, "There is a problem with the sai_config file,please check it!");
            System.exit(-1);
        }
    }



    private List<BLFamilyModuleInfo> blFamilyModuleInfo;

    //博联账号登录，实现账号打通
    private void broadlinkAcount(){
        new BLLoginTask("15308630310","mayday3591657").execute();
    }

    //博联登录
    private class BLLoginTask extends AsyncTask<URL,Integer,BLLoginResult>{

        private String account;
        private String password;

        public BLLoginTask(String account,String password){
            this.account = account;
            this.password = password;
        }

        @Override
        protected BLLoginResult doInBackground(URL... params) {
            BLLoginResult blLoginResult = BLLet.Account.login(account,password);
            return blLoginResult;
        }

        @Override
        protected void onPostExecute(BLLoginResult blLoginResult) {
            super.onPostExecute(blLoginResult);
            L.i("博联登录结果："+blLoginResult.getMsg());
            if ("ok".equals(blLoginResult.getMsg().trim())){
                new BLGetFamilyIdTask().execute();
            }
        }
    }

    //博联获取家庭info
    private class BLGetFamilyIdTask extends AsyncTask<URL,Integer,BLFamilyIdListGetResult>{

        @Override
        protected BLFamilyIdListGetResult doInBackground(URL... params) {
            BLFamilyIdListGetResult blFamilyIdListGetResult = BLLet.Family.queryLoginUserFamilyIdList();
            return blFamilyIdListGetResult;
        }

        @Override
        protected void onPostExecute(BLFamilyIdListGetResult blFamilyIdListGetResult) {
            super.onPostExecute(blFamilyIdListGetResult);
            Gson gson = new Gson();
            L.i("博联家庭id列表："+gson.toJson(blFamilyIdListGetResult));
            L.i("familyId:"+blFamilyIdListGetResult.getIdInfoList().get(0).getFamilyId());
            String familyId = blFamilyIdListGetResult.getIdInfoList().get(0).getFamilyId();
            new BLGetFamilyInfoTask(familyId).execute();
        }
    }

    //博联获取家庭info
    private class BLGetFamilyInfoTask extends AsyncTask<URL,Integer,BLAllFamilyInfoResult>{
        private String familyId;

        public BLGetFamilyInfoTask(String familyId){
            this.familyId = familyId;
        }

        @Override
        protected BLAllFamilyInfoResult doInBackground(URL... params) {
            BLAllFamilyInfoResult blAllFamilyInfoResult = BLLet.Family.queryAllFamilyInfos(new String[]{familyId});
            return blAllFamilyInfoResult;
        }

        @Override
        protected void onPostExecute(BLAllFamilyInfoResult blAllFamilyInfoResult) {
            super.onPostExecute(blAllFamilyInfoResult);
            Gson gson1 = new Gson();
            L.logE("模块信息："+gson1.toJson(blAllFamilyInfoResult.getAllInfos().get(0).getModuleInfos()));
            blFamilyModuleInfo = new LinkedList<BLFamilyModuleInfo>();
            for (int i=0;i<blAllFamilyInfoResult.getAllInfos().get(0).getModuleInfos().size();i++){
                blFamilyModuleInfo.add(blAllFamilyInfoResult.getAllInfos().get(0).getModuleInfos().get(i));
                L.i("博联设备名："+blAllFamilyInfoResult.getAllInfos().get(0).getModuleInfos().get(i).getName()+"  模块类型："+blAllFamilyInfoResult.getAllInfos().get(0).getModuleInfos().get(i).getModuleType());
                L.i("模块的dev："+blAllFamilyInfoResult.getAllInfos().get(0).getModuleInfos().get(i).getModuleDevs().get(0).getContent());

            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        saiAPI_wrap.stop_service();
        saiAPI_wrap.terminate_system();
        Process.killProcess(Process.myPid());
    }

    private void initUI() {
        ivLight1 = (ImageView) findViewById(R.id.iv_light_1);
        ivLight2 = (ImageView) findViewById(R.id.iv_light_2);
        ivLight3 = (ImageView) findViewById(R.id.iv_light_3);
        ivLight4 = (ImageView) findViewById(R.id.iv_light_4);
        ivLight5 = (ImageView) findViewById(R.id.iv_light_5);
        ivLight6 = (ImageView) findViewById(R.id.iv_light_6);
        ivLight7 = (ImageView) findViewById(R.id.iv_light_7);
        ivLight8 = (ImageView) findViewById(R.id.iv_light_8);
        ivLight9 = (ImageView) findViewById(R.id.iv_light_9);
        ivLight10 = (ImageView) findViewById(R.id.iv_light_10);
        ivLight11 = (ImageView) findViewById(R.id.iv_light_11);
        ivLight12 = (ImageView) findViewById(R.id.iv_light_12);
        ivLightList.add(ivLight1);
        ivLightList.add(ivLight2);
        ivLightList.add(ivLight3);
        ivLightList.add(ivLight4);
        ivLightList.add(ivLight5);
        ivLightList.add(ivLight6);
        ivLightList.add(ivLight7);
        ivLightList.add(ivLight8);
        ivLightList.add(ivLight9);
        ivLightList.add(ivLight10);
        ivLightList.add(ivLight11);
        ivLightList.add(ivLight12);

        bt1 = (Button) findViewById(R.id.bt_1);
        bt2 = (Button) findViewById(R.id.bt_2);
        bt3 = (Button) findViewById(R.id.bt_3);
        bt4 = (Button) findViewById(R.id.bt_4);
        bt5 = (Button) findViewById(R.id.bt_5);
        bt6 = (Button) findViewById(R.id.bt_6);
        bt7 = (Button) findViewById(R.id.bt_7);
        bt8 = (Button) findViewById(R.id.bt_8);
    }

    /**
     UI相关
     *
     */

    public void click(View view){
        switch (view.getId()){
            case R.id.bt_1:
                //开机
                powerOn(R.drawable.cycler_shape_blue);
                //clearAllLightToColor(R.drawable.cycler_shape_blue);
                break;
            case R.id.bt_2:
                //唤醒
                //saiAPI_wrap.set_talking_angle(1.0f);
                saiAPI_wrap.set_wake_status(true);
                //saiAPI_wrap.set_wake_status(true);
                //测试清除缓存
                //CleanMessageUtil.clearAllCache(getApplicationContext());
                break;
            case R.id.bt_3:
                //配置
                configWIFI(R.drawable.cycler_shape_white);

                break;
            case R.id.bt_4:
                //关机
                powerOff(R.drawable.cycler_shape_red);
                //重启
                /*try {
                    Process proc = Runtime.getRuntime().exec("su -c reboot");
                } catch (IOException e) {
                    e.printStackTrace();
                }*/

                //关机
                try {
                    java.lang.Process proc = Runtime.getRuntime().exec("su -c reboot -p");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                break;
            case R.id.bt_5:
                //休眠
                TTSLight();
                break;
            case R.id.bt_6:
                //加载
                loadingLight(R.drawable.cycler_shape_white);
                break;
            case R.id.bt_7:
                //音量+
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                //当前音量
                currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                L.i("当前音量"+currentVolume);
                setVolumeLight(currentVolume);
                break;
            case R.id.bt_8:
                //音量-
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                //当前音量
                currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                L.i("当前音量"+currentVolume);
                setVolumeLight(currentVolume);
                break;
            case R.id.bt_9:
                //结束唤醒
                saiAPI_wrap.set_wake_status(false);
                break;
            case R.id.bt_10:
                //测试打开灯带
                Set<String> get = yeelightStripeMap.keySet();
                for (String test:get){
                    YeelightStripeIpAndPortBean yeelightStripeIpAndPortBean = yeelightStripeMap.get(test);
                    String ip = yeelightStripeIpAndPortBean.getIp();
                    String port = yeelightStripeIpAndPortBean.getPort();
                    YeelightControl.getInstance(app,iGetYeelight).connectAndControl(ip,Integer.parseInt(port),true,-1,-1,-1); //全开
                }
                break;
            case R.id.bt_11:
                //测试关上灯带
                Set<String> get1 = yeelightStripeMap.keySet();
                for (String test:get1){
                    YeelightStripeIpAndPortBean yeelightStripeIpAndPortBean = yeelightStripeMap.get(test);
                    String ip = yeelightStripeIpAndPortBean.getIp();
                    String port = yeelightStripeIpAndPortBean.getPort();
                    YeelightControl.getInstance(app,iGetYeelight).connectAndControl(ip,Integer.parseInt(port),false,-1,-1,-1); //全开
                }
                break;
            case R.id.bt_12:
                //测试调色
                Set<String> get2 = yeelightStripeMap.keySet();
                for (String test:get2){
                    YeelightStripeIpAndPortBean yeelightStripeIpAndPortBean = yeelightStripeMap.get(test);
                    String ip = yeelightStripeIpAndPortBean.getIp();
                    String port = yeelightStripeIpAndPortBean.getPort();
                    YeelightControl.getInstance(app,iGetYeelight).connectAndControl(ip,Integer.parseInt(port),false,-1, 0, -1); //全开
                }
                break;
            case R.id.bt_13:
                /*long ledModel = Integer.parseInt("00111111111110110110110110110110",2);      //二进制转10进制,第0位置设成0表示亮绿灯，第1位置设成0表示亮蓝灯，第2位置设成0表示亮红灯
                saiAPI_wrap.set_led_lights(ledModel);*/


                //测试触摸板
                startActivity(new Intent(MainActivity.this,TouchTabletTestActivity.class));
                break;
            case R.id.bt_14:
                saiAPI_wrap.set_led_mode(0);
                break;
            case R.id.bt_15:
                saiAPI_wrap.set_led_mode(1);
                break;
            case R.id.bt_16:
                blControl.dnaControlSet("0000000000000000000034ea34f63fdf","1","pwr");
                break;
            default:
                break;
        }
    }

    //开机
    private void powerOn(final int resColorId){
        new Thread(){
            @Override
            public void run() {
                super.run();
                for (int i=0;i<ivLightList.size();i++){
                    try {
                        final int finalI = i;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ivLightList.get(finalI).setAlpha(1f);
                                ivLightList.get(finalI).setImageResource(resColorId);
                            }
                        });
                        sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i=0;i<ivLightList.size();i++){

                            ObjectAnimator alpha = ObjectAnimator.ofFloat(ivLightList.get(i),"alpha",1f,0f,1f);
                            alpha.setDuration(1000);
                            alpha.setRepeatCount(2);
                            alpha.start();
                        }
                    }
                });

            }
        }.start();
    }


    //关机
    private void powerOff(int resColorId){
        for (int i=0;i<ivLightList.size();i++){
            ivLightList.get(i).setImageResource(resColorId);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(ivLightList.get(i),"alpha",0f,1f,0f);
            alpha.setDuration(2000);
            alpha.setRepeatCount(1);
            alpha.start();
        }
    }

    //关机
    private void configWIFI(int resColorId){
        for (int i=0;i<ivLightList.size();i++){
            ivLightList.get(i).setImageResource(resColorId);
            ObjectAnimator alpha = ObjectAnimator.ofFloat(ivLightList.get(i),"alpha",0f,1f,0f);
            alpha.setDuration(2000);
            alpha.setRepeatCount(1);
            alpha.start();
        }
    }

    //根据唤醒的角度亮灯
    private void wakeUpByAngle(int angle){
        if (angle>=0&&angle<30){
            ivLight1.setImageResource(R.drawable.cycler_shape_green);
            ivLight2.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=30&&angle<60){
            ivLight2.setImageResource(R.drawable.cycler_shape_green);
            ivLight3.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=60&&angle<90){
            ivLight3.setImageResource(R.drawable.cycler_shape_green);
            ivLight4.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=90&&angle<120){
            ivLight4.setImageResource(R.drawable.cycler_shape_green);
            ivLight5.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=120&&angle<150){
            ivLight5.setImageResource(R.drawable.cycler_shape_green);
            ivLight6.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=150&&angle<180){
            ivLight6.setImageResource(R.drawable.cycler_shape_green);
            ivLight7.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=180&&angle<210){
            ivLight7.setImageResource(R.drawable.cycler_shape_green);
            ivLight8.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=210&&angle<240){
            ivLight8.setImageResource(R.drawable.cycler_shape_green);
            ivLight9.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=240&&angle<270){
            ivLight9.setImageResource(R.drawable.cycler_shape_green);
            ivLight10.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=270&&angle<300){
            ivLight10.setImageResource(R.drawable.cycler_shape_green);
            ivLight11.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=300&&angle<330){
            ivLight11.setImageResource(R.drawable.cycler_shape_green);
            ivLight12.setImageResource(R.drawable.cycler_shape_green);
        }else if (angle>=330&&angle<=360){
            ivLight12.setImageResource(R.drawable.cycler_shape_green);
            ivLight1.setImageResource(R.drawable.cycler_shape_green);
        }
    }

    //所有灯变成一个颜色
    private void clearAllLightToColor(int resColorId){
        for (int i=0;i<ivLightList.size();i++){
            ivLightList.get(i).setImageResource(resColorId);
        }
    }

    //点亮某一灯
    private void lightUp(int index){
        for (int i=0;i<ivLightList.size();i++){
            if (index==i+1){
                ivLightList.get(i).setImageResource(R.drawable.cycler_shape_white);
            }else{
                ivLightList.get(i).setImageResource(R.drawable.cycler_shape_blue);
            }

        }
    }

    //加载灯
    private void loadingLight(final int resColorId){
        new Thread(){
            @Override
            public void run() {
                super.run();
                for (int i=0;i<ivLightList.size();i++){
                    try {
                        final int finalI = i;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                for (int i=0;i<ivLightList.size();i++){
                                    ivLightList.get(finalI).setImageResource(R.drawable.cycler_shape_blue);
                                }
                                ivLightList.get(finalI).setAlpha(1f);
                                ivLightList.get(finalI).setImageResource(resColorId);
                            }
                        });
                        sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    //说话时候的灯光效果
    private void TTSLight(){
        new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    L.i("dooooooooooooooooooooooooo1");
                    sleep(3000);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            L.i("dooooooooooooooooooooooooo2");
                        }
                    });

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }

    //音量灯
    private Thread time;
    private void setVolumeLight(int currentVolume){
        L.i("dddddd"+currentVolume);

        if (time!=null&&time.isAlive()){
            time.interrupt();
        }
        time = new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    sleep(3000);
                    handler.sendEmptyMessage(CLEAR_ALL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        time.start();

        //根据当前音量设置灯光
        if (currentVolume==0||currentVolume==1){

        }else if (currentVolume==2||currentVolume==3){
            ivLight7.setImageResource(R.drawable.cycler_shape_blue);
            ivLight8.setImageResource(R.drawable.cycler_shape_blue);

            ivLight1.setImageResource(R.drawable.cycler_shape_gray);
            ivLight2.setImageResource(R.drawable.cycler_shape_gray);
            ivLight3.setImageResource(R.drawable.cycler_shape_gray);
            ivLight4.setImageResource(R.drawable.cycler_shape_gray);
            ivLight5.setImageResource(R.drawable.cycler_shape_gray);
            ivLight6.setImageResource(R.drawable.cycler_shape_gray);
            ivLight9.setImageResource(R.drawable.cycler_shape_gray);
            ivLight10.setImageResource(R.drawable.cycler_shape_gray);
            ivLight11.setImageResource(R.drawable.cycler_shape_gray);
            ivLight12.setImageResource(R.drawable.cycler_shape_gray);

        }else if (currentVolume==4||currentVolume==5){
            ivLight7.setImageResource(R.drawable.cycler_shape_blue);
            ivLight8.setImageResource(R.drawable.cycler_shape_blue);
            ivLight6.setImageResource(R.drawable.cycler_shape_blue);
            ivLight9.setImageResource(R.drawable.cycler_shape_blue);

            ivLight1.setImageResource(R.drawable.cycler_shape_gray);
            ivLight2.setImageResource(R.drawable.cycler_shape_gray);
            ivLight3.setImageResource(R.drawable.cycler_shape_gray);
            ivLight4.setImageResource(R.drawable.cycler_shape_gray);
            ivLight5.setImageResource(R.drawable.cycler_shape_gray);
            ivLight10.setImageResource(R.drawable.cycler_shape_gray);
            ivLight11.setImageResource(R.drawable.cycler_shape_gray);
            ivLight12.setImageResource(R.drawable.cycler_shape_gray);
        }else if (currentVolume==6||currentVolume==7){
            ivLight7.setImageResource(R.drawable.cycler_shape_blue);
            ivLight8.setImageResource(R.drawable.cycler_shape_blue);
            ivLight6.setImageResource(R.drawable.cycler_shape_blue);
            ivLight9.setImageResource(R.drawable.cycler_shape_blue);
            ivLight5.setImageResource(R.drawable.cycler_shape_blue);
            ivLight10.setImageResource(R.drawable.cycler_shape_blue);

            ivLight1.setImageResource(R.drawable.cycler_shape_gray);
            ivLight2.setImageResource(R.drawable.cycler_shape_gray);
            ivLight3.setImageResource(R.drawable.cycler_shape_gray);
            ivLight4.setImageResource(R.drawable.cycler_shape_gray);
            ivLight11.setImageResource(R.drawable.cycler_shape_gray);
            ivLight12.setImageResource(R.drawable.cycler_shape_gray);

        }else if (currentVolume==8||currentVolume==9){
            ivLight7.setImageResource(R.drawable.cycler_shape_blue);
            ivLight8.setImageResource(R.drawable.cycler_shape_blue);
            ivLight6.setImageResource(R.drawable.cycler_shape_blue);
            ivLight9.setImageResource(R.drawable.cycler_shape_blue);
            ivLight5.setImageResource(R.drawable.cycler_shape_blue);
            ivLight10.setImageResource(R.drawable.cycler_shape_blue);
            ivLight4.setImageResource(R.drawable.cycler_shape_blue);
            ivLight11.setImageResource(R.drawable.cycler_shape_blue);

            ivLight1.setImageResource(R.drawable.cycler_shape_gray);
            ivLight2.setImageResource(R.drawable.cycler_shape_gray);
            ivLight3.setImageResource(R.drawable.cycler_shape_gray);
            ivLight12.setImageResource(R.drawable.cycler_shape_gray);

        }else if (currentVolume==10||currentVolume==11){
            ivLight7.setImageResource(R.drawable.cycler_shape_blue);
            ivLight8.setImageResource(R.drawable.cycler_shape_blue);
            ivLight6.setImageResource(R.drawable.cycler_shape_blue);
            ivLight9.setImageResource(R.drawable.cycler_shape_blue);
            ivLight5.setImageResource(R.drawable.cycler_shape_blue);
            ivLight10.setImageResource(R.drawable.cycler_shape_blue);
            ivLight4.setImageResource(R.drawable.cycler_shape_blue);
            ivLight11.setImageResource(R.drawable.cycler_shape_blue);
            ivLight3.setImageResource(R.drawable.cycler_shape_blue);
            ivLight12.setImageResource(R.drawable.cycler_shape_blue);

            ivLight1.setImageResource(R.drawable.cycler_shape_gray);
            ivLight2.setImageResource(R.drawable.cycler_shape_gray);

        }else if (currentVolume==12||currentVolume==13){
            ivLight7.setImageResource(R.drawable.cycler_shape_blue);
            ivLight8.setImageResource(R.drawable.cycler_shape_blue);
            ivLight6.setImageResource(R.drawable.cycler_shape_blue);
            ivLight9.setImageResource(R.drawable.cycler_shape_blue);
            ivLight5.setImageResource(R.drawable.cycler_shape_blue);
            ivLight10.setImageResource(R.drawable.cycler_shape_blue);
            ivLight4.setImageResource(R.drawable.cycler_shape_blue);
            ivLight11.setImageResource(R.drawable.cycler_shape_blue);
            ivLight3.setImageResource(R.drawable.cycler_shape_blue);
            ivLight12.setImageResource(R.drawable.cycler_shape_blue);
            ivLight2.setImageResource(R.drawable.cycler_shape_blue);
            ivLight1.setImageResource(R.drawable.cycler_shape_blue);
        }else if (currentVolume==14||currentVolume==15){
            ivLight7.setImageResource(R.drawable.cycler_shape_blue);
            ivLight8.setImageResource(R.drawable.cycler_shape_blue);
            ivLight6.setImageResource(R.drawable.cycler_shape_blue);
            ivLight9.setImageResource(R.drawable.cycler_shape_blue);
            ivLight5.setImageResource(R.drawable.cycler_shape_blue);
            ivLight10.setImageResource(R.drawable.cycler_shape_blue);
            ivLight4.setImageResource(R.drawable.cycler_shape_blue);
            ivLight11.setImageResource(R.drawable.cycler_shape_blue);
            ivLight3.setImageResource(R.drawable.cycler_shape_blue);
            ivLight12.setImageResource(R.drawable.cycler_shape_blue);
            ivLight2.setImageResource(R.drawable.cycler_shape_blue);
            ivLight1.setImageResource(R.drawable.cycler_shape_blue);
        }
    }

    private void showUnWakeUp(String reason) {
        runOnUiThread(() -> vadTxv.setText(reason + "进入未唤醒状态"));
    }

    private void showWakeUp() {
        runOnUiThread(() -> vadTxv.setText("进入唤醒状态"));
    }

    private void showWakeUpCount(int count) {
        runOnUiThread(() -> countTv.setText("唤醒次数：" + count));
    }

    private void showAsr(String asr) {
        runOnUiThread(() -> asrTxv.setText(asr));
    }

    private void showLastAsr(String asr) {
        runOnUiThread(() -> lastAsrTxv.setText("上一条：" + asr));
    }



    private class BaseCallBack extends DirectorBaseMsg {

        //唤醒
        @Override
        public void outter_wakeup_now(int wakeup_result, float angle, String s) {
            wakeUpId = JsonPraser.getDialogId(s);
            Log.d(TAG, "outter_wakeup_now : " + wakeUpId);
            ++wakeUpCount;
            asrCount = 0;
            showWakeUp();
            showWakeUpCount(wakeUpCount);
            timeCounter.restart();

            Message msg = Message.obtain();
            msg.what = 1;
            msg.obj = angle;
            mHandler.sendMessageDelayed(msg, 5);
        }

        /*//语音前后端点
        @Override
        public void outter_get_vad_status(int vad_status) {
            Log.d(TAG, "outter_get_vad_status: " + vad_status);
            switch (vad_status) {
                case VAD_END:
                    showUnWakeUp("检测到Vad End。");

                    saiAPI_wrap.set_unwakeup_status();
                    timeCounter.cancel();
                    break;
                case VAD_START_TIME_OUT:
                    showUnWakeUp("Vad前端点超时。");

                    //clearWakeUpId();
                    saiAPI_wrap.set_unwakeup_status();
                    timeCounter.cancel();
                    break;
                case ASR_ERROR:
                    showUnWakeUp("ASR ERROR!");

                    clearWakeUpId();
                    saiAPI_wrap.set_unwakeup_status();
                    timeCounter.cancel();
                    break;
            }
        }*/

        //获取到ASR
        @Override
        public void outter_get_asr(String asr_rslt) {
            Log.d("hy_debug_message", "outter_get_asr: asr_result = " + asr_rslt);

            String dialogId = JsonPraser.getDialogId(asr_rslt);
            /*if (!dialogId.equals(wakeUpId) || ++asrCount > 1) {
                return;
            }*/

            if (!TextUtils.isEmpty(lastAsrStr)) {
                showLastAsr(lastAsrStr);

            }

            String asrStr = JsonPraser.getAsrStr(asr_rslt);

            if (scene(asrStr,"下一首")||scene(asrStr,"换一首")){
                if (currentPlaySongIndex==(musicListSize-1)){
                    currentPlaySongIndex = 0;
                }else {
                    currentPlaySongIndex++;
                }
                playingmusic(MusicPlayerService.NEXT_MUSIC,musicUrl.get(currentPlaySongIndex));
            }else if (scene(asrStr,"上一首")){
                if (currentPlaySongIndex==0){
                    currentPlaySongIndex = musicListSize-1;
                }else {
                    currentPlaySongIndex--;
                }
                playingmusic(MusicPlayerService.NEXT_MUSIC,musicUrl.get(currentPlaySongIndex));
            }else if (scene(asrStr,"声音")&&scene(asrStr,"大")||scene(asrStr,"音量")&&scene(asrStr,"大")){
                //音量+
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                //当前音量
                currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                L.i("当前音量"+currentVolume);
                setVolumeLight(currentVolume);
            }else if (scene(asrStr,"声音")&&scene(asrStr,"小")||scene(asrStr,"音量")&&scene(asrStr,"小")){
                //音量-
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                //当前音量
                currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                L.i("当前音量"+currentVolume);
                setVolumeLight(currentVolume);
            }else if (scene(asrStr,"台灯")&&scene(asrStr,"开")){
                Set<String> get = yeelightStripeMap.keySet();
                for (String test:get){
                    YeelightStripeIpAndPortBean yeelightStripeIpAndPortBean = yeelightStripeMap.get(test);
                    String model = yeelightStripeIpAndPortBean.getModel();
                    L.i("model"+model);
                    String ip = yeelightStripeIpAndPortBean.getIp();
                    String port = yeelightStripeIpAndPortBean.getPort();
                    if ("desklamp".equals(model)){
                        YeelightControl.getInstance(app, iGetYeelight).connectAndControl(ip,Integer.parseInt(port),true,-1,-1,-1); //全开
                    }
                }
            }else if (scene(asrStr,"台灯")&&scene(asrStr,"关")){
                Set<String> get = yeelightStripeMap.keySet();
                for (String test:get){
                    YeelightStripeIpAndPortBean yeelightStripeIpAndPortBean = yeelightStripeMap.get(test);
                    String model = yeelightStripeIpAndPortBean.getModel();
                    String ip = yeelightStripeIpAndPortBean.getIp();
                    String port = yeelightStripeIpAndPortBean.getPort();
                    if ("desklamp".equals(model)){
                        YeelightControl.getInstance(app,iGetYeelight).connectAndControl(ip,Integer.parseInt(port),false,-1,-1,-1);
                    }
                }

            }else if (scene(asrStr,"台灯")&&scene(asrStr,"亮")){
                Set<String> get = yeelightStripeMap.keySet();
                for (String test:get){
                    YeelightStripeIpAndPortBean yeelightStripeIpAndPortBean = yeelightStripeMap.get(test);
                    String model = yeelightStripeIpAndPortBean.getModel();
                    String ip = yeelightStripeIpAndPortBean.getIp();
                    String port = yeelightStripeIpAndPortBean.getPort();
                    if ("desklamp".equals(model)){
                        YeelightControl.getInstance(app,iGetYeelight).connectAndControl(ip,Integer.parseInt(port),false,-1,-1,-1);
                    }
                }

            }else if (scene(asrStr,"灯带")&&scene(asrStr,"开")){
                Set<String> get = yeelightStripeMap.keySet();
                for (String test:get){
                    YeelightStripeIpAndPortBean yeelightStripeIpAndPortBean = yeelightStripeMap.get(test);
                    String model = yeelightStripeIpAndPortBean.getModel();
                    String ip = yeelightStripeIpAndPortBean.getIp();
                    String port = yeelightStripeIpAndPortBean.getPort();
                    if ("stripe".equals(model)){
                        YeelightControl.getInstance(app,iGetYeelight).connectAndControl(ip,Integer.parseInt(port),true,-1,-1,-1);
                    }
                }

            }else if (scene(asrStr,"灯带")&&scene(asrStr,"关")){
                Set<String> get = yeelightStripeMap.keySet();
                for (String test:get){
                    YeelightStripeIpAndPortBean yeelightStripeIpAndPortBean = yeelightStripeMap.get(test);
                    String model = yeelightStripeIpAndPortBean.getModel();
                    String ip = yeelightStripeIpAndPortBean.getIp();
                    String port = yeelightStripeIpAndPortBean.getPort();
                    if ("stripe".equals(model)){
                        YeelightControl.getInstance(app,iGetYeelight).connectAndControl(ip,Integer.parseInt(port),false,-1,-1,-1);
                    }
                }

            }else if (scene(asrStr,"灯带")&&scene(asrStr,"红")){
                Set<String> get = yeelightStripeMap.keySet();
                for (String test:get){
                    YeelightStripeIpAndPortBean yeelightStripeIpAndPortBean = yeelightStripeMap.get(test);
                    String model = yeelightStripeIpAndPortBean.getModel();
                    String ip = yeelightStripeIpAndPortBean.getIp();
                    String port = yeelightStripeIpAndPortBean.getPort();
                    if ("stripe".equals(model)){
                        YeelightControl.getInstance(app, iGetYeelight).connectAndControl(ip, Integer.parseInt(port), false, -1, 0, -1);
                    }
                }

            }else if (scene(asrStr,"灯带")&&scene(asrStr,"绿")){
                Set<String> get = yeelightStripeMap.keySet();
                for (String test:get){
                    YeelightStripeIpAndPortBean yeelightStripeIpAndPortBean = yeelightStripeMap.get(test);
                    String model = yeelightStripeIpAndPortBean.getModel();
                    String ip = yeelightStripeIpAndPortBean.getIp();
                    String port = yeelightStripeIpAndPortBean.getPort();
                    if ("stripe".equals(model)){
                        YeelightControl.getInstance(app, iGetYeelight).connectAndControl(ip, Integer.parseInt(port), false, -1, 110, -1);
                    }
                }

            }else if (scene(asrStr,"灯带")&&scene(asrStr,"蓝")){
                Set<String> get = yeelightStripeMap.keySet();
                for (String test:get){
                    YeelightStripeIpAndPortBean yeelightStripeIpAndPortBean = yeelightStripeMap.get(test);
                    String model = yeelightStripeIpAndPortBean.getModel();
                    String ip = yeelightStripeIpAndPortBean.getIp();
                    String port = yeelightStripeIpAndPortBean.getPort();
                    if ("stripe".equals(model)){
                        YeelightControl.getInstance(app, iGetYeelight).connectAndControl(ip, Integer.parseInt(port), false, -1, 230, -1);
                    }
                }

            }else if (scene(asrStr,"灯带")&&scene(asrStr,"黄")){
                Set<String> get = yeelightStripeMap.keySet();
                for (String test:get){
                    YeelightStripeIpAndPortBean yeelightStripeIpAndPortBean = yeelightStripeMap.get(test);
                    String model = yeelightStripeIpAndPortBean.getModel();
                    String ip = yeelightStripeIpAndPortBean.getIp();
                    String port = yeelightStripeIpAndPortBean.getPort();
                    if ("stripe".equals(model)){
                        YeelightControl.getInstance(app, iGetYeelight).connectAndControl(ip, Integer.parseInt(port), false, -1, 45, -1);
                    }
                }

            }else if (scene(asrStr,"灯带")&&scene(asrStr,"粉")){
                Set<String> get = yeelightStripeMap.keySet();
                for (String test:get){
                    YeelightStripeIpAndPortBean yeelightStripeIpAndPortBean = yeelightStripeMap.get(test);
                    String model = yeelightStripeIpAndPortBean.getModel();
                    String ip = yeelightStripeIpAndPortBean.getIp();
                    String port = yeelightStripeIpAndPortBean.getPort();
                    if ("stripe".equals(model)){
                        YeelightControl.getInstance(app, iGetYeelight).connectAndControl(ip, Integer.parseInt(port), false, -1, 320, -1);
                    }
                }

            }else if (sceneALL(asrStr,blFamilyModuleInfo)){
                L.i("匹配到了博联智慧星的设备");
                for (int i=0;i<blFamilyModuleInfo.size();i++){
                    if (scene(asrStr,blFamilyModuleInfo.get(i).getName())){
                        L.i("博联设备名："+blFamilyModuleInfo.get(i).getName());
                        String did = blFamilyModuleInfo.get(i).getModuleDevs().get(0).getDid();
                        L.i("博联设备did"+did);
                        List<DeviceBean> deviceBeanList = app.getBldnaDeviceList();
                        for (int j=0;j<deviceBeanList.size();j++){
                            if (deviceBeanList.get(j).getDid().equals(did)){
                                //判断设备类型
                                if (blFamilyModuleInfo.get(i).getModuleType()==20){      //自定义面板20
                                    //自定义面板的dev数据
                                    try {
                                        JSONArray array = new JSONArray(blFamilyModuleInfo.get(i).getModuleDevs().get(0).getContent());
                                        for (int k=0;k<array.length();k++){
                                            JSONObject obj = (JSONObject) array.get(k);

                                            JSONArray codeList = obj.getJSONArray("codeList");
                                            JSONObject objCode = (JSONObject) codeList.get(0);
                                            L.i("自定义面板的指令名："+obj.getString("name")+" 指令："+objCode.getString("code"));
                                            if (scene(asrStr,obj.getString("name"))){
                                                //找到指令，发送红外码
                                                blControl.commandRedCodeDevice(objCode.getString("code"),did);

                                            }


                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }

                                }else if (blFamilyModuleInfo.get(i).getModuleType()==1){
                                    if (scene(asrStr,"开")){
                                        blControl.dnaControlSet(did,"1","pwr");
                                    }else if (scene(asrStr,"关")){
                                        blControl.dnaControlSet(did,"0","pwr");
                                    }
                                }else if (blFamilyModuleInfo.get(i).getModuleType()==3){
                                    if (scene(asrStr,"开")){
                                        blControl.dnaControlSet(did,"1","curtain_work");
                                    }else if (scene(asrStr,"关")){
                                        blControl.dnaControlSet(did,"0","curtain_work");
                                    }
                                }

                            }
                        }

                    }
                }
            }
            else if (scene(asrStr,"回家模式")||scene(asrStr,"回家了")){
                blControl.dnaControlSet("0000000000000000000034ea34d06857","1","curtain_work");

            }
            else if (scene(asrStr,"离家模式")||scene(asrStr,"我出门了")){
                blControl.dnaControlSet("0000000000000000000034ea34d06857","0","curtain_work");

            }
            else if (scene(asrStr,"睡眠模式")||scene(asrStr,"我要睡觉了")){
                blControl.dnaControlSet("0000000000000000000034ea34d06857","0","curtain_work");

            }
            else if (scene(asrStr,"起床模式")||scene(asrStr,"我要起床了")){
                blControl.dnaControlSet("0000000000000000000034ea34d06857","1","curtain_work");

            }
            else {
                messageCreat(Mac.getMac(),String.valueOf(1200020190),String.valueOf(302902090),"device_text",asrStr,"13145");
            }
            lastAsrStr = asrStr;

            showAsr(asrStr);
        }
    }



    private void clearWakeUpId() {
        wakeUpId = "";
    }

    private class TimeCounter extends CountDownTimer {

        TimeCounter(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long l) {
            Log.d(TAG, "onTick: " + l);
        }

        @Override
        public void onFinish() {
            Log.d(TAG, "onFinish: Time End");

            showUnWakeUp("未检测到语音");

            clearWakeUpId();
            saiAPI_wrap.set_unwakeup_status();

        }

        void restart() {
            this.cancel();
            this.start();
        }
    }

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            //showTip("开始播放");
            L.i("语音合成回调监听-----------"+"开始播放");
            playingmusic(MusicPlayerService.REDUCE_MUSIC_VOLUME,"");
        }

        @Override
        public void onSpeakPaused() {
            // showTip("暂停播放");
            L.i("语音合成回调监听-----------"+"暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            //showTip("继续播放");
            L.i("语音合成回调监听-----------"+"继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                L.i("语音合成回调监听-----------"+"播放完成");
                playingmusic(MusicPlayerService.RECOVER_MUSIC_VOLUME,"");   //恢复音乐音量
            } else if (error != null) {
                //showTip(error.getPlainDescription(true));
                L.i("语音合成回调监听-------错误----"+error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    //发送客户消息请求
    private void messageCreat(String userId, String x, String y, final String messageType, String content, String password){
        String timeStamp = SignAndEncrypt.getTimeStamp();

        Gson gson = new Gson();
        HashMap<String, Object> params = new HashMap<>();
        params.put("user_id", userId);
        params.put("x", x);
        params.put("y", y);
        params.put("message_type", messageType);
        HashMap<String, Object> map = new HashMap<>();
        map.put("content", content);
        params.put("message_body", map);
        params.put("password",password);
        L.i("---------发出的json-"+gson.toJson(params));

        LinkedHashMap<String, Object> par = new LinkedHashMap<>();
        par.put("method", "message/from_customer/create");
        par.put("api_key", ApiManager.api_key);
        par.put("timestamp", timeStamp);
        par.put("http_body", gson.toJson(params));
        String sign = SignAndEncrypt.signRequest(par, ApiManager.api_secret);
        ApiManager.fitmeApiService.messageCreateVB(ApiManager.api_key, timeStamp, sign,params)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<MessageGet>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        L.i("错误信息："+e.toString());
                        wordsToVoice.startSynthesizer("小秘正在开小差",mTtsListener);

                    }
                    @Override
                    public void onNext(MessageGet messageGet) {
                        /*try {
                            L.logE("json:"+messageGet.string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/

                        L.logE("收到回复的消息:"+new Gson().toJson(messageGet));
                        fitme_result.setText(new Gson().toJson(messageGet));
                        //成功收到回复的消息
                        if (null!=messageGet.getStatus()&&"success".equals(messageGet.getStatus())){
                            L.logE("成功收到回复的消息");
                            if ("text".equals(messageGet.getMessages()[0].getMessage_type())){        //单句回复
                                L.logE("单句回复");
                                wordsToVoice.startSynthesizer(messageGet.getMessages()[0].getMessage_body().getContent(),mTtsListener);
                            } else if ("multiline_text".equals(messageGet.getMessages()[0].getMessage_type())){         //多句回复
                                L.logE("多句回复");
                                doMultilineText(messageGet.getMessages()[0].getMessage_body().getContents());
                            }else if ("task_result".equals(messageGet.getMessages()[0].getMessage_type())){        //控制命令或音乐
                                L.logE("控制命令或音乐");

                                if ("query_music".equals(messageGet.getMessages()[0].getMessage_body().getTask_type())){     //查询音乐
                                    L.logE("查询音乐");
                                    String speechText = messageGet.getMessages()[0].getMessage_body().getTask_result_speech_text();
                                    int musicsNum = messageGet.getMessages()[0].getMessage_body().getTask_result_body().getMusics().size();
                                    musicList = new LinkedList<Music>();
                                    for (int i=0;i<musicsNum;i++){
                                        musicList.add(messageGet.getMessages()[0].getMessage_body().getTask_result_body().getMusics().get(i));
                                    }
                                    //初始化歌单
                                    initMusicList(musicList);
                                    playingmusic(MusicPlayerService.NEXT_MUSIC,musicUrl.get(currentPlaySongIndex));
                                    isPlayingMusic = true;
                                }else if ("command".equals(messageGet.getMessages()[0].getMessage_body().getTask_type())){
                                    //控制
                                    L.logE("控制");
                                    playingmusic(MusicPlayerService.RECOVER_MUSIC_VOLUME,"");   //恢复音乐音量

                                    int devicesLength = messageGet.getMessages()[0].getMessage_body().getTask_result_body().getDevices().size();
                                    for (int i=0;i<devicesLength;i++){
                                        String deviceType = messageGet.getMessages()[0].getMessage_body().getTask_result_body().getDevices().get(i).getDevice_type();
                                        if ("20045".equals(deviceType)){  //杜亚窗帘
                                            L.logE("杜亚窗帘");
                                            //blControl.dnaControlSet("curtain",messageGet.getMessages()[0].getMessage_body().getTask_result_body().getDevices().get(i).getCommand_code(),"curtain_work");
                                            //blControl.curtainControl(Integer.parseInt(messageGet.getMessages()[0].getMessage_body().getTask_result_body().getCommand_code()));
                                        }else if ("10026".equals(deviceType) || "10039".equals(deviceType)){     //RM红外遥控
                                            L.logE("RM红外遥控");
                                            blControl.commandRedCodeDevice(messageGet.getMessages()[0].getMessage_body().getTask_result_body().getDevices().get(i).getCommand_code(),
                                                    messageGet.getMessages()[0].getMessage_body().getTask_result_body().getDevices().get(i).getDid());
                                        }else if ("30014".equals(deviceType)){     //SP系列wifi开关
                                            //blControl.dnaControlSet("sp","1","val");
                                        }else if ("20149".equals(deviceType)){        //四位排插

                                        }
                                    }

                                }else if ("music_command".equals(messageGet.getMessages()[0].getMessage_body().getTask_type())){
                                    commandMusicPlayer(messageGet.getMessages()[0].getMessage_body().getTask_result_body().getCommand());
                                }else if ("tv_command".equals(messageGet.getMessages()[0].getMessage_body().getTask_type())){
                                    //控制电视播放
                                    wordsToVoice.startSynthesizer("正在电视设备上为您播放："+messageGet.getMessages()[0].getMessage_body().getTask_result_body().getFilm_name(),mTtsListener);
                                }else if ("box_command".equals(messageGet.getMessages()[0].getMessage_body().getTask_type())){
                                    if ("next_page".equals(messageGet.getMessages()[0].getMessage_body().getTask_result_body().getCommand())){
                                        //下一页
                                        L.i("电视盒子下一页");
                                        blControl.commandRedCodeDevice(BLControlConstants.TV_BOX_NEXT_PAGE, BLControlConstants.RM_MINI_DID);
                                    }else if ("prev_page".equals(messageGet.getMessages()[0].getMessage_body().getTask_result_body().getCommand())){
                                        //上一页
                                        L.i("电视盒子上一页");
                                        blControl.commandRedCodeDevice(BLControlConstants.TV_BOX_PRE_PAGE, BLControlConstants.RM_MINI_DID);
                                    }
                                }
                            }
                        }


                    }
                });
    }

    /**
     * 获取音乐广播数据
     */
    public class MusicReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            L.i("音乐广播数据:"+intent.getBooleanExtra("next_music",false));
            if (currentPlaySongIndex==(musicListSize-1)){
                currentPlaySongIndex = 0;
            }else {
                currentPlaySongIndex++;
            }
            playingmusic(MusicPlayerService.NEXT_MUSIC,musicUrl.get(currentPlaySongIndex));
        }
    }

    //发送指令到音乐播放的service
    private List<Music> musicList;
    private boolean isPlayingMusic;
    private void playingmusic(int type,String songUrl) {
        //判断是否放新一曲
        if (type== MusicPlayerService.PLAT_MUSIC||type== MusicPlayerService.NEXT_MUSIC){
            String strMusicInfo = musicList.get(currentPlaySongIndex).getSinger()+","+musicList.get(currentPlaySongIndex).getName();
            wordsToVoice.startSynthesizer("正在为您播放："+strMusicInfo,mTtsListener);
        }
        //启动服务，播放音乐
        Intent intent = new Intent(this,MusicPlayerService.class);
        intent.putExtra("type",type);
        intent.putExtra("songUrl",songUrl);
        startService(intent);
    }

    //初始化音乐列表
    private List<String> musicUrl = null;
    private int musicListSize = 0;
    private int currentPlaySongIndex = 0;
    private void initMusicList(List<Music> musicList){
        musicUrl = new LinkedList<>();
        musicListSize = musicList.size();
        currentPlaySongIndex = 0;     //当前播放的歌曲在歌单中的位置
        for (int i=0;i<musicListSize;i++){
            musicUrl.add(musicList.get(i).getSong_url());
            L.i("歌曲URL："+musicUrl.get(i));
        }
    }

    //处理多个回复
    private void doMultilineText(String[] multiline_text){
        wordsToVoice.startSynthesizer(multiline_text[0],mTtsListener);
        //后续还要处理
    }

    //控制音乐播放器
    private void commandMusicPlayer(String command){
        L.i("控制音乐播放器:"+command);
        switch (command){
            case "next":      //下一曲
                if (currentPlaySongIndex==(musicListSize-1)){
                    currentPlaySongIndex = 0;
                }else {
                    currentPlaySongIndex++;
                }
                Toast.makeText(this, "哪一首："+currentPlaySongIndex, Toast.LENGTH_SHORT).show();
                playingmusic(MusicPlayerService.NEXT_MUSIC,musicUrl.get(currentPlaySongIndex));
                break;
            case "prev":      //上一曲
                if (currentPlaySongIndex==0){
                    currentPlaySongIndex = musicListSize-1;
                }else {
                    currentPlaySongIndex--;
                }
                Toast.makeText(this, "哪一首："+currentPlaySongIndex, Toast.LENGTH_SHORT).show();
                playingmusic(MusicPlayerService.NEXT_MUSIC,musicUrl.get(currentPlaySongIndex));
                break;
            case "down":
                L.i("音量减");
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                Toast.makeText(this, "当前音量"+currentVolume, Toast.LENGTH_SHORT).show();
                break;
            case "up":
                L.i("音量加");
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                Toast.makeText(this, "当前音量"+currentVolume, Toast.LENGTH_SHORT).show();
                break;
            case "pause":
                playingmusic(MusicPlayerService.PAUSE_MUSIC,"");
                break;
            case "stop":
                playingmusic(MusicPlayerService.STOP_MUSIC,"");
                break;
            case "play":
                playingmusic(MusicPlayerService.RESUME_MUSIC,"");
                playingmusic(MusicPlayerService.RECOVER_MUSIC_VOLUME,"");   //恢复音乐音量
                break;
            case "max":
                currentVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_PLAY_SOUND);
                break;
            case "mini":
                currentVolume = 0;
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, AudioManager.FLAG_PLAY_SOUND);
                break;
        }
    }


    //yeelight彩光灯带的控制
    public void yeelightStripeControl(JSONObject device) {
        // 2017/9/15 控制yeelight设备方法
        //yeelight彩光灯带的控制
        L.i("***************yeelight彩光灯带的控制******************");
        String command_code = null;
        String did=null;
        try {
            command_code = device.getString("command_code");
            did = device.getString("did");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Iterator<YeelightStripeIpAndPortBean> yeelightIt=yeelightStripeMap.values().iterator();
        L.i("*************************yeelightStripeMap.size()*****"+yeelightStripeMap.size());
        if(did==null||"".equals(did.trim())){
            return;
        }

        YeelightStripeIpAndPortBean yeelightStripeIpAndPortBean=yeelightStripeMap.get(did);

        L.logE("*************did*****"+did);
        Iterator<String> keyIt=yeelightStripeMap.keySet().iterator();
        while(keyIt.hasNext()){
            String yeelightKey=keyIt.next();
            L.logE("********************yeelightStripeMap**key*"+yeelightKey);
            L.logE("********************yeelightStripeMap**key*"+(did.equals(yeelightKey)));
        }
        if(yeelightStripeIpAndPortBean==null){
            return;
        }
        String ip = yeelightStripeIpAndPortBean.getIp();
        String port = yeelightStripeIpAndPortBean.getPort();
        bright = yeelightStripeIpAndPortBean.getBright();
        String ct = yeelightStripeIpAndPortBean.getCt();
        String hue = yeelightStripeIpAndPortBean.getHue();

        //控制部分
        if ("1".equals(command_code)) {
            //开灯
            L.i("********打开灯::" + ip + "::" + port);
            YeelightControl.getInstance(app,iGetYeelight).connectAndControl(ip,Integer.parseInt(port),true,-1,-1,-1);
        } else if ("0".equals(command_code)) {
            //关灯
            L.i("********关闭灯::" + ip + "::" + port);
            YeelightControl.getInstance(app,iGetYeelight).connectAndControl(ip,Integer.parseInt(port),false,-1,-1,-1);
        } else if ("2".equals(command_code)) {
            //调亮
//            L.i("11111111111111111111111111" + bright);
            YeelightControl.getInstance(app, new IGetYeelight() {
                @Override
                public void getInfoList(List<YeelightDeviceBean> templist) {

                }

                @Override
                public void getIpAndDevice(YeelightDeviceBean yeelightDeviceBean) {
                    int brightnew = yeelightDeviceBean.getBright();
                    bright = brightnew;
                }

                @Override
                public void getResponse(String value) {

                }
            }).searchDevice();
            if (bright <= 50) {
                L.i("调亮灯");
                YeelightControl.getInstance(app, iGetYeelight).connectAndControl(ip, Integer.parseInt(port), false, 80, -1, -1);
            }
        } else if ("3".equals(command_code)) {
            //调暗
            L.i("22222222222222222222222222");
            YeelightControl.getInstance(app, new IGetYeelight() {
                @Override
                public void getInfoList(List<YeelightDeviceBean> templist) {

                }

                @Override
                public void getIpAndDevice(YeelightDeviceBean yeelightDeviceBean) {
                    int brightnew = yeelightDeviceBean.getBright();
                    bright = brightnew;
                }

                @Override
                public void getResponse(String value) {

                }
            }).searchDevice();
            if (bright > 50) {
                L.i("调暗灯");
                YeelightControl.getInstance(app, iGetYeelight).connectAndControl(ip, Integer.parseInt(port), false, 30, -1, -1);
            }
        } else if ("4".equals(command_code)) {
            //调成红色
            L.i("调成红色");
            YeelightControl.getInstance(app, iGetYeelight).connectAndControl(ip, Integer.parseInt(port), false, -1, 0, -1);
        } else if ("5".equals(command_code)) {
            //调成绿色
            L.i("调成绿色");
            YeelightControl.getInstance(app, iGetYeelight).connectAndControl(ip, Integer.parseInt(port), false, -1, 110, -1);
        } else if ("6".equals(command_code)) {
            //调成蓝色
            L.i("调成蓝色");
            YeelightControl.getInstance(app, iGetYeelight).connectAndControl(ip, Integer.parseInt(port), false, -1, 230, -1);
        }
    }


    //绑定设备
    private void bindDevice(){

        String mac = Mac.getMac();
        L.i("mac地址为："+mac+"---------"+ NetworkStateUtil.getLocalMacAddressFromWifiInfo(MainActivity.this));

        String timeStamp = SignAndEncrypt.getTimeStamp();
        LinkedHashMap<String, Object> params = new LinkedHashMap<>();
        params.put("method", "account/device/create");
        params.put("api_key", ApiManager.api_key);
        params.put("timestamp", timeStamp);

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        List<LinkedHashMap> devices = new ArrayList<>();

        LinkedHashMap<String, Object> mapDevices = new LinkedHashMap<>();
        mapDevices.put("identifier",mac);
        mapDevices.put("did","");
        mapDevices.put("nickname","声智开发板");
        mapDevices.put("pid","");
        mapDevices.put("mac",mac);
        mapDevices.put("device_name","fitmeSound");
        mapDevices.put("device_lock","");
        mapDevices.put("device_type","");
        mapDevices.put("category","");
        mapDevices.put("command","");
        mapDevices.put("command_code","");
        mapDevices.put("user_group","客厅");
        devices.add(mapDevices);

        map.put("user_id", "1067");  //15308630310的userid:1067     13071860782userid:445
        map.put("devices", devices);

        Gson gson = new Gson();
        params.put("http_body", gson.toJson(map));
        L.i("http_body:"+gson.toJson(map));
        String sign = SignAndEncrypt.signRequest(params, ApiManager.api_secret);
        ApiManager.fitmeApiService.deviceBind(ApiManager.api_key, timeStamp, sign, map)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ResponseBody>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        try {
                            L.i("服务器回复："+responseBody.string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                });
    }

    //正则判断场景
    private boolean scene(String sendMsg,String regEx) {
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(sendMsg);
        // 查找字符串中是否有匹配正则表达式的字符/字符串
        boolean rs = matcher.find();
        //L.i("是否找到该字符："+rs);
        return rs;
    }

    //多正则判断
    private boolean sceneALL(String sendMsg,List<BLFamilyModuleInfo> info){
        boolean isFind = false;
        for (int i=0;i<info.size();i++){
            if (scene(sendMsg,info.get(i).getName())){
                isFind = true;
            }
        }
        return isFind;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode()==KeyEvent.KEYCODE_BACK){
            finish();
            System.exit(0);
        }
        return super.onKeyDown(keyCode, event);
    }
}
