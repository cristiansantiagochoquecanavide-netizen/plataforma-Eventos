package com.plataformaeventos.grupos.servicio;

import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.grupos.dto.ActualizarGrupoUsuarioSolicitud;
import com.plataformaeventos.grupos.dto.AsignarMiembrosGrupoSolicitud;
import com.plataformaeventos.grupos.dto.AsignarRolesGrupoSolicitud;
import com.plataformaeventos.grupos.dto.CrearGrupoUsuarioSolicitud;
import com.plataformaeventos.grupos.dto.GrupoUsuarioRespuesta;
import com.plataformaeventos.grupos.dto.RolGrupoRespuesta;
import com.plataformaeventos.grupos.modelo.GrupoUsuario;
import com.plataformaeventos.grupos.repositorio.GrupoUsuarioRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.roles.modelo.EstadoRol;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.usuarios.modelo.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GrupoUsuarioService {
    private static final String ROL_SAAS_ADMINISTRADOR = "SAAS_ADMINISTRADOR";

    private final GrupoUsuarioRepository grupos;
    private final RolRepository roles;

    public GrupoUsuarioService(GrupoUsuarioRepository grupos, RolRepository roles) {
        this.grupos = grupos;
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public List<GrupoUsuarioRespuesta> listar(Organizacion organizacion) {
        return grupos.findByOrganizacionIdOrderByNombreAsc(organizacion.getId()).stream()
                .map(GrupoUsuarioRespuesta::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public GrupoUsuarioRespuesta obtener(Organizacion organizacion, Long idGrupo) {
        return GrupoUsuarioRespuesta.desde(buscarGrupoConDetalle(organizacion, idGrupo));
    }

    @Transactional(readOnly = true)
    public List<RolGrupoRespuesta> listarRolesDisponibles() {
        return roles.findAll().stream()
                .filter(rol -> rol.getEstado() == EstadoRol.ACTIVO)
                .filter(rol -> !ROL_SAAS_ADMINISTRADOR.equals(rol.getNombre()))
                .map(RolGrupoRespuesta::desde)
                .toList();
    }


    @Transactional
    public GrupoUsuarioRespuesta crear(Organizacion organizacion, CrearGrupoUsuarioSolicitud solicitud) {
        String nombre = normalizarNombre(solicitud.nombre());
        validarNombreDisponible(organizacion, nombre);
        GrupoUsuario grupo = new GrupoUsuario(organizacion, nombre, normalizarDescripcion(solicitud.descripcion()));
        return GrupoUsuarioRespuesta.desde(grupos.save(grupo));
    }

    @Transactional
    public GrupoUsuarioRespuesta actualizar(Organizacion organizacion, Long idGrupo, ActualizarGrupoUsuarioSolicitud solicitud) {
        GrupoUsuario grupo = buscarGrupo(organizacion, idGrupo);
        String nombre = normalizarNombre(solicitud.nombre());
        if (!grupo.getNombre().equalsIgnoreCase(nombre)) {
            validarNombreDisponible(organizacion, nombre);
        }
        grupo.actualizarDatos(nombre, normalizarDescripcion(solicitud.descripcion()));
        return GrupoUsuarioRespuesta.desde(grupo);
    }

    @Transactional
    public GrupoUsuarioRespuesta desactivar(Organizacion organizacion, Long idGrupo) {
        GrupoUsuario grupo = buscarGrupo(organizacion, idGrupo);
        grupo.desactivar();
        return GrupoUsuarioRespuesta.desde(grupo);
    }

    @Transactional
    public GrupoUsuarioRespuesta activar(Organizacion organizacion, Long idGrupo) {
        GrupoUsuario grupo = buscarGrupo(organizacion, idGrupo);
        grupo.activar();
        return GrupoUsuarioRespuesta.desde(grupo);
    }

    @Transactional
    public GrupoUsuarioRespuesta asignarMiembros(Organizacion organizacion, Long idGrupo, AsignarMiembrosGrupoSolicitud solicitud) {
        GrupoUsuario grupo = buscarGrupo(organizacion, idGrupo);
        Set<Long> idsUsuarios = idsSolicitados(solicitud.idsUsuarios());
        if (idsUsuarios.isEmpty()) {
            grupo.asignarMiembros(Set.of());
            return GrupoUsuarioRespuesta.desde(grupo);
        }
        List<Usuario> encontrados = grupos.findUsuariosConMembresiaActivaYCuentaActiva(organizacion.getId(), idsUsuarios);
        if (encontrados.size() != idsUsuarios.size()) {
            throw new IllegalArgumentException("Uno o mas miembros no pertenecen activos a la organizacion seleccionada");
        }
        grupo.asignarMiembros(Set.copyOf(encontrados));
        return GrupoUsuarioRespuesta.desde(grupo);
    }

    @Transactional
    public GrupoUsuarioRespuesta asignarRoles(Organizacion organizacion, Long idGrupo, AsignarRolesGrupoSolicitud solicitud) {
        GrupoUsuario grupo = buscarGrupo(organizacion, idGrupo);
        Set<Long> idsRoles = idsSolicitados(solicitud.idsRoles());
        if (idsRoles.isEmpty()) {
            grupo.asignarRoles(Set.of());
            return GrupoUsuarioRespuesta.desde(grupo);
        }
        List<Rol> encontrados = roles.findAllById(idsRoles);
        if (encontrados.size() != idsRoles.size()) {
            throw new IllegalArgumentException("Uno o mas roles no existen");
        }
        if (encontrados.stream().anyMatch(rol -> ROL_SAAS_ADMINISTRADOR.equals(rol.getNombre()))) {
            throw new IllegalArgumentException("SAAS_ADMINISTRADOR no puede asignarse a grupos tenant");
        }
        if (encontrados.stream().anyMatch(rol -> rol.getEstado() != EstadoRol.ACTIVO)) {
            throw new IllegalArgumentException("Uno o mas roles se encuentran inactivos");
        }
        grupo.asignarRoles(Set.copyOf(encontrados));
        return GrupoUsuarioRespuesta.desde(grupo);
    }

    private GrupoUsuario buscarGrupo(Organizacion organizacion, Long idGrupo) {
        return grupos.findByIdAndOrganizacionId(idGrupo, organizacion.getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Grupo no encontrado"));
    }

    private GrupoUsuario buscarGrupoConDetalle(Organizacion organizacion, Long idGrupo) {
        return grupos.findByIdAndOrganizacionIdConMiembrosYRoles(idGrupo, organizacion.getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Grupo no encontrado"));
    }

    private void validarNombreDisponible(Organizacion organizacion, String nombre) {
        if (grupos.existsByOrganizacionIdAndNombreIgnoreCase(organizacion.getId(), nombre)) {
            throw new IllegalArgumentException("Ya existe un grupo con ese nombre en la organizacion");
        }
    }

    private Set<Long> idsSolicitados(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        if (ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("Los identificadores deben ser positivos");
        }
        return new HashSet<>(ids);
    }

    private String normalizarNombre(String nombre) {
        return nombre.trim();
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        return descripcion.trim();
    }
}
