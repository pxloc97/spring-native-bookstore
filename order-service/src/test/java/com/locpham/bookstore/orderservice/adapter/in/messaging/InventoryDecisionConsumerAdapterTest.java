package com.locpham.bookstore.orderservice.adapter.in.messaging;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.locpham.bookstore.orderservice.application.port.in.ProcessInventoryDecisionUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

@SpringBootTest(classes = InventoryDecisionConsumerAdapterTest.TestApp.class)
class InventoryDecisionConsumerAdapterTest {

    @Autowired private InputDestination input;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean private ProcessInventoryDecisionUseCase processInventoryDecisionUseCase;

    @Test
    void consumeInventoryDecision_shouldDelegateToUseCase() throws Exception {
        given(
                        processInventoryDecisionUseCase.processDecision(
                                42L, ProcessInventoryDecisionUseCase.DecisionStatus.RESERVED))
                .willReturn(Mono.empty());

        var payload = new InventoryDecisionMessage(42L, "RESERVED", null);

        input.send(
                MessageBuilder.withPayload(objectMapper.writeValueAsBytes(payload))
                        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .build(),
                "handleInventoryDecision-in-0");

        // The consumer subscribes internally, so verify the use case is called.
        verify(processInventoryDecisionUseCase)
                .processDecision(42L, ProcessInventoryDecisionUseCase.DecisionStatus.RESERVED);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(
            excludeName = {
                "org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration",
                "org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcAutoConfiguration",
                "org.springframework.boot.data.r2dbc.autoconfigure.DataR2dbcRepositoriesAutoConfiguration",
                "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration",
                "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
                "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration"
            })
    @Import({TestChannelBinderConfiguration.class, InventoryDecisionConsumerAdapter.class})
    static class TestApp {}
}
