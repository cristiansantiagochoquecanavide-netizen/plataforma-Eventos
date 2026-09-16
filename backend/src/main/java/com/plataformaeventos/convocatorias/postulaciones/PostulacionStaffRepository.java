package com.plataformaeventos.convocatorias.postulaciones;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostulacionStaffRepository extends JpaRepository<PostulacionStaff, Long> {

    boolean existsByConvocatoriaIdAndUsuarioId(Long idConvocatoria, Long idUsuario);

    Optional<PostulacionStaff> findByConvocatoriaIdAndUsuarioId(Long idConvocatoria, Long idUsuario);

    Optional<PostulacionStaff> findByIdAndUsuarioId(Long id, Long idUsuario);

    List<PostulacionStaff> findByUsuarioIdOrderByFechaPostulacionDescIdDesc(Long idUsuario);
}
