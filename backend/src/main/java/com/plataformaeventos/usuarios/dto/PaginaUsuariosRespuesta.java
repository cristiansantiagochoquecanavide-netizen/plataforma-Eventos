package com.plataformaeventos.usuarios.dto;

import java.util.List;

public record PaginaUsuariosRespuesta(
        List<UsuarioRespuesta> elementos,
        int pagina,
        int tamano,
        long totalElementos,
        int totalPaginas
) {
}
