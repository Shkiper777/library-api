package com.example.loan;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/** HTTP-клиент к reader-service. Адрес берётся из переменной окружения READER_URL. */
@Component
public class ReaderClient {

    private final RestClient client;

    public ReaderClient(RestClient.Builder builder, @Value("${services.reader-url}") String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.client = builder.requestFactory(factory).baseUrl(baseUrl).build();
    }

    /** Проверить, что читатель существует, и получить его данные. */
    public ReaderDto getReader(Long readerId) {
        try {
            return client.get().uri("/readers/{id}", readerId).retrieve().body(ReaderDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reader " + readerId + " not found");
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "reader-service is unavailable");
        }
    }
}
