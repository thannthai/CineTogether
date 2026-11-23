package com.thanh.cinetogether.catalog.infrastructure;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sun.net.httpserver.HttpServer;

/** A local stand-in for the provider: serves canned replies by path and remembers every request it received. */
final class FakeProviderServer implements AutoCloseable {

    private record Reply(int status, String body, long delayMillis) {
    }

    private final HttpServer server;
    private final Map<String, Reply> replies = new ConcurrentHashMap<>();
    private final List<URI> requests = new CopyOnWriteArrayList<>();

    FakeProviderServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/", exchange -> {
            requests.add(exchange.getRequestURI());
            Reply reply = replies.getOrDefault(exchange.getRequestURI().getPath(), new Reply(404, "{}", 0));
            try {
                Thread.sleep(reply.delayMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] body = reply.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(reply.status(), body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    void respond(String path, String json) {
        replies.put(path, new Reply(200, json, 0));
    }

    void respond(String path, int status, String body) {
        replies.put(path, new Reply(status, body, 0));
    }

    void respondSlowly(String path, String json, long delayMillis) {
        replies.put(path, new Reply(200, json, delayMillis));
    }

    List<URI> requests() {
        return requests;
    }

    @Override
    public void close() {
        server.stop(0);
    }

}
