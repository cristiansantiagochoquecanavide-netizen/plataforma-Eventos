package com.plataformaeventos.audit.cifrado;

/**
 * Sensitive audit details that must be encrypted before persistence.
 * Technical identifiers such as audit row id, tenant id and timestamp stay outside this payload.
 */
public record BitacoraPayload(
        Long actorId,
        String accion,
        String modulo,
        String entidad,
        Long entidadId,
        String ip,
        boolean exito,
        String resultado
) {
}
