package ru.beeline.architecting_graph.service.graph;

import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.model.*;
import ru.beeline.architecting_graph.repository.neo4j.BuildGraphQuery;

import java.util.HashMap;
import java.util.Map;

@Service
public class DeploymentNodeUpdateFunctions {

    @Autowired
    BuildGraphQuery buildGraphQuery;

    @Autowired
    ContainerInstanceUpdateFunctions containerInstanceUpdateFunctions;

    @Autowired
    InfrastructureNodeUpdateFunctions infrastructureNodeUpdateFunctions;

    @Autowired
    CreateExternalObjects createExternalObjects;

    public void setDeploymentNodeProperties(Session session, String graphTag, DeploymentNode deploymentNode) {
        if (deploymentNode.getProperties() != null) {
            for (Map.Entry<String, Object> entry : deploymentNode.getProperties().entrySet()) {
                String key = entry.getKey().replaceAll("[^a-zA-Z0-9]", "_");
                buildGraphQuery.setDeploymentNodeProperty(session, graphTag, deploymentNode.getName(), key, entry.getValue());
            }
        }
    }

    public void setParametersForDeploymentNode(Session session, String graphTag, DeploymentNode deploymentNode,
                                               GraphObject deploymentNodeGraphObject, String curVersion) {
        if (graphTag.equals("Global") && buildGraphQuery.getObjectParameter(session, graphTag, deploymentNodeGraphObject,
                "startVersion").toString().equals("NULL")) {
            buildGraphQuery.setObjectParameter(session, graphTag, deploymentNodeGraphObject, "startVersion", curVersion);
        }
        buildGraphQuery.updateDeploymentNode(session, graphTag, deploymentNode);
        setDeploymentNodeProperties(session, graphTag, deploymentNode);
    }

    public void createEnvironmentRelation(Session session, String graphTag, String environment, String NodeId, String curVersion,
                                          String cmdb, Model model, HashMap<String, GraphObject> objects) {
        updateEnvironment(session, graphTag, environment, objects);
        createExternalObjects.updateChildRelationship(session, graphTag, model, curVersion, environment,
                NodeId,
                cmdb, objects);
    }

    public void updateEnvironment(Session session, String graphTag, String environment,
                                  HashMap<String, GraphObject> objects) {
        GraphObject environmentGraphObject = new GraphObject("Environment", "name", environment);
        if (!buildGraphQuery.checkIfObjectExists(session, graphTag, environmentGraphObject)) {
            buildGraphQuery.createObject(session, graphTag, environmentGraphObject);
        }
        if (!objects.containsKey(environment)) {
            objects.put(environment, environmentGraphObject);
        }
    }

    public void updateChildInfrastructureNodes(Session session, String graphTag, DeploymentNode deploymentNode,
                                               String curVersion, String cmdb, Model model, HashMap<String, GraphObject> objects) {
        if (deploymentNode.getInfrastructureNodes() != null) {
            for (InfrastructureNode infrastructureNode : deploymentNode.getInfrastructureNodes()) {

                infrastructureNode.setName(infrastructureNode.getName() + "."
                        + deploymentNode.getName().toString());
                infrastructureNodeUpdateFunctions.updateInfrastructureNode(session, graphTag, infrastructureNode, curVersion,
                        objects);
                createExternalObjects.updateChildRelationship(session, graphTag, model, curVersion,
                        deploymentNode.getId(), infrastructureNode.getId(), cmdb, objects);
                createEnvironmentRelation(session, graphTag, infrastructureNode.getEnvironment(), infrastructureNode.getId(),
                        curVersion, cmdb, model, objects);
            }
        }
    }

    public void updateChildContainerInstances(Session session, String graphTag, DeploymentNode deploymentNode, String curVersion,
                                              String cmdb, Model model, HashMap<String, GraphObject> objects) {
        if (deploymentNode.getContainerInstances() != null) {
            for (ContainerInstance containerInstance : deploymentNode.getContainerInstances()) {
                containerInstanceUpdateFunctions.updateContainerInstance(session, graphTag, model, deploymentNode,
                        containerInstance, curVersion, objects);
                createExternalObjects.updateDeployRelationship(session, graphTag, model, curVersion,
                        containerInstance.getContainerId(), containerInstance.getId(), cmdb, objects);
                createExternalObjects.updateChildRelationship(session, graphTag, model, curVersion, deploymentNode.getId(),
                        containerInstance.getId(), cmdb, objects);
                createEnvironmentRelation(session, graphTag, containerInstance.getEnvironment(), containerInstance.getId(),
                        curVersion, cmdb, model, objects);
            }
        }
    }

    public void updateChildDeploymentNodes(Session session, String graphTag, DeploymentNode deploymentNode,
                                           String curVersion, String cmdb, Model model, HashMap<String, GraphObject> objects) {
        if (deploymentNode.getChildren() != null) {
            for (DeploymentNode childDeploymentNode : deploymentNode.getChildren()) {
                childDeploymentNode.setName(childDeploymentNode.getName() + "." + deploymentNode.getName().toString());
                updateDeploymentNode(session, graphTag, childDeploymentNode, curVersion, cmdb, model, objects);
                createExternalObjects.updateChildRelationship(session, graphTag, model, curVersion, deploymentNode.getId(),
                        childDeploymentNode.getId(), cmdb, objects);
            }
        }
    }

