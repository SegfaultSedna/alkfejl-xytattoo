package com.example.xytattoo;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppointmentsActivity extends AppCompatActivity {
    private RecyclerView appointmentsRecycler;
    private AppointmentAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseUser user;

    TextView artist, date, time;
    Button editButton, deleteButton, calendarButton;

    private static final int CALENDAR_PERMISSION_REQUEST_CODE = 100;
    private Appointment pendingCalendarAppointment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make status bar transparent before setContentView
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_appointments);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        setupToolbar();
        setupRecycler();
        loadAppointments();

        RecyclerView appointmentsRecycler = findViewById(R.id.appointmentsRecycler);
    }

    // Replace the showTimePicker method with this
    private void showTimePicker(Appointment appointment) {
        // Get the appointment duration in minutes
        long durationMinutes = getDuration(appointment);

        // Get existing appointments for the same date and artist (excluding this one)
        db.collection("appointments")
                .whereEqualTo("artistId", appointment.getArtistId())
                .whereEqualTo("date", appointment.getDate())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Appointment> existingAppointments = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // Skip the current appointment
                        if (!doc.getId().equals(appointment.getId())) {
                            existingAppointments.add(doc.toObject(Appointment.class));
                        }
                    }

                    // Calculate available time slots
                    List<TimeSlot> availableSlots = calculateAvailableSlots(
                            existingAppointments,
                            LocalDate.parse(appointment.getDate()),
                            durationMinutes);

                    // Show dialog with available time slots
                    showTimeSlotSelectionDialog(appointment, availableSlots);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading appointments", Toast.LENGTH_SHORT).show());
    }

    private List<TimeSlot> calculateAvailableSlots(List<Appointment> existingAppointments,
                                                   LocalDate date, long durationMinutes) {
        List<TimeSlot> availableTimeSlots = new ArrayList<>();
        LocalTime start = LocalTime.of(7, 0);  // Studio opens at 7:00
        LocalTime end = LocalTime.of(16, 0);   // Studio closes at 16:00

        // Convert booked appointments to time slots
        List<TimeSlot> bookedSlots = new ArrayList<>();
        for (Appointment app : existingAppointments) {
            LocalTime appStart = LocalTime.parse(app.getStartTime());
            LocalTime appEnd = LocalTime.parse(app.getEndTime());
            bookedSlots.add(new TimeSlot(appStart, appEnd));
        }

        // Calculate available slots
        LocalTime current = start;
        while (current.plusMinutes(durationMinutes).isBefore(end.plusSeconds(1))) {
            LocalTime slotEnd = current.plusMinutes(durationMinutes);

            boolean isAvailable = true;
            for (TimeSlot booked : bookedSlots) {
                if (current.isBefore(booked.getEndTime()) &&
                        slotEnd.isAfter(booked.getStartTime())) {
                    isAvailable = false;
                    break;
                }
            }

            if (isAvailable) {
                availableTimeSlots.add(new TimeSlot(current, slotEnd));
            }
            current = current.plusMinutes(30);
        }

        return availableTimeSlots;
    }

    private void showTimeSlotSelectionDialog(Appointment appointment, List<TimeSlot> availableSlots) {
        if (availableSlots.isEmpty()) {
            Toast.makeText(this, "Nincs szabad időpont erre a napra", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create dialog items
        CharSequence[] items = new CharSequence[availableSlots.size()];
        for (int i = 0; i < availableSlots.size(); i++) {
            TimeSlot slot = availableSlots.get(i);
            items[i] = slot.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")) +
                    " - " + slot.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        }

        // Show alert dialog with time slots
        new AlertDialog.Builder(this)
                .setTitle("Válassz időpontot")
                .setItems(items, (dialog, which) -> {
                    TimeSlot selectedSlot = availableSlots.get(which);
                    updateAppointmentTime(
                            appointment,
                            selectedSlot.getStartTime().format(DateTimeFormatter.ISO_TIME),
                            selectedSlot.getEndTime().format(DateTimeFormatter.ISO_TIME)
                    );
                })
                .setNegativeButton("Mégse", null)
                .show();
    }

    // Update this method to accept both start and end time
    private void updateAppointmentTime(Appointment appointment, String newStartTime, String newEndTime) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("startTime", newStartTime);
        updates.put("endTime", newEndTime);

        db.collection("appointments").document(appointment.getId())
                .update(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Időpont módosítva", Toast.LENGTH_SHORT).show();
                    loadAppointments();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Módosítási hiba", Toast.LENGTH_SHORT).show());
    }

    private void loadAppointments() {
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("appointments")
                .whereEqualTo("userId", user.getUid())
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Appointment> appointments = queryDocumentSnapshots.toObjects(Appointment.class);
                    int index = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        appointments.get(index).setId(doc.getId());
                        index++;
                    }
                    adapter.setAppointments(appointments);

                    // Show empty view if no appointments
                    TextView emptyView = findViewById(R.id.emptyView);
                    if (appointments.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading appointments", Toast.LENGTH_SHORT).show());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecycler() {
        appointmentsRecycler = findViewById(R.id.appointmentsRecycler);
        appointmentsRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppointmentAdapter();
        appointmentsRecycler.setAdapter(adapter);
    }


    private class AppointmentAdapter extends RecyclerView.Adapter<AppointmentViewHolder> {
        private List<Appointment> appointments = new ArrayList<>();
        private Map<String, String> artistNameCache = new HashMap<>();

        @NonNull
        @Override
        public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_appointment, parent, false);
            return new AppointmentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
            Appointment appointment = appointments.get(position);
            String cachedName = artistNameCache.get(appointment.getArtistId());
            if (cachedName != null) {
                holder.artist.setText(cachedName);
            } else {
                // Get artist name from Firestore (you might want to cache this)
                db.collection("artists").document(appointment.getArtistId())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            Artist artist = documentSnapshot.toObject(Artist.class);
                            if (artist != null) {
                                holder.artist.setText(artist.getName());
                            }
                        });
            }


            holder.date.setText("Dátum: " + appointment.getDate());
            holder.time.setText("Idő: " + appointment.getStartTime() + " - " + appointment.getEndTime());

            holder.editButton.setOnClickListener(v -> showTimePicker(appointment));
            holder.deleteButton.setOnClickListener(v -> deleteAppointment(appointment));

            holder.calendarButton.setOnClickListener(v -> {
                // Store appointment for use after permission check
                pendingCalendarAppointment = appointments.get(position);

                // Check calendar permission
                if (checkCalendarPermission()) {
                    addToCalendar(pendingCalendarAppointment);
                }
            });
        }

        @Override
        public int getItemCount() {
            return appointments.size();
        }

        public void setAppointments(List<Appointment> appointments) {
            this.appointments = appointments;
            notifyDataSetChanged();
        }
    }

    private static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        TextView artist, date, time;
        Button editButton, deleteButton, calendarButton;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            artist = itemView.findViewById(R.id.appointmentArtist);
            date = itemView.findViewById(R.id.appointmentDate);
            time = itemView.findViewById(R.id.appointmentTime);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            calendarButton = itemView.findViewById(R.id.calendarButton);
        }
    }

    private void updateAppointmentTime(Appointment appointment, String newStartTime) {
        // Calculate new end time based on duration
        LocalTime start = LocalTime.parse(newStartTime);
        LocalTime end = start.plusMinutes(getDuration(appointment));

        Map<String, Object> updates = new HashMap<>();
        updates.put("startTime", newStartTime);
        updates.put("endTime", end.toString());

        db.collection("appointments").document(appointment.getId())
                .update(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Időpont módosítva", Toast.LENGTH_SHORT).show();
                    loadAppointments();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Módosítási hiba", Toast.LENGTH_SHORT).show());
    }

    private long getDuration(Appointment appointment) {
        LocalTime start = LocalTime.parse(appointment.getStartTime());
        LocalTime end = LocalTime.parse(appointment.getEndTime());
        return ChronoUnit.MINUTES.between(start, end);
    }

    private void deleteAppointment(Appointment appointment) {
        db.collection("appointments").document(appointment.getId())
                .delete()
                .addOnSuccessListener(v -> {
                    // Cancel reminders when appointment is deleted
                    AppointmentReminder.cancelReminders(this, appointment);

                    Toast.makeText(this, "Időpont törölve", Toast.LENGTH_SHORT).show();
                    loadAppointments();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Törlési hiba", Toast.LENGTH_SHORT).show());
    }

    private boolean checkCalendarPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_CALENDAR)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{android.Manifest.permission.WRITE_CALENDAR},
                        CALENDAR_PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CALENDAR_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Naptár hozzáférés engedélyezve", Toast.LENGTH_SHORT).show();
                // Process the pending calendar appointment if it exists
                if (pendingCalendarAppointment != null) {
                    addToCalendar(pendingCalendarAppointment);
                    pendingCalendarAppointment = null;
                }
            } else {
                Toast.makeText(this, "Naptár hozzáférés szükséges a funkció használatához",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void addToCalendar(Appointment appointment) {
        try {
            // Get artist name for the event title
            db.collection("artists").document(appointment.getArtistId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Artist artist = documentSnapshot.toObject(Artist.class);
                            String title = "Tetoválás - " + artist.getName();
                            createCalendarEvent(appointment, title);
                        } else {
                            createCalendarEvent(appointment, "Tetoválás időpont");
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Fallback if artist fetch fails
                        createCalendarEvent(appointment, "Tetoválás időpont");
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Hiba a naptár esemény létrehozásakor: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (appointmentsRecycler != null) {
            appointmentsRecycler.setAdapter(null); // Clear adapter
        }
        // Also clear your data list
        if (adapter != null) {
            adapter.setAppointments(new ArrayList<>());
        }
    }

    private void createCalendarEvent(Appointment appointment, String title) {
        try {
            long startMillis = 0;
            long endMillis = 0;

            // Parse date and time to milliseconds
            LocalDate date = LocalDate.parse(appointment.getDate());
            LocalTime startTime = LocalTime.parse(appointment.getStartTime());
            LocalTime endTime = LocalTime.parse(appointment.getEndTime());

            // Combine date and time
            startMillis = date.atTime(startTime)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

            endMillis = date.atTime(endTime)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant().toEpochMilli();

            // Intent for creating calendar event
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_INSERT)
                    .setData(android.provider.CalendarContract.Events.CONTENT_URI)
                    .putExtra(android.provider.CalendarContract.Events.TITLE, title)
                    .putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "XYTattoo időpont")
                    .putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                    .putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                    .putExtra(android.provider.CalendarContract.Events.ALL_DAY, false)
                    .putExtra(android.provider.CalendarContract.Events.HAS_ALARM, true)
                    .putExtra(android.provider.CalendarContract.Events.AVAILABILITY,
                            android.provider.CalendarContract.Events.AVAILABILITY_BUSY);

            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Hiba a naptár esemény létrehozásakor: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}