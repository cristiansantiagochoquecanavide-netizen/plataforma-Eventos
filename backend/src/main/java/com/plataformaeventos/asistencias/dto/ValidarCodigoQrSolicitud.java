package com.plataformaeventos.asistencias.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ValidarCodigoQrSolicitud(
        @NotBlank @Size(max = 255) String codigoQr
) {}
