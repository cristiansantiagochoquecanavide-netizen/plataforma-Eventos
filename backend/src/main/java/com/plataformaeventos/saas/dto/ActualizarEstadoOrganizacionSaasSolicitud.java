package com.plataformaeventos.saas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ActualizarEstadoOrganizacionSaasSolicitud(
        @NotBlank @Pattern(regexp = "ACTIVA|INACTIVA") String estado
) {
}
