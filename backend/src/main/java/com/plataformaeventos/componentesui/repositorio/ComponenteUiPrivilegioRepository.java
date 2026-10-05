package com.plataformaeventos.componentesui.repositorio;

import com.plataformaeventos.componentesui.modelo.GrupoComponenteUiPrivilegio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComponenteUiPrivilegioRepository extends JpaRepository<GrupoComponenteUiPrivilegio, Long> {
    List<GrupoComponenteUiPrivilegio> findByOrganizacionIdAndGrupoId(Long idOrganizacion, Long idGrupo);

    void deleteByOrganizacionIdAndGrupoId(Long idOrganizacion, Long idGrupo);

    @Query("select p from GrupoComponenteUiPrivilegio p join p.grupo g join g.miembros u "
            + "join MembresiaOrganizacion m on m.usuario = u and m.organizacion = g.organizacion "
            + "where u.correo = :correo and g.organizacion.id = :organizacion and p.organizacion.id = :organizacion "
            + "and g.activo = true and m.activo = true")
    List<GrupoComponenteUiPrivilegio> findActivosPorUsuarioYOrganizacion(
            @Param("correo") String correo,
            @Param("organizacion") Long organizacion
    );
}
