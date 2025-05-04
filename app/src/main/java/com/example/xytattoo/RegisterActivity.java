package com.example.xytattoo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {
    private static final String LOG_TAG = RegisterActivity.class.getName();
    private static final String PREF_KEY = MainActivity.class.getPackage().toString();
    private SharedPreferences preferences;

    private FirebaseAuth mAuth;

    EditText emailET;
    EditText passwordET;
    EditText passwordConfirmET;
    EditText lastNameET;
    EditText firstNameET;
    EditText phoneET;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make status bar transparent before setContentView
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_register);

        // Find the container view
        LinearLayout container = findViewById(R.id.containerRegistration);

        // Load the animation defined in res/anim/fade_slide_in.xml
        Animation fadeSlideIn = AnimationUtils.loadAnimation(this, R.anim.fade_slide_in);

        // Start the animation on the container
        container.startAnimation(fadeSlideIn);

        int secret_key = getIntent().getIntExtra("SECRET_KEY", 0);

        if (secret_key != 99) {
            finish();
        }

        emailET = findViewById(R.id.etEmail);
        passwordET = findViewById(R.id.etPassword);
        passwordConfirmET = findViewById(R.id.etConfirmPassword);
        lastNameET = findViewById(R.id.etLastName);
        firstNameET = findViewById(R.id.etFirstName);
        phoneET = findViewById(R.id.etPhoneNumber);

        preferences = getSharedPreferences(PREF_KEY, MODE_PRIVATE);
        String email = preferences.getString("email", "");
        String password = preferences.getString("password", "");

        emailET.setText(email);
        passwordET.setText(password);
        passwordConfirmET.setText(password);


        Log.i(LOG_TAG, "onCreate");
        mAuth = FirebaseAuth.getInstance();
    }

    public void goToLogin(View view) {
        // Navigate back to the login page
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Close the current activity
    }

    public void registerUser(View view) {
        String email = emailET.getText().toString();
        String password = passwordET.getText().toString();
        String passwordConfirm = passwordConfirmET.getText().toString();
        String lastName = lastNameET.getText().toString();
        String firstName = firstNameET.getText().toString();
        String phone = phoneET.getText().toString();

        // Check if any field is empty
        if (email.isEmpty()) {
            emailET.setError("Az email mező nem lehet üres!");
            emailET.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordET.setError("A jelszó mező nem lehet üres!");
            passwordET.requestFocus();
            return;
        }

        if (passwordConfirm.isEmpty()) {
            passwordConfirmET.setError("A jelszó megerősítése mező nem lehet üres!");
            passwordConfirmET.requestFocus();
            return;
        }

        if (lastName.isEmpty()) {
            lastNameET.setError("A vezetéknév mező nem lehet üres!");
            lastNameET.requestFocus();
            return;
        }

        if (firstName.isEmpty()) {
            firstNameET.setError("A keresztnév mező nem lehet üres!");
            firstNameET.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            phoneET.setError("A telefonszám mező nem lehet üres!");
            phoneET.requestFocus();
            return;
        }

        if (!password.equals(passwordConfirm)) {
            passwordConfirmET.setError("A jelszavak nem egyeznek meg!");
            passwordConfirmET.requestFocus();
            return;
        }

        // Check if the email format is valid
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailET.setError("Érvénytelen email formátum!");
            emailET.requestFocus();
            return;
        }

        // Check if the password is at least 6 characters long
        if (password.length() < 6) {
            passwordET.setError("A jelszónak legalább 6 karakter hosszúnak kell lennie!");
            passwordET.requestFocus();
            return;
        }


        Log.i(LOG_TAG, "> [Regisztrált]: " + lastName + " " + firstName + "\nAz alábbi adatokkal:\nemail: " + email + ", jelszó: " + password + ", jelszó megerősítése: " + passwordConfirm + ", telefonszám: " + phone);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();

                        Toast.makeText(RegisterActivity.this, "Regisztráció sikeres!\nKérjük jelentkezzen be.", Toast.LENGTH_LONG).show();

                        // Navigate back to the login page
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish(); // Close the current activity
                    } else {
                        Log.e(LOG_TAG, "Regisztrációs hiba: " + task.getException());
                        Toast.makeText(RegisterActivity.this, "Regisztrációs hiba: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.i(LOG_TAG, "onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(LOG_TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(LOG_TAG, "onDestroy");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(LOG_TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(LOG_TAG, "onPause");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(LOG_TAG, "onRestart");
    }
}