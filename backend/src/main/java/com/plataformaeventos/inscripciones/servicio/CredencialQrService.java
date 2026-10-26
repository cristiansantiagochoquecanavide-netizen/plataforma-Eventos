package com.plataformaeventos.inscripciones.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.inscripciones.dto.CredencialDigitalRespuesta;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CredencialQrService {
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;
    private final ObjectMapper mapper;
    private final byte[] clave;
    private final long expiracionHoras;

    public CredencialQrService(InscripcionRepository inscripciones, UsuarioRepository usuarios, ObjectMapper mapper,
                               @Value("${seguridad.qr.clave}") String clave,
                               @Value("${seguridad.qr.expiracion-horas:24}") long expiracionHoras) {
        if (clave == null || clave.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("QR_HMAC_SECRET debe tener al menos 32 bytes");
        }
        this.inscripciones = inscripciones;
        this.usuarios = usuarios;
        this.mapper = mapper;
        this.clave = clave.getBytes(StandardCharsets.UTF_8);
        this.expiracionHoras = Math.max(1, expiracionHoras);
    }

    @Transactional(readOnly = true)
    public CredencialDigitalRespuesta emitir(Long idInscripcion, String correo, Organizacion organizacion) {
        Usuario participante = usuarios.findByCorreo(correo.trim().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Usuario autenticado no encontrado"));
        Inscripcion inscripcion = inscripciones.findCredentialByIdAndParticipant(idInscripcion, participante.getId(),
                        organizacion == null ? null : organizacion.getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Inscripción no encontrada o no pertenece al usuario autenticado"));
        if (inscripcion.getEstado() != EstadoInscripcion.CONFIRMADA) {
            throw new IllegalStateException("Solo las inscripciones confirmadas pueden emitir una credencial QR");
        }

        Instant expira = Instant.now().plusSeconds(expiracionHoras * 3600);
        Long tenantId = inscripcion.getOrganizacion() != null ? inscripcion.getOrganizacion().getId()
                : inscripcion.getEvento().getOrganizacion() == null ? null : inscripcion.getEvento().getOrganizacion().getId();
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", inscripcion.getId());
        claims.put("evt", inscripcion.getEvento().getId());
        claims.put("org", tenantId);
        claims.put("exp", expira.getEpochSecond());
        claims.put("jti", UUID.randomUUID().toString());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(toJson(claims));
        String token = payload + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(firmar(payload));

        var evento = inscripcion.getEvento();
        String lugar = evento.getLugarPrincipal();
        if ((lugar == null || lugar.isBlank()) && evento.getEspacio() != null) {
            lugar = evento.getEspacio().getNombre();
        }
        return new CredencialDigitalRespuesta(inscripcion.getId(),
                participante.getNombres() + " " + participante.getApellidos(), evento.getNombre(),
                evento.getCategoria() == null ? "GENERAL" : evento.getCategoria().getNombre(), evento.getFechaInicio(),
                lugar == null || lugar.isBlank() ? "Sin lugar asignado" : lugar, token, generarQr(token),
                OffsetDateTime.ofInstant(expira, ZoneOffset.UTC));
    }

    private byte[] toJson(Map<String, Object> claims) {
        try { return mapper.writeValueAsBytes(claims); }
        catch (Exception e) { throw new IllegalStateException("No se pudo construir la credencial", e); }
    }

    private byte[] firmar(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) { throw new IllegalStateException("No se pudo firmar la credencial", e); }
    }

    private String generarQr(String token) {
        try {
            var hints = Map.of(EncodeHintType.MARGIN, 2, EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M);
            BitMatrix matrix = new MultiFormatWriter().encode(token, BarcodeFormat.QR_CODE, 360, 360, hints);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) { throw new IllegalStateException("No se pudo generar el código QR", e); }
    }
}
