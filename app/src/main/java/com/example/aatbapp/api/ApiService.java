package com.example.aatbapp.api;

import java.util.List;
import java.util.Map;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {

    @POST("login.php")
    Call<LoginResponse> login(@Body Map<String, String> body);

    @Multipart
    @POST("scan.php")
    Call<ScanResponse> scanPlate(@Part List<MultipartBody.Part> images);

    @POST("register.php")
    Call<Map<String, String>> registerPlate(@Body Map<String, String> body);

    @POST("delete_plate.php")
    Call<Map<String, String>> deletePlate(@Body Map<String, String> body);

    @GET("dashboard_stats.php")
    Call<Map<String, Integer>> getDashboardStats();

    @GET("attendance_log.php")
    Call<List<Map<String, String>>> getAttendanceLog();

    @GET("get_plates.php")
    Call<List<Map<String, String>>> getPlates();
}
