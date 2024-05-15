package com.example.models;

import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class RegistrationStartResponse {
    private String registrationId;
    private PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions;
}
