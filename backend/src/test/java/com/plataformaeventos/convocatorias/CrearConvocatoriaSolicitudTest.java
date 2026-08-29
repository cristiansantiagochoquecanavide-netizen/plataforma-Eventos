package com.plataformaeventos.convocatorias;

import com.plataformaeventos.convocatorias.dto.CrearConvocatoriaSolicitud;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CrearConvocatoriaSolicitudTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rechazaCamposObligatoriosInvalidos() {
        CrearConvocatoriaSolicitud solicitud = new CrearConvocatoriaSolicitud(
                null,
                null,
                "   ",
                0,
                LocalDate.now().minusDays(1)
        );

        assertThat(validator.validate(solicitud))
                .extracting(violacion -> violacion.getPropertyPath().toString())
                .contains("idEvento", "tipoPersonal", "descripcion", "cantidadRequerida", "fechaLimite");
    }

    @Test
    void aceptaSolicitudCompleta() {
        CrearConvocatoriaSolicitud solicitud = new CrearConvocatoriaSolicitud(
                1L,
                TipoPersonal.STAFF,
                "Apoyo en registro y acreditacion",
                5,
                LocalDate.now().plusDays(1)
        );

        assertThat(validator.validate(solicitud)).isEmpty();
    }
}
