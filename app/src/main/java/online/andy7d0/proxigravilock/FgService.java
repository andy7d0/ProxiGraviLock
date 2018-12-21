package online.andy7d0.proxigravilock;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

public class FgService extends Service {

    GPworker worker = new GPworker();

    public FgService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not implemented");
        //return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_screen_lock_rotation_black_24dp)
                .setContentText(getText(R.string.notifiction_text))
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        Log.d("!R!FGSERVICE", "fg");
        return START_REDELIVER_INTENT;
    }


    @Override
    public void onCreate() {
        worker.Init(this, null, null);
        worker.onResume();
    }

    @Override
    public void onDestroy() {
        worker.onPause();
    }

}
