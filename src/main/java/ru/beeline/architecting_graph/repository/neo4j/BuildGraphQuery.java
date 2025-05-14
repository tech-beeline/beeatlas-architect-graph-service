package ru.beeline.architecting_graph.repository.neo4j;

import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Component;
import ru.beeline.architecting_graph.model.GraphObject;

@Component
public class BuildGraphQuery {

    public  boolean checkIfObjectExists(Session session, String graphTag, GraphObject graphObject) {
        String checkObjectExist = "MATCH (n:" + graphObject.getType() + " {" + graphObject.getKey()
                + ": $value, graphTag: $graphTag1}) RETURN n";
        Value parameters = Values.parameters("value", graphObject.getValue(), "graphTag1", graphTag);
        Result result = session.run(checkObjectExist, parameters);
        return result.hasNext();
    }
}
