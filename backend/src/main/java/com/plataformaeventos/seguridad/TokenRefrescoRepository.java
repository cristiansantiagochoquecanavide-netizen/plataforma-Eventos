package com.plataformaeventos.seguridad;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface TokenRefrescoRepository extends JpaRepository<TokenRefresco, Long> {

    @EntityGraph(attributePaths = "usuario")
    Optional<TokenRefresco> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "usuario")
    @Query("select token from TokenRefresco token where token.tokenHash = :tokenHash")
    Optional<TokenRefresco> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("update TokenRefresco token set token.revocado = true, token.fechaRevocacion = :fechaRevocacion "
            + "where token.usuario.id = :idUsuario and token.revocado = false")
    void revocarTokensActivosPorUsuario(
            @Param("idUsuario") Long idUsuario,
            @Param("fechaRevocacion") OffsetDateTime fechaRevocacion
    );
}
