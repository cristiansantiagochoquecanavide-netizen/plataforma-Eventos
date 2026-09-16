package com.plataformaeventos.organizaciones.dto;

import com.plataformaeventos.organizaciones.modelo.Organizacion;

public record OrganizacionRespuesta(Long id, String nombre, String slug, String estado) {
    public static OrganizacionRespuesta desde(Organizacion organizacion) {
        return new OrganizacionRespuesta(organizacion.getId(), organizacion.getNombre(), organizacion.getSlug(), organizacion.getEstado());
    }
}
