package com.example.trazabilidadapi.models;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class OrderLine {
    private int lineId;
    private int orderId;
    private int lotId;
    private String lotNumber;
    private String productName;
    private String productSku;
    private String measureUnit;
    private BigDecimal reservedQuantity;
    private BigDecimal consumedQuantity;
}
