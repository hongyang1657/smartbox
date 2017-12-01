package fitme.ai.setting.api;



import java.util.Map;

import fitme.ai.bean.TokenInfo;
import okhttp3.ResponseBody;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by blw on 2016/8/26.
 */
public interface ApiService {



    //设备绑定,绑定智能音箱
    @POST("account/device/create")
    Observable<ResponseBody> deviceBind(@Query("api_key") String apiKey,
                                        @Query("timestamp") String timestamp,
                                        @Query("sign") String sign,
                                        @Body Map<String, Object> http_body);


    /**
     * 二代接口方法
     */

    //根据手机号获取用户id
    @POST("get_user_id_by_mobile")
    Observable<ResponseBody> getUserIdByMobile(@Query("api_key") String apiKey,
                                               @Query("timestamp") String timestamp,
                                               @Query("sign") String sign,
                                               @Query("version") String version,
                                               @Body Map<String, Object> http_body);

    //登录功能，即获取用户信息功能
    @POST("token")
    Observable<TokenInfo> token(@Query("api_key") String apiKey,
                                @Query("timestamp") String timestamp,
                                @Query("sign") String sign,
                                @Query("version") String version,
                                @Query("method") String method,
                                @Body Map<String, Object> http_body);

    //新增用户消息
    @POST("message/from_user/create")
    Observable<ResponseBody> messageCreate(@Query("api_key") String apiKey,
                                           @Query("timestamp") String timestamp,
                                           @Query("sign") String sign,
                                           @Query("version") String version,
                                           @Body Map<String, Object> http_body);

    //获取服务器回复的消息
    @POST("message/to_user")
    Observable<ResponseBody> getMessage(@Query("api_key") String apiKey,
                                        @Query("timestamp") String timestamp,
                                        @Query("sign") String sign,
                                        @Query("version") String version,
                                        @Body Map<String, Object> http_body);

    //确认消息收到
    @POST("message_arrived/create")
    Observable<ResponseBody> messageArrived(@Query("api_key") String apiKey,
                                            @Query("timestamp") String timestamp,
                                            @Query("sign") String sign,
                                            @Query("version") String version,
                                            @Body Map<String, Object> http_body);

    // 上传交互设备
    @POST("device/put")
    Observable<ResponseBody> deviceInfoUpload(@Query("api_key") String apiKey,
                                              @Query("timestamp") String timestamp,
                                              @Query("sign") String sign,
                                              @Query("version") String version,
                                              @Query("method") String method,
                                              @Body Map<String, Object> http_body);

    /**
     * 媒体播放同步接口
     */
    @POST("/next")
    Observable<ResponseBody> mediaNext(@Body Map<String, Object> http_body);

    @POST("/playprevious")
    Observable<ResponseBody> mediaPlayPrevious(@Body Map<String, Object> http_body);

    @POST("/pause")
    Observable<ResponseBody> mediaPause(@Body Map<String, Object> http_body);

    @POST("/continue")
    Observable<ResponseBody> mediaContinue(@Body Map<String, Object> http_body);
}