    public void updateDeploymentNode(Session session, String graphTag, DeploymentNode deploymentNode,
                                     String curVersion, String cmdb, Model model, HashMap<String, GraphObject> objects) {
        GraphObject deploymentNodeGraphObject = new GraphObject("DeploymentNode", "name", deploymentNode.getName());
        boolean exists = buildGraphQuery.checkIfObjectExists(session, graphTag, deploymentNodeGraphObject);
        if (!exists) {
            buildGraphQuery.createObject(session, graphTag, deploymentNodeGraphObject);
        }
        objects.put(deploymentNode.getId(), deploymentNodeGraphObject);
        setParametersForDeploymentNode(session, graphTag, deploymentNode, deploymentNodeGraphObject, curVersion);
        createEnvironmentRelation(session, graphTag, deploymentNode.getEnvironment(), deploymentNode.getId(),
                curVersion, cmdb, model, objects);
        updateChildInfrastructureNodes(session, graphTag, deploymentNode, curVersion, cmdb, model, objects);
        updateChildContainerInstances(session, graphTag, deploymentNode, curVersion, cmdb, model, objects);
        updateChildDeploymentNodes(session, graphTag, deploymentNode, curVersion, cmdb, model, objects);
    }

    public void setChildDeploymentNodeEndVersion(Session session, String graphTag, String deploymentNodeName,
                                                 String curVersion, String cmdb) {
        Result result = buildGraphQuery.findChildDeploymentNodesWithNullEndVersion(session, graphTag, deploymentNodeName);
        while (result.hasNext()) {
            String childDeploymentNodeName = result.next().get("childDeploymentNodeName").toString();
            childDeploymentNodeName = childDeploymentNodeName.substring(1, childDeploymentNodeName.length() - 1);
            setDeploymentNodeEndVersion(session, graphTag, childDeploymentNodeName, curVersion, cmdb);
        }
    }

    public void setDeploymentNodeEndVersion(Session session, String graphTag, String deploymentNodeName,
                                            String curVersion, String cmdb) {
        GraphObject deploymentNodeGraphObject = new GraphObject("DeploymentNode", "name", deploymentNodeName);
        buildGraphQuery.setObjectParameter(session, graphTag, deploymentNodeGraphObject, "endVersion", curVersion);
        infrastructureNodeUpdateFunctions.setInfrastructureNodeEndVersion(session, graphTag, deploymentNodeName, curVersion);
        containerInstanceUpdateFunctions.setContainerInstanceEndVersion(session, graphTag, deploymentNodeName, curVersion);
        setChildDeploymentNodeEndVersion(session, graphTag, deploymentNodeName, curVersion, cmdb);
    }

    public void updateChildDeploymentNodeRelationships(Session session, String graphTag, DeploymentNode deploymentNode,
                                                       String curVersion, String cmdb, Model model, HashMap<String, GraphObject> objects) {
        if (deploymentNode.getChildren() != null) {
            for (DeploymentNode childDeploymentNode : deploymentNode.getChildren()) {
                updateDeploymentNodeRelationships(session, graphTag, childDeploymentNode, curVersion, cmdb, model, objects);
            }
        }
    }

    public void updateDeploymentNodeRelationships(Session session, String graphTag, DeploymentNode deploymentNode, String curVersion,
                                                  String cmdb, Model model, HashMap<String, GraphObject> objects) {
        if (deploymentNode.getRelationships() != null) {
            for (RelationshipEntity relationship : deploymentNode.getRelationships()) {
                createExternalObjects.updateDefaultRelationship(session, graphTag, relationship, model, curVersion, cmdb, "", objects);
            }
        }
        updateInfrastructureNodeRelationships(session, graphTag, deploymentNode, curVersion, cmdb, model, objects);
        containerInstanceUpdateFunctions.updateContainerInstanceRelationships(session, graphTag, deploymentNode,
                curVersion, cmdb, model, objects);
        updateChildDeploymentNodeRelationships(session, graphTag, deploymentNode, curVersion, cmdb, model, objects);
    }

    public void updateInfrastructureNodeRelationships(Session session, String graphTag, DeploymentNode deploymentNode,
                                                      String curVersion, String cmdb, Model model, HashMap<String, GraphObject> objects) {
        if (deploymentNode.getInfrastructureNodes() != null) {
            for (InfrastructureNode infrastructureNode : deploymentNode.getInfrastructureNodes()) {
                if (infrastructureNode.getRelationships() != null) {
                    for (RelationshipEntity relationship : infrastructureNode.getRelationships()) {
                        createExternalObjects.updateDefaultRelationship(session, graphTag, relationship, model, curVersion,
                                cmdb, "", objects);
                    }
                }
            }
        }
    }
}
