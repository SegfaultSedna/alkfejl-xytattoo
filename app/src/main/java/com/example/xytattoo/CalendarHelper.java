package com.example.xytattoo;

import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Helper class for calendar-related operations
 */
public class CalendarHelper {

    /**
     * Adds an appointment to the device calendar
     *
     * @param context The context to use for starting the calendar intent
     * @param appointment The appointment to add to the calendar
     * @param artistName The name of the artist for the appointment
     */
    public static void addAppointmentToCalendar(Context context, Appointment appointment, String artistName) {
        try {
            long startMillis = 0;
            long endMillis = 0;

            // Parse date and time to milliseconds
            LocalDate date = LocalDate.parse(appointment.getDate());
            LocalTime startTime = LocalTime.parse(appointment.getStartTime());
            LocalTime endTime = LocalTime.parse(appointment.getEndTime());

            // Combine date and time
            startMillis = date.atTime(startTime)
                    .atZone(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

            endMillis = date.atTime(endTime)
                    .atZone(ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

            // Create title for the event
            String title = "Tetoválás - " + artistName;

            // Intent for creating calendar event
            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, title)
                    .putExtra(CalendarContract.Events.DESCRIPTION, "XYTattoo időpont")
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                    .putExtra(CalendarContract.Events.ALL_DAY, false)
                    .putExtra(CalendarContract.Events.HAS_ALARM, true)
                    .putExtra(CalendarContract.Events.AVAILABILITY,
                            CalendarContract.Events.AVAILABILITY_BUSY);

            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Hiba a naptár esemény létrehozásakor: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}