package com.venegas.pedidos.activities;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.venegas.pedidos.R;
import com.venegas.pedidos.database.DatabaseHelper;
import com.venegas.pedidos.models.Order;

import java.io.File;
import java.util.Locale;

/**
 * OrderDetailActivity — muestra todos los datos de un pedido guardado.
 * Recibe el ID del pedido por Intent extra ("order_id").
 */
public class OrderDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getResources().getColor(R.color.primary, null));
        setupToolbar();

        long orderId = getIntent().getLongExtra("order_id", -1);
        if (orderId == -1) {
            finish();
            return;
        }

        Order order = DatabaseHelper.getInstance(this).getOrderById(orderId);
        if (order == null) {
            finish();
            return;
        }

        populateViews(order);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Detalle del Pedido");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void populateViews(Order order) {
        TextView tvStatusBadge  = findViewById(R.id.tvStatusBadge);
        TextView tvClientName   = findViewById(R.id.tvClientName);
        TextView tvClientPhone  = findViewById(R.id.tvClientPhone);
        TextView tvClientAddr   = findViewById(R.id.tvClientAddress);
        TextView tvOrderDetail  = findViewById(R.id.tvOrderDetail);
        TextView tvPaymentType  = findViewById(R.id.tvPaymentType);
        TextView tvDateTime     = findViewById(R.id.tvDateTime);
        TextView tvLatLng       = findViewById(R.id.tvLatLng);
        ImageView imgOrder      = findViewById(R.id.imgOrder);
        CardView  cardError     = findViewById(R.id.cardError);
        TextView  tvErrorMsg    = findViewById(R.id.tvErrorMessage);

        // ── Estado ────────────────────────────────────────────────────────
        switch (order.getStatus()) {
            case Order.STATUS_SYNCED:
                tvStatusBadge.setText("✅ Sincronizado");
                tvStatusBadge.setBackgroundColor(
                        getResources().getColor(R.color.status_synced, null));
                break;
            case Order.STATUS_ERROR:
                tvStatusBadge.setText("❌ Error de sincronización");
                tvStatusBadge.setBackgroundColor(
                        getResources().getColor(R.color.status_error, null));
                // Mostrar tarjeta de error
                cardError.setVisibility(View.VISIBLE);
                tvErrorMsg.setText(order.getErrorMessage());
                break;
            default:
                tvStatusBadge.setText("⏳ Pendiente de sincronización");
                tvStatusBadge.setBackgroundColor(
                        getResources().getColor(R.color.status_pending, null));
                break;
        }

        // ── Datos del cliente ─────────────────────────────────────────────
        tvClientName.setText(order.getClientName());
        tvClientPhone.setText("📞 " + (order.getClientPhone() != null ? order.getClientPhone() : "Sin teléfono"));
        tvClientAddr.setText("📍 " + (order.getClientAddress() != null ? order.getClientAddress() : "Sin dirección"));

        // ── Datos del pedido ──────────────────────────────────────────────
        tvOrderDetail.setText(order.getOrderDetail());

        String payment = "transferencia".equals(order.getPaymentType())
                ? "💳 Transferencia" : "💵 Efectivo";
        tvPaymentType.setText("Pago: " + payment);

        tvDateTime.setText("🕒 " + (order.getCreatedAt() != null
                ? order.getCreatedAt() : "Sin fecha"));

        // ── GPS ───────────────────────────────────────────────────────────
        if (order.getLatitude() != 0.0 || order.getLongitude() != 0.0) {
            tvLatLng.setText(String.format(Locale.getDefault(),
                    "Latitud:  %.6f\nLongitud: %.6f",
                    order.getLatitude(), order.getLongitude()));
        } else {
            tvLatLng.setText("Sin datos de ubicación");
        }

        // ── Fotografía ────────────────────────────────────────────────────
        if (order.getPhotoPath() != null && !order.getPhotoPath().isEmpty()) {
            File photoFile = new File(order.getPhotoPath());
            if (photoFile.exists()) {
                Glide.with(this)
                        .load(photoFile)
                        .placeholder(R.drawable.ic_photo_placeholder)
                        .error(R.drawable.ic_photo_placeholder)
                        .centerCrop()
                        .into(imgOrder);
            } else {
                imgOrder.setImageResource(R.drawable.ic_photo_placeholder);
            }
        } else {
            imgOrder.setImageResource(R.drawable.ic_photo_placeholder);
        }

        // ID del servidor si ya fue sincronizado
        if (order.getServerId() != null) {
            tvDateTime.append("\nID Servidor: " + order.getServerId());
        }
    }
}