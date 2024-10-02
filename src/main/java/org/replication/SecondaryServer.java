package org.replication;

import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.replication.handlers.GetHandler;
import org.replication.handlers.PostHandler;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;


public class SecondaryServer {
    private static final List<String> messages = new ArrayList<>();
    public static final Logger logger = LogManager.getLogger(SecondaryServer.class);

    public static void main(String[] args) throws IOException {
        //create instance of HttpServer
        HttpServer server = HttpServer.create(
                new InetSocketAddress(8080), 0);

        // define handlers for GET and POST methods
        server.createContext("/", new GetHandler(messages));
        server.createContext("/saveData", new PostHandler(messages));

        // start server
        server.start();
        logger.info("Secondary server is started.");    }
}