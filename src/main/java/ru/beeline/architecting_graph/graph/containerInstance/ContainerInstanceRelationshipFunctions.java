package ru.beeline.architecting_graph.graph.containerInstance;

import java.util.HashMap;

import org.neo4j.driver.Session;

import ru.beeline.architecting_graph.graph.graphObject.GraphObject;
import ru.beeline.architecting_graph.graph.model.Model;
import ru.beeline.architecting_graph.graph.deploymentNode.DeploymentNode;
import ru.beeline.architecting_graph.graph.relationship.Relationship;
import ru.beeline.architecting_graph.graph.relationship.RelationshipUpdateFunctions;

public class ContainerInstanceRelationshipFunctions {

    public static void updateContainerInstanceRelationships(Session session, String graphTag,
            DeploymentNode deploymentNode, String curVersion, String cmdb, Model model,
            HashMap<String, GraphObject> objects) {

        if (deploymentNode.getContainerInstances() != null) {
            for (ContainerInstance containerInstance : deploymentNode.getContainerInstances()) {
                if (containerInstance.getRelationships() != null) {
                    for (Relationship relationship : containerInstance.getRelationships()) {
                        RelationshipUpdateFunctions.updateDefaultRelationship(session, graphTag, relationship, model,
                                curVersion, cmdb, "", objects);
                    }
                }
            }
        }
    }
}
