package com.plataformaeventos.convocatorias.postulaciones;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AsignacionStaffRepository extends JpaRepository<AsignacionStaff, Long> {

    boolean existsByPostulacionId(Long idPostulacion);

    long countByPostulacionConvocatoriaId(Long idConvocatoria);
}
