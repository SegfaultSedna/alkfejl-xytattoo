package com.example.xytattoo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AppointmentAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");
        int notificationId = intent.getIntExtra("notificationId", 0);

        NotificationHelper.showAppointmentNotification(context, title, message, notificationId);
    }
}