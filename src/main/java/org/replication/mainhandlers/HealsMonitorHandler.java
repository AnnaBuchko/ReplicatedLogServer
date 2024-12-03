package org.replication.mainhandlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.replication.scheduler.SecondaryHealthMonitor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public class HealsMonitorHandler implements HttpHandler {
    private final Map<String, SecondaryHealthMonitor.ServerStatus> secondaryHealth;

    public HealsMonitorHandler(Map<String, SecondaryHealthMonitor.ServerStatus> secondaryHealth) {
        this.secondaryHealth = secondaryHealth;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        StringBuilder response = new StringBuilder();
        for (Map.Entry<String, SecondaryHealthMonitor.ServerStatus> entry : secondaryHealth.entrySet()) {
            response.append(entry.getKey()).append(" -> ").append(entry.getValue()).append("\n");
        }

        exchange.sendResponseHeaders(200, response.toString().getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.toString().getBytes());
        os.close();
    }
}
