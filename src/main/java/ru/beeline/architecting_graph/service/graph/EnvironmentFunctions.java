package ru.beeline.architecting_graph.service.graph;

import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.repository.neo4j.BuildGraphQuery;

import java.util.HashMap;

@Component
public class EnvironmentFunctions {

    @Autowired
    BuildGraphQuery buildGraphQuery;

    public void updateEnvironment(Session session, String graphTag, String environment,
                                  HashMap<String, GraphObject> objects) {

        GraphObject environmentGraphObject = new GraphObject("Environment", "name", environment);

        if (!buildGraphQuery.checkIfObjectExists(session, graphTag, environmentGraphObject)) {
            CommonFunctions.createObject(session, graphTag, environmentGraphObject);
        }

        if (!objects.containsKey(environment)) {
            objects.put(environment, environmentGraphObject);
        }
    }
}
