package com.thanh.cinetogether.catalog.domain;

public class InvalidPagingException extends RuntimeException {

    public InvalidPagingException(String message) {
        super(message);
    }

}
