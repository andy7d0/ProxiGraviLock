package online.andy7d0.proxigravilock;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;

import static android.view.Surface.ROTATION_0;
import static android.view.Surface.ROTATION_270;
import static android.view.Surface.ROTATION_90;


final class GPworker implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mProximity;
    private Sensor mGravity;

    public static final double SENSOR_SENSITIVITY = 2;
    public static final double SENSOR_SENSITIVITY_CLICK = 0.1;


    //dim if
    // face down - proj(gravity, Z) << 0
    // head up - proj(gravity, Y) < - 45 deg << 0
    // landscape - proj(gravity, Y) ~ 0 and proj(gravity, X) << 0

    // 50deg right and more :
    // -g < gravity[y] < g*cos(50)
    // && gravity[x] < 0
    //
    public static final double SENSOR_DIM_BY_Y_UP =
            6.17; //~ 50 deg head up && gravity_x < 0
    public static final double SENSOR_DIM_BY_Y_DOWN =
           -7.8; //~45 deg head down
    public static final double SENSOR_DIM_BY_Z =
            -5;

    //"landscape" mode
    //  -20 degree < z-axis ^ vert < 50 degree
    //  -9.8 * cos(20) < gravity[z] < 9.8 * cos(50)
    //  y-axis ^ vert ~ 90
    //  abs(gravity[y]) < 9.8 * cos (30)
    public static final double SENSOR_Z_ROTSCREN_FD =
            -3.6;//~ 20 deg
    public static final double SENSOR_Z_ROTSCREN_FU =
            7.5; //~ -50 deg
    public static final double SENSOR_Y_ROTSCREN =
            3.3;// ~ +-20 deg

    public static final double SENSOR_SENSITIVITY_Y_WAKEUP =
            (9.8*Math.cos(Math.PI*45/180));
    public static final double SENSOR_SENSITIVITY_Z_WAKEUP =
            (9.8*Math.cos(Math.PI*45/180));
    public static final double SENSOR_SENSITIVITY_X_WAKEUP =
            (9.8*Math.cos(Math.PI*45/180));

    private boolean wasSwitchedRatationMode = false;
    private boolean landscape = false;
    private boolean face_down = false;
    private boolean head_down = false;
    private boolean near = false;
    private int nearDeltaSign = 0; //0 - same, 1 - far->near -1 - near->far
    private long nearOnTime = 0;
    private long nearOffTime = 0;
    private boolean near_s = false;
    private int nearDeltaSign_s = 0; //0 - same, 1 - far->near -1 - near->far
    private long nearOnTime_s = 0;
    private long nearOffTime_s = 0;
    private boolean head_up_wakeup = false;
    private boolean su_startSent = false;
    private boolean su_stopSent = false;
    private SharedPreferences mPrefs;
    private Context context;

    public interface Notify {
        public void Notify(Context ctx, String cls, String msg);
    }
    private Notify mNotify;

    public class ToastNotify implements  Notify {
        @Override
        public void Notify(Context ctx, String cls, String msg) {
            Toast.makeText(ctx, cls + ": "+msg, 2000 );
        }
    }
    public class SilentNotify implements  Notify {
        @Override
        public void Notify(Context ctx, String cls, String msg) {
        }
    }

    void Init(Context ctx, Bundle savedInstanceState, Notify extNotify) {
        context = ctx.getApplicationContext();
        wasSwitchedRatationMode = savedInstanceState != null?
                savedInstanceState.getBoolean(
                        "wasRotated", false)
                : false;

        mNotify = extNotify != null ? extNotify : new SilentNotify();

        mPrefs = PreferenceManager
                .getDefaultSharedPreferences(ctx);
        ctx.getContentResolver();

        mSensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);
        mProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        landscape = false;
        face_down = false;
        head_down = false;
        near = false;
        nearDeltaSign = 0; //0 - same, 1 - far->near -1 - near->far
        nearOnTime = 0;
        nearOffTime = 0;
        near_s = false;
        nearDeltaSign_s = 0; //0 - same, 1 - far->near -1 - near->far
        nearOnTime_s = 0;
        nearOffTime_s = 0;
        head_up_wakeup = false;
        su_startSent = false;
        su_stopSent = true;


        Log.d("!R!WRK", "!CREATE!");
    }

    protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("wasRotated", wasSwitchedRatationMode);
    }


    public void onResume() {
        mSensorManager.registerListener(this, mProximity, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGravity, SensorManager.SENSOR_DELAY_NORMAL);
        landscape = false;
        Log.d("!R!WRK", "!RESUME!");
    }
    public void onPause() {
        mSensorManager.unregisterListener(this, mProximity);
        mSensorManager.unregisterListener(this, mGravity);
        Log.d("!R!WRK", "!PAUSE!");
        screenOffCancel();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            nearDeltaSign = 0;
            if (event.values[0] >= 0 && event.values[0] <= SENSOR_SENSITIVITY) {
                mNotify.Notify(context,"proximity", "near");
                if(!near) { nearDeltaSign = 1; nearOnTime = System.currentTimeMillis(); }
                near = true;
            } else {
                mNotify.Notify(context,"proximity", "far");
                if(near) { nearDeltaSign = -1; nearOffTime = System.currentTimeMillis(); }
                near = false;
            }
        }
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            nearDeltaSign_s = 0;
            if (event.values[0] >= 0 && event.values[0] <= SENSOR_SENSITIVITY_CLICK) {
                //mNotify.Notify(context,"proximity", "near");
                if(!near_s) { nearDeltaSign_s = 1; nearOnTime_s = System.currentTimeMillis(); }
                near_s = true;
            } else {
                //mNotify.Notify(context,"proximity", "far");
                if(near_s) { nearDeltaSign_s = -1; nearOffTime_s = System.currentTimeMillis(); }
                near_s = false;
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            if (event.values[1] < SENSOR_DIM_BY_Y_UP
                    && event.values[0] < 0
                    ||
                    event.values[1] < SENSOR_DIM_BY_Y_DOWN
                    ) {
                //head down
                mNotify.Notify(context,"Y", "DOWN");
                head_down = true;
            } else {
                //head up
                mNotify.Notify(context,"Y", "UP");
                head_down = false;
            }

            if (event.values[2] < SENSOR_DIM_BY_Z) {
                //face down
                mNotify.Notify(context,"Z", "face - down");
                face_down = true;
            } else {
                //face up
                mNotify.Notify(context,"Z", "face - up");
                face_down = false;
            }

            //
            if ( Math.abs(event.values[1])
                    < SENSOR_Y_ROTSCREN
                    &&
                    event.values[0] > 0
                    && SENSOR_Z_ROTSCREN_FD < event.values[2]
                    //&& event.values[2] < SENSOR_Z_ROTSCREN_FU
                    ) {
                mNotify.Notify(context,"X", "landscape");
                landscape = true;
            } else {
                mNotify.Notify(context,"X", "portrait");
                landscape = false;
                //Log.d("!R!", wasSwitchedRatationMode ? "!SW!" : "!NS!");
                if (wasSwitchedRatationMode) {
                    wasSwitchedRatationMode = false;
                    //Settings.System.putInt(context.getContentResolver(),
                      //      Settings.System.ACCELEROMETER_ROTATION, 0);
                    Settings.System.putInt(context.getContentResolver(),
                            Settings.System.USER_ROTATION,  ROTATION_0);
                }
            }

            if( Math.abs(event.values[0]) < SENSOR_SENSITIVITY_X_WAKEUP
            && Math.abs(event.values[2]) < SENSOR_SENSITIVITY_Z_WAKEUP
            && event.values[1] > SENSOR_SENSITIVITY_Y_WAKEUP
                    ) {
                head_up_wakeup = true;
            } else {
                head_up_wakeup = false;
            }
        }

        processStates();
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void processStates() {
        if (
                mPrefs.getBoolean("pref_proximity_rotate", false)
                &&
                landscape
                &&
                nearOnTime_s < nearOffTime_s
                &&
                nearOffTime_s < nearOnTime_s + 1500 // click(near->far) within 500ms
                &&
                nearDeltaSign_s < 0
                &&
                Settings.System.getInt(context.getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION, 0) == 0
                ) {
            //Settings.System.putInt(context.getContentResolver(),
              //      Settings.System.ACCELEROMETER_ROTATION, 1);
            Settings.System.putInt(context.getContentResolver(),
                        Settings.System.USER_ROTATION,  ROTATION_90);
            wasSwitchedRatationMode = true;
            Log.d("!R!", "!WS!");
        }
        if (mPrefs.getBoolean("pref_proximity_lock", false) &&
                mPrefs.getBoolean("pref_face_down", false)
                &&
                face_down
                &&
                near
                ) {
            Log.d("!R!OFF", "!FACE!");
            screenOff();
        }
        if (mPrefs.getBoolean("pref_proximity_lock", false) &&
                mPrefs.getBoolean("pref_head_down", false)
                &&
                head_down
                &&
                near
                ) {
            Log.d("!R!OFF", "!HEAD!");
            screenOff();
        }
        if(mPrefs.getBoolean("pref_proximity_lock", false) &&
                !mPrefs.getBoolean("pref_head_down", false)
                &&
                !mPrefs.getBoolean("pref_face_down", false)
                &&
                !landscape
                ) {
            Log.d("!R!OFF", "!PROXIMITY-ONLY!");
            screenOff();
        }

        if (mPrefs.getBoolean("pref_proximity_lock", false) &&
                nearDeltaSign < 0
                ) {
            Log.d("!R!OFF", "!CANCEL!");
            screenOffCancel();
        }

        //TODO: calculate face up head up status (~45 degree)
        // we can use it later for wake up
    }

    public void screenOff() {
        //FLAG_KEEP_SCREEN_ON
        //Device.getState() STATE_ON
        //powerManager.isInteractive();  powerManager.isScreenOn();
        // if screen is ON ...
        // start empty fullscreen activity
        // turn screen ON
        // dim screen
        // change screen timeout to min
        // wait
        // change timeout back

        // while this activity works, the power button LOCKS screen (actually!)
        // and after that start unlock with no visual difference
        // it's bad!
        // but! we do it from proximity lock, so there is no reason to press power button
        // and if we lost proximity, we should unlock instantly!

        if(mPrefs.getBoolean("pref_su", false)) {
            //input keyevent 26
            if(!su_startSent) {
                su_startSent = true;
                su_stopSent = false;
                exec();
             }
            return;
        }

        Intent i = new Intent(context, DimByTimeout.class);
        context.startService(i);
    }
    public void screenOffCancel() {
        // when Dim is active
        if(mPrefs.getBoolean("pref_su", false)) {
            //input keyevent 26
            if(!su_stopSent) {
                su_startSent = false;
                su_stopSent = true;
                exec();
            }
            return;
        }
        Intent i = new Intent(context, DimByTimeout.class);
        i.putExtra("cancel", true);
        context.startService(i);
    }
    private void exec() {
        try{
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            outputStream.writeBytes("input keyevent 26\n");
            outputStream.flush();

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            su.waitFor();
        }catch(IOException e){
            //throw new Exception(e);
        }catch(InterruptedException e){
            //throw new Exception(e);
        }
    }
}
