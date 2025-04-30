package ru.beeline.architecting_graph.service.graph.containerInstance;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.service.graph.commonFunctions.CommonFunctions;

public class ContainerInstanceEndVersionFunctions {

    public static void setContainerInstanceEndVersion(Session session, String graphTag, String deploymentNodeName,
            String curVersion) {

        String getContainerInstances = "MATCH (n:DeploymentNode "
                + "{name: $name1, graphTag: $graphTag1})-[r:Child]->(m:ContainerInstance) "
                + "WHERE m.endVersion IS NULL RETURN m.name AS containerInstanceName";
        Value parameters = Values.parameters("graphTag1", graphTag, "name1", deploymentNodeName);
        Result result = session.run(getContainerInstances, parameters);

        while (result.hasNext()) {
            String containerInstanceName = result.next().get("containerInstanceName").toString();
            containerInstanceName = containerInstanceName.substring(1, containerInstanceName.length() - 1);
            GraphObject containerInstanceGraphObject = GraphObject.createGraphObject("ContainerInstance", "name",
                    containerInstanceName);
            CommonFunctions.setObjectParameter(session, graphTag, containerInstanceGraphObject, "endVersion",
                    curVersion);
        }
    }
}
