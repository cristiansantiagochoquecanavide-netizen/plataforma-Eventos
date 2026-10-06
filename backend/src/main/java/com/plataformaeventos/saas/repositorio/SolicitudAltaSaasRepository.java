package com.plataformaeventos.saas.repositorio;

import com.plataformaeventos.saas.modelo.SolicitudAltaSaas;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.Collection;
import java.util.UUID;
import com.plataformaeventos.saas.modelo.EstadoSolicitudAltaSaas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudAltaSaasRepository extends JpaRepository<SolicitudAltaSaas, UUID> {
    Optional<SolicitudAltaSaas> findByTokenVerificacionHash(String tokenVerificacionHash);
    Optional<SolicitudAltaSaas> findByTokenActivacionHash(String tokenActivacionHash);
    Optional<SolicitudAltaSaas> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);
    Optional<SolicitudAltaSaas> findFirstByCorreoContactoIgnoreCaseAndEstadoInOrderByFechaCreacionDesc(
            String correoContacto, Collection<EstadoSolicitudAltaSaas> estados);
    boolean existsByCorreoContactoIgnoreCaseAndEstadoIn(String correoContacto, Collection<EstadoSolicitudAltaSaas> estados);
    Optional<SolicitudAltaSaas> findByStripeSubscriptionId(String stripeSubscriptionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitudAltaSaas s where s.id = :id")
    Optional<SolicitudAltaSaas> buscarPorIdParaActualizar(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitudAltaSaas s where s.tokenVerificacionHash = :token")
    Optional<SolicitudAltaSaas> buscarPorTokenVerificacionParaActualizar(@Param("token") String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitudAltaSaas s where s.tokenActivacionHash = :token")
    Optional<SolicitudAltaSaas> buscarPorTokenActivacionParaActualizar(@Param("token") String token);
}
