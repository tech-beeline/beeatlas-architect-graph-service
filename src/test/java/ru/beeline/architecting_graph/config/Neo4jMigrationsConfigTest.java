/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.config;

import ac.simons.neo4j.migrations.core.MigrationChain;
import ac.simons.neo4j.migrations.core.MigrationState;
import ac.simons.neo4j.migrations.core.Migrations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.summary.Plan;
import ru.beeline.architecting_graph.support.EmbeddedNeo4j;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class Neo4jMigrationsConfigTest {

    private static final List<String> BUILD_INDEXES = List.of(
            "software_system_cmdb",
            "container_name",
            "container_external_name",
            "component_name",
            "component_external_name",
            "deployment_node_name",
            "infrastructure_node_name",
            "container_instance_name",
            "environment_name",
            "software_system_graph_tag",
            "container_graph_tag",
            "component_graph_tag",
            "deployment_node_graph_tag",
            "infrastructure_node_graph_tag",
            "container_instance_graph_tag",
            "environment_graph_tag",
            "relationship_source_workspace",
            "child_source_workspace",
            "deploy_source_workspace");

    private final Driver driver = EmbeddedNeo4j.driver();

    @BeforeEach
    void resetSchema() {
        EmbeddedNeo4j.clean();
        for (String index : BUILD_INDEXES) {
            run("DROP INDEX " + index + " IF EXISTS");
        }
    }

    @Test
    void firstStart_createsGraphBuildIndexesAndRecordsVersion() {
        Migrations migrations = new Neo4jMigrationsConfig().neo4jMigrations(driver);

        assertThat(onlineIndexes()).containsAll(BUILD_INDEXES);
        MigrationChain chain = migrations.info();
        assertThat(chain.getLastAppliedVersion()).hasValueSatisfying(v -> assertThat(v.getValue()).isEqualTo("002"));
        assertThat(chain.getElements()).extracting(MigrationChain.Element::getState)
                .containsOnly(MigrationState.APPLIED);
    }

    @Test
    void repeatedStart_doesNotReapplyMigrations() {
        new Neo4jMigrationsConfig().neo4jMigrations(driver);

        Migrations migrations = new Neo4jMigrationsConfig().neo4jMigrations(driver);

        assertThat(migrations.info().getElements()).hasSize(2);
        assertThat(onlineIndexes()).containsAll(BUILD_INDEXES);
    }

    @Test
    void indexesCreatedManuallyBeforeFirstStart_doNotBreakMigration() {
        run("CREATE INDEX container_name IF NOT EXISTS FOR (n:Container) ON (n.name)");

        Migrations migrations = new Neo4jMigrationsConfig().neo4jMigrations(driver);

        assertThat(migrations.info().isApplied("002")).isTrue();
        assertThat(onlineIndexes()).containsAll(BUILD_INDEXES);
    }

    @Test
    void graphBuildLookups_useIndexesInsteadOfLabelScan() {
        new Neo4jMigrationsConfig().neo4jMigrations(driver);

        assertThat(plan("MATCH (n:Container {graphTag: 'Global', name: 'x'}) RETURN n"))
                .contains("NodeIndexSeek").doesNotContain("NodeByLabelScan");
        assertThat(plan("MATCH (n:SoftwareSystem {graphTag: 'Global', cmdb: 'x'}) RETURN n"))
                .contains("NodeIndexSeek").doesNotContain("NodeByLabelScan");
        assertThat(plan("MATCH (n:DeploymentNode {graphTag: 'Global', name: 'x'}) RETURN n"))
                .contains("NodeIndexSeek").doesNotContain("NodeByLabelScan");
        assertThat(plan("MATCH (n:Component {graphTag: 'Global', external_name: 'x'}) RETURN n"))
                .contains("NodeIndexSeek").doesNotContain("NodeByLabelScan");
    }

    @Test
    void localGraphDeletionAndRelationshipClosing_avoidFullScans() {
        new Neo4jMigrationsConfig().neo4jMigrations(driver);

        assertThat(plan("""
                CALL {
                    MATCH (n:SoftwareSystem {graphTag: 'Local X'}) RETURN n
                    UNION MATCH (n:Container {graphTag: 'Local X'}) RETURN n
                    UNION MATCH (n:Component {graphTag: 'Local X'}) RETURN n
                    UNION MATCH (n:DeploymentNode {graphTag: 'Local X'}) RETURN n
                    UNION MATCH (n:InfrastructureNode {graphTag: 'Local X'}) RETURN n
                    UNION MATCH (n:ContainerInstance {graphTag: 'Local X'}) RETURN n
                    UNION MATCH (n:Environment {graphTag: 'Local X'}) RETURN n
                }
                DETACH DELETE n
                """)).contains("NodeIndexSeek").doesNotContain("AllNodesScan", "NodeByLabelScan");
        for (String type : List.of("Relationship", "Child", "Deploy")) {
            assertThat(plan("MATCH ()-[r:" + type + " {sourceWorkspace: 'X'}]->() "
                    + "WHERE r.graphTag = 'Global' AND r.endVersion IS NULL SET r.endVersion = '1'"))
                    .as(type).contains("RelationshipIndexSeek")
                    .doesNotContain("AllRelationshipsScan", "RelationshipTypeScan");
        }
    }

    private Set<String> onlineIndexes() {
        run("CALL db.awaitIndexes(60)");
        try (Session session = driver.session()) {
            return session.run("SHOW INDEXES YIELD name, state WHERE state = 'ONLINE' RETURN name").list().stream()
                    .map(r -> r.get("name").asString())
                    .collect(Collectors.toSet());
        }
    }

    private String plan(String cypher) {
        try (Session session = driver.session()) {
            return operators(session.run("EXPLAIN " + cypher).consume().plan());
        }
    }

    private static String operators(Plan plan) {
        return plan.operatorType() + plan.children().stream().map(Neo4jMigrationsConfigTest::operators)
                .collect(Collectors.joining(" ", " ", ""));
    }

    private void run(String cypher) {
        try (Session session = driver.session()) {
            session.run(cypher).consume();
        }
    }
}
