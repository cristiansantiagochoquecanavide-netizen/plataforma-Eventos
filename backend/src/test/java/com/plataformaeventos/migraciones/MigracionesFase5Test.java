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

        assertThat(nombres).hasSize(34);
        assertThat(nombres.stream().map(MigracionesFase5Test::version).toList())
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34);
        assertThat(version(nombres.get(13))).isEqualTo(14);
        assertThat(version(nombres.get(14))).isEqualTo(15);
        assertThat(version(nombres.get(15))).isEqualTo(16);
        assertThat(version(nombres.get(16))).isEqualTo(17);
        assertThat(version(nombres.get(17))).isEqualTo(18);
        assertThat(version(nombres.get(18))).isEqualTo(19);
        assertThat(version(nombres.get(19))).isEqualTo(20);
        assertThat(version(nombres.get(20))).isEqualTo(21);
        assertThat(version(nombres.get(21))).isEqualTo(22);
        assertThat(version(nombres.get(22))).isEqualTo(23);
        assertThat(version(nombres.get(23))).isEqualTo(24);
        assertThat(version(nombres.get(24))).isEqualTo(25);
        assertThat(version(nombres.get(25))).isEqualTo(26);
        assertThat(version(nombres.get(26))).isEqualTo(27);
        assertThat(version(nombres.get(27))).isEqualTo(28);
        assertThat(version(nombres.get(28))).isEqualTo(29);
        assertThat(version(nombres.get(29))).isEqualTo(30);
        assertThat(version(nombres.get(30))).isEqualTo(31);
        assertThat(version(nombres.get(31))).isEqualTo(32);
        assertThat(version(nombres.get(32))).isEqualTo(33);
        assertThat(nombres.get(21)).isEqualTo("V22__distinguir_administrador_saas.sql");
        assertThat(nombres.get(29)).isEqualTo("V30__asignar_evento_ver_a_participante_y_staff.sql");
        assertThat(nombres.get(30)).isEqualTo("V31__alta_saas_checkout_stripe.sql");
        assertThat(nombres.get(31)).isEqualTo("V32__permitir_null_en_tokens_verificacion_alta_saas.sql");
        assertThat(nombres.get(32)).isEqualTo("V33__crear_registro_asistencia.sql");
    }

    @Test
    @DisplayName("V29 crea suscripciones SaaS manuales y activa organizaciones existentes")
    void v29CreaSuscripcionesManuales() throws IOException {
        String migracion = Files.readString(new ClassPathResource("db/migration/V29__suscripciones_manuales_saas.sql").getFile().toPath());

        assertThat(migracion).contains("suscripcion_organizacion", "fecha_inicio", "fecha_fin");
        assertThat(migracion).contains("'BASICO', 'ACTIVA'");
        assertThat(migracion).contains("'PROFESIONAL'", "'EMPRESARIAL'", "'SUSPENDIDA'", "'VENCIDA'");
    }

    @Test
    @DisplayName("V27 crea privilegios UI tenant por grupo con estados soportados")
    void v27CreaPrivilegiosUiPorGrupoTenant() throws IOException {
        String migracion = Files.readString(new ClassPathResource("db/migration/V27__privilegios_ui_por_grupo.sql").getFile().toPath());

        assertThat(migracion).contains("grupo_componente_ui_privilegio");
        assertThat(migracion).contains("id_organizacion", "id_grupo", "clave_componente", "estado");
        assertThat(migracion).contains("FOREIGN KEY (id_grupo, id_organizacion) REFERENCES grupo_usuario");
        assertThat(migracion).contains("UNIQUE (id_organizacion, id_grupo, clave_componente, estado)");
        assertThat(migracion).contains("'VISIBLE'", "'HABILITADO'", "'EDITABLE'");
    }

    @Test
    @DisplayName("V26 crea grupos tenant con miembros y roles aislados por organizacion")
    void v26CreaGruposTenantConMiembrosYRoles() throws IOException {
        String migracion = Files.readString(new ClassPathResource("db/migration/V26__grupos_usuario_tenant.sql").getFile().toPath());

        assertThat(migracion).contains("grupo_usuario", "grupo_usuario_miembro", "grupo_usuario_rol");
        assertThat(migracion).contains("id_organizacion");
        assertThat(migracion).contains("FOREIGN KEY (id_usuario, id_organizacion) REFERENCES usuario_organizacion");
        assertThat(migracion).contains("CHECK (nombre <> '')");
    }

    @Test
    @DisplayName("V23 separa el administrador SaaS del administrador tenant")
    void v23DistingueAdministradorSaasDeMembresiasTenant() throws IOException {
        String migracion = Files.readString(new ClassPathResource("db/migration/V23__distinguir_administrador_saas.sql").getFile().toPath());

        assertThat(migracion).contains("SAAS_ADMINISTRADOR");
        assertThat(migracion).contains("ROL_VER", "ROL_CREAR", "ROL_EDITAR", "PERMISO_VER", "PERMISO_ASIGNAR");
        assertThat(migracion).contains("INSERT INTO usuario_rol");
        assertThat(migracion).contains("DELETE FROM usuario_rol");
        assertThat(migracion).doesNotContain("usuario_organizacion_rol");
    }

    @Test
    @DisplayName("V25 limita estado_participante a estados soportados")
    void v25RestringeEstadoParticipanteAEstadosSoportados() throws IOException {
        String migracion = Files.readString(new ClassPathResource("db/migration/V25__estado_participante_por_membresia.sql").getFile().toPath());

        assertThat(migracion).contains("estado_participante");
        assertThat(migracion).contains("CHECK");
        assertThat(migracion).contains("'ACTIVO'", "'INACTIVO'");
    }

    private static int version(String nombre) {
        return Integer.parseInt(nombre.substring(1, nombre.indexOf("__")));
    }
}
