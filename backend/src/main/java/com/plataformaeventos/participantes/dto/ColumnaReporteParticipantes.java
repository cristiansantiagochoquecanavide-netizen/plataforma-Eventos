package com.plataformaeventos.participantes.dto;

public enum ColumnaReporteParticipantes {
    ID("ID", ParticipanteResumenRespuesta::idUsuario),
    NOMBRES("Nombres", ParticipanteResumenRespuesta::nombres),
    APELLIDOS("Apellidos", ParticipanteResumenRespuesta::apellidos),
    CORREO("Correo", ParticipanteResumenRespuesta::correo),
    ESTADO("Estado", fila -> fila.estado().name()),
    TOTAL_INSCRIPCIONES("Total inscripciones", ParticipanteResumenRespuesta::totalInscripciones),
    INSCRIPCIONES_CONFIRMADAS("Inscripciones confirmadas", ParticipanteResumenRespuesta::inscripcionesConfirmadas),
    INSCRIPCIONES_CANCELADAS("Inscripciones canceladas", ParticipanteResumenRespuesta::inscripcionesCanceladas),
    ULTIMA_INSCRIPCION("Última inscripción", fila -> ReporteParticipantesSolicitud.formatear(fila.ultimaInscripcion())),
    FECHA_REGISTRO("Fecha de registro", fila -> ReporteParticipantesSolicitud.formatear(fila.fechaRegistro()));

    private final String encabezado;
    private final java.util.function.Function<ParticipanteResumenRespuesta, Object> valor;

    ColumnaReporteParticipantes(String encabezado,
                                java.util.function.Function<ParticipanteResumenRespuesta, Object> valor) {
        this.encabezado = encabezado;
        this.valor = valor;
    }

    public String encabezado() { return encabezado; }

    public String valor(ParticipanteResumenRespuesta fila) {
        Object dato = valor.apply(fila);
        return dato == null ? "" : dato.toString();
    }
}
