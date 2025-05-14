package ru.beeline.architecting_graph.service.graph;

import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.model.Component;
import ru.beeline.architecting_graph.model.Connection;
import ru.beeline.architecting_graph.model.Container;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.model.Model;
import ru.beeline.architecting_graph.model.Relationship;
import ru.beeline.architecting_graph.model.SoftwareSystem;
import ru.beeline.architecting_graph.repository.neo4j.BuildGraphQuery;

import java.util.HashMap;
import java.util.Map;

@Service
public class CreateExternalObjects {

    @Autowired
    BuildGraphQuery buildGraphQuery;

    public void createExternalSystem(Session session, String graphTag, SoftwareSystem softwareSystem,
                                     String cmdb, HashMap<String, GraphObject> objects) {

        GraphObject systemGraphObject = new GraphObject("SoftwareSystem", "cmdb", cmdb);
        boolean exists = buildGraphQuery.checkIfObjectExists(session, graphTag, systemGraphObject);
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

    public void createExternalContainer(Session session, String graphTag, Container container,
                                        String containerExternalName, HashMap<String, GraphObject> objects) {

        GraphObject containerGraphObject = new GraphObject("Container", "external_name",
                containerExternalName);
        Boolean exists = buildGraphQuery.checkIfObjectExists(session, graphTag, containerGraphObject);
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

    public void createExternalComponent(Session session, String graphTag, Component component,
                                        String componentExternalName, HashMap<String, GraphObject> objects) {

        GraphObject componentGraphObject = new GraphObject("Component", "external_name",
                componentExternalName);
        Boolean exists = buildGraphQuery.checkIfObjectExists(session, graphTag, componentGraphObject);
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

    public void createExternalObject(Session session, String graphTag, Model model, String curVersion,
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
                        updateChildRelationship(session, graphTag, model, curVersion,
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
                                updateChildRelationship(session, graphTag, model,
                                        curVersion, container.getId(), component.getId(), cmdb, objects);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    public static void createRelationship(Session session, String graphTag, Relationship relationship,
                                          Connection connection) {

        String createRelationshipQuery = "MATCH (a:" + connection.getSource().getType() + " {graphTag: $graphTag1, "
                + connection.getSource().getKey() + ": $value1}), (b:" + connection.getDestination().getType()
                + " {graphTag: $graphTag1, " + connection.getDestination().getKey() + ": $value2}) CREATE (a)-[r:"
                + connection.getRelationshipType()
                + " {graphTag: $graphTag1, sourceWorkspace: $cmdb, description: $description1}]->(b) RETURN a, b";
        Value parameters = Values.parameters("graphTag1", graphTag, "value1", connection.getSource().getValue(),
                "cmdb", connection.getCmdb(), "value2", connection.getDestination().getValue(),
                "description1", relationship.getDescription());
        session.run(createRelationshipQuery, parameters);
    }

    public static void setRelationshipProperties(Session session, String graphTag, Relationship relationship,
                                                 Connection connection) {
        if (relationship.getProperties() != null) {
            for (Map.Entry<String, Object> entry : relationship.getProperties().entrySet()) {
                String key = entry.getKey();
                key = key.replaceAll("[^a-zA-Z0-9]", "_");
                String setProperties = "MATCH (a:" + connection.getSource().getType() + " {graphTag: $graphTag1, "
                        + connection.getSource().getKey() + ": $val1})-[r:" + connection.getRelationshipType()
                        + " {graphTag: $graphTag1, sourceWorkspace: $cmdb, description: $description1}]->(b:"
                        + connection.getDestination().getType() + " {graphTag: $graphTag1, "
                        + connection.getDestination().getKey() + ": $val2}) SET r." + key + " = $value";
                Value parameters = Values.parameters("graphTag1", graphTag, "val1",
                        connection.getSource().getKey(), "cmdb", connection.getCmdb(), "description1",
                        relationship.getDescription(), "val2", connection.getDestination().getValue(), "value",
                        entry.getValue());
                session.run(setProperties, parameters);
            }
        }
    }

    public static Value getRelationshipParameter(Session session, String graphTag, String realtionshipDescription,
                                                 Connection connection, String parameter) {

        String getParameter = "MATCH (a:" + connection.getSource().getType() + " {graphTag: $graphTag1, "
                + connection.getSource().getKey() + ": $val1})-[r:" + connection.getRelationshipType()
                + " {graphTag: $graphTag1, sourceWorkspace: $cmdb, description: $description1}]->(b:"
                + connection.getDestination().getType() + " {graphTag: $graphTag1, "
                + connection.getDestination().getKey() + ": $val2}) RETURN r." + parameter + " AS parameter";
        Value parameters = Values.parameters("graphTag1", graphTag, "val1",
                connection.getSource().getValue(), "cmdb", connection.getCmdb(), "description1",
                realtionshipDescription, "val2", connection.getDestination().getValue());
        Result result = session.run(getParameter, parameters);

        return result.next().get("parameter");
    }

    public static void setRelationshipParameter(Session session, String graphTag, String realtionshipDescription,
                                                Connection connection, String parameter, String value) {

        String setParameter = "MATCH (a:" + connection.getSource().getType() + " {graphTag: $graphTag1, "
                + connection.getSource().getKey() + ": $val1})-[r:" + connection.getRelationshipType()
                + " {graphTag: $graphTag1, sourceWorkspace: $cmdb, description: $description1}]->(b:"
                + connection.getDestination().getType() + " {graphTag: $graphTag1, "
                + connection.getDestination().getKey() + ": $val2}) SET r." + parameter + " = $parameter";
        Value parameters = Values.parameters("graphTag1", graphTag, "val1",
                connection.getSource().getValue(), "cmdb", connection.getCmdb(), "description1",
                realtionshipDescription, "val2", connection.getDestination().getValue(), "parameter", value);
        session.run(setParameter, parameters);
    }

    public static void setRelationshipNumberOfConnects(Session session, String graphTag, Relationship relationship,
                                                       Connection connection) {

        if (relationship.getDescription().equals("None")) {
            String numberOfConnects = getRelationshipParameter(session, graphTag, "None", connection,
                    "numberOfConnects").toString();

            String endVersionRelationship = getRelationshipParameter(session, graphTag, "None", connection,
                    "endVersion").toString();

            System.out.println(endVersionRelationship);

            if (numberOfConnects.equals("NULL") || !endVersionRelationship.equals("NULL")) {
                numberOfConnects = "0";
            } else {
                numberOfConnects = numberOfConnects.substring(1, numberOfConnects.length() - 1);
            }

            Integer newNumberOfConnects = Integer.parseInt(numberOfConnects) + 1;

            setRelationshipParameter(session, graphTag, "None", connection, "numberOfConnects",
                    newNumberOfConnects.toString());
        }
    }

    public static void setParametersForRelationship(Session session, String graphTag, Relationship relationship,
                                                    Connection connection, String curVersion) {

        if (graphTag.equals("Global") && getRelationshipParameter(session, graphTag, relationship.getDescription(),
                connection, "startVersion").toString().equals("NULL")) {

            setRelationshipParameter(session, graphTag, relationship.getDescription(), connection, "startVersion",
                    curVersion);
        }

        setRelationshipNumberOfConnects(session, graphTag, relationship, connection);

        String setParameters = "MATCH (a:" + connection.getSource().getType() + " {graphTag: $graphTag1, "
                + connection.getSource().getKey() + ": $val1})-[r:" + connection.getRelationshipType()
                + " {graphTag: $graphTag1, sourceWorkspace: $cmdb, description: $description1}]->(b:"
                + connection.getDestination().getType() + " {graphTag: $graphTag1, "
                + connection.getDestination().getKey() + ": $val2}) SET r.endVersion = $endVersion1, r.tags = $tags1,  "
                + "r.url = $url1, r.technology = $technology1, r.interactionStyle = $interactionStyle1, r.level = $level1";
        Value parameters = Values.parameters("graphTag1", graphTag, "val1",
                connection.getSource().getValue(), "cmdb", connection.getCmdb(), "description1",
                relationship.getDescription(), "val2", connection.getDestination().getValue(), "endVersion1", null,
                "tags1", relationship.getTags(), "url1", relationship.getUrl(), "technology1",
                relationship.getTechnology(), "interactionStyle1", relationship.getInteractionStyle(), "level1",
                connection.getLevel());
        session.run(setParameters, parameters);

        setRelationshipProperties(session, graphTag, relationship, connection);
    }

    public void updateChildRelationship(Session session, String graphTag, Model model, String curVersion,
                                        String sourceId, String destinationId, String cmdb, HashMap<String, GraphObject> objects) {

        Relationship relationship = new Relationship();
        relationship.setSourceId(sourceId);
        relationship.setDestinationId(destinationId);
        relationship.setDescription("Child");

        Connection connection = new Connection();
        connection.setRelationshipType("Child");
        connection.setCmdb(cmdb);

        updateRelationship(session, graphTag, relationship, model, curVersion, connection, objects);
    }

    public void updateDeployRelationship(Session session, String graphTag, Model model, String curVersion,
                                         String sourceId, String destinationId, String cmdb, HashMap<String, GraphObject> objects) {

        Relationship relationship = new Relationship();
        relationship.setSourceId(sourceId);
        relationship.setDestinationId(destinationId);
        relationship.setDescription("Deploy");

        Connection connection = new Connection();
        connection.setRelationshipType("Deploy");
        connection.setCmdb(cmdb);

        updateRelationship(session, graphTag, relationship, model, curVersion, connection, objects);
    }

    public void updateDefaultRelationship(Session session, String graphTag, Relationship relationship,
                                          Model model, String curVersion, String cmdb, String level, HashMap<String, GraphObject> objects) {

        Connection connection = new Connection();
        connection.setRelationshipType("Relationship");
        connection.setCmdb(cmdb);
        connection.setLevel(level);

        updateRelationship(session, graphTag, relationship, model, curVersion, connection, objects);
    }

    public static Boolean checkIfRelationshipExists(Session session, String graphTag, Relationship relationship,
                                                    Connection connection) {
        String query = "MATCH (a:" + connection.getSource().getType() + " {graphTag: $graphTag1, "
                + connection.getSource().getKey() + ": $value1})-[r:" + connection.getRelationshipType()
                + " {sourceWorkspace: $cmdb, description: $description1, graphTag: $graphTag1}]->(b:"
                + connection.getDestination().getType() + " {graphTag: $graphTag1, "
                + connection.getDestination().getKey() + ": $value2}) RETURN EXISTS((a)-->(b)) AS relationshipExists";
        Value parameters = Values.parameters("graphTag1", graphTag, "value1", connection.getSource().getValue(),
                "value2", connection.getDestination().getValue(), "cmdb", connection.getCmdb(),
                "description1", relationship.getDescription());
        Result result = session.run(query, parameters);

        if (result.hasNext()) {
            return true;
        }

        return false;
    }

    public void updateRelationship(Session session, String graphTag, Relationship relationship, Model model,
                                   String curVersion, Connection connection, HashMap<String, GraphObject> objects) {

        if (!objects.containsKey(relationship.getSourceId())) {
            createExternalObject(session, graphTag, model, curVersion, relationship.getSourceId(),
                    objects);
        }

        GraphObject source = objects.get(relationship.getSourceId());

        if (source == null) {
            return;
        }

        if (!objects.containsKey(relationship.getDestinationId())) {
            createExternalObject(session, graphTag, model, curVersion,
                    relationship.getDestinationId(), objects);
        }

        GraphObject destination = objects.get(relationship.getDestinationId());

        if (destination == null) {
            return;
        }

        connection.setSource(source);
        connection.setDestination(destination);

        if (relationship.getDescription() == null) {
            relationship.setDescription("None");
        }

        if (!checkIfRelationshipExists(session, graphTag, relationship, connection)) {
            createRelationship(session, graphTag, relationship, connection);
        }

        setParametersForRelationship(session, graphTag, relationship, connection, curVersion);
    }


}
