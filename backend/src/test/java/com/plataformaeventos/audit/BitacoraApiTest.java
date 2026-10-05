package com.plataformaeventos.audit;

import com.plataformaeventos.audit.controlador.BitacoraController;
import com.plataformaeventos.audit.repositorio.BitacoraRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.reflect.Method;
import static org.assertj.core.api.Assertions.assertThat;

class BitacoraApiTest {
    @Test
    void bitacoraSoloExponeLecturaYRepositorioNoTieneBorrado() {
        assertThat(java.util.Arrays.stream(BitacoraController.class.getDeclaredMethods())
                .map(Method::getName).filter(n -> n.equals("actualizar") || n.equals("eliminar"))).isEmpty();
        assertThat(java.util.Arrays.stream(BitacoraRepository.class.getDeclaredMethods())
                .map(Method::getName).filter(n -> n.startsWith("delete") || n.startsWith("update"))).isEmpty();
    }

    @Test
    void bitacoraAutorizaConPermisoDeMembresiaActivaOSaasExplicito() throws NoSuchMethodException {
        Method listar = BitacoraController.class.getDeclaredMethod(
                "listar",
                String.class,
                String.class,
                String.class,
                Boolean.class,
                Long.class,
                java.time.OffsetDateTime.class,
                java.time.OffsetDateTime.class,
                org.springframework.data.domain.Pageable.class,
                org.springframework.security.core.Authentication.class,
                jakarta.servlet.http.HttpServletRequest.class
        );

        String expresion = listar.getAnnotation(PreAuthorize.class).value();

        assertThat(expresion).contains("@contextoOrganizacion.tieneAutoridad(authentication,#request,'BITACORA_VER')");
        assertThat(expresion).contains("@contextoOrganizacion.esAdministrador(authentication,#request)");
        assertThat(expresion).doesNotContain("hasAnyAuthority('BITACORA_VER','ROLE_ADMINISTRADOR')");
        assertThat(expresion).doesNotContain("ROLE_ADMINISTRADOR");
    }

}
