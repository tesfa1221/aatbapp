package com.example.aatbapp.api;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("status")  private String status;
    @SerializedName("name")    private String name;
    @SerializedName("username")private String username;
    @SerializedName("role")    private String role;
    @SerializedName("token")   private String token;
    @SerializedName("message") private String message;

    public String getStatus()   { return status; }
    public String getName()     { return name; }
    public String getUsername() { return username; }
    public String getRole()     { return role; }
    public String getToken()    { return token; }
    public String getMessage()  { return message; }
}
