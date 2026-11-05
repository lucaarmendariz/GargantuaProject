package com.example.trazabilidadapi.models;

import lombok.*;

import java.util.List;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class OrderDetail {
    private CustomerOrder order;
    private List<OrderLine> lines;
    private List<OrderEvent> events;
}
