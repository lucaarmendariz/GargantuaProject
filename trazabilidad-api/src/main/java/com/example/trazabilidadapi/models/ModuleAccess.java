package com.example.trazabilidadapi.models;

/** Lo que un usuario puede hacer en un modulo: NINGUNO, VER o GESTIONAR. */
public record ModuleAccess(String moduleCode, String name, String access) {
}
