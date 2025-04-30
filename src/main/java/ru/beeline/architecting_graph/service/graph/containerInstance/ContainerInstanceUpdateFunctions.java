package ru.beeline.architecting_graph.service.graph.containerInstance;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.Session;

import ru.beeline.architecting_graph.model.ContainerInstance;
import ru.beeline.architecting_graph.model.Model;
import ru.beeline.architecting_graph.model.SoftwareSystem;
import ru.beeline.architecting_graph.model.Container;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.service.graph.commonFunctions.CommonFunctions;

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
            ContainerInstance containerInstance, String curVersion, HashMap<String, GraphObject> objects) {

        String containerInstanceName = getContainerforContainerInstance(model, containerInstance.getContainerId());

        if (containerInstanceName == null) {
            return;
        }

        containerInstanceName = "ContainerInstance." + containerInstanceName;

        GraphObject containerInstanceGraphObject = GraphObject.createGraphObject("ContainerInstance", "name",
                containerInstanceName);

        if (!CommonFunctions.checkIfObjectExists(session, graphTag, containerInstanceGraphObject)) {
            CommonFunctions.createObject(session, graphTag, containerInstanceGraphObject);
        }

        objects.put(containerInstance.getId(), containerInstanceGraphObject);
        setParametersForContainerInstance(session, graphTag, containerInstance, containerInstanceGraphObject,
                curVersion);
    }
}
