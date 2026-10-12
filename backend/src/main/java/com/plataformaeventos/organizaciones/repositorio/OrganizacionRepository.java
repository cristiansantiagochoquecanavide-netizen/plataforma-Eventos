package com.plataformaeventos.organizaciones.repositorio;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.saas.dto.OrganizacionSaasFila;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface OrganizacionRepository extends JpaRepository<Organizacion, Long> {
    Optional<Organizacion> findBySlug(String slug);

    List<Organizacion> findAllByEstadoOrderByNombreAsc(String estado);

    @Query(value = """
            select new com.plataformaeventos.saas.dto.OrganizacionSaasFila(
                o.id, o.nombre, o.slug, o.estado, o.fechaCreacion,
                count(distinct case when m.activo = true then m.usuario.id else null end),
                s.plan, s.estado, s.fechaInicio, s.fechaFin)
            from Organizacion o
            left join MembresiaOrganizacion m on m.organizacion = o
            left join SuscripcionOrganizacion s on s.organizacion = o
            where (:estado = '' or o.estado = :estado)
              and (:busqueda = '' or lower(o.nombre) like concat('%', :busqueda, '%')
                   or lower(o.slug) like concat('%', :busqueda, '%'))
            group by o.id, o.nombre, o.slug, o.estado, o.fechaCreacion,
                     s.plan, s.estado, s.fechaInicio, s.fechaFin
            order by o.nombre asc
            """,
            countQuery = """
                    select count(o)
                    from Organizacion o
                    where (:estado = '' or o.estado = :estado)
                      and (:busqueda = '' or lower(o.nombre) like concat('%', :busqueda, '%')
                           or lower(o.slug) like concat('%', :busqueda, '%'))
                    """)
    Page<OrganizacionSaasFila> listarParaAdministracionSaas(
            @Param("busqueda") String busqueda,
            @Param("estado") String estado,
            Pageable pageable
    );
}
