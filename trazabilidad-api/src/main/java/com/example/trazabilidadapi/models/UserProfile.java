package com.example.trazabilidadapi.models;

/** Perfil del usuario conectado. avatar: foto como data URL, o null si no tiene. */
public record UserProfile(String username, String name, String role, String avatar, Long createdAt) {
}
