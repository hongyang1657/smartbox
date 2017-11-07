package fitme.ai.setting.api;


import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by blw on 2016/8/26.
 */
public class ApiManager {
    //一代fitme的api访问基础地址
    //外网
    private static final String FITMEURL="http://app.fitme.ai:7001";

    //private static final String FITMEURL = "http://172.16.12.195:7001";


    //内网,mingNan
    //二代Fitme的api访问接口地址
    //对话管理器
    private static final String DIALOG_MANAGER_URL = "http://172.16.12.195:7050";
    //消息推送服务
    private static final String NOTIFICATION_URL = "http://172.16.12.151:12345";


    //ApiKey
    public static final String api_key = "testkey";
    //ApiSecret
    public static final String api_secret = "fb78fceaf5d445409610398d83088533";
    //okhttp客户端
    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .readTimeout(15, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .build();
    //创建用于fitme的api的retrofit服务接口
    private static final Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(FITMEURL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .build();
    //设置成公共方法让model访问
    public static final ApiService fitmeApiService = retrofit.create(ApiService.class);


    //二代对话管理器retrofit服务接口
    private static final Retrofit retrofit_dialog_manager = new Retrofit.Builder()
            .baseUrl(DIALOG_MANAGER_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .build();
    //设置成公共方法让model访问
    public static final ApiService DialogManagerService = retrofit_dialog_manager.create(ApiService.class);


    //二代消息推送服务retrofit服务接口
    private static final Retrofit retrofit_notification = new Retrofit.Builder()
            .baseUrl(NOTIFICATION_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
            .build();
    //设置成公共方法让model访问
    public static final ApiService NotificationService = retrofit_notification.create(ApiService.class);

}
