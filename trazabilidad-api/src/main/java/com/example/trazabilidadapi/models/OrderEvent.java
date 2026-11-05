package com.example.trazabilidadapi.models;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class OrderEvent {
    private int eventId;
    private int orderId;
    private Integer lotId;
    private String lotNumber;
    private String measureUnit;
    private String eventType;
    private BigDecimal quantity;
    private String operator;
    private String notes;
    private Long eventTimestamp;
}
