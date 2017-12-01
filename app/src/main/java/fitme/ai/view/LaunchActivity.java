package fitme.ai.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.google.gson.Gson;
import com.stericson.RootTools.RootTools;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;

import fitme.ai.MyApplication;
import fitme.ai.R;
import fitme.ai.bean.TokenInfo;
import fitme.ai.setting.api.ApiManager;
import fitme.ai.setting.api.HttpConstant;
import fitme.ai.utils.L;
import fitme.ai.utils.NetworkStateUtil;
import fitme.ai.utils.PhoneUtils;
import fitme.ai.utils.SignAndEncrypt;
import fitme.ai.utils.WordsToVoice;
import okhttp3.ResponseBody;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by hongy on 2017/9/18.
 */

public class LaunchActivity extends Activity{

    private MyApplication application;
    private Intent intent;
    private Thread thread;
    private boolean tomain = true;

    //讯飞TTS
    private WordsToVoice wordsToVoice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        initAndroidFunctionState();
        //init();
    }

    private void init(){

        thread = new Thread(){
            @Override
            public void run() {
                super.run();
                try {
                    sleep(3000);
                    if (tomain){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                intent = new Intent(LaunchActivity.this,MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    public void launchClick(View v){
        switch (v.getId()){
            case R.id.bt_to_main:
                tomain = false;
                intent = new Intent(this,MainActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.bt_to_config:
                tomain = false;
                //intent = new Intent(this,ConfigActivity.class);
                startActivity(intent);
                finish();
                break;
        }
    }

    private void initAndroidFunctionState(){
        //判断网络状态
        if (!NetworkStateUtil.isNetworkAvailable(this)){
            L.i("当前没网络");
            wordsToVoice = new WordsToVoice(LaunchActivity.this);
            wordsToVoice.startSynthesizer("未连接网络",null);
            @SuppressLint("WifiManagerLeak") final WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            if(wifiManager != null && wifiManager.getWifiState()==wifiManager.WIFI_STATE_DISABLED){


                /*L.i("开启wifi");
                wifiManager.setWifiEnabled(true);*/

                /*new Thread(){
                    @Override
                    public void run() {
                        super.run();
                        while (isWifiClose){
                            try {
                                sleep(1000);
                                if (wifiManager.getWifiState()==wifiManager.WIFI_STATE_ENABLED){
                                    isWifiClose = false;
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        *//*L.i("连接指定的网络");
                        WifiAutoConnectManager wifiAutoConnectManager = new WifiAutoConnectManager(wifiManager);
                        wifiAutoConnectManager.connect("FITME-GUEST","28230152", WifiAutoConnectManager.WifiCipherType.WIFICIPHER_WPA);*//*
                    }
                }.start();*/
            }else if (wifiManager != null && wifiManager.getWifiState()==wifiManager.WIFI_STATE_ENABLED){
                /*L.i("连接指定的网络");
                WifiAutoConnectManager wifiAutoConnectManager = new WifiAutoConnectManager(wifiManager);
                wifiAutoConnectManager.connect("FITME-GUEST","28230152", WifiAutoConnectManager.WifiCipherType.WIFICIPHER_WPA);*/
            }
        }else {
            //有网络,初始化其他组件
            L.i("有网络");
            wordsToVoice = new WordsToVoice(LaunchActivity.this);
            wordsToVoice.startSynthesizer("欢迎回来，想我了吗？",null);
            PhoneUtils.init(this);
            getUserId("15308630310");
        }
    }

    //根据手机号获取用户id
    private void getUserId(String mobile){
        String timeStamp = SignAndEncrypt.getTimeStamp();
        HashMap<String, Object> params = new HashMap<>();
        params.put("method", "get_user_id_by_mobile");
        params.put("api_key", ApiManager.user_center_api_key);
        params.put("timestamp", timeStamp);
        params.put("version",ApiManager.VERSION);

        HashMap<String, Object> map = new HashMap<>();
        map.put("mobile", mobile);
        Gson gson = new Gson();
        params.put("http_body",gson.toJson(map));

        String sign = SignAndEncrypt.signRequest(params, ApiManager.user_center_api_secret);
        ApiManager.UserCenterService.getUserIdByMobile(ApiManager.user_center_api_key, timeStamp,sign,ApiManager.VERSION,map)
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
                            L.i("res:"+responseBody.string());
                            JSONObject object = new JSONObject(responseBody.string());
                            String userId = object.getString("user_id");
                            getToken(userId,"3591657");
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    //登录获取token
    private void getToken(String userId,String password){

        String timeStamp = SignAndEncrypt.getTimeStamp();
        String apikey= ApiManager.authorization_api_key;
        String apiSecret= ApiManager.authorization_api_secret;
        //MD5非对称加密
        final String asymmetric = SignAndEncrypt.asymmetricEncryptMd5(password,apiSecret, timeStamp);
        HashMap<String, Object> params = new HashMap<>();
        params.put("method", HttpConstant.METHOD_AT_TOKEN);
        params.put("api_key", apikey);
        params.put("timestamp", timeStamp);
        params.put("version", HttpConstant.API_VERSION);
        HashMap<String, Object> map = new HashMap<>();
        map.put("user_id", userId);
        map.put("password", asymmetric);
        Gson gson = new Gson();
        params.put("http_body", gson.toJson(map));
        String sign = SignAndEncrypt.signRequest(params, apiSecret);
        ApiManager.AuthorizationService.token(apikey, timeStamp, sign,HttpConstant.API_VERSION, HttpConstant.METHOD_AT_TOKEN,map)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<TokenInfo>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                    }
                    @Override
                    public void onNext(TokenInfo jsonObject) {
                        L.logE("TokenModel:"+jsonObject.toString());
                        deviceInfoUpLoad(userId,"");
                    }
                });
    }

    //上传交互设备
    private void deviceInfoUpLoad(String userId,String token){
        /*if(StringUtils.isEmpty(userInfo.getUser_id())){
            return;
        }
        if(StringUtils.isEmpty(PhoneUtils.getInfo().getDeviceId())){
            return;
        }*/
        String timeStamp = SignAndEncrypt.getTimeStamp();
        String apikey= "user_center";
        String apiSecret= "ga78fceaf5d445409610398d83088528";
        HashMap<String, Object> params = new HashMap<>();
        params.put("method", HttpConstant.METHOD_UC_DEVICE_PUT);
        params.put("api_key", apikey);
        params.put("timestamp", timeStamp);
        params.put("version", HttpConstant.API_VERSION);
        HashMap<String, Object> map = new HashMap<>();
        map.put("user_id", userId);
        map.put("token", token);
        L.i("上传的device_id："+PhoneUtils.getInfo().getDeviceId());
        map.put("device_id", PhoneUtils.getInfo().getDeviceId());
        map.put("device_type","smart_speaker");

        Gson gson = new Gson();
        params.put("http_body", gson.toJson(map));
        L.logE("______deviceInfoUpload:"+ gson.toJson(map));
        String sign = SignAndEncrypt.signRequest(params, apiSecret);
        ApiManager.UserCenterService.deviceInfoUpload(apikey, timeStamp, sign,HttpConstant.API_VERSION, HttpConstant.METHOD_UC_DEVICE_PUT,map)
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
                    public void onNext(ResponseBody jsonObject) {
                        try {
                            L.i("上传设备类型："+jsonObject.string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

}
