package com.cyanogenmod.filemanager.analytics;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import com.cyanogenmod.filemanager.FileManagerApplication;

import java.util.Random;

/**
 * Created by herriojr on 8/7/15.
 */
public class AnalyticsService extends IntentService {
    private static final String TAG = AnalyticsService.class.getSimpleName();
    private static final boolean DEBUG = false;

    public static long getLastRuntime(Context context) {
        SharedPreferences p = context.getSharedPreferences(TAG, MODE_PRIVATE);
        return p.getLong("last_runtime", 0);
    }

    public static void updateLastRuntime(Context context) {
        SharedPreferences p = context.getSharedPreferences(TAG, MODE_PRIVATE);
        p.edit().putLong("last_runtime", System.currentTimeMillis()).commit();
    }

    public static class Receiver extends BroadcastReceiver {
        private static final int REQUEST_CODE_DAILY_ANALYTICS = 124367236;

        private static final long TWENTY_FOUR_HOURS = 1000 * 60 * 60 * 24;
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                    || Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
                AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                // The reason we interval on half days is it is better to send it twice a day
                // than per chance not sending it on a day.
                mgr.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        getStartTime(context),
                        AlarmManager.INTERVAL_HALF_DAY,
                        getDailyReportIntent(context));
            } else {
                AnalyticsService.start(context);
            }
        }

        private PendingIntent getDailyReportIntent(Context context) {
            Intent intent = new Intent();
            intent.setClass(context, Receiver.class);

            return PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_DAILY_ANALYTICS,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
        }

        /**
         * Randomization may not be necessary due to how InexactRepeating works, however, just
         * in case we are randomizing throughout the day to minimize the likelihood too many
         * people will hit our servers in a short time just for this request.
         *
         * @return A time between System.currentTimeMillis() and 24 hours in the future
         */
        private static long getStartTime(Context context) {
            long last = getLastRuntime(context);
            return last == 0 ? 0 : last
                    + new Random(System.currentTimeMillis()).nextLong() % TWENTY_FOUR_HOURS;
        }
    }

    private static Object sLockLock = new Object();
    private static PowerManager.WakeLock sLock;

    private Analytics mAnalytics;

    private static void lock(Context context) {
        synchronized(sLockLock) {
            PowerManager mgr = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (sLock == null) {
                sLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            }
            if (sLock.isHeld()) {
                Log.w(TAG, "Attempting to acquire wakelock that is already held");
                return;
            }
            sLock.acquire();
        }
    }

    private static void unlock() {
        synchronized(sLockLock) {
            if (sLock == null || !sLock.isHeld()) {
                Log.w(TAG, "Attempted to release wakelock before created");
                return;
            }
            sLock.release();
        }
    }

    private static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static void start(Context context) {
        if (!isMainThread()) {
            throw new IllegalStateException("start must be called on the UI Thread");
        }

        lock(context);

        Intent intent = new Intent(context, AnalyticsService.class);
        ComponentName component = context.startService(intent);
        if (component == null) {
            Log.e(TAG, "Service not found for " + intent);
            unlock();
        }
    }

    public AnalyticsService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mAnalytics = ((FileManagerApplication)getApplication()).getAnalyticsApi();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Here we will handle sending daily events

        updateLastRuntime(this);
    }
}
