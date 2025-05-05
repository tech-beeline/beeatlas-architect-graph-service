package ru.beeline.architecting_graph.service.graph.deploymentNode;

import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import ru.beeline.architecting_graph.model.ContainerInstance;
import ru.beeline.architecting_graph.model.DeploymentNode;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.model.InfrastructureNode;
import ru.beeline.architecting_graph.model.Model;
import ru.beeline.architecting_graph.service.graph.commonFunctions.CommonFunctions;
import ru.beeline.architecting_graph.service.graph.containerInstance.ContainerInstanceUpdateFunctions;
import ru.beeline.architecting_graph.service.graph.environment.EnvironmentFunctions;
import ru.beeline.architecting_graph.service.graph.infrastructureNode.InfrastructureNodeUpdateFunctions;
import ru.beeline.architecting_graph.service.graph.relationship.RelationshipUpdateFunctions;

import java.util.HashMap;
import java.util.Map;

public class DeploymentNodeUpdateFunctions {

        public static void setDeploymentNodeProperties(Session session, String graphTag,
                        DeploymentNode deploymentNode) {
                if (deploymentNode.getProperties() != null) {
                        for (Map.Entry<String, Object> entry : deploymentNode.getProperties().entrySet()) {
                                String key = entry.getKey();
                                key = key.replaceAll("[^a-zA-Z0-9]", "_");
                                String setProperties = "MATCH (n:DeploymentNode {graphTag: $graphTag1, name: $name1}) SET n."
                                                + key + " = $value";
                                Value parameters = Values.parameters("graphTag1", graphTag, "name1",
                                                deploymentNode.getName(), "value", entry.getValue());
                                session.run(setProperties, parameters);
                        }
                }
        }

        public static void setParametersForDeploymentNode(Session session, String graphTag,
                        DeploymentNode deploymentNode, GraphObject deploymentNodeGraphObject, String curVersion) {

                if (graphTag.equals("Global")
                                && CommonFunctions.getObjectParameter(session, graphTag, deploymentNodeGraphObject,
                                                "startVersion").toString().equals("NULL")) {

                        CommonFunctions.setObjectParameter(session, graphTag, deploymentNodeGraphObject, "startVersion",
                                        curVersion);
                }

                String updateNode = "MATCH (n:DeploymentNode {graphTag: $graphTag1, name: $name1}) "
                                + "SET n.description = $description1, n.technology = $technology1, n.instances = $instances1, "
                                + "n.tags = $tags1, n.url = $url1, n.endVersion = $endVersion1";
                Value parameters = Values.parameters("graphTag1", graphTag, "name1", deploymentNode.getName(),
                                "description1", deploymentNode.getDescription(), "technology1",
                                deploymentNode.getTechnology(),
                                "instances1", deploymentNode.getInstances(), "tags1", deploymentNode.getTags(), "url1",
                                deploymentNode.getUrl(), "endVersion1", null);
                session.run(updateNode, parameters);

                setDeploymentNodeProperties(session, graphTag, deploymentNode);
        }

        public static void createEnvironmentRelation(Session session, String graphTag, String environment,
                        String NodeId, String curVersion, String cmdb, Model model,
                        HashMap<String, GraphObject> objects) {

                EnvironmentFunctions.updateEnvironment(session, graphTag, environment, objects);
                RelationshipUpdateFunctions.updateChildRelationship(session, graphTag, model, curVersion, environment,
                                NodeId,
                                cmdb, objects);
        }

        public static void updateChildInfrastructureNodes(Session session, String graphTag,
                        DeploymentNode deploymentNode,
                        String curVersion, String cmdb, Model model, HashMap<String, GraphObject> objects) {

                if (deploymentNode.getInfrastructureNodes() != null) {
                        for (InfrastructureNode infrastructureNode : deploymentNode.getInfrastructureNodes()) {

                                infrastructureNode.setName(infrastructureNode.getName() + "."
                                                + deploymentNode.getName().toString());
                                InfrastructureNodeUpdateFunctions.updateInfrastructureNode(session, graphTag,
                                                infrastructureNode, curVersion,
                                                objects);

                                RelationshipUpdateFunctions.updateChildRelationship(session, graphTag, model,
                                                curVersion,
                                                deploymentNode.getId(), infrastructureNode.getId(), cmdb, objects);

                                createEnvironmentRelation(session, graphTag, infrastructureNode.getEnvironment(),
                                                infrastructureNode.getId(), curVersion, cmdb, model, objects);
                        }
                }
        }

