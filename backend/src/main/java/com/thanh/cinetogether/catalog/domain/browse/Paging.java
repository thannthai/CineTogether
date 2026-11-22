package com.thanh.cinetogether.catalog.domain.browse;

public record Paging(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    public Paging {
        if (page < 0) {
            throw new InvalidPagingException("page must be 0 or more.");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidPagingException("size must be between 1 and " + MAX_SIZE + ".");
        }
    }

    public static Paging of(Integer page, Integer size) {
        return new Paging(page == null ? 0 : page, size == null ? DEFAULT_SIZE : size);
    }

}
