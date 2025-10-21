package ru.beeline.architecting_graph.repository.neo4j;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Repository;
import ru.beeline.architecting_graph.model.Component;
import ru.beeline.architecting_graph.model.GraphObject;

@Slf4j
@Repository
public class ComponentRepository {
    public Result getComponents(Session session, String containerDSLIdentifier) {
        String query = "MATCH (n:Container {graphTag: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m) "
                + "RETURN m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", containerDSLIdentifier);
        return session.run(query, parameters);
    }

    public Result getContainerComponents(Session session, String containerName) {
        String query = "MATCH (n:Container {graphTag: \"Global\", name: $val1})-[r:Child]->(m:Component) " +
                "RETURN n, m, m.name, m.startVersion, m.endVersion";
        Value parameters = Values.parameters("val1", containerName);
        return session.run(query, parameters);
    }

    public Result getComponentsByContainerName(Session session, String containerName) {
        String query = "MATCH (n:Container {graphTag: \"Global\", name: $val1})-[r:Child]->(m:Component) " +
                "RETURN n, m, m.name, m.startVersion, m.endVersion";
        Value parameters = Values.parameters("val1", containerName);
        return session.run(query, parameters);
    }

    public void setComponentProperty(Session session, String graphTag, String componentName, String sanitizedKey,
                                     Object propertyValue) {
        String cypherQuery = "MATCH (n:Component {graphTag: $graphTag, name: $name}) SET n." + sanitizedKey
                + " = $value";
        Value parameters = Values.parameters(
                "graphTag", graphTag,
                "name", componentName,
                "value", propertyValue);
        session.run(cypherQuery, parameters);
    }

    public void setMainComponentFields(Session session, String graphTag, GraphObject componentGraphObject,
                                       Component component) {
        String cypher = "MATCH (n:Component {graphTag: $graphTag, " + componentGraphObject.getKey()
                + ": $value}) "
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
                "endVersion", null);
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

    public Result findComponentNamesWithNullEndVersion(Session session, String graphTag, String containerName) {
        String cypher = "MATCH (n:Container {name: $name1, graphTag: $graphTag1})-[r:Child]->(m:Component) " +
                "WHERE m.endVersion IS NULL RETURN m.name AS componentName";
        Value parameters = Values.parameters("graphTag1", graphTag, "name1", containerName);
        return session.run(cypher, parameters);
    }

}
