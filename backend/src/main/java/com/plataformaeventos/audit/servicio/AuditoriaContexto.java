package com.plataformaeventos.audit.servicio;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;

public final class AuditoriaContexto {
    private AuditoriaContexto() { }

    /** Only syntactically valid addresses are accepted; the first valid forwarded hop is used. */
    public static String ip(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null) {
            for (String candidate : forwarded.split(",")) {
                String value = candidate.trim();
                if (esIp(value)) return value;
            }
        }
        String remote = request.getRemoteAddr();
        return esIp(remote) ? remote : null;
    }

    public static String ipActual() {
        var atributos = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        return atributos instanceof org.springframework.web.context.request.ServletRequestAttributes servlet
                ? ip(servlet.getRequest()) : null;
    }

    private static boolean esIp(String value) {
        if (value == null || value.isBlank() || value.length() > 45) return false;
        if (!value.matches("[0-9a-fA-F:.]+")) return false;
        try { InetAddress.getByName(value); return !value.contains("/"); }
        catch (Exception ex) { return false; }
    }
}
