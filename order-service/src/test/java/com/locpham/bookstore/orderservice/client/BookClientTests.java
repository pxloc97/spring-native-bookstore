package com.locpham.bookstore.orderservice.client;

import com.locpham.bookstore.orderservice.book.BookClient;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

public class BookClientTests {

    private MockWebServer mockWebServer;
    private BookClient bookClient;

    @BeforeEach
    void setUp() throws IOException {
        this.mockWebServer = new MockWebServer();
        this.mockWebServer.start();
        var webClient =
                WebClient.builder().baseUrl(mockWebServer.url("/").uri().toString()).build();
        this.bookClient = new BookClient(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        this.mockWebServer.shutdown();
    }

    @Test
    void whenBookExists_thenReturnBook() {
        var bookIsbn = "1234567890";

        var mockResponse =
                new MockResponse()
                        .addHeader("Content-Type", "application/json")
                        .setBody(
                                """
                        {
                            "isbn": "%s",
                            "title": "Book Title",
                            "author": "Book Author",
                            "price": 10.99
                        }
                        """
                                        .formatted(bookIsbn));

        this.mockWebServer.enqueue(mockResponse);

        var book = this.bookClient.getBookByIsbn(bookIsbn);

        StepVerifier.create(book)
                .expectNextMatches(b -> b.isbn().equals(bookIsbn))
                .verifyComplete();
    }
}
