package com.plataformaeventos.certificados.repositorio;

import com.plataformaeventos.certificados.modelo.Certificado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CertificadoRepository extends JpaRepository<Certificado, Long> {

    Optional<Certificado> findByCodigoVerificacion(String codigoVerificacion);

    Optional<Certificado> findByInscripcionIdAndOrganizacionId(Long idInscripcion, Long idOrganizacion);

    boolean existsByInscripcionIdAndOrganizacionId(Long idInscripcion, Long idOrganizacion);

    @Query("SELECT c FROM Certificado c JOIN FETCH c.inscripcion i JOIN FETCH i.evento e JOIN FETCH i.participante u JOIN FETCH c.organizacion o WHERE u.id = :idUsuario AND c.organizacion.id = :idOrganizacion ORDER BY c.fechaEmision DESC")
    List<Certificado> findByUsuarioIdAndOrganizacionId(@Param("idUsuario") Long idUsuario, @Param("idOrganizacion") Long idOrganizacion);

    @Query("SELECT c FROM Certificado c JOIN FETCH c.inscripcion i JOIN FETCH i.evento e JOIN FETCH i.participante u JOIN FETCH c.organizacion o WHERE e.id = :idEvento AND c.organizacion.id = :idOrganizacion ORDER BY c.fechaEmision DESC")
    List<Certificado> findByEventoIdAndOrganizacionId(@Param("idEvento") Long idEvento, @Param("idOrganizacion") Long idOrganizacion);

    @Query("SELECT c FROM Certificado c JOIN FETCH c.inscripcion i JOIN FETCH i.evento e JOIN FETCH i.participante u JOIN FETCH c.organizacion o WHERE c.codigoVerificacion = :codigo")
    Optional<Certificado> findByCodigoVerificacionConDetalles(@Param("codigo") String codigo);
}
