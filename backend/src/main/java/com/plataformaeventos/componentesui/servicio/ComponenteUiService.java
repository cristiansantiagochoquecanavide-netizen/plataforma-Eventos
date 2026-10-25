package com.plataformaeventos.componentesui.servicio;

import com.plataformaeventos.componentesui.dto.DefinicionComponenteUiRespuesta;
import com.plataformaeventos.componentesui.dto.EstadoComponenteEfectivoRespuesta;
import com.plataformaeventos.componentesui.dto.GuardarPrivilegiosGrupoSolicitud;
import com.plataformaeventos.componentesui.dto.PrivilegioComponenteRespuesta;
import com.plataformaeventos.componentesui.dto.PrivilegioComponenteSolicitud;
import com.plataformaeventos.componentesui.dto.PrivilegiosEfectivosRespuesta;
import com.plataformaeventos.componentesui.dto.PrivilegiosGrupoRespuesta;
import com.plataformaeventos.componentesui.modelo.EstadoPrivilegioUi;
import com.plataformaeventos.componentesui.modelo.GrupoComponenteUiPrivilegio;
import com.plataformaeventos.componentesui.repositorio.ComponenteUiPrivilegioRepository;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.grupos.modelo.GrupoUsuario;
import com.plataformaeventos.grupos.repositorio.GrupoUsuarioRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ComponenteUiService {
    private final ComponenteUiRegistry registry;
    private final GrupoUsuarioRepository grupos;
    private final ComponenteUiPrivilegioRepository privilegios;

    public ComponenteUiService(ComponenteUiRegistry registry, GrupoUsuarioRepository grupos, ComponenteUiPrivilegioRepository privilegios) {
        this.registry = registry;
        this.grupos = grupos;
        this.privilegios = privilegios;
    }

    @Transactional(readOnly = true)
    public List<DefinicionComponenteUiRespuesta> catalogo() {
        return registry.listar().stream()
                .sorted(Comparator.comparing(DefinicionComponenteUi::clave))
                .map(definicion -> new DefinicionComponenteUiRespuesta(
                        definicion.clave(), definicion.tipo(), definicion.descripcion(), definicion.estadosSoportados(), definicion.controlRecuperacion()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PrivilegiosGrupoRespuesta privilegiosGrupo(Organizacion organizacion, Long idGrupo) {
        validarGrupoTenant(organizacion, idGrupo);
        return respuestaGrupo(idGrupo, privilegios.findByOrganizacionIdAndGrupoId(organizacion.getId(), idGrupo));
    }

    @Transactional
    public PrivilegiosGrupoRespuesta guardarPrivilegiosGrupo(Organizacion organizacion, Long idGrupo, GuardarPrivilegiosGrupoSolicitud solicitud) {
        GrupoUsuario grupo = validarGrupoTenant(organizacion, idGrupo);
        List<GrupoComponenteUiPrivilegio> nuevos = normalizar(solicitud).stream()
                .flatMap(privilegio -> privilegio.estados().stream()
                        .map(estado -> new GrupoComponenteUiPrivilegio(organizacion, grupo, privilegio.claveComponente().trim(), estado)))
                .toList();
        privilegios.deleteByOrganizacionIdAndGrupoId(organizacion.getId(), idGrupo);
        Iterable<GrupoComponenteUiPrivilegio> guardados = privilegios.saveAll(nuevos);
        List<GrupoComponenteUiPrivilegio> lista = new ArrayList<>();
        guardados.forEach(lista::add);
        return respuestaGrupo(idGrupo, lista);
    }

    @Transactional(readOnly = true)
    public PrivilegiosEfectivosRespuesta privilegiosEfectivos(Organizacion organizacion, String correo, boolean administradorTenant) {
        return privilegiosEfectivos(organizacion, correo, administradorTenant, Set.of(), Set.of());
    }

    @Transactional(readOnly = true)
    public PrivilegiosEfectivosRespuesta privilegiosEfectivos(Organizacion organizacion, String correo,
                                                               boolean administradorTenant, Set<String> permisos,
                                                               Set<String> rolesGlobales) {
        Set<String> autoridades = new HashSet<>(permisos == null ? Set.of() : permisos);
        autoridades.addAll(rolesGlobales == null ? Set.of() : rolesGlobales);
        Map<String, EnumSet<EstadoPrivilegioUi>> efectivos = new LinkedHashMap<>();
        registry.listar().stream()
                .sorted(Comparator.comparing(DefinicionComponenteUi::clave))
                .forEach(definicion -> {
                    EnumSet<EstadoPrivilegioUi> estados = EnumSet.noneOf(EstadoPrivilegioUi.class);
                    Set<String> requeridos = registry.permisosBase(definicion.clave());
                    boolean perfilAutenticado = "profile.menu".equals(definicion.clave());
                    boolean accesoBase = perfilAutenticado
                            || (administradorTenant && definicion.controlRecuperacion())
                            || (requeridos.contains("__ADMINISTRADOR_TENANT__") && administradorTenant)
                            || requeridos.stream().anyMatch(autoridades::contains);
                    if (accesoBase) estados.addAll(definicion.estadosSoportados());
                    efectivos.put(definicion.clave(), estados);
                });

        Map<String, EnumSet<EstadoPrivilegioUi>> configurados = new LinkedHashMap<>();
        for (GrupoComponenteUiPrivilegio grant : privilegios.findActivosPorUsuarioYOrganizacion(correo, organizacion.getId())) {
            registry.buscar(grant.getClaveComponente())
                    .filter(definicion -> definicion.estadosSoportados().contains(grant.getEstado()))
                    .ifPresent(definicion -> configurados
                            .computeIfAbsent(definicion.clave(), ignorado -> EnumSet.noneOf(EstadoPrivilegioUi.class))
                            .add(grant.getEstado()));
        }

        configurados.forEach((clave, estadosConfigurados) -> efectivos.get(clave).retainAll(estadosConfigurados));

        Map<String, EstadoComponenteEfectivoRespuesta> respuesta = new LinkedHashMap<>();
        efectivos.forEach((clave, estados) -> respuesta.put(clave, new EstadoComponenteEfectivoRespuesta(Set.copyOf(estados))));
        return new PrivilegiosEfectivosRespuesta(respuesta);
    }

    private GrupoUsuario validarGrupoTenant(Organizacion organizacion, Long idGrupo) {
        return grupos.findByIdAndOrganizacionId(idGrupo, organizacion.getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Grupo no encontrado"));
    }

    private Set<PrivilegioComponenteSolicitud> normalizar(GuardarPrivilegiosGrupoSolicitud solicitud) {
        if (solicitud == null || solicitud.privilegios() == null) {
            return Set.of();
        }
        for (PrivilegioComponenteSolicitud privilegio : solicitud.privilegios()) {
            validar(privilegio);
        }
        return solicitud.privilegios();
    }

    private void validar(PrivilegioComponenteSolicitud privilegio) {
        if (privilegio == null || privilegio.claveComponente() == null || privilegio.claveComponente().isBlank()) {
            throw new IllegalArgumentException("La clave de componente es obligatoria");
        }
        DefinicionComponenteUi definicion = registry.buscar(privilegio.claveComponente().trim())
                .orElseThrow(() -> new IllegalArgumentException("Componente UI no registrado: " + privilegio.claveComponente()));
        if (privilegio.estados() == null || privilegio.estados().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos un estado para " + privilegio.claveComponente());
        }
        for (EstadoPrivilegioUi estado : privilegio.estados()) {
            if (estado == null || !definicion.estadosSoportados().contains(estado)) {
                throw new IllegalArgumentException("El componente " + definicion.clave() + " no soporta el estado " + estado);
            }
        }
        if ((privilegio.estados().contains(EstadoPrivilegioUi.HABILITADO)
                || privilegio.estados().contains(EstadoPrivilegioUi.EDITABLE))
                && !privilegio.estados().contains(EstadoPrivilegioUi.VISIBLE)) {
            throw new IllegalArgumentException("El estado VISIBLE es obligatorio para conceder HABILITADO o EDITABLE en " + definicion.clave());
        }
    }

    private PrivilegiosGrupoRespuesta respuestaGrupo(Long idGrupo, List<GrupoComponenteUiPrivilegio> grants) {
        Map<String, Set<EstadoPrivilegioUi>> agrupados = grants.stream()
                .filter(this::grantRegistradoYSoportado)
                .collect(Collectors.groupingBy(GrupoComponenteUiPrivilegio::getClaveComponente, TreeMap::new,
                        Collectors.mapping(GrupoComponenteUiPrivilegio::getEstado, Collectors.toSet())));
        List<PrivilegioComponenteRespuesta> respuesta = agrupados.entrySet().stream()
                .map(entry -> new PrivilegioComponenteRespuesta(entry.getKey(), entry.getValue()))
                .toList();
        return new PrivilegiosGrupoRespuesta(idGrupo, respuesta);
    }

    private boolean grantRegistradoYSoportado(GrupoComponenteUiPrivilegio grant) {
        return registry.buscar(grant.getClaveComponente())
                .map(definicion -> definicion.estadosSoportados().contains(grant.getEstado()))
                .orElse(false);
    }
}
