package org.replication.mainhandlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.SortedMap;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RecoveryHandler implements HttpHandler {
    private final SortedMap<Integer, String> messages;

    public RecoveryHandler(SortedMap<Integer, String> messages) {
        this.messages = messages;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        StringBuilder response = new StringBuilder();
        for (Map.Entry<Integer, String> message : messages.entrySet()) {
            response.append(message.getKey())
                    .append(":")
                    .append(message.getValue())
                    .append("\n");
        }
        exchange.sendResponseHeaders(200, response.toString().getBytes(UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.toString().getBytes(UTF_8));
        }
    }
}
