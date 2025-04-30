package ru.beeline.architecting_graph.service.graph.externalObjects;

import org.neo4j.driver.Session;
import ru.beeline.architecting_graph.model.Component;
import ru.beeline.architecting_graph.model.Container;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.model.Model;
import ru.beeline.architecting_graph.model.SoftwareSystem;
import ru.beeline.architecting_graph.service.graph.commonFunctions.CommonFunctions;
import ru.beeline.architecting_graph.service.graph.relationship.RelationshipUpdateFunctions;

import java.util.HashMap;

public class CreateExternalObjects {

    public static void createExternalSystem(Session session, String graphTag, SoftwareSystem softwareSystem,
                                            String cmdb, HashMap<String, GraphObject> objects) {

        GraphObject systemGraphObject = new GraphObject("SoftwareSystem", "cmdb", cmdb);
        boolean exists = CommonFunctions.checkIfObjectExists(session, graphTag, systemGraphObject);
        if (!exists) {
            CommonFunctions.createObject(session, graphTag, systemGraphObject);
            CommonFunctions.setObjectParameter(session, graphTag, systemGraphObject, "name", softwareSystem.getName());

            if (softwareSystem.getProperties() != null
                    && softwareSystem.getProperties().containsKey("structurizr.dsl.identifier")) {

                CommonFunctions.setObjectParameter(session, graphTag, systemGraphObject, "structurizr_dsl_identifier",
                        softwareSystem.getProperties().get("structurizr.dsl.identifier").toString());
            }
        }
        objects.put(softwareSystem.getId(), systemGraphObject);
    }

    public static void createExternalContainer(Session session, String graphTag, Container container,
                                               String containerExternalName, HashMap<String, GraphObject> objects) {

        GraphObject containerGraphObject = new GraphObject("Container", "external_name",
                containerExternalName);
        Boolean exists = CommonFunctions.checkIfObjectExists(session, graphTag, containerGraphObject);
        if (!exists) {
            CommonFunctions.createObject(session, graphTag, containerGraphObject);
            CommonFunctions.setObjectParameter(session, graphTag, containerGraphObject, "name", container.getName());

            if (container.getProperties() != null
                    && container.getProperties().containsKey("structurizr.dsl.identifier")) {

                CommonFunctions.setObjectParameter(session, graphTag, containerGraphObject,
                        "structurizr_dsl_identifier",
                        container.getProperties().get("structurizr.dsl.identifier").toString());
            }
        }
        objects.put(container.getId(), containerGraphObject);
    }

    public static void createExternalComponent(Session session, String graphTag, Component component,
                                               String componentExternalName, HashMap<String, GraphObject> objects) {

        GraphObject componentGraphObject = new GraphObject("Component", "external_name",
                componentExternalName);
        Boolean exists = CommonFunctions.checkIfObjectExists(session, graphTag, componentGraphObject);
        if (!exists) {
            CommonFunctions.createObject(session, graphTag, componentGraphObject);
            CommonFunctions.setObjectParameter(session, graphTag, componentGraphObject, "name", component.getName());

            if (component.getProperties() != null
                    && component.getProperties().containsKey("structurizr.dsl.identifier")) {

                CommonFunctions.setObjectParameter(session, graphTag, componentGraphObject,
                        "structurizr_dsl_identifier",
                        component.getProperties().get("structurizr.dsl.identifier").toString());
            }
        }
        objects.put(component.getId(), componentGraphObject);
    }

    public static void createExternalObject(Session session, String graphTag, Model model, String curVersion,
                                            String objectId, HashMap<String, GraphObject> objects) {

        for (SoftwareSystem softwareSystem : model.getSoftwareSystems()) {

            if (softwareSystem.getProperties() == null || !softwareSystem.getProperties().containsKey("cmdb")) {
                continue;
            }

            String cmdb = softwareSystem.getProperties().get("cmdb").toString();

            if (objectId.equals(softwareSystem.getId())) {
                createExternalSystem(session, graphTag, softwareSystem, cmdb, objects);
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
                        createExternalContainer(session, graphTag, container, containerExternalName, objects);
                        RelationshipUpdateFunctions.updateChildRelationship(session, graphTag, model, curVersion,
                                softwareSystem.getId(), container.getId(), cmdb, objects);
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

                                createExternalComponent(session, graphTag, component, componentExternalName,
                                        objects);
                                RelationshipUpdateFunctions.updateChildRelationship(session, graphTag, model,
                                        curVersion, container.getId(), component.getId(), cmdb, objects);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
