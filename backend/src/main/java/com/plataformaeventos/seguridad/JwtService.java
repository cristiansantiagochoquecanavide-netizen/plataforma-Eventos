package com.plataformaeventos.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final JwtPropiedades propiedades;
    private SecretKey claveFirma;

    public JwtService(JwtPropiedades propiedades) {
        this.propiedades = propiedades;
    }

    @PostConstruct
    void inicializarClaveFirma() {
        byte[] secreto = propiedades.secreto().getBytes(StandardCharsets.UTF_8);
        if (secreto.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        claveFirma = Keys.hmacShaKeyFor(secreto);
    }

    public String generarTokenAcceso(String correo) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(correo)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(propiedades.accesoExpiracionMinutos(), ChronoUnit.MINUTES)))
                .signWith(claveFirma)
                .compact();
    }

    public String extraerCorreo(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(claveFirma)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}
