package com.library.app.auth.config;

import com.library.app.auth.model.LibraryUser;
import com.library.app.auth.repository.LibraryUserRepository;
import com.library.app.auth.service.LibraryUserService;
import org.hibernate.annotations.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class AdminInitializer implements CommandLineRunner {
    @Autowired
    private LibraryUserService userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            LibraryUser user = new LibraryUser();
            user.setUsername("admin");
            user.setPassword("admin123");
            user.setEnabled(true);
            user.setExpired(false);

            Set<String> roles = new HashSet<>();

            roles.add("ROLE_USER");
            roles.add("ROLE_ADMIN");

            user.setRoles(roles);

            userRepository.register(user);
        }
    }
}
