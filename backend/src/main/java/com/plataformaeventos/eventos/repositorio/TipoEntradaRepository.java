package com.plataformaeventos.eventos.repositorio;

import com.plataformaeventos.eventos.modelo.TipoEntrada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoEntradaRepository extends JpaRepository<TipoEntrada, Long> {

    List<TipoEntrada> findByEventoIdAndActivoTrueOrderByPrecioAsc(Long eventoId);

    List<TipoEntrada> findByEventoIdOrderByPrecioAsc(Long eventoId);

    Optional<TipoEntrada> findByIdAndEventoId(Long id, Long eventoId);

    Optional<TipoEntrada> findByIdAndOrganizacionId(Long id, Long organizacionId);

    void deleteByEventoId(Long eventoId);
}
