package fitme.ai;

import android.os.Environment;

import java.io.File;

/**
 * Description:
 * Created by WangXin on 2017/9/23.
 */
public final class FileName {

    public static final String CONFIG_PATH = Environment.getExternalStorageDirectory().toString()
            + File.separator
            + "sai_config"
            + File.separator;

    public static final String ASSET_PATH = "SAI3229" + File.separator;

    public static final String SAI_CONFIG = "sai_config.txt";
    public static final String WOPT_SAI = "wopt_6mic_sai.bin";
    public static final String SAI_API = "sai_api.q";
    public static final String SAIRES = "saires.q";
    public static final String SAIRES2 = "saires2.q";
}
