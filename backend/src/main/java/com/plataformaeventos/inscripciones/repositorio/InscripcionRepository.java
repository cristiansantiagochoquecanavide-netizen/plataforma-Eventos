package com.plataformaeventos.inscripciones.repositorio;

import com.plataformaeventos.inscripciones.modelo.EstadoInscripcion;
import com.plataformaeventos.inscripciones.modelo.Inscripcion;
import com.plataformaeventos.asistencias.dto.InscripcionElegibleAsistenciaRespuesta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    @Query("select distinct new com.plataformaeventos.asistencias.dto.InscripcionElegibleAsistenciaRespuesta(" +
            "i.id, p.nombres, p.apellidos, p.correo, e.id, e.nombre, i.estado, " +
            "case when a.id is null then false else true end, a.fechaHora) " +
            "from Inscripcion i join i.participante p join i.evento e " +
            "join AsignacionStaff asignacion on asignacion.postulacion.convocatoria.evento = e " +
            "left join Asistencia a on a.inscripcion = i " +
            "where i.organizacion.id = :organizacionId and e.organizacion.id = :organizacionId " +
            "and asignacion.organizacion.id = :organizacionId and asignacion.usuario.id = :staffId " +
            "and asignacion.estado = com.plataformaeventos.asignacionesstaff.modelo.EstadoAsignacionStaff.ASIGNADA " +
            "and i.estado = com.plataformaeventos.inscripciones.modelo.EstadoInscripcion.CONFIRMADA " +
            "and e.estado in (com.plataformaeventos.eventos.modelo.EstadoEvento.PUBLICADO, " +
            "com.plataformaeventos.eventos.modelo.EstadoEvento.EN_CURSO) " +
            "order by e.nombre, p.apellidos, p.nombres")
    List<InscripcionElegibleAsistenciaRespuesta> listarElegiblesParaAsistencia(
            @Param("organizacionId") Long organizacionId, @Param("staffId") Long staffId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inscripcion i join fetch i.evento e join fetch i.participante p where i.id = :id and i.organizacion.id = :org")
    Optional<Inscripcion> bloquearPorIdEOrganizacion(@Param("id") Long id, @Param("org") Long org);

    @Query("SELECT i FROM Inscripcion i JOIN FETCH i.evento e LEFT JOIN FETCH e.categoria LEFT JOIN FETCH e.espacio WHERE i.participante.id = :idParticipante ORDER BY i.fechaInscripcion DESC")
    List<Inscripcion> findByParticipanteIdConEvento(@Param("idParticipante") Long idParticipante);
    @Query("SELECT i FROM Inscripcion i JOIN FETCH i.evento e LEFT JOIN FETCH e.categoria LEFT JOIN FETCH e.espacio WHERE i.participante.id = :usuario AND i.organizacion.id = :org ORDER BY i.fechaInscripcion DESC")
    List<Inscripcion> findByParticipanteIdAndOrganizacionIdConEvento(@Param("usuario") Long usuario, @Param("org") Long org);

    Optional<Inscripcion> findByEventoIdAndParticipanteId(Long idEvento, Long idParticipante);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inscripcion i where i.codigoQr = :codigoQr and i.organizacion.id = :org")
    Optional<Inscripcion> findByCodigoQrAndOrganizacionId(@Param("codigoQr") String codigoQr, @Param("org") Long organizacionId);

    boolean existsByEventoIdAndParticipanteIdAndEstado(Long idEvento, Long idParticipante, EstadoInscripcion estado);

    long countByEventoIdAndEstado(Long idEvento, EstadoInscripcion estado);
    long countByEventoIdAndOrganizacionIdAndEstado(Long idEvento, Long organizacionId, EstadoInscripcion estado);

    long countByTipoEntradaIdAndEstado(Long tipoEntradaId, EstadoInscripcion estado);

    @Query("SELECT COALESCE(SUM(i.montoPagado), 0) FROM Inscripcion i WHERE i.evento.id = :eventoId AND i.estado = com.plataformaeventos.inscripciones.modelo.EstadoInscripcion.CONFIRMADA")
    java.math.BigDecimal sumMontoPagadoByEventoId(@Param("eventoId") Long eventoId);

    @Query("SELECT COALESCE(SUM(i.montoPagado), 0) FROM Inscripcion i WHERE i.organizacion.id = :orgId AND i.estado = com.plataformaeventos.inscripciones.modelo.EstadoInscripcion.CONFIRMADA")
    java.math.BigDecimal sumMontoPagadoByOrganizacionId(@Param("orgId") Long orgId);

    @Query("""
            SELECT i.evento.id,
                   COALESCE(te.nombre, i.categoriaEntrada, 'General'),
                   COALESCE(te.precio, 0),
                   COUNT(i),
                   COALESCE(SUM(i.montoPagado), 0),
                   COALESCE(MAX(te.cupoMaximo), 0)
            FROM Inscripcion i
            LEFT JOIN i.tipoEntrada te
            WHERE i.organizacion.id = :organizacionId
              AND i.estado = com.plataformaeventos.inscripciones.modelo.EstadoInscripcion.CONFIRMADA
            GROUP BY i.evento.id, COALESCE(te.nombre, i.categoriaEntrada, 'General'), COALESCE(te.precio, 0)
            ORDER BY i.evento.id ASC, COALESCE(te.precio, 0) ASC
            """)
    List<Object[]> resumirVentasPorTipoEntradaOrganizacion(@Param("organizacionId") Long organizacionId);

    @Query("""
            SELECT i.evento.id, COUNT(a.id)
            FROM com.plataformaeventos.asistencias.modelo.Asistencia a
            JOIN a.inscripcion i
            WHERE a.organizacion.id = :organizacionId
            GROUP BY i.evento.id
            """)
    List<Object[]> contarAsistenciasPorEventoOrganizacion(@Param("organizacionId") Long organizacionId);

    Optional<Inscripcion> findByIdAndParticipanteId(Long idInscripcion, Long idParticipante);

    @Query("SELECT i FROM Inscripcion i JOIN FETCH i.participante u JOIN FETCH i.evento e LEFT JOIN FETCH e.categoria LEFT JOIN FETCH e.espacio WHERE i.id = :id AND i.participante.id = :usuario AND (:org IS NULL OR i.organizacion.id = :org)")
    Optional<Inscripcion> findCredentialByIdAndParticipant(@Param("id") Long id, @Param("usuario") Long usuario, @Param("org") Long organizacionId);

    @Query("select i from Inscripcion i where i.id = :id and i.participante.id = :usuario and i.organizacion.id = :org")
    Optional<Inscripcion> findByIdAndParticipanteIdAndOrganizacionId(@Param("id") Long id, @Param("usuario") Long usuario, @Param("org") Long org);

    @Query("select i from Inscripcion i where i.evento.id = :evento and i.participante.id = :usuario and i.organizacion.id = :org")
    Optional<Inscripcion> findByEventoIdAndParticipanteIdAndOrganizacionId(@Param("evento") Long evento, @Param("usuario") Long usuario, @Param("org") Long org);

    List<Inscripcion> findByEventoIdAndOrganizacionId(Long evento, Long organizacionId);

    @Query("select i from Inscripcion i join fetch i.participante p join fetch i.evento e "
            + "left join fetch e.categoria left join fetch e.espacio "
            + "where i.evento.id = :evento and i.organizacion.id = :org "
            + "and e.organizacion.id = :org "
            + "and i.estado = com.plataformaeventos.inscripciones.modelo.EstadoInscripcion.CONFIRMADA "
            + "order by i.id asc")
    List<Inscripcion> findConfirmedByEventoAndOrganizacion(@Param("evento") Long evento,
                                                            @Param("org") Long organizacionId);

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
