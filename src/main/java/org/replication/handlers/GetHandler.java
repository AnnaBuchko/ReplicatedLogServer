package org.replication.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.*;

public class GetHandler implements HttpHandler {
    private final List<String> messages;

    public GetHandler(List<String> messages) {
        this.messages = messages;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String response = "Stored messages:\n" + join("\n", messages);
        exchange.sendResponseHeaders(200, response.getBytes(UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(UTF_8));
        }
    }
}
