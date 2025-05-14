package ru.beeline.architecting_graph.repository.neo4j;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import ru.beeline.architecting_graph.model.Component;
import org.springframework.stereotype.Repository;
import ru.beeline.architecting_graph.model.GraphObject;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class BuildGraphQuery {

    public boolean checkIfObjectExists(Session session, String graphTag, GraphObject graphObject) {
        String checkObjectExist = "MATCH (n:" + graphObject.getType() + " {" + graphObject.getKey()
                + ": $value, graphTag: $graphTag1}) RETURN n";
        Value parameters = Values.parameters("value", graphObject.getValue(), "graphTag1", graphTag);
        Result result = session.run(checkObjectExist, parameters);
        return result.hasNext();
    }

    public void createObject(Session session, String graphTag, GraphObject graphObject) {
        String createObject = "CREATE (n:" + graphObject.getType() + " {graphTag: $graphTag1, "
                + graphObject.getKey() + ": $value})";
        Value parameters = Values.parameters("graphTag1", graphTag, "value", graphObject.getValue());
        session.run(createObject, parameters);
    }

    public Value getObjectParameter(Session session, String graphTag, GraphObject graphObject,
                                    String parameter) {
        String getParameter = "MATCH (n:" + graphObject.getType() + " {graphTag: $graphTag1, "
                + graphObject.getKey() + ": $value}) " + "RETURN n." + parameter + " AS parameter";
        log.info(getParameter);
        Value parameters = Values.parameters("graphTag1", graphTag, "value", graphObject.getValue());
        Result result = session.run(getParameter, parameters);
        return result.next().get("parameter");
    }

    public void setObjectParameter(Session session, String graphTag, GraphObject graphObject,
                                   String parameter, String value) {
        String setParameter = "MATCH (n:" + graphObject.getType() + " {graphTag: $graphTag1, "
                + graphObject.getKey() + ": $value}) " + "SET n." + parameter + " = $parameter1";
        Value parameters = Values.parameters("graphTag1", graphTag, "value", graphObject.getValue(),
                "parameter1", value);
        session.run(setParameter, parameters);
    }

    public void setComponentProperty(Session session, String graphTag, String componentName, String sanitizedKey, Object propertyValue) {
        String cypherQuery = "MATCH (n:Component {graphTag: $graphTag, name: $name}) SET n." + sanitizedKey + " = $value";
        Value parameters = Values.parameters(
                "graphTag", graphTag,
                "name", componentName,
                "value", propertyValue
        );
        session.run(cypherQuery, parameters);
    }

    public void setMainComponentFields(Session session, String graphTag, GraphObject componentGraphObject, Component component) {
        String cypher = "MATCH (n:Component {graphTag: $graphTag, " + componentGraphObject.getKey() + ": $value}) "
                + "SET n.description = $description, n.technology = $technology, n.tags = $tags, "
                + "n.url = $url, n.group = $group, n.endVersion = $endVersion";
        Value params = Values.parameters(
                "graphTag", graphTag,
                "value", componentGraphObject.getValue(),
                "description", component.getDescription(),
                "technology", component.getTechnology(),
                "tags", component.getTags(),
                "url", component.getUrl(),
                "group", component.getGroup(),
                "endVersion", null
        );
        session.run(cypher, params);
    }

    public Integer fetchNumberOfConnections(Session session, String graphTag, String externalName) {
        String cypher = "MATCH (n:Component {graphTag: $graphTag1, external_name: $external_name1})-[r]-() "
                + "RETURN count(r) AS numberOfRelationships";
        Value parameters = Values.parameters("graphTag1", graphTag, "external_name1", externalName);
        Result result = session.run(cypher, parameters);
        String numberOfRelationships = result.next().get("numberOfRelationships").toString();
        if ("NULL".equals(numberOfRelationships)) {
            return 0;
        }
        return Integer.parseInt(numberOfRelationships);
    }

    public List<String> findComponentNamesWithNullEndVersion(Session session, String graphTag, String containerName) {
        String cypher = "MATCH (n:Container {name: $name1, graphTag: $graphTag1})-[r:Child]->(m:Component) " +
                "WHERE m.endVersion IS NULL RETURN m.name AS componentName";
        Value parameters = Values.parameters("graphTag1", graphTag, "name1", containerName);
        Result result = session.run(cypher, parameters);
        List<String> componentNames = new ArrayList<>();
        while (result.hasNext()) {
            String rawName = result.next().get("componentName").toString();
            String cleanedName = rawName.substring(1, rawName.length() - 1); // удаление кавычек
            componentNames.add(cleanedName);
        }
        return componentNames;
    }
}
