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
import fitme.ai.utils.AudioRecoderUtils;
import fitme.ai.utils.CreateMessageIdUtils;
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
    private Handler asrHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case TIMER:
                    Log.i("result", "handleMessage: "+timer);
                    if (timer==12){
                        timer = 0;
                    }
                    break;
                case CLEAR_ALL:
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

    private AudioRecoderUtils audioRecoderUtils;

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
                    asrHandler.sendEmptyMessageDelayed(CLEAR_ALL,3000);
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

    //当前进行到的网络请求消息的次数,每次发送消息重置
    private int nowTime;
    //定时循环执行任务
    private Handler handler = new Handler();
    //定时处理runnable
    private Runnable task = new Runnable() {
        @Override
        public void run() {
//            if (nowTime < LOOP_TIMES) {
//                mMessageGetPresenter.messageGet(localUser, 5);
//                nowTime += 1;
//                handler.postDelayed(this, 500);
//            }
            L.i("task______________________________nowTime:"+nowTime);
            if (nowTime <= 10) {
                getMessage();
                nowTime += 1;
                handler.postDelayed(this, 300);
            } else if (nowTime > 10 && nowTime <= 17) {
                getMessage();
                nowTime += 1;
                handler.postDelayed(this, 1000);
            } else if (nowTime > 17 && nowTime <= 50) {
                getMessage();
                nowTime += 1;
                handler.postDelayed(this, 3000);
            }
            else{
                getMessage();
                nowTime += 1;
                handler.postDelayed(this, 10000);
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        asrTxv = (TextView) findViewById(R.id.asr_result);
        fitme_result = (TextView) findViewById(R.id.fitme_result);
        lastAsrTxv = (TextView) findViewById(R.id.last_asr_result);
        vadTxv = (TextView) findViewById(R.id.vad_result);
        countTv = (TextView) findViewById(R.id.count);


        initUI();
        wordsToVoice = new WordsToVoice(MainActivity.this);
        app = (MyApplication) getApplication();
        new BLControl.LoginWithoutNameTask(app).execute();    //初始化博联
        blControl = new BLControl();
        yeelightStripeMap = new HashMap<String, YeelightStripeIpAndPortBean>();
        YeelightControl.getInstance(app,iGetYeelight).searchDevice();    //初始化Yeelight
        //绑定设备

        //测试，博联云云对接
        broadlinkAcount();

        //测试，初始化录音
        audioRecoderUtils = new AudioRecoderUtils();
        audioRecoderUtils.setOnAudioStatusUpdateListener(new AudioRecoderUtils.OnAudioStatusUpdateListener(){

            @Override
            public void onUpdate(double db, long time) {
                L.i("分贝："+db+"  time:"+time);
            }

            @Override
            public void onStop(String filePath) {
                L.i("stop");
            }
        });

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
                saiAPI_wrap.set_talking_angle(1.0f);
                saiAPI_wrap.set_wake_status(true);

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
                break;
            case R.id.bt_6:
                //加载
                break;
            case R.id.bt_7:
                //音量+
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_RAISE,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                //当前音量
                currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                L.i("当前音量"+currentVolume);
                break;
            case R.id.bt_8:
                //音量-
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,AudioManager.ADJUST_LOWER,
                        AudioManager.FX_FOCUS_NAVIGATION_UP);
                //当前音量
                currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                L.i("当前音量"+currentVolume);
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
                //测试结束录音
                audioRecoderUtils.stopRecord();
                break;
            case R.id.bt_15:
                saiAPI_wrap.set_led_mode(1);
                //测试开始录音
                audioRecoderUtils.startRecord();
                break;
            case R.id.bt_16:
                //获取麦克风数据
                saiAPI_wrap.enable_wave_commun(true);
                DirectorBaseMsg directorBaseMsg = new DirectorBaseMsg();
                break;
            case R.id.bt_17:
                //测试播放音频
                playingmusic(MusicPlayerService.PLAT_MUSIC,"http://od.open.qingting.fm/vod/00/00/0000000000000000000024144869_24.m4a?u=865&channelId=76206&programId=1318474");  //测试播放一个音频
                break;
            case R.id.bt_18:
                //暂停播放
                playingmusic(MusicPlayerService.PAUSE_MUSIC,"");

                break;
            case R.id.bt_19:
                //继续播放
                mediaContinue(5);
                playingmusic(MusicPlayerService.RESUME_MUSIC,"");
                break;
            case R.id.bt_20:
                //下一曲
                mediaNext(url);
                //playingmusic(MusicPlayerService.NEXT_MUSIC,"http://od.open.qingting.fm/vod/00/00/0000000000000000000024144869_24.m4a?u=865&channelId=76206&programId=1318474");
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
            saiAPI_wrap.set_wake_status(false);
            String dialogId = JsonPraser.getDialogId(asr_rslt);
            /*if (!dialogId.equals(wakeUpId) || ++asrCount > 1) {
                return;
            }*/

            if (!TextUtils.isEmpty(lastAsrStr)) {
                showLastAsr(lastAsrStr);

            }

            String asrStr = JsonPraser.getAsrStr(asr_rslt);
            messageCreat(asrStr);    //发送消息

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
            playingmusic(MusicPlayerService.REDUCE_MUSIC_VOLUME,"");     //减小音乐音量
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

    /**
     * 获取音乐广播数据
     */
    public class MusicReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String mediaPlayerState = intent.getStringExtra("mediaPlayerState");
            L.i("媒体播放器状态:"+mediaPlayerState);
            switch (mediaPlayerState){
                case "pause":
                    double position = intent.getDoubleExtra("currentPosition",0);
                    int i = (int) (position*100);
                    L.i("当前播放位置："+position+"位置:"+i);
                    //暂停时同步播放的位置
                    mediaPause(i);
                    break;
                case "next":
                    L.i("next");
                    mediaNext("url");
                    break;
                case "resume":
                    L.i("resume");
                    double position1 = intent.getDoubleExtra("currentPosition",0);
                    int i1 = (int) (position1*100);
                    L.i("当前播放位置："+position1+"位置:"+i1);
                    mediaContinue(i1);
                    break;
            }
        }
    }

    //发送指令到音乐播放的service
    private void playingmusic(int type,String songUrl) {
        //启动服务，播放音乐
        Intent intent = new Intent(this,MusicPlayerService.class);
        intent.putExtra("type",type);
        intent.putExtra("songUrl",songUrl);
        startService(intent);
    }


    private String userId = "36";
    private String deviceId = "36";
    private String token = "";

    //用户新增消息
    private void messageCreat(String speech){
        String timeStamp = SignAndEncrypt.getTimeStamp();
        HashMap<String, Object> params = new HashMap<>();
        params.put("method", "message/from_user/create");
        params.put("api_key", ApiManager.dialog_manage_api_key);
        params.put("timestamp", timeStamp);
        params.put("version",ApiManager.VERSION);

        HashMap<String, Object> map = new HashMap<>();
        map.put("message_id", CreateMessageIdUtils.getMessageId(userId,this));
        map.put("user_id", userId);
        map.put("token", token);
        map.put("device_id", deviceId);
        map.put("x", 1200002200);
        map.put("y", 302873830);
        map.put("speech", speech);
        map.put("member_id", "");
        map.put("intent", "");
        map.put("slots", null);

        Gson gson = new Gson();
        params.put("http_body",gson.toJson(map));

        String sign = SignAndEncrypt.signRequest(params, ApiManager.dialog_manage_api_secret);
        ApiManager.DialogManagerService.messageCreate(ApiManager.dialog_manage_api_key, timeStamp,sign,ApiManager.VERSION,map)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ResponseBody>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        L.i("e:"+e.toString());
                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        try {
                            String res = responseBody.string();
                            L.i("消息res:"+res);
                            JSONObject object = new JSONObject(res);
                            String status = object.getString("status");
                            L.i("status:"+status);
                            if ("success".equals(status)){
                                //请求成功，将当前请求的次数设置为0
                                L.i("请求成功!!!");
                                nowTime = 0;
                                handler.removeCallbacks(task);
                                handler.post(task);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    String url = "";
    //用户新增消息
    private void getMessage(){
        String timeStamp = SignAndEncrypt.getTimeStamp();
        HashMap<String, Object> params = new HashMap<>();
        params.put("method", "message/to_user");
        params.put("api_key", ApiManager.notification_api_key);
        params.put("timestamp", timeStamp);
        params.put("version",ApiManager.VERSION);

        HashMap<String, Object> map = new HashMap<>();
        map.put("user_id", userId);
        map.put("device_id", deviceId);
        map.put("token", token);
        map.put("max_count", 1);

        Gson gson = new Gson();
        params.put("http_body",gson.toJson(map));

        String sign = SignAndEncrypt.signRequest(params, ApiManager.notification_api_secret);
        ApiManager.NotificationService.getMessage(ApiManager.notification_api_key, timeStamp,sign,ApiManager.VERSION,map)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ResponseBody>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        L.i("e:"+e.toString());
                    }

                    @Override
                    public void onNext(ResponseBody messageGet) {
                        try {
                            String res = messageGet.string();
                            L.i("messageGEt:"+res);
                            JSONObject object = new JSONObject(res);
                            JSONArray array = object.getJSONArray("messages");
                            for (int i=0;i<array.length();i++){
                                JSONObject message = array.getJSONObject(i);
                                String messageId = message.getString("message_id");
                                L.i("messageId:"+messageId);
                                messageArrived(messageId);
                            }
                            JSONObject message = array.getJSONObject(0);
                            String message_type = message.getString("message_type");
                            L.i("message_type:"+message_type);
                            if ("api_call_action".equals(message_type)){
                                //播放url
                                JSONObject message_body = message.getJSONObject("message_body");
                                JSONObject slots = message_body.getJSONObject("slots");
                                url = slots.getString("url");
                                playingmusic(MusicPlayerService.PLAT_MUSIC,url);
                            }else if ("speech/url_img_title_h1".equals(message_type)){
                                //取speach
                                String speech = message.getJSONObject("message_body").getString("speech");
                                fitme_result.setText(speech);
                                wordsToVoice.startSynthesizer(speech,mTtsListener);
                                Toast.makeText(MainActivity.this, speech, Toast.LENGTH_LONG).show();
                                L.i("取speach");
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    //用户确认消息到达
    private void messageArrived(String messageId){
        String timeStamp = SignAndEncrypt.getTimeStamp();
        HashMap<String, Object> params = new HashMap<>();
        params.put("method", "message_arrived/create");
        params.put("api_key", ApiManager.notification_api_key);
        params.put("timestamp", timeStamp);
        params.put("version",ApiManager.VERSION);

        HashMap<String, Object> map = new HashMap<>();
        map.put("message_id", messageId);
        map.put("user_id", userId);
        map.put("token", token);

        Gson gson = new Gson();
        params.put("http_body",gson.toJson(map));

        String sign = SignAndEncrypt.signRequest(params, ApiManager.notification_api_secret);
        ApiManager.NotificationService.messageArrived(ApiManager.notification_api_key, timeStamp,sign,ApiManager.VERSION,map)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ResponseBody>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        L.i("e:"+e.toString());
                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        try {
                            String res = responseBody.string();
                            L.i("res:"+res);
                            JSONObject object = new JSONObject(res);
                            String status = object.getString("status");
                            if ("success".equals(status)){

                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }


    //切换下一曲
    private void mediaNext(String perUrl){
        HashMap<String, Object> map = new HashMap<>();
        map.put("user_id", "36");
        map.put("device_id", "36");
        map.put("url", perUrl);

        ApiManager.MediaPlayerService.mediaNext(map)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ResponseBody>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        L.i("e:"+e.toString());
                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        try {
                            String res = responseBody.string();
                            L.i("mediaNext:"+res);
                            JSONObject object = null;
                            try {
                                object = new JSONObject(res);
                                if (object.getInt("code")==200){
                                    url = object.getJSONObject("data").getString("url");
                                    playingmusic(MusicPlayerService.PLAT_MUSIC,url);

                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    //切换上一曲
    private void mediaPlayPrevious(String url){
        HashMap<String, Object> map = new HashMap<>();
        map.put("user_id", "36");
        map.put("device_id", "36");
        map.put("url", "http://od.open.qingting.fm/m4a/5a13a8667cb8914777e63dce_8254257_64.m4a?u=865&channelId=231024&programId=8087152");

        ApiManager.MediaPlayerService.mediaPlayPrevious(map)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ResponseBody>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        L.i("e:"+e.toString());
                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        try {
                            String res = responseBody.string();
                            L.i("mediaNext:"+res);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    //上传暂停播放状态
    private void mediaPause(int position){
        HashMap<String, Object> map = new HashMap<>();
        map.put("user_id", "36");
        map.put("device_id", "36");
        map.put("position", position);

        ApiManager.MediaPlayerService.mediaPause(map)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ResponseBody>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        L.i("e:"+e.toString());
                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        try {
                            String res = responseBody.string();
                            L.i("mediaPause:"+res);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }


    //继续播放
    private void mediaContinue(int position){
        HashMap<String, Object> map = new HashMap<>();
        map.put("user_id", "36");
        map.put("device_id", "36");
        map.put("position", position);

        ApiManager.MediaPlayerService.mediaContinue(map)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ResponseBody>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        L.i("e:"+e.toString());
                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        try {
                            String res = responseBody.string();
                            L.i("mediaPause:"+res);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
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
