package com.plataformaeventos.espacios.repositorio;

import com.plataformaeventos.espacios.modelo.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EspacioRepository extends JpaRepository<Espacio, Long> {
    List<Espacio> findAllByOrderByNombreAsc();
}
