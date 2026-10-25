package com.plataformaeventos.espacios.repositorio;

import com.plataformaeventos.espacios.modelo.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EspacioRepository extends JpaRepository<Espacio, Long> {
    List<Espacio> findAllByOrderByNombreAsc();
    List<Espacio> findByOrganizacionIdOrderByNombreAsc(Long organizacionId);
    Optional<Espacio> findByIdAndOrganizacionId(Long id, Long organizacionId);
    Optional<Espacio> findFirstByNombreIgnoreCaseAndOrganizacionId(String nombre, Long organizacionId);
}
