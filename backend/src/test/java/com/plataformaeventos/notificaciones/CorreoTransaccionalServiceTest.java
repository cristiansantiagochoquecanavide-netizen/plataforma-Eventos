package com.plataformaeventos.notificaciones;

import com.plataformaeventos.notificaciones.servicio.CorreoTransaccionalService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class CorreoTransaccionalServiceTest {
    @Test
    void enviaCorreoYAdjuntoPorApiBrevoYDevuelveElIdentificador() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-api-key"))
                .andExpect(content().json("""
                        {
                          "sender": {"email": "eventos@example.test", "name": "Eventos"},
                          "to": [{"email": "ana@example.test"}],
                          "subject": "Tu entrada está confirmada",
                          "htmlContent": "<p>Inscripción confirmada</p>",
                          "attachment": [{"name": "credencial.png", "content": "UVJDT0RF"}]
                        }
                        """))
                .andRespond(withStatus(org.springframework.http.HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"messageId\":\"<brevo-123>\"}"));

        var service = new CorreoTransaccionalService(
                mock(JavaMailSender.class), builder, "smtp@example.test", "test-api-key",
                "eventos@example.test", "Eventos");
        var resultado = service.enviar("ana@example.test", "Tu entrada está confirmada",
                "<p>Inscripción confirmada</p>", List.of(new CorreoTransaccionalService.AdjuntoCorreo(
                        "credencial.png", "image/png", "QRCODE".getBytes(StandardCharsets.UTF_8))));

        server.verify();
        assertThat(resultado.canal()).isEqualTo("BREVO");
        assertThat(resultado.idMensaje()).isEqualTo("<brevo-123>");
    }

    @Test
    void informaSiBrevoRechazaElCorreo() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED));
        var service = new CorreoTransaccionalService(
                mock(JavaMailSender.class), builder, "smtp@example.test", "test-api-key",
                "eventos@example.test", "Eventos");

        assertThatThrownBy(() -> service.enviar("ana@example.test", "Aviso", "<p>Aviso</p>", List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Brevo rechazó la notificación (HTTP 401)");
        server.verify();
    }
}
