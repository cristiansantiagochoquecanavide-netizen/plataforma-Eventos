package com.plataformaeventos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PlataformaGestionEventosAplicacion {

    public static void main(String[] args) {
        SpringApplication.run(PlataformaGestionEventosAplicacion.class, args);
    }
}
