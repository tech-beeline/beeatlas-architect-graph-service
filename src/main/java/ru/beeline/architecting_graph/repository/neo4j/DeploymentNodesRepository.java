package ru.beeline.architecting_graph.repository.neo4j;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Result;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.beeline.architecting_graph.model.DeploymentNode;
import ru.beeline.architecting_graph.service.graph.Neo4jSessionManager;

@Slf4j
@Repository
public class DeploymentNodesRepository {

    @Autowired
    private Neo4jSessionManager neo4jSessionManager;

    public Result getById(String id) {
        String query = "MATCH (n:DeploymentNode) WHERE n.id = id RETURN n";
        Value parameters = Values.parameters("id", id);
        return neo4jSessionManager.getSession().run(query, parameters);
    }

    public Result getDeploymentNodes(String systemDSLIdentifier) {
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + "-[r:Child]->(m:DeploymentNode) RETURN m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", systemDSLIdentifier);
        return neo4jSessionManager.getSession().run(query, parameters);
    }

    public Result findDeploymentNodesBySearch(String search) {
        String cypher = "MATCH (n:DeploymentNode) " +
                "WHERE toLower(n.graphTag) = toLower('Global')" +
                "AND (toLower(n.host) = toLower($search) OR toLower(n.ip) = toLower($search)) " +
                "RETURN n";
        Value parameters = Values.parameters("search", search);
        return neo4jSessionManager.getSession().run(cypher, parameters);
    }

    public Result getChildDeploymentNodes(String deploymentNodeDSLIdentifier) {
        String query = "MATCH (n:DeploymentNode {graphTag: \"Global\", structurizr_dsl_identifier: $val1})"
                + "-[r:Child]->(m:DeploymentNode) RETURN m, m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", deploymentNodeDSLIdentifier);
        return neo4jSessionManager.getSession().run(query, parameters);
    }

    public Result getDeploymentNodesByCmdb(String cmdb) {
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Child]->(m:DeploymentNode) " +
                "RETURN n, m, m.name, m.startVersion, m.endVersion";
        Value parameters = Values.parameters("val1", cmdb);
        return neo4jSessionManager.getSession().run(query, parameters);
    }
    public Result getDeploymentNodeNames(String graphTag, String cmdb) {
        String getDeploymentNode = "MATCH (n:SoftwareSystem {cmdb: $cmdb1, graphTag: $graphTag1})-[r:Child]->(m:DeploymentNode) "
                + "WHERE m.endVersion IS NULL RETURN m.name as deploymentNodeName";
        Value parameters = Values.parameters("graphTag1", graphTag, "cmdb1", cmdb);
        return neo4jSessionManager.getSession().run(getDeploymentNode, parameters);
    }

    public Result getParentDeploymentNodeId(Long id) {
        String query = "MATCH (parent:DeploymentNode)-[:Child]->(child:DeploymentNode) " +
                "WHERE id(child) = $val1 RETURN parent";
        Value parameters = Values.parameters("val1", id);
        return neo4jSessionManager.getSession().run(query, parameters);
    }

    public Result findChildDeploymentNodesWithNullEndVersion(String graphTag,
                                                             String deploymentNodeName) {
        String cypher = "MATCH (n:DeploymentNode {name: $name1, graphTag: $graphTag1})-[r:Child]->(m:DeploymentNode) "
                + "WHERE m.endVersion IS NULL RETURN m.name AS childDeploymentNodeName";
        Value parameters = Values.parameters("graphTag1", graphTag, "name1", deploymentNodeName);
        return neo4jSessionManager.getSession().run(cypher, parameters);
    }

    public void updateDeploymentNode(String graphTag, DeploymentNode deploymentNode) {
        String cypher = "MATCH (n:DeploymentNode {graphTag: $graphTag1, name: $name1}) "
                + "SET n.description = $description1, n.technology = $technology1, n.instances = $instances1, "
                + "n.tags = $tags1, n.url = $url1, n.endVersion = $endVersion1";
        Value parameters = Values.parameters(
                "graphTag1", graphTag, "name1", deploymentNode.getName(), "description1",
                deploymentNode.getDescription(),
                "technology1", deploymentNode.getTechnology(), "instances1",
                deploymentNode.getInstances(),
                "tags1", deploymentNode.getTags(), "url1", deploymentNode.getUrl(), "endVersion1",
                null);
        neo4jSessionManager.getSession().run(cypher, parameters);
    }

    public void setDeploymentNodeProperty(String graphTag, String name, String propertyKey,
                                          Object value) {
        String cypher = "MATCH (n:DeploymentNode {graphTag: $graphTag1, name: $name1}) SET n." + propertyKey
                + " = $value";
        Value parameters = Values.parameters("graphTag1", graphTag, "name1", name, "value", value);
        neo4jSessionManager.getSession().run(cypher, parameters);
    }

}
