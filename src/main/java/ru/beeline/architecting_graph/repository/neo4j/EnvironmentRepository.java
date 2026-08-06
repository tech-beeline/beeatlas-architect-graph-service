/*
 * Copyright (c) 2024 PJSC VimpelCom
 */

package ru.beeline.architecting_graph.repository.neo4j;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Result;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.beeline.architecting_graph.service.graph.Neo4jSessionManager;

@Slf4j
@Repository
public class EnvironmentRepository {
    @Autowired
    private Neo4jSessionManager neo4jSessionManager;

    public Result checkIfEnvironmentExists(String environment) {
        String query = "MATCH (n:Environment {name: $val1}) RETURN n";
        Value parameters = Values.parameters("val1", environment);
        return neo4jSessionManager.getSession().run(query, parameters);
    }

    public Result getInfrastructureNodeEnvironment(String infrastructureNodeDSLIdentifier) {
        String query = "MATCH (n:Environment)-[r:Child]->(m:InfrastructureNode "
                + "{graphTag: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n.name";
        Value parameters = Values.parameters("val1", infrastructureNodeDSLIdentifier);
        return neo4jSessionManager.getSession().run(query, parameters);
    }

    public Result getContainerInstanceEnvironment(String containerInstanceDSLIdentifier) {
        String query = "MATCH (n:Environment)-[r:Child]->(m:ContainerInstance "
                + "{graphTag: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n.name";
        Value parameters = Values.parameters("val1", containerInstanceDSLIdentifier);
        return neo4jSessionManager.getSession().run(query, parameters);
    }

    public Result getDeploymentNodeEnvironment(String deploymentNodeDSLIdentifier) {
        String query = "MATCH (n:Environment)-[r:Child]->(m:DeploymentNode "
                + "{graphTag: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n.name";
        Value parameters = Values.parameters("val1", deploymentNodeDSLIdentifier);
        return neo4jSessionManager.getSession().run(query, parameters);
    }

    public Result getDeploymentNodeEnvironmentNameByIdChild(Long id) {
        String query = "MATCH (parent:Environment)-[:Child]->(child:DeploymentNode) "
                + "WHERE id(child) = $val1 RETURN parent.name";
        Value parameters = Values.parameters("val1", id);
        return neo4jSessionManager.getSession().run(query, parameters);
    }

    public Result getActiveParentEnvironmentNameByChild(String graphTag, String cmdb, String childType, String childName) {
        String query = "MATCH (env:Environment {graphTag: $graphTag1})"
                + "-[r:Child {graphTag: $graphTag1, sourceWorkspace: $cmdb, description: \"Child\"}]->"
                + "(child:" + childType + " {graphTag: $graphTag1, name: $childName}) "
                + "WHERE r.endVersion IS NULL "
                + "RETURN env.name AS name "
                + "LIMIT 1";
        Value parameters = Values.parameters(
                "graphTag1", graphTag,
                "cmdb", cmdb,
                "childName", childName
        );
        return neo4jSessionManager.getSession().run(query, parameters);
    }

    public void setChildRelationshipEndVersion(String graphTag, String cmdb, String childType, String childName, String endVersion)
    { String query = "MATCH (env:Environment {graphTag: $graphTag})" +
        "-[r:Child {graphTag: $graphTag, sourceWorkspace: $cmdb, description: \"Child\"}]->" +
            "(child:" + childType + " {graphTag: $graphTag, name: $childName}) " + "WHERE r.endVersion IS NULL " +
            "SET r.endVersion = $endVersion";
        Value parameters =
        Values.parameters( "graphTag", graphTag, "cmdb", cmdb, "childName", childName, "endVersion", endVersion);
        neo4jSessionManager.getSession().run(query, parameters);
    }
}
