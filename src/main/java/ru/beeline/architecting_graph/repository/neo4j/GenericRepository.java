package ru.beeline.architecting_graph.repository.neo4j;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Result;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.service.graph.Neo4jSessionManager;

@Slf4j
@Repository
public class GenericRepository {
    @Autowired
    private Neo4jSessionManager neo4jSessionManager;

    public boolean checkIfObjectExists(String graphTag, GraphObject graphObject) {
        String checkObjectExist = "MATCH (n:" + graphObject.getType() + " {" + graphObject.getKey()
                + ": $value, graphTag: $graphTag1}) RETURN n";
        Value parameters = Values.parameters("value", graphObject.getValue(), "graphTag1", graphTag);
        Result result = neo4jSessionManager.getSession().run(checkObjectExist, parameters);
        return result.hasNext();
    }

    public void createObject(String graphTag, GraphObject graphObject) {
        String createObject = "CREATE (n:" + graphObject.getType() + " {graphTag: $graphTag1, "
                + graphObject.getKey() + ": $value})";
        Value parameters = Values.parameters("graphTag1", graphTag, "value", graphObject.getValue());
        neo4jSessionManager.getSession().run(createObject, parameters);
    }

    public Value getObjectParameter(String graphTag, GraphObject graphObject,
                                    String parameter) {
        String getParameter = "MATCH (n:" + graphObject.getType() + " {graphTag: $graphTag1, "
                + graphObject.getKey() + ": $value}) " + "RETURN n." + parameter + " AS parameter";
        Value parameters = Values.parameters("graphTag1", graphTag, "value", graphObject.getValue());
        Result result = neo4jSessionManager.getSession().run(getParameter, parameters);
        return result.next().get("parameter");
    }

    public void setObjectParameter(String graphTag, GraphObject graphObject,
                                   String parameter, String value) {
        String setParameter = "MATCH (n:" + graphObject.getType() + " {graphTag: $graphTag1, "
                + graphObject.getKey() + ": $value}) " + "SET n." + parameter + " = $parameter1";
        Value parameters = Values.parameters("graphTag1", graphTag, "value", graphObject.getValue(),
                "parameter1", value);
        neo4jSessionManager.getSession().run(setParameter, parameters);
    }

    public void deleteGraph(String graphTag) {
        String deleteLocalGraph = "MATCH (n) WHERE n.graphTag = $graphTag1 DETACH DELETE n";
        Value parameters = Values.parameters("graphTag1", graphTag);
        neo4jSessionManager.getSession().run(deleteLocalGraph, parameters);
    }

    public Result getContainerInstancesWithContainersAndSoftwareSystems(Long deploymentNodeId) {
        String cypher = "MATCH (dn:DeploymentNode {graphTag: 'Global'})-[:Child]->(ci:ContainerInstance) where id(dn)=$dnId " +
                "OPTIONAL MATCH (c:Container)-[:Deploy]->(ci) " +
                "OPTIONAL MATCH (ss:SoftwareSystem)-[:Child]->(c) " +
                "RETURN ci, c, ss";
        return neo4jSessionManager.getSession().run(cypher, Values.parameters("dnId", deploymentNodeId));
    }

    public Result findIncomingDeploymentNodeRelationships(Long deploymentNodeId) {
        String cypher = "MATCH (src:DeploymentNode)-[r:Relationship]->(dst:DeploymentNode) "
                + "WHERE id(dst) = $deploymentNodeId "
                + "RETURN r, src, dst";

        Value params = Values.parameters("deploymentNodeId", deploymentNodeId);
        return neo4jSessionManager.getSession().run(cypher, params);
    }

    public Result findIncomingRelationshipsByContainerInstance(Long containerInstanceId) {
        String cypher = """
        MATCH (src:ContainerInstance)-[r:Relationship]->(dst:ContainerInstance)
        WHERE id(dst) = $containerInstanceId
        MATCH (container:Container)-[:Deploy]->(dst)
        MATCH (ss:SoftwareSystem)-[:Child]->(container)
        MATCH (dn:DeploymentNode)-[:Child]->(dst)
        RETURN
            r, src, dst, container, ss, dn
    """;

        Value params = Values.parameters("containerInstanceId", containerInstanceId);
        return neo4jSessionManager.getSession().run(cypher, params);
    }

