package com.plataformaeventos.audit.dto;

import com.plataformaeventos.audit.modelo.Bitacora;
import java.time.OffsetDateTime;

public record BitacoraRespuesta(Long id, Long organizacion, Long actor, String accion, String modulo,
                                 String entidad, Long idEntidad, OffsetDateTime fecha, String ip,
                                 boolean exito, String resultado) {
    public static BitacoraRespuesta desde(Bitacora b) {
        return new BitacoraRespuesta(b.getId(), b.getOrganizacion().getId(),
                b.getActor() == null ? null : b.getActor().getId(), b.getAccion(), b.getModulo(),
                b.getEntidad(), b.getIdEntidad(), b.getFecha(), b.getIp(), b.isExito(), b.getResultado());
    }
}
