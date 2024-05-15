package com.example.repository;

import com.example.entity.Passkey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;

public interface PasskeyRepository extends JpaRepository<Passkey, Long> {
    Set<Passkey> findByUserId(Long userId);
    Passkey findByCredentialIdAndUserId(byte[] credentialId, Long userId);
    Set<Passkey> findByCredentialId(byte[] credentialId);

    // List<Passkey> findAllByAppUserId(Long userId);
    List<Passkey> findAllByUserId(Long userId);



}
