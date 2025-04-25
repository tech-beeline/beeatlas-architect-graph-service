package ru.beeline.architecting_graph.graph.deploymentNode;

import java.util.HashMap;

import org.neo4j.driver.Session;

import ru.beeline.architecting_graph.graph.model.Model;
import ru.beeline.architecting_graph.graph.relationship.Relationship;
import ru.beeline.architecting_graph.graph.relationship.RelationshipUpdateFunctions;
import ru.beeline.architecting_graph.graph.graphObject.GraphObject;
import ru.beeline.architecting_graph.graph.infrastructureNode.InfrastructureNodeRelationshipFunctions;
import ru.beeline.architecting_graph.graph.containerInstance.ContainerInstanceRelationshipFunctions;

public class DeploymentNodeRelationshipFunctions {

    public static void updateChildDeploymentNodeRelationships(Session session, String graphTag,
            DeploymentNode deploymentNode, String curVersion, String cmdb, Model model,
            HashMap<String, GraphObject> objects) {

        if (deploymentNode.getChildren() != null) {
            for (DeploymentNode childDeploymentNode : deploymentNode.getChildren()) {
                updateDeploymentNodeRelationships(session, graphTag, childDeploymentNode, curVersion, cmdb, model,
                        objects);
            }
        }
    }

    public static void updateDeploymentNodeRelationships(Session session, String graphTag,
            DeploymentNode deploymentNode, String curVersion, String cmdb, Model model,
            HashMap<String, GraphObject> objects) {

        if (deploymentNode.getRelationships() != null) {
            for (Relationship relationship : deploymentNode.getRelationships()) {
                RelationshipUpdateFunctions.updateDefaultRelationship(session, graphTag, relationship, model,
                        curVersion,
                        cmdb, "", objects);
            }
        }

        InfrastructureNodeRelationshipFunctions.updateInfrastructureNodeRelationships(session, graphTag, deploymentNode,
                curVersion, cmdb, model, objects);
        ContainerInstanceRelationshipFunctions.updateContainerInstanceRelationships(session, graphTag, deploymentNode,
                curVersion, cmdb, model, objects);
        updateChildDeploymentNodeRelationships(session, graphTag, deploymentNode, curVersion, cmdb, model, objects);
    }
}
