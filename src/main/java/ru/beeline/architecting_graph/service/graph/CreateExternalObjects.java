package ru.beeline.architecting_graph.service.graph;

import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.model.*;
import ru.beeline.architecting_graph.repository.neo4j.GenericRepository;
import ru.beeline.architecting_graph.repository.neo4j.RelationshipRepository;

import java.util.HashMap;
import java.util.Map;

@Service
public class CreateExternalObjects {

    @Autowired
    GenericRepository genericRepository;
    @Autowired
    private RelationshipRepository relationshipRepository;

    public void createExternalSystem(Session session, String graphTag, SoftwareSystem softwareSystem,
                                     String cmdb, HashMap<String, GraphObject> objects) {
        GraphObject systemGraphObject = new GraphObject("SoftwareSystem", "cmdb", cmdb);
        boolean exists = genericRepository.checkIfObjectExists(session, graphTag, systemGraphObject);
        if (!exists) {
            genericRepository.createObject(session, graphTag, systemGraphObject);
            genericRepository.setObjectParameter(session, graphTag, systemGraphObject, "name", softwareSystem.getName());
            if (softwareSystem.getProperties() != null
                    && softwareSystem.getProperties().containsKey("structurizr.dsl.identifier")) {
                genericRepository.setObjectParameter(session, graphTag, systemGraphObject, "structurizr_dsl_identifier",
                                                     softwareSystem.getProperties().get("structurizr.dsl.identifier").toString());
            }
        }
        objects.put(softwareSystem.getId(), systemGraphObject);
    }

    public void createExternalContainer(Session session, String graphTag, Container container,
                                        String containerExternalName, HashMap<String, GraphObject> objects) {
        GraphObject containerGraphObject = new GraphObject("Container", "external_name",
                containerExternalName);
        Boolean exists = genericRepository.checkIfObjectExists(session, graphTag, containerGraphObject);
        if (!exists) {
            genericRepository.createObject(session, graphTag, containerGraphObject);
            genericRepository.setObjectParameter(session, graphTag, containerGraphObject, "name", container.getName());
            if (container.getProperties() != null
                    && container.getProperties().containsKey("structurizr.dsl.identifier")) {
                genericRepository.setObjectParameter(session, graphTag, containerGraphObject,
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
        Boolean exists = genericRepository.checkIfObjectExists(session, graphTag, componentGraphObject);
        if (!exists) {
            genericRepository.createObject(session, graphTag, componentGraphObject);
            genericRepository.setObjectParameter(session, graphTag, componentGraphObject, "name", component.getName());

            if (component.getProperties() != null
                    && component.getProperties().containsKey("structurizr.dsl.identifier")) {
                genericRepository.setObjectParameter(session, graphTag, componentGraphObject,
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

    public void createRelationship(Session session, String graphTag, RelationshipEntity relationship, Connection connection) {
        relationshipRepository.createRelationshipQuery(session, graphTag, relationship, connection);
    }

    public void setRelationshipProperties(Session session, String graphTag, RelationshipEntity relationship, Connection connection) {
        if (relationship.getProperties() != null) {
            for (Map.Entry<String, Object> entry : relationship.getProperties().entrySet()) {
                String sanitizedKey = entry.getKey().replaceAll("[^a-zA-Z0-9]", "_");
                relationshipRepository.setRelationshipProperty(session, graphTag, sanitizedKey, entry.getValue(), relationship, connection);
            }
        }
    }

    public void setRelationshipNumberOfConnects(Session session, String graphTag, RelationshipEntity relationship,
                                                Connection connection) {
        if (relationship.getDescription().equals("None")) {
            String numberOfConnects = relationshipRepository.getRelationshipParameter(session, graphTag, "None", connection,
                                                                                      "numberOfConnects").toString();
            String endVersionRelationship = relationshipRepository.getRelationshipParameter(session, graphTag, "None", connection,
                                                                                            "endVersion").toString();
            if (numberOfConnects.equals("NULL") || !endVersionRelationship.equals("NULL")) {
                numberOfConnects = "0";
            } else {
                numberOfConnects = numberOfConnects.substring(1, numberOfConnects.length() - 1);
            }
            Integer newNumberOfConnects = Integer.parseInt(numberOfConnects) + 1;
            relationshipRepository.setRelationshipParameter(session, graphTag, "None", connection, "numberOfConnects",
                                                            newNumberOfConnects.toString());
        }
    }

    public void setParametersForRelationship(Session session, String graphTag, RelationshipEntity relationship,
                                             Connection connection, String curVersion) {
        if (graphTag.equals("Global") && relationshipRepository.getRelationshipParameter(session, graphTag, relationship.getDescription(),
                                                                                         connection, "startVersion").toString().equals("NULL")) {
            relationshipRepository.setRelationshipParameter(session, graphTag, relationship.getDescription(), connection, "startVersion",
                                                            curVersion);
        }
        setRelationshipNumberOfConnects(session, graphTag, relationship, connection);
        relationshipRepository.buildSetRelationshipParameters(session, graphTag, relationship, connection);
        setRelationshipProperties(session, graphTag, relationship, connection);
    }

    public void updateChildRelationship(Session session, String graphTag, Model model, String curVersion,
                                        String sourceId, String destinationId, String cmdb, HashMap<String, GraphObject> objects) {
        RelationshipEntity relationship = new RelationshipEntity();
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

        RelationshipEntity relationship = new RelationshipEntity();
        relationship.setSourceId(sourceId);
        relationship.setDestinationId(destinationId);
        relationship.setDescription("Deploy");
        Connection connection = new Connection();
        connection.setRelationshipType("Deploy");
        connection.setCmdb(cmdb);
        updateRelationship(session, graphTag, relationship, model, curVersion, connection, objects);
    }

    public void updateDefaultRelationship(Session session, String graphTag, RelationshipEntity relationship, Model model,
                                          String curVersion, String cmdb, String level, HashMap<String, GraphObject> objects) {
        Connection connection = new Connection();
        connection.setRelationshipType("Relationship");
        connection.setCmdb(cmdb);
        connection.setLevel(level);
        updateRelationship(session, graphTag, relationship, model, curVersion, connection, objects);
    }

    public void updateRelationship(Session session, String graphTag, RelationshipEntity relationship, Model model,
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
        if (!relationshipRepository.checkIfRelationshipExists(session, graphTag, relationship, connection)) {
            createRelationship(session, graphTag, relationship, connection);
        }
        setParametersForRelationship(session, graphTag, relationship, connection, curVersion);
    }
}
