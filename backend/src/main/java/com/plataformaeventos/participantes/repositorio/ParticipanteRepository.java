package com.plataformaeventos.participantes.repositorio;

import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipanteRepository extends JpaRepository<Usuario, Long> {

    @Query("""
            select distinct u from Usuario u
            where (
                exists (
                    select 1 from Inscripcion i
                    where i.participante.id = u.id and i.organizacion.id = :org
                      and (:eventoId is null or i.evento.id = :eventoId)
                )
                or (
                    :eventoId is null and exists (
                        select 1 from MembresiaOrganizacion m join m.roles r
                        where m.usuario.id = u.id and m.organizacion.id = :org and r.nombre = 'PARTICIPANTE'
                    )
                )
            )
            and (
                :estado is null
                or exists (
                    select 1 from MembresiaOrganizacion em
                    where em.usuario.id = u.id and em.organizacion.id = :org and em.estadoParticipante = :estado
                )
                or (
                    not exists (
                        select 1 from MembresiaOrganizacion nm
                        where nm.usuario.id = u.id and nm.organizacion.id = :org
                    )
                    and u.estado = :estado
                )
            )
            and (:busqueda is null or (
                lower(u.nombres) like concat('%', lower(:busqueda), '%') or
                lower(u.apellidos) like concat('%', lower(:busqueda), '%') or
                lower(u.correo) like concat('%', lower(:busqueda), '%')
            ))
            order by u.apellidos asc, u.nombres asc
            """)
    Page<Usuario> buscarParticipantes(
            @Param("org") Long org,
            @Param("eventoId") Long eventoId,
            @Param("busqueda") String busqueda,
            @Param("estado") EstadoUsuario estado,
            Pageable pageable
    );

    @Query("""
            select distinct u from Usuario u
            where (
                exists (
                    select 1 from Inscripcion i
                    where i.participante.id = u.id and i.organizacion.id = :org
                      and (:eventoId is null or i.evento.id = :eventoId)
                )
                or (
                    :eventoId is null and exists (
                        select 1 from MembresiaOrganizacion m join m.roles r
                        where m.usuario.id = u.id and m.organizacion.id = :org and r.nombre = 'PARTICIPANTE'
                    )
                )
            )
            and (
                :estado is null
                or exists (
                    select 1 from MembresiaOrganizacion em
                    where em.usuario.id = u.id and em.organizacion.id = :org and em.estadoParticipante = :estado
                )
                or (
                    not exists (
                        select 1 from MembresiaOrganizacion nm
                        where nm.usuario.id = u.id and nm.organizacion.id = :org
                    )
                    and u.estado = :estado
                )
            )
            and (:busqueda is null or (
                lower(u.nombres) like concat('%', lower(:busqueda), '%') or
                lower(u.apellidos) like concat('%', lower(:busqueda), '%') or
                lower(u.correo) like concat('%', lower(:busqueda), '%')
            ))
            order by u.apellidos asc, u.nombres asc
            """)
    List<Usuario> listarTodosParaExportacion(
            @Param("org") Long org,
            @Param("eventoId") Long eventoId,
            @Param("busqueda") String busqueda,
            @Param("estado") EstadoUsuario estado
    );

    @Query("""
            select count(u) > 0 from Usuario u
            where u.id = :id
              and (
                exists (
                    select 1 from Inscripcion i
                    where i.participante.id = u.id and i.organizacion.id = :org
                )
                or exists (
                    select 1 from MembresiaOrganizacion m
                    where m.usuario.id = u.id and m.organizacion.id = :org
                )
              )
            """)
    boolean perteneceAOrganizacion(@Param("id") Long id, @Param("org") Long org);

    @Query("""
            select u from Usuario u
            where u.id = :id
              and (
                exists (
                    select 1 from Inscripcion i
                    where i.participante.id = u.id and i.organizacion.id = :org
                )
                or exists (
                    select 1 from MembresiaOrganizacion m
                    where m.usuario.id = u.id and m.organizacion.id = :org
                )
              )
            """)
    Optional<Usuario> buscarPorIdYOrganizacion(@Param("id") Long id, @Param("org") Long org);

    @Query("""
            select m.estadoParticipante from MembresiaOrganizacion m
            where m.usuario.id = :id and m.organizacion.id = :org
            """)
    Optional<EstadoUsuario> buscarEstadoParticipante(@Param("id") Long id, @Param("org") Long org);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update MembresiaOrganizacion m
            set m.estadoParticipante = :estado
            where m.usuario.id = :id and m.organizacion.id = :org
            """)
    int cambiarEstadoParticipante(
            @Param("id") Long id,
            @Param("org") Long org,
            @Param("estado") EstadoUsuario estado
    );
}
