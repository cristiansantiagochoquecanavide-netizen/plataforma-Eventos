package com.plataformaeventos.audit.cifrado;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.GenerateMacRequest;
import software.amazon.awssdk.services.kms.model.MacAlgorithmSpec;

@Service
public class BitacoraIndiceService {
    private static final String DOMINIO = "bitacora-blind-index-v1";

    private final KmsClient kmsClient;
    private final BitacoraCifradoProperties properties;

    public BitacoraIndiceService(KmsClient kmsClient, BitacoraCifradoProperties properties) {
        this.kmsClient = kmsClient;
        this.properties = properties;
    }

    public String token(Long tenantId, String campo, Object valor) {
        if (valor == null) return null;
        if (tenantId == null) throw new IllegalStateException("Tenant requerido para indice de bitacora");
        if (campo == null || campo.isBlank()) throw new IllegalStateException("Campo requerido para indice de bitacora");

        String normalizado = normalizar(valor);
        if (normalizado == null || normalizado.isBlank()) return null;
        String indexKey = properties == null ? null : properties.kmsIndiceKeyId();
        if (indexKey == null || indexKey.isBlank()) {
            throw new IllegalStateException("Configuracion de indice KMS heredado de bitacora incompleta");
        }
        byte[] mensaje = mensaje(tenantId, campo, normalizado);
        try {
            var response = kmsClient.generateMac(GenerateMacRequest.builder()
                    .keyId(indexKey)
                    .macAlgorithm(MacAlgorithmSpec.HMAC_SHA_256)
                    .message(SdkBytes.fromByteArray(mensaje))
                    .build());
            return HexFormat.of().formatHex(response.mac().asByteArray());
        } finally {
            java.util.Arrays.fill(mensaje, (byte) 0);
        }
    }

    public String normalizar(Object valor) {
        return valor == null ? null : valor.toString().trim().toLowerCase(java.util.Locale.ROOT);
    }

    private byte[] mensaje(Long tenantId, String campo, String normalizado) {
        return (DOMINIO + "\ntenant=" + tenantId + "\nfield=" + campo.trim().toLowerCase(java.util.Locale.ROOT)
                + "\nvalue=" + normalizado).getBytes(StandardCharsets.UTF_8);
    }
}
