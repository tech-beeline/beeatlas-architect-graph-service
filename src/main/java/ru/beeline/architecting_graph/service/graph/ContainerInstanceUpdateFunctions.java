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
public class ContainerInstanceUpdateFunctions {

    @Autowired
    BuildGraphQuery buildGraphQuery;

    @Autowired
    CreateExternalObjects createExternalObjects;

    public void setContainerInstanceProperties(Session session, String graphTag,
                                               ContainerInstance containerInstance, String containerInstanceName) {
        if (containerInstance.getProperties() != null) {
            for (Map.Entry<String, Object> entry : containerInstance.getProperties().entrySet()) {
                String key = entry.getKey().replaceAll("[^a-zA-Z0-9]", "_"); // 👉 оставить тут как бизнес-логику
                buildGraphQuery.setProperty(session, graphTag, containerInstanceName, key, entry.getValue());
            }
        }
    }

    public void setParametersForContainerInstance(Session session, String graphTag,
                                                  ContainerInstance containerInstance,
                                                  GraphObject containerInstanceGraphObject,
                                                  String curVersion) {

        if (graphTag.equals("Global")
                && buildGraphQuery.getObjectParameter(session, graphTag, containerInstanceGraphObject, "startVersion")
                .toString().equals("NULL")) {
            buildGraphQuery.setObjectParameter(session, graphTag, containerInstanceGraphObject, "startVersion", curVersion);
        }

        buildGraphQuery.updateContainerInstance(session, graphTag,
                containerInstanceGraphObject.getValue(),
                containerInstance.getInstanceId(),
                containerInstance.getTags());

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

    public void updateContainerInstance(Session session, String graphTag, Model model,
                                        DeploymentNode deploymentNode, ContainerInstance containerInstance, String curVersion,
                                        HashMap<String, GraphObject> objects) {
        String containerInstanceName = getContainerforContainerInstance(model, containerInstance.getContainerId());
        if (containerInstanceName == null) {
            return;
        }
        containerInstanceName = containerInstanceName + ".ContainerInstance." + deploymentNode.getName().toString();
        GraphObject containerInstanceGraphObject = new GraphObject("ContainerInstance", "name",
                containerInstanceName);
        if (!buildGraphQuery.checkIfObjectExists(session, graphTag, containerInstanceGraphObject)) {
            buildGraphQuery.createObject(session, graphTag, containerInstanceGraphObject);
        }
        objects.put(containerInstance.getId(), containerInstanceGraphObject);
        setParametersForContainerInstance(session, graphTag, containerInstance, containerInstanceGraphObject,
                curVersion);
    }

    public void setContainerInstanceEndVersion(Session session, String graphTag, String deploymentNodeName,
                                               String curVersion) {
        Result result = buildGraphQuery.findContainerInstanceNamesWithNullEndVersion(session, graphTag, deploymentNodeName);
        while (result.hasNext()) {
            String rawName = result.next().get("containerInstanceName").toString();
            String cleanedName = rawName.substring(1, rawName.length() - 1);
            GraphObject containerInstanceGraphObject = new GraphObject("ContainerInstance", "name", cleanedName);
            buildGraphQuery.setObjectParameter(session, graphTag, containerInstanceGraphObject, "endVersion", curVersion);
        }
    }

    public void updateContainerInstanceRelationships(Session session, String graphTag,
                                                     DeploymentNode deploymentNode, String curVersion, String cmdb, Model model,
                                                     HashMap<String, GraphObject> objects) {
        if (deploymentNode.getContainerInstances() != null) {
            for (ContainerInstance containerInstance : deploymentNode.getContainerInstances()) {
                if (containerInstance.getRelationships() != null) {
                    for (RelationshipEntity relationship : containerInstance.getRelationships()) {
                        createExternalObjects.updateDefaultRelationship(session, graphTag, relationship, model,
                                curVersion, cmdb, "", objects);
                    }
                }
            }
        }
    }
}
