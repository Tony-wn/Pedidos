package com.venegas.pedidos.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * RetrofitClient — singleton que provee la instancia de ApiService.
 *
 * ⚠️  BASE_URL:
 *   - Emulador Android:       http://10.0.2.2:3000/
 *   - Dispositivo físico LAN: http://192.168.X.X:3000/   (cambiar según tu red)
 */
public class RetrofitClient {

    // ── CAMBIAR ESTA URL según tu entorno ────────────────────────────────────
    public static final String BASE_URL = "http://192.168.1.26:3000/";
    // Para dispositivo físico usa tu IP local, ej: "http://192.168.1.100:3000/"

    private static ApiService apiService;

    public static synchronized ApiService getService() {
        if (apiService == null) {
            // Logging interceptor (ver tráfico HTTP en Logcat)
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)  // más tiempo por si sube foto grande
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }
}