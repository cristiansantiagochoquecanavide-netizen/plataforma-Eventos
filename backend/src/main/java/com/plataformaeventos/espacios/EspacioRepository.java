package com.plataformaeventos.espacios;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EspacioRepository extends JpaRepository<Espacio, Long> {
    List<Espacio> findAllByOrderByNombreAsc();
}
