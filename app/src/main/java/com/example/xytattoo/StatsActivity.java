package com.example.xytattoo;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class StatsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private BarChart artistChart;
    private LineChart monthlyChart;
    private BarChart dayChart;
    private BarChart timeSlotChart;
    private TextView userStatsText;
    private LineChart dailyChart;
    private BarChart userArtistChart;      // New chart for user's appointments by artist
    private BarChart recentArtistChart;    // New chart for recent artist workload
    private LineChart userMonthlyChart;    // New chart for user's monthly appointments

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        // Initialize Firebase and views
        db = FirebaseFirestore.getInstance();
        dayChart = findViewById(R.id.dayChart);
        timeSlotChart = findViewById(R.id.timeSlotChart);
        artistChart = findViewById(R.id.artistChart);
        userStatsText = findViewById(R.id.userActivityText);
        dailyChart = findViewById(R.id.dailyChart);
        userArtistChart = findViewById(R.id.userArtistChart);
        recentArtistChart = findViewById(R.id.recentArtistChart);
        userMonthlyChart = findViewById(R.id.userMonthlyChart);

        // Set up toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        configureCharts();
        loadAllStatistics();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void configureCharts() {
        // Configure existing bar charts
        configureBarChart(artistChart, "Appointments per Artist");
        configureBarChart(dayChart, "Most Popular Days");
        configureBarChart(timeSlotChart, "Most Popular Time Slots");

        // Configure new bar charts
        configureBarChart(userArtistChart, "Your Appointments by Artist");
        configureBarChart(recentArtistChart, "Appointments in Last 30 Days by Artist");

        // Configure existing line chart
        configureLineChart(dailyChart, "Daily Appointments");

        // Configure new line chart
        configureLineChart(userMonthlyChart, "Your Monthly Appointments");
    }

    private void loadDailyAppointments() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -30);
        String thirtyDaysAgo = new java.text.SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

        db.collection("appointments")
                .whereGreaterThanOrEqualTo("date", thirtyDaysAgo)
                .orderBy("date")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Integer> dailyCounts = new TreeMap<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String date = doc.getString("date");
                        if (date != null) {
                            dailyCounts.put(date, dailyCounts.getOrDefault(date, 0) + 1);
                        }
                    }

                    List<Entry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>(dailyCounts.keySet());
                    for (int i = 0; i < labels.size(); i++) {
                        entries.add(new Entry(i, dailyCounts.get(labels.get(i))));
                    }

                    LineDataSet dataSet = new LineDataSet(entries, "Appointments");
                    dataSet.setColor(Color.parseColor("#FF5722"));
                    dataSet.setCircleColor(Color.parseColor("#FF5722"));
                    LineData data = new LineData(dataSet);

                    dailyChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                    dailyChart.setData(data);
                    dailyChart.animateX(1000);
                    dailyChart.invalidate();
                })
                .addOnFailureListener(e -> showError("Failed to load daily appointments"));
    }

    private void configureBarChart(BarChart chart, String label) {
        chart.getDescription().setEnabled(false);
        chart.getXAxis().setTextColor(Color.BLACK);
        chart.getAxisLeft().setTextColor(Color.BLACK);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setTextColor(Color.BLACK);
        chart.setGridBackgroundColor(Color.WHITE);
        chart.getXAxis().setGranularity(1f);
        chart.getXAxis().setGranularityEnabled(true);
    }

    private void configureLineChart(LineChart chart, String label) {
        chart.getDescription().setEnabled(false);
        chart.getXAxis().setTextColor(Color.BLACK);
        chart.getAxisLeft().setTextColor(Color.BLACK);
        chart.getAxisRight().setEnabled(false);
        chart.getLegend().setTextColor(Color.BLACK);
        chart.setGridBackgroundColor(Color.WHITE);
        chart.getXAxis().setGranularity(1f);
        chart.getXAxis().setGranularityEnabled(true);
    }

    private void loadAllStatistics() {
        loadUserStats();
        loadArtistStats();
        loadDayStats();
        loadTimeSlotStats();
        loadDailyAppointments();
        loadUserArtistStats();      // New method call
        loadRecentArtistStats();    // New method call
        loadUserMonthlyStats();     // New method call
    }

    private void loadUserArtistStats() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("appointments")
                .whereEqualTo("userId", user.getUid())
                .orderBy("artistId")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Integer> artistCounts = new HashMap<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String artistId = doc.getString("artistId");
                        if (artistId != null) {
                            artistCounts.put(artistId, artistCounts.getOrDefault(artistId, 0) + 1);
                        }
                    }

                    db.collection("artists").get()
                            .addOnSuccessListener(artistsSnapshot -> {
                                List<BarEntry> entries = new ArrayList<>();
                                List<String> labels = new ArrayList<>();
                                int index = 0;

                                for (QueryDocumentSnapshot artistDoc : artistsSnapshot) {
                                    Artist artist = artistDoc.toObject(Artist.class);
                                    artist.setId(artistDoc.getId());
                                    if (artistCounts.containsKey(artist.getId())) {
                                        int count = artistCounts.get(artist.getId());
                                        entries.add(new BarEntry(index++, count));
                                        labels.add(artist.getName());
                                    }
                                }

                                BarDataSet dataSet = new BarDataSet(entries, "Appointments");
                                dataSet.setColor(Color.parseColor("#FF9800"));
                                BarData data = new BarData(dataSet);

                                userArtistChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                                userArtistChart.setData(data);
                                userArtistChart.animateY(1000);
                                userArtistChart.invalidate();
                            })
                            .addOnFailureListener(e -> showError("Failed to load artist names"));
                })
                .addOnFailureListener(e -> showError("Failed to load user artist stats"));
    }

    private void loadRecentArtistStats() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -30);
        String thirtyDaysAgo = new java.text.SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

        db.collection("appointments")
                .whereGreaterThanOrEqualTo("date", thirtyDaysAgo)
                .orderBy("date")
                .orderBy("artistId")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Integer> artistCounts = new HashMap<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String artistId = doc.getString("artistId");
                        if (artistId != null) {
                            artistCounts.put(artistId, artistCounts.getOrDefault(artistId, 0) + 1);
                        }
                    }

                    db.collection("artists").get()
                            .addOnSuccessListener(artistsSnapshot -> {
                                List<BarEntry> entries = new ArrayList<>();
                                List<String> labels = new ArrayList<>();
                                int index = 0;

                                for (QueryDocumentSnapshot artistDoc : artistsSnapshot) {
                                    Artist artist = artistDoc.toObject(Artist.class);
                                    artist.setId(artistDoc.getId());
                                    if (artistCounts.containsKey(artist.getId())) {
                                        int count = artistCounts.get(artist.getId());
                                        entries.add(new BarEntry(index++, count));
                                        labels.add(artist.getName());
                                    }
                                }

                                BarDataSet dataSet = new BarDataSet(entries, "Appointments in Last 30 Days");
                                dataSet.setColor(Color.parseColor("#795548"));
                                BarData data = new BarData(dataSet);

                                recentArtistChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                                recentArtistChart.setData(data);
                                recentArtistChart.animateY(1000);
                                recentArtistChart.invalidate();
                            })
                            .addOnFailureListener(e -> showError("Failed to load artist names"));
                })
                .addOnFailureListener(e -> showError("Failed to load recent artist stats"));
    }

    private void loadUserMonthlyStats() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("appointments")
                .whereEqualTo("userId", user.getUid())
                .orderBy("date")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Integer> monthlyCounts = new TreeMap<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String date = doc.getString("date");
                        if (date != null && date.length() >= 7) {
                            String monthYear = date.substring(0, 7); // e.g., "2023-10"
                            monthlyCounts.put(monthYear, monthlyCounts.getOrDefault(monthYear, 0) + 1);
                        }
                    }

                    List<Entry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>(monthlyCounts.keySet());
                    for (int i = 0; i < labels.size(); i++) {
                        entries.add(new Entry(i, monthlyCounts.get(labels.get(i))));
                    }

                    LineDataSet dataSet = new LineDataSet(entries, "Appointments");
                    dataSet.setColor(Color.parseColor("#4CAF50"));
                    dataSet.setCircleColor(Color.parseColor("#4CAF50"));
                    LineData data = new LineData(dataSet);

                    userMonthlyChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                    userMonthlyChart.setData(data);
                    userMonthlyChart.animateX(1000);
                    userMonthlyChart.invalidate();
                })
                .addOnFailureListener(e -> showError("Failed to load user monthly stats"));
    }

    private void loadDayStats() {
        db.collection("appointments").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Integer> dayCounts = new HashMap<>();
                    String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
                    for (String day : days) {
                        dayCounts.put(day, 0);
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            String dateStr = doc.getString("date");
                            LocalDate date = LocalDate.parse(dateStr);
                            String dayName = date.getDayOfWeek().toString();
                            dayName = dayName.substring(0, 1) + dayName.substring(1).toLowerCase();
                            dayCounts.put(dayName, dayCounts.get(dayName) + 1);
                        } catch (Exception e) {
                            Log.e("DayStats", "Error parsing date", e);
                        }
                    }

                    List<BarEntry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>();
                    int index = 0;

                    for (String day : days) {
                        entries.add(new BarEntry(index++, dayCounts.get(day)));
                        labels.add(day);
                    }

                    BarDataSet dataSet = new BarDataSet(entries, "Appointments");
                    dataSet.setColor(Color.parseColor("#2196F3"));
                    BarData data = new BarData(dataSet);

                    dayChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                    dayChart.setData(data);
                    dayChart.animateY(1000);
                    dayChart.invalidate();
                })
                .addOnFailureListener(e -> showError("Failed to load day stats"));
    }

    private void loadTimeSlotStats() {
        db.collection("appointments").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Integer> timeSlotCounts = new HashMap<>();
                    String[] slots = {"7-9", "9-11", "11-13", "13-15", "15-17"};
                    for (String slot : slots) {
                        timeSlotCounts.put(slot, 0);
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            String startTimeStr = doc.getString("startTime");
                            LocalTime startTime = LocalTime.parse(startTimeStr);
                            int hour = startTime.getHour();

                            String slot;
                            if (hour < 9) slot = "7-9";
                            else if (hour < 11) slot = "9-11";
                            else if (hour < 13) slot = "11-13";
                            else if (hour < 15) slot = "13-15";
                            else slot = "15-17";

                            timeSlotCounts.put(slot, timeSlotCounts.get(slot) + 1);
                        } catch (Exception e) {
                            Log.e("TimeStats", "Error parsing time", e);
                        }
                    }

                    List<BarEntry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>(timeSlotCounts.keySet());
                    int index = 0;

                    for (String slot : slots) {
                        entries.add(new BarEntry(index++, timeSlotCounts.get(slot)));
                    }

                    BarDataSet dataSet = new BarDataSet(entries, "Appointments");
                    dataSet.setColor(Color.parseColor("#4CAF50"));
                    BarData data = new BarData(dataSet);

                    timeSlotChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(slots));
                    timeSlotChart.setData(data);
                    timeSlotChart.animateY(1000);
                    timeSlotChart.invalidate();
                })
                .addOnFailureListener(e -> showError("Failed to load time stats"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (artistChart != null) {
            artistChart.clear();
            artistChart = null;
        }
        if (monthlyChart != null) {
            monthlyChart.clear();
            monthlyChart = null;
        }
        if (dayChart != null) {
            dayChart.clear();
            dayChart = null;
        }
        if (timeSlotChart != null) {
            timeSlotChart.clear();
            timeSlotChart = null;
        }
        if (dailyChart != null) {
            dailyChart.clear();
            dailyChart = null;
        }
        if (userArtistChart != null) {
            userArtistChart.clear();
            userArtistChart = null;
        }
        if (recentArtistChart != null) {
            recentArtistChart.clear();
            recentArtistChart = null;
        }
        if (userMonthlyChart != null) {
            userMonthlyChart.clear();
            userMonthlyChart = null;
        }
    }

    private void loadUserStats() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        db.collection("appointments")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalAppointments = queryDocumentSnapshots.size();
                    String text = String.format(Locale.getDefault(),
                            "\n\nTotal Appointments: %d\nMonthly Average: %.1f",
                            totalAppointments,
                            totalAppointments / 12.0);
                    userStatsText.setText(text);
                })
                .addOnFailureListener(e -> showError("Error loading user stats"));
    }

    private void loadArtistStats() {
        db.collection("appointments").get()
                .addOnSuccessListener(appointmentsSnapshot -> {
                    Map<String, Integer> artistCounts = new HashMap<>();

                    for (QueryDocumentSnapshot appointmentDoc : appointmentsSnapshot) {
                        String artistId = appointmentDoc.getString("artistId");
                        if (artistId != null) {
                            artistCounts.put(artistId, artistCounts.getOrDefault(artistId, 0) + 1);
                        }
                    }

                    db.collection("artists").get()
                            .addOnSuccessListener(artistsSnapshot -> {
                                List<BarEntry> entries = new ArrayList<>();
                                List<String> labels = new ArrayList<>();
                                int index = 0;

                                for (QueryDocumentSnapshot artistDoc : artistsSnapshot) {
                                    Artist artist = artistDoc.toObject(Artist.class);
                                    artist.setId(artistDoc.getId());
                                    int count = artistCounts.getOrDefault(artist.getId(), 0);
                                    entries.add(new BarEntry(index++, count));
                                    labels.add(artist.getName());
                                }

                                BarDataSet dataSet = new BarDataSet(entries, "Appointments");
                                dataSet.setColor(Color.parseColor("#FFA726"));
                                BarData data = new BarData(dataSet);

                                artistChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                                artistChart.setData(data);
                                artistChart.animateY(1000);
                                artistChart.invalidate();
                            })
                            .addOnFailureListener(e -> showError("Artist data load failed: " + e.getMessage()));
                })
                .addOnFailureListener(e -> showError("Appointment data load failed: " + e.getMessage()));
    }

    private void loadMonthlyTrends() {
        db.collection("appointments")
                .orderBy("date")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Integer> monthlyCounts = new TreeMap<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String date = doc.getString("date");
                        if (date != null && date.length() >= 7) {
                            String monthYear = date.substring(0, 7);
                            monthlyCounts.put(monthYear, monthlyCounts.getOrDefault(monthYear, 0) + 1);
                        }
                    }

                    List<Entry> entries = new ArrayList<>();
                    List<String> labels = new ArrayList<>(monthlyCounts.keySet());
                    for (int i = 0; i < labels.size(); i++) {
                        entries.add(new Entry(i, monthlyCounts.get(labels.get(i))));
                    }

                    LineDataSet dataSet = new LineDataSet(entries, "Appointments");
                    dataSet.setColor(Color.parseColor("#4CAF50"));
                    dataSet.setCircleColor(Color.parseColor("#4CAF50"));
                    LineData data = new LineData(dataSet);
                    monthlyChart.setData(data);
                    monthlyChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                    monthlyChart.animateX(1000);
                    monthlyChart.invalidate();
                })
                .addOnFailureListener(e -> showError("Failed to load monthly trends"));
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}