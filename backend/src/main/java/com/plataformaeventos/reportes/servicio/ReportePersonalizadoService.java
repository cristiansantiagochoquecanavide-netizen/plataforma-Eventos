package com.plataformaeventos.reportes.servicio;

import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.reportes.dto.ConsultaReporteIaDTO;
import com.plataformaeventos.reportes.dto.ReporteMetricasRespuesta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportePersonalizadoService {

    private final EventoRepository eventoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final RestClient restClient;

    public ReportePersonalizadoService(
            EventoRepository eventoRepository,
            InscripcionRepository inscripcionRepository,
            @Value("${servicio.ia.url:http://servicio-ia:8000}") String servicioIaUrl
    ) {
        this.eventoRepository = eventoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.restClient = RestClient.builder().baseUrl(servicioIaUrl).build();
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
                    ev.getCategoria() != null ? ev.getCategoria().getNombre() : "Sin categoría",
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
                    "intencion", "Interpretación estándar",
                    "parametros", Map.of("tipo_metrica", "GENERAL", "tipo_grafico_recomendado", "BAR_CHART"),
                    "mensaje_asistente", "Configurando reporte con los filtros indicados."
            );
        }
    }

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
                    "resumen_ejecutivo", "Resumen generado para los eventos de la organización.",
                    "puntos_clave", List.of("Total eventos: " + metricas.totalEventos(), "Inscripciones: " + metricas.totalInscripciones()),
                    "recomendaciones", List.of("Monitorear el porcentaje de ocupación de las salas.")
            );
        }
    }
}
