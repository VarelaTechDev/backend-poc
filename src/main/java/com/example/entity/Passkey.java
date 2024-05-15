package com.example.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.util.Base64;

@Entity
@Table(name = "passkeys")
@Data
@NoArgsConstructor
public class Passkey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(nullable = false)
    private byte[] credentialId;

    @Lob
    @Column(nullable = false)
    private byte[] publicKeyCose;

    @Column(nullable = false)
    private Long count;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    //@JsonBackReference
    private Users user;  // Changed from `users` to `user`
}
