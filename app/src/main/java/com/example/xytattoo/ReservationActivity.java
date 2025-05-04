package com.example.xytattoo;

import android.app.AlarmManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReservationActivity extends AppCompatActivity implements
        ArtistAdapter.ArtistSelectionListener,
        DateAdapter.DateSelectionListener,
        TimeSlotAdapter.TimeSlotSelectionListener {

    private static final int REQUEST_CODE_EXACT_ALARM = 1001;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

    private RadioGroup sizeRadioGroup;
    private RecyclerView artistRecycler;
    private RecyclerView dateRecycler;
    private RecyclerView timeSlotRecycler;
    private Button confirmButton;

    private int selectedDuration = 0;
    private Artist selectedArtist;
    private LocalDate selectedDate;
    private TimeSlot selectedTimeSlot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeViews();
        setupRadioGroup();
        setupConfirmButton();
        loadArtists();
    }

    private void initializeViews() {
        sizeRadioGroup = findViewById(R.id.sizeRadioGroup);
        artistRecycler = findViewById(R.id.artistRecycler);
        dateRecycler = findViewById(R.id.dateRecycler);
        timeSlotRecycler = findViewById(R.id.timeSlotRecycler);
        confirmButton = findViewById(R.id.confirmButton);

        artistRecycler.setLayoutManager(new GridLayoutManager(this, 2));
        dateRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        timeSlotRecycler.setLayoutManager(new GridLayoutManager(this, 4));
    }

    private void setupRadioGroup() {
        sizeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.smallSize) selectedDuration = 30;
            else if (checkedId == R.id.mediumSize) selectedDuration = 60;
            else if (checkedId == R.id.largeSize) selectedDuration = 150;
            updateTimeSlots();
        });
    }

    private void setupConfirmButton() {
        confirmButton.setOnClickListener(v -> {
            if (checkExactAlarmPermission()) {
                createAppointment();
            } else {
                showPermissionDialog();
            }
        });
    }

    private boolean checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            return alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("Exact alarm permission is needed for appointment reminders")
                .setPositiveButton("Allow", (d, w) -> requestExactAlarmPermission())
                .setNegativeButton("Skip", (d, w) -> createAppointmentWithoutReminders())
                .show();
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivityForResult(
                    new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM),
                    REQUEST_CODE_EXACT_ALARM
            );
        } else {
            createAppointment();
        }
    }

    private void createAppointmentWithoutReminders() {
        createAppointment(false);
        Toast.makeText(this, "Appointment created without reminders", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EXACT_ALARM && checkExactAlarmPermission()) {
            createAppointment();
        }
    }

    private void loadArtists() {
        db.collection("artists").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<Artist> artists = new ArrayList<>();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Artist artist = doc.toObject(Artist.class);
                    artist.setId(doc.getId());
                    artists.add(artist);
                }
                artistRecycler.setAdapter(new ArtistAdapter(artists, this));
            }
        });
    }

    @Override
    public void onArtistSelected(Artist artist) {
        selectedArtist = artist;
        generateAvailableDates();
    }

    private void generateAvailableDates() {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 28; i++) {
            LocalDate date = today.plusDays(i);
            if (date.getDayOfWeek().getValue() < 6) dates.add(date);
        }
        dateRecycler.setAdapter(new DateAdapter(dates, this));
    }

    @Override
    public void onDateSelected(LocalDate date) {
        selectedDate = date;
        updateTimeSlots();
    }

    private void updateTimeSlots() {
        if (selectedArtist == null || selectedDate == null || selectedDuration == 0) return;

        db.collection("appointments")
                .whereEqualTo("artistId", selectedArtist.getId())
                .whereEqualTo("date", selectedDate.toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<TimeSlot> slots = calculateAvailableSlots(task.getResult().toObjects(Appointment.class));
                        timeSlotRecycler.setAdapter(new TimeSlotAdapter(slots, this));
                        confirmButton.setEnabled(!slots.isEmpty());
                    }
                });
    }

    private List<TimeSlot> calculateAvailableSlots(List<Appointment> appointments) {
        List<TimeSlot> available = new ArrayList<>();
        LocalTime start = LocalTime.of(7, 0);
        LocalTime end = LocalTime.of(16, 0);

        LocalTime current = start;
        while (current.plusMinutes(selectedDuration).isBefore(end)) {
            LocalTime slotEnd = current.plusMinutes(selectedDuration);
            if (isSlotAvailable(current, slotEnd, appointments)) {
                available.add(new TimeSlot(current, slotEnd));
            }
            current = current.plusMinutes(30);
        }
        return available;
    }

    private boolean isSlotAvailable(LocalTime start, LocalTime end, List<Appointment> appointments) {
        for (Appointment app : appointments) {
            LocalTime appStart = LocalTime.parse(app.getStartTime());
            LocalTime appEnd = LocalTime.parse(app.getEndTime());
            if (start.isBefore(appEnd) && end.isAfter(appStart)) return false;
        }
        return true;
    }

    @Override
    public void onTimeSlotSelected(TimeSlot timeSlot) {
        selectedTimeSlot = timeSlot;
    }

    private void createAppointment() {
        createAppointment(true);
    }

    private void createAppointment(boolean scheduleReminders) {
        if (!validateSelections()) return;

        Appointment appointment = new Appointment(
                null,
                currentUser.getUid(),
                selectedArtist.getId(),
                selectedDate.toString(),
                selectedTimeSlot.getStartTime().format(DateTimeFormatter.ISO_TIME),
                selectedTimeSlot.getEndTime().format(DateTimeFormatter.ISO_TIME)
        );

        db.collection("appointments").add(appointment)
                .addOnSuccessListener(ref -> {
                    appointment.setId(ref.getId());
                    if (scheduleReminders) {
                        AppointmentReminder.scheduleReminder(this, appointment, selectedArtist.getName());
                    }
                    Toast.makeText(this, "Időpont lefoglalva!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Időpont foglalás sikertelen: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // This will navigate back to the previous activity
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean validateSelections() {
        if (selectedDuration == 0) showError("Válassz méretet");
        else if (selectedArtist == null) showError("Válassz művészt");
        else if (selectedDate == null) showError("Válassz dátumot");
        else if (selectedTimeSlot == null) showError("Válassz időintervallumot");
        else return true;
        return false;
    }



    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}