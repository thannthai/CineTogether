package com.thanh.cinetogether.identity.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.thanh.cinetogether.identity.domain.InvalidDisplayNameException;

@RestControllerAdvice
class IdentityExceptionHandler {

    @ExceptionHandler(InvalidDisplayNameException.class)
    ProblemDetail invalidDisplayName(InvalidDisplayNameException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setType(URI.create("about:blank"));
        problem.setProperty("code", "VALIDATION_ERROR");
        return problem;
    }

}
