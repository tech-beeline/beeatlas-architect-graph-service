package ru.beeline.architecting_graph.service.graph.container;

import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.service.graph.CommonFunctions;
import ru.beeline.architecting_graph.service.graph.component.ComponentEndVersionFunctions;

public class ContainerEndVersionFunctions {

    public static void setContainerEndVersion(Session session, String graphTag, String cmdb, String curVersion) {

        String getContainers = "MATCH (n:SoftwareSystem {cmdb: $cmdb1, graphTag: $graphTag1})-[r:Child]->(m:Container) "
                + "WHERE m.endVersion IS NULL RETURN m.name AS containerName";
        Value parameters = Values.parameters("graphTag1", graphTag, "cmdb1", cmdb);
        Result result = session.run(getContainers, parameters);

        while (result.hasNext()) {
            String containerName = result.next().get("containerName").toString();
            containerName = containerName.substring(1, containerName.length() - 1);
            GraphObject containerGraphObject = new GraphObject("Container", "name", containerName);

            CommonFunctions.setObjectParameter(session, graphTag, containerGraphObject, "endVersion", curVersion);
            ComponentEndVersionFunctions.setComponentEndVersion(session, graphTag, containerName, cmdb, curVersion);
        }
    }
}
