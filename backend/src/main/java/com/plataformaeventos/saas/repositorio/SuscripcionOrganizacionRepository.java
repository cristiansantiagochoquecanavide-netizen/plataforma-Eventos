package com.plataformaeventos.saas.repositorio;

import com.plataformaeventos.saas.modelo.SuscripcionOrganizacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SuscripcionOrganizacionRepository extends JpaRepository<SuscripcionOrganizacion, Long> {
    @EntityGraph(attributePaths = "organizacion")
    List<SuscripcionOrganizacion> findAllByOrderByOrganizacionNombreAsc();

    @EntityGraph(attributePaths = "organizacion")
    Optional<SuscripcionOrganizacion> findByOrganizacionId(Long idOrganizacion);

    Optional<SuscripcionOrganizacion> findByStripeSubscriptionId(String stripeSubscriptionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SuscripcionOrganizacion s where s.stripeSubscriptionId = :subscriptionId")
    Optional<SuscripcionOrganizacion> buscarPorStripeSubscriptionIdParaActualizar(
            @Param("subscriptionId") String subscriptionId);
}
