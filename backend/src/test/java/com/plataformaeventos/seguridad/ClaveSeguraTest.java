package com.plataformaeventos.seguridad;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ClaveSeguraTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void aceptaClavesConMayusculaMinusculaMinimoOchoCaracteresYDosNumeros() {
        assertThat(validar("Admin1234")).isEmpty();
        assertThat(validar("Evento2026A")).isEmpty();
        assertThat(validar("Kevin2026")).isEmpty();
    }

    @Test
    void rechazaClavesQueNoCumplenLaPolitica() {
        assertThat(validar("admin1234")).contains(ClaveSegura.MENSAJE);
        assertThat(validar("Adminabcd")).contains(ClaveSegura.MENSAJE);
        assertThat(validar("ADMIN1234")).contains(ClaveSegura.MENSAJE);
        assertThat(validar("A1b")).contains(ClaveSegura.MENSAJE);
    }

    private Set<String> validar(String clave) {
        return validador.validate(new SolicitudPrueba(clave)).stream()
                .map(violacion -> violacion.getMessage())
                .collect(Collectors.toSet());
    }

    private record SolicitudPrueba(@ClaveSegura String clave) {
    }
}
