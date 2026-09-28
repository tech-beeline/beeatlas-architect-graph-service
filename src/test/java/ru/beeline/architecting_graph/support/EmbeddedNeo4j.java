/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.support;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EmbeddedNeo4j {

    private static DatabaseManagementService dbms;
    private static Driver driver;

    private EmbeddedNeo4j() {
    }

    public static synchronized Driver driver() {
        if (driver == null) {
            start();
        }
        return driver;
    }

    public static void clean() {
        try (Session session = driver().session()) {
            session.run("MATCH (n) DETACH DELETE n").consume();
        }
    }

    private static void start() {
        try {
            Path dir = Files.createTempDirectory("neo4j-test");
            int port = freePort();
            dbms = new DatabaseManagementServiceBuilder(dir)
                    .setConfig(BoltConnector.enabled, true)
                    .setConfig(BoltConnector.listen_address, new SocketAddress("localhost", port))
                    .setConfig(BoltConnector.encryption_level, BoltConnector.EncryptionLevel.DISABLED)
                    .setConfig(GraphDatabaseSettings.auth_enabled, false)
                    .build();
            driver = GraphDatabase.driver("bolt://localhost:" + port, AuthTokens.none());
            driver.verifyConnectivity();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                driver.close();
                dbms.shutdown();
            }));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
