package com.plataformaeventos.saas.dto;

import jakarta.validation.constraints.NotBlank;

public record VerificarAltaSaasSolicitud(@NotBlank String token) { }
