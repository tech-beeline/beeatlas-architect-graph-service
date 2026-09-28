/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.service.graph;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Record;
import ru.beeline.architecting_graph.support.GraphBuildTestSupport;
import ru.beeline.architecting_graph.support.Workspaces;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalGraphBuildTest extends GraphBuildTestSupport {

    @Test
    void firstBuild_setsSystemVersionAndStartVersionOne() {
        buildGlobal(Workspaces.load("alpha"));

        assertThat(systemVersion("ALPHA")).isEqualTo("1");
        assertThat(count("MATCH (n) WHERE n.graphTag <> 'Global' RETURN count(n)")).isZero();
        assertThat(count("MATCH (n) WHERE (n:Container OR n:Component OR n:DeploymentNode OR n:InfrastructureNode "
                + "OR n:ContainerInstance) AND n.external_name IS NULL AND n.startVersion <> '1' RETURN count(n)"))
                .isZero();
        assertThat(count("MATCH ()-[r]->() WHERE r.startVersion <> '1' RETURN count(r)")).isZero();
        assertThat(count("MATCH (n) WHERE n.endVersion IS NOT NULL RETURN count(n)")).isZero();
    }

    @Test
    void repeatedIdenticalBuild_incrementsVersionWithoutDuplicatesOrClosing() {
        buildGlobal(Workspaces.load("alpha"));
        String first = snapshot();

        buildGlobal(Workspaces.load("alpha"));

        assertThat(systemVersion("ALPHA")).isEqualTo("2");
        assertThat(snapshot()).isEqualTo(first.replace("version=1}", "version=2}"));
    }

    @Test
    void removedContainer_isClosedWithComponentsDeployAndRelationshipsButNotDeleted() {
        buildGlobal(Workspaces.load("alpha"));
        ObjectNode v2 = Workspaces.load("alpha");
        Workspaces.remove(v2, "2");

        buildGlobal(v2);

        assertThat(endVersion("MATCH (n:Container {name: 'api~ALPHA'}) RETURN n.endVersion")).isEqualTo("1");
        assertThat(query("MATCH (n:Component) RETURN n.endVersion AS e"))
                .extracting(r -> r.get("e").asString()).containsOnly("1");
        assertThat(endVersion("MATCH (:Container {name: 'api~ALPHA'})-[r:Deploy]->() RETURN r.endVersion"))
                .isEqualTo("1");
        assertThat(query("MATCH (:Container {name: 'api~ALPHA'})-[r:Relationship]->() RETURN r.endVersion AS e"))
                .extracting(r -> r.get("e").asString()).containsOnly("1");
        assertThat(endVersion("MATCH (n:Container {name: 'db~ALPHA'}) RETURN n.endVersion"))
                .isNull();
    }

    @Test
    void returnedContainer_isReopenedKeepingOriginalStartVersion() {
        buildGlobal(Workspaces.load("alpha"));
        ObjectNode withoutDb = Workspaces.load("alpha");
        Workspaces.remove(withoutDb, "3");
        buildGlobal(withoutDb);

        buildGlobal(Workspaces.load("alpha"));

        Record db = single("MATCH (n:Container {name: 'db~ALPHA'}) RETURN n.startVersion AS s, n.endVersion AS e");
        assertThat(db.get("s").asString()).isEqualTo("1");
        assertThat(db.get("e").isNull()).isTrue();
        assertThat(count("MATCH (n:Container {name: 'db~ALPHA'}) RETURN count(n)")).isEqualTo(1);
        assertThat(endVersion("MATCH (:Container {name: 'api~ALPHA'})-[r:Relationship {description: 'Reads'}]->() "
                + "RETURN r.endVersion")).isNull();
    }

    @Test
    void removedDeploymentNode_closesItsSubtree() {
        buildGlobal(Workspaces.load("alpha"));
        ObjectNode v2 = Workspaces.load("alpha");
        Workspaces.remove(v2, "50");

        buildGlobal(v2);

        assertThat(query("MATCH (n) WHERE n:DeploymentNode OR n:InfrastructureNode OR n:ContainerInstance "
                + "RETURN n.name AS name, n.endVersion AS e"))
                .allSatisfy(r -> assertThat(r.get("e").asString(null)).as(r.get("name").asString())
                        .isEqualTo(r.get("name").asString().equals("dr~ALPHA") ? null : "1"));
    }

    @Test
    void deploymentNodeMovedToAnotherEnvironment_closesOldEnvironmentLink() {
        buildGlobal(Workspaces.load("alpha"));
        ObjectNode v2 = Workspaces.load("alpha");
        Workspaces.element(v2, "55").put("environment", "Staging");

        buildGlobal(v2);

        assertThat(endVersion("MATCH (:Environment {name: 'Production'})-[r:Child]->(:DeploymentNode {name: 'dr~ALPHA'}) "
                + "RETURN r.endVersion")).isEqualTo("1");
        Record staging = single("MATCH (:Environment {name: 'Staging'})-[r:Child]->(:DeploymentNode {name: 'dr~ALPHA'}) "
                + "RETURN r.startVersion AS s, r.endVersion AS e");
        assertThat(staging.get("s").asString()).isEqualTo("2");
        assertThat(staging.get("e").isNull()).isTrue();
    }

    @Test
    void changedAttributes_areUpdatedInPlace() {
        buildGlobal(Workspaces.load("alpha"));
        ObjectNode v2 = Workspaces.load("alpha");
        Workspaces.element(v2, "2").put("description", "API v2").put("technology", "Kotlin");

        buildGlobal(v2);

        Record api = single("MATCH (n:Container {name: 'api~ALPHA'}) RETURN count(n) AS c, "
                + "collect(n.description)[0] AS d, collect(n.technology)[0] AS t");
        assertThat(api.get("c").asLong()).isEqualTo(1);
        assertThat(api.get("d").asString()).isEqualTo("API v2");
        assertThat(api.get("t").asString()).isEqualTo("Kotlin");
    }

    @Test
    void changedRelationshipDescription_opensNewRelationshipAndClosesOld() {
        buildGlobal(Workspaces.load("alpha"));
        ObjectNode v2 = Workspaces.load("alpha");
        Workspaces.array(Workspaces.element(v2, "2"), "relationships").forEach(r -> {
            if ("33".equals(r.path("id").asText())) {
                ((ObjectNode) r).put("description", "Reads and writes");
            }
        });

        buildGlobal(v2);

        assertThat(endVersion("MATCH (:Container {name: 'api~ALPHA'})-[r {description: 'Reads'}]->"
                + "(:Container {name: 'db~ALPHA'}) RETURN r.endVersion")).isEqualTo("1");
        Record fresh = single("MATCH (:Container {name: 'api~ALPHA'})-[r {description: 'Reads and writes'}]->"
                + "(:Container {name: 'db~ALPHA'}) RETURN r.startVersion AS s, r.endVersion AS e");
        assertThat(fresh.get("s").asString()).isEqualTo("2");
        assertThat(fresh.get("e").isNull()).isTrue();
    }

    @Test
    void externalSystemPlaceholder_doesNotOverwriteSystemLoadedFromOwnWorkspace() {
        buildGlobal(Workspaces.load("beta"));

        buildGlobal(Workspaces.load("alpha"));

        Record beta = single("MATCH (s:SoftwareSystem {cmdb: 'BETA'}) RETURN s.name AS name, s.version AS v");
        assertThat(beta.get("name").asString()).isEqualTo("Beta Platform");
        assertThat(beta.get("v").asString()).isEqualTo("1");
    }

    @Test
    void ownWorkspace_overwritesAttributesOfPlaceholderSystem() {
        buildGlobal(Workspaces.load("alpha"));

        buildGlobal(Workspaces.load("beta"));

        Record beta = single("MATCH (s:SoftwareSystem {cmdb: 'BETA'}) RETURN count(s) AS c, "
                + "collect(s.name)[0] AS name, collect(s.description)[0] AS d");
        assertThat(beta.get("c").asLong()).isEqualTo(1);
        assertThat(beta.get("name").asString()).isEqualTo("Beta Platform");
        assertThat(beta.get("d").asString()).isEqualTo("Beta own description");
        assertThat(single("MATCH (s:SoftwareSystem {cmdb: 'ALPHA'}) RETURN s.name").get(0).asString())
                .isEqualTo("Alpha System");
    }

    @Test
    void ownWorkspaceFirstLoad_closesExternalContainerPlaceholderButKeepsLinksToIt() {
        buildGlobal(Workspaces.load("alpha"));

        buildGlobal(Workspaces.load("beta"));

        assertThat(endVersion("MATCH (n:Container {external_name: 'BETA'}) RETURN n.endVersion")).isEqualTo("0");
        assertThat(query("MATCH (:Container {name: 'api~ALPHA'})-[r:Relationship]->(:Container {external_name: 'BETA'}) "
                + "RETURN r.endVersion AS e")).extracting(r -> r.get("e").isNull()).containsOnly(true);
        assertThat(count("MATCH (:SoftwareSystem {cmdb: 'BETA'})-[:Child]->(c:Container {name: 'gateway~BETA'}) "
                + "WHERE c.endVersion IS NULL RETURN count(c)")).isEqualTo(1);
    }

    @Test
    void workspaceCmdbInDifferentCase_orphansContainersAndDuplicatesSystemOnReload_knownDefect() {
        ObjectNode ws = Workspaces.load("alpha");
        ((ObjectNode) ws.get("model").get("properties")).put("workspace_cmdb", "alpha");

        buildGlobal(ws);

        assertThat(count("MATCH (s:SoftwareSystem) WHERE s.cmdb IN ['alpha', 'ALPHA'] RETURN count(s)")).isEqualTo(1);
        assertThat(single("MATCH (s:SoftwareSystem) WHERE toLower(s.cmdb) = 'alpha' RETURN s.cmdb").get(0).asString())
                .isEqualTo("ALPHA");
        assertThat(count("MATCH (c:Container {name: 'api~alpha'}) RETURN count(c)")).isEqualTo(1);
        assertThat(count("MATCH (:SoftwareSystem)-[:Child]->(c:Container {name: 'api~alpha'}) RETURN count(c)"))
                .isZero();
        assertThat(count("MATCH (:SoftwareSystem {cmdb: 'ALPHA'})-[r]-() RETURN count(r)")).isZero();

        buildGlobal(ws);

        assertThat(count("MATCH (s:SoftwareSystem {cmdb: 'ALPHA'}) RETURN count(s)")).isEqualTo(2);
    }

    private String systemVersion(String cmdb) {
        return single("MATCH (s:SoftwareSystem {cmdb: $cmdb}) RETURN s.version", "cmdb", cmdb).get(0).asString();
    }

    private String endVersion(String cypher) {
        return single(cypher).get(0).asString(null);
    }
}
