package com.plataformaeventos.seguridad;

import com.plataformaeventos.usuarios.EstadoUsuario;
import com.plataformaeventos.usuarios.Usuario;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecuperacionClaveServiceTest {

    private final TokenRecuperacionRepository tokenRepository = mock(TokenRecuperacionRepository.class);
    private final JavaMailSender correoSender = mock(JavaMailSender.class);
    private final RecuperacionClaveService servicio = new RecuperacionClaveService(
            tokenRepository,
            correoSender,
            new RecuperacionClavePropiedades(
                    30,
                    60,
                    "http://localhost:4200",
                    "eventos://recuperar-clave",
                    "no-reply@eventos.local"
            )
    );

    @Test
    void crearTokenInvalidaPendientesGuardaHashYEnviaInstrucciones() {
        Usuario usuario = usuarioConId(7L);
        ArgumentCaptor<TokenRecuperacion> tokenCaptor = ArgumentCaptor.forClass(TokenRecuperacion.class);
        ArgumentCaptor<SimpleMailMessage> mensajeCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        servicio.crearYEnviarToken(usuario);

        verify(tokenRepository).invalidarTokensPendientesPorUsuario(7L);
        verify(tokenRepository).saveAndFlush(tokenCaptor.capture());
        verify(correoSender).send(mensajeCaptor.capture());

        assertThat(tokenCaptor.getValue().estaActivo()).isTrue();
        SimpleMailMessage mensaje = mensajeCaptor.getValue();
        assertThat(mensaje.getFrom()).isEqualTo("no-reply@eventos.local");
        assertThat(mensaje.getTo()).containsExactly("persona@eventos.local");
        assertThat(mensaje.getText())
                .contains("Plataforma de Gestion de Eventos")
                .contains("eventos://recuperar-clave?token=")
                .contains("pega este token")
                .contains("30 minutos");
    }

    @Test
    void consumirTokenValidoLoMarcaComoUtilizadoEInactivo() {
        Usuario usuario = usuarioConId(8L);
        TokenRecuperacion token = new TokenRecuperacion(
                usuario,
                "hash",
                OffsetDateTime.now().plusMinutes(10)
        );
        when(tokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(token));

        Usuario resultado = servicio.consumirToken("token-valido");

        assertThat(resultado).isSameAs(usuario);
        assertThat(token.fueUtilizado()).isTrue();
        assertThat(token.estaActivo()).isFalse();
    }

    @Test
    void rechazaTokenExpiradoYLoInactiva() {
        TokenRecuperacion token = new TokenRecuperacion(
                usuarioConId(9L),
                "hash",
                OffsetDateTime.now().minusMinutes(1)
        );
        when(tokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> servicio.consumirToken("token-expirado"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La solicitud expir\u00f3.");
        assertThat(token.fueUtilizado()).isFalse();
        assertThat(token.estaActivo()).isFalse();
    }

    @Test
    void rechazaTokenReutilizado() {
        TokenRecuperacion token = new TokenRecuperacion(
                usuarioConId(10L),
                "hash",
                OffsetDateTime.now().plusMinutes(10)
        );
        token.marcarComoUtilizado(OffsetDateTime.now().minusMinutes(1));
        when(tokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> servicio.consumirToken("token-usado"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El enlace ya fue utilizado.");
    }

    @Test
    void rechazaTokenInexistente() {
        when(tokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.consumirToken("token-inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La solicitud expir\u00f3.");
    }

    private Usuario usuarioConId(Long id) {
        Usuario usuario = new Usuario(
                "Persona",
                "Prueba",
                "persona@eventos.local",
                "hash",
                EstadoUsuario.ACTIVO
        );
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }
}
