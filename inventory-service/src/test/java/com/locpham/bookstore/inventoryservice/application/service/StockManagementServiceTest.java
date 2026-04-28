package com.locpham.bookstore.inventoryservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.locpham.bookstore.inventoryservice.application.port.out.InventoryPort;
import com.locpham.bookstore.inventoryservice.domain.InventoryItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class StockManagementServiceTest {

    @Mock private InventoryPort inventoryPort;

    @InjectMocks private StockManagementService stockManagementService;

    @Test
    void addStock_shouldCreateNewItemWhenNotFound() {
        given(inventoryPort.findByIsbn("123")).willReturn(Mono.empty());
        given(inventoryPort.save(any())).willAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(stockManagementService.addStock("123", 10))
                .assertNext(item -> assertThat(item.availableQuantity()).isEqualTo(10))
                .verifyComplete();
    }

    @Test
    void addStock_shouldIncrementExistingStock() {
        InventoryItem existing = new InventoryItem(1L, "123", 5, 0, 0);
        given(inventoryPort.findByIsbn("123")).willReturn(Mono.just(existing));
        given(inventoryPort.save(any())).willAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(stockManagementService.addStock("123", 3))
                .assertNext(item -> assertThat(item.availableQuantity()).isEqualTo(8))
                .verifyComplete();
    }

    @Test
    void reduceStock_shouldDecrementStock() {
        InventoryItem existing = new InventoryItem(1L, "123", 10, 0, 0);
        given(inventoryPort.findByIsbn("123")).willReturn(Mono.just(existing));
        given(inventoryPort.save(any())).willAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(stockManagementService.reduceStock("123", 3))
                .assertNext(item -> assertThat(item.availableQuantity()).isEqualTo(7))
                .verifyComplete();
    }

    @Test
    void reduceStock_shouldFailWhenNotFound() {
        given(inventoryPort.findByIsbn("123")).willReturn(Mono.empty());

        StepVerifier.create(stockManagementService.reduceStock("123", 3))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void queryStock_shouldReturnItem() {
        InventoryItem existing = new InventoryItem(1L, "123", 10, 0, 0);
        given(inventoryPort.findByIsbn("123")).willReturn(Mono.just(existing));

        StepVerifier.create(stockManagementService.queryStock("123"))
                .assertNext(item -> assertThat(item.isbn()).isEqualTo("123"))
                .verifyComplete();
    }
}
