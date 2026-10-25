package com.plataformaeventos.asistencias.repositorio;

import com.plataformaeventos.asistencias.modelo.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {
    boolean existsByInscripcionId(Long idInscripcion);
}
