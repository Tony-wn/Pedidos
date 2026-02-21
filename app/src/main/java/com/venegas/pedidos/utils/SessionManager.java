package com.venegas.pedidos.utils;


import android.content.Context;
import android.content.SharedPreferences;

/**
 * SessionManager — almacena y recupera el token de autenticación
 * usando SharedPreferences (persistente entre reinicios de la app).
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

    /** Guarda el token y datos del usuario después del login exitoso. */
    public void saveSession(String token, String name, String username) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_NAME, name)
                .putString(KEY_USER, username)
                .apply();
    }

    /** Devuelve el token guardado, o null si no hay sesión. */
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    /** Devuelve el nombre completo del usuario. */
    public String getUserName() {
        return prefs.getString(KEY_NAME, "Vendedor");
    }

    /** Devuelve el username. */
    public String getUsername() {
        return prefs.getString(KEY_USER, "");
    }

    /** Verifica si hay una sesión activa. */
    public boolean isLoggedIn() {
        String token = getToken();
        return token != null && !token.isEmpty();
    }

    /** Elimina la sesión (logout). */
    public void clearSession() {
        prefs.edit().clear().apply();
    }

    /** Devuelve el header Authorization listo para usar en Retrofit. */
    public String getBearerToken() {
        return "Bearer " + getToken();
    }
}