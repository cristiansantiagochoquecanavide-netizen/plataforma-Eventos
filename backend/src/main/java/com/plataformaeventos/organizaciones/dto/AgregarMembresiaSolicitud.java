package com.plataformaeventos.organizaciones.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record AgregarMembresiaSolicitud(@NotNull Long idUsuario, Set<String> roles) {
    public AgregarMembresiaSolicitud(Long idUsuario) { this(idUsuario, Set.of("PARTICIPANTE")); }
}
