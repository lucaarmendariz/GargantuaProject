package com.example.trazabilidadapi.models;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class LotHistory {
    private int historyId;
    private int lotId;
    private String actionType;
    private String operator;
    private String operatorUsername;
    private Long actionTimestamp;
    private BigDecimal quantityAffected;
    private String ocrRawText;
    private String observations;
    private String orderNumber;
}