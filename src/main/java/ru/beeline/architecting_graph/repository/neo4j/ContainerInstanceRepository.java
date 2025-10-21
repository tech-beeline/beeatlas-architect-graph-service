package ru.beeline.architecting_graph.repository.neo4j;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class ContainerInstanceRepository {

    public Result getContainerInstances(Session session, String deploymentNodeDSLIdentifier) {
        String query = "MATCH (n:DeploymentNode {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + "-[r:Child]->(m:ContainerInstance) RETURN m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", deploymentNodeDSLIdentifier);
        return session.run(query, parameters);
    }

    public void updateContainerInstance(Session session, String graphTag, String name,
                                        Integer instanceId, String tags) {
        String query = "MATCH (n:ContainerInstance {graphTag: $graphTag1, name: $val1}) SET "
                + "n.instanceId = $instanceId1, n.tags = $tags1, n.endVersion = $endVersion1";
        Value parameters = Values.parameters("graphTag1", graphTag, "val1", name, "instanceId1", instanceId,
                                             "tags1", tags, "endVersion1", null);
        session.run(query, parameters);
    }

    public Result findContainerInstanceNamesWithNullEndVersion(Session session, String graphTag,
                                                               String deploymentNodeName) {
        String cypher = "MATCH (n:DeploymentNode {name: $name1, graphTag: $graphTag1})-[r:Child]->(m:ContainerInstance) "
                +
                "WHERE m.endVersion IS NULL RETURN m.name AS containerInstanceName";
        Value parameters = Values.parameters("graphTag1", graphTag, "name1", deploymentNodeName);
        return session.run(cypher, parameters);
    }

    public void setProperty(Session session, String graphTag, String containerInstanceName, String key,
                            Object value) {
        String query = "MATCH (n:ContainerInstance {graphTag: $graphTag1, name: $val1}) SET n." + key
                + " = $value";
        Value parameters = Values.parameters("graphTag1", graphTag, "val1", containerInstanceName, "value",
                                             value);
        session.run(query, parameters);
    }


}
