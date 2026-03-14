package com.locpham.bookstore.catalogservice.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;

@SpringBootTest
class BookRepositoryJdbcTests {

    @Autowired private BookRepository bookRepository;

    @Autowired private JdbcAggregateTemplate jdbcAggregateTemplate;

    @Test
    void findBookByIsbnWhenExisting() {
        var bookIsbn = "1234561235";
        var book = Book.build(bookIsbn, "Title", "Author", 12.90, "Polarsophia");
        var expectedBook = jdbcAggregateTemplate.insert(book);

        var actualBook = bookRepository.findByIsbn(bookIsbn);

        assertThat(actualBook).isPresent();
        assertThat(actualBook.get().createdDate()).isNotNull();
        assertThat(actualBook.get().lastModifiedDate()).isNotNull();
        assertThat(actualBook.get())
                .usingRecursiveComparison()
                .ignoringFields("createdDate", "lastModifiedDate")
                .isEqualTo(expectedBook);
    }
}
