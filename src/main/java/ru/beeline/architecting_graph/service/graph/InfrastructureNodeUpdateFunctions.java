package ru.beeline.architecting_graph.service.graph;

import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.beeline.architecting_graph.model.GraphObject;
import ru.beeline.architecting_graph.model.InfrastructureNode;
import ru.beeline.architecting_graph.repository.neo4j.BuildGraphQuery;

import java.util.HashMap;
import java.util.Map;

@Component
public class InfrastructureNodeUpdateFunctions {

    @Autowired
    BuildGraphQuery buildGraphQuery;

    public void setInfrastructureNodeProperties(Session session, String graphTag, InfrastructureNode infrastructureNode) {
        if (infrastructureNode.getProperties() != null) {
            for (Map.Entry<String, Object> entry : infrastructureNode.getProperties().entrySet()) {
                String rawKey = entry.getKey();
                String cleanedKey = rawKey.replaceAll("[^a-zA-Z0-9]", "_");
                buildGraphQuery.setInfrastructureNodeProperty(session, graphTag, infrastructureNode.getName(),
                        cleanedKey, entry.getValue());
            }
        }
    }

    public void setParametersForInfrastructureNode(Session session, String graphTag, InfrastructureNode infrastructureNode,
                                                   GraphObject infrastructureNodeGraphObject, String curVersion) {
        if ("Global".equals(graphTag)
                && buildGraphQuery.getObjectParameter(session, graphTag, infrastructureNodeGraphObject, "startVersion")
                .toString().equals("NULL")) {
            buildGraphQuery.setObjectParameter(session, graphTag, infrastructureNodeGraphObject, "startVersion", curVersion);
        }
        buildGraphQuery.updateInfrastructureNode(session, graphTag, infrastructureNode.getName(), infrastructureNode.getDescription(),
                infrastructureNode.getTechnology(), infrastructureNode.getTags(), infrastructureNode.getUrl(), null);
        setInfrastructureNodeProperties(session, graphTag, infrastructureNode);
    }


    public void updateInfrastructureNode(Session session, String graphTag, InfrastructureNode infrastructureNode, String curVersion,
                                         HashMap<String, GraphObject> objects) {
        GraphObject infrastructureNodeGraphObject = new GraphObject("InfrastructureNode", "name",
                infrastructureNode.getName());
        boolean exists = buildGraphQuery.checkIfObjectExists(session, graphTag, infrastructureNodeGraphObject);
        if (!exists) {
            buildGraphQuery.createObject(session, graphTag, infrastructureNodeGraphObject);
        }
        objects.put(infrastructureNode.getId(), infrastructureNodeGraphObject);
        setParametersForInfrastructureNode(session, graphTag, infrastructureNode, infrastructureNodeGraphObject,
                curVersion);
    }

    public void setInfrastructureNodeEndVersion(Session session, String graphTag, String deploymentNodeName, String curVersion) {
        Result result = buildGraphQuery.findInfrastructureNodesWithNullEndVersion(session, graphTag, deploymentNodeName);
        while (result.hasNext()) {
            String infrastructureNodeName = result.next().get("infrastructureNodeName").toString();
            infrastructureNodeName = infrastructureNodeName.substring(1, infrastructureNodeName.length() - 1);
            GraphObject infrastructureNodeGraphObject = new GraphObject("InfrastructureNode", "name", infrastructureNodeName);
            buildGraphQuery.setObjectParameter(session, graphTag, infrastructureNodeGraphObject, "endVersion", curVersion);
        }
    }
}
