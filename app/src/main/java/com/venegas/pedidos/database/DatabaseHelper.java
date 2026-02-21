package com.venegas.pedidos.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.venegas.pedidos.models.Order;

import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseHelper — gestiona la base de datos SQLite local.
 * Tabla: orders  — almacena todos los pedidos con su estado de sincronización.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // ── Versión y nombre de la DB ────────────────────────────────────────────
    private static final String DB_NAME    = "pedidos.db";
    private static final int    DB_VERSION = 1;

    // ── Tabla y columnas ─────────────────────────────────────────────────────
    public static final String TABLE_ORDERS       = "orders";
    public static final String COL_ID             = "id";
    public static final String COL_CLIENT_NAME    = "client_name";
    public static final String COL_CLIENT_PHONE   = "client_phone";
    public static final String COL_CLIENT_ADDRESS = "client_address";
    public static final String COL_ORDER_DETAIL   = "order_detail";
    public static final String COL_PAYMENT_TYPE   = "payment_type";
    public static final String COL_PHOTO_PATH     = "photo_path";
    public static final String COL_LATITUDE       = "latitude";
    public static final String COL_LONGITUDE      = "longitude";
    public static final String COL_STATUS         = "status";
    public static final String COL_ERROR_MSG      = "error_message";
    public static final String COL_CREATED_AT     = "created_at";
    public static final String COL_SERVER_ID      = "server_id";

    // ── Singleton ────────────────────────────────────────────────────────────
    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context ctx) {
        if (instance == null) {
            instance = new DatabaseHelper(ctx.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // ── Creación de tablas ───────────────────────────────────────────────────
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createOrders = "CREATE TABLE " + TABLE_ORDERS + " ("
                + COL_ID             + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_CLIENT_NAME    + " TEXT NOT NULL, "
                + COL_CLIENT_PHONE   + " TEXT, "
                + COL_CLIENT_ADDRESS + " TEXT, "
                + COL_ORDER_DETAIL   + " TEXT NOT NULL, "
                + COL_PAYMENT_TYPE   + " TEXT, "
                + COL_PHOTO_PATH     + " TEXT, "
                + COL_LATITUDE       + " REAL, "
                + COL_LONGITUDE      + " REAL, "
                + COL_STATUS         + " TEXT DEFAULT 'PENDING', "
                + COL_ERROR_MSG      + " TEXT, "
                + COL_CREATED_AT     + " TEXT, "
                + COL_SERVER_ID      + " TEXT"
                + ");";
        db.execSQL(createOrders);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDERS);
        onCreate(db);
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    /**
     * Inserta un nuevo pedido en SQLite.
     * @return ID generado o -1 si hubo error.
     */
    public long insertOrder(Order order) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = orderToContentValues(order);
        long id = db.insert(TABLE_ORDERS, null, cv);
        db.close();
        return id;
    }

    /**
     * Actualiza el estado de sincronización de un pedido.
     */
    public void updateOrderStatus(long id, String status, String errorMessage, String serverId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_STATUS,    status);
        cv.put(COL_ERROR_MSG, errorMessage);
        cv.put(COL_SERVER_ID, serverId);
        db.update(TABLE_ORDERS, cv, COL_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    /**
     * Devuelve todos los pedidos ordenados por fecha descendente.
     */
    public List<Order> getAllOrders() {
        List<Order> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_ORDERS, null, null, null,
                null, null, COL_ID + " DESC");
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToOrder(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * Devuelve solo los pedidos PENDING (para sincronizar).
     */
    public List<Order> getPendingOrders() {
        List<Order> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_ORDERS, null,
                COL_STATUS + "=?", new String[]{Order.STATUS_PENDING},
                null, null, COL_ID + " ASC");
        if (cursor.moveToFirst()) {
            do {
                list.add(cursorToOrder(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * Devuelve un pedido por su ID local.
     */
    public Order getOrderById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_ORDERS, null,
                COL_ID + "=?", new String[]{String.valueOf(id)},
                null, null, null);
        Order order = null;
        if (cursor.moveToFirst()) {
            order = cursorToOrder(cursor);
        }
        cursor.close();
        db.close();
        return order;
    }

    /**
     * Cuenta pedidos por estado.
     */
    public int countByStatus(String status) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_ORDERS + " WHERE " + COL_STATUS + "=?",
                new String[]{status});
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        db.close();
        return count;
    }

    // ── Conversores privados ─────────────────────────────────────────────────

    private ContentValues orderToContentValues(Order o) {
        ContentValues cv = new ContentValues();
        cv.put(COL_CLIENT_NAME,    o.getClientName());
        cv.put(COL_CLIENT_PHONE,   o.getClientPhone());
        cv.put(COL_CLIENT_ADDRESS, o.getClientAddress());
        cv.put(COL_ORDER_DETAIL,   o.getOrderDetail());
        cv.put(COL_PAYMENT_TYPE,   o.getPaymentType());
        cv.put(COL_PHOTO_PATH,     o.getPhotoPath());
        cv.put(COL_LATITUDE,       o.getLatitude());
        cv.put(COL_LONGITUDE,      o.getLongitude());
        cv.put(COL_STATUS,         o.getStatus());
        cv.put(COL_ERROR_MSG,      o.getErrorMessage());
        cv.put(COL_CREATED_AT,     o.getCreatedAt());
        cv.put(COL_SERVER_ID,      o.getServerId());
        return cv;
    }

    private Order cursorToOrder(Cursor c) {
        Order o = new Order();
        o.setId(           c.getLong(  c.getColumnIndexOrThrow(COL_ID)));
        o.setClientName(   c.getString(c.getColumnIndexOrThrow(COL_CLIENT_NAME)));
        o.setClientPhone(  c.getString(c.getColumnIndexOrThrow(COL_CLIENT_PHONE)));
        o.setClientAddress(c.getString(c.getColumnIndexOrThrow(COL_CLIENT_ADDRESS)));
        o.setOrderDetail(  c.getString(c.getColumnIndexOrThrow(COL_ORDER_DETAIL)));
        o.setPaymentType(  c.getString(c.getColumnIndexOrThrow(COL_PAYMENT_TYPE)));
        o.setPhotoPath(    c.getString(c.getColumnIndexOrThrow(COL_PHOTO_PATH)));
        o.setLatitude(     c.getDouble(c.getColumnIndexOrThrow(COL_LATITUDE)));
        o.setLongitude(    c.getDouble(c.getColumnIndexOrThrow(COL_LONGITUDE)));
        o.setStatus(       c.getString(c.getColumnIndexOrThrow(COL_STATUS)));
        o.setErrorMessage( c.getString(c.getColumnIndexOrThrow(COL_ERROR_MSG)));
        o.setCreatedAt(    c.getString(c.getColumnIndexOrThrow(COL_CREATED_AT)));
        o.setServerId(     c.getString(c.getColumnIndexOrThrow(COL_SERVER_ID)));
        return o;
    }
}