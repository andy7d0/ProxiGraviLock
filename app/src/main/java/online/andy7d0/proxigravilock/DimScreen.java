package online.andy7d0.proxigravilock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

public class DimScreen extends Activity {

    public static String CLOSE = "online.andy7d0.proxigravilock.closedim";

    LocalBroadcastManager mLocalBroadcastManager;
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(CLOSE)){
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(flags);


        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        //        WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Code below is to handle presses of Volume up or Volume down.
        // Without this, after pressing volume buttons, the navigation bar will
        // show up and won't hide
        final View decorView = getWindow().getDecorView();
        decorView
                .setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
                {

                    @Override
                    public void onSystemUiVisibilityChange(int visibility)
                    {
                        if((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
                        {
                            decorView.setSystemUiVisibility(flags);
                        }
                    }
                });


        View v = new View(this);
        v.setBackgroundColor(Color.BLACK);
        //LinearLayout.LayoutParams
        setContentView(v);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(CLOSE);
        mLocalBroadcastManager.registerReceiver(mBroadcastReceiver, mIntentFilter);

        Log.d("!R!DIM!","!CREATE!");

    }
    @Override
    protected void onDestroy() {
        Log.d("!R!DIM!","!DESTROY!");
        super.onDestroy();
        mLocalBroadcastManager.unregisterReceiver(mBroadcastReceiver);
    }
    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        setIntent(i);
        Log.d("!R!DIM!intent!new", i.toString());
    }

    // methods
    // 1. we can do nonthing, just waiting for native timeout
    // due to event ignorance in this activity, it's safe to touch in pocket
    // 2. we can setup short timeout and restore native one later

    /*
    * restore timeout
    * 1) we save it in oncreate
    * 2) we restore it in ondestroy
    * 3) normally it's ok, it's always happend as intendent
    * 4) but! our dim activity can be killed
    * 5) so, we can't catch ondestroy
    * 6) maybe, we can push this work to service!
    * 7) we send old timeout there
    * 8) or...
    * 9) system always calls onpause, but there are two cases
    * 10) a) after right 'kill/stop' b) before onnewintent
    * 11)
    * */


    @Override
    protected void onStart() {
        super.onStart();
        Log.d("!R!DIM!","!START!");


    }
    @Override
    protected void onStop() {
        super.onStop();
        Log.d("!R!DIM!","!STOP!");
        finish();
    }

    @Override
    protected void onResume() {
        Log.d("!R!DIM!","!RESUME!");
        super.onResume();
        Intent i = getIntent();
        if(i.getBooleanExtra("stop", false)) {
            Log.d("!R!DIM!","!RESUME!TOFINISH");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            finish();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        Log.d("!R!DIM!","!PAUSE!" +(isFinishing() ? "FINISHING" : ""));
    }

    @SuppressLint("NewApi")
    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);

        int currentApiVersion = android.os.Build.VERSION.SDK_INT;

        if(currentApiVersion >= Build.VERSION_CODES.KITKAT && hasFocus)
        {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

}
