package com.plataformaeventos.asistencias.repositorio;

import com.plataformaeventos.asistencias.modelo.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {
    boolean existsByInscripcionId(Long idInscripcion);

    List<Asistencia> findByInscripcionIdIn(Collection<Long> idsInscripcion);
}
