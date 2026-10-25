package com.plataformaeventos.postulacionesstaff.servicio;

import com.plataformaeventos.audit.servicio.BitacoraService;
import com.plataformaeventos.convocatorias.modelo.*;
import com.plataformaeventos.convocatorias.repositorio.ConvocatoriaRepository;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.postulacionesstaff.dto.*;
import com.plataformaeventos.postulacionesstaff.modelo.*;
import com.plataformaeventos.postulacionesstaff.repositorio.PostulacionStaffRepository;
import com.plataformaeventos.usuarios.modelo.*;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class PostulacionStaffService {
    private final PostulacionStaffRepository postulaciones;
    private final ConvocatoriaRepository convocatorias;
    private final UsuarioRepository usuarios;
    private final MembresiaOrganizacionRepository membresias;
    private final BitacoraService bitacora;

    public PostulacionStaffService(PostulacionStaffRepository p, ConvocatoriaRepository c, UsuarioRepository u,
                                   MembresiaOrganizacionRepository m, BitacoraService b) {
        postulaciones = p; convocatorias = c; usuarios = u; membresias = m; bitacora = b;
    }

    @Transactional
    public PostulacionStaffRespuesta crear(Long convocatoriaId, String correo, Organizacion org,
                                           CrearPostulacionStaffSolicitud solicitud) {
        Usuario usuario = usuario(correo);
        validarMembresia(usuario, org);
        Convocatoria convocatoria = convocatorias.findByIdAndOrganizacionIdForUpdate(convocatoriaId, org.getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Convocatoria no encontrada"));
        validarConvocatoriaAbierta(convocatoria);
        if (postulaciones.existsByConvocatoriaIdAndUsuarioIdAndOrganizacionId(convocatoriaId, usuario.getId(), org.getId()))
            throw new IllegalStateException("Ya existe una postulacion para esta convocatoria");
        PostulacionStaff postulacion = postulaciones.save(new PostulacionStaff(convocatoria, usuario, org, solicitud.motivacion()));
        bitacora.registrar(usuario, org, "CREAR", "POSTULACIONES_STAFF", "POSTULACION_STAFF", postulacion.getId(), com.plataformaeventos.audit.servicio.AuditoriaContexto.ipActual(), true, "Postulacion creada");
        return PostulacionStaffRespuesta.desde(postulacion);
    }

    @Transactional(readOnly = true)
    public List<PostulacionStaffRespuesta> propias(String correo, Long org) {
        return postulaciones.findByUsuarioIdAndOrganizacionIdOrderByFechaPostulacionDesc(usuario(correo).getId(), org)
                .stream().map(PostulacionStaffRespuesta::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<PostulacionStaffRespuesta> listar(Long convocatoria, Long org) {
        return postulaciones.findByConvocatoriaIdAndOrganizacionIdOrderByFechaPostulacionDesc(convocatoria, org)
                .stream().map(PostulacionStaffRespuesta::desde).toList();
    }

    @Transactional
    public PostulacionStaffRespuesta cambiar(Long id, Organizacion org, boolean aceptar, Usuario actor) {
        PostulacionStaff postulacion = postulaciones.findByIdAndOrganizacionId(id, org.getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Postulacion no encontrada"));
        Convocatoria convocatoria = convocatorias.findByIdAndOrganizacionIdForUpdate(
                postulacion.getConvocatoria().getId(), org.getId()).orElseThrow(() -> new ExcepcionNoEncontrado("Convocatoria no encontrada"));
        validarConvocatoriaAbierta(convocatoria);
        if (aceptar && postulaciones.countByConvocatoriaIdAndOrganizacionIdAndEstado(
                convocatoria.getId(), org.getId(), EstadoPostulacionStaff.ACEPTADA) >= convocatoria.getCantidadRequerida())
            throw new IllegalStateException("La convocatoria ya alcanzo la cantidad requerida");
        if (aceptar) postulacion.aceptar(); else postulacion.rechazar();
        bitacora.registrar(actor, org, aceptar ? "ACEPTAR" : "RECHAZAR", "POSTULACIONES_STAFF", "POSTULACION_STAFF", id, com.plataformaeventos.audit.servicio.AuditoriaContexto.ipActual(), true, postulacion.getEstado().name());
        return PostulacionStaffRespuesta.desde(postulacion);
    }

    private void validarConvocatoriaAbierta(Convocatoria c) {
        if (c.getEstado() != EstadoConvocatoria.ABIERTA || c.getFechaLimite().isBefore(LocalDate.now()))
            throw new IllegalStateException("La convocatoria no esta abierta o su fecha limite vencio");
    }
    private void validarMembresia(Usuario u, Organizacion o) {
        if (u.getEstado() != EstadoUsuario.ACTIVO || !membresias.existsByUsuarioIdAndOrganizacionIdAndActivoTrue(u.getId(), o.getId()))
            throw new IllegalStateException("El postulante no tiene una membresia activa en la organizacion");
    }
    private Usuario usuario(String correo) { return usuarios.findByCorreo(correo.trim().toLowerCase()).orElseThrow(() -> new ExcepcionNoEncontrado("Usuario no encontrado")); }
}