    public Result findIncomingDeploymentNodeRelationshipsFromInfrastructureNode(Long deploymentNodeId) {
        String cypher = "MATCH (src:InfrastructureNode)-[r:Relationship]->(dst:DeploymentNode) "
                + "WHERE id(dst) = $deploymentNodeId "
                + "RETURN r, src, dst";

        Value params = Values.parameters("deploymentNodeId", deploymentNodeId);
        return neo4jSessionManager.getSession().run(cypher, params);
    }

    public Result findDeploymentNodesByInfrastructureId(Long infrastructureNodeId) {
        String cypher = "MATCH (dn:DeploymentNode)-[:Child]->(in:InfrastructureNode) "
                + "WHERE id(in) = $infrastructureNodeId "
                + "RETURN dn";

        Value params = Values.parameters("infrastructureNodeId", infrastructureNodeId);
        return neo4jSessionManager.getSession().run(cypher, params);
    }

    public Result findRelationshipsFromDeploymentNode(Long $infrastructureNodeId) {
        String cypher = "MATCH (src:DeploymentNode)-[r:Relationship]->(dst:InfrastructureNode) "
                + "WHERE id(dst) = $infrastructureNodeId "
                + "RETURN r, src, dst";

        Value params = Values.parameters("$infrastructureNodeId", $infrastructureNodeId);
        return neo4jSessionManager.getSession().run(cypher, params);
    }

    public Result findIncomingDeploymentNodeRelationshipsFromIncomingDeploymentNode(Long infrastructureNodeId) {
        String cypher = "MATCH (src:InfrastructureNode)-[r:Relationship]->(dst:InfrastructureNode) "
                + "WHERE id(dst) = $infrastructureNodeId "
                + "RETURN r, src, dst";

        Value params = Values.parameters("infrastructureNodeId", infrastructureNodeId);
        return neo4jSessionManager.getSession().run(cypher, params);
    }

    public Result getNodeTypeAndNameById(Long nodeId) {
        String cypher = "MATCH (n) WHERE id(n) = $nodeId RETURN head(labels(n)) AS nodeType, n.name AS name";
        Value params = Values.parameters("nodeId", nodeId);
            return neo4jSessionManager.getSession().run(cypher, params);
    }

    public Result getDeploymentNodeDependencies(Long nodeId) {
        String cypher = """
            MATCH (start) WHERE id(start) = $nodeId
            OPTIONAL MATCH (deploymentSource:DeploymentNode)-[:Relationship]->(start)
            OPTIONAL MATCH (infrastructureSource:InfrastructureNode)-[:Relationship]->(start)
            OPTIONAL MATCH (start)-[:Child]->(deploymentTarget:DeploymentNode)
            OPTIONAL MATCH (start)-[:Child]->(infrastructure:InfrastructureNode)
            OPTIONAL MATCH (start)-[:Child]->(containerInstance:ContainerInstance)
            OPTIONAL MATCH (container:Container)-[:Deploy]->(containerInstance)
            RETURN 
                collect(DISTINCT deploymentSource) AS deploymentSources,
                collect(DISTINCT infrastructureSource) AS infrastructureSources,
                collect(DISTINCT deploymentTarget) AS deploymentTargets,
                collect(DISTINCT infrastructure) AS infrastructureNodes,
                collect(DISTINCT containerInstance) AS containerInstances,
                collect(DISTINCT container) AS containers
        """;
        Value params = Values.parameters("nodeId", nodeId);
        return neo4jSessionManager.getSession().run(cypher, params);
    }

    public Result getContainerDependencies(Long nodeId) {
        String cypher = """
            MATCH (start) WHERE id(start) = $nodeId
            MATCH (directContainer:Container)-[:Deploy]->(:ContainerInstance)
            WHERE (directContainer)-[:Relationship]->(start)
            RETURN collect(DISTINCT directContainer) AS containersSources
        """;
        Value params = Values.parameters("nodeId", nodeId);
        return neo4jSessionManager.getSession().run(cypher, params);
    }

    public Result getInfrastructureNodeDependencies(Long nodeId) {
        String cypher = """
            MATCH (start) WHERE id(start) = $nodeId
            OPTIONAL MATCH (infrastructureSource:InfrastructureNode)-[:Relationship]->(start)
            OPTIONAL MATCH (deploymentSource:DeploymentNode)-[:Relationship]->(start)
            RETURN 
                collect(DISTINCT infrastructureSource) AS infrastructureSources,
                collect(DISTINCT deploymentSource) AS deploymentSources
        """;
        Value params = Values.parameters("nodeId", nodeId);
        return neo4jSessionManager.getSession().run(cypher, params);
    }

}