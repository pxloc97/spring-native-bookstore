package com.locpham.bookstore.orderservice.web;

import com.locpham.bookstore.orderservice.domain.Order;
import com.locpham.bookstore.orderservice.domain.OrderService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Flux<Order> getAllOrders() {
        return orderService.findAll();
    }

    @PostMapping
    public Mono<Order> submitOrder(@RequestBody OrderRequest orderRequest) {
        return orderService.submitOrder(orderRequest.isbn(), orderRequest.quantity());
    }
}
