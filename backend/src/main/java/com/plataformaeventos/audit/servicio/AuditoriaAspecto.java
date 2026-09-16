package com.plataformaeventos.audit.servicio;

import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.servicio.ContextoOrganizacion;
import com.plataformaeventos.usuarios.modelo.Usuario;
import com.plataformaeventos.usuarios.repositorio.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect @Component
public class AuditoriaAspecto {
    private final BitacoraService bitacora;
    private final UsuarioRepository usuarios;
    private final ContextoOrganizacion contexto;
    public AuditoriaAspecto(BitacoraService bitacora, UsuarioRepository usuarios, ContextoOrganizacion contexto) { this.bitacora = bitacora; this.usuarios = usuarios; this.contexto = contexto; }

    @Around("@annotation(operacion)")
    public Object auditar(ProceedingJoinPoint punto, OperacionAuditable operacion) throws Throwable {
        Object resultado = punto.proceed();
        HttpServletRequest request = solicitudActual();
        Organizacion organizacion = argumento(punto, Organizacion.class);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || request == null) return resultado;
        if (organizacion == null) {
            try { organizacion = contexto.resolver(auth, request); }
            catch (RuntimeException ex) { return resultado; }
        }
        Usuario actor = argumento(punto, Usuario.class);
        if (actor == null) actor = usuarios.findByCorreo(auth.getName()).orElse(null);
        Long id = idResultado(resultado);
        bitacora.registrar(actor, organizacion, operacion.accion(), operacion.modulo(), operacion.entidad(), id,
                AuditoriaContexto.ip(request), true, "Operacion completada");
        return resultado;
    }

    private HttpServletRequest solicitudActual() {
        var atributos = RequestContextHolder.getRequestAttributes();
        return atributos instanceof ServletRequestAttributes servlet ? servlet.getRequest() : null;
    }
    private <T> T argumento(ProceedingJoinPoint punto, Class<T> tipo) {
        return java.util.Arrays.stream(punto.getArgs()).filter(tipo::isInstance).map(tipo::cast).findFirst().orElse(null);
    }
    private Long idResultado(Object resultado) {
        if (resultado == null) return null;
        try { Object id = resultado.getClass().getMethod("id").invoke(resultado); return id instanceof Long value ? value : null; }
        catch (ReflectiveOperationException ex) { return null; }
    }
}
