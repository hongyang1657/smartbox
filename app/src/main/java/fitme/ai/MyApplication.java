package fitme.ai;

import android.app.Application;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

import java.util.ArrayList;
import java.util.List;

import cn.com.broadlink.sdk.BLLet;
import cn.com.broadlink.sdk.data.controller.BLDNADevice;
import fitme.ai.bean.DeviceBean;
import fitme.ai.bean.YeelightDeviceBean;
import fitme.ai.utils.BLUserInfoUnits;

/**
 * Created by 69441 on 2017/4/6.
 */
public class MyApplication extends Application {

    static {
        System.loadLibrary("saiAPIs");
    }

    //博联
    public static ArrayList<BLDNADevice> mDevList = new ArrayList<BLDNADevice>();
    public static BLUserInfoUnits mBLUserInfoUnits;

    //保存经度信息
    private int longitude;
    //保存纬度信息
    private int latitude;

    @Override
    public void onCreate() {
        super.onCreate();
        //gqgz的账号  59ae515e
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=59ae515e");
        //初始化博联sdk
        mBLUserInfoUnits = new BLUserInfoUnits(this);
        BLLet.init(this);
        BLLet.DebugLog.on();
    }



    public int getLongitude() {
        return longitude;
    }

    public void setLongitude(int longitude) {
        this.longitude = longitude;
    }

    public int getLatitude() {
        return latitude;
    }

    public void setLatitude(int latitude) {
        this.latitude = latitude;
    }

    //获取博联设备
    private List<DeviceBean> bldnaDeviceList;

    public void setBldnaDevice(DeviceBean deviceBean) {
        bldnaDeviceList.add(deviceBean);
    }

    public List<DeviceBean> getBldnaDeviceList() {
        return bldnaDeviceList;
    }

    public void initBldnaDeviceList() {
        bldnaDeviceList = new ArrayList<>();
    }

    private List<YeelightDeviceBean> yeelightDeviceBeanList;

    public void initYeelightDeviceList(){
        yeelightDeviceBeanList = new ArrayList<>();
    }

    public void setYeelightDeviceBean(YeelightDeviceBean yeelightDeviceBean){
        yeelightDeviceBeanList.add(yeelightDeviceBean);
    }

    public List<YeelightDeviceBean> getYeelightDeviceBeanList(){
        return yeelightDeviceBeanList;
    }

    public void appFinish(){
        BLLet.finish();
    }

    /**
     * 关闭Activity列表中的所有Activity*/
    public void finishActivity(){

        //杀死该应用进程
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
