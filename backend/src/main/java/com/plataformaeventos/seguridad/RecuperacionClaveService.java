package com.plataformaeventos.seguridad;

import com.plataformaeventos.usuarios.modelo.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecuperacionClaveService {

    private static final SecureRandom GENERADOR_SEGURO = new SecureRandom();
    private static final String MENSAJE_SOLICITUD_EXPIRADA = "La solicitud expir\u00f3.";
    private static final String MENSAJE_ENLACE_UTILIZADO = "El enlace ya fue utilizado.";

    private final TokenRecuperacionRepository tokenRecuperacionRepository;
    private final JavaMailSender correoSender;
    private final RecuperacionClavePropiedades propiedades;
    private final String correoRemitenteBrevo;
    private final String nombreRemitenteBrevo;
    private final RestClient clienteBrevo;
    private final Map<Long, OffsetDateTime> ultimaSolicitudPorUsuario = new HashMap<>();

    public RecuperacionClaveService(
            TokenRecuperacionRepository tokenRecuperacionRepository,
            JavaMailSender correoSender,
            RecuperacionClavePropiedades propiedades
    ) {
        this(tokenRecuperacionRepository, correoSender, propiedades, null, null, "Plataforma de eventos", RestClient.builder());
    }

    @Autowired
    public RecuperacionClaveService(
            TokenRecuperacionRepository tokenRecuperacionRepository,
            JavaMailSender correoSender,
            RecuperacionClavePropiedades propiedades,
            @Value("${brevo.api-key:}") String claveApiBrevo,
            @Value("${brevo.from-email:}") String correoRemitenteBrevo,
            @Value("${brevo.from-name:Plataforma de eventos}") String nombreRemitenteBrevo,
            RestClient.Builder restClientBuilder
    ) {
        this.tokenRecuperacionRepository = tokenRecuperacionRepository;
        this.correoSender = correoSender;
        this.propiedades = propiedades;
        this.correoRemitenteBrevo = correoRemitenteBrevo;
        this.nombreRemitenteBrevo = nombreRemitenteBrevo;
        this.clienteBrevo = StringUtils.hasText(claveApiBrevo)
                ? restClientBuilder.clone()
                        .baseUrl("https://api.brevo.com/v3")
                        .defaultHeader("api-key", claveApiBrevo)
                        .build()
                : null;
    }

    @Transactional
    public synchronized void crearYEnviarToken(Usuario usuario) {
        OffsetDateTime ahora = OffsetDateTime.now();
        ultimaSolicitudPorUsuario.entrySet().removeIf(entrada -> entrada.getValue()
                .plusSeconds(propiedades.intervaloSolicitudSegundos())
                .isBefore(ahora));
        OffsetDateTime ultimaSolicitud = ultimaSolicitudPorUsuario.get(usuario.getId());
        if (ultimaSolicitud != null && ultimaSolicitud.plusSeconds(propiedades.intervaloSolicitudSegundos()).isAfter(ahora)) {
            return;
        }
        tokenRecuperacionRepository.invalidarTokensPendientesPorUsuario(usuario.getId(), ahora);
        String token = generarToken();
        tokenRecuperacionRepository.saveAndFlush(new TokenRecuperacion(
                usuario,
                calcularHash(token),
                ahora.plusMinutes(propiedades.expiracionMinutos())
        ));
        enviarCorreo(usuario, token);
        ultimaSolicitudPorUsuario.put(usuario.getId(), ahora);
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
        String enlace = UriComponentsBuilder.fromUriString(urlFrontendNormalizada())
                .path("/restablecer-clave")
                .queryParam("token", token)
                .toUriString();
        String asunto = "Recuperacion de acceso - Plataforma de Gestion de Eventos";
        String texto = "Plataforma de Gestion de Eventos\n\n"
                + "Recibimos una solicitud para restablecer tu clave. Usa este enlace dentro de "
                + propiedades.expiracionMinutos() + " minutos:\n\n" + enlace
                + "\n\nSi no solicitaste este cambio, puedes ignorar este correo.";

        if (clienteBrevo != null) {
            enviarConBrevo(usuario.getCorreo(), asunto, texto);
            return;
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(propiedades.correoRemitente());
        mensaje.setTo(usuario.getCorreo());
        mensaje.setSubject(asunto);
        mensaje.setText(texto);
        correoSender.send(mensaje);
    }

    private void enviarConBrevo(String destinatario, String asunto, String texto) {
        if (!StringUtils.hasText(correoRemitenteBrevo)) {
            throw new IllegalStateException("Configura BREVO_FROM_EMAIL con un remitente verificado en Brevo");
        }
        try {
            clienteBrevo.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CorreoBrevo(
                            new RemitenteBrevo(correoRemitenteBrevo, nombreRemitenteBrevo),
                            List.of(new DestinatarioBrevo(destinatario)), asunto, texto))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException exception) {
            throw new IllegalStateException(
                    "Brevo rechazó el correo transaccional (HTTP " + exception.getStatusCode().value() + ")", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo conectar con la API de correo Brevo", exception);
        }
    }

    private record CorreoBrevo(RemitenteBrevo sender, List<DestinatarioBrevo> to,
                               String subject, String textContent) { }
    private record RemitenteBrevo(String email, String name) { }
    private record DestinatarioBrevo(String email) { }

    private String urlFrontendNormalizada() {
        String url = propiedades.urlFrontendWeb().trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
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
