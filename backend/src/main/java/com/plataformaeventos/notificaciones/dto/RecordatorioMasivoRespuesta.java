package com.plataformaeventos.notificaciones.dto;

import java.time.OffsetDateTime;

public record RecordatorioMasivoRespuesta(Long idEvento, int destinatarios, OffsetDateTime fechaProgramada) { }
