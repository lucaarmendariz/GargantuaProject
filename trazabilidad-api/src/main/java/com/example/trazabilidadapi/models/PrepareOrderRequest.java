package com.example.trazabilidadapi.models;

import lombok.*;

import java.util.List;

@Builder
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class PrepareOrderRequest {
    private List<LineConsumption> lines;
}
