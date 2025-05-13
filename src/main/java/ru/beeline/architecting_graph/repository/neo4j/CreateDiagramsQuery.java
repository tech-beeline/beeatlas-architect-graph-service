package ru.beeline.architecting_graph.repository.neo4j;

import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Component;

@Component
public class CreateDiagramsQuery {

    public Result checkIfContainerExists(Session session, String softwareSystemMnemonic, String containerMnemonic) {
        String query = """
            MATCH (a:SoftwareSystem {graphTag: "Global", structurizr_dsl_identifier: $val1})
                  -[r:Child]->(b:Container {graphTag: "Global", structurizr_dsl_identifier: $val2})
            WHERE r.graphTag = "Global"
            RETURN EXISTS((a)-->(b)) AS relationship_exists
        """;
        Value parameters = Values.parameters("val1", softwareSystemMnemonic, "val2", containerMnemonic);
        return session.run(query, parameters);
    }

    public Result checkIfEnvironmentExists(Session session, String environment) {
        String query = """
            MATCH (n:Environment {name: $val1})
            RETURN n
        """;
        Value parameters = Values.parameters("val1", environment);
        return session.run(query, parameters);
    }
}
