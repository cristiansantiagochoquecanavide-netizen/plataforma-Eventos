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
        Backfill backfill
) {
    private static final int DEFAULT_BACKFILL_BATCH_SIZE = 500;

    @ConstructorBinding
    public BitacoraCifradoProperties {
        if (backfill == null) {
            backfill = new Backfill(false, DEFAULT_BACKFILL_BATCH_SIZE);
        }
    }

    public BitacoraCifradoProperties(String kmsKeyId, String kmsRegion, String proposito) {
        this(kmsKeyId, null, kmsRegion, proposito, null);
    }

    public BitacoraCifradoProperties(String kmsKeyId, String kmsIndiceKeyId, String kmsRegion, String proposito) {
        this(kmsKeyId, kmsIndiceKeyId, kmsRegion, proposito, null);
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

    public record Backfill(boolean enabled, Integer batchSize) {
        int batchSizeOrDefault(int defaultValue) {
            if (batchSize == null || batchSize < 1) {
                return defaultValue;
            }
            return batchSize;
        }
    }
}
