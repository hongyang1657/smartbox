package fitme.ai.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import fitme.ai.view.MainActivity;

/**
 * Created by fez on 2017/3/4.
 */
public class BootBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "SoundAi";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction().toString();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "onReceive: " + action);
            Intent startIntent = new Intent(context, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startIntent);
        }
    }
}
