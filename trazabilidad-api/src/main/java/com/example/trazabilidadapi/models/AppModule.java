package com.example.trazabilidadapi.models;

/** Modulo de la app y si el cliente lo tiene contratado. */
public record AppModule(String moduleCode, String name, boolean enabled) {
}
