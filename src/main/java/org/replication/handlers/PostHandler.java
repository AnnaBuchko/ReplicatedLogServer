package org.replication.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;
import static java.nio.charset.StandardCharsets.*;

// Handler for POST requests
public class PostHandler implements HttpHandler {
    protected final List<String> messages;
    private static final Logger logger = LogManager.getLogger(PostHandler.class);

    public PostHandler(List<String> messages) {
        this.messages = messages;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            readAndSaveMessage(exchange);
            String response = "Data received successfully!";
            // use for introduction of delay
           /* try {
                sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }*/
            exchange.sendResponseHeaders(200, response.getBytes(UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(UTF_8));
            }
        } else {
            exchange.sendResponseHeaders(405, -1);
            logger.error("Method is not allowed");
        }
    }

    protected String readAndSaveMessage(HttpExchange exchange){
        // read the request body
        InputStream is = exchange.getRequestBody();
        String requestBody = new BufferedReader(new InputStreamReader(is, UTF_8))
                .lines().collect(Collectors.joining("\n"));

        // save the received message
        synchronized (messages) {
            messages.add(requestBody);
        }
        logger.info("Received message: {} from {}", requestBody,
                exchange.getRequestHeaders().get("host"));
        return requestBody;
    }
}
