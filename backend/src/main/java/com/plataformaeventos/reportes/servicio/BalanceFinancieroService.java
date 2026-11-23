package com.plataformaeventos.reportes.servicio;

import com.plataformaeventos.eventos.modelo.EstadoEvento;
import com.plataformaeventos.eventos.modelo.Evento;
import com.plataformaeventos.eventos.repositorio.EventoRepository;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.repositorio.InscripcionRepository;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.recursos.modelo.AsignacionRecurso;
import com.plataformaeventos.recursos.modelo.EstadoAsignacion;
import com.plataformaeventos.recursos.repositorio.AsignacionRecursoRepository;
import com.plataformaeventos.reportes.dto.EventoFinancieroDTO;
import com.plataformaeventos.reportes.dto.ResumenFinancieroDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class BalanceFinancieroService {

    private final EventoRepository eventoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final AsignacionRecursoRepository asignacionRecursoRepository;

    public BalanceFinancieroService(
            EventoRepository eventoRepository,
            InscripcionRepository inscripcionRepository,
            AsignacionRecursoRepository asignacionRecursoRepository
    ) {
        this.eventoRepository = eventoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.asignacionRecursoRepository = asignacionRecursoRepository;
    }

    public ResumenFinancieroDTO calcularResumenFinanciero(Organizacion organizacion) {
        Long orgId = organizacion.getId();
        List<Evento> eventos = eventoRepository.findByOrganizacionIdAndEstadoNotOrderByFechaInicioAsc(orgId, EstadoEvento.CANCELADO);

        BigDecimal totalIngresosGlobal = BigDecimal.ZERO;
        BigDecimal totalCostosGlobal = BigDecimal.ZERO;
        long gananciaCount = 0;
        long perdidaCount = 0;
        long equilibrioCount = 0;

        List<EventoFinancieroDTO> detalles = new ArrayList<>();

        Map<Long, List<EventoFinancieroDTO.DesgloseEntradaFinancieroDTO>> desglosePorEvento = new HashMap<>();
        List<Object[]> filasDesglose = inscripcionRepository.resumirVentasPorTipoEntradaOrganizacion(orgId);
        if (filasDesglose != null) {
            for (Object[] fila : filasDesglose) {
                if (fila == null || fila.length < 5 || fila[0] == null) continue;
                Long evId = ((Number) fila[0]).longValue();
                String categoria = fila[1] != null ? fila[1].toString() : "General";
                BigDecimal precioUnitario = fila[2] instanceof BigDecimal bd ? bd : new BigDecimal(fila[2].toString());
                long cantidad = fila[3] != null ? ((Number) fila[3]).longValue() : 0L;
                BigDecimal subtotal = fila[4] instanceof BigDecimal bd ? bd : new BigDecimal(fila[4].toString());
                Integer cupoMaximo = (fila.length >= 6 && fila[5] != null) ? ((Number) fila[5]).intValue() : 0;
                desglosePorEvento.computeIfAbsent(evId, k -> new ArrayList<>())
                        .add(new EventoFinancieroDTO.DesgloseEntradaFinancieroDTO(categoria, precioUnitario, cantidad, cupoMaximo, subtotal));
            }
        }

        Map<Long, Integer> asistenciasPorEvento = new HashMap<>();
        List<Object[]> filasAsistencia = inscripcionRepository.contarAsistenciasPorEventoOrganizacion(orgId);
        if (filasAsistencia != null) {
            for (Object[] fila : filasAsistencia) {
                if (fila == null || fila.length < 2 || fila[0] == null) continue;
                Long evId = ((Number) fila[0]).longValue();
                int cant = fila[1] != null ? ((Number) fila[1]).intValue() : 0;
                asistenciasPorEvento.put(evId, cant);
            }
        }

        for (Evento evento : eventos) {
            BigDecimal ingresosEvento = inscripcionRepository.sumMontoPagadoByEventoId(evento.getId());
            if (ingresosEvento == null) {
                ingresosEvento = BigDecimal.ZERO;
            }

            long confirmadas = inscripcionRepository.countByEventoIdAndOrganizacionIdAndEstado(
                    evento.getId(), orgId, EstadoInscripcion.CONFIRMADA
            );

            BigDecimal costoEspacio = BigDecimal.ZERO;
            String nombreEspacio = "Sin recinto asignado";
            if (evento.getEspacio() != null) {
                if (evento.getEspacio().getNombre() != null) {
                    nombreEspacio = evento.getEspacio().getNombre();
                }
                if (evento.getEspacio().getCostoAlquiler() != null) {
                    costoEspacio = evento.getEspacio().getCostoAlquiler();
                }
            }

            BigDecimal costoRecursos = BigDecimal.ZERO;
            List<EventoFinancieroDTO.RecursoAsignadoFinancieroDTO> desgloseRecursos = new ArrayList<>();
            List<AsignacionRecurso> asignaciones = asignacionRecursoRepository.buscarConFiltrosTenant(
                    evento.getId(), null, null, orgId
            );
            if (asignaciones != null) {
                for (AsignacionRecurso asig : asignaciones) {
                    if (asig.getEstado() == EstadoAsignacion.CANCELADA) {
                        continue;
                    }
                    if (asig.getRecurso() != null && asig.getRecurso().getCostoUnitario() != null) {
                        BigDecimal unitario = asig.getRecurso().getCostoUnitario();
                        int cant = asig.getCantidadAsignada() != null ? asig.getCantidadAsignada() : 1;
                        BigDecimal subRecurso = unitario.multiply(BigDecimal.valueOf(cant));
                        costoRecursos = costoRecursos.add(subRecurso);
                        desgloseRecursos.add(new EventoFinancieroDTO.RecursoAsignadoFinancieroDTO(
                                asig.getRecurso().getCodigo() != null ? asig.getRecurso().getCodigo() : "-",
                                asig.getRecurso().getNombre() != null ? asig.getRecurso().getNombre() : "Recurso",
                                asig.getRecurso().getTipo() != null ? asig.getRecurso().getTipo().name() : "GENERAL",
                                cant,
                                unitario,
                                subRecurso
                        ));
                    }
                }
            }

            BigDecimal costosTotales = costoEspacio.add(costoRecursos);
            BigDecimal balanceNeto = ingresosEvento.subtract(costosTotales);

            double roi = 0.0;
            if (costosTotales.compareTo(BigDecimal.ZERO) > 0) {
                roi = balanceNeto.divide(costosTotales, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
            } else if (ingresosEvento.compareTo(BigDecimal.ZERO) > 0) {
                roi = 100.0;
            }

            String situacion;
            if (balanceNeto.compareTo(BigDecimal.ZERO) > 0) {
                situacion = "SUPERAVIT";
                gananciaCount++;
            } else if (balanceNeto.compareTo(BigDecimal.ZERO) < 0) {
                situacion = "DEFICIT";
                perdidaCount++;
            } else {
                situacion = "EQUILIBRIO";
                equilibrioCount++;
            }

            detalles.add(new EventoFinancieroDTO(
                    evento.getId(),
                    evento.getNombre(),
                    evento.getEstado().name(),
                    nombreEspacio,
                    evento.getAforoMaximo() != null ? evento.getAforoMaximo() : 0,
                    (int) confirmadas,
                    asistenciasPorEvento.getOrDefault(evento.getId(), 0),
                    ingresosEvento,
                    costoEspacio,
                    costoRecursos,
                    costosTotales,
                    balanceNeto,
                    roi,
                    situacion,
                    desglosePorEvento.getOrDefault(evento.getId(), List.of()),
                    desgloseRecursos
            ));

            totalIngresosGlobal = totalIngresosGlobal.add(ingresosEvento);
            totalCostosGlobal = totalCostosGlobal.add(costosTotales);
        }

        BigDecimal balanceNetoGlobal = totalIngresosGlobal.subtract(totalCostosGlobal);
        double roiGlobal = 0.0;
        if (totalCostosGlobal.compareTo(BigDecimal.ZERO) > 0) {
            roiGlobal = balanceNetoGlobal.divide(totalCostosGlobal, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        } else if (totalIngresosGlobal.compareTo(BigDecimal.ZERO) > 0) {
            roiGlobal = 100.0;
        }

        return new ResumenFinancieroDTO(
                totalIngresosGlobal,
                totalCostosGlobal,
                balanceNetoGlobal,
                roiGlobal,
                gananciaCount,
                perdidaCount,
                equilibrioCount,
                detalles
        );
    }
}
