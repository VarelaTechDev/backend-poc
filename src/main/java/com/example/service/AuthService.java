package com.example.service;

import com.example.entity.Passkey;
import com.example.entity.Users;
import com.example.models.AssertionFinishRequest;
import com.example.models.AssertionStartResponse;
import com.example.models.RegistrationFinishRequest;
import com.example.models.RegistrationStartResponse;
import com.example.repository.PasskeyRepository;
import com.example.repository.UsersRepository;
import com.example.utils.BytesUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.bind.ValidationException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthService {

    @Autowired
    private RelyingParty relyingParty;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PasskeyRepository passkeyRepository;

    @Autowired
    private Cache<String, RegistrationStartResponse> registrationCache;

    @Autowired
    private Cache<String, AssertionStartResponse> assertionCache;

    @Autowired
    private RegistrationService registrationService;

    @PostMapping("/createUser")
    public ResponseEntity<String> createUser(@RequestParam String username) {
        Optional<Users> existingUser = usersRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            return new ResponseEntity<>("Error: User already exists", HttpStatus.BAD_REQUEST);
        }

        Users user = new Users();
        user.setUsername(username);
        try {
            usersRepository.save(user);
            return new ResponseEntity<>("User created successfully", HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("Error creating user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/registration/start")
    public ResponseEntity<RegistrationStartResponse> startRegistration(@RequestParam String username) {
        Optional<Users> existingUser = usersRepository.findByUsername(username);
        if (!existingUser.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Users user = existingUser.get();

        UserIdentity userIdentity = UserIdentity.builder()
                .name(username)
                .displayName(username)
                .id(new ByteArray(BytesUtil.longToBytes(user.getId())))
//                .id(new ByteArray(user.getId().toString().getBytes()))
                .build();

        StartRegistrationOptions startRegistrationOptions = StartRegistrationOptions.builder()
                .user(userIdentity)
                .timeout(60000)
                .build();

        PublicKeyCredentialCreationOptions publicKeyCredentialCreationOptions = relyingParty
                .startRegistration(startRegistrationOptions);

        String registrationSessionId = UUID.randomUUID().toString();

        RegistrationStartResponse registrationStartResponse = new RegistrationStartResponse(
                registrationSessionId,
                publicKeyCredentialCreationOptions
        );

        registrationCache.put(registrationStartResponse.getRegistrationId(), registrationStartResponse);

        return new ResponseEntity<>(registrationStartResponse, HttpStatus.OK);
    }

    @PostMapping("/registration/finish")
    public ResponseEntity<String> registrationFinish(@RequestBody RegistrationFinishRequest finishRequest) {
        RegistrationStartResponse startResponse = registrationCache
                .getIfPresent(finishRequest.getRegistrationId());

        registrationCache.invalidate(finishRequest.getRegistrationId());


        try {
            System.out.println("BEFORE HELL");
            System.out.println(startResponse.getPublicKeyCredentialCreationOptions());
            System.out.println(finishRequest.getCredential().toString());

            RegistrationResult registrationResult = relyingParty
                    .finishRegistration(FinishRegistrationOptions.builder()
                            .request(startResponse.getPublicKeyCredentialCreationOptions())
                            .response(finishRequest.getCredential())
                            .build());

            System.out.println("WE MADE IT");

            UserIdentity userIdentity = startResponse.getPublicKeyCredentialCreationOptions().getUser();
            //long userId = Long.parseLong(new String(userIdentity.getId().getBytes()));
            //System.out.println("UserId is: " + userId);
            Long id = BytesUtil.bytesToLong(userIdentity.getId().getBytes());
//            System.out.println("After we ran util method" + BytesUtil.bytesToLong(userIdentity.getId().getBytes()));
            registrationService.addCredential(
                    id,
                    registrationResult.getKeyId().getId().getBytes(),
                    registrationResult.getPublicKeyCose().getBytes(),
                    finishRequest.getCredential().getResponse().getParsedAuthenticatorData().getSignatureCounter());

//
//                Users user = usersRepository.findById(userId).orElseThrow(() ->
//                        new RuntimeException("User not found with ID: " + userId));
//
//                Passkey passkey = new Passkey();
//                passkey.setUser(user);
//                passkey.setCredentialId(registrationResult.getKeyId().getId().getBytes());
//                passkey.setPublicKeyCose(registrationResult.getPublicKeyCose().getBytes());
//                passkey.setCount(finishRequest.getCredential().getResponse().getParsedAuthenticatorData().getSignatureCounter());
//
//                passkeyRepository.save(passkey);

            return new ResponseEntity<>("Registration completed successfully", HttpStatus.OK);
        } catch (Exception e) {
            System.out.println("ERROR");
            System.out.println(e.getMessage());
            return new ResponseEntity<>("Registration failed: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    private SecureRandom random = new SecureRandom();


    @PostMapping("/assertion/start")
    public ResponseEntity<AssertionStartResponse> start(@RequestParam String username) {
        Optional<Users> existingUser = usersRepository.findByUsername(username);
        if (!existingUser.isPresent()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Users user = existingUser.get();
        System.out.println("User found: " + user.getId());

        byte[] assertionId = new byte[16];
        this.random.nextBytes(assertionId);
        String assertionIdBase64 = Base64.getEncoder().encodeToString(assertionId);

        StartAssertionOptions.StartAssertionOptionsBuilder userVerificationBuilder = StartAssertionOptions.builder()
                .userVerification(UserVerificationRequirement.PREFERRED)
                .timeout(60000)
                .username(username);

        AssertionRequest assertionRequest = this.relyingParty.startAssertion(userVerificationBuilder.build());

        AssertionStartResponse response = new AssertionStartResponse(
                assertionIdBase64,
                assertionRequest);

        assertionCache.put(response.getAssertionId(), response);

        System.out.println("AssertionStartResponse created with ID: " + response.getAssertionId());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/assertion/finish")
    public ResponseEntity<String> finish(@RequestBody AssertionFinishRequest finishRequest) {
        // Retrieve the assertion start response from the cache
        AssertionStartResponse startResponse = this.assertionCache.getIfPresent(finishRequest.getAssertionId());
        this.assertionCache.invalidate(finishRequest.getAssertionId());

        if (startResponse == null) {
            System.out.println("No start response found for assertion ID: " + finishRequest.getAssertionId());
            return new ResponseEntity<>("Assertion ID not found", HttpStatus.NOT_FOUND);
        }

        try {
            // Complete the assertion
            AssertionResult result = this.relyingParty.finishAssertion(
                    FinishAssertionOptions.builder()
                            .request(startResponse.getAssertionRequest())
                            .response(finishRequest.getCredential())
                            .build());

            // Check if the assertion was successful
            if (result.isSuccess()) {
                // Update the signature count in the credential repository
                if (!this.registrationService.updateSignatureCount(result)) {
                    System.out.println("Failed to update signature count for user: " + result.getUsername() + ", credential: " + finishRequest.getCredential().getId());
                    return new ResponseEntity<>("Failed to update signature count", HttpStatus.INTERNAL_SERVER_ERROR);
                }

                System.out.println("Assertion successful for user: " + result.getUsername());
                return new ResponseEntity<>("Assertion successful", HttpStatus.OK);
            } else {
                System.out.println("Assertion failed for user: " + result.getUsername());
                return new ResponseEntity<>("Assertion failed", HttpStatus.UNAUTHORIZED);
            }
        } catch (AssertionFailedException e) {
            System.out.println("Assertion failed: " + e.getMessage());
            return new ResponseEntity<>("Assertion failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}