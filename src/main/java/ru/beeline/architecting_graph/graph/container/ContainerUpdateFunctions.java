package ru.beeline.architecting_graph.graph.container;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import ru.beeline.architecting_graph.graph.graphObject.GraphObject;
import ru.beeline.architecting_graph.graph.model.Model;
import ru.beeline.architecting_graph.graph.softwareSystem.SoftwareSystem;
import ru.beeline.architecting_graph.graph.commonFunctions.CommonFunctions;
import ru.beeline.architecting_graph.graph.relationship.RelationshipUpdateFunctions;
import ru.beeline.architecting_graph.graph.component.ComponentUpdateFunctions;

public class ContainerUpdateFunctions {

    public static void setContainerProperties(Session session, String graphTag, Container container) {
        if (container.getProperties() != null) {
            for (Map.Entry<String, Object> entry : container.getProperties().entrySet()) {
                String key = entry.getKey();
                key = key.replace(' ', '_');
                key = key.replace('.', '_');
                String setProperties = "MATCH (n:Container {graphTag: $graphTag1, name: $name1}) SET n." + key
                        + " = $value";
                Value parameters = Values.parameters("graphTag1", graphTag, "name1", container.getName(),
                        "value", entry.getValue());
                session.run(setProperties, parameters);
            }
        }
    }

    public static void setParametersForContainer(Session session, String graphTag, Container container,
            GraphObject containerGraphObject, String curVersion) {

        if (graphTag.equals("Global")
                && CommonFunctions.getObjectParameter(session, graphTag, containerGraphObject, "startVersion")
                        .toString().equals("NULL")) {

            CommonFunctions.setObjectParameter(session, graphTag, containerGraphObject, "startVersion", curVersion);
        }

        String setParameters = "MATCH (n:Container {graphTag: $graphTag1, " + containerGraphObject.getKey()
                + ": $value}) " + "SET n.description = $description1, n.technology = $technology1, n.tags = $tags1, "
                + "n.url = $url1, n.group = $group1, n.endVersion = $endVersion1";
        Value parameters = Values.parameters("graphTag1", graphTag, "value", containerGraphObject.getValue(),
                "description1", container.getDescription(), "technology1", container.getTechnology(), "tags1",
                container.getTags(), "url1", container.getUrl(), "group1", container.getGroup(), "endVersion1", null);
        session.run(setParameters, parameters);

        setContainerProperties(session, graphTag, container);
    }

    public static Integer getContainerNumberOfConnects(Session session, String graphTag, String containerExternalName) {
        String findConnects = "MATCH (n:Container {graphTag: $graphTag1, external_name: $external_name1})-[r]-() "
                + "RETURN count(r) AS numberOfRelationships";
        Value parameters = Values.parameters("graphTag1", graphTag, "external_name1", containerExternalName);
        Result result = session.run(findConnects, parameters);

        String numberOfRelationships = result.next().get("numberOfRelationships").toString();

        if (numberOfRelationships.equals("NULL")) {
            return 0;
        }

        return Integer.parseInt(numberOfRelationships);
    }

    public static Boolean needContainerReplace(Session session, String graphTag, Container container,
            GraphObject externalContainerGraphObject) {
        Integer numberOfRelationshipsFirst = getContainerNumberOfConnects(session, graphTag,
                externalContainerGraphObject.getValue());
        String source = CommonFunctions.getObjectParameter(session, graphTag, externalContainerGraphObject, "source")
                .toString();

        Integer numberOfRelationshipsSecond = 0;

        if (container.getRelationships() != null) {
            numberOfRelationshipsSecond = container.getRelationships().size();
        }

        if (container.getComponents() != null) {
            numberOfRelationshipsSecond = numberOfRelationshipsSecond + container.getComponents().size();
        }

        if (numberOfRelationshipsFirst >= numberOfRelationshipsSecond || source.equals("\"landscape\"")) {
            return false;
        }

        return true;
    }

    public static Boolean changeContainer(Session session, String graphTag, Container container,
            GraphObject externalContainerGraphObject, String curVersion, HashMap<String, GraphObject> objects) {

        String endVersion = CommonFunctions
                .getObjectParameter(session, graphTag, externalContainerGraphObject, "endVersion").toString();

        if (endVersion.equals("NULL")) {
            return needContainerReplace(session, graphTag, container, externalContainerGraphObject);
        }

        return true;
    }

    public static void updateContainer(Session session, String graphTag, Container container, String curVersion,
            HashMap<String, GraphObject> objects) {

        GraphObject containerGraphObject = GraphObject.createGraphObject("Container", "name", container.getName());

        boolean exists = CommonFunctions.checkIfObjectExists(session, graphTag, containerGraphObject);
        boolean externalExists = false;
        GraphObject externalContainerGraphObject = null;

        if (container.getProperties() != null && container.getProperties().containsKey("external_name")
                && container.getProperties().get("external_name") != null) {

            String containerExternalName = container.getProperties().get("external_name").toString();
            externalContainerGraphObject = GraphObject.createGraphObject("Container", "external_name",
                    containerExternalName);
            externalExists = CommonFunctions.checkIfObjectExists(session, graphTag, externalContainerGraphObject);
        }

        if (externalExists) {
            if (changeContainer(session, graphTag, container, externalContainerGraphObject, curVersion, objects)) {
                CommonFunctions.setObjectParameter(session, graphTag, externalContainerGraphObject, "external_name",
                        null);
            } else {
                container.getProperties().put("external_name", null);
            }
        }

        if (!exists) {
            CommonFunctions.createObject(session, graphTag, containerGraphObject);
        }

        objects.put(container.getId(), containerGraphObject);
        setParametersForContainer(session, graphTag, container, containerGraphObject, curVersion);
    }

    public static void updateContainers(Session session, String graphTag, Model model, SoftwareSystem softwareSystem,
            String cmdb, String curVersion, HashMap<String, GraphObject> objects) {

        if (softwareSystem.getContainers() != null) {
            for (Container container : softwareSystem.getContainers()) {

                container.setName(container.getName() + "." + cmdb.toString());

                String containerExternalName = cmdb;
                if (container.getProperties() != null && container.getProperties().containsKey("external_name")
                        && container.getProperties().get("external_name") != null) {

                    containerExternalName = container.getProperties().get("external_name").toString() + "."
                            + cmdb;
                    container.getProperties().put("external_name", containerExternalName);
                }

                updateContainer(session, graphTag, container, curVersion, objects);

                RelationshipUpdateFunctions.updateChildRelationship(session, graphTag, model, curVersion,
                        softwareSystem.getId(), container.getId(), cmdb, objects);

                ComponentUpdateFunctions.updateComponents(session, graphTag, model, container, cmdb, curVersion,
                        containerExternalName, objects);
            }
        }
    }
}
