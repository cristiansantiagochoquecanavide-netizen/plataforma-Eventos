package com.plataformaeventos.inscripciones;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plataformaeventos.categorias.modelo.Categoria;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.inscripciones.servicio.CredencialQrService;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CredencialQrServiceTest {
    private static final String SECRET = "test-qr-hmac-secret-with-at-least-32-bytes";

    @Test
    void emiteQrPngConFirmaHmacYDatosDeEventoParaInscripcionConfirmada() throws Exception {
        var repository = mock(InscripcionRepository.class);
        var users = mock(UsuarioRepository.class);
        var enrollment = mock(Inscripcion.class);
        var user = mock(Usuario.class);
        var event = mock(Evento.class);
        var category = mock(Categoria.class);
        var organization = mock(Organizacion.class);
        when(users.findByCorreo("persona@example.test")).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(42L);
        when(user.getNombres()).thenReturn("Ana");
        when(user.getApellidos()).thenReturn("Prueba");
        when(repository.findCredentialByIdAndParticipant(17L, 42L, 9L)).thenReturn(Optional.of(enrollment));
        when(enrollment.getEstado()).thenReturn(EstadoInscripcion.CONFIRMADA);
        when(enrollment.getId()).thenReturn(17L);
        when(enrollment.getEvento()).thenReturn(event);
        when(enrollment.getOrganizacion()).thenReturn(organization);
        when(organization.getId()).thenReturn(9L);
        when(event.getId()).thenReturn(31L);
        when(event.getNombre()).thenReturn("Foro de prueba");
        when(event.getFechaInicio()).thenReturn(OffsetDateTime.now().plusDays(2));
        when(event.getLugarPrincipal()).thenReturn("Auditorio central");
        when(event.getCategoria()).thenReturn(category);
        when(category.getNombre()).thenReturn("General");
        when(event.getEspacio()).thenReturn(null);

        var service = new CredencialQrService(repository, users, new ObjectMapper(), SECRET, 24);
        var credential = service.emitir(17L, "persona@example.test", organization);

        String[] pieces = credential.token().split("\\.");
        assertEquals(2, pieces.length);
        byte[] signature = Base64.getUrlDecoder().decode(pieces[1]);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        assertArrayEquals(mac.doFinal(pieces[0].getBytes(StandardCharsets.US_ASCII)), signature);
        Map<?, ?> claims = new ObjectMapper().readValue(Base64.getUrlDecoder().decode(pieces[0]), Map.class);
        assertEquals(17, claims.get("sub"));
        assertEquals(31, claims.get("evt"));
        assertEquals(9, claims.get("org"));
        assertEquals("Ana Prueba", credential.nombreParticipante());
        assertEquals("Foro de prueba", credential.nombreEvento());
        assertTrue(Base64.getDecoder().decode(credential.qrPngBase64()).length > 100);
    }

    @Test
    void rechazaEmitirCredencialSiLaInscripcionNoEstaConfirmada() {
        var repository = mock(InscripcionRepository.class);
        var users = mock(UsuarioRepository.class);
        var enrollment = mock(Inscripcion.class);
        var user = mock(Usuario.class);
        var organization = mock(Organizacion.class);
        when(organization.getId()).thenReturn(9L);
        when(users.findByCorreo("persona@example.test")).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(42L);
        when(repository.findCredentialByIdAndParticipant(17L, 42L, 9L)).thenReturn(Optional.of(enrollment));
        when(enrollment.getEstado()).thenReturn(EstadoInscripcion.CANCELADA);

        var service = new CredencialQrService(repository, users, new ObjectMapper(), SECRET, 24);
        assertThrows(IllegalStateException.class, () -> service.emitir(17L, "persona@example.test", organization));
    }

    @Test
    void requiereClaveDeFirmaConLongitudMinima() {
        assertThrows(IllegalArgumentException.class, () -> new CredencialQrService(
                mock(InscripcionRepository.class), mock(UsuarioRepository.class), new ObjectMapper(), "short", 24));
    }
}
