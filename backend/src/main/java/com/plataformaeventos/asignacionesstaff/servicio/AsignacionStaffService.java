package com.plataformaeventos.asignacionesstaff.servicio;

import com.plataformaeventos.asignacionesstaff.dto.*;
import com.plataformaeventos.asignacionesstaff.modelo.*;
import com.plataformaeventos.asignacionesstaff.repositorio.*;
import com.plataformaeventos.audit.servicio.BitacoraService;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.postulacionesstaff.modelo.*;
import com.plataformaeventos.postulacionesstaff.repositorio.PostulacionStaffRepository;
import com.plataformaeventos.usuarios.modelo.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AsignacionStaffService {
    private final AsignacionStaffRepository asignaciones;
    private final PostulacionStaffRepository postulaciones;
    private final BitacoraService bitacora;

    public AsignacionStaffService(AsignacionStaffRepository a, PostulacionStaffRepository p, BitacoraService b) { asignaciones=a; postulaciones=p; bitacora=b; }

    @Transactional
    public AsignacionStaffRespuesta crear(CrearAsignacionStaffSolicitud solicitud, Organizacion org, Usuario actor) {
        PostulacionStaff postulacion = postulaciones.findByIdAndOrganizacionId(solicitud.idPostulacion(), org.getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Postulacion no encontrada"));
        if (postulacion.getEstado() != EstadoPostulacionStaff.ACEPTADA) throw new IllegalStateException("Solo se puede asignar una postulacion aceptada");
        if (asignaciones.existsByPostulacionIdAndOrganizacionIdAndEstado(postulacion.getId(), org.getId(), EstadoAsignacionStaff.ASIGNADA)) throw new IllegalStateException("La postulacion ya tiene una asignacion activa");
        validarTurno(solicitud);
        if (asignaciones.existeConflicto(org.getId(), postulacion.getUsuario().getId(), solicitud.turnoInicio(), solicitud.turnoFin(), null))
            throw new IllegalStateException("El usuario ya tiene un turno solapado en la organizacion");
        AsignacionStaff asignacion = asignaciones.save(new AsignacionStaff(postulacion, org, solicitud.tarea().trim(), solicitud.turnoInicio(), solicitud.turnoFin(), solicitud.observaciones()));
        bitacora.registrar(actor, org, "CREAR", "ASIGNACIONES_STAFF", "ASIGNACION_STAFF", asignacion.getId(), com.plataformaeventos.audit.servicio.AuditoriaContexto.ipActual(), true, "Asignacion creada");
        return AsignacionStaffRespuesta.desde(asignacion);
    }

    @Transactional(readOnly = true)
    public List<AsignacionStaffRespuesta> listar(Long org) { return asignaciones.findByOrganizacionIdOrderByTurnoInicioAsc(org).stream().map(AsignacionStaffRespuesta::desde).toList(); }
    @Transactional(readOnly = true)
    public List<AsignacionStaffRespuesta> propias(Long org, Long usuario) { return asignaciones.findByOrganizacionIdAndPostulacionUsuarioIdOrderByTurnoInicioAsc(org, usuario).stream().map(AsignacionStaffRespuesta::desde).toList(); }

    @Transactional
    public AsignacionStaffRespuesta cancelar(Long id, Organizacion org, Usuario actor) {
        AsignacionStaff asignacion = asignaciones.findByIdAndOrganizacionId(id, org.getId()).orElseThrow(() -> new ExcepcionNoEncontrado("Asignacion no encontrada"));
        asignacion.cancelar();
        bitacora.registrar(actor, org, "CANCELAR", "ASIGNACIONES_STAFF", "ASIGNACION_STAFF", id, com.plataformaeventos.audit.servicio.AuditoriaContexto.ipActual(), true, "Asignacion cancelada");
        return AsignacionStaffRespuesta.desde(asignacion);
    }

    private void validarTurno(CrearAsignacionStaffSolicitud s) {
        if (s.turnoInicio() == null ^ s.turnoFin() == null) throw new IllegalArgumentException("El turno debe tener inicio y fin");
        if (s.turnoInicio() != null && !s.turnoFin().isAfter(s.turnoInicio())) throw new IllegalArgumentException("El fin del turno debe ser posterior al inicio");
    }
}
