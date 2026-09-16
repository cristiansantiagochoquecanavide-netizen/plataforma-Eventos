package com.plataformaeventos.convocatorias.postulaciones;

import com.plataformaeventos.convocatorias.Convocatoria;
import com.plataformaeventos.convocatorias.ConvocatoriaRepository;
import com.plataformaeventos.convocatorias.EstadoConvocatoria;
import com.plataformaeventos.convocatorias.postulaciones.dto.PostulacionStaffRespuesta;
import com.plataformaeventos.excepciones.ExcepcionNoAutorizado;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.usuarios.EstadoUsuario;
import com.plataformaeventos.usuarios.Usuario;
import com.plataformaeventos.usuarios.UsuarioRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PostulacionStaffService {

    private final PostulacionStaffRepository postulacionRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public PostulacionStaffService(PostulacionStaffRepository postulacionRepository,
                                  ConvocatoriaRepository convocatoriaRepository,
                                  UsuarioRepository usuarioRepository) {
        this.postulacionRepository = postulacionRepository;
        this.convocatoriaRepository = convocatoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public PostulacionStaffRespuesta postularse(Long idConvocatoria, String correo) {
        Usuario usuario = buscarUsuarioActivo(correo);
        Convocatoria convocatoria = convocatoriaRepository.buscarParaPostulacion(idConvocatoria)
                .orElseThrow(() -> new ExcepcionNoEncontrado(
                        "Convocatoria no encontrada con id " + idConvocatoria
                ));
        if (convocatoria.getEstado() != EstadoConvocatoria.ABIERTA) {
            throw new IllegalArgumentException("Solo se puede postular a una convocatoria abierta");
        }
        if (convocatoria.getFechaLimite().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha limite de la convocatoria ha vencido");
        }
        if (postulacionRepository.existsByConvocatoriaIdAndUsuarioId(idConvocatoria, usuario.getId())) {
            throw new IllegalArgumentException("El usuario ya se ha postulado a esta convocatoria");
        }
        try {
            return convertirARespuesta(postulacionRepository.saveAndFlush(new PostulacionStaff(usuario, convocatoria)));
        } catch (DataIntegrityViolationException excepcion) {
            // La restriccion UNIQUE tambien protege frente a solicitudes simultaneas.
            for (Throwable causa = excepcion; causa != null; causa = causa.getCause()) {
                if (causa instanceof ConstraintViolationException violacion
                        && "uk_postulacion_convocatoria_usuario".equals(violacion.getConstraintName())) {
                    throw new IllegalArgumentException("El usuario ya se ha postulado a esta convocatoria", excepcion);
                }
            }
            throw excepcion;
        }
    }

    @Transactional(readOnly = true)
    public Optional<PostulacionStaffRespuesta> consultar(Long idConvocatoria, String correo) {
        Usuario usuario = buscarUsuarioActivo(correo);
        buscarConvocatoria(idConvocatoria);
        return postulacionRepository.findByConvocatoriaIdAndUsuarioId(idConvocatoria, usuario.getId())
                .map(this::convertirARespuesta);
    }

    @Transactional(readOnly = true)
    public List<PostulacionStaffRespuesta> listarMias(String correo) {
        Usuario usuario = buscarUsuarioActivo(correo);
        return postulacionRepository.findByUsuarioIdOrderByFechaPostulacionDescIdDesc(usuario.getId())
                .stream().map(this::convertirARespuesta).toList();
    }

    @Transactional
    public PostulacionStaffRespuesta retirar(Long id, String correo) {
        Usuario usuario = buscarUsuarioActivo(correo);
        PostulacionStaff postulacion = postulacionRepository.findByIdAndUsuarioId(id, usuario.getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Postulacion no encontrada con id " + id));
        postulacion.retirar();
        return convertirARespuesta(postulacionRepository.saveAndFlush(postulacion));
    }

    private Usuario buscarUsuarioActivo(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new ExcepcionNoAutorizado("Debe autenticarse para gestionar sus postulaciones");
        }
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new ExcepcionNoAutorizado("Usuario autenticado no encontrado"));
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new AccessDeniedException("El usuario debe estar activo");
        }
        return usuario;
    }

    private Convocatoria buscarConvocatoria(Long id) {
        return convocatoriaRepository.findById(id)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Convocatoria no encontrada con id " + id));
    }

    private PostulacionStaffRespuesta convertirARespuesta(PostulacionStaff postulacion) {
        return new PostulacionStaffRespuesta(postulacion.getId(), postulacion.getUsuario().getId(),
                postulacion.getConvocatoria().getId(), postulacion.getEstado(),
                postulacion.getFechaPostulacion(), postulacion.getFechaActualizacion());
    }
}
