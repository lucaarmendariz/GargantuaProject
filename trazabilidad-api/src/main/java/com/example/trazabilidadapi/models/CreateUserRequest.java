package com.example.trazabilidadapi.models;

/** Alta de usuario desde el panel del cliente. */
public record CreateUserRequest(String username, String name, String role, String password) {
}
