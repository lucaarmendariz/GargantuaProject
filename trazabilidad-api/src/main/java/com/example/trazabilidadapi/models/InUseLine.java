package com.example.trazabilidadapi.models;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class InUseLine {
    private int lineId;
    private int orderId;
    private String orderNumber;
    private String customerName;
    private int lotId;
    private String lotNumber;
    private String productName;
    private String productSku;
    private String measureUnit;
    private BigDecimal reservedQuantity;
    private BigDecimal currentStock;
    private BigDecimal availableQuantity;
    private Long expirationDate;
    private Long reservedAt;
}
