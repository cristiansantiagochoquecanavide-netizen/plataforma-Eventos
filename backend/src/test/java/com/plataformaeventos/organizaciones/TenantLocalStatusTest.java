package com.plataformaeventos.organizaciones;

import com.plataformaeventos.organizaciones.modelo.MembresiaOrganizacion;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.roles.repositorio.RolRepository;
import com.plataformaeventos.seguridad.TokenRefrescoService;
import com.plataformaeventos.usuarios.dto.UsuarioRespuesta;
import com.plataformaeventos.usuarios.modelo.EstadoUsuario;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import com.plataformaeventos.usuarios.servicio.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantLocalStatusTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final MembresiaOrganizacionRepository membresias = mock(MembresiaOrganizacionRepository.class);
    private final TokenRefrescoService tokens = mock(TokenRefrescoService.class);
    private final UsuarioService servicio = new UsuarioService(
            usuarios,
            mock(RolRepository.class),
            mock(PasswordEncoder.class),
            tokens,
            membresias
    );

    @Test
    void cambiarEstadoTenantInactivaSoloLaMembresiaLocalSinTocarCuentaGlobalNiOtroTenant() {
        Usuario usuario = new Usuario("Ana", "Tenant", "ana@test.com", "hash", EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(usuario, "id", 7L);
        Organizacion orgA = organizacion(10L, "org-a");
        Organizacion orgB = organizacion(20L, "org-b");
        MembresiaOrganizacion membresiaA = new MembresiaOrganizacion(usuario, orgA);
        MembresiaOrganizacion membresiaB = new MembresiaOrganizacion(usuario, orgB);

        when(usuarios.findByIdAndOrganizacionId(7L, 10L)).thenReturn(Optional.of(usuario));
        when(membresias.findByUsuarioCorreoAndOrganizacionId("ana@test.com", 10L)).thenReturn(Optional.of(membresiaA));

        UsuarioRespuesta respuesta = servicio.cambiarEstado(7L, EstadoUsuario.INACTIVO, "admin@test.com", orgA);

        assertThat(respuesta.estado()).isEqualTo("INACTIVO");
        assertThat(membresiaA.isActivo()).isFalse();
        assertThat(membresiaB.isActivo()).isTrue();
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        verify(tokens, never()).revocarTokensDelUsuario(7L);
    }

    @Test
    void respuestaTenantExponeEstadoActivoDeLaMembresiaLocalNoDeLaCuentaGlobal() {
        Usuario usuario = new Usuario("Bruno", "Tenant", "bruno@test.com", "hash", EstadoUsuario.ACTIVO);
        ReflectionTestUtils.setField(usuario, "id", 8L);
        Organizacion orgA = organizacion(10L, "org-a");
        MembresiaOrganizacion membresiaA = new MembresiaOrganizacion(usuario, orgA);
        membresiaA.inactivar();

        when(usuarios.findByIdAndOrganizacionId(8L, 10L)).thenReturn(Optional.of(usuario));
        when(membresias.findByUsuarioCorreoAndOrganizacionId("bruno@test.com", 10L)).thenReturn(Optional.of(membresiaA));

        UsuarioRespuesta respuesta = servicio.obtenerUsuario(8L, orgA);

        assertThat(respuesta.estado()).isEqualTo("INACTIVO");
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }

    private Organizacion organizacion(Long id, String slug) {
        Organizacion organizacion = new Organizacion("Organizacion " + slug, slug);
        ReflectionTestUtils.setField(organizacion, "id", id);
        return organizacion;
    }
}
