/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.service.graph;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Record;
import ru.beeline.architecting_graph.support.GraphBuildTestSupport;
import ru.beeline.architecting_graph.support.Workspaces;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalGraphBuildTest extends GraphBuildTestSupport {

    @Test
    void build_tagsAllNodesAndRelationshipsWithLocalCmdbTag() {
        buildLocal(Workspaces.load("alpha"));

        assertThat(count("MATCH (n) WHERE n.graphTag <> 'Local ALPHA' RETURN count(n)")).isZero();
        assertThat(count("MATCH ()-[r]->() WHERE r.graphTag <> 'Local ALPHA' RETURN count(r)")).isZero();
        assertThat(count("MATCH (n) RETURN count(n)")).isEqualTo(14);
    }

    @Test
    void build_doesNotVersionElements() {
        buildLocal(Workspaces.load("alpha"));

        assertThat(count("MATCH (n) WHERE n.version IS NOT NULL OR n.startVersion IS NOT NULL "
                + "OR n.endVersion IS NOT NULL RETURN count(n)")).isZero();
        assertThat(count("MATCH ()-[r]->() WHERE r.startVersion IS NOT NULL OR r.endVersion IS NOT NULL "
                + "RETURN count(r)")).isZero();
    }

    @Test
    void rebuild_replacesPreviousLocalGraphEntirely() {
        ObjectNode withoutDb = Workspaces.load("alpha");
        Workspaces.remove(withoutDb, "3");
        buildLocal(withoutDb);
        String fresh = snapshot();
        execute("MATCH (n) DETACH DELETE n");

        buildLocal(Workspaces.load("alpha"));
        buildLocal(withoutDb);

        assertThat(snapshot()).isEqualTo(fresh);
        assertThat(count("MATCH (c:Container {name: 'db~ALPHA'}) RETURN count(c)")).isZero();
    }

    @Test
    void rebuild_leavesGlobalAndOtherLocalGraphsIntact() {
        buildGlobal(Workspaces.load("alpha"));
        buildLocal(Workspaces.load("beta"));
        String foreign = snapshot();

        buildLocal(Workspaces.load("alpha"));
        buildLocal(Workspaces.load("alpha"));

        execute("MATCH (n {graphTag: 'Local ALPHA'}) DETACH DELETE n");
        assertThat(snapshot()).isEqualTo(foreign);
    }

    @Test
    void build_namesElementsByHierarchy() {
        buildLocal(Workspaces.load("alpha"));

        assertThat(names("Container")).containsExactlyInAnyOrder("api~ALPHA", "db~ALPHA", "gateway");
        assertThat(names("Component")).containsExactlyInAnyOrder("controller~api~ALPHA", "service~api~ALPHA");
        assertThat(names("DeploymentNode")).containsExactlyInAnyOrder("k8s~ALPHA", "pod~k8s~ALPHA", "dr~ALPHA");
        assertThat(names("InfrastructureNode")).containsExactly("lb~k8s~ALPHA");
        assertThat(names("ContainerInstance")).containsExactlyInAnyOrder(
                "api~ALPHA.ContainerInstance.pod~k8s~ALPHA",
                "db~ALPHA.ContainerInstance.pod~k8s~ALPHA");
        assertThat(single("MATCH (c:Component {name: 'controller~api~ALPHA'}) RETURN c.originalName")
                .get(0).asString()).isEqualTo("controller");
    }

    @Test
    void build_sanitizesPropertyKeys() {
        buildLocal(Workspaces.load("alpha"));

        Record api = single("MATCH (c:Container {name: 'api~ALPHA'}) "
                + "RETURN c.api_style AS style, c.structurizr_dsl_identifier AS dsl");
        assertThat(api.get("style").asString()).isEqualTo("rest");
        assertThat(api.get("dsl").asString()).isEqualTo("alpha.api");
        assertThat(single("MATCH (:Container {name: 'api~ALPHA'})-[r:Relationship {description: 'Reads'}]->() "
                + "RETURN r.data_flow").get(0).asString()).isEqualTo("read");
    }

    @Test
    void build_setsRelationshipLevelByC4Layer() {
        buildLocal(Workspaces.load("alpha"));

        assertThat(levels("SoftwareSystem")).containsOnly("C1");
        assertThat(levels("Container")).containsOnly("C2");
        assertThat(levels("Component")).containsOnly("C3");
        assertThat(levels("DeploymentNode")).containsOnly("");
        assertThat(levels("InfrastructureNode")).containsOnly("");
        assertThat(levels("ContainerInstance")).containsOnly("");
    }

    @Test
    void build_skipsImpliedRelationshipsExceptOnContainerInstances() {
        buildLocal(Workspaces.load("alpha"));

        assertThat(count("MATCH (:SoftwareSystem {cmdb: 'ALPHA'})-[r:Relationship {description: 'Calls gateway'}]->() "
                + "RETURN count(r)")).isZero();
        assertThat(count("MATCH (:ContainerInstance)-[r:Relationship {description: 'Reads'}]->(:ContainerInstance) "
                + "RETURN count(r)")).isEqualTo(1);
    }

    @Test
    void build_skipsRelationshipsToSystemsWithoutCmdbAndFromPeople() {
        buildLocal(Workspaces.load("alpha"));

        assertThat(count("MATCH ()-[r {description: 'Feeds legacy'}]->() RETURN count(r)")).isZero();
        assertThat(count("MATCH ()-[r {description: 'Uses'}]->() RETURN count(r)")).isZero();
        assertThat(count("MATCH (n) WHERE n.name IN ['Legacy', 'User'] RETURN count(n)")).isZero();
    }

    @Test
    void build_createsExternalSystemFromWorkspaceWithoutItsContainersTree() {
        buildLocal(Workspaces.load("alpha"));

        Record beta = single("MATCH (s:SoftwareSystem {cmdb: 'BETA'}) RETURN s.name AS name, "
                + "s.structurizr_dsl_identifier AS dsl, s.description AS description");
        assertThat(beta.get("name").asString()).isEqualTo("Beta System");
        assertThat(beta.get("dsl").asString()).isEqualTo("beta");
        assertThat(beta.get("description").isNull()).isTrue();
    }

    @Test
    void build_collapsesExternalContainersWithoutExternalNameIntoSingleNode() {
        buildLocal(Workspaces.load("alpha"));

        Record external = single("MATCH (c:Container {external_name: 'BETA'}) RETURN count(c) AS nodes, "
                + "collect(c.name)[0] AS name");
        assertThat(external.get("nodes").asLong()).isEqualTo(1);
        assertThat(external.get("name").asString()).isEqualTo("gateway");
        assertThat(query("MATCH (:Container {name: 'api~ALPHA'})-[r:Relationship]->(:Container {external_name: 'BETA'}) "
                + "RETURN r.description AS d ORDER BY d").stream().map(r -> r.get("d").asString()))
                .containsExactly("Calls gateway", "Publishes");
        assertThat(single("MATCH (s:SoftwareSystem)-[r:Child]->(:Container {external_name: 'BETA'}) "
                + "RETURN s.cmdb AS parent, r.sourceWorkspace AS ws").get("ws").asString()).isEqualTo("BETA");
    }

    @Test
    void build_describesUndescribedRelationshipAsNoneAndCountsConnects() {
        buildLocal(Workspaces.load("alpha"));

        Record r = single("MATCH (:Component {name: 'controller~api~ALPHA'})-[r:Relationship]->"
                + "(:Component {name: 'service~api~ALPHA'}) RETURN r.description AS d, r.numberOfConnects AS n");
        assertThat(r.get("d").asString()).isEqualTo("None");
        assertThat(r.get("n").asString()).isEqualTo("1");
    }

    @Test
    void build_linksElementsWithChildDeployAndEnvironmentRelationships() {
        buildLocal(Workspaces.load("alpha"));

        assertThat(count("MATCH (:SoftwareSystem {cmdb: 'ALPHA'})-[:Child]->(d:DeploymentNode) RETURN count(d)"))
                .isEqualTo(2);
        assertThat(count("MATCH (:Container {name: 'api~ALPHA'})-[:Deploy]->(:ContainerInstance) RETURN count(*)"))
                .isEqualTo(1);
        assertThat(count("MATCH (:Environment {name: 'Production'})-[:Child]->(n) RETURN count(n)")).isEqualTo(6);
        assertThat(count("MATCH (:Environment) RETURN count(*)")).isEqualTo(1);
    }

    private List<String> names(String label) {
        return query("MATCH (n:" + label + ") RETURN n.name AS name").stream()
                .map(r -> r.get("name").asString())
                .toList();
    }

    private List<String> levels(String sourceLabel) {
        return query("MATCH (:" + sourceLabel + ")-[r:Relationship]->() RETURN r.level AS level").stream()
                .map(r -> r.get("level").asString())
                .toList();
    }
}
