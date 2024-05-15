package com.example.config;

import com.example.service.RegistrationService;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class RpConfiguration {

    @Value("${app.relying-party-id}")
    private String relyingPartyId;

    @Value("${app.relying-party-name}")
    private String relyingPartyName;

    @Value("${app.relying-party-origins}")
    private String relyingPartyOrigins;

    @Bean
    public RelyingParty relyingParty(RegistrationService credentialRepository) {
        Set<String> origins = Arrays.stream(relyingPartyOrigins.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        return RelyingParty.builder()
                .identity(RelyingPartyIdentity.builder()
                        .id(relyingPartyId)
                        .name(relyingPartyName)
                        .build())
                .credentialRepository(credentialRepository)
                .origins(origins)
                .build();
    }
}
