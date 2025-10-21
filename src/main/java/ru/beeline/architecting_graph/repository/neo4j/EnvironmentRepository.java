package ru.beeline.architecting_graph.repository.neo4j;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class EnvironmentRepository {
    public Result checkIfEnvironmentExists(Session session, String environment) {
        String query = "MATCH (n:Environment {name: $val1}) RETURN n";
        Value parameters = Values.parameters("val1", environment);
        return session.run(query, parameters);
    }

    public Result getInfrastructureNodeEnvironment(Session session, String infrastructureNodeDSLIdentifier) {
        String query = "MATCH (n:Environment)-[r:Child]->(m:InfrastructureNode "
                + "{graphTag: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n.name";
        Value parameters = Values.parameters("val1", infrastructureNodeDSLIdentifier);
        return session.run(query, parameters);
    }

    public Result getContainerInstanceEnvironment(Session session, String containerInstanceDSLIdentifier) {
        String query = "MATCH (n:Environment)-[r:Child]->(m:ContainerInstance "
                + "{graphTag: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n.name";
        Value parameters = Values.parameters("val1", containerInstanceDSLIdentifier);
        return session.run(query, parameters);
    }

    public Result getDeploymentNodeEnvironment(Session session, String deploymentNodeDSLIdentifier) {
        String query = "MATCH (n:Environment)-[r:Child]->(m:DeploymentNode "
                + "{graphTag: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n.name";
        Value parameters = Values.parameters("val1", deploymentNodeDSLIdentifier);
        return session.run(query, parameters);
    }
}
