package com.plataformaeventos.audit.cifrado;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import software.amazon.awssdk.regions.Region;

@ConfigurationProperties(prefix = "audit.cifrado")
public record BitacoraCifradoProperties(
        String kmsKeyId,
        String kmsIndiceKeyId,
        String kmsRegion,
        String proposito,
        Backfill backfill,
        String claveDesarrolladorBase64
) {
    private static final int DEFAULT_BACKFILL_BATCH_SIZE = 500;

    @ConstructorBinding
    public BitacoraCifradoProperties {
        if (backfill == null) {
            backfill = new Backfill(false, DEFAULT_BACKFILL_BATCH_SIZE);
        }
    }

    public BitacoraCifradoProperties(String kmsKeyId, String kmsRegion, String proposito) {
        this(kmsKeyId, null, kmsRegion, proposito, null, null);
    }

    public BitacoraCifradoProperties(String kmsKeyId, String kmsRegion, String proposito, Backfill backfill) {
        this(kmsKeyId, null, kmsRegion, proposito, backfill, null);
    }

    public BitacoraCifradoProperties(String kmsKeyId, String kmsRegion, String proposito,
                                     Backfill backfill, String claveDesarrolladorBase64) {
        this(kmsKeyId, null, kmsRegion, proposito, backfill, claveDesarrolladorBase64);
    }

    public BitacoraCifradoProperties(String kmsKeyId, String kmsIndiceKeyId,
                                     String kmsRegion, String proposito) {
        this(kmsKeyId, kmsIndiceKeyId, kmsRegion, proposito, null, null);
    }

    public Region configuredRegionOrDefault() {
        if (kmsRegion == null || kmsRegion.isBlank()) {
            return Region.US_EAST_1;
        }
        return Region.of(kmsRegion);
    }

    public int backfillBatchSize() {
        return backfill.batchSizeOrDefault(DEFAULT_BACKFILL_BATCH_SIZE);
    }

    @Override
    public String toString() {
        return "BitacoraCifradoProperties[kmsKeyId=" + kmsKeyId
                + ", kmsIndiceKeyId=" + kmsIndiceKeyId
                + ", kmsRegion=" + kmsRegion
                + ", proposito=" + proposito
                + ", backfill=" + backfill
                + ", claveDesarrolladorBase64=<redactada>]";
    }

    public record Backfill(boolean enabled, Integer batchSize, boolean reindexIndexes) {
        public Backfill(boolean enabled, Integer batchSize) {
            this(enabled, batchSize, false);
        }

        int batchSizeOrDefault(int defaultValue) {
            if (batchSize == null || batchSize < 1) {
                return defaultValue;
            }
            return batchSize;
        }
    }
}
