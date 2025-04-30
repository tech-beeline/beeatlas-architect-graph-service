package ru.beeline.architecting_graph.service.graph.infrastructureNode;

import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.service.graph.commonFunctions.CommonFunctions;

public class InfrastructureNodeEndVersionFunctions {

    public static void setInfrastructureNodeEndVersion(Session session, String graphTag, String deploymentNodeName,
                                                       String curVersion) {

        String getInfrastructureNodes = "MATCH (n:DeploymentNode "
                + "{name: $name1, graphTag: $graphTag1})-[r:Child]->(m:InfrastructureNode) "
                + "WHERE m.endVersion IS NULL RETURN m.name AS infrastructureNodeName";
        Value parameters = Values.parameters("graphTag1", graphTag, "name1", deploymentNodeName);
        Result result = session.run(getInfrastructureNodes, parameters);

        while (result.hasNext()) {
            String infrastructureNodeName = result.next().get("infrastructureNodeName").toString();
            infrastructureNodeName = infrastructureNodeName.substring(1, infrastructureNodeName.length() - 1);
            GraphObject infrastructureNodeGraphObject = new GraphObject("InfrastructureNode", "name",
                    infrastructureNodeName);
            CommonFunctions.setObjectParameter(session, graphTag, infrastructureNodeGraphObject, "endVersion",
                    curVersion);
        }
    }
}
