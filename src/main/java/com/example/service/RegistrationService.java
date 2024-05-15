package com.example.service;

import com.example.entity.Passkey;
import com.example.entity.Users;
import com.example.repository.PasskeyRepository;
import com.example.repository.UsersRepository;
import com.example.utils.BytesUtil;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.AuthenticatorTransport;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import lombok.Data;
import lombok.Getter;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Data
@Getter
public class RegistrationService implements CredentialRepository {

    @Autowired
    private UsersRepository userRepository;

    @Autowired
    private PasskeyRepository passkeyRepository;

    @Transactional
    public void addCredential(long userId, byte[] credentialIdBytes, byte[] publicKeyCoseBytes, long counter) {
        // Find the user by ID
        Optional<Users> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            throw new RuntimeException("User not found with ID: " + userId);
        }
        Users user = userOptional.get();

        // Create a new Passkey instance
        Passkey passkey = new Passkey();
        passkey.setUser(user);  // Set the user, not the user ID
        passkey.setCredentialId(credentialIdBytes);  // Set credentialId
        passkey.setPublicKeyCose(publicKeyCoseBytes);  // Set publicKeyCose
        passkey.setCount(counter);  // Set count

        // Save the passkey
        passkeyRepository.save(passkey);
    }


    // Registration phase
//    @Override
//    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
//        System.out.println("INVOKED: GET CREDENTIAL WAS INVOKED for username: " + username);
//
//        Optional<Users> usersOptional = userRepository.findByUsername(username);
//        if (usersOptional.isEmpty()) {
//            throw new RuntimeException("Username not found: " + username);
//        }
//        Users user = usersOptional.get();
//
//        // Found 8 users?
//        System.out.println("User id found: " + user.getId());
//        System.out.println("Passkey size " + passkeyRepository.findByUserId(user.getId()).size());
//        System.out.println("Passkey found " + passkeyRepository.findByUserId(user.getId()));
//
//        Set<PublicKeyCredentialDescriptor> credentials = passkeyRepository.findByUserId(user.getId())
//                .stream()
//                .map(passkey -> {
//                    System.out.println("Processing passkey: " + passkey.getId());
//                    return PublicKeyCredentialDescriptor.builder()
//                            .id(new ByteArray(passkey.getCredentialId())).build();
//                })
//                .collect(Collectors.toSet());
//
//        System.out.println("Credentials found: " + credentials.size());
//        return credentials;
//    }
    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        System.out.println("INVOKED: GET CREDENTIAL WAS INVOKED");

        Optional<Users> usersOptional = userRepository.findByUsername(username);
        if (usersOptional.isEmpty()) {
            throw new RuntimeException("Username not found: " + username);
        }
        Users user = usersOptional.get();
        System.out.println("User found: " + user.getId());

        return passkeyRepository.findByUserId(user.getId())
                .stream()
                .map(passkey -> {
                    System.out.println("Processing passkey: " + passkey.getId());
                    return PublicKeyCredentialDescriptor.builder()
                            .id(new ByteArray(passkey.getCredentialId()))
                            .transports(Collections.emptySet())  // Ensure transports is set)
                            .build();
                })
                .collect(Collectors.toSet());
    }



    // Registration and authentication phases
    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        System.out.println("INVOKED: GET USER HANDLE WAS INVOKED");

        Optional<Users> usersOptional = userRepository.findByUsername(username);

        if (usersOptional.isEmpty()) {
            throw new RuntimeException("Username not found: " + username);
        }
        Users user = usersOptional.get();

        // return Optional.of(new ByteArray(user.getId().toString().getBytes()));
        return Optional.of(new ByteArray(BytesUtil.longToBytes(user.getId())));
    }

    // Authentication Phase
    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        System.out.println("INVOKED: GET USERNAME WAS INVOKED");
        long id = BytesUtil.bytesToLong(userHandle.getBytes());
        Optional<Users> user = userRepository.findById(id);

        if (user.isEmpty()) {
            throw new RuntimeException("User not found with ID: " + id);
        }

        return Optional.of(user.get().getUsername());
    }

    // Authentication Phase
    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        System.out.println("INVOKED: LOOKUP WAS INVOKED");

        long id = BytesUtil.bytesToLong(userHandle.getBytes());

        return userRepository.findById(id)
                .flatMap(user -> Optional.ofNullable(passkeyRepository.findByCredentialIdAndUserId(credentialId.getBytes(), user.getId())))
                .map(passkey -> RegisteredCredential.builder()
                        .credentialId(new ByteArray(passkey.getCredentialId()))
                        .userHandle(userHandle)
                        .publicKeyCose(new ByteArray(passkey.getPublicKeyCose()))
                        .signatureCount(passkey.getCount())
                        .build());
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        System.out.println("INVOKED: LOOKUP ALL WAS INVOKED");

        return passkeyRepository.findByCredentialId(credentialId.getBytes())
                .stream()
                .map(passkey -> RegisteredCredential.builder()
                        .credentialId(new ByteArray(passkey.getCredentialId()))
                        .userHandle(new ByteArray(passkey.getUser().getId().toString().getBytes()))
                        .publicKeyCose(new ByteArray(passkey.getPublicKeyCose()))
                        .signatureCount(passkey.getCount())
                        .build())
                .collect(Collectors.toSet());
    }

    @Transactional
    public boolean updateSignatureCount(AssertionResult result) {
        long userId = BytesUtil.bytesToLong(result.getUserHandle().getBytes());
        Passkey passkey = passkeyRepository.findByCredentialIdAndUserId(result.getCredentialId().getBytes(), userId);

        if (passkey == null) {
            System.out.println("Passkey not found for user ID: " + userId);
            return false;
        }

        passkey.setCount(result.getSignatureCount());
        passkeyRepository.save(passkey);
        return true;
    }


}
