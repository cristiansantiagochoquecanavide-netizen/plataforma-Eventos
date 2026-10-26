package com.plataformaeventos.asistencias.servicio;

import com.plataformaeventos.asignacionesstaff.modelo.EstadoAsignacionStaff;
import com.plataformaeventos.asistencias.dto.AsistenciaRespuesta;
import com.plataformaeventos.asistencias.dto.InscripcionElegibleAsistenciaRespuesta;
import com.plataformaeventos.asistencias.modelo.Asistencia;
import com.plataformaeventos.asistencias.repositorio.AsignacionAsistenciaRepository;
import com.plataformaeventos.asistencias.repositorio.AsistenciaRepository;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsistenciaService {
    private final AsistenciaRepository asistencias;
    private final AsignacionAsistenciaRepository asignaciones;
    private final InscripcionRepository inscripciones;
    private final UsuarioRepository usuarios;

    public AsistenciaService(AsistenciaRepository asistencias,
                             AsignacionAsistenciaRepository asignaciones,
                             InscripcionRepository inscripciones,
                             UsuarioRepository usuarios) {
        this.asistencias = asistencias;
        this.asignaciones = asignaciones;
        this.inscripciones = inscripciones;
        this.usuarios = usuarios;
    }

    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "ASISTENCIAS", entidad = "ASISTENCIA", accion = "REGISTRAR")
    public AsistenciaRespuesta registrar(Long idInscripcion, String correoStaff, Organizacion organizacion) {
        if (idInscripcion == null) throw new IllegalArgumentException("idInscripcion es obligatorio");
        var staff = usuarios.findByCorreo(correoStaff.trim().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Usuario staff no encontrado"));
        Inscripcion inscripcion = inscripciones.bloquearPorIdEOrganizacion(idInscripcion, organizacion.getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Inscripcion no encontrada"));

        if (!asignaciones.existsByOrganizacionIdAndPostulacionUsuarioIdAndPostulacionConvocatoriaEventoIdAndEstado(
                organizacion.getId(), staff.getId(), inscripcion.getEvento().getId(), EstadoAsignacionStaff.ASIGNADA)) {
            throw new org.springframework.security.access.AccessDeniedException("No tiene una asignacion activa para este evento");
        }
        if (inscripcion.getEstado() != EstadoInscripcion.CONFIRMADA) {
            throw new IllegalStateException("Solo se registra asistencia para inscripciones confirmadas");
        }
        if (inscripcion.getEvento().getEstado() != EstadoEvento.PUBLICADO
                && inscripcion.getEvento().getEstado() != EstadoEvento.EN_CURSO) {
            throw new IllegalStateException("Solo se registra asistencia en eventos publicados o en curso");
        }
        if (asistencias.existsByInscripcionId(idInscripcion)) {
            throw new com.plataformaeventos.asistencias.servicio.AsistenciaDuplicadaException();
        }

        Asistencia asistencia = asistencias.save(new Asistencia(inscripcion, organizacion, staff));
        asistencias.flush();
        return AsistenciaRespuesta.desde(asistencia);
    }

    @Transactional(readOnly = true)
    public java.util.List<InscripcionElegibleAsistenciaRespuesta> listarInscripcionesElegibles(
            String correoStaff, Organizacion organizacion) {
        var staff = usuarios.findByCorreo(correoStaff.trim().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Usuario staff no encontrado"));
        return inscripciones.listarElegiblesParaAsistencia(organizacion.getId(), staff.getId());
    }
}
