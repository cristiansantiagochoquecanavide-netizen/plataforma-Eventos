package com.plataformaeventos.participantes.dto;

import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record ReporteParticipantesSolicitud(
        Long eventoId,
        @Size(max = 100, message = "La búsqueda no puede superar 100 caracteres") String busqueda,
        EstadoUsuario estado,
        @NotEmpty(message = "Selecciona al menos una columna")
        @Size(max = 10, message = "El reporte admite hasta 10 columnas")
        List<ColumnaReporteParticipantes> columnas,
        ColumnaReporteParticipantes ordenarPor,
        boolean ascendente
) {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm XXX");

    public static String formatear(OffsetDateTime fecha) {
        return fecha == null ? "" : FECHA.format(fecha);
    }
}
