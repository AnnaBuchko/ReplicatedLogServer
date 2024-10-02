package org.replication.handlers;

import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MainPostHandler extends PostHandler{
    private final List<String> secondaryAddresses;
    private static final Logger logger = LogManager.getLogger(MainPostHandler.class);

    public MainPostHandler(List<String> messages, List<String> secondaryAddresses) {
        super(messages);
        this.secondaryAddresses = secondaryAddresses;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            String requestBody = readAndSaveMessage(exchange);
            // send message to secondary servers
            secondaryAddresses.forEach(secondaryAddress ->
                    sendToSecondary(secondaryAddress, requestBody));

            // Prepare the response
            String response = "Data received successfully!";
            exchange.sendResponseHeaders(200, response.getBytes(UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(UTF_8));
            }
        } else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }

    public void sendToSecondary(String address, String message) {
        try {
            HttpURLConnection connection = getHttpURLConnection(address);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(message.getBytes());
                os.flush();
            }
            // get ACK from secondaries server
            int responseCode = connection.getResponseCode();
            if (responseCode == 200){
                logger.info("Message sent to {} ",address);
            }
            connection.disconnect();
        } catch (Exception ex) {
            logger.error("Got error from {}: {}", address, ex.getMessage());
        }
    }

    private static HttpURLConnection getHttpURLConnection(String address) throws IOException {
        URL url = new URL(address + "/saveData");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        return connection;
    }
}
