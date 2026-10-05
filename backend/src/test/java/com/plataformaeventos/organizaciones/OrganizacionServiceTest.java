package com.plataformaeventos.organizaciones;

import com.plataformaeventos.organizaciones.dto.AgregarMembresiaSolicitud;
import com.plataformaeventos.organizaciones.modelo.MembresiaOrganizacion;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.organizaciones.servicio.OrganizacionService;
import com.plataformaeventos.roles.modelo.EstadoRol;
import com.plataformaeventos.roles.modelo.Rol;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrganizacionServiceTest {
    private final OrganizacionRepository organizaciones = mock(OrganizacionRepository.class);
    private final MembresiaOrganizacionRepository membresias = mock(MembresiaOrganizacionRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RolRepository roles = mock(RolRepository.class);
    private final ContextoOrganizacion contexto = mock(ContextoOrganizacion.class);
    private final OrganizacionService servicio = new OrganizacionService(organizaciones, membresias, usuarios, roles, contexto);

    @Test
    void noPermiteAgregarMembresiaEnOtraOrganizacion() {
        Organizacion activa = org(1L);
        when(contexto.resolver(any(), any())).thenReturn(activa);
        when(contexto.esAdministrador(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> servicio.agregarMembresia(2L, new AgregarMembresiaSolicitud(8L), "actor@test", mock(Authentication.class), mock(HttpServletRequest.class)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("gestionar membresias");
        verifyNoInteractions(usuarios);
    }

    @Test
    void agregaMembresiaConRolSolicitado() {
        Organizacion activa = org(1L);
        Usuario usuario = new Usuario("Ana", "Lopez", "ana@test", "hash", EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(usuario, "id", 8L);
        Rol rol = new Rol("STAFF", null, EstadoRol.ACTIVO);
        when(contexto.resolver(any(), any())).thenReturn(activa);
        when(contexto.esAdministrador(any(), any())).thenReturn(true);
        when(organizaciones.findById(1L)).thenReturn(Optional.of(activa));
        when(usuarios.findById(8L)).thenReturn(Optional.of(usuario));
        when(membresias.existsById(any())).thenReturn(false);
        when(roles.findByNombreIn(Set.of("STAFF"))).thenReturn(java.util.List.of(rol));

        servicio.agregarMembresia(1L, new AgregarMembresiaSolicitud(8L, Set.of("STAFF")), "actor@test", mock(Authentication.class), mock(HttpServletRequest.class));

        verify(membresias).save(argThat(m -> m.getRoles().contains(rol)));
    }

    @Test
    void listarMiembrosDistingueMembresiaActivaDeCuentaActiva() {
        Organizacion activa = org(1L);
        Usuario cuentaActiva = usuario(8L, "Ana", EstadoUsuario.ACTIVO);
        Usuario cuentaInactiva = usuario(9L, "Luis", EstadoUsuario.INACTIVO);
        MembresiaOrganizacion membresiaActivaCuentaActiva = new MembresiaOrganizacion(cuentaActiva, activa);
        MembresiaOrganizacion membresiaActivaCuentaInactiva = new MembresiaOrganizacion(cuentaInactiva, activa);
        Rol rol = new Rol("STAFF", null, EstadoRol.ACTIVO);
        membresiaActivaCuentaActiva.asignarRoles(Set.of(rol));
        membresiaActivaCuentaInactiva.asignarRoles(Set.of(rol));
        when(contexto.resolver(any(), any())).thenReturn(activa);
        when(membresias.findByOrganizacionIdOrderByUsuarioApellidosAscUsuarioNombresAsc(1L))
                .thenReturn(java.util.List.of(membresiaActivaCuentaActiva, membresiaActivaCuentaInactiva));

        var respuesta = servicio.listarMiembros(mock(Authentication.class), mock(HttpServletRequest.class));

        assertThat(respuesta).extracting("idUsuario", "activo", "cuentaActiva")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(8L, true, true),
                        org.assertj.core.groups.Tuple.tuple(9L, true, false)
                );
    }

    private Usuario usuario(Long id, String nombres, EstadoUsuario estado) {
        Usuario usuario = new Usuario(nombres, "Lopez", nombres.toLowerCase() + "@test", "hash", estado);
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    private Organizacion org(Long id) {
        Organizacion org = new Organizacion("Org", "org" + id);
        ReflectionTestUtils.setField(org, "id", id);
        return org;
    }
}
