package com.library.app.auth.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum LibraryUserRoles {
    ROLE_USER("ROLE_USER"),
    ROLE_ADMIN("ROLE_ADMIN");

    private final String value;

    LibraryUserRoles(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
