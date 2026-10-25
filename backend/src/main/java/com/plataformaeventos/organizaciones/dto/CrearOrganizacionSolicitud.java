package com.plataformaeventos.organizaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearOrganizacionSolicitud(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(max = 100)
        @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "El slug debe usar minusculas, numeros y guiones")
        String slug
) {
}
