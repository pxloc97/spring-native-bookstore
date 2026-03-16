package com.locpham.bookstore.orderservice.book;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ClientConfig {

    @Bean
    WebClient webClient(ClientProperties clientProperties) {
        return WebClient.builder().baseUrl(clientProperties.catalogServiceUrl()).build();
    }
}
