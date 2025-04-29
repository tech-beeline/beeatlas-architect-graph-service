package ru.beeline.architecting_graph.service.graph.relationship;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import ru.beeline.architecting_graph.service.graph.connection.Connection;
import ru.beeline.architecting_graph.model.GraphObject;

public class RelationshipEndVersionFunctions {
    public static GraphObject getGraphObject(Value graphNode) {

        String label = graphNode.asNode().labels().toString();
        String type = label.substring(1, label.length() - 1);
        String key = "name";

        if (type.equals("SoftwareSystem")) {
            key = "cmdb";
        }

        Object value = graphNode.asNode().asMap().get(key);

        if (value == null) {
            value = graphNode.asNode().asMap().get("external_name");
        }

        return GraphObject.createGraphObject(type, key, value.toString());
    }

    public static void setRelationshipsEndVersion(Session session, String graphTag, String curVersion, String cmdb) {

        String getRelationships = "MATCH (n)-[r {sourceWorkspace: $cmdb, graphTag: $graphTag1}]->(m) "
                + "WHERE r.endVersion IS NULL RETURN n,m,r";
        Value parameters = Values.parameters("graphTag1", graphTag, "cmdb", cmdb);
        Result result = session.run(getRelationships, parameters);

        while (result.hasNext()) {

            org.neo4j.driver.Record record = result.next();

            Connection connection = new Connection();

            connection.setSource(getGraphObject(record.get("n")));
            connection.setDestination(getGraphObject(record.get("m")));
            connection.setCmdb(cmdb);

            Value connectValue = record.get("r");
            connection.setRelationshipType(connectValue.asRelationship().type().toString());
            String realtionshipDescription = connectValue.asRelationship().get("description").toString();
            realtionshipDescription = realtionshipDescription.substring(1, realtionshipDescription.length() - 1);

            RelationshipUpdateFunctions.setRelationshipParameter(session, graphTag, realtionshipDescription, connection,
                    "endVersion", curVersion);
        }
    }
}
