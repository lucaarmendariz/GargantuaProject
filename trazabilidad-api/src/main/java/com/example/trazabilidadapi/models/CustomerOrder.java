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
    private int lineCount;
}
