package it.polimi.steptrack.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import it.polimi.steptrack.AppUtils;
import it.polimi.steptrack.services.StepTrackingService;
import it.polimi.steptrack.ui.MainActivity;

import static it.polimi.steptrack.AppConstants.SERVICE_NOT_RUNNING;

public class ServiceRestartReceiver extends BroadcastReceiver {
    static final String TAG = ServiceRestartReceiver.class.getSimpleName();

    //Make sure mobile permission allow auto start after reboot!!
    @Override
    public void onReceive(final Context context, final Intent intent) {

        Log.i(TAG, "Service stopped");
        Log.i(TAG, "Service status:" + AppUtils.getServiceRunningStatus(context));

        if (AppUtils.getKeyPhoneReboot(context)) {
            int currentStepCount = AppUtils.getLastStepCount(context);
            AppUtils.setStepCountOffset(context, (0 - currentStepCount));
        }
        if (AppUtils.getServiceRunningStatus(context) == SERVICE_NOT_RUNNING) {
            context.startService(new Intent(context, StepTrackingService.class));
            //context.startActivity(new Intent(context, MainActivity.class));
        }
    }
}