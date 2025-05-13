package ru.beeline.architecting_graph.service.graph;

import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import ru.beeline.architecting_graph.model.*;

import java.util.HashMap;
import java.util.Map;

public class ContainerInstanceUpdateFunctions {

    public static void setContainerInstanceProperties(Session session, String graphTag,
            ContainerInstance containerInstance, String containerInstanceName) {
        if (containerInstance.getProperties() != null) {
            for (Map.Entry<String, Object> entry : containerInstance.getProperties().entrySet()) {
                String key = entry.getKey();
                key = key.replaceAll("[^a-zA-Z0-9]", "_");
                String setProperties = "MATCH (n:ContainerInstance {graphTag: $graphTag1, name: $val1}) SET n." + key
                        + " = $value";
                Value parameters = Values.parameters("graphTag1", graphTag, "val1", containerInstanceName,
                        "value", entry.getValue());
                session.run(setProperties, parameters);
            }
        }
    }

    public static void setParametersForContainerInstance(Session session, String graphTag,
            ContainerInstance containerInstance, GraphObject containerInstanceGraphObject, String curVersion) {

        if (graphTag.equals("Global")
                && CommonFunctions.getObjectParameter(session, graphTag, containerInstanceGraphObject,
                        "startVersion").toString().equals("NULL")) {

            CommonFunctions.setObjectParameter(session, graphTag, containerInstanceGraphObject, "startVersion",
                    curVersion);
        }

        String updateNode = "MATCH (n:ContainerInstance {graphTag: $graphTag1, name: $val1}) SET "
                + "n.instanceId = $instanceId1, n.tags = $tags1, n.endVersion = $endVersion1";
        Value parameters = Values.parameters("graphTag1", graphTag, "val1", containerInstanceGraphObject.getValue(),
                "instanceId1", containerInstance.getInstanceId(), "tags1", containerInstance.getTags(), "endVersion1",
                null);
        session.run(updateNode, parameters);

        setContainerInstanceProperties(session, graphTag, containerInstance, containerInstanceGraphObject.getValue());
    }

    public static String getContainerforContainerInstance(Model model, String containerInstanceContainerId) {
        for (SoftwareSystem softwareSystem : model.getSoftwareSystems()) {
            if (softwareSystem.getContainers() != null) {
                for (Container container : softwareSystem.getContainers()) {
                    if (container.getId().equals(containerInstanceContainerId)) {
                        return container.getName().toString();
                    }
                }
            }
        }
        return null;
    }

    public static void updateContainerInstance(Session session, String graphTag, Model model,
            DeploymentNode deploymentNode, ContainerInstance containerInstance, String curVersion,
            HashMap<String, GraphObject> objects) {

        String containerInstanceName = getContainerforContainerInstance(model, containerInstance.getContainerId());

        if (containerInstanceName == null) {
            return;
        }

        containerInstanceName = containerInstanceName + ".ContainerInstance." + deploymentNode.getName().toString();

        GraphObject containerInstanceGraphObject = new GraphObject("ContainerInstance", "name",
                containerInstanceName);

        if (!CommonFunctions.checkIfObjectExists(session, graphTag, containerInstanceGraphObject)) {
            CommonFunctions.createObject(session, graphTag, containerInstanceGraphObject);
        }

        objects.put(containerInstance.getId(), containerInstanceGraphObject);
        setParametersForContainerInstance(session, graphTag, containerInstance, containerInstanceGraphObject,
                curVersion);
    }

    public static void setContainerInstanceEndVersion(Session session, String graphTag, String deploymentNodeName,
                                                      String curVersion) {

        String getContainerInstances = "MATCH (n:DeploymentNode "
                + "{name: $name1, graphTag: $graphTag1})-[r:Child]->(m:ContainerInstance) "
                + "WHERE m.endVersion IS NULL RETURN m.name AS containerInstanceName";
        Value parameters = Values.parameters("graphTag1", graphTag, "name1", deploymentNodeName);
        Result result = session.run(getContainerInstances, parameters);

        while (result.hasNext()) {
            String containerInstanceName = result.next().get("containerInstanceName").toString();
            containerInstanceName = containerInstanceName.substring(1, containerInstanceName.length() - 1);
            GraphObject containerInstanceGraphObject = new GraphObject("ContainerInstance", "name",
                    containerInstanceName);
            CommonFunctions.setObjectParameter(session, graphTag, containerInstanceGraphObject, "endVersion",
                    curVersion);
        }
    }

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
