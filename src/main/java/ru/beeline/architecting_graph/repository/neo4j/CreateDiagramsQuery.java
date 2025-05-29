package ru.beeline.architecting_graph.repository.neo4j;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class CreateDiagramsQuery {

    public Result checkIfContainerExists(Session session, String softwareSystemMnemonic, String containerMnemonic) {
        String query = "MATCH (a:SoftwareSystem {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + "-[r:Child]->(b:Container {graphTag: \"Global\", structurizr_dsl_identifier: $val2})  "
                + "WHERE r.graphTag = \"Global\" RETURN EXISTS((a)-->(b)) AS relationship_exists";
        Value parameters = Values.parameters("val1", softwareSystemMnemonic, "val2", containerMnemonic);
        return session.run(query, parameters);
    }

    public Result checkIfEnvironmentExists(Session session, String environment) {
        String query = "MATCH (n:Environment {name: $val1}) RETURN n";
        Value parameters = Values.parameters("val1", environment);
        return session.run(query, parameters);
    }

    public Result getSystem(Session session, String systemDSLIdentifier) {
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n";
        Value parameters = Values.parameters("val1", systemDSLIdentifier);
        return session.run(query, parameters);
    }

    public Result getParentSystem(Session session, String containerDSLIdentifier) {
        String query = "MATCH (m:SoftwareSystem)-[r:Child]->(n:Container "
                + "{graphTag: \"Global\", structurizr_dsl_identifier: $val1}) RETURN m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", containerDSLIdentifier);
        return session.run(query, parameters);
    }

    public Result getContainer(Session session, String systemDSLIdentifier, String containerDSLIdentifier) {
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + "-[r:Child]->(m:Container {graphTag: \"Global\", structurizr_dsl_identifier: $val2}) RETURN m";
        Value parameters = Values.parameters("val1", systemDSLIdentifier, "val2", containerDSLIdentifier);
        return session.run(query, parameters);
    }

    public Result getContainers(Session session, String systemDSLIdentifier) {
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + "-[r:Child]->(m:Container) RETURN m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", systemDSLIdentifier);
        return session.run(query, parameters);
    }

    public Result getParentContainer(Session session, String componentDSLIdentifier) {
        String query = "MATCH (m:Container)-[r:Child]->(n:Component "
                + "{graphTag: \"Global\", structurizr_dsl_identifier: $val1}) RETURN m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", componentDSLIdentifier);
        return session.run(query, parameters);
    }

    public Result getComponents(Session session, String containerDSLIdentifier) {
        String query = "MATCH (n:Container {graphTag: \"Global\", structurizr_dsl_identifier: $val1})-[r:Child]->(m) "
                + "RETURN m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", containerDSLIdentifier);
        return session.run(query, parameters);
    }

    public Result getDeploymentNodes(Session session, String systemDSLIdentifier) {
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + "-[r:Child]->(m:DeploymentNode) RETURN m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", systemDSLIdentifier);
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

    public Result getContainerInstanceContainerId(Session session, String containerInstanceDSLIdentifier) {
        String query = "MATCH (n:Container)-[r:Deploy]->(m:ContainerInstance "
                + "{graphTag: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", containerInstanceDSLIdentifier);
        return session.run(query, parameters);
    }

    public Result getDeploymentNodeEnvironment(Session session, String deploymentNodeDSLIdentifier) {
        String query = "MATCH (n:Environment)-[r:Child]->(m:DeploymentNode "
                + "{graphTag: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n.name";
        Value parameters = Values.parameters("val1", deploymentNodeDSLIdentifier);
        return session.run(query, parameters);
    }

    public Result getInfrastructureNodes(Session session, String deploymentNodeDSLIdentifier) {
        String query = "MATCH (n:DeploymentNode {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + "-[r:Child]->(m:InfrastructureNode) RETURN m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", deploymentNodeDSLIdentifier);
        return session.run(query, parameters);
    }

    public Result getContainerInstances(Session session, String deploymentNodeDSLIdentifier) {
        String query = "MATCH (n:DeploymentNode {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + "-[r:Child]->(m:ContainerInstance) RETURN m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", deploymentNodeDSLIdentifier);
        return session.run(query, parameters);
    }

    public Result getChildDeploymentNodes(Session session, String deploymentNodeDSLIdentifier) {
        String query = "MATCH (n:DeploymentNode {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + "-[r:Child]->(m:DeploymentNode) RETURN m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", deploymentNodeDSLIdentifier);
        return session.run(query, parameters);
    }

    public Result getDeploymentNodeRelationships(Session session, String deploymentNodeDSLIdentifier) {
        String query = "MATCH (n:DeploymentNode {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + "-[r:Relationship]->(m) RETURN r, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", deploymentNodeDSLIdentifier);
        return session.run(query, parameters);
    }

    public Result getInfrastructureNodeRelationships(Session session, String infrastructureNodeDSLIdentifier) {
        String query = "MATCH (n:InfrastructureNode {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + "-[r:Relationship]->(m) RETURN r, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", infrastructureNodeDSLIdentifier);
        return session.run(query, parameters);
    }

    public Result getContainerInstanceRelationships(Session session, String containerInstanceDSLIdentifier) {
        String query = "MATCH (n:ContainerInstance {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + "-[r:Relationship]->(m) RETURN r, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", containerInstanceDSLIdentifier);
        return session.run(query, parameters);
    }

    public Result getDirectComponentRelationships(Session session, String componentDSLIndentifier) {
        String query = "MATCH (n:Component {graphTag: \"Global\", structurizr_dsl_identifier: $val1})" +
                "-[r:Relationship]->(m) RETURN r, m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", componentDSLIndentifier);
        return session.run(query, parameters);
    }

    public Result getReverseComponentRelationships(Session session, String componentDSLIndentifier) {
        String query = "MATCH (m)-[r:Relationship]->(n:Component {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + "RETURN r, m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", componentDSLIndentifier);
        return session.run(query, parameters);
    }

    public Result getDirectContainerRelationships(Session session, String containerDSLIndentifier) {
        String query = "MATCH (n:Container {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + "-[r:Relationship]->(m) RETURN r, m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", containerDSLIndentifier);
        return session.run(query, parameters);
    }

    public Result getReverseContainerRelationships(Session session, String containerDSLIndentifier) {
        String query = "MATCH (m)-[r:Relationship]->(n:Container {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + " RETURN r, m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", containerDSLIndentifier);
        return session.run(query, parameters);
    }

    public Result getDirectSystemRelationships(Session session, String systemDSLIndentifier) {
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + "-[r:Relationship]->(m) RETURN r, m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", systemDSLIndentifier);
        return session.run(query, parameters);
    }

    public Result getReverseSystemRelationships(Session session, String systemtDSLIndentifier) {
        String query = "MATCH (m)-[r:Relationship]->(n:SoftwareSystem "
                + "{graphTag: \"Global\", structurizr_dsl_identifier: $val1}) RETURN r, m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", systemtDSLIndentifier);
        return session.run(query, parameters);
    }
}
