package ru.beeline.architecting_graph.graph.externalObjects;

import java.util.HashMap;

import org.neo4j.driver.Session;

import ru.beeline.architecting_graph.graph.graphObject.GraphObject;
import ru.beeline.architecting_graph.graph.model.Model;
import ru.beeline.architecting_graph.graph.container.Container;
import ru.beeline.architecting_graph.graph.component.Component;
import ru.beeline.architecting_graph.graph.relationship.RelationshipUpdateFunctions;
import ru.beeline.architecting_graph.graph.softwareSystem.SoftwareSystem;
import ru.beeline.architecting_graph.graph.commonFunctions.CommonFunctions;

public class CreateExternalObjects {

    public static void createExternalSystem(Session session, String graphTag, String systemId, String cmdb,
            HashMap<String, GraphObject> objects) {

        GraphObject systemGraphObject = GraphObject.createGraphObject("SoftwareSystem", "cmdb", cmdb);
        boolean exists = CommonFunctions.checkIfObjectExists(session, graphTag, systemGraphObject);
        if (!exists) {
            CommonFunctions.createObject(session, graphTag, systemGraphObject);
        }
        objects.put(systemId, systemGraphObject);
    }

    public static void createExternalContainer(Session session, String graphTag, String containerId,
            String containerExternalName, HashMap<String, GraphObject> objects) {

        GraphObject containerGraphObject = GraphObject.createGraphObject("Container", "external_name",
                containerExternalName);
        Boolean exists = CommonFunctions.checkIfObjectExists(session, graphTag, containerGraphObject);
        if (!exists) {
            CommonFunctions.createObject(session, graphTag, containerGraphObject);
        }
        objects.put(containerId, containerGraphObject);
    }

    public static void createExternalComponent(Session session, String graphTag, String componentId,
            String componentExternalName, HashMap<String, GraphObject> objects) {

        GraphObject componentGraphObject = GraphObject.createGraphObject("Component", "external_name",
                componentExternalName);
        Boolean exists = CommonFunctions.checkIfObjectExists(session, graphTag, componentGraphObject);
        if (!exists) {
            CommonFunctions.createObject(session, graphTag, componentGraphObject);
        }
        objects.put(componentId, componentGraphObject);
    }

    public static void createExternalObject(Session session, String graphTag, Model model, String curVersion,
            String objectId, HashMap<String, GraphObject> objects) {

        for (SoftwareSystem softwareSystem : model.getSoftwareSystems()) {

            if (softwareSystem.getProperties() == null || !softwareSystem.getProperties().containsKey("cmdb")) {
                continue;
            }

            String cmdb = softwareSystem.getProperties().get("cmdb").toString();

            if (objectId.equals(softwareSystem.getId())) {
                createExternalSystem(session, graphTag, softwareSystem.getId(), cmdb, objects);
                return;
            }

            if (softwareSystem.getContainers() != null) {
                for (Container container : softwareSystem.getContainers()) {

                    String containerExternalName = cmdb;
                    if (container.getProperties() != null && container.getProperties().containsKey("external_name")
                            && container.getProperties().get("external_name") != null) {

                        containerExternalName = container.getProperties().get("external_name").toString() + "." + cmdb;
                    }

                    if (objectId.equals(container.getId())) {
                        createExternalContainer(session, graphTag, container.getId(), containerExternalName, objects);
                        RelationshipUpdateFunctions.updateChildRelationship(session, graphTag, model, curVersion,
                                softwareSystem.getId(),
                                container.getId(), cmdb, objects);
                        return;
                    }

                    if (container.getComponents() != null) {
                        for (Component component : container.getComponents()) {
                            if (objectId.equals(component.getId())) {

                                String componentExternalName = containerExternalName;
                                if (component.getProperties() != null
                                        && component.getProperties().containsKey("external_name")
                                        && component.getProperties().get("external_name") != null) {

                                    componentExternalName = component.getProperties().get("external_name").toString()
                                            + "." + containerExternalName;
                                }

                                createExternalComponent(session, graphTag, component.getId(), componentExternalName,
                                        objects);
                                RelationshipUpdateFunctions.updateChildRelationship(session, graphTag, model,
                                        curVersion,
                                        container.getId(),
                                        component.getId(), cmdb, objects);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
