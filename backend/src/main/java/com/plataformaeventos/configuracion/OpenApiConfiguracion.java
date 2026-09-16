package com.plataformaeventos.configuracion;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguracion {

    @Bean
    OpenAPI configurarOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Gestion de Eventos - Sprint 2")
                        .version("Sprint 2")
                        .description("API REST multi-tenant para organizaciones, usuarios, categorias, eventos, convocatorias, inscripciones y Staff. El tenant activo se resuelve con X-Organization-Id y requiere una membresia activa, excepto para el Administrador SaaS. Respuestas comunes: 401 si falta o es invalido el bearer JWT; 403 si el usuario no tiene permiso o membresia para el tenant; 404 si el recurso no existe en el tenant; 400 si los datos o la transicion de estado son invalidos.")
                        .license(new License().name("Uso academico")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSecuritySchemes("organizationId", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Organization-Id")
                                .description("Identificador del tenant activo. Debe corresponder a una membresia activa del usuario."))
                        .addResponses("Unauthorized", new ApiResponse().description("401 - Autenticacion bearer ausente o invalida"))
                        .addResponses("Forbidden", new ApiResponse().description("403 - Permiso o membresia insuficiente para el tenant activo"))
                        .addResponses("NotFound", new ApiResponse().description("404 - Recurso no encontrado dentro del tenant activo"))
                        .addResponses("BadRequest", new ApiResponse().description("400 - Datos, filtros o transicion de estado invalidos")));
    }
}
