package fitme.ai;

import android.content.Context;

import fitme.ai.utils.FileUtil;

import java.io.File;

/**
 * Created by Lixiaojie on 2016/10/31.
 */
public final class FileConfig {

    public static boolean checkConfigFile(Context context) {
        File saiConfig = new File(FileName.CONFIG_PATH);
        if (saiConfig.exists()) {
            FileUtil.clearSaiConfig(saiConfig);
        } else {
            saiConfig.mkdirs();
        }

        FileUtil.copyAsset(context, FileName.ASSET_PATH + FileName.SAI_CONFIG, FileName.CONFIG_PATH + FileName.SAI_CONFIG);
        FileUtil.copyAsset(context, FileName.ASSET_PATH + FileName.WOPT_SAI, FileName.CONFIG_PATH + FileName.WOPT_SAI);
        FileUtil.copyAsset(context, FileName.ASSET_PATH + FileName.SAIRES, FileName.CONFIG_PATH + FileName.SAIRES);
        FileUtil.copyAsset(context, FileName.ASSET_PATH + FileName.SAIRES2, FileName.CONFIG_PATH + FileName.SAIRES2);

        return saiConfig.list().length >= 4;
    }
}
