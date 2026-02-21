package com.venegas.pedidos.utils;


import android.content.Context;
import android.content.SharedPreferences;

/**
    almacena y recupera el token de autenticación
 */
public class SessionManager {

    private static final String PREFS_NAME  = "pedidos_session";
    private static final String KEY_TOKEN   = "auth_token";
    private static final String KEY_NAME    = "user_name";
    private static final String KEY_USER    = "username";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String token, String name, String username) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_NAME, name)
                .putString(KEY_USER, username)
                .apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getUserName() {
        return prefs.getString(KEY_NAME, "Vendedor");
    }

    public String getUsername() {
        return prefs.getString(KEY_USER, "");
    }

    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }

    public String getBearerToken() {
        return "Bearer " + getToken();
    }
}