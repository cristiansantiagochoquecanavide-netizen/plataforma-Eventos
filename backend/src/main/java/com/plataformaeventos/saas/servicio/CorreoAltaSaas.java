package com.plataformaeventos.saas.servicio;

import com.plataformaeventos.saas.modelo.SolicitudAltaSaas;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class CorreoAltaSaas {
    private final JavaMailSender correoSender;
    private final String urlFrontend;
    private final String correoRemitente;

    public CorreoAltaSaas(
            JavaMailSender correoSender,
            @Value("${seguridad.recuperacion.url-frontend-web}") String urlFrontend,
            @Value("${seguridad.recuperacion.correo-remitente}") String correoRemitente
    ) {
        this.correoSender = correoSender;
        this.urlFrontend = quitarBarraFinal(urlFrontend);
        this.correoRemitente = correoRemitente;
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
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(correoRemitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(texto);
        correoSender.send(mensaje);
    }

    private String quitarBarraFinal(String url) {
        String normalizada = url == null ? "" : url.trim();
        while (normalizada.endsWith("/")) normalizada = normalizada.substring(0, normalizada.length() - 1);
        return normalizada;
    }
}
