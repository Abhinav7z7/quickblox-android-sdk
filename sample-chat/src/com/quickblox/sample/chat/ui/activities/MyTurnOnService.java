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

            Notification notification = new Notification();
            Intent notificationIntent = new Intent(this, ChatActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Notification.Builder builder = new Notification.Builder(this);
            builder.setContentText("Content text");
            builder.setContentIntent(pendingIntent);
            Notification foregraundNitification = builder.build();
            startForeground(11111, notification);

                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Intent intent1 = new Intent(getBaseContext(), SplashActivity.class);
                        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent1);
                    }
                }, 2000);
            return super.onStartCommand(intent, flags, startId);
        }
    }
