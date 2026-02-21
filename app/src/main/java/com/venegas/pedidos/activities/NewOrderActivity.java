package com.venegas.pedidos.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import com.venegas.pedidos.R;
import com.venegas.pedidos.database.DatabaseHelper;
import com.venegas.pedidos.models.Order;
import com.venegas.pedidos.utils.QRParser;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * NewOrderActivity — formulario para crear un nuevo pedido.
 * Integra: GPS automático, cámara, lectura de QR y guardado en SQLite.
 */
public class NewOrderActivity extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────
    private TextInputEditText etClientName;
    private TextInputEditText etClientPhone;
    private TextInputEditText etClientAddress;
    private TextInputEditText etOrderDetail;
    private RadioGroup        radioGroupPayment;
    private TextView          tvGPSCoords;
    private TextView          tvPhotoPath;
    private ImageView         imgPreview;

    // ── Estado ────────────────────────────────────────────────────────────────
    private double  currentLatitude  = 0.0;
    private double  currentLongitude = 0.0;
    private String  currentPhotoPath = null;
    private Uri     photoUri         = null;

    private FusedLocationProviderClient locationClient;
    private DatabaseHelper db;

    // ── Permisos solicitados ──────────────────────────────────────────────────
    private static final int REQ_LOCATION = 100;
    private static final int REQ_CAMERA   = 101;

    // ── Launchers de actividades ──────────────────────────────────────────────

    // Resultado de la cámara
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && currentPhotoPath != null) {
                    imgPreview.setVisibility(View.VISIBLE);
                    Glide.with(this).load(currentPhotoPath).into(imgPreview);
                    tvPhotoPath.setText("✅ Foto guardada");
                } else {
                    showToast("No se tomó la foto");
                }
            });

    // Resultado del escáner QR (ZXing)
    private final ActivityResultLauncher<ScanOptions> qrLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) {
                    processQRContent(result.getContents());
                }
            });

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_order);
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getResources().getColor(R.color.primary, null));
        db             = DatabaseHelper.getInstance(this);
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        setupToolbar();
        bindViews();
        setupButtons();
        requestLocationAndUpdate();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Nuevo Pedido");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void bindViews() {
        etClientName    = findViewById(R.id.etClientName);
        etClientPhone   = findViewById(R.id.etClientPhone);
        etClientAddress = findViewById(R.id.etClientAddress);
        etOrderDetail   = findViewById(R.id.etOrderDetail);
        radioGroupPayment = findViewById(R.id.radioGroupPayment);
        tvGPSCoords     = findViewById(R.id.tvGPSCoords);
        tvPhotoPath     = findViewById(R.id.tvPhotoPath);
        imgPreview      = findViewById(R.id.imgPreview);
    }

    private void setupButtons() {
        MaterialButton btnReadQR   = findViewById(R.id.btnReadQR);
        MaterialButton btnTakePhoto = findViewById(R.id.btnTakePhoto);
        MaterialButton btnRefreshGPS = findViewById(R.id.btnRefreshGPS);
        MaterialButton btnSaveOrder  = findViewById(R.id.btnSaveOrder);

        btnReadQR.setOnClickListener(v -> launchQRScanner());
        btnTakePhoto.setOnClickListener(v -> launchCamera());
        btnRefreshGPS.setOnClickListener(v -> requestLocationAndUpdate());
        btnSaveOrder.setOnClickListener(v -> saveOrder());
    }

    // ── GPS ───────────────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private void requestLocationAndUpdate() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_LOCATION);
            return;
        }

        tvGPSCoords.setText("📍 Obteniendo ubicación...");

        locationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLatitude  = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        tvGPSCoords.setText(String.format(Locale.getDefault(),
                                "📍 Lat: %.6f\nLng: %.6f", currentLatitude, currentLongitude));
                    } else {
                        tvGPSCoords.setText("⚠️ No se pudo obtener ubicación");
                    }
                })
                .addOnFailureListener(e ->
                        tvGPSCoords.setText("⚠️ Error GPS: " + e.getMessage()));
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    // ── CÁMARA ────────────────────────────────────────────────────────────────

    private void launchCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQ_CAMERA);
            return;
        }

        try {
            File photoFile = createImageFile();
            currentPhotoPath = photoFile.getAbsolutePath();
            photoUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(photoUri);
        } catch (IOException e) {
            showToast("Error al crear archivo de imagen: " + e.getMessage());
        }
    }

    /** Crea un archivo vacío en almacenamiento externo para guardar la foto. */
    private File createImageFile() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName  = "PEDIDO_" + timestamp;
        File storageDir  = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    // ── ESCÁNER QR ────────────────────────────────────────────────────────────

    private void launchQRScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("Apunta al código QR del cliente");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setBarcodeImageEnabled(false);
        qrLauncher.launch(options);
    }

    /** Procesa el contenido del QR escaneado y autocompleta los campos. */
    private void processQRContent(String rawContent) {
        if (!QRParser.looksLikeClientQR(rawContent)) {
            // QR no reconocido: mostrar lo que vino y preguntar
            showToast("QR no reconocido como cliente.\nContenido: " + rawContent);
            return;
        }

        QRParser.QRData data = QRParser.parse(rawContent);

        if (!data.isValid) {
            showToast("QR inválido: " + data.errorMessage);
            return;
        }

        // Autocompletar campos del cliente
        if (!data.clientName.isEmpty())    etClientName.setText(data.clientName);
        if (!data.clientPhone.isEmpty())   etClientPhone.setText(data.clientPhone);
        if (!data.clientAddress.isEmpty()) etClientAddress.setText(data.clientAddress);

        showToast("✅ Datos del cliente cargados desde QR");
    }

    // ── GUARDAR PEDIDO ────────────────────────────────────────────────────────

    private void saveOrder() {
        String clientName    = getText(etClientName);
        String clientPhone   = getText(etClientPhone);
        String clientAddress = getText(etClientAddress);
        String orderDetail   = getText(etOrderDetail);

        // ── Validaciones ──────────────────────────────────────────────────
        if (clientName.isEmpty()) {
            etClientName.setError("El nombre del cliente es obligatorio");
            etClientName.requestFocus();
            return;
        }
        if (orderDetail.isEmpty()) {
            etOrderDetail.setError("El detalle del pedido es obligatorio");
            etOrderDetail.requestFocus();
            return;
        }
        if (currentPhotoPath == null) {
            showToast("Debes tomar una fotografía antes de guardar");
            return;
        }

        // Tipo de pago según RadioButton seleccionado
        int selectedId   = radioGroupPayment.getCheckedRadioButtonId();
        String paymentType = (selectedId == R.id.rboTransfer) ? "transferencia" : "efectivo";

        // Fecha y hora actual
        String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        // Crear objeto Order y guardar en SQLite
        Order order = new Order(clientName, clientPhone, clientAddress,
                orderDetail, paymentType, currentPhotoPath,
                currentLatitude, currentLongitude, createdAt);

        long newId = db.insertOrder(order);

        if (newId != -1) {
            showToast("✅ Pedido guardado (ID: " + newId + ")");
            finish(); // volver a la lista
        } else {
            showToast("❌ Error al guardar el pedido. Intenta de nuevo.");
        }
    }

    // ── Permisos ──────────────────────────────────────────────────────────────
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationAndUpdate();
            } else {
                tvGPSCoords.setText("⚠️ Permiso de ubicación denegado");
            }
        } else if (requestCode == REQ_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchCamera();
            } else {
                showToast("Permiso de cámara denegado");
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}