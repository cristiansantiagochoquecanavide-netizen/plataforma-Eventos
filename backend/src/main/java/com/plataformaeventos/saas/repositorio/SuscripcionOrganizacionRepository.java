package com.plataformaeventos.saas.repositorio;

import com.plataformaeventos.saas.modelo.SuscripcionOrganizacion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SuscripcionOrganizacionRepository extends JpaRepository<SuscripcionOrganizacion, Long> {
    @EntityGraph(attributePaths = "organizacion")
    List<SuscripcionOrganizacion> findAllByOrderByOrganizacionNombreAsc();

    @EntityGraph(attributePaths = "organizacion")
    Optional<SuscripcionOrganizacion> findByOrganizacionId(Long idOrganizacion);
}
