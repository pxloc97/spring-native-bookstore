package com.locpham.bookstore.catalogservice.demo;

import com.locpham.bookstore.catalogservice.application.port.in.AddBookUseCase;
import com.locpham.bookstore.catalogservice.application.port.out.BookRepository;
import com.locpham.bookstore.catalogservice.domain.book.Book;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("test-data")
public class BookDataLoader {
    private final AddBookUseCase addBookUseCase;
    private final BookRepository bookRepository;

    public BookDataLoader(AddBookUseCase addBookUseCase, BookRepository bookRepository) {
        this.addBookUseCase = addBookUseCase;
        this.bookRepository = bookRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void loadBookTestData() {
        bookRepository.deleteByIsbn("1234567891");
        bookRepository.deleteByIsbn("1234567892");
        var book1 =
                Book.build(
                        "1234567891",
                        "Northern Lights",
                        "Lyra Silvertongue",
                        9.90,
                        "Northlight Press");
        var book2 =
                Book.build("1234567892", "Polar Journey", "Iorek Polarson", 12.90, "Polarsophia");
        addBookUseCase.addBookToCatalog(book1);
        addBookUseCase.addBookToCatalog(book2);
    }
}
