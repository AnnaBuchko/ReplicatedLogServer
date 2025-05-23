package org.replication;

import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.replication.configuration.ConfigFileReader;
import org.replication.mainhandlers.HealsMonitorHandler;
import org.replication.mainhandlers.RecoveryHandler;
import org.replication.mainhandlers.GetHandler;
import org.replication.mainhandlers.MainPostHandler;
import org.replication.scheduler.SecondaryHealthMonitor;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;

public class MainServer {
    public static final Logger logger = LogManager.getLogger(MainServer.class);
    private static final SortedMap<Integer, String> messages = new TreeMap<>();
    private static final SecondaryHealthMonitor healthMonitor = new SecondaryHealthMonitor();

    public static void main(String[] args) throws IOException {
        // represents a list of http addresses of secondary servers, configured in configuration file
        List<String> secondaryAddresses = new ConfigFileReader().getSecondariesServersList();

        secondaryAddresses.forEach(healthMonitor::addSecondary);
        healthMonitor.startHeartbeat();

        //create instance of HttpServer
        HttpServer server = HttpServer.create(
                new InetSocketAddress(8080), 0);

        // define handlers for GET and POST methods
        server.createContext("/", new GetHandler(messages));
        server.createContext("/data", new MainPostHandler(messages, secondaryAddresses, healthMonitor));
        server.createContext("/recover", new RecoveryHandler(messages));
        server.createContext("/health", new HealsMonitorHandler(healthMonitor.getSecondaryHealth()));

        // start server
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        logger.info("Main server is started.");
        logger.info("Available list of replication servers:{}", secondaryAddresses.toString());
    }
}