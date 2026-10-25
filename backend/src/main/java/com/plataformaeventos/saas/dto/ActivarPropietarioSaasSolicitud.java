package com.plataformaeventos.saas.dto;

import com.plataformaeventos.seguridad.ClaveSegura;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivarPropietarioSaasSolicitud(
        @NotBlank String token,
        @NotBlank @Size(max = 72) @ClaveSegura String nuevaClave
) { }
