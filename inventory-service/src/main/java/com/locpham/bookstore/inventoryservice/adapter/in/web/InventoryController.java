package com.locpham.bookstore.inventoryservice.adapter.in.web;

import com.locpham.bookstore.inventoryservice.adapter.in.web.dto.StockAdjustmentRequest;
import com.locpham.bookstore.inventoryservice.application.port.in.ManageStockUseCase;
import com.locpham.bookstore.inventoryservice.domain.InventoryItem;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final ManageStockUseCase manageStockUseCase;

    public InventoryController(ManageStockUseCase manageStockUseCase) {
        this.manageStockUseCase = manageStockUseCase;
    }

    @GetMapping("/{isbn}")
    public Mono<InventoryItem> getStock(@PathVariable String isbn) {
        return manageStockUseCase.queryStock(isbn);
    }

    @GetMapping
    public Flux<InventoryItem> getStocks(@RequestParam List<String> isbn) {
        return Flux.fromIterable(isbn).flatMap(manageStockUseCase::queryStock);
    }

    @PostMapping("/{isbn}/adjust")
    public Mono<InventoryItem> adjustStock(
            @PathVariable String isbn, @RequestBody @Valid StockAdjustmentRequest request) {
        if (request.delta() >= 0) {
            return manageStockUseCase.addStock(isbn, request.delta());
        } else {
            return manageStockUseCase.reduceStock(isbn, -request.delta());
        }
    }
}
