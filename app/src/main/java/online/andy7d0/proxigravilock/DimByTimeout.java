package online.andy7d0.proxigravilock;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.WindowManager;

public class DimByTimeout extends Service  {

    private boolean startSent = false;
    private boolean stopSent = false;

    public DimByTimeout() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void startDim(boolean retry) {
        Log.d("!R!SDIM!TOSTART", retry? "retry" : "new");

        if(startSent) return;
        startSent = true;
        stopSent = false;


        if(!retry) {
            int savedTimeout =
                    android.provider.Settings.System.getInt(getContentResolver(),
                            Settings.System.SCREEN_OFF_TIMEOUT, 30 * 1000); //def 30s
            SharedPreferences.Editor pe =
                    PreferenceManager.getDefaultSharedPreferences(this).edit();
            pe.putInt("saved_timeout", savedTimeout);
            pe.commit();

            Log.d("!R!SDIM!method!timeout!saved", String.valueOf(savedTimeout));
        }
        //here we can do screen dim
        android.provider.Settings.System.putInt(getContentResolver(),
                Settings.System.SCREEN_OFF_TIMEOUT, 2*1000);
        Log.d("!R!SDIM!method!timeout!setted", String.valueOf(
                android.provider.Settings.System.getInt(getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT, -1)));

        Intent i = new Intent(this, DimScreen.class);
        //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    public void stopDim(boolean retry) {
        Log.d("!R!SDIM!TOSTOP", retry ? "retry" : "plain");
        //optimization: if we anready sent stop, do nothing
        if(stopSent) return;
        startSent = false;
        stopSent = true;

        int savedTimeout = PreferenceManager.getDefaultSharedPreferences(this)
                .getInt("saved_timeout", 0);
        Log.d("!R!SDIM!method!timeout!to-restore", String.valueOf(savedTimeout));
        if (savedTimeout > 0) {
            SharedPreferences.Editor pe =
                    PreferenceManager.getDefaultSharedPreferences(this).edit();
            pe.putInt("saved_timeout", 0);
            pe.commit();
            android.provider.Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT,
                    savedTimeout);
        }
        Log.d("!R!SDIM!method!timeout!restored", String.valueOf(
                android.provider.Settings.System.getInt(getContentResolver(),
                        Settings.System.SCREEN_OFF_TIMEOUT, -1)));

        //Intent i = new Intent(this, DimScreen.class);
        //i.putExtra("stop", true);
        //i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //startActivity(i);
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager
                .getInstance(getApplicationContext());
        localBroadcastManager.sendBroadcast(new Intent(
                DimScreen.CLOSE));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean cancel = intent.getBooleanExtra("cancel", false);
        if(cancel) {
            stopDim(flags != 0);
        } else {
            startDim(flags != 0);
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onCreate() {
        Log.d("!R!SDIM!CREATE", "");
    }

    @Override
    public void onDestroy() {
        Log.d("!R!SDIM!DESTROY", "");
    }
}
