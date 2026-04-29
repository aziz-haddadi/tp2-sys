package com.tp2.sync;

import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class AppConfig {
    private final Properties properties;

    public AppConfig() {
        this.properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new IllegalStateException("db.properties not found in resources");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load db.properties", e);
        }
    }

    public Connection openDbConnection(String officeKey) throws SQLException {
        String prefix = "db." + officeKey + ".";
        return DriverManager.getConnection(
                properties.getProperty(prefix + "url"),
                properties.getProperty(prefix + "user"),
                properties.getProperty(prefix + "password")
        );
    }

    public String queueNameForOffice(String officeKey) {
        return properties.getProperty("rabbitmq.queue." + officeKey);
    }

    public ConnectionFactory rabbitFactory() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(properties.getProperty("rabbitmq.host"));
        factory.setPort(Integer.parseInt(properties.getProperty("rabbitmq.port")));
        factory.setUsername(properties.getProperty("rabbitmq.user"));
        factory.setPassword(properties.getProperty("rabbitmq.password"));
        return factory;
    }
}
