package com.plataformaeventos.configuracion;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguracion {

    @Bean
    OpenAPI configurarOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Gestion de Eventos")
                        .version("Sprint 0")
                        .description("API REST para la gestion de identidad, acceso, usuarios, roles y permisos.")
                        .license(new License().name("Uso academico")))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
