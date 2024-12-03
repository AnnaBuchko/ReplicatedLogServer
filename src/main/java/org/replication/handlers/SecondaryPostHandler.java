package org.replication.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.*;

// Handler for POST requests
public class SecondaryPostHandler implements HttpHandler {
    protected final SortedMap<Integer, String> messages;
    private static final Logger logger = LogManager.getLogger(SecondaryPostHandler.class);

    public SecondaryPostHandler(SortedMap<Integer, String> messages) {
        this.messages = messages;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            // use for introduction of delay
           /* try {
                int random = (int)(10 * random());
                logger.info("Delay for {} seconds", random * 5);
                sleep(random * 5000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }*/
            readAndSaveMessage(exchange);
            String response = "Data received successfully!";

            exchange.sendResponseHeaders(200, response.getBytes(UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(UTF_8));
            }
        } else {
            exchange.sendResponseHeaders(405, -1);
            logger.error("Method is not allowed");
        }
    }

    private void readAndSaveMessage(HttpExchange exchange){
        // read the request body
        InputStream is = exchange.getRequestBody();
        String requestBody = new BufferedReader(new InputStreamReader(is, UTF_8))
                .lines().collect(Collectors.joining("\n"));
        Map<Integer, String> requestBodyParsed = parseRequestBody(requestBody);
      //TEST total order
      // Generate exception on message 3 until 6 messages were not saved
      /* if(requestBodyParsed.containsKey(3) && !messages.containsKey(6)){
           if (exchange.getRequestHeaders().getFirst("Host").equals("secondary1:8080")){
               logger.error("Error for total order check");
               throw new RuntimeException("Error for total order check");
           }
        }*/
        // save the received message
        synchronized (messages) {
            messages.putAll(requestBodyParsed);
        }
        logger.info("Received message: {} from {}", requestBody,
                exchange.getRequestHeaders().get("host"));
    }

    private Map<Integer, String> parseRequestBody(String data){
        Map<String, String> rawData = MainPostHandler.parseQueryParams(data);
        Map<Integer, String> parsedData = new HashMap<>();
        int counter = Integer.parseInt(rawData.get("counter"));
        String message = rawData.get("message");
        parsedData.put(counter, message);
        return parsedData;
    }
}
