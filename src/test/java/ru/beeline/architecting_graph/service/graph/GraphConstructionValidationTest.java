/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.service.graph;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import ru.beeline.architecting_graph.exception.NotFoundException;
import ru.beeline.architecting_graph.exception.ValidationException;
import ru.beeline.architecting_graph.support.GraphBuildTestSupport;
import ru.beeline.architecting_graph.support.Workspaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class GraphConstructionValidationTest extends GraphBuildTestSupport {

    @Test
    void invalidJson_isRejectedAsInvalidWorkspace() {
        assertThatThrownBy(() -> build("{not json", "Local"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Полученный workspace не валиден");
    }

    @Test
    void missingWorkspaceCmdb_isRejectedWithoutWrites() {
        ObjectNode ws = Workspaces.load("alpha");
        ((ObjectNode) ws.get("model").get("properties")).remove("workspace_cmdb");

        assertThatThrownBy(() -> buildGlobal(ws))
                .isInstanceOf(ValidationException.class)
                .hasMessageStartingWith("Граф не построен");
        assertThat(count("MATCH (n) RETURN count(n)")).isZero();
    }

    @Test
    void missingModelProperties_isRejected() {
        ObjectNode ws = Workspaces.load("alpha");
        ((ObjectNode) ws.get("model")).remove("properties");

        assertThatThrownBy(() -> buildGlobal(ws)).isInstanceOf(ValidationException.class);
    }

    @Test
    void workspaceCmdbWithoutMatchingSystem_keepsExistingLocalGraph() {
        buildLocal(Workspaces.load("alpha"));
        String before = snapshot();
        ObjectNode ws = Workspaces.load("alpha");
        ((ObjectNode) Workspaces.element(ws, "1").get("properties")).put("cmdb", "OTHER");

        assertThatThrownBy(() -> buildLocal(ws))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("workspace_cmdb не соответствует ни один из SoftwareSystem");
        assertThat(snapshot()).isEqualTo(before);
    }

    @Test
    void enumsInAnyCaseAndUnknownValuesOrFields_areAccepted() {
        ObjectNode ws = Workspaces.load("alpha");
        Workspaces.element(ws, "1").put("location", "internal");
        Workspaces.element(ws, "20").put("location", "Somewhere");
        Workspaces.element(ws, "2").put("unknownField", "value");

        buildGlobal(ws);

        assertThat(count("MATCH (s:SoftwareSystem {cmdb: 'ALPHA'}) RETURN count(s)")).isEqualTo(1);
    }

    @Test
    void relationshipWithInteractionStyle_failsAndLeavesPartialGraph_knownDefect() {
        ObjectNode ws = Workspaces.load("alpha");
        Workspaces.array(Workspaces.element(ws, "2"), "relationships").forEach(r -> {
            if ("33".equals(r.path("id").asText())) {
                ((ObjectNode) r).put("interactionStyle", "Asynchronous");
            }
        });

        assertThatThrownBy(() -> buildGlobal(ws))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("InteractionStyle");
        assertThat(count("MATCH (n) RETURN count(n)")).isPositive();
        assertThat(count("MATCH (n:DeploymentNode) RETURN count(n)")).isZero();
    }

    @Test
    void buildByDocId_loadsWorkspaceFromDocumentService() {
        when(documentClient.getDocument(42L)).thenReturn(Workspaces.json(Workspaces.load("alpha")));

        try {
            assertThat(graphConstructionService.graphConstruct(42L, "Local")).isEqualTo("Граф построен");
        } finally {
            sessionManager.closeSession();
        }

        assertThat(count("MATCH (n {graphTag: 'Local ALPHA'}) RETURN count(n)")).isEqualTo(14);
    }

    @Test
    void buildByDocId_missingDocument_isNotFound() {
        when(documentClient.getDocument(42L)).thenReturn(null);

        assertThatThrownBy(() -> graphConstructionService.graphConstruct(42L, "Local"))
                .isInstanceOf(NotFoundException.class);
    }
}
