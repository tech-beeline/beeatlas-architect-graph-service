package ru.beeline.architecting_graph.graph.component;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import ru.beeline.architecting_graph.graph.graphObject.GraphObject;
import ru.beeline.architecting_graph.graph.commonFunctions.CommonFunctions;

public class ComponentEndVersionFunctions {

    public static void setComponentEndVersion(Session session, String graphTag, String containerName, String cmdb,
            String curVersion) {

        String getComponents = "MATCH (n:Container {name: $name1, graphTag: $graphTag1})-[r:Child]->(m:Component) " +
                "WHERE m.endVersion IS NULL RETURN m.name AS componentName";
        Value parameters = Values.parameters("graphTag1", graphTag, "name1", containerName);
        Result result = session.run(getComponents, parameters);

        while (result.hasNext()) {
            String componentName = result.next().get("componentName").toString();
            componentName = componentName.substring(1, componentName.length() - 1);
            GraphObject componentGraphObject = GraphObject.createGraphObject("Component", "name", componentName);

            CommonFunctions.setObjectParameter(session, graphTag, componentGraphObject, "endVersion", curVersion);
        }
    }
}
