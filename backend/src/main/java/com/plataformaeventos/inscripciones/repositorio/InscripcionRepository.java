package com.plataformaeventos.inscripciones.repositorio;

import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    @Query("SELECT i FROM Inscripcion i JOIN FETCH i.evento e LEFT JOIN FETCH e.categoria LEFT JOIN FETCH e.espacio WHERE i.participante.id = :idParticipante ORDER BY i.fechaInscripcion DESC")
    List<Inscripcion> findByParticipanteIdConEvento(@Param("idParticipante") Long idParticipante);
    @Query("SELECT i FROM Inscripcion i JOIN FETCH i.evento e LEFT JOIN FETCH e.categoria LEFT JOIN FETCH e.espacio WHERE i.participante.id = :usuario AND i.organizacion.id = :org ORDER BY i.fechaInscripcion DESC")
    List<Inscripcion> findByParticipanteIdAndOrganizacionIdConEvento(@Param("usuario") Long usuario, @Param("org") Long org);

    Optional<Inscripcion> findByEventoIdAndParticipanteId(Long idEvento, Long idParticipante);

    boolean existsByEventoIdAndParticipanteIdAndEstado(Long idEvento, Long idParticipante, EstadoInscripcion estado);

    long countByEventoIdAndEstado(Long idEvento, EstadoInscripcion estado);
    long countByEventoIdAndOrganizacionIdAndEstado(Long idEvento, Long organizacionId, EstadoInscripcion estado);

    Optional<Inscripcion> findByIdAndParticipanteId(Long idInscripcion, Long idParticipante);

    @Query("SELECT i FROM Inscripcion i JOIN FETCH i.participante u JOIN FETCH i.evento e LEFT JOIN FETCH e.categoria LEFT JOIN FETCH e.espacio WHERE i.id = :id AND i.participante.id = :usuario AND (:org IS NULL OR i.organizacion.id = :org)")
    Optional<Inscripcion> findCredentialByIdAndParticipant(@Param("id") Long id, @Param("usuario") Long usuario, @Param("org") Long organizacionId);

    @Query("select i from Inscripcion i where i.id = :id and i.participante.id = :usuario and i.organizacion.id = :org")
    Optional<Inscripcion> findByIdAndParticipanteIdAndOrganizacionId(@Param("id") Long id, @Param("usuario") Long usuario, @Param("org") Long org);

    @Query("select i from Inscripcion i where i.evento.id = :evento and i.participante.id = :usuario and i.organizacion.id = :org")
    Optional<Inscripcion> findByEventoIdAndParticipanteIdAndOrganizacionId(@Param("evento") Long evento, @Param("usuario") Long usuario, @Param("org") Long org);

    List<Inscripcion> findByEventoIdAndOrganizacionId(Long evento, Long organizacionId);

    long countByParticipanteIdAndOrganizacionId(Long idParticipante, Long organizacionId);
    long countByParticipanteIdAndOrganizacionIdAndEstado(Long idParticipante, Long organizacionId, EstadoInscripcion estado);

    Optional<Inscripcion> findFirstByParticipanteIdAndOrganizacionIdOrderByFechaInscripcionDesc(Long idParticipante, Long organizacionId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from com.plataformaeventos.eventos.modelo.Evento e where e.id = :id and e.organizacion.id = :org")
    java.util.Optional<com.plataformaeventos.eventos.modelo.Evento> bloquearEvento(@Param("id") Long id, @Param("org") Long org);

    @Query("SELECT i FROM Inscripcion i JOIN FETCH i.participante u JOIN FETCH i.evento e WHERE i.organizacion.id = :org ORDER BY i.fechaInscripcion DESC")
    List<Inscripcion> findByOrganizacionIdConDetalles(@Param("org") Long org);

    @Query("SELECT i FROM Inscripcion i JOIN FETCH i.participante u JOIN FETCH i.evento e WHERE i.evento.id = :idEvento AND i.organizacion.id = :org ORDER BY i.fechaInscripcion DESC")
    List<Inscripcion> findByEventoIdAndOrganizacionIdConDetalles(@Param("idEvento") Long idEvento, @Param("org") Long org);

    @Query("SELECT i FROM Inscripcion i JOIN FETCH i.participante u JOIN FETCH i.evento e ORDER BY i.fechaInscripcion DESC")
    List<Inscripcion> findAllConDetalles();

    @Query("SELECT i FROM Inscripcion i JOIN FETCH i.participante u JOIN FETCH i.evento e WHERE i.evento.id = :idEvento ORDER BY i.fechaInscripcion DESC")
    List<Inscripcion> findByEventoIdConDetalles(@Param("idEvento") Long idEvento);
}
