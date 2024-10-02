package org.replication.configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;

public class ConfigFileReader {

    private final Properties properties;

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
        return null;
    }
}