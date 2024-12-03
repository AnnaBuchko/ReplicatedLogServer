package org.replication.mainhandlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.replication.scheduler.SecondaryHealthMonitor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.replication.scheduler.SecondaryHealthMonitor.ServerStatus.*;

public class MainPostHandler implements HttpHandler {
    protected final SortedMap<Integer, String> messages;
    private final List<String> secondaryAddresses;
    private SecondaryHealthMonitor healthMonitor;
    private static int counter = 0;
    private static final Logger logger = LogManager.getLogger(MainPostHandler.class);

    public MainPostHandler(SortedMap<Integer, String> messages, List<String> secondaryAddresses,
                           SecondaryHealthMonitor healthMonitor) {
        this.messages = messages;
        this.secondaryAddresses = secondaryAddresses;
        this.healthMonitor = healthMonitor;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            if(!healthMonitor.isQuorumReached()){
                String response = "Data append is not allowed. Replication servers are down.";
                exchange.sendResponseHeaders(200, response.getBytes(UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(UTF_8));
                }
                return;
            }
            int writeConcern = readWriteConcerns(exchange, secondaryAddresses.size());
            CountDownLatch countDownLatch = new CountDownLatch(writeConcern - 1);
            // returns the formated message with counter and message
            String requestBody = readAndSaveMessage(exchange);
            // send message to secondary servers
            for (String secondaryAddress : secondaryAddresses) {
                CompletableFuture.runAsync(() -> {
                    try {
                        sendToSecondary(secondaryAddress, requestBody);
                    } finally {
                        if (countDownLatch.getCount() > 0) {
                            countDownLatch.countDown();
                        }
                    }
                    });
            }
            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
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

    private int readWriteConcerns(HttpExchange exchange, int serversCount){
        // Extract the query parameter from the URI
        URI requestURI = exchange.getRequestURI();
        String query = requestURI.getQuery();
        Map<String, String> queryParams = parseQueryParams(query);
        // if value is null the return 1 as default
        int writeConcern = parseInt(queryParams.getOrDefault("writeConcern", "1"));
        // check if writeConcern is not bigger than count of replication servers
        if (writeConcern > serversCount + 1){
            logger.info("There are available {} replication servers, executing task with write concerns {}",
                    serversCount, serversCount + 1);
            return serversCount + 1;
        }
        return Math.max(writeConcern, 1);
    }

    public static Map<String, String> parseQueryParams(String query) {
        Map<String, String> queryPairs = new HashMap<>();
        if (query == null) return queryPairs;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            queryPairs.put(pair.substring(0, idx), pair.substring(idx + 1));
        }
        return queryPairs;
    }

    protected String readAndSaveMessage(HttpExchange exchange){
        // read the request body
        InputStream is = exchange.getRequestBody();
        String requestBody = new BufferedReader(new InputStreamReader(is, UTF_8))
                .lines().collect(Collectors.joining("\n"));

        // save the received message
        synchronized (messages) {
            messages.put(++counter, requestBody);
        }
        logger.info("Received message: {} from {}", requestBody,
                exchange.getRequestHeaders().get("host"));
        // format the request body to be able to send the counter and message
        return "counter=" + counter + "&message=" + requestBody;
    }

    public void sendToSecondary(String address, String message) {
        int attempt = 0;
        int initialDelay = 1000;
        if(address.isEmpty()){
            logger.error("Secondary address is not defined. Pleases set it in configuration!");
            return;
        }
        SecondaryHealthMonitor.ServerStatus status = healthMonitor.getSecondaryHealth().getOrDefault(address, UNHEALTHY);
        if (status == UNHEALTHY) {
            logger.info("Skipping retry for {}  as it is UNHEALTHY", address);
            return;
        }
        int noAttempts = status == SUSPECTED ? 3: 100;
        logger.info("Set number attempts to {}", noAttempts);
        while (attempt < noAttempts){
            attempt++;
            try {
                HttpURLConnection connection = getHttpURLConnection(address);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(message.getBytes());
                    os.flush();
                }
                // get ACK from secondaries server
                int responseCode = connection.getResponseCode();
                if (responseCode == 200){
                    logger.info("Message \"{}\" sent to {} on attempt {}", message, address, attempt);
                    connection.disconnect();
                    break;
                }
            } catch (Exception ex) {
                logger.error("Attempt {} failed for {}: {}. Retrying...",
                        attempt, address, ex.getMessage());
            }
            long delay = getExpBackoff(initialDelay, attempt);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                logger.error("Retry interrupted: {}", ex.getMessage());
                Thread.currentThread().interrupt();
                break;
            }
        }

        logger.info("All retry attempts failed for {}. Marking as SUSPECTED.", address);
        healthMonitor.getSecondaryHealth().put(address, SUSPECTED);
    }

    private long getExpBackoff(int initialDelayMillis, int attempt) {
        int exponentialBackoff = (int) Math.min(Integer.MAX_VALUE, initialDelayMillis * Math.pow(2, attempt - 1));
        int jitter = (int) (Math.random() * (exponentialBackoff / 2));
        return exponentialBackoff + jitter;
    }

    private static HttpURLConnection getHttpURLConnection(String address) throws IOException {
        URL url = new URL(address + "/saveData");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        return connection;
    }
}
