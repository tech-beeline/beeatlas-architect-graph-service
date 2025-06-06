package ru.beeline.architecting_graph.repository.neo4j;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Repository;
import ru.beeline.architecting_graph.model.*;

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

        public void setProperty(Session session, String graphTag, String containerInstanceName, String key,
                        Object value) {
                String query = "MATCH (n:ContainerInstance {graphTag: $graphTag1, name: $val1}) SET n." + key
                                + " = $value";
                Value parameters = Values.parameters("graphTag1", graphTag, "val1", containerInstanceName, "value",
                                value);
                session.run(query, parameters);
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

        public void setInfrastructureNodeProperty(Session session, String graphTag, String nodeName,
                        String propertyKey, Object propertyValue) {
                String query = "MATCH (n:InfrastructureNode {graphTag: $graphTag1, name: $name1}) SET n." + propertyKey
                                + " = $value";
                Value parameters = Values.parameters("graphTag1", graphTag, "name1", nodeName, "value", propertyValue);
                session.run(query, parameters);
        }

        public void updateInfrastructureNode(Session session, String graphTag, String name,
                        String description, String technology, String tags,
                        String url, String endVersion) {
                String query = "MATCH (n:InfrastructureNode {graphTag: $graphTag1, name: $name1}) "
                                + "SET n.description = $description1, n.technology = $technology1, n.tags = $tags1, "
                                + "n.url = $url1, n.endVersion = $endVersion1";
                Value parameters = Values.parameters(
                                "graphTag1", graphTag, "name1", name, "description1", description, "technology1",
                                technology,
                                "tags1", tags, "url1", url, "endVersion1", endVersion);
                session.run(query, parameters);
        }

        public Result findInfrastructureNodesWithNullEndVersion(Session session, String graphTag,
                        String deploymentNodeName) {
                String cypher = "MATCH (n:DeploymentNode {name: $name1, graphTag: $graphTag1})-[r:Child]->(m:InfrastructureNode) "
                                + "WHERE m.endVersion IS NULL RETURN m.name AS infrastructureNodeName";
                Value parameters = Values.parameters("graphTag1", graphTag, "name1", deploymentNodeName);
                return session.run(cypher, parameters);
        }

        public Result findChildDeploymentNodesWithNullEndVersion(Session session, String graphTag,
                        String deploymentNodeName) {
                String cypher = "MATCH (n:DeploymentNode {name: $name1, graphTag: $graphTag1})-[r:Child]->(m:DeploymentNode) "
                                + "WHERE m.endVersion IS NULL RETURN m.name AS childDeploymentNodeName";
                Value parameters = Values.parameters("graphTag1", graphTag, "name1", deploymentNodeName);
                return session.run(cypher, parameters);
        }

        public void updateDeploymentNode(Session session, String graphTag, DeploymentNode deploymentNode) {
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
                session.run(cypher, parameters);
        }

        public void setDeploymentNodeProperty(Session session, String graphTag, String name, String propertyKey,
                        Object value) {
                String cypher = "MATCH (n:DeploymentNode {graphTag: $graphTag1, name: $name1}) SET n." + propertyKey
                                + " = $value";
                Value parameters = Values.parameters("graphTag1", graphTag, "name1", name, "value", value);
                session.run(cypher, parameters);
        }

        public void createRelationshipQuery(Session session, String graphTag, Relationship relationship,
                        Connection connection) {
                String cypher = "MATCH (a:" + connection.getSource().getType() + " {graphTag: $graphTag1, "
                                + connection.getSource().getKey() + ": $value1}), (b:"
                                + connection.getDestination().getType()
                                + " {graphTag: $graphTag1, " + connection.getDestination().getKey() + ": $value2}) "
                                + "CREATE (a)-[r:" + connection.getRelationshipType()
                                + " {graphTag: $graphTag1, sourceWorkspace: $cmdb, description: $description1}]->(b) RETURN a, b";

                Value parameters = Values.parameters("graphTag1", graphTag, "value1", connection.getSource().getValue(),
                                "value2", connection.getDestination().getValue(), "cmdb", connection.getCmdb(),
                                "description1", relationship.getDescription());
                session.run(cypher, parameters);
        }

        public void setRelationshipProperty(Session session, String graphTag, String key, Object value,
                        Relationship relationship,
                        Connection connection) {
                String cypher = "MATCH (a:" + connection.getSource().getType() + " {graphTag: $graphTag1, "
                                + connection.getSource().getKey()
                                + ": $val1})" + "-[r:" + connection.getRelationshipType() +
                                " {graphTag: $graphTag1, sourceWorkspace: $cmdb, description: $description1}]->" + "(b:"
                                + connection.getDestination().getType()
                                + " {graphTag: $graphTag1, " + connection.getDestination().getKey() + ": $val2}) "
                                + "SET r." + key + " = $value";
                Value parameters = Values.parameters("graphTag1", graphTag, "val1", connection.getSource().getValue(),
                                "val2", connection.getDestination().getValue(), "cmdb", connection.getCmdb(),
                                "description1", relationship.getDescription(),
                                "value", value);
                session.run(cypher, parameters);
        }

        public Value getRelationshipParameter(Session session, String graphTag, String realtionshipDescription,
                        Connection connection, String parameter) {

                String getParameter = "MATCH (a:" + connection.getSource().getType() + " {graphTag: $graphTag1, "
                                + connection.getSource().getKey() + ": $val1})-[r:" + connection.getRelationshipType()
                                + " {graphTag: $graphTag1, sourceWorkspace: $cmdb, description: $description1}]->(b:"
                                + connection.getDestination().getType() + " {graphTag: $graphTag1, "
                                + connection.getDestination().getKey() + ": $val2}) RETURN r." + parameter
                                + " AS parameter";
                Value parameters = Values.parameters("graphTag1", graphTag, "val1",
                                connection.getSource().getValue(), "cmdb", connection.getCmdb(), "description1",
                                realtionshipDescription, "val2", connection.getDestination().getValue());
                Result result = session.run(getParameter, parameters);
                return result.next().get("parameter");
        }

        public void setRelationshipParameter(Session session, String graphTag, String realtionshipDescription,
                        Connection connection, String parameter, String value) {
                String setParameter = "MATCH (a:" + connection.getSource().getType() + " {graphTag: $graphTag1, "
                                + connection.getSource().getKey() + ": $val1})-[r:" + connection.getRelationshipType()
                                + " {graphTag: $graphTag1, sourceWorkspace: $cmdb, description: $description1}]->(b:"
                                + connection.getDestination().getType() + " {graphTag: $graphTag1, "
                                + connection.getDestination().getKey() + ": $val2}) SET r." + parameter
                                + " = $parameter";
                Value parameters = Values.parameters("graphTag1", graphTag, "val1",
                                connection.getSource().getValue(), "cmdb", connection.getCmdb(), "description1",
                                realtionshipDescription, "val2", connection.getDestination().getValue(), "parameter",
                                value);
                session.run(setParameter, parameters);
        }

        public Boolean checkIfRelationshipExists(Session session, String graphTag, Relationship relationship,
                        Connection connection) {
                String query = "MATCH (a:" + connection.getSource().getType() + " {graphTag: $graphTag1, "
                                + connection.getSource().getKey() + ": $value1})-[r:" + connection.getRelationshipType()
                                + " {sourceWorkspace: $cmdb, description: $description1, graphTag: $graphTag1}]->(b:"
                                + connection.getDestination().getType() + " {graphTag: $graphTag1, "
                                + connection.getDestination().getKey()
                                + ": $value2}) RETURN EXISTS((a)-->(b)) AS relationshipExists";
                Value parameters = Values.parameters("graphTag1", graphTag, "value1", connection.getSource().getValue(),
                                "value2", connection.getDestination().getValue(), "cmdb", connection.getCmdb(),
                                "description1", relationship.getDescription());
                Result result = session.run(query, parameters);
                if (result.hasNext()) {
                        return true;
                }
                return false;
        }

        public void buildSetRelationshipParameters(Session session, String graphTag, Relationship relationship,
                        Connection connection) {
                String setParameters = "MATCH (a:" + connection.getSource().getType() + " {graphTag: $graphTag1, "
                                + connection.getSource().getKey() + ": $val1})-[r:" + connection.getRelationshipType()
                                + " {graphTag: $graphTag1, sourceWorkspace: $cmdb, description: $description1}]->(b:"
                                + connection.getDestination().getType() + " {graphTag: $graphTag1, "
                                + connection.getDestination().getKey()
                                + ": $val2}) SET r.endVersion = $endVersion1, r.tags = $tags1,  "
                                + "r.url = $url1, r.technology = $technology1, r.interactionStyle = $interactionStyle1, r.level = $level1";
                Value parameters = Values.parameters("graphTag1", graphTag, "val1",
                                connection.getSource().getValue(), "cmdb", connection.getCmdb(), "description1",
                                relationship.getDescription(), "val2", connection.getDestination().getValue(),
                                "endVersion1", null,
                                "tags1", relationship.getTags(), "url1", relationship.getUrl(), "technology1",
                                relationship.getTechnology(), "interactionStyle1", relationship.getInteractionStyle(),
                                "level1",
                                connection.getLevel());
                session.run(setParameters, parameters);
        }

        public void setSystemParameters(Session session, String graphTag, String cmdb, SoftwareSystem softwareSystem,
                        String version) {
                String setParameters = "MATCH (n:SoftwareSystem {graphTag: $graphTag1, cmdb: $cmdb1}) "
                                + "SET n.name = $name1, n.description = $description1, n.tags = $tags1, n.url = $url1, "
                                + "n.group = $group1, n.version = $version1";
                Value parameters = Values.parameters("graphTag1", graphTag, "cmdb1", cmdb, "name1",
                                softwareSystem.getName(), "description1", softwareSystem.getDescription(), "tags1",
                                softwareSystem.getTags(), "url1", softwareSystem.getUrl(), "group1",
                                softwareSystem.getGroup(),
                                "version1", version);
                session.run(setParameters, parameters);
        }

        public void updateSystemProperty(Session session, String graphTag, String cmdb, String key, Object value) {
                String setProperty = "MATCH (n:SoftwareSystem {graphTag: $graphTag1, cmdb: $cmdb1}) SET n." + key
                                + " = $value";
                Value parameters = Values.parameters("graphTag1", graphTag, "cmdb1", cmdb, "value", value);
                session.run(setProperty, parameters);
        }

        public void deleteGraph(Session session, String graphTag) {
                String deleteLocalGraph = "MATCH (n) WHERE n.graphTag = $graphTag1 DETACH DELETE n";
                Value parameters = Values.parameters("graphTag1", graphTag);
                session.run(deleteLocalGraph, parameters);
        }

        public Result getRelationships(Session session, String graphTag, String cmdb) {
                String getRelationships = "MATCH (n)-[r {sourceWorkspace: $cmdb, graphTag: $graphTag1}]->(m) "
                                + "WHERE r.endVersion IS NULL RETURN n,m,r";
                Value parameters = Values.parameters("graphTag1", graphTag, "cmdb", cmdb);
                return session.run(getRelationships, parameters);
        }

        public Result getDeploymentNodeNames(Session session, String graphTag, String cmdb) {
                String getDeploymentNode = "MATCH (n:SoftwareSystem {cmdb: $cmdb1, graphTag: $graphTag1})-[r:Child]->(m:DeploymentNode) "
                                + "WHERE m.endVersion IS NULL RETURN m.name as deploymentNodeName";
                Value parameters = Values.parameters("graphTag1", graphTag, "cmdb1", cmdb);
                return session.run(getDeploymentNode, parameters);
        }

        public Result getContainers(Session session, String graphTag, String cmdb) {
                String getContainers = "MATCH (n:SoftwareSystem {cmdb: $cmdb1, graphTag: $graphTag1})-[r:Child]->(m:Container) "
                                + "WHERE m.endVersion IS NULL RETURN m.name AS containerName";
                Value parameters = Values.parameters("graphTag1", graphTag, "cmdb1", cmdb);
                return session.run(getContainers, parameters);
        }

        public Result getContainerRelationships(Session session, String graphTag, String containerExternalName) {
                String findConnects = "MATCH (n:Container {graphTag: $graphTag1, external_name: $external_name1})-[r]-() "
                                + "RETURN count(r) AS numberOfRelationships";
                Value parameters = Values.parameters("graphTag1", graphTag, "external_name1", containerExternalName);
                return session.run(findConnects, parameters);
        }

        public void setContainerParameters(Session session, String graphTag, Container container,
                        GraphObject containerGraphObject, String curVersion) {
                String setParameters = "MATCH (n:Container {graphTag: $graphTag1, " + containerGraphObject.getKey()
                                + ": $value}) "
                                + "SET n.description = $description1, n.technology = $technology1, n.tags = $tags1, "
                                + "n.url = $url1, n.group = $group1, n.endVersion = $endVersion1";
                Value parameters = Values.parameters("graphTag1", graphTag, "value", containerGraphObject.getValue(),
                                "description1", container.getDescription(), "technology1", container.getTechnology(),
                                "tags1",
                                container.getTags(), "url1", container.getUrl(), "group1", container.getGroup(),
                                "endVersion1", null);
                session.run(setParameters, parameters);
        }

        public void executeSetContainerProperties(Session session, String graphTag, String containerName, String key,
                        Object value) {
                String setProperties = "MATCH (n:Container {graphTag: $graphTag1, name: $name1}) SET n." + key
                                + " = $value";
                Value parameters = Values.parameters("graphTag1", graphTag, "name1", containerName, "value", value);
                session.run(setProperties, parameters);
        }
}
