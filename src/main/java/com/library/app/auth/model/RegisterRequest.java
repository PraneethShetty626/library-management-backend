package com.library.app.auth.model;


import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private Set<LibraryUserRoles> roles;
}
