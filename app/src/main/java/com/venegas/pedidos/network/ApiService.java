package com.venegas.pedidos.network;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * Interfaz Retrofit que define los endpoints de la API.
 */
public interface ApiService {

    /**
     * POST /auth/login
     * Envía credenciales y recibe token de acceso.
     */
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest body);

    /**
     * POST /orders
     * Envía un pedido con foto (multipart/form-data).
     * Requiere Authorization: Bearer <token>
     */
    @Multipart
    @POST("orders")
    Call<OrderResponse> createOrder(
            @Header("Authorization") String authHeader,
            @Part("clientName")    RequestBody clientName,
            @Part("clientPhone")   RequestBody clientPhone,
            @Part("clientAddress") RequestBody clientAddress,
            @Part("orderDetail")   RequestBody orderDetail,
            @Part("paymentType")   RequestBody paymentType,
            @Part("latitude")      RequestBody latitude,
            @Part("longitude")     RequestBody longitude,
            @Part("localId")       RequestBody localId,
            @Part("createdAt")     RequestBody createdAt,
            @Part MultipartBody.Part photo           // puede ser null
    );

    // ── Clases de Request/Response anidadas ──────────────────────────────────

    class LoginRequest {
        public String username;
        public String password;
        public LoginRequest(String u, String p) {
            this.username = u;
            this.password = p;
        }
    }

    class LoginResponse {
        public String token;
        public String name;
        public String username;
        public String message;
        public String error;
    }

    class OrderResponse {
        public String message;
        public String serverId;
        public String status;
        public String error;
    }
}