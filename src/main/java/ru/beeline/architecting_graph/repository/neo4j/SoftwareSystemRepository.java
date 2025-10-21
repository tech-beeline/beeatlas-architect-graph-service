package ru.beeline.architecting_graph.repository.neo4j;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Repository;
import ru.beeline.architecting_graph.model.SoftwareSystem;

import java.util.List;

@Slf4j
@Repository
public class SoftwareSystemRepository {
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

    public List<String> getInfluencingSystems(Session session, String cmdb) {
        String query =
                "MATCH (influencing:SoftwareSystem)-[r:Relationship]->(p:SoftwareSystem {graphTag: 'Global'}) " +
                        "WHERE toLower(p.cmdb) = toLower($cmdb) " +
                        "RETURN collect(influencing.cmdb) AS influencingSystems";
        return session.run(query, Values.parameters("cmdb", cmdb))
                .single()
                .get("influencingSystems")
                .asList(Value::asString);
    }

    public List<String> getDependentSystems(Session session, String cmdb) {
        String query =
                "MATCH (p:SoftwareSystem {graphTag: 'Global'}) " +
                        "WHERE toLower(p.cmdb) = toLower($cmdb) " +
                        "MATCH (p)-[r:Relationship]->(dependent:SoftwareSystem) " +
                        "RETURN collect(dependent.cmdb) AS dependentSystems";
        return session.run(query, Values.parameters("cmdb", cmdb))
                .single()
                .get("dependentSystems")
                .asList(Value::asString);
    }

    public boolean productExists(Session session, String cmdb) {
        String query = "MATCH (p:SoftwareSystem {graphTag: 'Global'}) " +
                "WHERE toLower(p.cmdb) = toLower($cmdb) " +
                "RETURN p LIMIT 1";
        Record productRecord = session.run(query, Values.parameters("cmdb", cmdb)).single();
        return productRecord != null;
    }

    public void updateSystemProperty(Session session, String graphTag, String cmdb, String key, Object value) {
        String setProperty = "MATCH (n:SoftwareSystem {graphTag: $graphTag1, cmdb: $cmdb1}) SET n." + key
                + " = $value";
        Value parameters = Values.parameters("graphTag1", graphTag, "cmdb1", cmdb, "value", value);
        session.run(setProperty, parameters);
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

}