        public static void updateChildContainerInstances(Session session, String graphTag,
                        DeploymentNode deploymentNode, String curVersion, String cmdb, Model model,
                        HashMap<String, GraphObject> objects) {

                if (deploymentNode.getContainerInstances() != null) {
                        for (ContainerInstance containerInstance : deploymentNode.getContainerInstances()) {

                                ContainerInstanceUpdateFunctions.updateContainerInstance(session, graphTag, model,
                                                deploymentNode,
                                                containerInstance,
                                                curVersion, objects);

                                RelationshipUpdateFunctions.updateDeployRelationship(session, graphTag, model,
                                                curVersion,
                                                containerInstance.getContainerId(), containerInstance.getId(), cmdb,
                                                objects);

                                RelationshipUpdateFunctions.updateChildRelationship(session, graphTag, model,
                                                curVersion,
                                                deploymentNode.getId(), containerInstance.getId(), cmdb, objects);

                                createEnvironmentRelation(session, graphTag, containerInstance.getEnvironment(),
                                                containerInstance.getId(), curVersion, cmdb, model, objects);
                        }
                }
        }

        public static void updateChildDeploymentNodes(Session session, String graphTag, DeploymentNode deploymentNode,
                        String curVersion, String cmdb, Model model, HashMap<String, GraphObject> objects) {

                if (deploymentNode.getChildren() != null) {
                        for (DeploymentNode childDeploymentNode : deploymentNode.getChildren()) {

                                childDeploymentNode.setName(childDeploymentNode.getName() + "."
                                                + deploymentNode.getName().toString());
                                updateDeploymentNode(session, graphTag, childDeploymentNode, curVersion, cmdb, model,
                                                objects);

                                RelationshipUpdateFunctions.updateChildRelationship(session, graphTag, model,
                                                curVersion,
                                                deploymentNode.getId(), childDeploymentNode.getId(), cmdb, objects);
                        }
                }
        }

        public static void updateDeploymentNode(Session session, String graphTag, DeploymentNode deploymentNode,
                        String curVersion, String cmdb, Model model, HashMap<String, GraphObject> objects) {

                GraphObject deploymentNodeGraphObject = new GraphObject("DeploymentNode", "name",
                                deploymentNode.getName());

                boolean exists = CommonFunctions.checkIfObjectExists(session, graphTag, deploymentNodeGraphObject);
                if (!exists) {
                        CommonFunctions.createObject(session, graphTag, deploymentNodeGraphObject);
                }

                objects.put(deploymentNode.getId(), deploymentNodeGraphObject);
                setParametersForDeploymentNode(session, graphTag, deploymentNode, deploymentNodeGraphObject,
                                curVersion);
                createEnvironmentRelation(session, graphTag, deploymentNode.getEnvironment(), deploymentNode.getId(),
                                curVersion, cmdb, model, objects);

                updateChildInfrastructureNodes(session, graphTag, deploymentNode, curVersion, cmdb, model, objects);
                updateChildContainerInstances(session, graphTag, deploymentNode, curVersion, cmdb, model, objects);
                updateChildDeploymentNodes(session, graphTag, deploymentNode, curVersion, cmdb, model, objects);
        }

        public static void updateDeploymentNodes(Session session, String graphTag, Model model, String softwareSystemId,
                        String cmdb, String curVersion, HashMap<String, GraphObject> objects) {

                if (model.getDeploymentNodes() != null) {
                        for (DeploymentNode deploymentNode : model.getDeploymentNodes()) {
                                deploymentNode.setName(deploymentNode.getName() + "." + cmdb.toString());
                                updateDeploymentNode(session, graphTag, deploymentNode, curVersion, cmdb, model,
                                                objects);

                                RelationshipUpdateFunctions.updateChildRelationship(session, graphTag, model,
                                                curVersion, softwareSystemId, deploymentNode.getId(), cmdb, objects);
                        }
                }
        }
}
