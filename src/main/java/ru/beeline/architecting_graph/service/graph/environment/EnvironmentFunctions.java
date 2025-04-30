package ru.beeline.architecting_graph.service.graph.environment;

import org.neo4j.driver.Session;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.service.graph.commonFunctions.CommonFunctions;

import java.util.HashMap;

public class EnvironmentFunctions {

    public static void updateEnvironment(Session session, String graphTag, String environment,
                                         HashMap<String, GraphObject> objects) {

        GraphObject environmentGraphObject = new GraphObject("Environment", "name", environment);

        if (!CommonFunctions.checkIfObjectExists(session, graphTag, environmentGraphObject)) {
            CommonFunctions.createObject(session, graphTag, environmentGraphObject);
        }

        if (!objects.containsKey(environment)) {
            objects.put(environment, environmentGraphObject);
        }
    }
}
