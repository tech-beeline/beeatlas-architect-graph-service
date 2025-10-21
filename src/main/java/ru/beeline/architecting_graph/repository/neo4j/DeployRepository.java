package ru.beeline.architecting_graph.repository.neo4j;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class DeployRepository {
    public Result getSoftwareSystemInstances(Session session, String cmdb) {
        String query = "MATCH (n:SoftwareSystem {graphTag: \"Global\", cmdb: $val1})-[r:Deploy {sourceWorkspace: $cmdb}]->(m) "
                +
                "RETURN n, m, r.startVersion, r.endVersion";
        Value parameters = Values.parameters("val1", cmdb, "cmdb", cmdb);
        return session.run(query, parameters);
    }

}
