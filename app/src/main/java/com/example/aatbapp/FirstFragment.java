package com.example.aatbapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.aatbapp.api.ApiService;
import com.example.aatbapp.databinding.FragmentFirstBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private ApiService apiService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(MainActivity.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        // Date
        String date = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(new Date());
        binding.tvDate.setText(date);

        // Navigate to scan screen
        binding.btnScanAttendance.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean("register_mode", false);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_FirstFragment_to_SecondFragment, args);
        });

        binding.btnRegisterPlate.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putBoolean("register_mode", true);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_FirstFragment_to_SecondFragment, args);
        });

        binding.btnRefresh.setOnClickListener(v -> loadDashboard());

        loadDashboard();
    }

    private void loadDashboard() {
        apiService.getDashboardStats().enqueue(new Callback<Map<String, Integer>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Integer>> call,
                                   @NonNull Response<Map<String, Integer>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Integer> d = response.body();
                    binding.tvTotal.setText(String.valueOf(d.getOrDefault("total_plates", 0)));
                    binding.tvPresent.setText(String.valueOf(d.getOrDefault("today_present", 0)));
                    binding.tvAbsent.setText(String.valueOf(d.getOrDefault("today_absent", 0)));
                    binding.tvScans.setText(String.valueOf(d.getOrDefault("today_scans", 0)));
                }
            }
            @Override public void onFailure(@NonNull Call<Map<String, Integer>> call, @NonNull Throwable t) {}
        });

        apiService.getAttendanceLog().enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> call,
                                   @NonNull Response<List<Map<String, String>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    populateRecentList(response.body());
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, String>>> call, @NonNull Throwable t) {}
        });
    }

    private void populateRecentList(List<Map<String, String>> items) {
        LinearLayout container = binding.recentList;
        container.removeAllViews();
        if (items.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("No scans yet");
            empty.setTextColor(0xFF94A3B8);
            empty.setPadding(0, 16, 0, 16);
            container.addView(empty);
            return;
        }
        int limit = Math.min(items.size(), 6);
        for (int i = 0; i < limit; i++) {
            Map<String, String> item = items.get(i);
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_scan_row, container, false);

            TextView tvPlate  = row.findViewById(R.id.tv_row_plate);
            TextView tvStatus = row.findViewById(R.id.tv_row_status);
            TextView tvTime   = row.findViewById(R.id.tv_row_time);

            tvPlate.setText(item.getOrDefault("plate_number", "—"));
            String status = item.getOrDefault("status", "");
            tvStatus.setText(status.toUpperCase());
            boolean valid = "valid".equalsIgnoreCase(status);
            tvStatus.setTextColor(valid ? 0xFF22C55E : 0xFFEF4444);
            tvTime.setText(item.getOrDefault("scanned_at", "—"));

            container.addView(row);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
