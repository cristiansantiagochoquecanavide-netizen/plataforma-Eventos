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
                .orElseThrow(() -> new IllegalArgumentException("El token de recuperacion es invalido o vencio"));
        if (!tokenRecuperacion.estaDisponible()) {
            throw new IllegalArgumentException("El token de recuperacion es invalido o vencio");
        }
        tokenRecuperacion.marcarComoUtilizado();
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
        mensaje.setSubject("Recuperacion de clave");
        mensaje.setText("Recibimos una solicitud para restablecer tu clave. Usa este enlace dentro de "
                + propiedades.expiracionMinutos() + " minutos:\n\n" + enlace
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
