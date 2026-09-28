/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.service.graph;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import ru.beeline.architecting_graph.support.GraphBuildTestSupport;
import ru.beeline.architecting_graph.support.Workspaces;

class GraphBuildGoldenTest extends GraphBuildTestSupport {

    @Test
    void local_alpha() {
        buildLocal(Workspaces.load("alpha"));

        assertMatchesGolden("local-alpha");
    }

    @Test
    void global_alpha_firstVersion() {
        buildGlobal(Workspaces.load("alpha"));

        assertMatchesGolden("global-alpha-v1");
    }

    @Test
    void global_alpha_secondVersion_withoutDbContainerAndDrNode() {
        buildGlobal(Workspaces.load("alpha"));
        ObjectNode v2 = Workspaces.load("alpha");
        Workspaces.remove(v2, "3");
        Workspaces.remove(v2, "55");
        Workspaces.element(v2, "2").put("description", "API v2");

        buildGlobal(v2);

        assertMatchesGolden("global-alpha-v2");
    }

    @Test
    void global_alphaThenBeta() {
        buildGlobal(Workspaces.load("alpha"));
        buildGlobal(Workspaces.load("beta"));

        assertMatchesGolden("global-alpha-then-beta");
    }

    @Test
    void globalAndLocal_coexist() {
        buildGlobal(Workspaces.load("alpha"));
        buildLocal(Workspaces.load("alpha"));
        buildLocal(Workspaces.load("beta"));

        assertMatchesGolden("global-and-local-coexist");
    }
}
