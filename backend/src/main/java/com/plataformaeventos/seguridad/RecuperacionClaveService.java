package com.plataformaeventos.seguridad;

import com.plataformaeventos.usuarios.Usuario;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
public class RecuperacionClaveService {

    private static final SecureRandom GENERADOR_SEGURO = new SecureRandom();
    private static final String MENSAJE_SOLICITUD_EXPIRADA = "La solicitud expir\u00f3.";
    private static final String MENSAJE_ENLACE_UTILIZADO = "El enlace ya fue utilizado.";

    private final TokenRecuperacionRepository tokenRecuperacionRepository;
    private final JavaMailSender correoSender;
    private final RecuperacionClavePropiedades propiedades;

    public RecuperacionClaveService(
            TokenRecuperacionRepository tokenRecuperacionRepository,
            JavaMailSender correoSender,
            RecuperacionClavePropiedades propiedades
    ) {
        this.tokenRecuperacionRepository = tokenRecuperacionRepository;
        this.correoSender = correoSender;
        this.propiedades = propiedades;
    }

    @Transactional
    public void crearYEnviarToken(Usuario usuario) {
        tokenRecuperacionRepository.invalidarTokensPendientesPorUsuario(usuario.getId());
        String token = generarToken();
        tokenRecuperacionRepository.save(new TokenRecuperacion(
                usuario,
                calcularHash(token),
                OffsetDateTime.now().plusMinutes(propiedades.expiracionMinutos())
        ));
        enviarCorreo(usuario, token);
    }

    @Transactional
    public Usuario consumirToken(String token) {
        TokenRecuperacion tokenRecuperacion = tokenRecuperacionRepository.findByTokenHashForUpdate(calcularHash(token))
                .orElseThrow(() -> new IllegalArgumentException(MENSAJE_SOLICITUD_EXPIRADA));

        OffsetDateTime ahora = OffsetDateTime.now();
        if (tokenRecuperacion.fueUtilizado() || !tokenRecuperacion.estaActivo()) {
            throw new IllegalArgumentException(MENSAJE_ENLACE_UTILIZADO);
        }
        if (tokenRecuperacion.estaExpirado(ahora)) {
            tokenRecuperacion.invalidar(ahora);
            throw new IllegalArgumentException(MENSAJE_SOLICITUD_EXPIRADA);
        }
        tokenRecuperacion.marcarComoUtilizado(ahora);
        return tokenRecuperacion.getUsuario();
    }

    private String generarToken() {
        byte[] bytes = new byte[48];
        GENERADOR_SEGURO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void enviarCorreo(Usuario usuario, String token) {
        String enlace = UriComponentsBuilder.fromUriString(propiedades.urlFrontendWeb())
                .path("/restablecer-clave")
                .queryParam("token", token)
                .toUriString();
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(propiedades.correoRemitente());
        mensaje.setTo(usuario.getCorreo());
        mensaje.setSubject("Recuperacion de acceso - Plataforma de Gestion de Eventos");
        mensaje.setText("Plataforma de Gestion de Eventos\n\n"
                + "Recibimos una solicitud para restablecer tu clave. Usa este enlace dentro de "
                + propiedades.expiracionMinutos() + " minutos:\n\n" + enlace
                + "\n\nCodigo de recuperacion:\n" + token
                + "\n\nSi no solicitaste este cambio, puedes ignorar este correo.");
        correoSender.send(mensaje);
    }

    private String calcularHash(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException excepcion) {
            throw new IllegalStateException("SHA-256 no esta disponible", excepcion);
        }
    }
}
