package com.library.app.auth.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Generated;
import lombok.Getter;

import java.util.Set;

@Entity(name = "users")
@Getter
@Data
public class LibraryUser {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column
    private boolean expired = false;

    @Column(columnDefinition = "boolean default true")
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles;
}
