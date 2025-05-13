package ru.beeline.architecting_graph.service.graph;

import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import ru.beeline.architecting_graph.model.*;

import java.util.HashMap;
import java.util.Map;

public class InfrastructureNodeUpdateFunctions {

    public static void setInfrastructureNodeProperties(Session session, String graphTag,
                                                       InfrastructureNode infrastructureNode) {

                if (infrastructureNode.getProperties() != null) {
                        for (Map.Entry<String, Object> entry : infrastructureNode.getProperties().entrySet()) {
                                String key = entry.getKey();
                                key = key.replaceAll("[^a-zA-Z0-9]", "_");
                                String setProperties = "MATCH (n:InfrastructureNode {graphTag: $graphTag1, name: $name1}) SET n."
                                                + key + " = $value";
                                Value parameters = Values.parameters("graphTag1", graphTag, "name1",
                                                infrastructureNode.getName(), "value", entry.getValue());
                                session.run(setProperties, parameters);
                        }
                }
        }

    public static void setParametersForInfrastructureNode(Session session, String graphTag,
                                                          InfrastructureNode infrastructureNode, GraphObject infrastructureNodeGraphObject,
                                                          String curVersion) {

        if (graphTag.equals("Global")
                && CommonFunctions.getObjectParameter(session, graphTag, infrastructureNodeGraphObject,
                "startVersion").toString().equals("NULL")) {

            CommonFunctions.setObjectParameter(session, graphTag, infrastructureNodeGraphObject,
                    "startVersion",
                    curVersion);
        }

        String updateNode = "MATCH (n:InfrastructureNode {graphTag: $graphTag1, name: $name1}) "
                + "SET n.description = $description1, n.technology = $technology1, n.tags = $tags1, "
                + "n.url = $url1, n.endVersion = $endVersion1";
        Value parameters = Values.parameters("graphTag1", graphTag, "name1", infrastructureNode.getName(),
                "description1", infrastructureNode.getDescription(), "technology1",
                infrastructureNode.getTechnology(),
                "tags1", infrastructureNode.getTags(), "url1", infrastructureNode.getUrl(),
                "endVersion1", null);
        session.run(updateNode, parameters);

        setInfrastructureNodeProperties(session, graphTag, infrastructureNode);
    }

    public static void updateInfrastructureNode(Session session, String graphTag,
                                                InfrastructureNode infrastructureNode, String curVersion,
                                                HashMap<String, GraphObject> objects) {

        GraphObject infrastructureNodeGraphObject = new GraphObject("InfrastructureNode", "name",
                infrastructureNode.getName());

        boolean exists = CommonFunctions.checkIfObjectExists(session, graphTag, infrastructureNodeGraphObject);
        if (!exists) {
            CommonFunctions.createObject(session, graphTag, infrastructureNodeGraphObject);
        }

        objects.put(infrastructureNode.getId(), infrastructureNodeGraphObject);
        setParametersForInfrastructureNode(session, graphTag, infrastructureNode, infrastructureNodeGraphObject,
                curVersion);
    }

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

    public static void updateInfrastructureNodeRelationships(Session session, String graphTag,
                                                             DeploymentNode deploymentNode, String curVersion, String cmdb, Model model,
                                                             HashMap<String, GraphObject> objects) {

        if (deploymentNode.getInfrastructureNodes() != null) {
            for (InfrastructureNode infrastructureNode : deploymentNode.getInfrastructureNodes()) {
                if (infrastructureNode.getRelationships() != null) {
                    for (Relationship relationship : infrastructureNode.getRelationships()) {
                        RelationshipUpdateFunctions.updateDefaultRelationship(session, graphTag, relationship, model,
                                curVersion, cmdb, "", objects);
                    }
                }
            }
        }
    }
}
