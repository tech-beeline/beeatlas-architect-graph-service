package ru.beeline.architecting_graph.repository.neo4j;

import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CompareVersionsQuery {

    public Result getSystemRelationshipsOut(Session session, String cmdb) {
        String query = """
                    MATCH (n:SoftwareSystem {graphTag: "Global", cmdb: $val1})
                    -[r:Relationship {sourceWorkspace: $cmdb}]->(m)
                    RETURN n, m, r.startVersion, r.endVersion, r.description
                """;
        Value parameters = Values.parameters("val1", cmdb, "cmdb", cmdb);
        return session.run(query, parameters);
    }

    public Result getSystemRelationshipsIn(Session session, String cmdb) {
        String query = """
                    MATCH (m)-[r:Relationship {sourceWorkspace: $cmdb}]
                    ->(n:SoftwareSystem {graphTag: "Global", cmdb: $val1})
                    RETURN n, m, r.startVersion, r.endVersion, r.description
                """;
        Value parameters = Values.parameters("val1", cmdb, "cmdb", cmdb);
        return session.run(query, parameters);
    }

    public Result getContainers(Session session, String cmdb) {
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Child]->(m:Container) " +
                "RETURN n, m, m.name, m.startVersion, m.endVersion";
        Value parameters = Values.parameters("val1", cmdb);
        return session.run(query, parameters);
    }

    public Result getComponents(Session session, String containerName) {
        String query = "MATCH (n:Container {graphTag: \"Global\", name: $val1})-[r:Child]->(m:Component) " +
                "RETURN n, m, m.name, m.startVersion, m.endVersion";
        Value parameters = Values.parameters("val1", containerName);
        return session.run(query, parameters);
    }

    public Result getNodeRelationshipsOut(Session session, String label, String name, String cmdb) {
        String query = String.format(
                "MATCH (n:%s {graphTag: \"Global\", name: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) " +
                        "RETURN n, m, r.startVersion, r.endVersion, r.description",
                label);
        Value parameters = Values.parameters("val1", name, "cmdb", cmdb);
        return session.run(query, parameters);
    }

    public Result getNodeRelationshipsIn(Session session, String label, String name, String cmdb) {
        String query = String.format(
                "MATCH (m)-[r:Relationship {sourceWorkspace: $cmdb}]->(n:%s {graphTag: \"Global\", name: $val1}) " +
                        "RETURN n, m, r.startVersion, r.endVersion, r.description",
                label);
        Value parameters = Values.parameters("val1", name, "cmdb", cmdb);
        return session.run(query, parameters);
    }

    public Result getSoftwareSystemInstances(Session session, String cmdb) {
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Deploy {sourceWorkspace: $cmdb}]->(m) "
                +
                "RETURN n, m, r.startVersion, r.endVersion";
        Value parameters = Values.parameters("val1", cmdb, "cmdb", cmdb);
        return session.run(query, parameters);
    }

    public Result getDeploymentNodes(Session session, String cmdb) {
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Child]->(m:DeploymentNode) " +
                "RETURN n, m, m.name, m.startVersion, m.endVersion";
        Value parameters = Values.parameters("val1", cmdb);
        return session.run(query, parameters);
    }

    public Result getContainerRelationshipsOut(Session session, String containerName, String cmdb) {
        String query = "MATCH (n:Container {graphTag: \"Global\", name: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) "
                +
                "RETURN n, m, r.startVersion, r.endVersion, r.description";
        Value parameters = Values.parameters("val1", containerName, "cmdb", cmdb);
        return session.run(query, parameters);
    }

    public Result getContainerRelationshipsIn(Session session, String containerName, String cmdb) {
        String query = "MATCH (m)-[r:Relationship {sourceWorkspace: $cmdb}]->(n:Container {graphTag: \"Global\", name: $val1}) "
                +
                "RETURN n, m, r.startVersion, r.endVersion, r.description";
        Value parameters = Values.parameters("val1", containerName, "cmdb", cmdb);
        return session.run(query, parameters);
    }

    public Result getContainerComponents(Session session, String containerName) {
        String query = "MATCH (n:Container {graphTag: \"Global\", name: $val1})-[r:Child]->(m:Component) " +
                "RETURN n, m, m.name, m.startVersion, m.endVersion";
        Value parameters = Values.parameters("val1", containerName);
        return session.run(query, parameters);
    }

    public Result getComponentRelationshipsOut(Session session, String componentName, String cmdb) {
        String query = "MATCH (n:Component {graphTag: \"Global\", name: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) "
                +
                "RETURN n, m, r.startVersion, r.endVersion, r.description";
        Value parameters = Values.parameters("val1", componentName, "cmdb", cmdb);
        return session.run(query, parameters);
    }

    public Result getComponentRelationshipsIn(Session session, String componentName, String cmdb) {
        String query = "MATCH (m)-[r:Relationship {sourceWorkspace: $cmdb}]->(n:Component {graphTag: \"Global\", name: $val1}) "
                +
                "RETURN n, m, r.startVersion, r.endVersion, r.description";
        Value parameters = Values.parameters("val1", componentName, "cmdb", cmdb);
        return session.run(query, parameters);
    }

    public Result getChildren(Session session, String parentType, String childType, String parentName) {
        String query = "MATCH (n:" + parentType + " {graphTag: \"Global\", name: $val1})-[r:Child]->(m:" + childType
                + ") " +
                "RETURN n, m, m.name, m.startVersion, m.endVersion";
        Value params = Values.parameters("val1", parentName);
        return session.run(query, params);
    }

    public Result getRelationships(Session session, String label, String name, String cmdb) {
        String query = "MATCH (n:" + label
                + " {graphTag: \"Global\", name: $val1})-[r:Relationship {sourceWorkspace: $cmdb}]->(m) " +
                "RETURN n, m, r.startVersion, r.endVersion, r.description";
        Value params = Values.parameters("cmdb", cmdb, "val1", name);
        return session.run(query, params);
    }

    public boolean productExists(Session session, String cmdb) {
        String query = "MATCH (p:SoftwareSystem {graphTag: 'Global', cmdb: $cmdb}) RETURN p LIMIT 1";
        Record productRecord = session.run(query, Values.parameters("cmdb", cmdb)).single();
        return productRecord != null;
    }

    public List<String> getDependentSystems(Session session, String cmdb) {
        String query = "MATCH (p:SoftwareSystem {graphTag: 'Global', cmdb: $cmdb})-[r:Relationship]->(dependent:SoftwareSystem) " +
                "RETURN collect(dependent.cmdb) AS dependentSystems";
        return session.run(query, Values.parameters("cmdb", cmdb))
                .single()
                .get("dependentSystems")
                .asList(Value::asString);
    }

    public List<String> getInfluencingSystems(Session session, String cmdb) {
        String query = "MATCH (influencing:SoftwareSystem)-[r:Relationship]->(p:SoftwareSystem {graphTag: 'Global', cmdb: $cmdb}) " +
                "RETURN collect(influencing.cmdb) AS influencingSystems";
        return session.run(query, Values.parameters("cmdb", cmdb))
                .single()
                .get("influencingSystems")
                .asList(Value::asString);
    }
}
