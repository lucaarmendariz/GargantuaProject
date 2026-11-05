package com.example.trazabilidadapi.models;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class CurrentLotStatus {
    private int productId;
    private String productSku;
    private String productName;
    private int lotId;
    private String lotNumber;
    private BigDecimal initialQuantity;
    private BigDecimal reservedQuantity;
    private BigDecimal consumedQuantity;
    private BigDecimal currentStock;
    private BigDecimal availableQuantity;
    private String measureUnit;
    private Long expirationDate;
    private String currentState;
    private Long lastUpdate;
    private String lastOperator;
    private String ocrRawText;
    private String lastObservations;

}