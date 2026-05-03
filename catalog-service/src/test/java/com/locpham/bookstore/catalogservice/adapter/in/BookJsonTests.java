package com.locpham.bookstore.catalogservice.adapter.in;

import static org.assertj.core.api.Assertions.assertThat;

import com.locpham.bookstore.catalogservice.adapter.in.dto.BookRequest;
import com.locpham.bookstore.catalogservice.adapter.in.dto.BookResponse;
import com.locpham.bookstore.catalogservice.domain.book.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
class BookJsonTests {
    @Autowired private JacksonTester<BookRequest> bookRequestJson;
    @Autowired private JacksonTester<BookResponse> bookResponseJson;

    @Test
    void testSerializeBookResponse() throws Exception {
        var book = Book.build("1234567890", "Title", "Author", 9.90, "Polarsophia");
        var response = BookResponse.fromDomain(book);
        var jsonContent = bookResponseJson.write(response);
        assertThat(jsonContent).extractingJsonPathStringValue("@.isbn").isEqualTo(book.isbn());
        assertThat(jsonContent).extractingJsonPathStringValue("@.title").isEqualTo(book.title());
        assertThat(jsonContent).extractingJsonPathStringValue("@.author").isEqualTo(book.author());
        assertThat(jsonContent).extractingJsonPathNumberValue("@.price").isEqualTo(book.price());
        assertThat(jsonContent)
                .extractingJsonPathStringValue("@.publisher")
                .isEqualTo(book.publisher());
    }

    @Test
    void testDeserializeBookRequest() throws Exception {
        var content =
                """
            {
                "isbn": "1234567890",
                "title": "Title",
                "author": "Author",
                "price": 9.90,
                "publisher": "Polarsophia"
            }
            """;
        var request = bookRequestJson.parseObject(content);
        assertThat(request.isbn()).isEqualTo("1234567890");
        assertThat(request.title()).isEqualTo("Title");
        assertThat(request.author()).isEqualTo("Author");
        assertThat(request.price()).isEqualTo(9.90);
        assertThat(request.publisher()).isEqualTo("Polarsophia");
    }
}
