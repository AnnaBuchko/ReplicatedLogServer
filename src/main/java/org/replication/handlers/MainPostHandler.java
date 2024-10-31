package org.replication.handlers;

import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.Executors.newFixedThreadPool;

public class MainPostHandler extends PostHandler {
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
            ExecutorService executor = newFixedThreadPool(secondaryAddresses.size());
            List<Future<String>>  futures = new ArrayList<>();

            for (String secondaryAddress: secondaryAddresses){
                futures.add(executor.submit(()->
                        sendToSecondary(secondaryAddress, requestBody)));
            }

                futures.forEach(future -> {
                    try {
                        logger.info(future.get());
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                });

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

    public String sendToSecondary(String address, String message) throws IOException {
        HttpURLConnection connection = getHttpURLConnection(address);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(message.getBytes());
            os.flush();
        }
        // get ACK from secondaries server
        int responseCode = connection.getResponseCode();
        if (responseCode == 200){
           return "Message sent to " + address;
        }
        connection.disconnect();
        return "Got response from the server with code " + responseCode + ". Message was not saved!";
    }

    private static HttpURLConnection getHttpURLConnection(String address) throws IOException {
        URL url = new URL(address + "/saveData");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        return connection;
    }
}
