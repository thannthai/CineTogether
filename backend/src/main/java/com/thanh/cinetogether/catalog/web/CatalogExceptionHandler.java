package com.thanh.cinetogether.catalog.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.thanh.cinetogether.catalog.domain.ProviderUnavailableException;
import com.thanh.cinetogether.catalog.domain.browse.InvalidPagingException;

@RestControllerAdvice
class CatalogExceptionHandler {

    @ExceptionHandler(InvalidPagingException.class)
    ProblemDetail invalidPaging(InvalidPagingException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", "VALIDATION_ERROR");
        return problem;
    }

    // The detail is fixed: the cause (a timeout, an address...) is for the logs, not for the client.
    @ExceptionHandler(ProviderUnavailableException.class)
    ProblemDetail providerUnavailable() {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The movie source is temporarily unavailable.");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", "PROVIDER_UNAVAILABLE");
        return problem;
    }

}
