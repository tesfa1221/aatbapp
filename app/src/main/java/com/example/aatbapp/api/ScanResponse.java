package com.example.aatbapp.api;

import com.google.gson.annotations.SerializedName;

public class ScanResponse {
    @SerializedName("plate")
    private String plate;
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("reason")
    private String reason;

    public String getPlate() { return plate; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
}
