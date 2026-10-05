package com.plataformaeventos.audit.cifrado;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;

@Service
public class BitacoraCifradoService {
    private static final int VERSION_ACTUAL = 1;
    private static final int NONCE_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final String ALGORITMO = "AES/GCM/NoPadding";

    private final KmsClient kmsClient;
    private final ObjectMapper objectMapper;
    private final BitacoraCifradoProperties properties;
    private final SecureRandom secureRandom;

    public BitacoraCifradoService(
            KmsClient kmsClient,
            ObjectMapper objectMapper,
            BitacoraCifradoProperties properties
    ) {
        this(kmsClient, objectMapper, properties, new SecureRandom());
    }

    BitacoraCifradoService(
            KmsClient kmsClient,
            ObjectMapper objectMapper,
            BitacoraCifradoProperties properties,
            SecureRandom secureRandom
    ) {
        this.kmsClient = kmsClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.secureRandom = secureRandom;
    }

    public BitacoraPayloadCifrado cifrar(Long tenantId, BitacoraPayload payload) {
        validarTenant(tenantId);
        validarConfiguracion();

        byte[] dataKey = null;
        try {
            var dataKeyResponse = kmsClient.generateDataKey(GenerateDataKeyRequest.builder()
                    .keyId(properties.kmsKeyId())
                    .keySpec(DataKeySpec.AES_256)
                    .encryptionContext(contextoKms(tenantId))
                    .build());

            dataKey = dataKeyResponse.plaintext().asByteArray();
            byte[] nonce = nonce();
            byte[] plaintext = serializar(payload);
            try {
                byte[] ciphertext = cifrarAesGcm(dataKey, nonce, datosAutenticados(tenantId), plaintext);
                return new BitacoraPayloadCifrado(
                        Base64.getEncoder().encodeToString(ciphertext),
                        Base64.getEncoder().encodeToString(dataKeyResponse.ciphertextBlob().asByteArray()),
                        Base64.getEncoder().encodeToString(nonce),
                        VERSION_ACTUAL
                );
            } finally {
                Arrays.fill(plaintext, (byte) 0);
            }
        } catch (RuntimeException | GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo cifrar el payload de bitacora", e);
        } finally {
            limpiar(dataKey);
        }
    }

    public BitacoraPayload descifrar(Long tenantId, BitacoraPayloadCifrado envelope) {
        validarTenant(tenantId);
        validarConfiguracion();
        validarEnvelope(envelope);

        byte[] dataKey = null;
        byte[] plaintext = null;
        try {
            var decryptResponse = kmsClient.decrypt(DecryptRequest.builder()
                    .ciphertextBlob(SdkBytes.fromByteArray(Base64.getDecoder().decode(envelope.dataKeyCiphertext())))
                    .encryptionContext(contextoKms(tenantId))
                    .build());

            dataKey = decryptResponse.plaintext().asByteArray();
            plaintext = descifrarAesGcm(
                    dataKey,
                    Base64.getDecoder().decode(envelope.nonce()),
                    datosAutenticados(tenantId),
                    Base64.getDecoder().decode(envelope.ciphertext())
            );
            return objectMapper.readValue(plaintext, BitacoraPayload.class);
        } catch (RuntimeException | GeneralSecurityException | java.io.IOException e) {
            throw new IllegalStateException("No se pudo descifrar el payload de bitacora", e);
        } finally {
            limpiar(plaintext);
            limpiar(dataKey);
        }
    }

    private byte[] serializar(BitacoraPayload payload) {
        try {
            return objectMapper.writeValueAsBytes(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el payload de bitacora", e);
        }
    }

    private byte[] cifrarAesGcm(byte[] dataKey, byte[] nonce, byte[] aad, byte[] plaintext)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITMO);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(dataKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
        cipher.updateAAD(aad);
        return cipher.doFinal(plaintext);
    }

    private byte[] descifrarAesGcm(byte[] dataKey, byte[] nonce, byte[] aad, byte[] ciphertext)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(ALGORITMO);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(dataKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
        cipher.updateAAD(aad);
        return cipher.doFinal(ciphertext);
    }

    private Map<String, String> contextoKms(Long tenantId) {
        return Map.of(
                "tenantId", tenantId.toString(),
                "purpose", properties.proposito()
        );
    }

    private byte[] datosAutenticados(Long tenantId) {
        return ("tenantId=" + tenantId + ";purpose=" + properties.proposito() + ";version=" + VERSION_ACTUAL)
                .getBytes(StandardCharsets.UTF_8);
    }

    private byte[] nonce() {
        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        return nonce;
    }

    private void validarEnvelope(BitacoraPayloadCifrado envelope) {
        if (envelope == null || envelope.cryptoVersion() != VERSION_ACTUAL
                || sinTexto(envelope.ciphertext()) || sinTexto(envelope.dataKeyCiphertext()) || sinTexto(envelope.nonce())) {
            throw new IllegalStateException("Envelope de bitacora cifrada invalido");
        }
    }

    private void validarTenant(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalStateException("Tenant requerido para cifrado de bitacora");
        }
    }

    private void validarConfiguracion() {
        if (properties == null || sinTexto(properties.kmsKeyId()) || sinTexto(properties.kmsRegion()) || sinTexto(properties.proposito())) {
            throw new IllegalStateException("Configuracion de cifrado de bitacora incompleta");
        }
    }

    private boolean sinTexto(String valor) {
        return valor == null || valor.isBlank();
    }

    private void limpiar(byte[] bytes) {
        if (bytes != null) {
            Arrays.fill(bytes, (byte) 0);
        }
    }
}
