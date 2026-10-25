package com.plataformaeventos.audit.cifrado;

public record BitacoraPayloadCifrado(
        String ciphertext,
        String dataKeyCiphertext,
        String nonce,
        int cryptoVersion
) {
}
