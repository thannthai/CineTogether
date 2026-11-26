package com.thanh.cinetogether.catalog.web;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.thanh.cinetogether.catalog.domain.MovieNotFoundException;
import com.thanh.cinetogether.catalog.domain.ProviderUnavailableException;
import com.thanh.cinetogether.catalog.domain.browse.InvalidFilterException;
import com.thanh.cinetogether.catalog.domain.browse.InvalidPagingException;

@RestControllerAdvice
class CatalogExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CatalogExceptionHandler.class);

    @ExceptionHandler({ InvalidPagingException.class, InvalidFilterException.class })
    ProblemDetail invalidInput(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", "VALIDATION_ERROR");
        return problem;
    }

    @ExceptionHandler(MovieNotFoundException.class)
    ProblemDetail movieNotFound(MovieNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", "MOVIE_NOT_FOUND");
        return problem;
    }

    // The detail is fixed: the cause (a timeout, an address...) is for the logs, not for the client.
    @ExceptionHandler(ProviderUnavailableException.class)
    ProblemDetail providerUnavailable(ProviderUnavailableException exception) {
        log.warn("The movie provider is unavailable", exception);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The movie source is temporarily unavailable.");
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", "PROVIDER_UNAVAILABLE");
        return problem;
    }

}
