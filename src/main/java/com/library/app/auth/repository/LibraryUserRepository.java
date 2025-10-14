package com.library.app.auth.repository;

import com.library.app.auth.model.LibraryUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface LibraryUserRepository extends JpaRepository<LibraryUser, Long> {
    LibraryUser findByUsername(String username);
}