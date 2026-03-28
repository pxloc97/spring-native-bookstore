package com.locpham.bookstore.catalogservice.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.locpham.bookstore.catalogservice.domain.Book;
import com.locpham.bookstore.catalogservice.domain.BookNotFoundException;
import com.locpham.bookstore.catalogservice.domain.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
public class BookControllerMvcTests {

    @Autowired private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockitoBean private BookService bookService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void whenGetBookNotFoundThenShouldReturn404() throws Exception {
        String isbn = "1234567890";
        given(bookService.viewBookDetail(isbn)).willThrow(new BookNotFoundException(isbn));
        mockMvc.perform(get("/books/" + isbn)).andExpect(status().isNotFound());
    }

    @Test
    void whenCreateBookWithoutTokenThenShouldReturn401() throws Exception {
        var payload = """
                {"isbn":"1234567890","title":"Title","author":"Author","price":10.0,"publisher":"Polarsophia"}
                """;
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenCreateBookWithCustomerRoleThenShouldReturn403() throws Exception {
        var payload = """
                {"isbn":"1234567890","title":"Title","author":"Author","price":10.0,"publisher":"Polarsophia"}
                """;
        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_customer"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenCreateBookWithEmployeeRoleThenShouldReturn201() throws Exception {
        var book = Book.build("1234567890", "Title", "Author", 10.0, "Polarsophia");
        var payload = """
                {"isbn":"1234567890","title":"Title","author":"Author","price":10.0,"publisher":"Polarsophia"}
                """;
        given(bookService.addBookToCatalog(book)).willReturn(book);

        mockMvc.perform(post("/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_employee"))))
                .andExpect(status().isCreated());
    }
}
