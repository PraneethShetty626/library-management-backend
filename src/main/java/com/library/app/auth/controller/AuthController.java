package com.library.app.auth.controller;

import com.library.app.auth.model.LibraryUser;
import com.library.app.auth.service.LibraryUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private LibraryUserService libraryUserService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LibraryUser user) {
        try {
            libraryUserService.register(user);
            return ResponseEntity.status(HttpStatus.CREATED).body("success");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during registration."+e);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LibraryUser user) {
        try {
            String token = libraryUserService.verify(user);
            return ResponseEntity.ok(token);
        }  catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (SecurityException | BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (LockedException e ) {
            return ResponseEntity.status(HttpStatus.LOCKED).body(e.getMessage());
        }catch (AccountExpiredException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred during login."+ e);
        }
    }

}
