package com.example.aatbapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.aatbapp.api.ApiService;
import com.example.aatbapp.databinding.FragmentProfileBinding;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ApiService apiService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = new Retrofit.Builder()
                .baseUrl(MainActivity.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ApiService.class);

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("aatb_prefs", android.content.Context.MODE_PRIVATE);

        String name     = prefs.getString("name", "Controller");
        String username = prefs.getString("username", "—");
        String role     = prefs.getString("role", "controller");

        binding.tvProfileName.setText(name);
        binding.tvProfileInitial.setText(name.substring(0, 1).toUpperCase());
        binding.tvProfileRole.setText(role.toUpperCase());
        binding.tvProfileUsername.setText(username);
        binding.tvProfileRole2.setText(role);

        loadStats();

        binding.btnLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });
    }

    private void loadStats() {
        apiService.getDashboardStats().enqueue(new Callback<Map<String, Integer>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Integer>> c,
                                   @NonNull Response<Map<String, Integer>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    Map<String, Integer> d = r.body();
                    binding.tvProfScans.setText(String.valueOf(d.getOrDefault("today_scans", 0)));
                    binding.tvProfPresent.setText(String.valueOf(d.getOrDefault("today_present", 0)));
                    binding.tvProfAbsent.setText(String.valueOf(d.getOrDefault("today_absent", 0)));
                }
            }
            @Override public void onFailure(@NonNull Call<Map<String, Integer>> c, @NonNull Throwable t) {}
        });
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
