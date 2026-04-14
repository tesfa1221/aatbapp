package com.example.aatbapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.aatbapp.api.ApiService;
import com.example.aatbapp.api.LoginResponse;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private ProgressBar progress;
    private TextView tvError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Auto-login if session exists
        SharedPreferences prefs = getSharedPreferences("aatb_prefs", MODE_PRIVATE);
        if (prefs.contains("token")) {
            startMain();
            return;
        }

        setContentView(R.layout.activity_login);
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin   = findViewById(R.id.btn_login);
        progress   = findViewById(R.id.progress_login);
        tvError    = findViewById(R.id.tv_error);

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password");
            return;
        }

        btnLogin.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(MainActivity.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        ApiService api = retrofit.create(ApiService.class);

        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);

        api.login(body).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                progress.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse r = response.body();
                    if ("success".equals(r.getStatus())) {
                        // Save session
                        getSharedPreferences("aatb_prefs", MODE_PRIVATE).edit()
                                .putString("token", r.getToken())
                                .putString("name", r.getName())
                                .putString("username", r.getUsername())
                                .putString("role", r.getRole())
                                .apply();
                        startMain();
                    } else {
                        showError(r.getMessage() != null ? r.getMessage() : "Invalid credentials");
                    }
                } else {
                    showError("Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                progress.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                showError("Network error. Check server connection.");
            }
        });
    }

    private void showError(String msg) {
        tvError.setText(msg);
        tvError.setVisibility(View.VISIBLE);
    }

    private void startMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
