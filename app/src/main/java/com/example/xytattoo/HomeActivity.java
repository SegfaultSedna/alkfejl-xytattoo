package com.example.xytattoo;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {
    private static final String LOG_TAG = HomeActivity.class.getName();
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make status bar transparent before setContentView
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_home);

        Button appointmentsButton = findViewById(R.id.appointmentsButton);
        // Load and apply the animation
        Animation slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in);
        appointmentsButton.startAnimation(slideUpAnimation);

        appointmentsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AppointmentsActivity.class);
            startActivity(intent);
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        setupToolbar();
        setupBookingButton();

        TextView welcomeText = findViewById(R.id.tvWelcome);
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        fadeIn.setDuration(1000);
        welcomeText.startAnimation(fadeIn);

        // Add this code in your HomeActivity.java onCreate method
        Button adminPanelButton = findViewById(R.id.adminPanelButton);
        adminPanelButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, StatsActivity.class);
            startActivity(intent);
        });

        new Handler().postDelayed(() -> adminPanelButton.startAnimation(slideUpAnimation), 400);

        /* Show admin button only for admin users
        if (user != null && isAdmin(user.getUid())) {
            adminButton.setVisibility(View.VISIBLE);
        } else {
            adminButton.setVisibility(View.GONE);
        }*/
    }



    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                performLogout();
                return true;
            }
            return false;
        });
    }

    private void setupBookingButton() {
        Button btnBook = findViewById(R.id.btnBookAppointment);
        // Adding animation with delay
        Animation slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in);
        new Handler().postDelayed(() -> btnBook.startAnimation(slideUpAnimation), 200);

        btnBook.setOnClickListener(v -> {
            if (user != null) {
                startActivity(new Intent(HomeActivity.this, ReservationActivity.class));
            } else {
                Log.d(LOG_TAG, "User not logged in, redirecting to login");
                startActivity(new Intent(HomeActivity.this, MainActivity.class));
            }
        });
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh any data when returning to this activity
        Log.d(LOG_TAG, "HomeActivity: onResume called");

        // Refresh UI or restart animations if needed
        if (user != null && user.getDisplayName() != null) {
            TextView welcomeText = findViewById(R.id.tvWelcome);
            welcomeText.setText("Üdvözlünk, " + user.getDisplayName() + "!");

            // Apply fade-in animation again
            Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
            fadeIn.setDuration(800);
            welcomeText.startAnimation(fadeIn);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "HomeActivity: onPause called");

        // Save any important state before the activity becomes invisible
        // For example, save scroll positions, form data, etc.

        // Cancel any running animations to prevent memory leaks
        ViewGroup rootView = findViewById(R.id.homeRoot);
        cancelAnimations(rootView);
    }

    // Helper method to cancel animations recursively
    private void cancelAnimations(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            child.clearAnimation();

            if (child instanceof ViewGroup) {
                cancelAnimations((ViewGroup) child);
            }
        }
    }


}