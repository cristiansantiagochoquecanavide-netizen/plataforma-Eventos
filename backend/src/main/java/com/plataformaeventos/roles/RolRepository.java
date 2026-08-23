package com.plataformaeventos.roles;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombre(String nombre);

    List<Rol> findByNombreIn(Collection<String> nombres);

    @EntityGraph(attributePaths = "permisos")
    @Query("select rol from Rol rol where rol.id = :idRol")
    Optional<Rol> findByIdWithPermisos(@Param("idRol") Long idRol);
}
