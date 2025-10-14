package com.library.app.auth.service;

import com.library.app.auth.config.JwtFilter;
import com.library.app.auth.model.LibraryUser;
import com.library.app.auth.repository.LibraryUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class LibraryUserService {

    private static final Logger logger = LoggerFactory.getLogger(LibraryUserService.class);

    @Autowired
    private LibraryUserRepository userRepository;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private AuthenticationManager authManager;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public void register(LibraryUser user) {
        if (userRepository.findByUsername(user.getUsername()) != null) {
            throw new IllegalArgumentException("User already exists");
        }

        user.setPassword(encoder.encode(user.getPassword()));
        logger.info("Registering user: {}", user.toString());

        userRepository.save(user);
    }

    public String verify(LibraryUser user) {
        Authentication authentication = authManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        if (authentication.isAuthenticated()) {

            user.setRoles(userRepository.findByUsername(user.getUsername()).getRoles());

            return jwtService.generateToken(user.getUsername(), user.getRoles());
        } else {
            throw new SecurityException("Invalid login credentials");
        }
    }

    public long count() {
        return  userRepository.count();
    }
}
