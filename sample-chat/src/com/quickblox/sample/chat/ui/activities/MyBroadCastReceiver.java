package com.quickblox.sample.chat.ui.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created on 1/16/15.
 *
 * @author Bogatov Evgeniy bogatovevgeniy@gmail.com
 */

public class MyBroadCastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        new Thread(){
            @Override
            public void run() {

                try {
                    Thread.currentThread().sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Intent activityIntent = new Intent(context, SplashActivity.class);
                context.startActivity(activityIntent);
            }
        }.start();
    }
}
