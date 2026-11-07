package com.example.trazabilidadapi.models;

/** Usuario del cliente tal y como se lista en el panel (sin la contraseña). */
public record UserSummary(int userId, String username, String name, String role, boolean active, Long createdAt) {
}
