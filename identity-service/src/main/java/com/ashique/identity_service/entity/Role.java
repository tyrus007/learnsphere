package com.ashique.identity_service.entity;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Role {
    STUDENT,
    INSTRUCTOR;

    @JsonValue
    public String toJson() {
        return name();
    }
}

