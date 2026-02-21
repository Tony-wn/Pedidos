package com.venegas.pedidos.activities;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.venegas.pedidos.R;
import com.venegas.pedidos.network.ApiService;
import com.venegas.pedidos.network.RetrofitClient;
import com.venegas.pedidos.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * LoginActivity — primera pantalla de la app.
 * Gestiona autenticación contra POST /auth/login.
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private MaterialButton    btnLogin;
    private ProgressBar       progressLogin;
    private SessionManager    sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new SessionManager(this);

        // Si ya hay sesión activa, ir directo a MainActivity
        if (sessionManager.isLoggedIn()) {
            goToMain();
            return;
        }

        bindViews();
        setupClickListeners();
    }

    private void bindViews() {
        etUsername    = findViewById(R.id.etUsername);
        etPassword    = findViewById(R.id.etPassword);
        btnLogin      = findViewById(R.id.btnLogin);
        progressLogin = findViewById(R.id.progressLogin);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        // También hacer login al presionar "Done" en el teclado
        etPassword.setOnEditorActionListener((v, actionId, event) -> {
            attemptLogin();
            return true;
        });
    }

    private void attemptLogin() {
        String username = getEditTextValue(etUsername);
        String password = getEditTextValue(etPassword);

        // ── Validación básica ─────────────────────────────────────────────
        if (username.isEmpty()) {
            etUsername.setError("Ingresa tu usuario");
            etUsername.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Ingresa tu contraseña");
            etPassword.requestFocus();
            return;
        }

        // ── Llamada a la API ──────────────────────────────────────────────
        setLoading(true);
        android.util.Log.d("LOGIN_DEBUG", "Intentando conectar a: " + RetrofitClient.BASE_URL);
        ApiService.LoginRequest request = new ApiService.LoginRequest(username, password);
        RetrofitClient.getService().login(request).enqueue(new Callback<ApiService.LoginResponse>() {

            @Override
            public void onResponse(Call<ApiService.LoginResponse> call,
                                   Response<ApiService.LoginResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    ApiService.LoginResponse body = response.body();

                    if (body.token != null && !body.token.isEmpty()) {
                        // ✅ Login exitoso: guardar sesión y navegar
                        sessionManager.saveSession(body.token, body.name, body.username);
                        showToast("¡Bienvenido, " + body.name + "!");
                        goToMain();
                    } else {
                        showToast("Error: respuesta sin token");
                    }
                } else {
                    // 401 u otro error HTTP
                    String msg = "Credenciales incorrectas";
                    if (response.code() == 400) msg = "Usuario y contraseña requeridos";
                    showToast(msg);
                }
            }

            @Override
            public void onFailure(Call<ApiService.LoginResponse> call, Throwable t) {
                setLoading(false);
                android.util.Log.e("LOGIN_DEBUG", "Error: " + t.getClass().getName() + " - " + t.getMessage());
                showToast("No se pudo conectar al servidor.\nVerifica tu conexión.");
            }
        });
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish(); // elimina LoginActivity del stack
    }

    private void setLoading(boolean loading) {
        btnLogin.setEnabled(!loading);
        progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setText(loading ? "Ingresando..." : "Ingresar");
    }

    private String getEditTextValue(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}