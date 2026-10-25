package com.plataformaeventos.audit.cifrado;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.kms.KmsClient;

@Configuration
public class BitacoraCifradoConfiguracion {

    @Bean
    KmsClient kmsClient(
            BitacoraCifradoProperties properties,
            @Value("${audit.cifrado.kms-endpoint:}") String kmsEndpoint) {
        var builder = KmsClient.builder()
                .region(properties.configuredRegionOrDefault())
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (kmsEndpoint != null && !kmsEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(kmsEndpoint));
        }
        return builder.build();
    }
}
