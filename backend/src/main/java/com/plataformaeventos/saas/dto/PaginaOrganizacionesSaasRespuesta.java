package com.plataformaeventos.saas.dto;

import java.util.List;

public record PaginaOrganizacionesSaasRespuesta(
        List<OrganizacionSaasRespuesta> organizaciones,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {
}
