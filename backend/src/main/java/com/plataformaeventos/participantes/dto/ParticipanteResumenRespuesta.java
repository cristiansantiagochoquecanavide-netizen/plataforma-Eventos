package com.plataformaeventos.participantes.dto;

import com.plataformaeventos.usuarios.modelo.EstadoUsuario;

import java.time.OffsetDateTime;

public record ParticipanteResumenRespuesta(
        Long idUsuario,
        String nombres,
        String apellidos,
        String correo,
        EstadoUsuario estado,
        OffsetDateTime fechaRegistro,
        long totalInscripciones,
        long inscripcionesConfirmadas,
        long inscripcionesCanceladas,
        OffsetDateTime ultimaInscripcion
) {}
