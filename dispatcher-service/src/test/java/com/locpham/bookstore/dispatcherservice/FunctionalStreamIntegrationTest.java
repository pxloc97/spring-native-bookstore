package com.locpham.bookstore.dispatcherservice;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.locpham.bookstore.dispatcherservice.message.OrderAcceptedMessage;
import com.locpham.bookstore.dispatcherservice.message.OrderDispatchedMessage;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@Import(TestChannelBinderConfiguration.class)
public class FunctionalStreamIntegrationTest {
    @Autowired private InputDestination input;

    @Autowired private OutputDestination output;

    @Autowired private ObjectMapper objectMapper;

    @Test
    void whenOrderAcceptedThenDispatched() throws IOException {
        long orderId = 121;

        Message<OrderAcceptedMessage> inputMessage =
                MessageBuilder.withPayload(new OrderAcceptedMessage(orderId)).build();

        Message<OrderDispatchedMessage> outputMessage =
                MessageBuilder.withPayload(new OrderDispatchedMessage(orderId)).build();

        this.input.send(inputMessage);

        assertThat(
                        objectMapper.readValue(
                                output.receive().getPayload(), OrderDispatchedMessage.class))
                .isEqualTo(outputMessage.getPayload());
    }
}
