package com.example.xytattoo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

public class AppointmentReminder {

    // Method to schedule reminders for an appointment
    public static void scheduleReminder(Context context, Appointment appointment, String artistName) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Parse appointment date and time
        LocalDate appointmentDate = LocalDate.parse(appointment.getDate());
        LocalTime appointmentTime = LocalTime.parse(appointment.getStartTime());
        LocalDateTime appointmentDateTime = LocalDateTime.of(appointmentDate, appointmentTime);

        // Schedule day-before reminder (24 hours before)
        LocalDateTime dayBeforeDateTime = appointmentDateTime.minusDays(1);
        scheduleNotification(
                context,
                alarmManager,
                dayBeforeDateTime,
                "Holnapi időpont emlékeztető",
                "Holnap " + appointmentTime + " órakor időpontod van " + artistName + " művésszel.",
                appointment.getId().hashCode()
        );

        // Schedule hour-before reminder (1 hour before)
        LocalDateTime hourBeforeDateTime = appointmentDateTime.minusHours(1);
        scheduleNotification(
                context,
                alarmManager,
                hourBeforeDateTime,
                "Közelgő időpont",
                "Egy óra múlva időpontod van " + artistName + " művésszel.",
                appointment.getId().hashCode() + 1
        );
    }

    private static void scheduleNotification(Context context, AlarmManager alarmManager,
                                             LocalDateTime dateTime, String title, String message, int requestCode) {
        // Convert LocalDateTime to milliseconds
        long triggerTime = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        // Create intent for the alarm broadcast
        Intent intent = new Intent(context, AppointmentAlarmReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);
        intent.putExtra("notificationId", requestCode);

        // Create pending intent
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule the alarm
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    public static void cancelReminders(Context context, Appointment appointment) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Cancel the day-before reminder
        Intent intent1 = new Intent(context, AppointmentAlarmReceiver.class);
        PendingIntent pendingIntent1 = PendingIntent.getBroadcast(
                context,
                appointment.getId().hashCode(),
                intent1,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent1);

        // Cancel the hour-before reminder
        Intent intent2 = new Intent(context, AppointmentAlarmReceiver.class);
        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(
                context,
                appointment.getId().hashCode() + 1,
                intent2,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent2);
    }

    // Test method to trigger a notification quickly (after 30 seconds)
    public static void scheduleTestReminder(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, AppointmentAlarmReceiver.class);
        intent.putExtra("title", "Teszt értesítés");
        intent.putExtra("message", "Ez egy teszt értesítés a tetoválás alkalmazástól");
        intent.putExtra("notificationId", 999);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Set alarm for 30 seconds from now
        long triggerTime = System.currentTimeMillis() + 30 * 1000;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }
}