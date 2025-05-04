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
    private TextView userStatsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        // Initialize Firebase and views
        db = FirebaseFirestore.getInstance();
        artistChart = findViewById(R.id.artistChart);
        monthlyChart = findViewById(R.id.monthlyChart);
        userStatsText = findViewById(R.id.userActivityText);

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
        // Configure bar chart
        artistChart.getDescription().setEnabled(false);
        artistChart.getXAxis().setTextColor(Color.BLACK);
        artistChart.getAxisLeft().setTextColor(Color.BLACK);
        artistChart.getAxisRight().setEnabled(false);
        artistChart.getLegend().setTextColor(Color.BLACK);
        artistChart.setGridBackgroundColor(Color.WHITE);

        // Configure line chart
        monthlyChart.getDescription().setEnabled(false);
        monthlyChart.getXAxis().setTextColor(Color.BLACK);
        monthlyChart.getAxisLeft().setTextColor(Color.BLACK);
        monthlyChart.getAxisRight().setEnabled(false);
        monthlyChart.getLegend().setTextColor(Color.BLACK);
        monthlyChart.setGridBackgroundColor(Color.WHITE);
    }

    private void loadAllStatistics() {
        loadUserStats();
        loadArtistStats();
        loadMonthlyTrends();
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
                            "Your Statistics\n\nTotal Appointments: %d\nMonthly Average: %.1f",
                            totalAppointments,
                            totalAppointments / 12.0);
                    userStatsText.setText(text);
                })
                .addOnFailureListener(e -> showError("Error loading user stats"));
    }

    private void loadArtistStats() {
        db.collection("appointments").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Integer> artistCounts = new HashMap<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String artistId = doc.getString("artistId");
                        if (artistId != null) {
                            artistCounts.put(artistId, artistCounts.getOrDefault(artistId, 0) + 1);
                        }
                    }

                    db.collection("artists").get().addOnSuccessListener(artistSnapshots -> {
                        List<BarEntry> entries = new ArrayList<>();
                        List<String> labels = new ArrayList<>();
                        int index = 0;

                        for (QueryDocumentSnapshot artistDoc : artistSnapshots) {
                            Artist artist = artistDoc.toObject(Artist.class);
                            int count = artistCounts.getOrDefault(artist.getId(), 0);
                            entries.add(new BarEntry(index++, count));
                            labels.add(artist.getName());
                        }

                        BarDataSet dataSet = new BarDataSet(entries, "Appointments");
                        dataSet.setColor(Color.parseColor("#FFA726"));
                        BarData data = new BarData(dataSet);
                        artistChart.setData(data);
                        artistChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
                        artistChart.animateY(1000);
                        artistChart.invalidate();
                    });
                })
                .addOnFailureListener(e -> showError("Failed to load artist stats"));
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