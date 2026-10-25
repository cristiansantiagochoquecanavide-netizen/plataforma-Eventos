package com.plataformaeventos.seguridad;

import com.plataformaeventos.usuarios.modelo.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

@Service
public class TokenRefrescoService {

    private static final SecureRandom GENERADOR_SEGURO = new SecureRandom();

    private final TokenRefrescoRepository tokenRefrescoRepository;
    private final JwtPropiedades propiedades;

    public TokenRefrescoService(TokenRefrescoRepository tokenRefrescoRepository, JwtPropiedades propiedades) {
        this.tokenRefrescoRepository = tokenRefrescoRepository;
        this.propiedades = propiedades;
    }

    @Transactional
    public String crearToken(Usuario usuario) {
        byte[] bytes = new byte[48];
        GENERADOR_SEGURO.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        TokenRefresco tokenRefresco = new TokenRefresco(
                usuario,
                calcularHash(token),
                OffsetDateTime.now().plusDays(propiedades.refrescoExpiracionDias())
        );
        tokenRefrescoRepository.save(tokenRefresco);
        return token;
    }

    @Transactional
    public TokenRenovado renovarToken(String token) {
        TokenRefresco tokenRefresco = tokenRefrescoRepository.findByTokenHashForUpdate(calcularHash(token))
                .orElseThrow(() -> new IllegalArgumentException("Token de refresco invalido"));
        if (!tokenRefresco.estaActivo()) {
            throw new IllegalArgumentException("Token de refresco invalido");
        }
        Usuario usuario = tokenRefresco.getUsuario();
        tokenRefresco.revocar();
        return new TokenRenovado(usuario, crearToken(usuario));
    }

    @Transactional
    public void revocarToken(String token) {
        tokenRefrescoRepository.findByTokenHash(calcularHash(token))
                .filter(TokenRefresco::estaActivo)
                .ifPresent(tokenRefresco -> {
                    tokenRefresco.revocar();
                    tokenRefrescoRepository.save(tokenRefresco);
                });
    }

    @Transactional
    public void revocarTokensDelUsuario(Long idUsuario) {
        tokenRefrescoRepository.revocarTokensActivosPorUsuario(idUsuario, OffsetDateTime.now());
    }

    private String calcularHash(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException excepcion) {
            throw new IllegalStateException("SHA-256 no esta disponible", excepcion);
        }
    }

    public record TokenRenovado(Usuario usuario, String token) {
    }
}
