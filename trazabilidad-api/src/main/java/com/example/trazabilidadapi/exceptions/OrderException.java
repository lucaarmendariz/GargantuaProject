package com.example.trazabilidadapi.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Regla de negocio incumplida en un pedido; el mensaje se muestra al usuario. */
@Getter
public class OrderException extends RuntimeException {

    private final HttpStatus status;

    public OrderException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
