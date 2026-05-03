package com.locpham.bookstore.catalogservice.application.service;

import com.locpham.bookstore.catalogservice.application.port.in.AddBookUseCase;
import com.locpham.bookstore.catalogservice.application.port.in.EditBookUseCase;
import com.locpham.bookstore.catalogservice.application.port.in.ViewBookDetailUseCase;
import com.locpham.bookstore.catalogservice.application.port.in.ViewListBookUseCase;
import com.locpham.bookstore.catalogservice.application.port.out.BookRepository;
import com.locpham.bookstore.catalogservice.domain.book.Book;
import com.locpham.bookstore.catalogservice.domain.book.exception.BookAlreadyExistsException;
import com.locpham.bookstore.catalogservice.domain.book.exception.BookNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class BookCatalogService
        implements ViewListBookUseCase, ViewBookDetailUseCase, AddBookUseCase, EditBookUseCase {
    private BookRepository bookRepository;

    public BookCatalogService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public Book editBookDetails(Book book) {
        var isbn = book.isbn();

        if (!bookRepository.existsByIsbn(isbn)) {
            throw new BookNotFoundException(isbn);
        }

        var existing = bookRepository.findByIsbn(isbn).orElseThrow();
        var updated =
                new Book(
                        existing.id(),
                        isbn,
                        book.title() != null ? book.title() : existing.title(),
                        book.author() != null ? book.author() : existing.author(),
                        book.price() != null ? book.price() : existing.price(),
                        book.publisher() != null ? book.publisher() : existing.publisher(),
                        existing.auditMetadata());

        return bookRepository.save(updated);
    }

    @Override
    public void deleteBook(String isbn) {
        if (!bookRepository.existsByIsbn(isbn)) {
            throw new BookNotFoundException(isbn);
        }

        bookRepository.deleteByIsbn(isbn);
    }

    @Override
    public Book viewBookDetail(String isbn) throws BookNotFoundException {
        return bookRepository.findByIsbn(isbn).orElseThrow(() -> new BookNotFoundException(isbn));
    }

    @Override
    public Iterable<Book> viewBookList() {
        return bookRepository.findAll();
    }

    @Override
    public Book addBookToCatalog(Book book) {
        if (bookRepository.existsByIsbn(book.isbn())) {
            throw new BookAlreadyExistsException(book.isbn());
        }

        return bookRepository.save(book);
    }
}
