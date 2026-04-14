package com.example.aatbapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aatbapp.api.ApiService;
import com.example.aatbapp.databinding.FragmentNotificationsBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private ApiService apiService;
    private NotifAdapter adapter;
    private final List<Map<String, String>> items = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = new Retrofit.Builder()
                .baseUrl(MainActivity.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ApiService.class);

        adapter = new NotifAdapter(items);
        binding.rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvNotifications.setAdapter(adapter);

        binding.btnRefreshNotif.setOnClickListener(v -> loadNotifications());
        loadNotifications();
    }

    private void loadNotifications() {
        // Use attendance log as notification source — rejected scans become alerts
        apiService.getAttendanceLog().enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> c,
                                   @NonNull Response<List<Map<String, String>>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    items.clear();
                    // Filter to show rejected/notable events
                    for (Map<String, String> entry : r.body()) {
                        String status = entry.getOrDefault("status", "");
                        if (!"valid".equalsIgnoreCase(status)) items.add(entry);
                        if (items.size() >= 30) break;
                    }
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, String>>> c, @NonNull Throwable t) {}
        });
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    static class NotifAdapter extends RecyclerView.Adapter<NotifAdapter.VH> {
        private final List<Map<String, String>> data;
        NotifAdapter(List<Map<String, String>> data) { this.data = data; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Map<String, String> item = data.get(pos);
            String status = item.getOrDefault("status", "");
            String reason = item.getOrDefault("reason", "");
            String plate  = item.getOrDefault("plate_number", "—");
            String time   = item.getOrDefault("scanned_at", "");

            boolean isRejected = "rejected".equalsIgnoreCase(status);
            h.tvIcon.setText(isRejected ? "⚠️" : "ℹ️");
            h.tvTitle.setText(isRejected ? "Scan Rejected — " + plate : "Scan Alert — " + plate);
            h.tvBody.setText(reason.isEmpty() ? status : reason);
            h.tvTime.setText(time.length() > 16 ? time.substring(11, 16) : time);
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvIcon, tvTitle, tvBody, tvTime;
            VH(View v) {
                super(v);
                tvIcon  = v.findViewById(R.id.tv_notif_icon);
                tvTitle = v.findViewById(R.id.tv_notif_title);
                tvBody  = v.findViewById(R.id.tv_notif_body);
                tvTime  = v.findViewById(R.id.tv_notif_time);
            }
        }
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
