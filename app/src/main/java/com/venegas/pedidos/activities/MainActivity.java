package com.venegas.pedidos.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.venegas.pedidos.R;
import com.venegas.pedidos.database.DatabaseHelper;
import com.venegas.pedidos.models.Order;
import com.venegas.pedidos.network.ApiService;
import com.venegas.pedidos.network.RetrofitClient;
import com.venegas.pedidos.utils.SessionManager;

import java.io.File;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MainActivity — pantalla principal con la lista de pedidos
 * y el botón de sincronización manual.
 */
public class MainActivity extends AppCompatActivity {

    private RecyclerView   recyclerOrders;
    private View           layoutEmpty;
    private ProgressBar    progressSync;
    private TextView       tvPendingCount;
    private TextView       tvSyncedCount;
    private TextView       tvErrorCount;

    private OrdersAdapter  adapter;
    private List<Order>    orderList;
    private DatabaseHelper db;
    private SessionManager sessionManager;

    // Contador para la sincronización secuencial
    private int syncIndex      = 0;
    private int syncSuccess    = 0;
    private int syncErrors     = 0;
    private List<Order> pendingOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getResources().getColor(R.color.primary, null));
        db             = DatabaseHelper.getInstance(this);
        sessionManager = new SessionManager(this);

        setupToolbar();
        bindViews();
        setupRecyclerView();
        setupButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar lista cada vez que se vuelve a la pantalla
        loadOrders();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mis Pedidos - " + sessionManager.getUserName());
        }
    }

    private void bindViews() {
        recyclerOrders  = findViewById(R.id.recyclerOrders);
        layoutEmpty     = findViewById(R.id.layoutEmpty);
        progressSync    = findViewById(R.id.progressSync);
        tvPendingCount  = findViewById(R.id.tvPendingCount);
        tvSyncedCount   = findViewById(R.id.tvSyncedCount);
        tvErrorCount    = findViewById(R.id.tvErrorCount);
    }

    private void setupRecyclerView() {
        recyclerOrders.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupButtons() {
        MaterialButton btnNewOrder = findViewById(R.id.btnNewOrder);
        MaterialButton btnSync     = findViewById(R.id.btnSync);

        btnNewOrder.setOnClickListener(v ->
                startActivity(new Intent(this, NewOrderActivity.class)));

        btnSync.setOnClickListener(v -> startSync());
    }

    // ── Cargar lista de pedidos desde SQLite ──────────────────────────────────
    private void loadOrders() {
        orderList = db.getAllOrders();

        if (orderList.isEmpty()) {
            recyclerOrders.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            recyclerOrders.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);

            adapter = new OrdersAdapter(orderList, order -> {
                Intent intent = new Intent(this, OrderDetailActivity.class);
                intent.putExtra("order_id", order.getId());
                startActivity(intent);
            });
            recyclerOrders.setAdapter(adapter);
        }

        updateStats();
    }

    private void updateStats() {
        int pending = db.countByStatus(Order.STATUS_PENDING);
        int synced  = db.countByStatus(Order.STATUS_SYNCED);
        int error   = db.countByStatus(Order.STATUS_ERROR);

        tvPendingCount.setText("⏳ " + pending + "\nPendientes");
        tvSyncedCount.setText("✅ " + synced + "\nSincronizados");
        tvErrorCount.setText("❌ " + error + "\nErrores");
    }

    // ── SINCRONIZACIÓN MANUAL ─────────────────────────────────────────────────

    private void startSync() {
        pendingOrders = db.getPendingOrders();

        if (pendingOrders.isEmpty()) {
            Toast.makeText(this, "✅ No hay pedidos pendientes de sincronización", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Tu sesión expiró. Vuelve a iniciar sesión.", Toast.LENGTH_LONG).show();
            logout();
            return;
        }

        // Mostrar confirmación
        new AlertDialog.Builder(this)
                .setTitle("Sincronizar pedidos")
                .setMessage("Se enviarán " + pendingOrders.size() + " pedido(s) al servidor.\n¿Continuar?")
                .setPositiveButton("Sincronizar", (d, w) -> {
                    syncIndex   = 0;
                    syncSuccess = 0;
                    syncErrors  = 0;
                    progressSync.setVisibility(View.VISIBLE);
                    syncNext();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /** Sincroniza los pedidos pendientes de forma secuencial (uno a uno). */
    private void syncNext() {
        if (syncIndex >= pendingOrders.size()) {
            // ── Fin de la sincronización ──────────────────────────────────
            progressSync.setVisibility(View.GONE);
            String msg = "Sincronización completada:\n✅ " + syncSuccess + " exitosos\n❌ " + syncErrors + " errores";
            new AlertDialog.Builder(this)
                    .setTitle("Resultado")
                    .setMessage(msg)
                    .setPositiveButton("OK", null)
                    .show();
            loadOrders(); // Recargar lista
            return;
        }

        Order order = pendingOrders.get(syncIndex);
        syncOrder(order, () -> {
            syncIndex++;
            syncNext(); // procesar el siguiente
        });
    }

    /** Envía un pedido al servidor. Llama a onDone cuando termina (éxito o error). */
    private void syncOrder(Order order, Runnable onDone) {
        String authHeader = sessionManager.getBearerToken();

        // Preparar campos de texto
        RequestBody clientName    = toReqBody(order.getClientName());
        RequestBody clientPhone   = toReqBody(order.getClientPhone() != null ? order.getClientPhone() : "");
        RequestBody clientAddress = toReqBody(order.getClientAddress() != null ? order.getClientAddress() : "");
        RequestBody orderDetail   = toReqBody(order.getOrderDetail());
        RequestBody paymentType   = toReqBody(order.getPaymentType() != null ? order.getPaymentType() : "");
        RequestBody latitude      = toReqBody(String.valueOf(order.getLatitude()));
        RequestBody longitude     = toReqBody(String.valueOf(order.getLongitude()));
        RequestBody localId       = toReqBody(String.valueOf(order.getId()));
        RequestBody createdAt     = toReqBody(order.getCreatedAt() != null ? order.getCreatedAt() : "");

        // Preparar foto (puede ser null si no se tomó)
        MultipartBody.Part photoPart = null;
        if (order.getPhotoPath() != null && !order.getPhotoPath().isEmpty()) {
            File photoFile = new File(order.getPhotoPath());
            if (photoFile.exists()) {
                RequestBody photoBody = RequestBody.create(
                        MediaType.parse("image/jpeg"), photoFile);
                photoPart = MultipartBody.Part.createFormData("photo", photoFile.getName(), photoBody);
            }
        }

        RetrofitClient.getService().createOrder(
                authHeader, clientName, clientPhone, clientAddress,
                orderDetail, paymentType, latitude, longitude,
                localId, createdAt, photoPart
        ).enqueue(new Callback<ApiService.OrderResponse>() {

            @Override
            public void onResponse(Call<ApiService.OrderResponse> call,
                                   Response<ApiService.OrderResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // ✅ Éxito: marcar como sincronizado
                    db.updateOrderStatus(order.getId(), Order.STATUS_SYNCED, null,
                            response.body().serverId);
                    syncSuccess++;
                } else {
                    // ❌ Error HTTP (400, 401, 500...)
                    String errMsg = "HTTP " + response.code();
                    if (response.code() == 401) {
                        errMsg = "Token inválido o expirado";
                    }
                    db.updateOrderStatus(order.getId(), Order.STATUS_ERROR, errMsg, null);
                    syncErrors++;
                }
                onDone.run();
            }

            @Override
            public void onFailure(Call<ApiService.OrderResponse> call, Throwable t) {
                // ❌ Error de red (sin conexión)
                String errMsg = "Sin conexión: " + t.getMessage();
                db.updateOrderStatus(order.getId(), Order.STATUS_ERROR, errMsg, null);
                syncErrors++;
                onDone.run();
            }
        });
    }

    private RequestBody toReqBody(String value) {
        return RequestBody.create(MediaType.parse("text/plain"), value);
    }

    // ── Menú opciones (logout) ────────────────────────────────────────────────
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Cerrar sesión").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            new AlertDialog.Builder(this)
                    .setTitle("Cerrar sesión")
                    .setMessage("¿Deseas cerrar sesión?")
                    .setPositiveButton("Sí", (d, w) -> logout())
                    .setNegativeButton("No", null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        sessionManager.clearSession();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}