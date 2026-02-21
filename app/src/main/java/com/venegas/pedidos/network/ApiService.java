package com.venegas.pedidos.network;

import com.venegas.pedidos.model.LoginRequest;
import com.venegas.pedidos.model.LoginResponse;
import com.venegas.pedidos.model.ProfileResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {
    @POST("/api/v1/auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    @GET("/api/v1/auth/profile")
    Call<ProfileResponse> getProfile(@Header("Authorization") String token);
}
