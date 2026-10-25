package com.plataformaeventos.organizaciones.servicio;

import com.plataformaeventos.excepciones.ExcepcionNoAutorizado;
import com.plataformaeventos.organizaciones.modelo.Organizacion;
import com.plataformaeventos.organizaciones.repositorio.MembresiaOrganizacionRepository;
import com.plataformaeventos.organizaciones.repositorio.OrganizacionRepository;
import org.springframework.security.access.AccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import com.plataformaeventos.saas.servicio.SuscripcionOrganizacionService;
import java.time.OffsetDateTime;

@Service
public class ContextoOrganizacion {
    public static final String CABECERA = "X-Organization-Id";
    private final OrganizacionRepository organizaciones; private final MembresiaOrganizacionRepository membresias;
    private final SuscripcionOrganizacionService suscripciones;
    @Autowired
    public ContextoOrganizacion(OrganizacionRepository organizaciones, MembresiaOrganizacionRepository membresias,
                                SuscripcionOrganizacionService suscripciones) {
        this.organizaciones = organizaciones;
        this.membresias = membresias;
        this.suscripciones = suscripciones;
    }
    public Organizacion resolver(Authentication auth, HttpServletRequest request) {
        if (auth == null || !auth.isAuthenticated()) throw new ExcepcionNoAutorizado("Se requiere autenticacion");
        String valor = request.getHeader(CABECERA);
        Organizacion org = valor == null || valor.isBlank() ? organizaciones.findBySlug("principal").orElseThrow(() -> new ExcepcionNoAutorizado("Organizacion principal no encontrada")) : organizaciones.findById(parsear(valor)).orElseThrow(() -> new ExcepcionNoAutorizado("Organizacion no encontrada"));
        if (!"ACTIVA".equalsIgnoreCase(org.getEstado())) throw new ExcepcionNoAutorizado("La organizacion se encuentra inactiva");
        if (!esSaas(auth) && suscripciones != null && !suscripciones.tieneAccesoActivo(org.getId(), OffsetDateTime.now())) {
            throw new AccessDeniedException("La suscripcion de la organizacion no esta vigente");
        }
        if (!esSaas(auth) && !membresias.existsByUsuarioCorreoAndOrganizacionIdAndActivoTrue(auth.getName(), org.getId())) throw new ExcepcionNoAutorizado("El usuario no pertenece a la organizacion seleccionada");
        return org;
    }
    public boolean tieneAutoridad(Authentication auth, HttpServletRequest request, String permiso) {
        try { return auth != null && resolver(auth, request) != null && (esSaas(auth) || membresias.tienePermisoActivo(auth.getName(), resolver(auth, request).getId(), permiso)); }
        catch (RuntimeException ex) { return false; }
    }
    public boolean esAdministrador(Authentication auth, HttpServletRequest request) {
        try { return auth != null && (esSaas(auth) || membresias.esAdministradorActivo(auth.getName(), resolver(auth, request).getId())); }
        catch (RuntimeException ex) { return false; }
    }
    public boolean tieneRolActivo(Authentication auth, HttpServletRequest request, String rol) {
        try {
            Organizacion organizacion = resolver(auth, request);
            return auth != null && rol != null && membresias.tieneRolActivo(auth.getName(), organizacion.getId(), rol);
        } catch (RuntimeException ex) { return false; }
    }
    public boolean esSaas(Authentication auth) { return auth != null && auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch("SAAS_ADMINISTRADOR"::equals); }
    private Long parsear(String value) { try { return Long.valueOf(value); } catch (NumberFormatException e) { throw new ExcepcionNoAutorizado("X-Organization-Id invalido"); } }
}
