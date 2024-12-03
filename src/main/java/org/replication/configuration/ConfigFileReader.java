package org.replication.configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;

public class ConfigFileReader {

    private final Properties properties;
    private static final Logger logger = LogManager.getLogger(ConfigFileReader.class);

    public ConfigFileReader() {
        // read configuration file
        String configFilePath = "properties//server.properties";
        File ConfigFile = new File(configFilePath);
        try {
            FileInputStream configFileReader = new FileInputStream(ConfigFile);
            properties = new Properties();
            try {
                properties.load(configFileReader);
                configFileReader.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException("server.properties not found at config file path " + configFilePath);
        }
    }

   public List<String> getSecondariesServersList(){
        String rawSecondariesServersList = properties.getProperty("secondary.servers.addresses", null);
        if (rawSecondariesServersList != null){
            return Arrays.asList(rawSecondariesServersList.split(","));
        }
       logger.warn("Property secondary.servers.addresses not found");
        return null;
    }

    public String getMainUrl() {
       String mainUrl = properties.getProperty("main.server.address");
       if (mainUrl != null){
           return mainUrl;
       }
       logger.warn("Property main.server.address not found");
       return null;
    }
}