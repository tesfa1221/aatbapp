package com.example.aatbapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aatbapp.api.ApiService;
import com.example.aatbapp.databinding.FragmentPlatesBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PlatesFragment extends Fragment {

    private FragmentPlatesBinding binding;
    private ApiService apiService;
    private PlatesAdapter adapter;
    private final List<Map<String, String>> plates = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPlatesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = new Retrofit.Builder()
                .baseUrl(MainActivity.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ApiService.class);

        adapter = new PlatesAdapter(plates, this::deletePlate);
        binding.rvPlates.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvPlates.setAdapter(adapter);

        binding.btnRefreshPlates.setOnClickListener(v -> loadPlates());
        binding.btnAddPlate.setOnClickListener(v -> addPlate());

        loadPlates();
    }

    private void loadPlates() {
        apiService.getPlates().enqueue(new Callback<List<Map<String, String>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, String>>> c,
                                   @NonNull Response<List<Map<String, String>>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    plates.clear();
                    plates.addAll(r.body());
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, String>>> c, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Failed to load plates", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addPlate() {
        String plate = binding.etNewPlate.getText().toString().trim().toUpperCase();
        if (plate.isEmpty()) { Toast.makeText(requireContext(), "Enter a plate number", Toast.LENGTH_SHORT).show(); return; }
        Map<String, String> body = new HashMap<>();
        body.put("plate_number", plate);
        apiService.registerPlate(body).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> c, @NonNull Response<Map<String, String>> r) {
                if (r.isSuccessful()) {
                    binding.etNewPlate.setText("");
                    Toast.makeText(requireContext(), "Plate " + plate + " registered", Toast.LENGTH_SHORT).show();
                    loadPlates();
                }
            }
            @Override public void onFailure(@NonNull Call<Map<String, String>> c, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deletePlate(String plateNumber) {
        Map<String, String> body = new HashMap<>();
        body.put("plate_number", plateNumber);
        apiService.deletePlate(body).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> c, @NonNull Response<Map<String, String>> r) {
                Toast.makeText(requireContext(), "Plate deleted", Toast.LENGTH_SHORT).show();
                loadPlates();
            }
            @Override public void onFailure(@NonNull Call<Map<String, String>> c, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    static class PlatesAdapter extends RecyclerView.Adapter<PlatesAdapter.VH> {
        private final List<Map<String, String>> data;
        private final OnDeleteListener listener;

        interface OnDeleteListener { void onDelete(String plate); }

        PlatesAdapter(List<Map<String, String>> data, OnDeleteListener listener) {
            this.data = data; this.listener = listener;
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plate, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            Map<String, String> item = data.get(pos);
            h.tvPlate.setText(item.getOrDefault("plate_number", "—"));
            h.tvDate.setText(item.getOrDefault("created_at", ""));
            h.btnDelete.setOnClickListener(v -> listener.onDelete(item.getOrDefault("plate_number", "")));
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvPlate, tvDate, btnDelete;
            VH(View v) {
                super(v);
                tvPlate   = v.findViewById(R.id.tv_plate_number);
                tvDate    = v.findViewById(R.id.tv_plate_date);
                btnDelete = v.findViewById(R.id.btn_delete);
            }
        }
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
