package online.andy7d0.proxigravilock;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class StartupReceiver extends BroadcastReceiver {
    static boolean registred = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        String strAction = intent.getAction();
        if(strAction.equals(Intent.ACTION_USER_PRESENT)) {
            initListener(context, false);
        }
        if(strAction.equals(Intent.ACTION_SCREEN_ON)) {

            Log.d("!R!FGSERVICE", "start");
            context.getApplicationContext().startService(new Intent(context, FgService.class));

        }
        if(strAction.equals(Intent.ACTION_SCREEN_OFF)) {

            Log.d("!R!FGSERVICE", "stop");
            context.getApplicationContext().stopService(new Intent(context, FgService.class));

        }
     }
     public void initListener(Context context, boolean manual) {
         final IntentFilter theFilter = new IntentFilter();
         /** System Defined Broadcast */
         theFilter.addAction(Intent.ACTION_SCREEN_ON);
         theFilter.addAction(Intent.ACTION_SCREEN_OFF);

         Log.d("!R!FGSERVICE", "LOGIN");
         SharedPreferences mPrefs = PreferenceManager
                 .getDefaultSharedPreferences(context.getApplicationContext());
         if( (manual || mPrefs.getBoolean("pref_start_on_boot", false))
                 && !registred) {
             registred = true;
             context.getApplicationContext().registerReceiver(this, theFilter);
             context.getApplicationContext().startService(new Intent(context, FgService.class));
         }
     }
}
