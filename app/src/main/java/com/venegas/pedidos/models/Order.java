package com.venegas.pedidos.models;

/**
 * Modelo de datos de un Pedido.
 * Representa tanto la entidad local (SQLite) como el objeto que se envía a la API.
 */
public class Order {

    // ── Constantes de estado ─────────────────────────────────────────────────
    public static final String STATUS_PENDING  = "PENDING";    // Pendiente de sincronización
    public static final String STATUS_SYNCED   = "SYNCED";     // Sincronizado con el servidor
    public static final String STATUS_ERROR    = "ERROR";      // Error al sincronizar

    // ── Campos de la entidad ─────────────────────────────────────────────────
    private long   id;            // ID local SQLite (autoincrement)
    private String clientName;    // Nombre del cliente
    private String clientPhone;   // Teléfono
    private String clientAddress; // Dirección o referencia
    private String orderDetail;   // Detalle del pedido
    private String paymentType;   // "efectivo" o "transferencia"
    private String photoPath;     // Ruta local de la fotografía
    private double latitude;      // Latitud GPS
    private double longitude;     // Longitud GPS
    private String status;        // STATUS_PENDING / SYNCED / ERROR
    private String errorMessage;  // Mensaje de error si falló la sync
    private String createdAt;     // Fecha/hora de creación (ISO 8601)
    private String serverId;      // ID asignado por el servidor (null si no sincronizado)

    // ── Constructor vacío ────────────────────────────────────────────────────
    public Order() {
        this.status = STATUS_PENDING;
    }

    // ── Constructor completo ─────────────────────────────────────────────────
    public Order(String clientName, String clientPhone, String clientAddress,
                 String orderDetail, String paymentType, String photoPath,
                 double latitude, double longitude, String createdAt) {
        this.clientName    = clientName;
        this.clientPhone   = clientPhone;
        this.clientAddress = clientAddress;
        this.orderDetail   = orderDetail;
        this.paymentType   = paymentType;
        this.photoPath     = photoPath;
        this.latitude      = latitude;
        this.longitude     = longitude;
        this.createdAt     = createdAt;
        this.status        = STATUS_PENDING;
    }

    // ── Getters y Setters ────────────────────────────────────────────────────
    public long getId()                     { return id; }
    public void setId(long id)              { this.id = id; }

    public String getClientName()           { return clientName; }
    public void setClientName(String v)     { clientName = v; }

    public String getClientPhone()          { return clientPhone; }
    public void setClientPhone(String v)    { clientPhone = v; }

    public String getClientAddress()        { return clientAddress; }
    public void setClientAddress(String v)  { clientAddress = v; }

    public String getOrderDetail()          { return orderDetail; }
    public void setOrderDetail(String v)    { orderDetail = v; }

    public String getPaymentType()          { return paymentType; }
    public void setPaymentType(String v)    { paymentType = v; }

    public String getPhotoPath()            { return photoPath; }
    public void setPhotoPath(String v)      { photoPath = v; }

    public double getLatitude()             { return latitude; }
    public void setLatitude(double v)       { latitude = v; }

    public double getLongitude()            { return longitude; }
    public void setLongitude(double v)      { longitude = v; }

    public String getStatus()               { return status; }
    public void setStatus(String v)         { status = v; }

    public String getErrorMessage()         { return errorMessage; }
    public void setErrorMessage(String v)   { errorMessage = v; }

    public String getCreatedAt()            { return createdAt; }
    public void setCreatedAt(String v)      { createdAt = v; }

    public String getServerId()             { return serverId; }
    public void setServerId(String v)       { serverId = v; }

    // ── Helpers ──────────────────────────────────────────────────────────────
    public boolean isPending()  { return STATUS_PENDING.equals(status); }
    public boolean isSynced()   { return STATUS_SYNCED.equals(status); }
    public boolean isError()    { return STATUS_ERROR.equals(status); }

    public String getStatusLabel() {
        switch (status) {
            case STATUS_SYNCED:  return "Sincronizado";
            case STATUS_ERROR:   return "Error";
            default:             return "Pendiente";
        }
    }
}