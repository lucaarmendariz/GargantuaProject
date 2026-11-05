package com.example.trazabilidadapi.models;

import lombok.*;

import java.util.List;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class LotTraceability {
    private ProductLot lot;
    private Product product;
    private List<LotOrderUsage> orders;
    private List<LotHistory> history;
}
