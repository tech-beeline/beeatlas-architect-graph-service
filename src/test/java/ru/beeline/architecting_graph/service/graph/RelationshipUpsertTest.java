/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.service.graph;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Record;
import ru.beeline.architecting_graph.support.GraphBuildTestSupport;
import ru.beeline.architecting_graph.support.QueryCounter;
import ru.beeline.architecting_graph.support.Workspaces;

import static org.assertj.core.api.Assertions.assertThat;

class RelationshipUpsertTest extends GraphBuildTestSupport {

    @Test
    void duplicateUndescribedRelationship_isStoredOnceAndCountsConnects() {
        buildLocal(withDuplicates());

        Record r = single("MATCH (:Component {name: 'controller~api~ALPHA'})-[r:Relationship]->"
                + "(:Component {name: 'service~api~ALPHA'}) RETURN count(r) AS c, collect(r.numberOfConnects)[0] AS n");
        assertThat(r.get("c").asLong()).isEqualTo(1);
        assertThat(r.get("n").asString()).isEqualTo("2");
    }

    @Test
    void duplicateDescribedRelationship_takesLastAttributesAndMergesProperties() {
        buildLocal(withDuplicates());

        Record r = single("MATCH (:Container {name: 'api~ALPHA'})-[r:Relationship {description: 'Reads'}]->"
                + "(:Container {name: 'db~ALPHA'}) RETURN count(r) AS c, collect(r)[0] AS r");
        assertThat(r.get("c").asLong()).isEqualTo(1);
        assertThat(r.get("r").asRelationship().asMap())
                .containsEntry("technology", "ODBC")
                .containsEntry("data_flow", "write")
                .containsEntry("extra_key", "x")
                .doesNotContainKey("numberOfConnects");
    }

    @Test
    void globalReload_recountsConnectsInsteadOfAccumulating() {
        buildGlobal(withDuplicates());
        buildGlobal(withDuplicates());

        assertThat(single("MATCH (:Component {name: 'controller~api~ALPHA'})-[r:Relationship]->"
                + "(:Component {name: 'service~api~ALPHA'}) RETURN r.numberOfConnects").get(0).asString())
                .isEqualTo("2");
    }

    @Test
    void globalReload_keepsStartVersionAndReopensRelationship() {
        buildGlobal(Workspaces.load("alpha"));
        ObjectNode withoutReads = Workspaces.load("alpha");
        ArrayNode rels = Workspaces.array(Workspaces.element(withoutReads, "2"), "relationships");
        for (int i = rels.size() - 1; i >= 0; i--) {
            if ("33".equals(rels.get(i).path("id").asText())) {
                rels.remove(i);
            }
        }
        buildGlobal(withoutReads);

        buildGlobal(Workspaces.load("alpha"));

        Record r = single("MATCH (:Container {name: 'api~ALPHA'})-[r:Relationship {description: 'Reads'}]->() "
                + "RETURN r.startVersion AS s, r.endVersion AS e, r.data_flow AS f");
        assertThat(r.get("s").asString()).isEqualTo("1");
        assertThat(r.get("e").isNull()).isTrue();
        assertThat(r.get("f").asString()).isEqualTo("read");
    }

    @Test
    void queriesPerBuild_doNotGrowWithNumberOfRelationships() {
        long base = queriesToBuildGlobal(Workspaces.load("alpha"));
        ObjectNode many = Workspaces.load("alpha");
        ArrayNode rels = Workspaces.array(Workspaces.element(many, "2"), "relationships");
        for (int i = 0; i < 200; i++) {
            rels.addObject().put("id", "x" + i).put("sourceId", "2").put("destinationId", i % 2 == 0 ? "3" : "20")
                    .put("description", "extra " + i).put("technology", "REST");
        }
        execute("MATCH (n) DETACH DELETE n");

        long withMany = queriesToBuildGlobal(many);

        assertThat(withMany - base).isLessThanOrEqualTo(5);
        assertThat(count("MATCH (:Container {name: 'api~ALPHA'})-[r:Relationship]->() WHERE r.description STARTS WITH 'extra' "
                + "RETURN count(r)")).isEqualTo(200);
    }

    private long queriesToBuildGlobal(ObjectNode workspace) {
        long before = QueryCounter.count();
        buildGlobal(workspace);
        return QueryCounter.count() - before;
    }

    private static ObjectNode withDuplicates() {
        ObjectNode ws = Workspaces.load("alpha");
        Workspaces.array(Workspaces.element(ws, "4"), "relationships").addObject()
                .put("id", "90").put("sourceId", "4").put("destinationId", "5");
        ObjectNode reads = Workspaces.array(Workspaces.element(ws, "2"), "relationships").addObject()
                .put("id", "91").put("sourceId", "2").put("destinationId", "3")
                .put("description", "Reads").put("technology", "ODBC");
        reads.putObject("properties").put("data.flow", "write").put("extra.key", "x");
        return ws;
    }
}
