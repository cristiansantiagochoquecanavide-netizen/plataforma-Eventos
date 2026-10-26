package com.plataformaeventos.asistencias.dto;

import java.time.OffsetDateTime;
import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;

public record InscripcionElegibleAsistenciaRespuesta(Long idInscripcion, String nombres, String apellidos,
        String correo, Long idEvento, String tituloEvento, EstadoInscripcion estado, boolean asistenciaRegistrada,
        OffsetDateTime fechaAsistencia) {
}
