package com.plataformaeventos.eventos.servicio;

import com.plataformaeventos.categorias.dto.CategoriaRespuesta;
import com.plataformaeventos.categorias.modelo.Categoria;
import com.plataformaeventos.categorias.repositorio.CategoriaRepository;
import com.plataformaeventos.espacios.modelo.Espacio;
import com.plataformaeventos.espacios.repositorio.EspacioRepository;
import com.plataformaeventos.eventos.dto.AsignarResponsablesSolicitud;
import com.plataformaeventos.eventos.dto.EspacioEventoRespuesta;
import com.plataformaeventos.eventos.dto.EventoRecomendadoRespuesta;
import com.plataformaeventos.eventos.dto.EventoRespuesta;
import com.plataformaeventos.eventos.dto.GuardarEventoSolicitud;
import com.plataformaeventos.eventos.dto.ResponsableEventoRespuesta;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.excepciones.ExcepcionNoEncontrado;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import com.plataformaeventos.eventos.dto.TipoEntradaDTO;
import com.plataformaeventos.eventos.modelo.TipoEntrada;
import com.plataformaeventos.eventos.repositorio.TipoEntradaRepository;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.notificaciones.servicio.NotificacionEventoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EventoService {

    private static final Set<String> ROLES_RESPONSABLES = Set.of("ORGANIZADOR", "STAFF");

    private final EventoRepository eventoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspacioRepository espacioRepository;
    private final NotificacionEventoService notificaciones;
    private final TipoEntradaRepository tipoEntradaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final RestClient restClient;

    public EventoService(
            EventoRepository eventoRepository,
            CategoriaRepository categoriaRepository,
            UsuarioRepository usuarioRepository,
            EspacioRepository espacioRepository
    ) {
        this(eventoRepository, categoriaRepository, usuarioRepository, espacioRepository, null, null, null, null);
    }

    public EventoService(
            EventoRepository eventoRepository,
            CategoriaRepository categoriaRepository,
            UsuarioRepository usuarioRepository,
            EspacioRepository espacioRepository,
            NotificacionEventoService notificaciones
    ) {
        this(eventoRepository, categoriaRepository, usuarioRepository, espacioRepository, notificaciones, null, null, null);
    }

    @Autowired
    public EventoService(
            EventoRepository eventoRepository,
            CategoriaRepository categoriaRepository,
            UsuarioRepository usuarioRepository,
            EspacioRepository espacioRepository,
            @Autowired(required = false) NotificacionEventoService notificaciones,
            TipoEntradaRepository tipoEntradaRepository,
            @Autowired(required = false) InscripcionRepository inscripcionRepository,
            @Value("${servicio.ia.url:http://servicio-ia:8000}") String servicioIaUrl
    ) {
        this.eventoRepository = eventoRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.espacioRepository = espacioRepository;
        this.notificaciones = notificaciones;
        this.tipoEntradaRepository = tipoEntradaRepository;
        this.inscripcionRepository = inscripcionRepository;
        String url = (servicioIaUrl != null && !servicioIaUrl.isBlank()) ? servicioIaUrl : "http://servicio-ia:8000";
        this.restClient = RestClient.builder().baseUrl(url).build();
    }

    @Transactional(readOnly = true)
    public List<EventoRespuesta> listarEventos(String termino, String categoria) {
        return listarEventos(termino, categoria, null);
    }
    @Transactional(readOnly = true)
    public List<EventoRespuesta> listarEventos(String termino, String categoria, Organizacion organizacion) {
        String texto = termino == null ? "" : termino.trim().toLowerCase();
        String filtroCategoria = categoria == null ? "" : categoria.trim().toLowerCase();
        return (organizacion == null ? eventoRepository.findByEstadoNotOrderByFechaInicioAsc(EstadoEvento.CANCELADO) : eventoRepository.findByOrganizacionIdAndEstadoNotOrderByFechaInicioAsc(organizacion.getId(), EstadoEvento.CANCELADO))
                .stream()
                // Se filtra en el servicio para que web y movil reciban exactamente el mismo resultado.
                .filter(evento -> texto.isEmpty() || evento.getNombre().toLowerCase().contains(texto))
                .filter(evento -> filtroCategoria.isEmpty() || nombreCategoria(evento).toLowerCase().equals(filtroCategoria))
                .map(evento -> convertirEvento(evento, organizacion))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventoRespuesta obtenerEvento(Long idEvento) {
        return obtenerEvento(idEvento, null);
    }
    @Transactional(readOnly = true)
    public EventoRespuesta obtenerEvento(Long idEvento, Organizacion organizacion) {
        return convertirEvento(buscarEvento(idEvento, organizacion), organizacion);
    }

    @Transactional(readOnly = true)
    public List<ResponsableEventoRespuesta> listarResponsablesDisponibles() {
        return listarResponsablesDisponibles(null);
    }
    @Transactional(readOnly = true)
    public List<ResponsableEventoRespuesta> listarResponsablesDisponibles(Organizacion organizacion) {
        return (organizacion == null ? usuarioRepository.findResponsablesDisponibles(ROLES_RESPONSABLES) : usuarioRepository.findResponsablesDisponiblesPorOrganizacion(ROLES_RESPONSABLES, organizacion.getId())).stream()
                .map(usuario -> convertirResponsable(usuario, organizacion))
                .toList();
    }

    @Transactional
    public EventoRespuesta crearEvento(GuardarEventoSolicitud solicitud) {
        return crearEvento(solicitud, null);
    }
    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "EVENTOS", entidad = "EVENTO", accion = "CREAR")
    public EventoRespuesta crearEvento(GuardarEventoSolicitud solicitud, Organizacion organizacion) {
        validarFechas(solicitud.fechaInicio(), solicitud.fechaFin());
        Categoria categoria = buscarCategoria(solicitud.idCategoria(), organizacion);
        Espacio espacio = validarProgramacion(solicitud, null, organizacion);
        Evento evento = new Evento(
                solicitud.nombre().trim(),
                normalizarTexto(solicitud.descripcion()),
                solicitud.fechaInicio(),
                solicitud.fechaFin(),
                normalizarTexto(solicitud.lugarPrincipal()),
                solicitud.estado() == null ? EstadoEvento.PLANIFICADO : solicitud.estado(),
                categoria
        );
        evento.asignarOrganizacion(organizacion);
        evento.configurarProgramacion(espacio, solicitud.aforoMaximo());
        if (solicitud.responsables() != null) {
             evento.asignarResponsables(obtenerResponsablesValidos(solicitud.responsables(), organizacion));
        }
        Evento eventoGuardado = eventoRepository.save(evento);
        guardarTiposEntrada(eventoGuardado, solicitud.tiposEntrada(), organizacion);
        return convertirEvento(eventoGuardado, organizacion);
    }

    @Transactional
    public EventoRespuesta actualizarEvento(Long idEvento, GuardarEventoSolicitud solicitud) {
        return actualizarEvento(idEvento, solicitud, null);
    }
    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "EVENTOS", entidad = "EVENTO", accion = "ACTUALIZAR")
    public EventoRespuesta actualizarEvento(Long idEvento, GuardarEventoSolicitud solicitud, Organizacion organizacion) {
        validarFechas(solicitud.fechaInicio(), solicitud.fechaFin());
        Evento evento = buscarEvento(idEvento, organizacion);
        OffsetDateTime fechaInicioAnterior = evento.getFechaInicio();
        OffsetDateTime fechaFinAnterior = evento.getFechaFin();
        EstadoEvento estadoAnterior = evento.getEstado();
        String ubicacionAnterior = ubicacionEvento(evento);
        Categoria categoria = buscarCategoria(solicitud.idCategoria(), organizacion);
        Espacio espacio = validarProgramacion(solicitud, idEvento, organizacion);
        evento.actualizarDatos(
                solicitud.nombre().trim(),
                normalizarTexto(solicitud.descripcion()),
                solicitud.fechaInicio(),
                solicitud.fechaFin(),
                normalizarTexto(solicitud.lugarPrincipal()),
                solicitud.estado() == null ? evento.getEstado() : solicitud.estado(),
                categoria
        );
        evento.configurarProgramacion(espacio, solicitud.aforoMaximo());
        if (notificaciones != null && organizacion != null) {
            boolean reprogramado = !Objects.equals(fechaInicioAnterior, evento.getFechaInicio())
                    || !Objects.equals(fechaFinAnterior, evento.getFechaFin());
            boolean cambioEspacio = !Objects.equals(ubicacionAnterior, ubicacionEvento(evento));
            boolean cancelado = estadoAnterior != EstadoEvento.CANCELADO
                    && evento.getEstado() == EstadoEvento.CANCELADO;
            if (reprogramado || cambioEspacio || cancelado) {
                notificaciones.notificarCambioEvento(evento, organizacion, reprogramado, cambioEspacio, cancelado);
            }
        }
        if (solicitud.tiposEntrada() != null) {
            guardarTiposEntrada(evento, solicitud.tiposEntrada(), organizacion);
        }
        return convertirEvento(evento, organizacion);
    }

    @Transactional
    public EventoRespuesta asignarResponsables(Long idEvento, AsignarResponsablesSolicitud solicitud) {
        return asignarResponsables(idEvento, solicitud, null);
    }
    @Transactional
    @com.plataformaeventos.audit.servicio.OperacionAuditable(modulo = "EVENTOS", entidad = "EVENTO", accion = "RESPONSABLES")
    public EventoRespuesta asignarResponsables(Long idEvento, AsignarResponsablesSolicitud solicitud, Organizacion organizacion) {
        Evento evento = buscarEvento(idEvento, organizacion);
        evento.asignarResponsables(obtenerResponsablesValidos(solicitud.responsables(), organizacion));
        return convertirEvento(evento, organizacion);
    }

    private Evento buscarEvento(Long idEvento) { return buscarEvento(idEvento, null); }
    private Evento buscarEvento(Long idEvento, Organizacion organizacion) {
        return (organizacion == null ? eventoRepository.findById(idEvento) : eventoRepository.findByIdAndOrganizacionId(idEvento, organizacion.getId()))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Evento no encontrado"));
    }

    private Categoria buscarCategoria(Long idCategoria) {
        if (idCategoria == null) {
            throw new IllegalArgumentException("La categoria del evento es obligatoria");
        }
        return categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Categoria no encontrada"));
    }
    private Categoria buscarCategoria(Long idCategoria, Organizacion organizacion) {
        if (organizacion == null) return buscarCategoria(idCategoria);
        return categoriaRepository.findByIdAndOrganizacionId(idCategoria, organizacion.getId())
                .orElseThrow(() -> new ExcepcionNoEncontrado("Categoria no encontrada"));
    }

    private Set<Usuario> obtenerResponsablesValidos(Set<Long> idsResponsables, Organizacion organizacion) {
        if (idsResponsables == null || idsResponsables.isEmpty()) {
            return Set.of();
        }
        Set<Usuario> responsables = idsResponsables.stream()
                .map(id -> buscarResponsableValido(id, organizacion))
                .collect(Collectors.toSet());
        if (responsables.size() != idsResponsables.size()) {
            throw new IllegalArgumentException("No se puede asignar el mismo responsable mas de una vez");
        }
        if (organizacion == null) {
            boolean hayResponsableInvalido = responsables.stream().anyMatch(usuario ->
                    usuario.getEstado() != EstadoUsuario.ACTIVO || usuario.getRoles().stream()
                            .map(Rol::getNombre)
                            .noneMatch(ROLES_RESPONSABLES::contains)
            );
            if (hayResponsableInvalido) {
                throw new IllegalArgumentException("Los responsables deben estar activos y tener rol ORGANIZADOR o STAFF");
            }
        }
        return responsables;
    }

    private Usuario buscarResponsableValido(Long id, Organizacion organizacion) {
        if (organizacion != null) {
            return usuarioRepository.findResponsableActivoPorOrganizacionYRol(id, organizacion.getId(), ROLES_RESPONSABLES)
                    .orElseThrow(() -> new ExcepcionNoEncontrado("Responsable no encontrado"));
        }
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ExcepcionNoEncontrado("Responsable no encontrado"));
    }

    private void validarFechas(OffsetDateTime fechaInicio, OffsetDateTime fechaFin) {
        if (fechaFin != null && !fechaFin.isAfter(fechaInicio)) {
            throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio");
        }
    }

    private Espacio validarProgramacion(GuardarEventoSolicitud solicitud, Long idEvento, Organizacion organizacion) {
        if (solicitud.idEspacio() == null) {
            throw new IllegalArgumentException("El espacio es obligatorio");
        }
        if (solicitud.aforoMaximo() == null) {
            throw new IllegalArgumentException("El aforo maximo es obligatorio");
        }
        Espacio espacio = (organizacion == null ? espacioRepository.findById(solicitud.idEspacio()) : espacioRepository.findByIdAndOrganizacionId(solicitud.idEspacio(), organizacion.getId()))
                .orElseThrow(() -> new ExcepcionNoEncontrado("Espacio no encontrado"));
        if (solicitud.aforoMaximo() > espacio.getCapacidad()) {
            throw new IllegalArgumentException("El aforo del evento no puede superar la capacidad del espacio");
        }
        if (existeConflictoProgramacion(espacio.getId(), solicitud.fechaInicio(), solicitud.fechaFin(), idEvento, organizacion)) {
            throw new IllegalArgumentException("El espacio ya esta ocupado en el horario seleccionado");
        }
        return espacio;
    }

    private boolean existeConflictoProgramacion(
            Long idEspacio,
            OffsetDateTime fechaInicio,
            OffsetDateTime fechaFin,
            Long idEvento,
            Organizacion organizacion
    ) {
        if (organizacion != null) {
            Long org = organizacion.getId();
            if (idEvento == null) return fechaFin == null
                    ? eventoRepository.existeConflictoProgramacionCrearSinFinTenant(idEspacio, fechaInicio, org)
                    : eventoRepository.existeConflictoProgramacionCrearConFinTenant(idEspacio, fechaInicio, fechaFin, org);
            return fechaFin == null
                    ? eventoRepository.existeConflictoProgramacionEditarSinFinTenant(idEspacio, fechaInicio, idEvento, org)
                    : eventoRepository.existeConflictoProgramacionEditarConFinTenant(idEspacio, fechaInicio, fechaFin, idEvento, org);
        }
        if (idEvento == null) {
            return fechaFin == null
                    ? eventoRepository.existeConflictoProgramacionCrearSinFin(idEspacio, fechaInicio)
                    : eventoRepository.existeConflictoProgramacionCrearConFin(idEspacio, fechaInicio, fechaFin);
        }
        return fechaFin == null
                ? eventoRepository.existeConflictoProgramacionEditarSinFin(idEspacio, fechaInicio, idEvento)
                : eventoRepository.existeConflictoProgramacionEditarConFin(
                        idEspacio, fechaInicio, fechaFin, idEvento
                );
    }

    private String normalizarTexto(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private EventoRespuesta convertirEvento(Evento evento) { return convertirEvento(evento, null); }
    private EventoRespuesta convertirEvento(Evento evento, Organizacion organizacion) {
        CategoriaRespuesta categoria = convertirCategoria(evento.getCategoria());
        List<TipoEntradaDTO> tiposEntrada = List.of();
        if (tipoEntradaRepository != null) {
            tiposEntrada = tipoEntradaRepository.findByEventoIdAndActivoTrueOrderByPrecioAsc(evento.getId())
                    .stream()
                    .map(te -> {
                        long vendidas = inscripcionRepository != null
                                ? inscripcionRepository.countByTipoEntradaIdAndEstado(te.getId(), EstadoInscripcion.CONFIRMADA)
                                : 0;
                        int disp = Math.max(0, (te.getCupoMaximo() != null ? te.getCupoMaximo() : 0) - (int) vendidas);
                        return new TipoEntradaDTO(
                                te.getId(),
                                te.getNombre(),
                                te.getPrecio(),
                                te.getCupoMaximo(),
                                te.getDescripcion(),
                                (int) vendidas,
                                disp
                        );
                    })
                    .toList();
        }
        return new EventoRespuesta(
                evento.getId(),
                evento.getNombre(),
                evento.getDescripcion(),
                evento.getFechaInicio(),
                evento.getFechaFin(),
                evento.getLugarPrincipal(),
                categoria,
                categoria == null ? null : categoria.nombre(),
                convertirEspacio(evento.getEspacio()),
                evento.getAforoMaximo(),
                evento.getEstado().name(),
                evento.getResponsables().stream()
                        .sorted(Comparator.comparing(Usuario::getApellidos).thenComparing(Usuario::getNombres))
                        .map(usuario -> convertirResponsable(usuario, organizacion))
                        .toList(),
                tiposEntrada
        );
    }

    private void guardarTiposEntrada(Evento evento, List<TipoEntradaDTO> dtos, Organizacion organizacion) {
        if (tipoEntradaRepository == null) return;
        if (dtos == null || dtos.isEmpty()) {
            List<TipoEntrada> existentes = tipoEntradaRepository.findByEventoIdAndActivoTrueOrderByPrecioAsc(evento.getId());
            if (existentes.isEmpty()) {
                tipoEntradaRepository.save(new TipoEntrada(
                        organizacion,
                        evento,
                        "General",
                        BigDecimal.ZERO,
                        evento.getAforoMaximo() != null ? evento.getAforoMaximo() : 100,
                        "Entrada General"
                ));
            }
            return;
        }

        java.util.Set<String> nombresVistos = new java.util.HashSet<>();
        java.util.Set<Long> idsConservados = new java.util.HashSet<>();
        int sumaCupos = 0;

        for (TipoEntradaDTO dto : dtos) {
            if (dto.nombre() == null || dto.nombre().isBlank()) {
                throw new IllegalArgumentException("El nombre de cada categoria de entrada es obligatorio");
            }
            String nombreLimpio = dto.nombre().trim();
            if (nombreLimpio.length() > 100) {
                throw new IllegalArgumentException("El nombre de la categoria '" + nombreLimpio + "' no debe superar 100 caracteres");
            }
            if (!nombresVistos.add(nombreLimpio.toLowerCase(java.util.Locale.ROOT))) {
                throw new IllegalArgumentException("No puede haber categorias de entrada duplicadas: '" + nombreLimpio + "'");
            }
            BigDecimal precio = dto.precio() != null ? dto.precio() : BigDecimal.ZERO;
            if (precio.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El precio de la categoria '" + nombreLimpio + "' no puede ser negativo");
            }
            if (dto.cupoMaximo() == null || dto.cupoMaximo() <= 0) {
                throw new IllegalArgumentException("El cupo maximo de la categoria '" + nombreLimpio + "' debe ser mayor que cero");
            }
            sumaCupos += dto.cupoMaximo();
        }

        if (evento.getAforoMaximo() != null && sumaCupos > evento.getAforoMaximo()) {
            throw new IllegalArgumentException(
                    "La suma de cupos de las entradas (" + sumaCupos + ") no puede superar el aforo maximo del evento (" + evento.getAforoMaximo() + ")"
            );
        }

        List<TipoEntrada> existentesActivos = tipoEntradaRepository.findByEventoIdAndActivoTrueOrderByPrecioAsc(evento.getId());

        for (TipoEntradaDTO dto : dtos) {
            String nombreLimpio = dto.nombre().trim();
            BigDecimal precio = dto.precio() != null ? dto.precio() : BigDecimal.ZERO;
            TipoEntrada te = null;
            if (dto.id() != null) {
                te = tipoEntradaRepository.findByIdAndEventoId(dto.id(), evento.getId()).orElse(null);
            }
            if (te == null) {
                te = new TipoEntrada(
                        organizacion,
                        evento,
                        nombreLimpio,
                        precio,
                        dto.cupoMaximo(),
                        normalizarTexto(dto.descripcion())
                );
            } else {
                long vendidas = inscripcionRepository != null
                        ? inscripcionRepository.countByTipoEntradaIdAndEstado(te.getId(), EstadoInscripcion.CONFIRMADA)
                        : 0;
                if (dto.cupoMaximo() < vendidas) {
                    throw new IllegalArgumentException(
                            "El cupo maximo de '" + nombreLimpio + "' (" + dto.cupoMaximo() + ") no puede ser menor a las " + vendidas + " entradas ya vendidas"
                    );
                }
                te.setNombre(nombreLimpio);
                te.setPrecio(precio);
                te.setCupoMaximo(dto.cupoMaximo());
                te.setDescripcion(normalizarTexto(dto.descripcion()));
                te.setActivo(true);
            }
            TipoEntrada guardado = tipoEntradaRepository.save(te);
            if (guardado != null && guardado.getId() != null) {
                idsConservados.add(guardado.getId());
            }
        }

        for (TipoEntrada previo : existentesActivos) {
            if (previo.getId() != null && !idsConservados.contains(previo.getId())) {
                long vendidas = inscripcionRepository != null
                        ? inscripcionRepository.countByTipoEntradaIdAndEstado(previo.getId(), EstadoInscripcion.CONFIRMADA)
                        : 0;
                if (vendidas > 0) {
                    throw new IllegalArgumentException(
                            "No se puede eliminar la categoria '" + previo.getNombre() + "' porque ya tiene " + vendidas + " inscripciones confirmadas"
                    );
                }
                previo.setActivo(false);
                tipoEntradaRepository.save(previo);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<TipoEntradaDTO> listarTiposEntrada(Long idEvento) {
        if (tipoEntradaRepository == null) return List.of();
        return tipoEntradaRepository.findByEventoIdAndActivoTrueOrderByPrecioAsc(idEvento)
                .stream()
                .map(te -> {
                    long vendidas = inscripcionRepository != null
                            ? inscripcionRepository.countByTipoEntradaIdAndEstado(te.getId(), EstadoInscripcion.CONFIRMADA)
                            : 0;
                    int disp = Math.max(0, (te.getCupoMaximo() != null ? te.getCupoMaximo() : 0) - (int) vendidas);
                    return new TipoEntradaDTO(
                            te.getId(),
                            te.getNombre(),
                            te.getPrecio(),
                            te.getCupoMaximo(),
                            te.getDescripcion(),
                            (int) vendidas,
                            disp
                    );
                })
                .toList();
    }

    private String nombreCategoria(Evento evento) {
        Categoria categoria = evento.getCategoria();
        return categoria == null || categoria.getNombre() == null ? "" : categoria.getNombre();
    }

    private CategoriaRespuesta convertirCategoria(Categoria categoria) {
        if (categoria == null) {
            return null;
        }
        return new CategoriaRespuesta(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getFechaCreacion(),
                categoria.getFechaActualizacion()
        );
    }

    private EspacioEventoRespuesta convertirEspacio(Espacio espacio) {
        if (espacio == null) {
            return null;
        }
        return new EspacioEventoRespuesta(
                espacio.getId(),
                espacio.getNombre(),
                espacio.getUbicacion(),
                espacio.getCapacidad()
        );
    }

    private ResponsableEventoRespuesta convertirResponsable(Usuario usuario) { return convertirResponsable(usuario, null); }
    private ResponsableEventoRespuesta convertirResponsable(Usuario usuario, Organizacion organizacion) {
        return new ResponsableEventoRespuesta(
                usuario.getId(),
                usuario.getNombres(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                nombresRoles(usuario, organizacion)
        );
    }

    private List<String> nombresRoles(Usuario usuario, Organizacion organizacion) {
        if (organizacion != null) {
            return usuarioRepository.findNombresRolesActivosPorUsuarioYOrganizacion(usuario.getId(), organizacion.getId());
        }
        return usuario.getRoles().stream()
                .map(Rol::getNombre)
                .sorted()
                .toList();
    }

    private String ubicacionEvento(Evento evento) {
        if (evento.getLugarPrincipal() != null && !evento.getLugarPrincipal().isBlank()) {
            return evento.getLugarPrincipal().trim();
        }
        if (evento.getEspacio() == null) return null;
        return evento.getEspacio().getId() + ":" + evento.getEspacio().getNombre() + ":" + evento.getEspacio().getUbicacion();
    }

    @Transactional(readOnly = true)
    public List<EventoRecomendadoRespuesta> obtenerRecomendacionesIa(String correoUsuario, Organizacion organizacion) {
        if (organizacion == null) {
            return List.of();
        }
        Long orgId = organizacion.getId();
        Usuario usuario = usuarioRepository.findByCorreo(correoUsuario).orElse(null);
        Long usuarioId = usuario != null ? usuario.getId() : null;

        // 1. Obtener eventos de la organizacion que no esten cancelados
        List<Evento> eventosOrg = eventoRepository.findByOrganizacionIdAndEstadoNotOrderByFechaInicioAsc(orgId, EstadoEvento.CANCELADO);
        if (eventosOrg.isEmpty()) {
            return List.of();
        }

        // 2. Extraer historial de participacion del usuario en la organizacion
        List<String> historialCategorias = new ArrayList<>();
        List<String> historialTitulos = new ArrayList<>();
        Set<Long> eventosInscritosIds = new HashSet<>();

        if (usuarioId != null && inscripcionRepository != null) {
            List<com.plataformaeventos.inscripciones.modelo.Inscripcion> inscripciones =
                    inscripcionRepository.findByParticipanteIdAndOrganizacionIdConEvento(usuarioId, orgId);
            for (var ins : inscripciones) {
                if (ins.getEvento() != null) {
                    eventosInscritosIds.add(ins.getEvento().getId());
                    historialTitulos.add(ins.getEvento().getNombre());
                    if (ins.getEvento().getCategoria() != null && ins.getEvento().getCategoria().getNombre() != null) {
                        historialCategorias.add(ins.getEvento().getCategoria().getNombre());
                    }
                }
            }
        }

        // Eventos a los cuales el usuario todavia no se ha inscrito
        List<Evento> disponibles = eventosOrg.stream()
                .filter(e -> !eventosInscritosIds.contains(e.getId()))
                .toList();

        List<Evento> candidatos = disponibles.isEmpty() ? eventosOrg : disponibles;

        // 3. Preparar payload para servicio IA
        List<Map<String, Object>> eventosPayload = candidatos.stream().map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id_evento", e.getId());
            map.put("titulo", e.getNombre());
            map.put("descripcion", e.getDescripcion() != null ? e.getDescripcion() : "");
            map.put("categoria", e.getCategoria() != null ? e.getCategoria().getNombre() : "General");
            return map;
        }).toList();

        Map<String, Object> iaRequest = Map.of(
                "id_usuario", usuarioId != null ? usuarioId : 0L,
                "intereses", List.of(),
                "historial_categorias", historialCategorias.stream().distinct().toList(),
                "historial_eventos_titulos", historialTitulos,
                "eventos_disponibles", eventosPayload
        );

        Map<Long, Double> afinidades = new HashMap<>();
        Map<Long, String> razones = new HashMap<>();
        Map<Long, List<String>> etiquetasMap = new HashMap<>();

        try {
            if (restClient != null) {
                Map<String, Object> respuestaIa = restClient.post()
                        .uri("/api/ia/eventos/recomendar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(iaRequest)
                        .retrieve()
                        .body(Map.class);

                if (respuestaIa != null && respuestaIa.containsKey("recomendaciones")) {
                    List<Map<String, Object>> recs = (List<Map<String, Object>>) respuestaIa.get("recomendaciones");
                    for (Map<String, Object> rec : recs) {
                        Long idEv = ((Number) rec.get("id_evento")).longValue();
                        Double score = rec.get("score") != null ? ((Number) rec.get("score")).doubleValue() : 0.8;
                        String razon = (String) rec.get("razon");
                        List<String> etiqs = (List<String>) rec.getOrDefault("etiquetas", List.of());
                        afinidades.put(idEv, score);
                        razones.put(idEv, razon);
                        etiquetasMap.put(idEv, etiqs);
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("Fallback local IA recomendaciones: " + ex.getMessage());
        }

        // 4. Mapear respuesta DTO ordenada por afinidad
        List<EventoRecomendadoRespuesta> resultado = new ArrayList<>();
        for (Evento e : candidatos) {
            Double afinidad = afinidades.getOrDefault(e.getId(), 0.75);
            String motivo = razones.getOrDefault(e.getId(), "Evento destacado de la organización según tus posibles intereses.");
            List<String> etiqs = etiquetasMap.getOrDefault(e.getId(), List.of("Destacado", "Recomendado"));

            long inscritos = inscripcionRepository != null
                    ? inscripcionRepository.countByEventoIdAndOrganizacionIdAndEstado(e.getId(), orgId, EstadoInscripcion.CONFIRMADA)
                    : 0;

            String nombreCat = e.getCategoria() != null ? e.getCategoria().getNombre() : "General";
            String ubicacion = e.getEspacio() != null ? e.getEspacio().getNombre() : (e.getLugarPrincipal() != null ? e.getLugarPrincipal() : "Por definir");

            resultado.add(new EventoRecomendadoRespuesta(
                    e.getId(),
                    e.getNombre(),
                    e.getDescripcion(),
                    e.getFechaInicio(),
                    e.getFechaFin(),
                    ubicacion,
                    nombreCat,
                    e.getAforoMaximo(),
                    (int) inscritos,
                    afinidad,
                    motivo,
                    etiqs
            ));
        }

        resultado.sort((a, b) -> Double.compare(b.afinidad(), a.afinidad()));
        return resultado;
    }
}
