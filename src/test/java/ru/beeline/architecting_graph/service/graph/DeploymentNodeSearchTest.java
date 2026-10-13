/*
 * Copyright (c) 2026 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.service.graph;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.beeline.architecting_graph.dto.DeploymentNodeDTO;
import ru.beeline.architecting_graph.support.GraphBuildTestSupport;
import ru.beeline.architecting_graph.support.Workspaces;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentNodeSearchTest extends GraphBuildTestSupport {

    @ParameterizedTest
    @ValueSource(strings = {"77.88.", "auto_test_host703", "auto_test_orig703", "AUTO_TEST_HOST703"})
    void nodeWithoutSoftwareSystem_isFoundAndTakesCmdbFromName(String search) {
        execute("CREATE (e:Environment {graphTag: 'Global', name: 'auto_test_env703'}) "
                + "CREATE (n:DeploymentNode {graphTag: 'Global', name: 'auto_test_orig703~CMDB703', "
                + "originalName: 'auto_test_orig703', ip: '10.77.88.99', host: 'auto_test_host703'}) "
                + "CREATE (e)-[:Child]->(n)");

        List<DeploymentNodeDTO> result = search(search);

        assertThat(result).hasSize(1);
        DeploymentNodeDTO node = result.get(0);
        assertThat(node.getId()).isEqualTo(nodeId("auto_test_orig703~CMDB703"));
        assertThat(node.getDeploymentName()).isEqualTo("auto_test_orig703");
        assertThat(node.getCmdb()).isEqualTo("CMDB703");
        assertThat(node.getEnvironmentName()).isEqualTo("auto_test_env703");
        assertThat(node.getIp()).isEqualTo("10.77.88.99");
        assertThat(node.getHost()).isEqualTo("auto_test_host703");
    }

    @Test
    void softwareSystemCmdb_winsOverNameSuffix() {
        execute("CREATE (s:SoftwareSystem {graphTag: 'Global', cmdb: 'REAL'}) "
                + "CREATE (n:DeploymentNode {graphTag: 'Global', name: 'node~OTHER', originalName: 'node', host: 'h1'}) "
                + "CREATE (s)-[:Child]->(n)");

        assertThat(search("h1")).singleElement()
                .extracting(DeploymentNodeDTO::getCmdb).isEqualTo("REAL");
    }

    @Test
    void publishedTopLevelNode_isFoundWithSystemAndEnvironment() {
        buildGlobal(Workspaces.load("alpha"));

        List<DeploymentNodeDTO> result = search("k8s");

        assertThat(result).singleElement().satisfies(node -> {
            assertThat(node.getDeploymentName()).isEqualTo("k8s");
            assertThat(node.getCmdb()).isEqualTo("ALPHA");
            assertThat(node.getEnvironmentName()).isEqualTo("Production");
            assertThat(node.getIp()).isEqualTo("10.0.0.1");
        });
    }

    @Test
    void publishedNestedNode_takesEnvironmentAndSystemFromParent() {
        buildGlobal(Workspaces.load("alpha"));

        List<DeploymentNodeDTO> result = search("pod");

        assertThat(result).singleElement().satisfies(node -> {
            assertThat(node.getDeploymentName()).isEqualTo("pod");
            assertThat(node.getCmdb()).isEqualTo("ALPHA");
            assertThat(node.getEnvironmentName()).isEqualTo("Production");
        });
    }

    @Test
    void nodeWithoutAnyParent_isReturnedWithEmptyCmdbAndEnvironment() {
        execute("CREATE (:DeploymentNode {graphTag: 'Global', name: 'lonely', originalName: 'lonely', host: 'lonely-host'})");

        assertThat(search("lonely-host")).singleElement().satisfies(node -> {
            assertThat(node.getDeploymentName()).isEqualTo("lonely");
            assertThat(node.getCmdb()).isNull();
            assertThat(node.getEnvironmentName()).isNull();
        });
    }

    @Test
    void unknownFullIp_withoutProductInfra_returnsEmptyList() {
        assertThat(search("10.9.9.9")).isEmpty();
    }

    private List<DeploymentNodeDTO> search(String search) {
        try {
            return graphConstructionService.getDeploymentNode(search).getBody();
        } finally {
            sessionManager.closeSession();
        }
    }

    private long nodeId(String name) {
        return single("MATCH (n:DeploymentNode {name: $name}) RETURN id(n)", "name", name).get(0).asLong();
    }
}
