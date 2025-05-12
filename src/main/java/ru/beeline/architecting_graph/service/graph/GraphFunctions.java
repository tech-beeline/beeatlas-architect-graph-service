package ru.beeline.architecting_graph.service.graph;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;

import java.util.HashMap;

import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import ru.beeline.architecting_graph.service.graph.container.ContainerEndVersionFunctions;
import ru.beeline.architecting_graph.model.DeploymentNode;
import ru.beeline.architecting_graph.service.graph.deploymentNode.DeploymentNodeRelationshipFunctions;
import ru.beeline.architecting_graph.service.graph.deploymentNode.DeploymentNodeUpdateFunctions;
import ru.beeline.architecting_graph.service.graph.deploymentNode.DeploymentNodeEndVersionFunctions;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.model.Model;
import ru.beeline.architecting_graph.service.graph.ModelFunctions;
import ru.beeline.architecting_graph.service.graph.relationship.RelationshipEndVersionFunctions;
import ru.beeline.architecting_graph.model.SoftwareSystem;
import ru.beeline.architecting_graph.service.graph.softwareSystem.SoftwareSystemRelationshipFunctions;
import ru.beeline.architecting_graph.service.graph.softwareSystem.SoftwareSystemUpdateFunctions;
import ru.beeline.architecting_graph.model.Workspace;
import ru.beeline.architecting_graph.service.graph.container.ContainerUpdateFunctions;

public class GraphFunctions {

    public static void setEndVersion(Session session, String graphTag, String cmdb, String curVersion) {
        ContainerEndVersionFunctions.setContainerEndVersion(session, graphTag, cmdb, curVersion);

        String getDeploymentNode = "MATCH (n:SoftwareSystem {cmdb: $cmdb1, graphTag: $graphTag1})-[r:Child]->(m:DeploymentNode) "
                + "WHERE m.endVersion IS NULL RETURN m.name as deploymentNodeName";
        Value parameters = Values.parameters("graphTag1", graphTag, "cmdb1", cmdb);
        Result result = session.run(getDeploymentNode, parameters);

        while (result.hasNext()) {
            String deploymentNodeName = result.next().get("deploymentNodeName").toString();
            deploymentNodeName = deploymentNodeName.substring(1, deploymentNodeName.length() - 1);
            DeploymentNodeEndVersionFunctions.setDeploymentNodeEndVersion(session, graphTag, deploymentNodeName,
                    curVersion, cmdb);
        }

        RelationshipEndVersionFunctions.setRelationshipsEndVersion(session, graphTag, curVersion, cmdb);
    }

    public static void deleteLocalGraph(Session session) {
        String deleteLocalGraph = "MATCH (n) WHERE n.graphTag = \"Local\" DETACH DELETE n";
        session.run(deleteLocalGraph);
    }

    public static void createGraph(Session session, String graphTag, Workspace workspace) throws Exception {

        Model model = workspace.getModel();
        String cmdb = model.getProperties().get("workspace_cmdb").toString();
        SoftwareSystem softwareSystem = ModelFunctions.getSoftwareSystem(model, cmdb);
        HashMap<String, GraphObject> objects = new HashMap<>();
        String curVersion = null;

        if (graphTag.equals("Global")) {
            curVersion = SoftwareSystemUpdateFunctions.updateSystem(session, graphTag, softwareSystem, cmdb, objects);
            Integer prevVersion = Integer.parseInt(curVersion) - 1;
            setEndVersion(session, graphTag, cmdb, prevVersion.toString());
        } else {
            deleteLocalGraph(session);
            SoftwareSystemUpdateFunctions.updateSystem(session, graphTag, softwareSystem, cmdb, objects);
        }

        ContainerUpdateFunctions.updateContainers(session, graphTag, model, softwareSystem, cmdb, curVersion, objects);
        SoftwareSystemRelationshipFunctions.updateSystemRelationships(session, graphTag, model, cmdb, curVersion,
                objects);
        DeploymentNodeUpdateFunctions.updateDeploymentNodes(session, graphTag, model, softwareSystem.getId(),
                cmdb, curVersion, objects);

        if (model.getDeploymentNodes() != null) {
            for (DeploymentNode deploymentNode : model.getDeploymentNodes()) {
                DeploymentNodeRelationshipFunctions.updateDeploymentNodeRelationships(session, graphTag, deploymentNode,
                        curVersion, cmdb, model, objects);
            }
        }

    }
}
