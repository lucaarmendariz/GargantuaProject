package com.example.trazabilidadapi.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cada cierto tiempo pasa a CERRADO los pedidos entregados hace más del plazo (48 h por defecto). */
@Component
public class OrderAutoCloser {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoCloser.class);

    private final OrderService service;

    public OrderAutoCloser(OrderService service) {
        this.service = service;
    }

    @Scheduled(initialDelayString = "${app.orders.auto-close-initial-delay-ms:10000}",
            fixedDelayString = "${app.orders.auto-close-check-ms:600000}")
    public void closeDeliveredOrders() {
        try {
            int closed = service.closeDelivered();
            if (closed > 0) {
                log.info("Cierre automático: {} pedido(s) entregado(s) pasan a CERRADO", closed);
            }
        } catch (RuntimeException e) {
            // Si falla (por ejemplo, la base de datos no responde) se reintenta en la siguiente pasada
            log.warn("No se pudo ejecutar el cierre automático de pedidos", e);
        }
    }
}
