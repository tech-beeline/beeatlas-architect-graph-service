package ru.beeline.architecting_graph.service.graph.commonFunctions;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import ru.beeline.architecting_graph.model.GraphObject;

@Slf4j
public class CommonFunctions {

    public static boolean checkIfObjectExists(Session session, String graphTag, GraphObject graphObject) {
        String checkObjectExist = "MATCH (n:" + graphObject.getType() + " {" + graphObject.getKey()
                + ": $value, graphTag: $graphTag1}) RETURN n";
        Value parameters = Values.parameters("value", graphObject.getValue(), "graphTag1", graphTag);
        Result result = session.run(checkObjectExist, parameters);
        return result.hasNext();
    }

    public static void createObject(Session session, String graphTag, GraphObject graphObject) {
        String createObject = "CREATE (n:" + graphObject.getType() + " {graphTag: $graphTag1, "
                + graphObject.getKey() + ": $value})";
        Value parameters = Values.parameters("graphTag1", graphTag, "value", graphObject.getValue());
        session.run(createObject, parameters);
    }

    public static Value getObjectParameter(Session session, String graphTag, GraphObject graphObject,
                                           String parameter) {
        String getParameter = "MATCH (n:" + graphObject.getType() + " {graphTag: $graphTag1, "
                + graphObject.getKey() + ": $value}) " + "RETURN n." + parameter + " AS parameter";
        log.info(getParameter);
        Value parameters = Values.parameters("graphTag1", graphTag, "value", graphObject.getValue());
        Result result = session.run(getParameter, parameters);
        return result.next().get("parameter");
    }

    public static void setObjectParameter(Session session, String graphTag, GraphObject graphObject,
                                          String parameter, String value) {

        String setParameter = "MATCH (n:" + graphObject.getType() + " {graphTag: $graphTag1, "
                + graphObject.getKey() + ": $value}) " + "SET n." + parameter + " = $parameter1";
        Value parameters = Values.parameters("graphTag1", graphTag, "value", graphObject.getValue(),
                "parameter1", value);
        session.run(setParameter, parameters);
    }
}
