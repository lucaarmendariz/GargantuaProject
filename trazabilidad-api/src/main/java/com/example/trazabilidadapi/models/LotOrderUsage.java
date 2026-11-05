package com.example.trazabilidadapi.models;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class LotOrderUsage {
    private int orderId;
    private String orderNumber;
    private String customerName;
    private String orderStatus;
    private Long scheduledDate;
    private BigDecimal reservedQuantity;
    private BigDecimal consumedQuantity;
}
