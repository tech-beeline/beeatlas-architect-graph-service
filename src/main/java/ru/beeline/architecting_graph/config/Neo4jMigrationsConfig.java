/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.config;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.neo4j.migrations.enabled", havingValue = "true", matchIfMissing = true)
public class Neo4jMigrationsConfig {

    public static final String LOCATION = "classpath:neo4j/migrations";

    @Bean
    public Migrations neo4jMigrations(Driver driver) {
        Migrations migrations = new Migrations(
                MigrationsConfig.builder().withLocationsToScan(LOCATION).build(), driver);
        migrations.apply().ifPresentOrElse(
                version -> log.info("Neo4j schema migrated to version {}", version.getValue()),
                () -> log.info("Neo4j schema is up to date"));
        return migrations;
    }
}
