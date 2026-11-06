package com.example.trazabilidadapi.models;

import lombok.*;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class CustomerOrder {
    private int orderId;
    private String orderNumber;
    private String customerName;
    private Long scheduledDate;
    private String status;
    private String notes;
    private String createdBy;
    private String closedBy;
    private Long createdAt;
    private Long closedAt;
    private String preparedBy;
    private Long preparedAt;
    private String deliveredBy;
    private Long deliveredAt;
    /** Solo en pedidos ENTREGADO: cuándo pasará a CERRADO (epoch en segundos). */
    private Long autoCloseAt;
    private int lineCount;
}
