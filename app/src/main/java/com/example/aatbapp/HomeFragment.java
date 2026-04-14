package com.example.aatbapp;

import android.content.SharedPreferences;
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
import com.example.aatbapp.databinding.FragmentHomeBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ApiService apiService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = new Retrofit.Builder()
                .baseUrl(MainActivity.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ApiService.class);

        // Greeting
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("aatb_prefs", android.content.Context.MODE_PRIVATE);
        String name = prefs.getString("name", "Controller");
        binding.tvUsername.setText(name);
        binding.tvAvatarInitial.setText(name.substring(0, 1).toUpperCase());

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting = hour < 12 ? "Good morning," : hour < 17 ? "Good afternoon," : "Good evening,";
        binding.tvGreeting.setText(greeting);

        binding.tvDate.setText(new SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(new Date()));

        binding.btnQuickScan.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.scanFragment));

        binding.btnRefresh.setOnClickListener(v -> loadData());
        binding.btnNotif.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.notificationsFragment));
        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.profileFragment));

        loadData();
    }

    private void loadData() {
        apiService.getDashboardStats().enqueue(new Callback<Map<String, Integer>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Integer>> call,
                                   @NonNull Response<Map<String, Integer>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Map<String, Integer> d = r.body();
                    binding.tvTotal.setText(String.valueOf(d.getOrDefault("total_plates", 0)));
                    binding.tvPresent.setText(String.valueOf(d.getOrDefault("today_present", 0)));
                    binding.tvAbsent.setText(String.valueOf(d.getOrDefault("today_absent", 0)));
                }
            }
            @Override public void onFailure(@NonNull Call<Map<String, Integer>> c, @NonNull Throwable t) {}
        });

        apiService.getAttendanceLog().enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> call,
                                   @NonNull Response<List<Map<String, String>>> r) {
                if (r.isSuccessful() && r.body() != null) buildRecentList(r.body());
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, String>>> c, @NonNull Throwable t) {}
        });
    }

    private void buildRecentList(List<Map<String, String>> items) {
        LinearLayout container = binding.recentList;
        container.removeAllViews();
        if (items.isEmpty()) {
            addEmptyRow(container, "No scans yet today");
            return;
        }
        int limit = Math.min(items.size(), 6);
        for (int i = 0; i < limit; i++) {
            Map<String, String> item = items.get(i);
            View row = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_scan_row, container, false);
            ((TextView) row.findViewById(R.id.tv_row_plate))
                    .setText(item.getOrDefault("plate_number", "—"));
            String status = item.getOrDefault("status", "");
            TextView tvStatus = row.findViewById(R.id.tv_row_status);
            tvStatus.setText(status.toUpperCase());
            tvStatus.setTextColor("valid".equalsIgnoreCase(status) ? 0xFF22C55E : 0xFFEF4444);
            ((TextView) row.findViewById(R.id.tv_row_time))
                    .setText(item.getOrDefault("scanned_at", "—"));
            container.addView(row);
        }
    }

    private void addEmptyRow(LinearLayout container, String msg) {
        TextView tv = new TextView(requireContext());
        tv.setText(msg);
        tv.setTextColor(0xFF94A3B8);
        tv.setTextSize(13);
        tv.setPadding(0, 24, 0, 24);
        tv.setGravity(android.view.Gravity.CENTER);
        container.addView(tv);
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
