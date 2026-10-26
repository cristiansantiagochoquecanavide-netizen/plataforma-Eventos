package com.plataformaeventos.notificaciones;

import com.plataformaeventos.categorias.modelo.Categoria;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.inscripciones.dto.CredencialDigitalRespuesta;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.inscripciones.servicio.CredencialQrService;
import com.plataformaeventos.notificaciones.modelo.EstadoEnvioNotificacion;
import com.plataformaeventos.notificaciones.modelo.NotificacionEvento;
import com.plataformaeventos.notificaciones.modelo.TipoNotificacionEvento;
import com.plataformaeventos.notificaciones.repositorio.NotificacionEventoRepository;
import com.plataformaeventos.notificaciones.servicio.CorreoTransaccionalService;
import com.plataformaeventos.notificaciones.servicio.NotificacionEventoService;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.usuarios.modelo.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificacionEventoServiceTest {
    private final NotificacionEventoRepository notificaciones = mock(NotificacionEventoRepository.class);
    private final InscripcionRepository inscripciones = mock(InscripcionRepository.class);
    private final EventoRepository eventos = mock(EventoRepository.class);
    private final CredencialQrService credenciales = mock(CredencialQrService.class);
    private final CorreoTransaccionalService correo = mock(CorreoTransaccionalService.class);
    private final NotificacionEventoService service = new NotificacionEventoService(
            notificaciones, inscripciones, eventos, credenciales, correo);

    @Test
    void programaRecordatoriosDe24Y2HorasUnaVezPorInscripcionConfirmada() {
        var organization = mock(Organizacion.class);
        when(organization.getId()).thenReturn(7L);
        var event = mock(Evento.class);
        OffsetDateTime start = OffsetDateTime.now().plusHours(20);
        when(event.getId()).thenReturn(81L);
        when(event.getOrganizacion()).thenReturn(organization);
        when(event.getEstado()).thenReturn(EstadoEvento.PUBLICADO);
        when(event.getFechaInicio()).thenReturn(start);
        when(event.getNombre()).thenReturn("Festival Demo");
        when(event.getLugarPrincipal()).thenReturn("Auditorio Norte");
        when(event.getEspacio()).thenReturn(null);
        var registrationA = inscripcion(101L, event);
        var registrationB = inscripcion(102L, event);
        when(eventos.findUpcomingForReminders(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of(event));
        when(inscripciones.findConfirmedByEventoAndOrganizacion(81L, 7L))
                .thenReturn(List.of(registrationA, registrationB));
        when(notificaciones.existsByOrganizacionIdAndClaveDeduplicacion(eq(7L), anyString())).thenReturn(false);
        when(notificaciones.save(any(NotificacionEvento.class))).thenAnswer(call -> call.getArgument(0));

        int created = service.programarRecordatoriosProximos();

        var captor = org.mockito.ArgumentCaptor.forClass(NotificacionEvento.class);
        verify(notificaciones, times(4)).save(captor.capture());
        assertThat(created).isEqualTo(4);
        assertThat(captor.getAllValues()).extracting(NotificacionEvento::getTipo)
                .containsExactlyInAnyOrder(
                        TipoNotificacionEvento.RECORDATORIO_24H,
                        TipoNotificacionEvento.RECORDATORIO_2H,
                        TipoNotificacionEvento.RECORDATORIO_24H,
                        TipoNotificacionEvento.RECORDATORIO_2H);
        assertThat(captor.getAllValues()).allSatisfy(notification -> {
            assertThat(notification.getEstadoEnvio()).isEqualTo(EstadoEnvioNotificacion.PENDIENTE);
            assertThat(notification.getOrganizacion()).isSameAs(organization);
            assertThat(notification.getCuerpoMensaje()).contains("Auditorio Norte");
        });
    }

    @Test
    void despachaConfirmacionConQrYRegistraIdExterno() {
        var event = mock(Evento.class);
        OffsetDateTime eventStart = OffsetDateTime.now().plusDays(1);
        when(event.getId()).thenReturn(81L);
        when(event.getNombre()).thenReturn("Festival Demo");
        when(event.getFechaInicio()).thenReturn(eventStart);
        when(event.getLugarPrincipal()).thenReturn("Auditorio Norte");
        when(event.getEspacio()).thenReturn(null);
        var user = mock(Usuario.class);
        when(user.getCorreo()).thenReturn("ana@example.test");
        when(user.getNombres()).thenReturn("Ana");
        var registration = inscripcion(101L, event);
        when(registration.getParticipante()).thenReturn(user);
        when(registration.getEstado()).thenReturn(EstadoInscripcion.CONFIRMADA);
        var notification = mock(NotificacionEvento.class);
        when(notification.getTipo()).thenReturn(TipoNotificacionEvento.CONFIRMACION_INSCRIPCION);
        when(notification.getInscripcion()).thenReturn(registration);
        when(notification.getEvento()).thenReturn(event);
        when(notification.getUsuario()).thenReturn(user);
        when(notification.getAsunto()).thenReturn("Inscripción confirmada");
        when(notification.getCuerpoMensaje()).thenReturn("Tu acceso está confirmado.");
        when(notificaciones.bloquearPendientes(eq(EstadoEnvioNotificacion.PENDIENTE),
                any(OffsetDateTime.class), any(Pageable.class))).thenReturn(List.of(notification));
        when(credenciales.emitirPara(registration)).thenReturn(new CredencialDigitalRespuesta(
                101L, "Ana Prueba", "Festival Demo", "General", eventStart,
                "Auditorio Norte", "payload.signature", Base64.getEncoder().encodeToString(new byte[]{1, 2, 3}),
                OffsetDateTime.now().plusDays(1)));
        when(correo.enviar(eq("ana@example.test"), eq("Inscripción confirmada"), anyString(), anyList()))
                .thenReturn(new CorreoTransaccionalService.ResultadoCorreo("BREVO", "<msg-123>"));

        int processed = service.despacharPendientes();

        assertThat(processed).isEqualTo(1);
        verify(notification).iniciarEnvio();
        verify(notification).marcarEnviado(eq("BREVO"), eq("<msg-123>"), any(OffsetDateTime.class));
        verify(correo).enviar(eq("ana@example.test"), eq("Inscripción confirmada"),
                contains("Auditorio Norte"), argThat(adjunto -> adjunto.size() == 1
                        && adjunto.getFirst().nombre().equals("credencial-101.png")));
        verify(notificaciones).flush();
    }

    @Test
    void siBrevoFallaLaNotificacionSeMarcaFallidaYNoSePierde() {
        var event = mock(Evento.class);
        OffsetDateTime eventStart = OffsetDateTime.now().plusDays(1);
        when(event.getId()).thenReturn(81L);
        when(event.getNombre()).thenReturn("Festival Demo");
        when(event.getFechaInicio()).thenReturn(eventStart);
        when(event.getLugarPrincipal()).thenReturn("Auditorio Norte");
        when(event.getEspacio()).thenReturn(null);
        var user = mock(Usuario.class);
        when(user.getCorreo()).thenReturn("ana@example.test");
        when(user.getNombres()).thenReturn("Ana");
        var registration = inscripcion(101L, event);
        when(registration.getParticipante()).thenReturn(user);
        when(registration.getEstado()).thenReturn(EstadoInscripcion.CONFIRMADA);
        var notification = mock(NotificacionEvento.class);
        when(notification.getTipo()).thenReturn(TipoNotificacionEvento.CONFIRMACION_INSCRIPCION);
        when(notification.getInscripcion()).thenReturn(registration);
        when(notification.getEvento()).thenReturn(event);
        when(notification.getUsuario()).thenReturn(user);
        when(notification.getAsunto()).thenReturn("Inscripción confirmada");
        when(notification.getCuerpoMensaje()).thenReturn("Tu acceso está confirmado.");
        when(notificaciones.bloquearPendientes(eq(EstadoEnvioNotificacion.PENDIENTE),
                any(OffsetDateTime.class), any(Pageable.class))).thenReturn(List.of(notification));
        when(credenciales.emitirPara(registration)).thenReturn(new CredencialDigitalRespuesta(
                101L, "Ana Prueba", "Festival Demo", "General", eventStart,
                "Auditorio Norte", "payload.signature", Base64.getEncoder().encodeToString(new byte[]{1}),
                OffsetDateTime.now().plusDays(1)));
        when(correo.enviar(anyString(), anyString(), anyString(), anyList()))
                .thenThrow(new IllegalStateException("Brevo no disponible"));

        assertThat(service.despacharPendientes()).isEqualTo(1);

        verify(notification).marcarFallido("Brevo no disponible");
        verify(notificaciones).flush();
    }

    private Inscripcion inscripcion(Long id, Evento event) {
        var enrollment = mock(Inscripcion.class);
        when(enrollment.getId()).thenReturn(id);
        when(enrollment.getEvento()).thenReturn(event);
        when(enrollment.getEstado()).thenReturn(EstadoInscripcion.CONFIRMADA);
        when(enrollment.getFechaInscripcion()).thenReturn(OffsetDateTime.now());
        var user = mock(Usuario.class);
        when(user.getCorreo()).thenReturn("participante" + id + "@example.test");
        when(user.getNombres()).thenReturn("Participante");
        when(user.getApellidos()).thenReturn(id.toString());
        when(enrollment.getParticipante()).thenReturn(user);
        return enrollment;
    }
}
