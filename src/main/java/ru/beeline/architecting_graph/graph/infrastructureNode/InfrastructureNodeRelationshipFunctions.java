package ru.beeline.architecting_graph.graph.infrastructureNode;

import java.util.HashMap;

import org.neo4j.driver.Session;

import ru.beeline.architecting_graph.graph.model.Model;
import ru.beeline.architecting_graph.graph.deploymentNode.DeploymentNode;
import ru.beeline.architecting_graph.graph.relationship.Relationship;
import ru.beeline.architecting_graph.graph.graphObject.GraphObject;
import ru.beeline.architecting_graph.graph.relationship.RelationshipUpdateFunctions;

public class InfrastructureNodeRelationshipFunctions {

    public static void updateInfrastructureNodeRelationships(Session session, String graphTag,
            DeploymentNode deploymentNode, String curVersion, String cmdb, Model model,
            HashMap<String, GraphObject> objects) {

        if (deploymentNode.getInfrastructureNodes() != null) {
            for (InfrastructureNode infrastructureNode : deploymentNode.getInfrastructureNodes()) {
                if (infrastructureNode.getRelationships() != null) {
                    for (Relationship relationship : infrastructureNode.getRelationships()) {
                        RelationshipUpdateFunctions.updateDefaultRelationship(session, graphTag, relationship, model,
                                curVersion, cmdb, "", objects);
                    }
                }
            }
        }
    }
}
