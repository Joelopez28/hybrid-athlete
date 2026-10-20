package com.ruky.training.postgrest;

import org.springframework.http.HttpStatusCode;

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
