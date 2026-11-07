package com.example.trazabilidadapi.models;

/** Cambio de rol o de estado de un usuario desde el panel del cliente. */
public record UpdateUserRequest(String role, Boolean active) {
}
