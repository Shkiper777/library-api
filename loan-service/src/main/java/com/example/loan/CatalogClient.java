package com.example.loan;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/** HTTP-клиент к catalog-service. Адрес берётся из переменной окружения CATALOG_URL. */
@Component
public class CatalogClient {

    private final RestClient client;

    public CatalogClient(RestClient.Builder builder, @Value("${services.catalog-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.client = builder.requestFactory(factory).baseUrl(baseUrl).build();
    }

    /** Выдать один экземпляр книги: уменьшает copies в catalog-service. */
    public BookDto takeCopy(Long bookId) {
        try {
            return client.post().uri("/books/{id}/take", bookId).retrieve().body(BookDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book " + bookId + " not found");
        } catch (HttpClientErrorException.Conflict e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No copies of book " + bookId + " available");
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "catalog-service is unavailable");
        }
    }

    /** Вернуть один экземпляр книги: увеличивает copies в catalog-service. */
    public BookDto returnCopy(Long bookId) {
        try {
            return client.post().uri("/books/{id}/return", bookId).retrieve().body(BookDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book " + bookId + " not found");
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "catalog-service is unavailable");
        }
    }
}
