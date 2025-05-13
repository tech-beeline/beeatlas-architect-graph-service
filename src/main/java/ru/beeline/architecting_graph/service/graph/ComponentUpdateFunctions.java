package ru.beeline.architecting_graph.service.graph;

import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.model.*;

import java.util.HashMap;
import java.util.Map;
@Service
public class ComponentUpdateFunctions {

    public  void setComponentProperties(Session session, String graphTag, Component component) {
        if (component.getProperties() != null) {
            for (Map.Entry<String, Object> entry : component.getProperties().entrySet()) {
                String key = entry.getKey();
                key = key.replaceAll("[^a-zA-Z0-9]", "_");
                String setProperties = "MATCH (n:Component {graphTag: $graphTag1, name: $name1}) SET n." + key
                        + " = $value";
                Value parameters = Values.parameters("graphTag1", graphTag, "name1", component.getName(), "value",
                        entry.getValue());
                session.run(setProperties, parameters);
            }
        }
    }

    public  void setParametersForComponent(Session session, String graphTag, Component component,
                                                 GraphObject componentGraphObject, String curVersion) {

        if (graphTag.equals("Global")
                && CommonFunctions.getObjectParameter(session, graphTag, componentGraphObject, "startVersion")
                .toString().equals("NULL")) {

            CommonFunctions.setObjectParameter(session, graphTag, componentGraphObject, "startVersion", curVersion);
        }

        String setParameters = "MATCH (n:Component {graphTag: $graphTag1, " + componentGraphObject.getKey()
                + ": $value}) "
                + "SET n.description = $description1, n.technology = $technology1, n.tags = $tags1, "
                + "n.url = $url1, n.group = $group1, n.endVersion = $endVersion1";
        Value parameters = Values.parameters("graphTag1", graphTag, "value", componentGraphObject.getValue(),
                "description1", component.getDescription(), "technology1", component.getTechnology(), "tags1",
                component.getTags(), "url1", component.getUrl(), "group1", component.getGroup(), "endVersion1", null);
        session.run(setParameters, parameters);

        setComponentProperties(session, graphTag, component);
    }

    public  Integer getComponentNumberOfConnects(Session session, String graphTag, String componentExternalName) {
        String findConnects = "MATCH (n:Component {graphTag: $graphTag1, external_name: $external_name1})-[r]-() "
                + "RETURN count(r) AS numberOfRelationships";
        Value parameters = Values.parameters("graphTag1", graphTag, "external_name1", componentExternalName);
        Result result = session.run(findConnects, parameters);

        String numberOfRelationships = result.next().get("numberOfRelationships").toString();

        if (numberOfRelationships.equals("NULL")) {
            return 0;
        }

        return Integer.parseInt(numberOfRelationships);
    }

    public  Boolean needComponentReplace(Session session, String graphTag, Component component,
                                               GraphObject externalComponentGraphObject) {

        Integer numberOfRelationshipsFirst = getComponentNumberOfConnects(session, graphTag,
                externalComponentGraphObject.getValue());
        String source = CommonFunctions.getObjectParameter(session, graphTag, externalComponentGraphObject, "source")
                .toString();

        Integer numberOfRelationshipsSecond = 0;

        if (component.getRelationships() != null) {
            numberOfRelationshipsSecond = component.getRelationships().size();
        }

        if (numberOfRelationshipsFirst >= numberOfRelationshipsSecond || source.equals("\"landscape\"")) {
            return false;
        }

        return true;
    }

    public  Boolean changeComponent(Session session, String graphTag, Component component,
                                          GraphObject externalComponentGraphObject, String curVersion, HashMap<String, GraphObject> objects) {

        String startVersion = CommonFunctions
                .getObjectParameter(session, graphTag, externalComponentGraphObject, "startVersion").toString();

        if (startVersion.equals("NULL")) {
            CommonFunctions.setObjectParameter(session, graphTag, externalComponentGraphObject, "name",
                    component.getName());
            return true;
        }

        String endVersion = CommonFunctions
                .getObjectParameter(session, graphTag, externalComponentGraphObject, "endVersion").toString();

        if (endVersion.equals("NULL")) {
            return needComponentReplace(session, graphTag, component, externalComponentGraphObject);
        }

        return true;
    }

