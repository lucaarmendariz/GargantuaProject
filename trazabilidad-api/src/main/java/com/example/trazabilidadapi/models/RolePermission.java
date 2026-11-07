package com.example.trazabilidadapi.models;

/** Permiso de un rol del cliente (ENCARGADO u OPERARIO) en un modulo. */
public record RolePermission(String role, String moduleCode, String access) {
}
