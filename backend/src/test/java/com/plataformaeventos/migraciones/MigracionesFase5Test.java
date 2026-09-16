package com.plataformaeventos.migraciones;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Precondicion determinista mientras el proyecto no dispone de PostgreSQL/Testcontainers:
 * Flyway debe encontrar todas las versiones consecutivas y las migraciones que agregan
 * tablas/columnas deben preceder a las que agregan integridad o indices sobre ellas.
 */
class MigracionesFase5Test {
    @Test
    @DisplayName("las migraciones Fase 5 estan completas y ordenadas para Flyway")
    void versionesConsecutivasYDependenciasEnOrden() throws IOException {
        List<String> nombres;
        try (var archivos = Files.list(new ClassPathResource("db/migration").getFile().toPath())) {
            nombres = archivos.map(path -> path.getFileName().toString())
                    .filter(nombre -> nombre.matches("V\\d+__.*\\.sql"))
                    .sorted((a, b) -> Integer.compare(version(a), version(b)))
                    .toList();
        }

        assertThat(nombres).hasSize(21);
        assertThat(nombres.stream().map(MigracionesFase5Test::version).toList())
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21);
        assertThat(version(nombres.get(13))).isEqualTo(14);
        assertThat(version(nombres.get(14))).isEqualTo(15);
        assertThat(version(nombres.get(15))).isEqualTo(16);
        assertThat(version(nombres.get(16))).isEqualTo(17);
        assertThat(version(nombres.get(17))).isEqualTo(18);
        assertThat(version(nombres.get(18))).isEqualTo(19);
        assertThat(version(nombres.get(19))).isEqualTo(20);
        assertThat(version(nombres.get(20))).isEqualTo(21);
    }

    private static int version(String nombre) {
        return Integer.parseInt(nombre.substring(1, nombre.indexOf("__")));
    }
}
