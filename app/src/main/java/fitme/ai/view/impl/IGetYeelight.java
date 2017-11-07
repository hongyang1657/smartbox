package fitme.ai.view.impl;

import java.util.HashMap;
import java.util.List;

import fitme.ai.bean.YeelightDeviceBean;

/**
 * Created by hongy on 2017/8/30.
 */

public interface IGetYeelight {
    void getInfoList(List<YeelightDeviceBean> templist);
    void getIpAndDevice(YeelightDeviceBean yeelightDeviceBean);
    void getResponse(String value);
}
