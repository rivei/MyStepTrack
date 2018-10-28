package it.polimi.steptrack.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import it.polimi.steptrack.AppUtils;

public class PhoneShutdownReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        AppUtils.setKeyPhoneReboot(context,true);
        Log.e("PhoneShutdownReceiver", "Shutting down");
    }
}
