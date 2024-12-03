package org.replication;

import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.replication.configuration.ConfigFileReader;
import org.replication.handlers.GetSecondaryHandler;
import org.replication.handlers.SecondaryPostHandler;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;


public class SecondaryServer {
    private static final SortedMap<Integer, String> messages = new TreeMap<>();
    public static final Logger logger = LogManager.getLogger(SecondaryServer.class);

    public static void main(String[] args) throws IOException {
        String mainUrl = new ConfigFileReader().getMainUrl();
        //create instance of HttpServer
        HttpServer server = HttpServer.create(
                new InetSocketAddress(8080), 0);

        // define handlers for GET and POST methods
        server.createContext("/", new GetSecondaryHandler(messages, mainUrl));
        server.createContext("/saveData", new SecondaryPostHandler(messages));

        // start server
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        logger.info("Secondary server is started.");
        if(mainUrl != null) {
            logger.info("Start recovering from main...");
            recoverFromMain(mainUrl);
        }
    }

    private static void recoverFromMain(String mainUrl) {
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
                            String message = parts[1];
                            // deduplication
                            if (!messages.containsKey(counter)) {
                                messages.put(counter, message);
                                logger.info("Recovered message: {}: {}", counter, message);
                            }
                        }
                    }
                }
            } else {
                logger.error("Failed to recover messages, response code: {}", responseCode);
            }
            connection.disconnect();
        } catch (Exception ex) {
            logger.error("Error during recovery: {}", ex.getMessage());
        }
    }
}