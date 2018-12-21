package online.andy7d0.proxigravilock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity
{
    private TextView textProximity;
    private TextView textY;
    private TextView textZ;
    private TextView textX;

    GPworker worker = new GPworker();

    public class ViewNotify implements GPworker.Notify {
        @Override
        public void Notify(Context ctx, String cls, String msg) {
            switch(cls) {
                case "proximity":
                    textProximity.setText(msg); break;
                case "X":
                    textX.setText(msg); break;
                case "Y":
                    textY.setText(msg); break;
                case "Z":
                    textZ.setText(msg); break;
            }
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        worker.onSaveInstanceState(state);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);


        textProximity = ((TextView) findViewById(R.id.proximity));
        textY = ((TextView) findViewById(R.id.gravityY));
        textZ = ((TextView) findViewById(R.id.gravityZ));
        textX = ((TextView) findViewById(R.id.gravityX));

        worker.Init(this, savedInstanceState, new ViewNotify());

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                    Log.d("!R!SCREEN_NOTIFY!", Intent.ACTION_SCREEN_OFF);
                } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                    Log.d("!R!SCREEN_NOTIFY!", Intent.ACTION_SCREEN_ON);
                }
            }
        }, intentFilter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        worker.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        worker.onPause();
    }

}
