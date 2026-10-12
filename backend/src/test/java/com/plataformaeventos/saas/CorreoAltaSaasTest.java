package com.plataformaeventos.saas;

import com.plataformaeventos.saas.modelo.PlanSuscripcion;
import com.plataformaeventos.saas.modelo.SolicitudAltaSaas;
import com.plataformaeventos.saas.servicio.CorreoAltaSaas;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CorreoAltaSaasTest {
    private static final String URL_FRONTEND = "http://localhost:4200";

    @Test
    void enviaVerificacionConApiKeyDeBrevoYRemitenteVerificado() {
        JavaMailSender smtp = mock(JavaMailSender.class);
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer brevo = MockRestServiceServer.bindTo(builder).build();
        CorreoAltaSaas correo = new CorreoAltaSaas(
                smtp, URL_FRONTEND, "no-reply@eventos.local", "xkeysib-test", "verificado@example.com",
                "Plataforma", builder);

        brevo.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "xkeysib-test"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sender.email").value("verificado@example.com"))
                .andExpect(jsonPath("$.sender.name").value("Plataforma"))
                .andExpect(jsonPath("$.to[0].email").value("ana@example.com"))
                .andExpect(jsonPath("$.subject").value("Confirma tu correo para continuar"))
                .andExpect(jsonPath("$.textContent", containsString("/verificar-alta-saas?token=token-prueba")))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        correo.enviarVerificacion(alta(), "token-prueba");

        brevo.verify();
        verify(smtp, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void conservaSmtpCuandoNoHayApiKeyDeBrevo() {
        JavaMailSender smtp = mock(JavaMailSender.class);
        CorreoAltaSaas correo = new CorreoAltaSaas(
                smtp, URL_FRONTEND, "no-reply@eventos.local", "", "", "Plataforma", RestClient.builder());

        correo.enviarVerificacion(alta(), "token-prueba");

        var mensaje = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(smtp).send(mensaje.capture());
        assertThat(mensaje.getValue().getTo()).containsExactly("ana@example.com");
        assertThat(mensaje.getValue().getText()).contains("/verificar-alta-saas?token=token-prueba");
    }

    @Test
    void exigeRemitenteBrevoVerificadoCuandoLaApiEstaHabilitada() {
        CorreoAltaSaas correo = new CorreoAltaSaas(
                mock(JavaMailSender.class), URL_FRONTEND, "no-reply@eventos.local", "xkeysib-test", "", "Plataforma",
                RestClient.builder());

        assertThatThrownBy(() -> correo.enviarVerificacion(alta(), "token-prueba"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BREVO_FROM_EMAIL");
    }

    private SolicitudAltaSaas alta() {
        return new SolicitudAltaSaas(UUID.randomUUID(), "Colectivo", "colectivo", "Ana", "Prueba",
                "ana@example.com", PlanSuscripcion.PROFESIONAL, "hash", OffsetDateTime.now().plusMinutes(30));
    }
}
