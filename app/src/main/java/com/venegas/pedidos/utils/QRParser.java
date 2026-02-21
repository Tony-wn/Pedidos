package com.venegas.pedidos.utils;

import java.util.HashMap;
import java.util.Map;
public class QRParser {

    public static class QRData {
        public String clientName;
        public String clientPhone;
        public String clientAddress;
        public boolean isValid;
        public String errorMessage;
    }
    public static QRData parse(String rawContent) {
        QRData data = new QRData();

        if (rawContent == null || rawContent.trim().isEmpty()) {
            data.isValid = false;
            data.errorMessage = "El QR está vacío";
            return data;
        }

        String[] parts = rawContent.split("\\|");
        Map<String, String> map = new HashMap<>();

        for (String part : parts) {
            int eqIndex = part.indexOf('=');
            if (eqIndex > 0) {
                String key   = part.substring(0, eqIndex).trim().toUpperCase();
                String value = part.substring(eqIndex + 1).trim();
                map.put(key, value);
            }
        }

        data.clientName    = map.getOrDefault("CLIENTE", "");
        data.clientPhone   = map.getOrDefault("TEL", "");
        data.clientAddress = map.getOrDefault("DIR", "");

        // Validación mínima: al menos el nombre debe estar presente
        if (data.clientName.isEmpty()) {
            data.isValid = false;
            data.errorMessage = "El QR no contiene el campo CLIENTE.\n"
                    + "Formato esperado: CLIENTE=Nombre|TEL=0999|DIR=Dirección";
        } else {
            data.isValid = true;
        }

        return data;
    }

    public static boolean looksLikeClientQR(String rawContent) {
        return rawContent != null && rawContent.contains("CLIENTE=");
    }
}