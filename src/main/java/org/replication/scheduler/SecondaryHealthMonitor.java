package org.replication.scheduler;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.replication.scheduler.SecondaryHealthMonitor.ServerStatus.UNHEALTHY;

public class SecondaryHealthMonitor {
    private static final Logger logger = LogManager.getLogger(SecondaryHealthMonitor.class);
    private final Map<String, ServerStatus> secondaryHealth = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public enum ServerStatus {
        HEALTHY, SUSPECTED, UNHEALTHY
    }

    public void addSecondary(String url) {
        secondaryHealth.put(url, UNHEALTHY);
    }

    public void startHeartbeat() {
        scheduler.scheduleAtFixedRate(this::checkAllSecondaries, 0, 5, TimeUnit.SECONDS);
    }

    public void stopHeartbeat() {
        scheduler.shutdown();
    }

    private void checkAllSecondaries() {
        for (String secondaryUrl : secondaryHealth.keySet()) {
            checkSecondaryHealth(secondaryUrl);
        }
    }

    private void checkSecondaryHealth(String secondaryUrl) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(secondaryUrl + "/ping").openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000); // 2-second timeout
            connection.setReadTimeout(2000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                secondaryHealth.put(secondaryUrl, ServerStatus.HEALTHY);
                logger.info("{} is HEALTHY", secondaryUrl);
            } else {
                transitionToSuspectedOrUnhealthy(secondaryUrl);
            }
            connection.disconnect();
        } catch (Exception e) {
            transitionToSuspectedOrUnhealthy(secondaryUrl);
        }
    }

    private void transitionToSuspectedOrUnhealthy(String secondaryUrl) {
        ServerStatus currentStatus = secondaryHealth.getOrDefault(secondaryUrl, UNHEALTHY);
        if (currentStatus == ServerStatus.HEALTHY) {
            secondaryHealth.put(secondaryUrl, ServerStatus.SUSPECTED);
            logger.info("{} is SUSPECTED", secondaryUrl);
        } else if (currentStatus == ServerStatus.SUSPECTED) {
            secondaryHealth.put(secondaryUrl, UNHEALTHY);
            logger.info("{} is UNHEALTHY", secondaryUrl);
        }
    }

    public Map<String, ServerStatus> getSecondaryHealth() {
        return secondaryHealth;
    }

    public Boolean isQuorumReached(){
        int noOfHealthServers = 0;
        for(ServerStatus status : secondaryHealth.values()) {
            if(status != UNHEALTHY){
                noOfHealthServers++;
            }
        }
        return noOfHealthServers != 0;
    }
}