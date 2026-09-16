package com.plataformaeventos.convocatorias.postulaciones;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface PostulacionStaffRepository extends JpaRepository<PostulacionStaff, Long> {

    boolean existsByConvocatoriaIdAndUsuarioId(Long idConvocatoria, Long idUsuario);

    Optional<PostulacionStaff> findByConvocatoriaIdAndUsuarioId(Long idConvocatoria, Long idUsuario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PostulacionStaff> findByIdAndUsuarioId(Long id, Long idUsuario);

    List<PostulacionStaff> findByUsuarioIdOrderByFechaPostulacionDescIdDesc(Long idUsuario);

    @EntityGraph(attributePaths = {"usuario", "convocatoria", "convocatoria.evento"})
    List<PostulacionStaff> findByConvocatoriaIdOrderByFechaPostulacionAscIdAsc(Long idConvocatoria);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PostulacionStaff p WHERE p.id = :id")
    Optional<PostulacionStaff> buscarParaGestion(@Param("id") Long id);
}
