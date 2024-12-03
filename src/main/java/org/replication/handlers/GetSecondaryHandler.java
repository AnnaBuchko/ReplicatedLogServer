package org.replication.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.SortedMap;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GetSecondaryHandler implements HttpHandler {
    private final SortedMap<Integer, String> messages;
    private final String mainUrl;
    private static final Logger logger = LogManager.getLogger(GetSecondaryHandler.class);

    public GetSecondaryHandler(SortedMap<Integer, String> messages, String mainUrl) {
        this.messages = messages;
        this.mainUrl = mainUrl;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        StringBuilder messagesTobeSend = messageListAvailable();
        exchange.sendResponseHeaders(200, messagesTobeSend.toString().getBytes(UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(messagesTobeSend.toString().getBytes(UTF_8));
        }
    }

    private StringBuilder messageListAvailable() {
        StringBuilder response = new StringBuilder("Stored messages:\n");
        try {
            URL url = new URL(mainUrl + "/recover");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    synchronized (messages) {
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split(":", 2);
                            int counter = Integer.parseInt(parts[0]);
                            if (!messages.containsKey(counter)) {
                                connection.disconnect();
                                logger.info("Stored messages on secondary: \n {}", messages.toString());
                                return response;
                            }
                            response.append(counter)
                                    .append(": ")
                                    .append(messages.get(counter))
                                    .append("\n");
                        }
                    }
                }
            } else {
                logger.error("Connection to main server failed. Consistency check not possible");
            }
            connection.disconnect();
        } catch (Exception ex) {
            logger.error("Some error appeared during consistency check. {}", ex.getMessage());
        }
        return response;
    }

}
