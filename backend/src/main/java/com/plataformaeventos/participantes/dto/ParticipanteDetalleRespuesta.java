package com.plataformaeventos.participantes.dto;

import com.plataformaeventos.usuarios.modelo.EstadoUsuario;

import java.time.OffsetDateTime;
import java.util.List;

public record ParticipanteDetalleRespuesta(
        Long idUsuario,
        String nombres,
        String apellidos,
        String correo,
        EstadoUsuario estado,
        OffsetDateTime fechaRegistro,
        long totalInscripciones,
        long inscripcionesConfirmadas,
        long inscripcionesCanceladas,
        List<InscripcionResumenRespuesta> inscripciones
) {}
