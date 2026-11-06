package com.example.trazabilidadapi.models;

import lombok.*;


@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class Product {
    private int productId;
    private String sku;
    /** Código de barras comercial (EAN-8/13, UPC o GTIN-14), guardado siempre con 14 dígitos. */
    private String gtin;
    private String name;
    private String description;
    private boolean isActive;
    private Long createdAt;
    private Long updatedAt;
}