package ru.beeline.architecting_graph.graph.environment;

import java.util.HashMap;

import org.neo4j.driver.Session;

import ru.beeline.architecting_graph.graph.commonFunctions.CommonFunctions;
import ru.beeline.architecting_graph.graph.graphObject.GraphObject;

public class EnvironmentFunctions {

    public static void updateEnvironment(Session session, String graphTag, String environment,
            HashMap<String, GraphObject> objects) {

        GraphObject environmentGraphObject = GraphObject.createGraphObject("Environment", "name", environment);

        if (!CommonFunctions.checkIfObjectExists(session, graphTag, environmentGraphObject)) {
            CommonFunctions.createObject(session, graphTag, environmentGraphObject);
        }

        if (!objects.containsKey(environment)) {
            objects.put(environment, environmentGraphObject);
        }
    }
}
