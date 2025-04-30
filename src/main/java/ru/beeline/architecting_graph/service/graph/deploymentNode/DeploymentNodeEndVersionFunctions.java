package ru.beeline.architecting_graph.service.graph.deploymentNode;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;

import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.service.graph.commonFunctions.CommonFunctions;
import ru.beeline.architecting_graph.service.graph.infrastructureNode.InfrastructureNodeEndVersionFunctions;
import ru.beeline.architecting_graph.service.graph.containerInstance.ContainerInstanceEndVersionFunctions;

public class DeploymentNodeEndVersionFunctions {

    public static void setChildDeploymentNodeEndVersion(Session session, String graphTag, String deploymentNodeName,
            String curVersion, String cmdb) {

        String getChildDeploymentNodes = "MATCH (n:DeploymentNode "
                + "{name: $name1, graphTag: $graphTag1})-[r:Child]->(m:DeploymentNode) "
                + "WHERE m.endVersion IS NULL RETURN m.name AS childDeploymentNodeName";
        Value parameters = Values.parameters("graphTag1", graphTag, "name1", deploymentNodeName);
        Result result = session.run(getChildDeploymentNodes, parameters);

        while (result.hasNext()) {
            String childDeploymentNodeName = result.next().get("childDeploymentNodeName").toString();
            childDeploymentNodeName = childDeploymentNodeName.substring(1, childDeploymentNodeName.length() - 1);
            setDeploymentNodeEndVersion(session, graphTag, childDeploymentNodeName, curVersion, cmdb);
        }
    }

    public static void setDeploymentNodeEndVersion(Session session, String graphTag, String deploymentNodeName,
            String curVersion, String cmdb) {

        GraphObject deploymentNodeGraphObject = GraphObject.createGraphObject("DeploymentNode", "name",
                deploymentNodeName);

        CommonFunctions.setObjectParameter(session, graphTag, deploymentNodeGraphObject, "endVersion", curVersion);
        InfrastructureNodeEndVersionFunctions.setInfrastructureNodeEndVersion(session, graphTag, deploymentNodeName,
                curVersion);
        ContainerInstanceEndVersionFunctions.setContainerInstanceEndVersion(session, graphTag, deploymentNodeName,
                curVersion);
        setChildDeploymentNodeEndVersion(session, graphTag, deploymentNodeName, curVersion, cmdb);
    }
}
