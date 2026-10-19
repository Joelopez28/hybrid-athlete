package com.example.taskflow.postgrest;

import org.springframework.http.HttpStatusCode;

/**
 * A non-2xx answer from PostgREST, translated at the repository boundary so the
 * rest of the application never depends on HTTP client types.
 */
public class PostgrestException extends RuntimeException {

    private final HttpStatusCode upstreamStatus;

    public PostgrestException(HttpStatusCode upstreamStatus, String message, Throwable cause) {
        super(message, cause);
        this.upstreamStatus = upstreamStatus;
    }

    public HttpStatusCode getUpstreamStatus() {
        return upstreamStatus;
    }
}
