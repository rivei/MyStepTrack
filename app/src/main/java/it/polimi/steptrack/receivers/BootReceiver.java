package it.polimi.steptrack.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import it.polimi.steptrack.BuildConfig;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
/*        if (BuildConfig.DEBUG) Logger.log("booted");

        SharedPreferences prefs = context.getSharedPreferences("pedometer", Context.MODE_PRIVATE);

        Database db = Database.getInstance(context);

        if (!prefs.getBoolean("correctShutdown", false)) {
            if (BuildConfig.DEBUG) Logger.log("Incorrect shutdown");
            // can we at least recover some steps?
            int steps = Math.max(0, db.getCurrentSteps());
            if (BuildConfig.DEBUG) Logger.log("Trying to recover " + steps + " steps");
            db.addToLastEntry(steps);
        }
        // last entry might still have a negative step value, so remove that
        // row if that's the case
        db.removeNegativeEntries();
        db.saveCurrentSteps(0);
        db.close();
        prefs.edit().remove("correctShutdown").apply();

        context.startService(new Intent(context, SensorListener.class));
    */

        //After reboot, check if the service is still running and retrieve the steps record from last time

//        // Clear Steps
//        Preferences.clearStepCount(context);
//
//        // Check if the Step Counter service was running?
//        if (Preferences.shouldServiceRun(context))
//        {
//            // Start Step Counter service
//            Intent myIntent = new Intent(context, StepCounter.class);
//            context.startService(myIntent);
//        }

    }
}