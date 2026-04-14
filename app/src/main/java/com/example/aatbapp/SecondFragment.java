package com.example.aatbapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.aatbapp.api.ApiService;
import com.example.aatbapp.api.ScanResponse;
import com.example.aatbapp.databinding.FragmentSecondBinding;
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

public class SecondFragment extends Fragment {

    private static final String TAG = "SecondFragment";
    private FragmentSecondBinding binding;
    private ImageCapture imageCapture;
    private ApiService apiService;
    private boolean isRegisterMode = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);
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

        // Read mode from arguments
        if (getArguments() != null) {
            isRegisterMode = getArguments().getBoolean("register_mode", false);
        }
        applyMode(isRegisterMode);

        startCamera();

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment));

        binding.buttonCapture.setOnClickListener(v -> captureAndProcess());

        binding.btnModeScan.setOnClickListener(v -> applyMode(false));
        binding.btnModeRegister.setOnClickListener(v -> applyMode(true));
    }

    private void applyMode(boolean registerMode) {
        isRegisterMode = registerMode;
        if (registerMode) {
            binding.tvModeLabel.setText("REGISTER PLATE");
            binding.buttonCapture.setText("CAPTURE & REGISTER");
            binding.buttonCapture.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.success));
            binding.btnModeRegister.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.success));
            binding.btnModeRegister.setTextColor(0xFFFFFFFF);
            binding.btnModeScan.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.surface));
            binding.btnModeScan.setTextColor(0xFF94A3B8);
        } else {
            binding.tvModeLabel.setText("SCAN ATTENDANCE");
            binding.buttonCapture.setText("CAPTURE & SCAN");
            binding.buttonCapture.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.primary));
            binding.btnModeScan.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.primary));
            binding.btnModeScan.setTextColor(0xFFFFFFFF);
            binding.btnModeRegister.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.surface));
            binding.btnModeRegister.setTextColor(0xFF94A3B8);
        }
        binding.tvResult.setVisibility(View.GONE);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(requireContext());
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.cameraPreview.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();
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
        binding.tvResult.setVisibility(View.GONE);

        File outputFile = new File(requireContext().getCacheDir(), "plate_capture.jpg");
        ImageCapture.OutputFileOptions options =
                new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        imageCapture.takePicture(options,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults result) {
                        Bitmap bitmap = BitmapFactory.decodeFile(outputFile.getAbsolutePath());
                        if (bitmap == null) { resetUI(); showResult("Failed to read image", false); return; }

                        SpoofingDetector.Result spoof = SpoofingDetector.analyze(bitmap);
                        if (spoof.spoofed) {
                            resetUI();
                            showResult("Rejected: " + spoof.reason, false);
                            return;
                        }
                        if (isRegisterMode) uploadForRegistration(outputFile);
                        else uploadForScan(outputFile);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        resetUI();
                        showResult("Capture failed", false);
                    }
                });
    }

    private void uploadForScan(File file) {
        RequestBody reqBody = RequestBody.create(file, MediaType.parse("image/jpeg"));
        MultipartBody.Part part = MultipartBody.Part.createFormData("images[]", file.getName(), reqBody);

        apiService.scanPlate(Collections.singletonList(part)).enqueue(new Callback<ScanResponse>() {
            @Override
            public void onResponse(@NonNull Call<ScanResponse> call, @NonNull Response<ScanResponse> response) {
                resetUI();
                if (response.isSuccessful() && response.body() != null) {
                    ScanResponse body = response.body();
                    String plate = body.getPlate() != null ? body.getPlate() : "Unknown";
                    String status = body.getStatus() != null ? body.getStatus() : "";
                    boolean valid = "Valid".equalsIgnoreCase(status);
                    String msg = plate + "\n" + status.toUpperCase();
                    if (body.getReason() != null) msg += "\n" + body.getReason();
                    showResult(msg, valid);
                } else {
                    showResult("Server error: " + response.code(), false);
                }
            }
            @Override
            public void onFailure(@NonNull Call<ScanResponse> call, @NonNull Throwable t) {
                resetUI(); showResult("Network error", false);
            }
        });
    }

    private void uploadForRegistration(File file) {
        // First scan to get plate text, then register it
        RequestBody reqBody = RequestBody.create(file, MediaType.parse("image/jpeg"));
        MultipartBody.Part part = MultipartBody.Part.createFormData("images[]", file.getName(), reqBody);

        apiService.scanPlate(Collections.singletonList(part)).enqueue(new Callback<ScanResponse>() {
            @Override
            public void onResponse(@NonNull Call<ScanResponse> call, @NonNull Response<ScanResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String plate = response.body().getPlate();
                    if (plate == null || plate.isEmpty()) {
                        resetUI(); showResult("No plate detected", false); return;
                    }
                    // Register the detected plate
                    Map<String, String> body = new HashMap<>();
                    body.put("plate_number", plate);
                    apiService.registerPlate(body).enqueue(new Callback<Map<String, String>>() {
                        @Override
                        public void onResponse(@NonNull Call<Map<String, String>> c,
                                               @NonNull Response<Map<String, String>> r) {
                            resetUI();
                            if (r.isSuccessful()) showResult("Registered: " + plate, true);
                            else showResult("Registration failed", false);
                        }
                        @Override
                        public void onFailure(@NonNull Call<Map<String, String>> c, @NonNull Throwable t) {
                            resetUI(); showResult("Network error", false);
                        }
                    });
                } else {
                    resetUI(); showResult("Scan failed", false);
                }
            }
            @Override
            public void onFailure(@NonNull Call<ScanResponse> call, @NonNull Throwable t) {
                resetUI(); showResult("Network error", false);
            }
        });
    }

    private void showResult(String msg, boolean success) {
        binding.tvResult.setText(msg);
        binding.tvResult.setTextColor(success ? 0xFF22C55E : 0xFFEF4444);
        binding.tvResult.setVisibility(View.VISIBLE);
    }

    private void resetUI() {
        binding.buttonCapture.setEnabled(true);
        binding.progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
