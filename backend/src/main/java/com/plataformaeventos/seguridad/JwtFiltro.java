package com.plataformaeventos.seguridad;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFiltro extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final DetallesUsuarioService detallesUsuarioService;

    public JwtFiltro(JwtService jwtService, DetallesUsuarioService detallesUsuarioService) {
        this.jwtService = jwtService;
        this.detallesUsuarioService = detallesUsuarioService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest solicitud,
            HttpServletResponse respuesta,
            FilterChain cadena
    ) throws ServletException, IOException {
        String autorizacion = solicitud.getHeader("Authorization");
        if (autorizacion == null || !autorizacion.startsWith("Bearer ")) {
            cadena.doFilter(solicitud, respuesta);
            return;
        }

        try {
            String correo = jwtService.extraerCorreo(autorizacion.substring(7));
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails usuario = detallesUsuarioService.loadUserByUsername(correo);
                if (!usuario.isEnabled()) {
                    throw new IllegalArgumentException("La cuenta se encuentra inactiva");
                }
                UsernamePasswordAuthenticationToken autenticacion = new UsernamePasswordAuthenticationToken(
                        usuario,
                        null,
                        usuario.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException excepcion) {
            SecurityContextHolder.clearContext();
        }

        cadena.doFilter(solicitud, respuesta);
    }
}
