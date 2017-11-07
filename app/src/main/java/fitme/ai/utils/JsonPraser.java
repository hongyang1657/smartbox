package fitme.ai.utils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by 69441 on 2017/4/7.
 */
public final class JsonPraser {

    public static String getAsrStr(String json) {
        String asrStr = "";
        try {
            JSONObject jsonObject = new JSONObject(json);
            asrStr = jsonObject.getString("asr");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return asrStr;
    }

    public static String getDialogId(String json) {
        String id = "";
        try {
            JSONObject root = new JSONObject(json);
            id = root.getString("id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return id;
    }
}
