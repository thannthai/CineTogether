package com.thanh.cinetogether.catalog.infrastructure.kkphim;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpServer;

/** A local stand-in for the provider: serves canned JSON by path and remembers every request it received. */
final class FakeProviderServer implements AutoCloseable {

    private final HttpServer server;
    private final Map<String, String> responses = new ConcurrentHashMap<>();
    private final List<URI> requests = new CopyOnWriteArrayList<>();

    FakeProviderServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", exchange -> {
            requests.add(exchange.getRequestURI());
            String json = responses.get(exchange.getRequestURI().getPath());
            byte[] body = (json == null ? "{}" : json).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(json == null ? 404 : 200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    void respond(String path, String json) {
        responses.put(path, json);
    }

    List<URI> requests() {
        return requests;
    }

    @Override
    public void close() {
        server.stop(0);
    }

}
