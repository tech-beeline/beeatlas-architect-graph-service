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
        String cypher = "MATCH (dn:DeploymentNode {id: $dnId, graphTag: 'Global'})-[:Child]->(ci:ContainerInstance) " +
                "OPTIONAL MATCH (ci)-[:Deploy]->(c:Container) " +
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

    public Result findIncomingContainerRelationshipsExtendedContainer(Long containerId, String cmdb) {
        String cypher = """
                MATCH (src:Container)-[r:Relationship]->(dst:Container)
                WHERE id(dst) = $containerId
                  AND (src)-[:Deploy]->(:ContainerInstance)
                  AND EXISTS {
                        MATCH (ssCheck:SoftwareSystem)-[:Child]->(src)
                        WHERE ssCheck.cmdb <> $cmdb
                  }
                OPTIONAL MATCH (ss:SoftwareSystem)-[:Child]->(src)
                OPTIONAL MATCH (src)-[:Deploy]->(ci:ContainerInstance)<-[:Child]-(dn:DeploymentNode)
                RETURN 
                    r, src, dst, ci,
                    ss AS parentSoftwareSystem,
                    dn AS parentDeploymentNode
                    
                """;

        Value params = Values.parameters("containerId", containerId, "cmdb", cmdb);
        return neo4jSessionManager.getSession().run(cypher, params);
    }

    public Result findIncomingContainerRelationshipsExtendedComponent(Long containerId, String cmdb) {
        String cypher = """
                    MATCH (srcComponent:Component)-[r:Relationship]->(dst:Container)
                    WHERE id(dst) = $containerId
                      AND EXISTS {
                          MATCH (src:Container)-[:Child]->(srcComponent)
                          WHERE 
                              (src)-[:Deploy]->(:ContainerInstance)
                              AND EXISTS {
                                  MATCH (ssCheck:SoftwareSystem)-[:Child]->(src)
                                  WHERE ssCheck.cmdb <> $cmdb
                              }
                      }
                    OPTIONAL MATCH (src:Container)-[:Child]->(srcComponent)
                    OPTIONAL MATCH (ss:SoftwareSystem)-[:Child]->(src)
                    OPTIONAL MATCH (src)-[:Deploy]->(ci:ContainerInstance)<-[:Child]-(dn:DeploymentNode)
                    RETURN
                        r, src, dst, ci, srcComponent,
                        ss AS parentSoftwareSystem,
                        dn AS parentDeploymentNode
                """;

        Value params = Values.parameters("containerId", containerId, "cmdb", cmdb);
        return neo4jSessionManager.getSession().run(cypher, params);
    }

    public Result findIncomingContainerRelationshipsExtendedChildComponent(Long containerId, String cmdb) {
        String cypher = """
                    MATCH (dst:Container)-[:Child]->(dstComp:Component)
                    WHERE id(dst) = $containerId
                    MATCH (src:Container)-[r:Relationship]->(dstComp)
                    WHERE 
                        (src)-[:Deploy]->(:ContainerInstance)
                        AND EXISTS {
                            MATCH (ssCheck:SoftwareSystem)-[:Child]->(src)
                            WHERE ssCheck.cmdb <> $cmdb
                        }
                    OPTIONAL MATCH (ss:SoftwareSystem)-[:Child]->(src)
                    OPTIONAL MATCH (src)-[:Deploy]->(ci:ContainerInstance)<-[:Child]-(dn:DeploymentNode)
                    RETURN
                        r, src, dst, ci, dstComp,
                        ss AS parentSoftwareSystem,
                        dn AS parentDeploymentNode
                """;

        Value params = Values.parameters("containerId", containerId, "cmdb", cmdb);
        return neo4jSessionManager.getSession().run(cypher, params);
    }

    public Result findIncomingComponentRelationshipsExtended(Long containerId, String cmdb) {
        String cypher = """
                    MATCH (parentContainer:Container)-[:Child]->(dstComp:Component)
                    WHERE id(parentContainer) = $containerId
                    MATCH (srcComp:Component)-[r:Relationship]->(dstComp)
                    MATCH (src:Container)-[:Child]->(srcComp)
                    WHERE 
                        (src)-[:Deploy]->(:ContainerInstance)
                        AND EXISTS {
                            MATCH (ssCheck:SoftwareSystem)-[:Child]->(src)
                            WHERE ssCheck.cmdb <> $cmdb
                        }
                    OPTIONAL MATCH (ss:SoftwareSystem)-[:Child]->(src)
                    OPTIONAL MATCH (src)-[:Deploy]->(ci:ContainerInstance)<-[:Child]-(dn:DeploymentNode)
                    RETURN 
                        r, srcComp, dstComp, src, parentContainer, ci,
                        ss AS parentSoftwareSystem,
                        dn AS parentDeploymentNode
                """;

        Value params = Values.parameters("containerId", containerId, "cmdb", cmdb);
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


}