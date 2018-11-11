package it.polimi.steptrack.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import it.polimi.steptrack.AppUtils;

import static it.polimi.steptrack.AppConstants.TRANSITIONS_RECEIVER_ACTION;

public class PhoneShutdownReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
            AppUtils.setKeyPhoneReboot(context, true);
            Log.e("PhoneShutdownReceiver", "Shutting down");
        }
    }
}
