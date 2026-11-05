package com.example.trazabilidadapi.services;

import com.example.trazabilidadapi.exceptions.OrderException;
import com.example.trazabilidadapi.models.CloseOrderRequest;
import com.example.trazabilidadapi.models.CreateOrderRequest;
import com.example.trazabilidadapi.models.CustomerOrder;
import com.example.trazabilidadapi.models.InUseLine;
import com.example.trazabilidadapi.models.LineConsumption;
import com.example.trazabilidadapi.models.LotTraceability;
import com.example.trazabilidadapi.models.OrderDetail;
import com.example.trazabilidadapi.models.ProductLot;
import com.example.trazabilidadapi.repositories.LotHistoryRepository;
import com.example.trazabilidadapi.repositories.OrderRepository;
import com.example.trazabilidadapi.repositories.ProductLotRepository;
import com.example.trazabilidadapi.repositories.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final ProductLotRepository lots;
    private final ProductRepository products;
    private final LotHistoryRepository history;

    public OrderService(OrderRepository orders, ProductLotRepository lots, ProductRepository products, LotHistoryRepository history) {
        this.orders = orders;
        this.lots = lots;
        this.products = products;
        this.history = history;
    }

    @Transactional
    public int create(CreateOrderRequest request, String operator) {
        if (request.getOrderNumber() == null || request.getOrderNumber().isBlank()) {
            throw badRequest("El número de pedido es obligatorio.");
        }
        if (request.getScheduledDate() == null) {
            throw badRequest("La fecha programada es obligatoria.");
        }
        return orders.create(request, operator);
    }

    public List<CustomerOrder> list(String status, Long from, Long to) {
        return orders.findAll(status, from, to);
    }

    public List<InUseLine> inUse(Long from, Long to) {
        return orders.findInUse(from, to);
    }

    public OrderDetail detail(int orderId) {
        CustomerOrder order = orders.findById(orderId);
        if (order == null) {
            throw new OrderException(HttpStatus.NOT_FOUND, "El pedido no existe.");
        }
        return OrderDetail.builder()
                .order(order)
                .lines(orders.findLines(orderId))
                .events(orders.findEvents(orderId))
                .build();
    }

    @Transactional
    public int addLine(int orderId, Integer lotId, BigDecimal quantity, String operator) {
        if (lotId == null) {
            throw badRequest("Hay que indicar el lote.");
        }
        return orders.addLine(orderId, lotId, quantity, operator);
    }

    @Transactional
    public void removeLine(int orderId, int lineId, String operator) {
        orders.removeLine(orderId, lineId, operator);
    }

    /** O se registran todas las lineas y se cierra el pedido, o no cambia nada. */
    @Transactional
    public void close(int orderId, CloseOrderRequest request, String operator) {
        if (request == null || request.getLines() == null) {
            throw badRequest("Hay que indicar el consumo real de cada línea.");
        }
        for (LineConsumption line : request.getLines()) {
            if (line.getLineId() == null) {
                throw badRequest("Hay que indicar la línea.");
            }
            orders.consumeLine(orderId, line.getLineId(), line.getConsumedQuantity(), operator);
        }
        orders.close(orderId, operator);
    }

    @Transactional
    public void cancel(int orderId, String operator) {
        orders.cancel(orderId, operator);
    }

    public LotTraceability lotTraceability(int lotId) {
        ProductLot lot = lots.findById(lotId);
        if (lot == null) {
            throw new OrderException(HttpStatus.NOT_FOUND, "El lote no existe.");
        }
        return LotTraceability.builder()
                .lot(lot)
                .product(products.findById(lot.getProductId()))
                .orders(orders.findOrdersByLot(lotId))
                .history(history.findByLotId(lotId))
                .build();
    }

    private OrderException badRequest(String message) {
        return new OrderException(HttpStatus.BAD_REQUEST, message);
    }
}
