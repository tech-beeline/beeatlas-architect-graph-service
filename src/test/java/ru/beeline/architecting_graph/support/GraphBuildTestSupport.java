/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.support;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import ru.beeline.architecting_graph.client.DocumentClient;
import ru.beeline.architecting_graph.client.ProductClient;
import ru.beeline.architecting_graph.service.graph.GraphConstructionService;
import ru.beeline.architecting_graph.service.graph.Neo4jSessionManager;

import java.util.List;

@SpringJUnitConfig(GraphBuildTestConfig.class)
public abstract class GraphBuildTestSupport {

    @Autowired
    protected GraphConstructionService graphConstructionService;

    @Autowired
    protected Neo4jSessionManager sessionManager;

    @Autowired
    protected Driver driver;

    @MockBean
    protected DocumentClient documentClient;

    @MockBean
    protected ProductClient productClient;

    @BeforeEach
    void cleanGraph() {
        sessionManager.closeSession();
        EmbeddedNeo4j.clean();
    }

    protected void buildLocal(JsonNode workspace) {
        build(Workspaces.json(workspace), "Local");
    }

    protected void buildGlobal(JsonNode workspace) {
        build(Workspaces.json(workspace), "Global");
    }

    protected void build(String workspaceJson, String graphTag) {
        try {
            graphConstructionService.graphConstruct(workspaceJson, graphTag);
        } finally {
            sessionManager.closeSession();
        }
    }

    protected List<Record> query(String cypher, Object... params) {
        try (Session session = driver.session()) {
            return session.run(cypher, Values.parameters(params)).list();
        }
    }

    protected Record single(String cypher, Object... params) {
        try (Session session = driver.session()) {
            return session.run(cypher, Values.parameters(params)).single();
        }
    }

    protected void execute(String cypher, Object... params) {
        try (Session session = driver.session()) {
            session.run(cypher, Values.parameters(params)).consume();
        }
    }

    protected long count(String cypher, Object... params) {
        return single(cypher, params).get(0).asLong();
    }

    protected String snapshot() {
        return GraphSnapshot.take(driver);
    }

    protected void assertMatchesGolden(String name) {
        GraphSnapshot.assertMatchesGolden(driver, name);
    }
}
