package com.plataformaeventos.categorias.repositorio;

import com.plataformaeventos.categorias.modelo.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByNombreIgnoreCaseAndOrganizacionId(String nombre, Long organizacionId);
    Optional<Categoria> findFirstByNombreIgnoreCaseAndOrganizacionId(String nombre, Long organizacionId);
    boolean existsByNombreIgnoreCaseAndOrganizacionIdAndIdNot(String nombre, Long organizacionId, Long id);
    List<Categoria> findByOrganizacionIdOrderByNombreAsc(Long organizacionId);
    Optional<Categoria> findByIdAndOrganizacionId(Long id, Long organizacionId);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}
