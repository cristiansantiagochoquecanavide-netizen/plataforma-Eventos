package com.plataformaeventos.participantes.dto;

import java.util.List;

public record PaginaParticipantesRespuesta(
        List<ParticipanteResumenRespuesta> elementos,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {}
