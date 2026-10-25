package com.plataformaeventos.audit.cifrado;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(prefix = "audit.cifrado.backfill", name = "enabled", havingValue = "true")
public class BitacoraBackfillRunner implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(BitacoraBackfillRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final BitacoraCifradoService cifradoService;
    private final BitacoraIndiceService indiceService;
    private final BitacoraCifradoProperties properties;

    public BitacoraBackfillRunner(
            JdbcTemplate jdbcTemplate,
            PlatformTransactionManager transactionManager,
            BitacoraCifradoService cifradoService,
            BitacoraIndiceService indiceService,
            BitacoraCifradoProperties properties
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.cifradoService = cifradoService;
        this.indiceService = indiceService;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        int batchSize = properties.backfillBatchSize();
        boolean reindexIndexes = properties.backfill().reindexIndexes();
        long procesadas = 0;
        long ultimoIdProcesado = Long.MIN_VALUE;
        log.info("Starting bitacora ciphertext backfill with batch size {}; reindex indexes={}", batchSize, reindexIndexes);

        while (true) {
            List<BitacoraLegacyRow> batch = seleccionarLote(batchSize, reindexIndexes, ultimoIdProcesado);
            if (batch.isEmpty()) {
                break;
            }
            for (BitacoraLegacyRow row : batch) {
                procesarFila(row);
                procesadas++;
                ultimoIdProcesado = row.id();
            }
            log.info("Bitacora ciphertext backfill processed {} rows so far", procesadas);
        }

        Long restantes = plaintextRestante();
        if (restantes == null || restantes > 0) {
            throw new IllegalStateException("Bitacora ciphertext backfill finished with remaining plaintext rows: " + restantes);
        }
        log.info("Bitacora ciphertext backfill completed successfully; processed {} rows", procesadas);
    }

    private List<BitacoraLegacyRow> seleccionarLote(int batchSize, boolean reindexIndexes, long ultimoIdProcesado) {
        return jdbcTemplate.query("""
                        SELECT id_bitacora, id_organizacion, id_actor, accion, modulo, entidad,
                               id_entidad, ip, exito, resultado, payload_ciphertext,
                               payload_dek_cifrada, payload_nonce, crypto_version
                        FROM bitacora
                        WHERE id_bitacora > ?
                          AND (payload_ciphertext IS NULL OR crypto_version = 1
                               OR (? = TRUE AND crypto_version = 2))
                        ORDER BY id_bitacora ASC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new BitacoraLegacyRow(
                        rs.getLong("id_bitacora"),
                        rs.getLong("id_organizacion"),
                        rs.getObject("id_actor", Long.class),
                        rs.getString("accion"),
                        rs.getString("modulo"),
                        rs.getString("entidad"),
                        rs.getObject("id_entidad", Long.class),
                        rs.getString("ip"),
                        rs.getObject("exito", Boolean.class),
                        rs.getString("resultado"),
                        rs.getString("payload_ciphertext"),
                        rs.getString("payload_dek_cifrada"),
                        rs.getString("payload_nonce"),
                        rs.getObject("crypto_version", Integer.class)
                ),
                ultimoIdProcesado, reindexIndexes, batchSize);
    }

    private void procesarFila(BitacoraLegacyRow row) {
        try {
            Integer actualizadas = transactionTemplate.execute(status -> convertirFila(row));
            if (actualizadas == null || actualizadas != 1) {
                throw new IllegalStateException("Bitacora row " + row.id() + " was not updated; it may have been converted concurrently");
            }
        } catch (RuntimeException e) {
            log.error("Bitacora ciphertext backfill failed for row id {}", row.id());
            throw e;
        }
    }

    private int convertirFila(BitacoraLegacyRow row) {
        BitacoraPayload payload = row.payloadCiphertext() == null
                ? new BitacoraPayload(row.actorId(), row.accion(), row.modulo(), row.entidad(),
                        row.entidadId(), row.ip(), Boolean.TRUE.equals(row.exito()), row.resultado())
                : cifradoService.descifrar(row.organizacionId(), new BitacoraPayloadCifrado(
                        row.payloadCiphertext(), row.payloadDekCifrada(), row.payloadNonce(), row.cryptoVersion()));
        BitacoraPayloadCifrado envelope = cifradoService.cifrar(row.organizacionId(), payload);

        return jdbcTemplate.update("""
                        UPDATE bitacora
                        SET payload_ciphertext = ?,
                            payload_dek_cifrada = ?,
                            payload_nonce = ?,
                            crypto_version = ?,
                            idx_actor = ?,
                            idx_ip = ?,
                            idx_accion = ?,
                            idx_modulo = ?,
                            idx_entidad = ?,
                            idx_id_entidad = ?,
                            idx_exito = ?,
                            id_actor = NULL,
                            accion = NULL,
                            modulo = NULL,
                            entidad = NULL,
                            id_entidad = NULL,
                            ip = NULL,
                            exito = NULL,
                            resultado = NULL
                        WHERE id_bitacora = ?
                          AND ((payload_ciphertext IS NULL OR crypto_version = 1)
                               OR (? = TRUE AND crypto_version = 2))
                        """,
                envelope.ciphertext(),
                envelope.dataKeyCiphertext(),
                envelope.nonce(),
                envelope.cryptoVersion(),
                indiceService.token(row.organizacionId(), "actor", payload.actorId()),
                indiceService.token(row.organizacionId(), "ip", payload.ip()),
                indiceService.token(row.organizacionId(), "accion", payload.accion()),
                indiceService.token(row.organizacionId(), "modulo", payload.modulo()),
                indiceService.token(row.organizacionId(), "entidad", payload.entidad()),
                indiceService.token(row.organizacionId(), "idEntidad", payload.entidadId()),
                indiceService.token(row.organizacionId(), "exito", payload.exito()),
                row.id(),
                properties.backfill().reindexIndexes());
    }

    private Long plaintextRestante() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM bitacora
                WHERE payload_ciphertext IS NULL OR crypto_version = 1
                """, Long.class);
    }

    record BitacoraLegacyRow(
            Long id,
            Long organizacionId,
            Long actorId,
            String accion,
            String modulo,
            String entidad,
            Long entidadId,
            String ip,
            Boolean exito,
            String resultado,
            String payloadCiphertext,
            String payloadDekCifrada,
            String payloadNonce,
            Integer cryptoVersion
    ) {
    }
}