    public  void updateComponent(Session session, String graphTag, Component component, String curVersion,
                                       HashMap<String, GraphObject> objects) {

        boolean externalExists = false;
        GraphObject externalComponentGraphObject = null;

        if (component.getProperties() != null && component.getProperties().containsKey("external_name")
                && component.getProperties().get("external_name") != null) {

            String componentExternalName = component.getProperties().get("external_name").toString();
            externalComponentGraphObject = new GraphObject("Component", "external_name",
                    componentExternalName);
            externalExists = CommonFunctions.checkIfObjectExists(session, graphTag, externalComponentGraphObject);
        }

        if (externalExists) {
            if (changeComponent(session, graphTag, component, externalComponentGraphObject, curVersion, objects)) {
                CommonFunctions.setObjectParameter(session, graphTag, externalComponentGraphObject, "external_name",
                        null);
            } else {
                component.getProperties().put("external_name", null);
            }
        }

        GraphObject componentGraphObject = new GraphObject("Component", "name", component.getName());
        boolean exists = CommonFunctions.checkIfObjectExists(session, graphTag, componentGraphObject);

        if (!exists) {
            CommonFunctions.createObject(session, graphTag, componentGraphObject);
        }

        objects.put(component.getId(), componentGraphObject);
        setParametersForComponent(session, graphTag, component, componentGraphObject, curVersion);
    }

    public  void updateComponents(Session session, String graphTag, Model model, Container container, String cmdb,
                                        String curVersion, String containerExternalName, HashMap<String, GraphObject> objects) {

        if (container.getComponents() != null) {
            for (Component component : container.getComponents()) {

                component.setName(component.getName() + "." + container.getName());
                if (component.getProperties() != null && component.getProperties().containsKey("external_name")
                        && component.getProperties().get("external_name") != null) {

                    String componentExternalName = component.getProperties().get("external_name").toString() + "."
                            + containerExternalName;
                    component.getProperties().put("external_name", componentExternalName);
                }

                updateComponent(session, graphTag, component, curVersion, objects);

                RelationshipUpdateFunctions.updateChildRelationship(session, graphTag, model, curVersion,
                        container.getId(), component.getId(), cmdb, objects);
            }
        }
    }
    public  void updateComponentRelationships(Session session, String graphTag, Model model, Container container,
                                                    String cmdb, String curVersion, HashMap<String, GraphObject> objects) {

        if (container.getComponents() != null) {
            for (Component component : container.getComponents()) {

                if (component.getRelationships() != null) {
                    for (Relationship relationship : component.getRelationships()) {
                        if (relationship.getLinkedRelationshipId() == null) {
                            RelationshipUpdateFunctions.updateDefaultRelationship(session, graphTag, relationship,
                                    model, curVersion, cmdb, "C3", objects);
                        }
                    }
                }
            }
        }
    }

    public  void setComponentEndVersion(Session session, String graphTag, String containerName, String cmdb,
                                              String curVersion) {

        String getComponents = "MATCH (n:Container {name: $name1, graphTag: $graphTag1})-[r:Child]->(m:Component) " +
                "WHERE m.endVersion IS NULL RETURN m.name AS componentName";
        Value parameters = Values.parameters("graphTag1", graphTag, "name1", containerName);
        Result result = session.run(getComponents, parameters);

        while (result.hasNext()) {
            String componentName = result.next().get("componentName").toString();
            componentName = componentName.substring(1, componentName.length() - 1);
            GraphObject componentGraphObject = new GraphObject("Component", "name", componentName);

            CommonFunctions.setObjectParameter(session, graphTag, componentGraphObject, "endVersion", curVersion);
        }
    }
}
