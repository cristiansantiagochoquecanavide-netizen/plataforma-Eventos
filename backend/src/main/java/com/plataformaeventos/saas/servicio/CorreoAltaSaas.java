package com.plataformaeventos.saas.servicio;

import com.plataformaeventos.saas.modelo.SolicitudAltaSaas;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class CorreoAltaSaas {
    private final JavaMailSender correoSender;
    private final String urlFrontend;
    private final String correoRemitente;
    private final String correoRemitenteBrevo;
    private final String nombreRemitenteBrevo;
    private final RestClient clienteBrevo;

    public CorreoAltaSaas(
            JavaMailSender correoSender,
            @Value("${seguridad.recuperacion.url-frontend-web}") String urlFrontend,
            @Value("${seguridad.recuperacion.correo-remitente}") String correoRemitente,
            @Value("${brevo.api-key:}") String claveApiBrevo,
            @Value("${brevo.from-email:}") String correoRemitenteBrevo,
            @Value("${brevo.from-name:Plataforma de eventos}") String nombreRemitenteBrevo,
            RestClient.Builder restClientBuilder
    ) {
        this.correoSender = correoSender;
        this.urlFrontend = quitarBarraFinal(urlFrontend);
        this.correoRemitente = correoRemitente;
        this.correoRemitenteBrevo = correoRemitenteBrevo;
        this.nombreRemitenteBrevo = nombreRemitenteBrevo;
        this.clienteBrevo = StringUtils.hasText(claveApiBrevo)
                ? restClientBuilder.clone()
                        .baseUrl("https://api.brevo.com/v3")
                        .defaultHeader("api-key", claveApiBrevo)
                        .build()
                : null;
    }

    public void enviarVerificacion(SolicitudAltaSaas alta, String token) {
        String enlace = UriComponentsBuilder.fromUriString(urlFrontend)
                .path("/verificar-alta-saas")
                .queryParam("token", token)
                .build().toUriString();
        enviar(alta.getCorreoContacto(), "Confirma tu correo para continuar",
                "Hola " + alta.getNombresContacto() + ",\n\n"
                        + "Confirma tu correo para continuar con el alta de " + alta.getNombreEmpresa() + ":\n\n"
                        + enlace + "\n\nEl enlace vence en 30 minutos. Si no iniciaste esta solicitud, ignora este mensaje.");
    }

    public void enviarActivacion(SolicitudAltaSaas alta, String token) {
        String enlace = UriComponentsBuilder.fromUriString(urlFrontend)
                .path("/activar-cuenta-saas")
                .queryParam("token", token)
                .build().toUriString();
        enviar(alta.getCorreoContacto(), "Tu organización ya está lista",
                "Hola " + alta.getNombresContacto() + ",\n\n"
                        + "La suscripción de " + alta.getNombreEmpresa() + " está activa. Define tu contraseña y activa la cuenta aquí:\n\n"
                        + enlace + "\n\nEl enlace vence en 48 horas. No compartas este enlace.");
    }

    public void enviarPagoConfirmado(SolicitudAltaSaas alta) {
        enviar(alta.getCorreoContacto(), "Pago recibido para " + alta.getNombreEmpresa(),
                "Recibimos el pago de tu suscripción. En breve te enviaremos el enlace para activar el acceso de propietario.");
    }

    public void enviarOrganizacionAsociada(SolicitudAltaSaas alta) {
        String enlace = UriComponentsBuilder.fromUriString(urlFrontend)
                .path("/iniciar-sesion")
                .toUriString();
        enviar(alta.getCorreoContacto(), "Tu empresa ya está disponible",
                "Hola " + alta.getNombresContacto() + ",\n\n"
                        + "La organización " + alta.getNombreEmpresa() + " quedó asociada a tu cuenta. "
                        + "Inicia sesión para acceder: " + enlace);
    }

    private void enviar(String destinatario, String asunto, String texto) {
        if (clienteBrevo != null) {
            enviarConBrevo(destinatario, asunto, texto);
            return;
        }
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(correoRemitente);
        mensaje.setTo(destinatario);
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
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "Brevo rechazó el correo transaccional (HTTP " + exception.getStatusCode().value() + ")", exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("No se pudo conectar con la API de correo Brevo", exception);
        }
    }

    private record CorreoBrevo(RemitenteBrevo sender, List<DestinatarioBrevo> to,
                               String subject, String textContent) { }
    private record RemitenteBrevo(String email, String name) { }
    private record DestinatarioBrevo(String email) { }

    private String quitarBarraFinal(String url) {
        String normalizada = url == null ? "" : url.trim();
        while (normalizada.endsWith("/")) normalizada = normalizada.substring(0, normalizada.length() - 1);
        return normalizada;
    }
}
