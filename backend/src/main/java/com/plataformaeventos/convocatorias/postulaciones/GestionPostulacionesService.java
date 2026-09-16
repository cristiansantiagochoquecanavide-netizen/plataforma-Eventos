package com.plataformaeventos.convocatorias.postulaciones;

import com.plataformaeventos.convocatorias.modelo.Convocatoria;
import com.plataformaeventos.convocatorias.modelo.EstadoConvocatoria;
import com.plataformaeventos.convocatorias.postulaciones.dto.PostulanteConvocatoriaRespuesta;
import com.plataformaeventos.convocatorias.repositorio.ConvocatoriaRepository;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.roles.modelo.EstadoRol;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GestionPostulacionesService {

    private final PostulacionStaffRepository postulacionRepository;
    private final AsignacionStaffRepository asignacionRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;

    public GestionPostulacionesService(
            PostulacionStaffRepository postulacionRepository,
            AsignacionStaffRepository asignacionRepository,
            ConvocatoriaRepository convocatoriaRepository,
            EventoRepository eventoRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.postulacionRepository = postulacionRepository;
        this.asignacionRepository = asignacionRepository;
        this.convocatoriaRepository = convocatoriaRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<PostulanteConvocatoriaRespuesta> listarPostulantes(Long idConvocatoria, String correoGestor) {
        Convocatoria convocatoria = buscarConvocatoria(idConvocatoria);
        validarResponsable(convocatoria, correoGestor);
        return postulacionRepository.findByConvocatoriaIdOrderByFechaPostulacionAscIdAsc(idConvocatoria)
                .stream()
                .map(this::convertirARespuesta)
                .toList();
    }

    @Transactional
    public PostulanteConvocatoriaRespuesta seleccionar(Long idPostulacion, String correoGestor) {
        PostulacionStaff referencia = buscarPostulacion(idPostulacion);
        Convocatoria convocatoria = convocatoriaRepository.buscarParaGestion(
                        referencia.getConvocatoria().getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado(
                        "Convocatoria no encontrada con id " + referencia.getConvocatoria().getId()));
        validarResponsable(convocatoria, correoGestor);
        validarConvocatoriaAbierta(convocatoria);

        PostulacionStaff postulacion = postulacionRepository.buscarParaGestion(idPostulacion)
                .orElseThrow(() -> new ExcepcionNoEncontrado(
                        "Postulacion no encontrada con id " + idPostulacion));
        if (postulacion.getEstado() != EstadoPostulacion.PENDIENTE) {
            throw new IllegalArgumentException("Solo se puede seleccionar una postulacion pendiente");
        }
        if (postulacion.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new IllegalArgumentException("Solo se puede seleccionar un usuario activo");
        }
        if (asignacionRepository.existsByPostulacionId(idPostulacion)) {
            throw new IllegalArgumentException("La postulacion ya fue asignada al equipo");
        }
        if (asignacionRepository.countByPostulacionConvocatoriaId(convocatoria.getId())
                >= convocatoria.getCantidadRequerida()) {
            throw new IllegalArgumentException("La convocatoria ya completo la cantidad requerida");
        }

        postulacion.seleccionar();
        postulacionRepository.saveAndFlush(postulacion);
        try {
            asignacionRepository.saveAndFlush(new AsignacionStaff(postulacion));
        } catch (DataIntegrityViolationException excepcion) {
            if (esAsignacionDuplicada(excepcion)) {
                throw new IllegalArgumentException("La postulacion ya fue asignada al equipo", excepcion);
            }
            throw excepcion;
        }
        return convertirARespuesta(postulacion);
    }

    @Transactional
    public PostulanteConvocatoriaRespuesta rechazar(Long idPostulacion, String correoGestor) {
        PostulacionStaff referencia = buscarPostulacion(idPostulacion);
        Convocatoria convocatoria = convocatoriaRepository.buscarParaGestion(
                        referencia.getConvocatoria().getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado(
                        "Convocatoria no encontrada con id " + referencia.getConvocatoria().getId()));
        validarResponsable(convocatoria, correoGestor);
        PostulacionStaff postulacion = postulacionRepository.buscarParaGestion(idPostulacion)
                .orElseThrow(() -> new ExcepcionNoEncontrado(
                        "Postulacion no encontrada con id " + idPostulacion));
        postulacion.rechazar();
        return convertirARespuesta(postulacionRepository.saveAndFlush(postulacion));
    }

    private Convocatoria buscarConvocatoria(Long id) {
        return convocatoriaRepository.findById(id)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Convocatoria no encontrada con id " + id));
    }

    private PostulacionStaff buscarPostulacion(Long id) {
        return postulacionRepository.findById(id)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Postulacion no encontrada con id " + id));
    }

    private void validarConvocatoriaAbierta(Convocatoria convocatoria) {
        if (convocatoria.getEstado() != EstadoConvocatoria.ABIERTA) {
            throw new IllegalArgumentException("Solo se pueden gestionar postulaciones de convocatorias abiertas");
        }
    }

    private void validarResponsable(Convocatoria convocatoria, String correoGestor) {
        Usuario gestor = usuarioRepository.findByCorreoConRoles(correoGestor)
                .orElseThrow(() -> new AccessDeniedException("El usuario no puede gestionar esta convocatoria"));
        if (gestor.getEstado() != EstadoUsuario.ACTIVO) {
            throw new AccessDeniedException("El usuario debe estar activo");
        }
        boolean administrador = gestor.getRoles().stream()
                .anyMatch(rol -> rol.getEstado() == EstadoRol.ACTIVO && rol.getNombre().equals("ADMINISTRADOR"));
        if (!administrador && !eventoRepository.esResponsable(convocatoria.getEvento().getId(), gestor.getId())) {
            throw new AccessDeniedException("Solo los responsables del evento pueden gestionar sus postulaciones");
        }
    }

    private boolean esAsignacionDuplicada(DataIntegrityViolationException excepcion) {
        for (Throwable causa = excepcion; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacion
                    && "uk_asignacion_staff_postulacion".equals(violacion.getConstraintName())) {
                return true;
            }
        }
        return false;
    }

    private PostulanteConvocatoriaRespuesta convertirARespuesta(PostulacionStaff postulacion) {
        Convocatoria convocatoria = postulacion.getConvocatoria();
        Usuario usuario = postulacion.getUsuario();
        return new PostulanteConvocatoriaRespuesta(
                postulacion.getId(),
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                convocatoria.getId(),
                convocatoria.getEvento().getNombre(),
                convocatoria.getTipoPersonal(),
                postulacion.getEstado(),
                postulacion.getFechaPostulacion(),
                postulacion.getFechaActualizacion()
        );
    }
}
