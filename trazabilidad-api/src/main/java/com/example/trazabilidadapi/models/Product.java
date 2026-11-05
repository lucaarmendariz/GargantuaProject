package com.example.trazabilidadapi.models;

import lombok.*;


@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class Product {
    private int productId;
    private String sku;
    private String name;
    private String description;
    private boolean isActive;
    private Long createdAt;
    private Long updatedAt;
}