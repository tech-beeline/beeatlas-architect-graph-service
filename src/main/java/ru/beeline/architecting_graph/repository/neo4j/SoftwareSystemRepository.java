package ru.beeline.architecting_graph.repository.neo4j;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ru.beeline.architecting_graph.model.SoftwareSystem;
import ru.beeline.architecting_graph.service.graph.Neo4jSessionManager;

import java.util.List;

@Slf4j
@Repository
public class SoftwareSystemRepository {
    @Autowired
    private Neo4jSessionManager neo4jSessionManager;

    public Result getSystem(String systemDSLIdentifier) {
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", structurizr_dsl_identifier: $val1}) RETURN n";
        Value parameters = Values.parameters("val1", systemDSLIdentifier);
        return neo4jSessionManager.getSession().run(query, parameters);
    }

    public Result getSystemById(Long id) {
        String query = "MATCH (d:SoftwareSystem) WHERE id(d) = $val1 RETURN d";
        Value parameters = Values.parameters("val1", id);
        return neo4jSessionManager.getSession().run(query, parameters);
    }

    public Result getParentSystem(String containerDSLIdentifier) {
        String query = "MATCH (m:SoftwareSystem)-[r:Child]->(n:Container "
                + "{graphTag: \"Global\", structurizr_dsl_identifier: $val1}) RETURN m.structurizr_dsl_identifier";
        Value parameters = Values.parameters("val1", containerDSLIdentifier);
        return neo4jSessionManager.getSession().run(query, parameters);
    }
    public Result getParentSystemByDeploymentNodeId(Long id) {
        String query = "MATCH (parent:SoftwareSystem)-[r:Child]->(child:DeploymentNode)" +
                "WHERE id(child) = $val1 " +
                "RETURN parent";
        Value parameters = Values.parameters("val1", id);
        return neo4jSessionManager.getSession().run(query, parameters);
    }

    public List<String> getInfluencingSystems(String cmdb) {
        String query =
                "MATCH (influencing:SoftwareSystem)-[r:Relationship]->(p:SoftwareSystem {graphTag: 'Global'}) " +
                        "WHERE toLower(p.cmdb) = toLower($cmdb) " +
                        "RETURN collect(influencing.cmdb) AS influencingSystems";
        return neo4jSessionManager.getSession().run(query, Values.parameters("cmdb", cmdb))
                .single()
                .get("influencingSystems")
                .asList(Value::asString);
    }

    public List<String> getDependentSystems(String cmdb) {
        String query =
                "MATCH (p:SoftwareSystem {graphTag: 'Global'}) " +
                        "WHERE toLower(p.cmdb) = toLower($cmdb) " +
                        "MATCH (p)-[r:Relationship]->(dependent:SoftwareSystem) " +
                        "RETURN collect(dependent.cmdb) AS dependentSystems";
        return neo4jSessionManager.getSession().run(query, Values.parameters("cmdb", cmdb))
                .single()
                .get("dependentSystems")
                .asList(Value::asString);
    }

    public Result searchSoftwareSystemsByCMDBorName(String search) {
        String query =
                "MATCH (n:SoftwareSystem)" +
                        "WHERE toLower(n.graphTag) = toLower('Global')" +
                        "AND (toLower(n.cmdb) CONTAINS toLower($search) OR toLower(n.name) CONTAINS toLower($search))" +
                        "RETURN n";
        Value parameters = Values.parameters("search", search);
        return neo4jSessionManager.getSession().run(query, parameters);
    }

    public boolean productExists(String cmdb) {
        String query = "MATCH (p:SoftwareSystem {graphTag: 'Global'}) " +
                "WHERE toLower(p.cmdb) = toLower($cmdb) " +
                "RETURN p LIMIT 1";
        Record productRecord = neo4jSessionManager.getSession().run(query, Values.parameters("cmdb", cmdb)).single();
        return productRecord != null;
    }

    public void updateSystemProperty(String graphTag, String cmdb, String key, Object value) {
        String setProperty = "MATCH (n:SoftwareSystem {graphTag: $graphTag1, cmdb: $cmdb1}) SET n." + key
                + " = $value";
        Value parameters = Values.parameters("graphTag1", graphTag, "cmdb1", cmdb, "value", value);
        neo4jSessionManager.getSession().run(setProperty, parameters);
    }

    public void setSystemParameters(String graphTag, String cmdb, SoftwareSystem softwareSystem,
                                    String version) {
        String setParameters = "MATCH (n:SoftwareSystem {graphTag: $graphTag1, cmdb: $cmdb1}) "
                + "SET n.name = $name1, n.description = $description1, n.tags = $tags1, n.url = $url1, "
                + "n.group = $group1, n.version = $version1";
        Value parameters = Values.parameters("graphTag1", graphTag, "cmdb1", cmdb, "name1",
                                             softwareSystem.getName(), "description1", softwareSystem.getDescription(), "tags1",
                                             softwareSystem.getTags(), "url1", softwareSystem.getUrl(), "group1",
                                             softwareSystem.getGroup(),
                                             "version1", version);
        neo4jSessionManager.getSession().run(setParameters, parameters);
    }

    public List<String> findDependentSystemsByContainerId(Long containerId) {
        String query =
                "MATCH (con:Container)-[r:Relationship ]->(parent:SoftwareSystem) " +
                        "WHERE id(con)=$containerId " +
                        "RETURN parent.cmdb";
        return neo4jSessionManager.getSession()
                .run(query, Values.parameters("containerId", containerId))
                .list(record -> record.get("parent.cmdb").asString());
    }

    public List<String> findDependentChildSystemsByComponent(Long componentId) {
        String query =
                "MATCH (com:Component)-[r:Relationship ]->(parent:SoftwareSystem) " +
                        "WHERE id(com)=$componentId " +
                        "RETURN parent";
        return neo4jSessionManager.getSession()
                .run(query, Values.parameters("componentId", componentId))
                .list(record -> record.get("parent.cmdb").asString());
    }
    public List<String> findDependentParentSystemsByComponent(Long componentId) {
        String query =
                "MATCH (parent:SoftwareSystem)-[r:Relationship ]->(com:Component) " +
                        "WHERE id(com)=$componentId" +
                        "RETURN parent";
        return neo4jSessionManager.getSession()
                .run(query, Values.parameters("componentId", componentId))
                .list(record -> record.get("parent.cmdb").asString());
    }

    public List<String> findInfluencingSystemsByNodeId(Long containerId) {
        String query =
                "MATCH (parent:SoftwareSystem )-[r:Relationship]->(con:Container) " +
                        "WHERE id(con)=$containerId " +
                        "RETURN parent";
        return  neo4jSessionManager.getSession()
                .run(query, Values.parameters("containerId", containerId))
                .list(record -> record.get("parent.cmdb").asString());
    }
}
