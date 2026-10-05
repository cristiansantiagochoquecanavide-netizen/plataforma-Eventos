package com.plataformaeventos.audit.cifrado;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.OffsetDateTime;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BitacoraBackfillRunnerTest {
    private EmbeddedDatabase database;

    @AfterEach
    void cerrarBase() {
        if (database != null) {
            database.shutdown();
        }
    }

    @Test
    void noRegistraRunnerCuandoBackfillEstaDeshabilitadoPorDefecto() {
        new ApplicationContextRunner()
                .withUserConfiguration(BackfillConfiguration.class)
                .run(context -> assertThat(context).doesNotHaveBean(BitacoraBackfillRunner.class));
    }

    @Test
    void registraRunnerSoloConOptInExplicito() {
        new ApplicationContextRunner()
                .withUserConfiguration(BackfillConfiguration.class)
                .withPropertyValues(
                        "audit.cifrado.backfill.enabled=true",
                        "audit.cifrado.kms-key-id=kms",
                        "audit.cifrado.kms-indice-key-id=idx-kms",
                        "audit.cifrado.kms-region=us-east-1",
                        "audit.cifrado.proposito=bitacora-payload")
                .run(context -> assertThat(context).hasSingleBean(BitacoraBackfillRunner.class));
    }

    @Test
    void convierteFilasLegacyEnLotesYBorraColumnasSensibles() {
        JdbcTemplate jdbcTemplate = jdbcConEsquema();
        insertarLegacy(jdbcTemplate, 1L, "RESULTADO-SECRETO");
        insertarLegacy(jdbcTemplate, 2L, "OTRO-SECRETO");
        BitacoraCifradoService cifradoService = mock(BitacoraCifradoService.class);
        BitacoraIndiceService indiceService = indiceServiceMock();
        when(cifradoService.cifrar(eq(7L), any(BitacoraPayload.class))).thenAnswer(invocation -> {
            BitacoraPayload payload = invocation.getArgument(1);
            assertThat(payload.actorId()).isEqualTo(99L);
            assertThat(payload.accion()).isEqualTo("CREAR-SECRETO");
            assertThat(payload.modulo()).isEqualTo("MODULO-SECRETO");
            assertThat(payload.entidad()).isEqualTo("ENTIDAD-SECRETA");
            assertThat(payload.entidadId()).isEqualTo(123L);
            assertThat(payload.ip()).isEqualTo("198.51.100.10");
            assertThat(payload.exito()).isTrue();
            return new BitacoraPayloadCifrado("cipher-" + payload.resultado(), "wrapped", "nonce", 1);
        });

        runner(jdbcTemplate, cifradoService, indiceService, 1).run();

        Long restantes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bitacora WHERE payload_ciphertext IS NULL", Long.class);
        assertThat(restantes).isZero();
        Integer sensiblesRestantes = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM bitacora
                WHERE id_actor IS NOT NULL OR accion IS NOT NULL OR modulo IS NOT NULL OR entidad IS NOT NULL
                   OR id_entidad IS NOT NULL OR ip IS NOT NULL OR exito IS NOT NULL OR resultado IS NOT NULL
                """, Integer.class);
        assertThat(sensiblesRestantes).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT id_organizacion FROM bitacora WHERE id_bitacora = 1", Long.class)).isEqualTo(7L);
        assertThat(jdbcTemplate.queryForObject("SELECT fecha FROM bitacora WHERE id_bitacora = 1", String.class)).isNotBlank();
        assertThat(jdbcTemplate.queryForObject("SELECT idx_accion FROM bitacora WHERE id_bitacora = 1", String.class))
                .isEqualTo("idx-accion-CREAR-SECRETO");
    }

    @Test
    void noEscribeValoresSensiblesEnLogsSiFallaUnaFila() {
        JdbcTemplate jdbcTemplate = jdbcConEsquema();
        insertarLegacy(jdbcTemplate, 1L, "RESULTADO-SECRETO");
        BitacoraCifradoService cifradoService = mock(BitacoraCifradoService.class);
        when(cifradoService.cifrar(eq(7L), any(BitacoraPayload.class))).thenThrow(new IllegalStateException("kms unavailable"));
        ListAppender<ILoggingEvent> appender = capturarLogs();

        assertThatThrownBy(() -> runner(jdbcTemplate, cifradoService, indiceServiceMock(), 10).run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kms unavailable");

        String logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList().toString();
        assertThat(logs).contains("row id 1");
        assertThat(logs).doesNotContain("RESULTADO-SECRETO", "CREAR-SECRETO", "MODULO-SECRETO", "198.51.100.10");
    }

    @Test
    void fallaCerradoSinActualizarSiKmsFallaYPermiteReintentoIdempotente() {
        JdbcTemplate jdbcTemplate = jdbcConEsquema();
        insertarLegacy(jdbcTemplate, 1L, "RESULTADO-SECRETO");
        BitacoraCifradoService cifradoFallido = mock(BitacoraCifradoService.class);
        when(cifradoFallido.cifrar(eq(7L), any(BitacoraPayload.class))).thenThrow(new IllegalStateException("kms unavailable"));

        assertThatThrownBy(() -> runner(jdbcTemplate, cifradoFallido, indiceServiceMock(), 10).run())
                .isInstanceOf(IllegalStateException.class);

        assertThat(jdbcTemplate.queryForObject("SELECT payload_ciphertext FROM bitacora WHERE id_bitacora = 1", String.class)).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT resultado FROM bitacora WHERE id_bitacora = 1", String.class))
                .isEqualTo("RESULTADO-SECRETO");

        BitacoraCifradoService cifradoExitoso = mock(BitacoraCifradoService.class);
        when(cifradoExitoso.cifrar(eq(7L), any(BitacoraPayload.class)))
                .thenReturn(new BitacoraPayloadCifrado("cipher", "wrapped", "nonce", 1));
        runner(jdbcTemplate, cifradoExitoso, indiceServiceMock(), 10).run();

        assertThat(jdbcTemplate.queryForObject("SELECT payload_ciphertext FROM bitacora WHERE id_bitacora = 1", String.class))
                .isEqualTo("cipher");
        assertThat(jdbcTemplate.queryForObject("SELECT resultado FROM bitacora WHERE id_bitacora = 1", String.class)).isNull();
    }

    @Test
    void omiteFilasYaConvertidasEnReinicio() {
        JdbcTemplate jdbcTemplate = jdbcConEsquema();
        jdbcTemplate.update("""
                INSERT INTO bitacora (id_bitacora, id_organizacion, fecha, payload_ciphertext, payload_dek_cifrada,
                                      payload_nonce, crypto_version)
                VALUES (1, 7, ?, 'cipher-existing', 'wrapped', 'nonce', 1)
                """, OffsetDateTime.now().toString());
        BitacoraCifradoService cifradoService = mock(BitacoraCifradoService.class);

        runner(jdbcTemplate, cifradoService, indiceServiceMock(), 10).run();

        verify(cifradoService, never()).cifrar(any(), any());
        assertThat(jdbcTemplate.queryForObject("SELECT payload_ciphertext FROM bitacora WHERE id_bitacora = 1", String.class))
                .isEqualTo("cipher-existing");
    }

    private BitacoraBackfillRunner runner(JdbcTemplate jdbcTemplate, BitacoraCifradoService cifradoService,
                                          BitacoraIndiceService indiceService, int batchSize) {
        DataSource dataSource = jdbcTemplate.getDataSource();
        return new BitacoraBackfillRunner(
                jdbcTemplate,
                new DataSourceTransactionManager(dataSource),
                cifradoService,
                indiceService,
                new BitacoraCifradoProperties("kms", "idx-kms", "us-east-1", "bitacora-payload",
                        new BitacoraCifradoProperties.Backfill(true, batchSize))
        );
    }

    private JdbcTemplate jdbcConEsquema() {
        database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(database);
        jdbcTemplate.execute("""
                CREATE TABLE bitacora (
                    id_bitacora BIGINT PRIMARY KEY,
                    id_organizacion BIGINT NOT NULL,
                    id_actor BIGINT,
                    accion VARCHAR(255),
                    modulo VARCHAR(255),
                    entidad VARCHAR(255),
                    id_entidad BIGINT,
                    fecha VARCHAR(64) NOT NULL,
                    ip VARCHAR(64),
                    exito BOOLEAN,
                    resultado VARCHAR(1000),
                    payload_ciphertext CLOB,
                    payload_dek_cifrada CLOB,
                    payload_nonce VARCHAR(64),
                    crypto_version INTEGER,
                    idx_actor VARCHAR(64),
                    idx_ip VARCHAR(64),
                    idx_accion VARCHAR(64),
                    idx_modulo VARCHAR(64),
                    idx_entidad VARCHAR(64),
                    idx_id_entidad VARCHAR(64),
                    idx_exito VARCHAR(64)
                )
                """);
        return jdbcTemplate;
    }

    private void insertarLegacy(JdbcTemplate jdbcTemplate, long id, String resultado) {
        jdbcTemplate.update("""
                INSERT INTO bitacora (id_bitacora, id_organizacion, id_actor, accion, modulo, entidad,
                                      id_entidad, fecha, ip, exito, resultado)
                VALUES (?, 7, 99, 'CREAR-SECRETO', 'MODULO-SECRETO', 'ENTIDAD-SECRETA',
                        123, ?, '198.51.100.10', TRUE, ?)
                """, id, OffsetDateTime.now().toString(), resultado);
    }

    private BitacoraIndiceService indiceServiceMock() {
        BitacoraIndiceService indiceService = mock(BitacoraIndiceService.class);
        when(indiceService.token(eq(7L), any(String.class), any())).thenAnswer(invocation ->
                "idx-" + invocation.getArgument(1, String.class) + "-" + invocation.getArgument(2));
        return indiceService;
    }

    private ListAppender<ILoggingEvent> capturarLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(BitacoraBackfillRunner.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    @Configuration
    @EnableConfigurationProperties(BitacoraCifradoProperties.class)
    @Import(BitacoraBackfillRunner.class)
    static class BackfillConfiguration {
        @Bean
        JdbcTemplate jdbcTemplate() {
            return mock(JdbcTemplate.class);
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return mock(PlatformTransactionManager.class);
        }

        @Bean
        BitacoraCifradoService cifradoService() {
            return mock(BitacoraCifradoService.class);
        }

        @Bean
        BitacoraIndiceService indiceService() {
            return mock(BitacoraIndiceService.class);
        }
    }
}
