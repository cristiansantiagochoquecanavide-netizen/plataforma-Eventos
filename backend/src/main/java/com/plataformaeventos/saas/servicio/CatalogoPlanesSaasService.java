package com.plataformaeventos.saas.servicio;

import com.plataformaeventos.saas.dto.PlanSaasPublico;
import com.plataformaeventos.saas.modelo.PlanSuscripcion;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CatalogoPlanesSaasService {
    private static final List<String> FORMATOS_REPORTE = List.of("CSV", "XLSX", "HTML", "PDF", "CORREO");

    public List<PlanSaasPublico> listarPublicos() {
        return List.of(
                new PlanSaasPublico(PlanSuscripcion.BASICO.name(), "Básico", BigDecimal.ZERO, true,
                        5, 3, 300, FORMATOS_REPORTE),
                new PlanSaasPublico(PlanSuscripcion.PROFESIONAL.name(), "Profesional", new BigDecimal("399.00"), false,
                        25, 20, 3_000, FORMATOS_REPORTE),
                new PlanSaasPublico(PlanSuscripcion.EMPRESARIAL.name(), "Empresarial", new BigDecimal("1299.00"), false,
                        100, 100, 15_000, FORMATOS_REPORTE)
        );
    }

    public PlanSaasPublico obtener(PlanSuscripcion plan) {
        return listarPublicos().stream().filter(publico -> publico.codigo().equals(plan.name())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El plan SaaS no está disponible"));
    }

    public long importeMinor(PlanSuscripcion plan) {
        return obtener(plan).precioMensualBob().movePointRight(2).longValueExact();
    }
}
