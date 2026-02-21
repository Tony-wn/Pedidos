package com.venegas.pedidos.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * QRParser — parsea el contenido de un código QR de cliente.
 *
 * Formato esperado:
 *   CLIENTE=Juan Perez|TEL=0999999999|DIR=Av. Central y Loja
 *
 * Las claves reconocidas son:
 *   CLIENTE  → nombre del cliente
 *   TEL      → teléfono
 *   DIR      → dirección o referencia
 */
public class QRParser {

    public static class QRData {
        public String clientName;
        public String clientPhone;
        public String clientAddress;
        public boolean isValid;
        public String errorMessage;
    }

    /**
     * Parsea el contenido bruto del QR y devuelve un objeto QRData.
     *
     * @param rawContent contenido escaneado del QR
     * @return QRData con los campos extraídos y el flag isValid
     */
    public static QRData parse(String rawContent) {
        QRData data = new QRData();

        if (rawContent == null || rawContent.trim().isEmpty()) {
            data.isValid = false;
            data.errorMessage = "El QR está vacío";
            return data;
        }

        // Separar por "|" para obtener pares clave=valor
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

        // Extraer los campos conocidos
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

    /**
     * Valida si el contenido parece ser un QR de cliente válido
     * (comprobación rápida antes de hacer el parse completo).
     */
    public static boolean looksLikeClientQR(String rawContent) {
        return rawContent != null && rawContent.contains("CLIENTE=");
    }
}