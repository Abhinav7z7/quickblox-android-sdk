package com.quickblox.sample.chat.ui.activities;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.quickblox.sample.chat.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created on 1/16/15.
 *
 * @author Bogatov Evgeniy bogatovevgeniy@gmail.com
 */

    public class MyTurnOnService extends Service {

    @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {

                Notification notification = new Notification(R.drawable.abc_ab_bottom_solid_dark_holo, "test service",
                        System.currentTimeMillis());
                Intent notificationIntent = new Intent(this, SplashActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
                Notification.Builder builder = new Notification.Builder(this);
                builder.setContentIntent(pendingIntent);
                startForeground(111111, builder.build());

                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        startActivity(new Intent(getBaseContext(), SplashActivity.class));
                    }
                }, 2000);
            return super.onStartCommand(intent, flags, startId);
        }
    }
