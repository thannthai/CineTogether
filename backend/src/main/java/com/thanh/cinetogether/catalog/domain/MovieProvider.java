package com.thanh.cinetogether.catalog.domain;

public interface MovieProvider {

    String key();

    MoviePage browse(BrowseQuery query);

}
