package com.plataformaeventos.seguridad;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "usuario")
    @Query("select token from TokenRecuperacion token where token.tokenHash = :tokenHash")
    Optional<TokenRecuperacion> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("update TokenRecuperacion token set token.activo = false, token.fechaInactivacion = :fechaInactivacion "
            + "where token.usuario.id = :idUsuario and token.activo = true and token.utilizado = false")
    void invalidarTokensPendientesPorUsuario(
            @Param("idUsuario") Long idUsuario,
            @Param("fechaInactivacion") OffsetDateTime fechaInactivacion
    );
}
