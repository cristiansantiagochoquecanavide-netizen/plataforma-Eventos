package com.plataformaeventos.saas.dto;

import com.plataformaeventos.saas.modelo.PlanSuscripcion;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearAltaSaasSolicitud(
        @NotBlank @Size(max = 150) String nombreEmpresa,
        @Size(max = 100) @Pattern(regexp = "^$|[a-z0-9]+(?:-[a-z0-9]+)*") String slugSugerido,
        @NotBlank @Size(max = 100) String nombresContacto,
        @NotBlank @Size(max = 100) String apellidosContacto,
        @NotBlank @Email @Size(max = 254) String correoContacto,
        @NotNull PlanSuscripcion plan
) { }
