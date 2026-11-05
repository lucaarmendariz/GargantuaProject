package com.example.trazabilidadapi.controllers;

import com.example.trazabilidadapi.exceptions.OrderException;
import com.example.trazabilidadapi.models.AddOrderLineRequest;
import com.example.trazabilidadapi.models.CloseOrderRequest;
import com.example.trazabilidadapi.models.CreateOrderRequest;
import com.example.trazabilidadapi.models.CustomerOrder;
import com.example.trazabilidadapi.models.InUseLine;
import com.example.trazabilidadapi.models.LotTraceability;
import com.example.trazabilidadapi.models.OrderDetail;
import com.example.trazabilidadapi.services.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping("/pedidos")
    public List<CustomerOrder> list(@RequestParam(required = false) String estado,
                                    @RequestParam(required = false) Long desde,
                                    @RequestParam(required = false) Long hasta) {
        return service.list(estado, desde, hasta);
    }

    @GetMapping("/pedidos/en-uso")
    public List<InUseLine> inUse(@RequestParam(required = false) Long desde,
                                 @RequestParam(required = false) Long hasta) {
        return service.inUse(desde, hasta);
    }

    @GetMapping("/pedidos/{orderId}")
    public OrderDetail detail(@PathVariable int orderId) {
        return service.detail(orderId);
    }

    @PostMapping("/pedidos")
    public ResponseEntity<Integer> create(@RequestBody CreateOrderRequest request, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, operator(jwt)));
    }

    @PostMapping("/pedidos/{orderId}/lineas")
    public ResponseEntity<Integer> addLine(@PathVariable int orderId, @RequestBody AddOrderLineRequest request,
                                           @AuthenticationPrincipal Jwt jwt) {
        int lineId = service.addLine(orderId, request.getLotId(), request.getQuantity(), operator(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(lineId);
    }

    @DeleteMapping("/pedidos/{orderId}/lineas/{lineId}")
    public ResponseEntity<Void> removeLine(@PathVariable int orderId, @PathVariable int lineId, @AuthenticationPrincipal Jwt jwt) {
        service.removeLine(orderId, lineId, operator(jwt));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pedidos/{orderId}/cerrar")
    public ResponseEntity<Void> close(@PathVariable int orderId, @RequestBody CloseOrderRequest request, @AuthenticationPrincipal Jwt jwt) {
        service.close(orderId, request, operator(jwt));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pedidos/{orderId}/cancelar")
    public ResponseEntity<Void> cancel(@PathVariable int orderId, @AuthenticationPrincipal Jwt jwt) {
        service.cancel(orderId, operator(jwt));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/lotes/{lotId}/trazabilidad")
    public LotTraceability lotTraceability(@PathVariable int lotId) {
        return service.lotTraceability(lotId);
    }

    @ExceptionHandler(OrderException.class)
    public ResponseEntity<String> handleOrderError(OrderException e) {
        return ResponseEntity.status(e.getStatus()).body(e.getMessage());
    }

    private String operator(Jwt jwt) {
        return jwt.getClaimAsString("name");
    }
}
