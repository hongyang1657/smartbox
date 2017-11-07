package fitme.ai.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;

import fitme.ai.FileName;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by Lixiaojie on 2016/10/31.
 */
public final class FileUtil {

    private static final String TAG = "SoundAi";

    public static File downloadFile(String downloadUrl, String directory, String fileName) {
        try {
            URL url = new URL(downloadUrl);
            URLConnection conn = url.openConnection();
            InputStream is = conn.getInputStream();
            int length = conn.getContentLength();
            Log.d(TAG, "downloadFileLength: " + length);
            String downloadDir = Environment.getExternalStorageDirectory() + directory;
            File dir = new File(downloadDir);
            if (!dir.exists()) {
                dir.mkdir();
            }
            File file = new File(downloadDir, fileName);
            if (file.exists()) {
                file.delete();
            }
            byte[] b = new byte[1024];
            int len;
            OutputStream os = new FileOutputStream(file);
            while ((len = is.read(b)) != -1) {
                os.write(b, 0, len);
            }
            os.close();
            is.close();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void writeFile(byte[] buffer, String filePath, boolean isAppend) {
        File file = new File(filePath);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, isAppend);
            fos.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeStringFile(String content, String filePath, boolean isAppend, boolean newLine) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(filePath, isAppend);
            if (newLine) {
                writer.write("\n" + content);
            } else {
                writer.write(content);
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void copyAsset(Context context, String oldPath, String newPath) {
        InputStream is;
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(newPath);
            is = context.getAssets().open(oldPath);
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = is.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }
            fos.flush();
            fos.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getJsonFromAsset(Context mContext, String fileName) {
        StringBuilder sb = new StringBuilder();
        AssetManager am = mContext.getAssets();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    am.open(fileName)));
            String next;
            while (null != (next = br.readLine())) {
                sb.append(next);
            }
        } catch (IOException e) {
            e.printStackTrace();
            sb.delete(0, sb.length());
        }
        return sb.toString().trim();
    }

    public static String getStringFromFile(String pathName) {
        File file = new File(pathName);
        StringBuilder result = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String s;
            while ((s = br.readLine()) != null) {
                result.append(s);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public static void clearSaiConfig(File saiConfig) {
        if (saiConfig.isDirectory()) {
            File[] fileList = saiConfig.listFiles();
            for (File file : fileList) {
                if (file.getName().contains(FileName.SAI_API)) {
                    Log.d(TAG, "clearSaiConfig: skip file " + file.getName());
                } else {
                    file.delete();
                }
            }
        }
    }
}