package com.plataformaeventos.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SeguridadConfiguracion {

    private final JwtFiltro jwtFiltro;
    private final ManejadorAccesoNoAutorizado manejadorNoAutorizado;
    private final ManejadorAccesoDenegado manejadorAccesoDenegado;

    public SeguridadConfiguracion(
            JwtFiltro jwtFiltro,
            ManejadorAccesoNoAutorizado manejadorNoAutorizado,
            ManejadorAccesoDenegado manejadorAccesoDenegado
    ) {
        this.jwtFiltro = jwtFiltro;
        this.manejadorNoAutorizado = manejadorNoAutorizado;
        this.manejadorAccesoDenegado = manejadorAccesoDenegado;
    }

    @Bean
    SecurityFilterChain configurarSeguridad(
            HttpSecurity http,
            @Qualifier("configurarCors") CorsConfigurationSource fuenteCors
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(fuenteCors))
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(excepciones -> excepciones
                        .authenticationEntryPoint(manejadorNoAutorizado)
                        .accessDeniedHandler(manejadorAccesoDenegado))
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/salud", "/actuator/health", "/swagger-ui/**", "/swagger-ui.html", "/api-docs/**")
                        .permitAll()
                        .requestMatchers("/api/autenticacion/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFiltro, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder codificadorClave() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource configurarCors(@Value("${seguridad.cors.origenes}") String origenesConfigurados) {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(Arrays.stream(origenesConfigurados.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracion.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/**", configuracion);
        return fuente;
    }
}
