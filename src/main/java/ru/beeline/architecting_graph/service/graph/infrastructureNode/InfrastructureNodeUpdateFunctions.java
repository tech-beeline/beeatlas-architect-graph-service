package ru.beeline.architecting_graph.service.graph.infrastructureNode;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.Session;

import ru.beeline.architecting_graph.model.InfrastructureNode;
import ru.beeline.architecting_graph.service.graph.commonFunctions.CommonFunctions;
import ru.beeline.architecting_graph.model.GraphObject;

public class InfrastructureNodeUpdateFunctions {

        public static void setInfrastructureNodeProperties(Session session, String graphTag,
                        InfrastructureNode infrastructureNode) {

                if (infrastructureNode.getProperties() != null) {
                        for (Map.Entry<String, Object> entry : infrastructureNode.getProperties().entrySet()) {
                                String key = entry.getKey();
                                key = key.replace(' ', '_');
                                key = key.replace('.', '_');
                                String setProperties = "MATCH (n:InfrastructureNode {graphTag: $graphTag1, name: $name1}) SET n."
                                                + key
                                                + " = $value";
                                Value parameters = Values.parameters("graphTag1", graphTag, "name1",
                                                infrastructureNode.getName(),
                                                "value", entry.getValue());
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

                GraphObject infrastructureNodeGraphObject = GraphObject.createGraphObject("InfrastructureNode", "name",
                                infrastructureNode.getName());

                boolean exists = CommonFunctions.checkIfObjectExists(session, graphTag, infrastructureNodeGraphObject);
                if (!exists) {
                        CommonFunctions.createObject(session, graphTag, infrastructureNodeGraphObject);
                }

                objects.put(infrastructureNode.getId(), infrastructureNodeGraphObject);
                setParametersForInfrastructureNode(session, graphTag, infrastructureNode, infrastructureNodeGraphObject,
                                curVersion);
        }
}
