package com.plataformaeventos.audit.cifrado;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.KmsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BitacoraCifradoServiceTest {
    private static final byte[] DATA_KEY = new byte[32];
    private static final byte[] DATA_KEY_CIFRADA = "wrapped-data-key".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    static {
        Arrays.fill(DATA_KEY, (byte) 7);
    }

    @Test
    void cifraYDescifraConLaLlaveRuntimeDelDesarrolladorSinLlamarKmsParaPayload() {
        KmsClient kmsClient = mock(KmsClient.class);
        BitacoraCifradoService service = servicio(kmsClient);
        BitacoraPayload payload = payload();

        BitacoraPayloadCifrado envelope = service.cifrar(42L, payload);
        BitacoraPayload descifrado = service.descifrar(42L, envelope);

        assertThat(descifrado).isEqualTo(payload);
        assertThat(envelope.cryptoVersion()).isEqualTo(2);
        assertThat(envelope.ciphertext()).doesNotContain(payload.resultado());
        assertThat(envelope.dataKeyCiphertext()).isNull();
        org.mockito.Mockito.verifyNoInteractions(kmsClient);
    }

    @Test
    void rechazaDescifradoConTenantDistintoAlContextoOriginal() {
        BitacoraCifradoService service = servicio(kmsMockQueAceptaTenant("42"));
        BitacoraPayloadCifrado envelope = service.cifrar(42L, payload());

        assertThatThrownBy(() -> service.descifrar(99L, envelope))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se pudo descifrar");
    }

    @Test
    void rechazaCiphertextManipulado() {
        BitacoraCifradoService service = servicio(kmsMockQueAceptaTenant("42"));
        BitacoraPayloadCifrado envelope = service.cifrar(42L, payload());
        byte[] manipulado = Base64.getDecoder().decode(envelope.ciphertext());
        manipulado[0] = (byte) (manipulado[0] ^ 1);
        BitacoraPayloadCifrado envelopeManipulado = new BitacoraPayloadCifrado(
                Base64.getEncoder().encodeToString(manipulado),
                envelope.dataKeyCiphertext(),
                envelope.nonce(),
                envelope.cryptoVersion()
        );

        assertThatThrownBy(() -> service.descifrar(42L, envelopeManipulado))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se pudo descifrar");
    }

    @Test
    void descifraEnvelopesKmsVersionUnoMientrasHayDatosHeredados() throws Exception {
        KmsClient kmsClient = mock(KmsClient.class);
        when(kmsClient.decrypt(any(DecryptRequest.class))).thenReturn(DecryptResponse.builder()
                .plaintext(SdkBytes.fromByteArray(DATA_KEY)).build());
        BitacoraCifradoService service = servicio(kmsClient);
        byte[] nonce = new byte[12];
        byte[] aad = "tenantId=42;purpose=bitacora-payload;version=1".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] texto = new ObjectMapper().writeValueAsBytes(payload());
        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(DATA_KEY, "AES"), new GCMParameterSpec(128, nonce));
        aes.updateAAD(aad);
        byte[] ciphertext = aes.doFinal(texto);
        BitacoraPayloadCifrado legado = new BitacoraPayloadCifrado(
                Base64.getEncoder().encodeToString(ciphertext),
                Base64.getEncoder().encodeToString(DATA_KEY_CIFRADA),
                Base64.getEncoder().encodeToString(nonce), 1);

        assertThat(service.descifrar(42L, legado)).isEqualTo(payload());
        org.mockito.Mockito.verify(kmsClient).decrypt(any(DecryptRequest.class));
    }

    @Test
    void fallaCerradoCuandoNoHayLlaveRuntimeDelDesarrollador() {
        BitacoraCifradoProperties sinLlave = new BitacoraCifradoProperties(
                "legacy-kms", "us-east-1", "bitacora-payload", null, null);
        BitacoraCifradoService service = new BitacoraCifradoService(
                mock(KmsClient.class), new ObjectMapper(), sinLlave, new SecureRandom());

        assertThatThrownBy(() -> service.cifrar(42L, payload()))
                .isInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage("Falta la llave runtime de cifrado de bitacora del desarrollador");
    }

    @Test
    void ocultaLaLlaveDeDesarrolladorEnRepresentacionDeConfiguracion() {
        BitacoraCifradoProperties properties = new BitacoraCifradoProperties(
                "legacy-kms", "us-east-1", "bitacora-payload", null,
                Base64.getEncoder().encodeToString(DATA_KEY));

        assertThat(properties.toString()).doesNotContain(Base64.getEncoder().encodeToString(DATA_KEY));
        assertThat(properties.toString()).contains("<redactada>");
    }

    private BitacoraCifradoService servicio(KmsClient kmsClient) {
        return new BitacoraCifradoService(
                kmsClient,
                new ObjectMapper(),
                new BitacoraCifradoProperties("arn:aws:kms:us-east-1:111122223333:key/test",
                        "us-east-1", "bitacora-payload", null,
                        Base64.getEncoder().encodeToString(DATA_KEY)),
                new SecureRandom()
        );
    }

    private KmsClient kmsMockQueAceptaTenant(String tenantEsperado) {
        KmsClient kmsClient = mock(KmsClient.class);
        when(kmsClient.decrypt(any(DecryptRequest.class))).thenAnswer(invocation -> {
            DecryptRequest request = invocation.getArgument(0);
            if (!tenantEsperado.equals(request.encryptionContext().get("tenantId"))) {
                throw KmsException.builder().message("invalid encryption context").build();
            }
            assertThat(request.encryptionContext()).containsEntry("purpose", "bitacora-payload");
            return DecryptResponse.builder()
                    .plaintext(SdkBytes.fromByteArray(DATA_KEY))
                    .build();
        });
        return kmsClient;
    }

    private BitacoraPayload payload() {
        return new BitacoraPayload(
                10L,
                "CREAR",
                "EVENTOS",
                "EVENTO",
                55L,
                "203.0.113.10",
                true,
                "Evento creado con datos sensibles"
        );
    }
}
