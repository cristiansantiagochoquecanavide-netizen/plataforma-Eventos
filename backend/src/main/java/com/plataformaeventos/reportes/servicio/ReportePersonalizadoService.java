package com.plataformaeventos.reportes.servicio;

import com.plataformaeventos.convocatorias.repositorio.ConvocatoriaRepository;
import com.plataformaeventos.espacios.repositorio.EspacioRepository;
import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.participantes.repositorio.ParticipanteRepository;
import com.plataformaeventos.recursos.repositorio.RecursoRepository;
import com.plataformaeventos.reportes.dto.ConsultaReporteIaDTO;
import com.plataformaeventos.reportes.dto.ReporteMetricasRespuesta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportePersonalizadoService {

    private final EventoRepository eventoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final ConvocatoriaRepository convocatoriaRepository;
    private final EspacioRepository espacioRepository;
    private final RecursoRepository recursoRepository;
    private final ParticipanteRepository participanteRepository;
    private final BalanceFinancieroService balanceFinancieroService;
    private final RestClient restClient;

    public ReportePersonalizadoService(
            EventoRepository eventoRepository,
            InscripcionRepository inscripcionRepository,
            ConvocatoriaRepository convocatoriaRepository,
            EspacioRepository espacioRepository,
            RecursoRepository recursoRepository,
            ParticipanteRepository participanteRepository,
            BalanceFinancieroService balanceFinancieroService,
            @Value("${servicio.ia.url:http://servicio-ia:8000}") String servicioIaUrl,
            @Value("${servicio.ia.api-key:}") String servicioIaApiKey
    ) {
        this.eventoRepository = eventoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.convocatoriaRepository = convocatoriaRepository;
        this.espacioRepository = espacioRepository;
        this.recursoRepository = recursoRepository;
        this.participanteRepository = participanteRepository;
        this.balanceFinancieroService = balanceFinancieroService;
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(servicioIaUrl)
                .requestFactory(new JdkClientHttpRequestFactory(httpClient));
        if (servicioIaApiKey != null && !servicioIaApiKey.isBlank()) {
            restClientBuilder.defaultHeader("X-Internal-Service-Key", servicioIaApiKey);
        }
        this.restClient = restClientBuilder.build();
    }

    public ReporteMetricasRespuesta obtenerMetricasOrganizacion(
            Organizacion organizacion,
            Integer rangoDias,
            String estadoFiltro,
            String categoriaFiltro
    ) {
        Long orgId = organizacion.getId();
        List<Evento> todosEventos = eventoRepository.findAllByOrderByFechaInicioAsc().stream()
                .filter(e -> e.getOrganizacion() != null && e.getOrganizacion().getId().equals(orgId))
                .toList();

        OffsetDateTime fechaLimite = rangoDias != null ? OffsetDateTime.now().minusDays(rangoDias) : null;

        List<Evento> eventosFiltrados = todosEventos.stream()
                .filter(e -> {
                    if (fechaLimite != null && e.getFechaInicio() != null && e.getFechaInicio().isBefore(fechaLimite)) {
                        return false;
                    }
                    if (estadoFiltro != null && !estadoFiltro.isBlank()) {
                        if (!e.getEstado().name().equalsIgnoreCase(estadoFiltro)) {
                            return false;
                        }
                    }
                    if (categoriaFiltro != null && !categoriaFiltro.isBlank()) {
                        if (e.getCategoria() == null || !e.getCategoria().getNombre().toLowerCase().contains(categoriaFiltro.toLowerCase())) {
                            return false;
                        }
                    }
                    return true;
                })
                .toList();

        long eventosPublicados = eventosFiltrados.stream()
                .filter(e -> e.getEstado() == EstadoEvento.PUBLICADO)
                .count();

        long eventosFinalizados = eventosFiltrados.stream()
                .filter(e -> e.getEstado() == EstadoEvento.FINALIZADO)
                .count();

        List<ReporteMetricasRespuesta.EventoMetricaItem> detalleEventos = new ArrayList<>();
        long totalInscritosOrg = 0;
        long totalInscritosConfirmadosOrg = 0;
        long totalCapacidad = 0;

        for (Evento ev : eventosFiltrados) {
            long inscritosEvento = inscripcionRepository.countByEventoIdAndOrganizacionIdAndEstado(
                    ev.getId(), orgId, EstadoInscripcion.CONFIRMADA
            );
            totalInscritosConfirmadosOrg += inscritosEvento;
            totalInscritosOrg += inscritosEvento;

            int capacidad = ev.getAforoMaximo() != null ? ev.getAforoMaximo() : 0;
            totalCapacidad += capacidad;

            double porcentaje = capacidad > 0 ? Math.min(100.0, Math.round(((double) inscritosEvento / capacidad) * 1000.0) / 10.0) : 0.0;

            detalleEventos.add(new ReporteMetricasRespuesta.EventoMetricaItem(
                    ev.getId(),
                    ev.getNombre(),
                    ev.getCategoria() != null ? ev.getCategoria().getNombre() : "Sin categoria",
                    ev.getEstado().name(),
                    ev.getFechaInicio() != null ? ev.getFechaInicio().format(DateTimeFormatter.ISO_LOCAL_DATE) : "",
                    capacidad,
                    inscritosEvento,
                    porcentaje
            ));
        }

        double tasaOcupacion = totalCapacidad > 0
                ? Math.round(((double) totalInscritosConfirmadosOrg / totalCapacidad) * 1000.0) / 10.0
                : 0.0;

        Map<String, Long> agrupadoPorCategoria = eventosFiltrados.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategoria() != null ? e.getCategoria().getNombre() : "General",
                        Collectors.counting()
                ));

        List<ReporteMetricasRespuesta.CategoriaMetricaItem> distribucionCategorias = agrupadoPorCategoria.entrySet().stream()
                .map(entry -> new ReporteMetricasRespuesta.CategoriaMetricaItem(entry.getKey(), entry.getValue()))
                .sorted((a, b) -> Long.compare(b.cantidadEventos(), a.cantidadEventos()))
                .toList();

        Map<String, Long> inscripcionesPorMes = new LinkedHashMap<>();
        for (ReporteMetricasRespuesta.EventoMetricaItem item : detalleEventos) {
            if (item.fechaInicio() != null && item.fechaInicio().length() >= 7) {
                String mes = item.fechaInicio().substring(0, 7);
                inscripcionesPorMes.put(mes, inscripcionesPorMes.getOrDefault(mes, 0L) + item.inscritos());
            }
        }

        return new ReporteMetricasRespuesta(
                eventosFiltrados.size(),
                eventosPublicados,
                eventosFinalizados,
                totalInscritosOrg,
                totalInscritosConfirmadosOrg,
                totalCapacidad,
                tasaOcupacion,
                detalleEventos,
                distribucionCategorias,
                inscripcionesPorMes
        );
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> generarReporteEjecutivoIa(Organizacion organizacion, String consulta) {
        Long orgId = organizacion.getId();
        ReporteMetricasRespuesta metricas = obtenerMetricasOrganizacion(organizacion, null, null, null);

        var paginaParticipantes = participanteRepository.buscarResumenParaReporte(
                orgId, null, "", null, PageRequest.of(0, 100)
        );
        List<Map<String, Object>> participantes = paginaParticipantes.getContent().stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("idUsuario", p.idUsuario());
            m.put("nombres", p.nombres());
            m.put("apellidos", p.apellidos());
            m.put("correo", p.correo());
            m.put("estado", p.estado() != null ? p.estado().name() : "ACTIVO");
            m.put("totalInscripciones", p.totalInscripciones());
            m.put("inscripcionesConfirmadas", p.inscripcionesConfirmadas());
            m.put("inscripcionesCanceladas", p.inscripcionesCanceladas());
            return m;
        }).toList();

        List<Map<String, Object>> convocatorias = convocatoriaRepository
                .buscarConFiltrosTenant(null, null, null, "", orgId, Sort.by("fechaLimite").ascending())
                .stream().map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("evento", c.getEvento() != null ? c.getEvento().getNombre() : "-");
                    m.put("tipoPersonal", c.getTipoPersonal() != null ? c.getTipoPersonal().name() : "-");
                    m.put("estado", c.getEstado() != null ? c.getEstado().name() : "-");
                    m.put("cantidadRequerida", c.getCantidadRequerida());
                    m.put("fechaLimite", c.getFechaLimite() != null ? c.getFechaLimite().toString() : "-");
                    return m;
                }).toList();

        List<Map<String, Object>> recursos = recursoRepository
                .buscarConFiltrosTenant("", null, null, orgId, Sort.by("nombre").ascending())
                .stream().map(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", r.getId());
                    m.put("codigo", r.getCodigo());
                    m.put("nombre", r.getNombre());
                    m.put("tipo", r.getTipo() != null ? r.getTipo().name() : "-");
                    m.put("cantidadTotal", r.getCantidadTotal());
                    m.put("cantidadDisponible", r.getCantidadDisponible());
                    m.put("estado", r.getEstado() != null ? r.getEstado().name() : "-");
                    m.put("ubicacion", r.getUbicacion() != null ? r.getUbicacion() : "-");
                    m.put("costoUnitario", r.getCostoUnitario());
                    return m;
                }).toList();

        List<Map<String, Object>> espacios = espacioRepository
                .findByOrganizacionIdOrderByNombreAsc(orgId)
                .stream().map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", e.getId());
                    m.put("nombre", e.getNombre());
                    m.put("ubicacion", e.getUbicacion());
                    m.put("capacidad", e.getCapacidad());
                    m.put("costoAlquiler", e.getCostoAlquiler());
                    return m;
                }).toList();

        var resumenFinanzas = balanceFinancieroService.calcularResumenFinanciero(organizacion);

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("totalEventos", metricas.totalEventos());
        resumen.put("eventosPublicados", metricas.eventosPublicados());
        resumen.put("totalInscripciones", metricas.totalInscripciones());
        resumen.put("tasaOcupacionPromedio", metricas.tasaOcupacionPromedio());
        resumen.put("totalParticipantes", paginaParticipantes.getTotalElements());
        resumen.put("totalConvocatorias", convocatorias.size());
        resumen.put("totalRecursos", recursos.size());
        resumen.put("totalEspacios", espacios.size());
        resumen.put("ingresosTotales", resumenFinanzas.ingresosTotales());
        resumen.put("costosTotales", resumenFinanzas.costosTotales());
        resumen.put("balanceNetoGlobal", resumenFinanzas.balanceNetoGlobal());
        resumen.put("roiGlobal", resumenFinanzas.roiGlobal());

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("organizacion", organizacion.getNombre());
        snapshot.put("resumen", resumen);
        snapshot.put("finanzas", resumenFinanzas);
        snapshot.put("eventos", metricas.detalleEventos());
        snapshot.put("participantes", participantes);
        snapshot.put("convocatorias", convocatorias);
        snapshot.put("recursos", recursos);
        snapshot.put("espacios", espacios);

        try {
            return restClient.post()
                    .uri("/api/ia/reportes/generar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "consulta", consulta != null ? consulta : "",
                            "datos_organizacion", snapshot
                    ))
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            System.err.println("Error llamando a servicio-ia /generar: " + e.getMessage());
            return construirReporteRespaldo(organizacion.getNombre(), consulta, metricas);
        }
    }

    private Map<String, Object> construirReporteRespaldo(String nombreOrg, String consulta, ReporteMetricasRespuesta metricas) {
        List<List<String>> filas = metricas.detalleEventos().stream()
                .map(ev -> List.of(
                        ev.nombre(),
                        ev.categoria(),
                        ev.estado(),
                        ev.fechaInicio(),
                        String.valueOf(ev.aforoMaximo()),
                        String.valueOf(ev.inscritos()),
                        ev.porcentajeLlenado() + "%"
                ))
                .toList();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("titulo", "Informe Ejecutivo de Eventos y Ocupacion");
        resp.put("subtitulo", "Solicitud: \"" + (consulta != null ? consulta : "Reporte general") + "\" | " + nombreOrg);
        resp.put("tipo_reporte", "EVENTOS");
        resp.put("periodo", "Datos vigentes");
        resp.put("resumen_ejecutivo", "Se consolidaron " + metricas.totalEventos() + " eventos en " + nombreOrg
                + " con " + metricas.totalInscripciones() + " inscripciones confirmadas y una ocupacion promedio de "
                + metricas.tasaOcupacionPromedio() + "%.");
        resp.put("kpis", List.of(
                Map.of("etiqueta", "Eventos", "valor", String.valueOf(metricas.totalEventos()), "detalle", metricas.eventosPublicados() + " publicados"),
                Map.of("etiqueta", "Inscripciones", "valor", String.valueOf(metricas.totalInscripciones()), "detalle", "Confirmadas"),
                Map.of("etiqueta", "Ocupacion", "valor", metricas.tasaOcupacionPromedio() + "%", "detalle", "Promedio general")
        ));
        resp.put("columnas", List.of("Evento", "Categoria", "Estado", "Fecha Inicio", "Aforo", "Inscritos", "Ocupacion"));
        resp.put("filas", filas);
        resp.put("conclusiones", List.of(
                "Total de eventos analizados: " + metricas.totalEventos(),
                "Capacidad acumulada disponible: " + metricas.capacidadTotalEspacios() + " plazas"
        ));
        resp.put("recomendaciones", List.of(
                "Revisar los eventos con menor porcentaje de ocupacion para reforzar su difusion."
        ));
        resp.put("grafico_titulo", "Inscritos por Evento");
        resp.put("grafico_datos", metricas.detalleEventos().stream().limit(8).map(ev -> Map.of(
                "etiqueta", ev.nombre(),
                "valor", ev.inscritos(),
                "extra", ev.porcentajeLlenado() + "%"
        )).toList());
        return resp;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> interpretarConsultaIa(ConsultaReporteIaDTO solicitud) {
        try {
            return restClient.post()
                    .uri("/api/ia/reportes/interpretar")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "consulta", solicitud.consulta(),
                            "contexto_adicional", solicitud.contextoFiltros() != null ? solicitud.contextoFiltros() : Map.of()
                    ))
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            System.err.println("Error llamando a servicio-ia /interpretar: " + e.getMessage());
            return Map.of(
                    "intencion", "Interpretacion estandar",
                    "parametros", Map.of("tipo_metrica", "GENERAL", "tipo_grafico_recomendado", "BAR_CHART"),
                    "mensaje_asistente", "Configurando reporte con los filtros indicados."
            );
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> generarInsightsIa(String consulta, ReporteMetricasRespuesta metricas) {
        try {
            return restClient.post()
                    .uri("/api/ia/reportes/insights")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "consulta_original", consulta,
                            "datos_reporte", metricas
                    ))
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            System.err.println("Error llamando a servicio-ia /insights: " + e.getMessage());
            return Map.of(
                    "resumen_ejecutivo", "Resumen generado para los eventos de la organizacion.",
                    "puntos_clave", List.of("Total eventos: " + metricas.totalEventos(), "Inscripciones: " + metricas.totalInscripciones()),
                    "recomendaciones", List.of("Monitorear el porcentaje de ocupacion de las salas.")
            );
        }
    }
}
