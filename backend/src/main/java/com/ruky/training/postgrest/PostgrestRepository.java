package com.ruky.training.postgrest;

import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Shared plumbing for the PostgREST repositories. Column naming, pagination
 * headers and upstream errors stay in this layer.
 */
public abstract class PostgrestRepository {

    private static final Logger log = LoggerFactory.getLogger(PostgrestRepository.class);

    protected final RestClient client;

    protected PostgrestRepository(RestClient client) {
        this.client = client;
    }

    protected <T> T call(Supplier<T> action) {
        try {
            return action.get();
        } catch (RestClientResponseException ex) {
            log.warn("PostgREST replied {} {}: {}",
                    ex.getStatusCode().value(),
                    ex.getStatusText(),
                    ex.getResponseBodyAsString());
            throw new PostgrestException(ex.getStatusCode(),
                    "El servicio de datos rechazó la operación", ex);
        } catch (ResourceAccessException ex) {
            log.warn("PostgREST is unreachable: {}", ex.getMessage());
            throw new PostgrestException(HttpStatusCode.valueOf(502),
                    "El servicio de datos no está disponible", ex);
        }
    }

    protected static long parseTotal(HttpHeaders headers, int fallback) {
        String header = headers.getFirst("Content-Range");
        if (header == null) {
            return fallback;
        }
        int slash = header.lastIndexOf('/');
        if (slash < 0) {
            return fallback;
        }
        String rawTotal = header.substring(slash + 1).trim();
        if (rawTotal.isEmpty() || "*".equals(rawTotal)) {
            return fallback;
        }
        try {
            return Long.parseLong(rawTotal);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
