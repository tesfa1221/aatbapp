package com.example.aatbapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.aatbapp.api.ApiService;
import com.example.aatbapp.api.ScanResponse;
import com.example.aatbapp.databinding.FragmentScanBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ScanFragment extends Fragment {

    private static final String TAG = "ScanFragment";
    private FragmentScanBinding binding;
    private ImageCapture imageCapture;
    private ApiService apiService;
    private boolean isRegisterMode = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentScanBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = new Retrofit.Builder()
                .baseUrl(MainActivity.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(ApiService.class);

        startCamera();
        applyMode(false);

        binding.btnModeScan.setOnClickListener(v -> applyMode(false));
        binding.btnModeRegister.setOnClickListener(v -> applyMode(true));
        binding.buttonCapture.setOnClickListener(v -> captureAndProcess());
    }

    private void applyMode(boolean register) {
        isRegisterMode = register;
        if (register) {
            binding.tvModeLabel.setText("REGISTER PLATE");
            binding.buttonCapture.setText("📷  CAPTURE & REGISTER");
            binding.buttonCapture.setBackgroundResource(R.drawable.bg_btn_success);
            binding.btnModeRegister.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.success));
            binding.btnModeRegister.setTextColor(0xFFFFFFFF);
            binding.btnModeScan.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.surface));
            binding.btnModeScan.setTextColor(0xFF94A3B8);
        } else {
            binding.tvModeLabel.setText("SCAN ATTENDANCE");
            binding.buttonCapture.setText("📷  CAPTURE & SCAN");
            binding.buttonCapture.setBackgroundResource(R.drawable.bg_btn_primary);
            binding.btnModeScan.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.primary));
            binding.btnModeScan.setTextColor(0xFFFFFFFF);
            binding.btnModeRegister.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.surface));
            binding.btnModeRegister.setTextColor(0xFF94A3B8);
        }
        binding.resultCard.setVisibility(View.GONE);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.cameraPreview.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();
                provider.unbindAll();
                provider.bindToLifecycle(getViewLifecycleOwner(),
                        CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera init failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void captureAndProcess() {
        if (imageCapture == null) return;
        binding.buttonCapture.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.resultCard.setVisibility(View.GONE);

        File outputFile = new File(requireContext().getCacheDir(), "plate_capture.jpg");
        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(outputFile).build(),
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults result) {
                        Bitmap bmp = BitmapFactory.decodeFile(outputFile.getAbsolutePath());
                        if (bmp == null) { resetUI(); showResult("Failed to read image", null, false); return; }
                        SpoofingDetector.Result spoof = SpoofingDetector.analyze(bmp);
                        if (spoof.spoofed) { resetUI(); showResult("Rejected", spoof.reason, false); return; }
                        if (isRegisterMode) uploadForRegistration(outputFile);
                        else uploadForScan(outputFile);
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        resetUI(); showResult("Capture failed", e.getMessage(), false);
                    }
                });
    }

    private void uploadForScan(File file) {
        RequestBody rb = RequestBody.create(file, MediaType.parse("image/jpeg"));
        MultipartBody.Part part = MultipartBody.Part.createFormData("images[]", file.getName(), rb);
        apiService.scanPlate(Collections.singletonList(part)).enqueue(new Callback<ScanResponse>() {
            @Override
            public void onResponse(@NonNull Call<ScanResponse> c, @NonNull Response<ScanResponse> r) {
                resetUI();
                if (r.isSuccessful() && r.body() != null) {
                    ScanResponse body = r.body();
                    boolean valid = "Valid".equalsIgnoreCase(body.getStatus());
                    showResult(body.getPlate() != null ? body.getPlate() : "—",
                            body.getStatus(), valid);
                } else showResult("Server error", "Code: " + r.code(), false);
            }
            @Override public void onFailure(@NonNull Call<ScanResponse> c, @NonNull Throwable t) {
                resetUI(); showResult("Network error", t.getMessage(), false);
            }
        });
    }

    private void uploadForRegistration(File file) {
        RequestBody rb = RequestBody.create(file, MediaType.parse("image/jpeg"));
        MultipartBody.Part part = MultipartBody.Part.createFormData("images[]", file.getName(), rb);
        apiService.scanPlate(Collections.singletonList(part)).enqueue(new Callback<ScanResponse>() {
            @Override
            public void onResponse(@NonNull Call<ScanResponse> c, @NonNull Response<ScanResponse> r) {
                if (r.isSuccessful() && r.body() != null && r.body().getPlate() != null) {
                    String plate = r.body().getPlate();
                    Map<String, String> body = new HashMap<>();
                    body.put("plate_number", plate);
                    apiService.registerPlate(body).enqueue(new Callback<Map<String, String>>() {
                        @Override
                        public void onResponse(@NonNull Call<Map<String, String>> cc,
                                               @NonNull Response<Map<String, String>> rr) {
                            resetUI();
                            if (rr.isSuccessful()) showResult(plate, "Registered ✓", true);
                            else showResult(plate, "Registration failed", false);
                        }
                        @Override public void onFailure(@NonNull Call<Map<String, String>> cc, @NonNull Throwable t) {
                            resetUI(); showResult("Error", t.getMessage(), false);
                        }
                    });
                } else { resetUI(); showResult("No plate detected", null, false); }
            }
            @Override public void onFailure(@NonNull Call<ScanResponse> c, @NonNull Throwable t) {
                resetUI(); showResult("Network error", t.getMessage(), false);
            }
        });
    }

    private void showResult(String plate, String status, boolean success) {
        binding.tvPlateDetected.setText(plate != null ? plate : "—");
        if (status != null) {
            binding.tvResultStatus.setText(status.toUpperCase());
            binding.tvResultStatus.setTextColor(success ? 0xFF22C55E : 0xFFEF4444);
        }
        binding.tvResultReason.setVisibility(View.GONE);
        binding.resultCard.setVisibility(View.VISIBLE);
    }

    private void resetUI() {
        binding.buttonCapture.setEnabled(true);
        binding.progressBar.setVisibility(View.GONE);
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
