package com.plataformaeventos.convocatorias.dto;

import com.plataformaeventos.convocatorias.modelo.TipoPersonal;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CrearConvocatoriaSolicitud(
        @NotNull(message = "El evento es obligatorio")
        @Positive(message = "El identificador del evento debe ser valido")
        Long idEvento,

        @NotNull(message = "El tipo de personal es obligatorio")
        TipoPersonal tipoPersonal,

        @NotBlank(message = "La descripcion es obligatoria")
        @Size(max = 1000, message = "La descripcion no debe superar 1000 caracteres")
        String descripcion,

        @NotNull(message = "La cantidad requerida es obligatoria")
        @Positive(message = "La cantidad requerida debe ser mayor que cero")
        Integer cantidadRequerida,

        @NotNull(message = "La fecha limite es obligatoria")
        @FutureOrPresent(message = "La fecha limite no puede estar en el pasado")
        LocalDate fechaLimite
) {
}
