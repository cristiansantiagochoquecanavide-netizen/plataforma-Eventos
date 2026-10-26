package com.plataformaeventos.notificaciones.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
public class CorreoTransaccionalService {
    private final JavaMailSender mailSender;
    private final String smtpFrom;
    private final String brevoFrom;
    private final String senderName;
    private final RestClient brevo;

    public CorreoTransaccionalService(
            JavaMailSender mailSender,
            RestClient.Builder restClientBuilder,
            @Value("${spring.mail.properties.mail.smtp.from:${SMTP_FROM:no-reply@eventos.local}}") String smtpFrom,
            @Value("${brevo.api-key:}") String brevoApiKey,
            @Value("${brevo.from-email:}") String brevoFrom,
            @Value("${brevo.from-name:Plataforma de eventos}") String senderName
    ) {
        this.mailSender = mailSender;
        this.smtpFrom = smtpFrom;
        this.brevoFrom = brevoFrom;
        this.senderName = senderName;
        this.brevo = StringUtils.hasText(brevoApiKey)
                ? restClientBuilder.clone()
                        .baseUrl("https://api.brevo.com/v3")
                        .defaultHeader("api-key", brevoApiKey)
                        .build()
                : null;
    }

    public ResultadoCorreo enviar(String destinatario, String asunto, String html, List<AdjuntoCorreo> adjuntos) {
        if (brevo != null) return enviarBrevo(destinatario, asunto, html, adjuntos);
        return enviarSmtp(destinatario, asunto, html, adjuntos);
    }

    private ResultadoCorreo enviarBrevo(String destinatario, String asunto, String html,
                                        List<AdjuntoCorreo> adjuntos) {
        if (!StringUtils.hasText(brevoFrom)) {
            throw new IllegalStateException("Configura BREVO_FROM_EMAIL con un remitente verificado en Brevo");
        }
        List<AdjuntoBrevo> adjuntosBase64 = adjuntos == null ? List.of() : adjuntos.stream()
                .map(adjunto -> new AdjuntoBrevo(adjunto.nombre(),
                        Base64.getEncoder().encodeToString(adjunto.contenido())))
                .toList();
        try {
            var response = brevo.post()
                    .uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CorreoBrevo(
                            new RemitenteBrevo(brevoFrom, senderName),
                            List.of(new DestinatarioBrevo(destinatario)),
                            asunto,
                            html,
                            adjuntosBase64.isEmpty() ? null : adjuntosBase64))
                    .retrieve()
                    .toEntity(RespuestaBrevo.class);
            String idMensaje = response.getBody() == null ? null : response.getBody().messageId();
            return new ResultadoCorreo("BREVO", idMensaje);
        } catch (HttpStatusCodeException exception) {
            throw new IllegalStateException(
                    "Brevo rechazó la notificación (HTTP " + exception.getStatusCode().value() + ")", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo enviar la notificación mediante Brevo", exception);
        }
    }

    private ResultadoCorreo enviarSmtp(String destinatario, String asunto, String html,
                                       List<AdjuntoCorreo> adjuntos) {
        try {
            var mensaje = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mensaje, true, StandardCharsets.UTF_8.name());
            helper.setFrom(smtpFrom);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(html, true);
            if (adjuntos != null) {
                for (AdjuntoCorreo adjunto : adjuntos) {
                    helper.addAttachment(adjunto.nombre(), new ByteArrayResource(adjunto.contenido()), adjunto.contentType());
                }
            }
            mailSender.send(mensaje);
            return new ResultadoCorreo("SMTP", mensaje.getMessageID());
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible entregar el correo transaccional por SMTP", exception);
        }
    }

    public record AdjuntoCorreo(String nombre, String contentType, byte[] contenido) { }
    public record ResultadoCorreo(String canal, String idMensaje) { }
    private record CorreoBrevo(RemitenteBrevo sender, List<DestinatarioBrevo> to,
                              String subject, String htmlContent, List<AdjuntoBrevo> attachment) { }
    private record RemitenteBrevo(String email, String name) { }
    private record DestinatarioBrevo(String email) { }
    private record AdjuntoBrevo(String name, String content) { }
    private record RespuestaBrevo(String messageId) { }
}
