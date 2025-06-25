package ru.beeline.architecting_graph.service.createDiagrams;

import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.model.DeploymentNode;
import ru.beeline.architecting_graph.model.DiagramParameters;
import ru.beeline.architecting_graph.model.InfrastructureNode;
import ru.beeline.architecting_graph.model.RelationshipEntity;
import ru.beeline.architecting_graph.repository.neo4j.CreateDiagramsQuery;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class NodesBuilder {

    @Autowired
    CreateDiagramsQuery createDiagramsQuery;

    @Autowired
    ContainerComponentBuilder containerComponentBuilder;

    public void setInfrastructureNodeProperties(Node node, InfrastructureNode infrastructureNode) {
        for (String key : node.keys()) {
            try {
                Field field = InfrastructureNode.class.getDeclaredField(key);
                field.setAccessible(true);
                field.set(infrastructureNode, node.get(key).asObject());
            } catch (Exception e) {
                infrastructureNode.getProperties().put(key, node.get(key).asObject());
            }
        }
    }

    public void setInfrastructureNodeEnvironment(Session session, String infrastructureNodeDSLIdentifier, InfrastructureNode infrastructureNode) {
        Result result = createDiagramsQuery.getInfrastructureNodeEnvironment(session, infrastructureNodeDSLIdentifier);
        infrastructureNode.setEnvironment(result.next().get("n.name").asString());
    }

    public void getInfrastructureNode(Session session, Node node, String environment, DiagramParameters diagramParameters) {
        InfrastructureNode infrastructureNode = new InfrastructureNode();
        infrastructureNode.setProperties(new HashMap<>());
        infrastructureNode.setRelationships(new ArrayList<>());
        infrastructureNode.setId(String.valueOf(diagramParameters.getLastObjectId()));
        setInfrastructureNodeProperties(node, infrastructureNode);
        String infrastructureNodeDSLIdentifier = infrastructureNode.getProperties().get("structurizr_dsl_identifier")
                .toString();
        setInfrastructureNodeEnvironment(session, infrastructureNodeDSLIdentifier, infrastructureNode);
        if (!infrastructureNode.getEnvironment().equals(environment)) {
            return;
        }
        diagramParameters.getObjectMap().put(infrastructureNodeDSLIdentifier, diagramParameters.getLastObjectId());
        diagramParameters.setLastObjectId(diagramParameters.getLastObjectId() + 1);
        diagramParameters.getCreatedInfrastructureNodes().put(infrastructureNodeDSLIdentifier, infrastructureNode);
    }

    public void getInfrastructureNodeRelationships(Session session, InfrastructureNode infrastructureNode,
                                                   String infrastructureNodeDSLIdentifier, DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getInfrastructureNodeRelationships(session, infrastructureNodeDSLIdentifier);
        while (result.hasNext()) {
            Record record = result.next();
            String destinationDSLIdentifier = record.get("m.structurizr_dsl_identifier").asString();
            String destinationId = diagramParameters.getObjectMap().get(destinationDSLIdentifier).toString();
            RelationshipEntity relationship = containerComponentBuilder.getRelationship(record.get("r").asRelationship(), infrastructureNode.getId(),
                    destinationId, diagramParameters);
            if (relationship != null) {
                infrastructureNode.getRelationships().add(relationship);
            }
        }
    }

    public void setInfrastructureNodes(Session session, DeploymentNode deploymentNode, String deploymentNodeDSLIdentifier,
                                       DiagramParameters diagramParameters) {
        deploymentNode.setInfrastructureNodes(new ArrayList<>());
        Result result = createDiagramsQuery.getInfrastructureNodes(session, deploymentNodeDSLIdentifier);
        while (result.hasNext()) {
            Record record = result.next();
            String infrastructureNodeDSLIdentifier = record.get("m.structurizr_dsl_identifier").asString();
            InfrastructureNode infrastructureNode = diagramParameters.getCreatedInfrastructureNodes().get(infrastructureNodeDSLIdentifier);
            getInfrastructureNodeRelationships(session, infrastructureNode, infrastructureNodeDSLIdentifier, diagramParameters);
            deploymentNode.getInfrastructureNodes().add(infrastructureNode);
        }
    }

    public void getDeploymentNode(Session session, Node node, String environment, DiagramParameters diagramParameters) {
        DeploymentNode deploymentNode = new DeploymentNode();
        deploymentNode.setId(String.valueOf(diagramParameters.getLastObjectId()));
        deploymentNode.setProperties(new HashMap<>());
        setDeploymentNodeProperties(node, deploymentNode);
        String deploymentNodeDSLIdentifier = deploymentNode.getProperties().get("structurizr_dsl_identifier").toString();
        setDeploymentNodeEnvironment(session, deploymentNodeDSLIdentifier, deploymentNode);
        if (!deploymentNode.getEnvironment().equals(environment)) {
            return;
        }
        diagramParameters.getObjectMap().put(deploymentNodeDSLIdentifier, diagramParameters.getLastObjectId());
        diagramParameters.setLastObjectId(diagramParameters.getLastObjectId() + 1);
        getInfrastructureNodes(session, deploymentNodeDSLIdentifier, environment, diagramParameters);
        containerComponentBuilder.getContainerInstances(session, deploymentNodeDSLIdentifier, environment, diagramParameters);
        getChildDeploymentNodes(session, deploymentNodeDSLIdentifier, environment, diagramParameters);
        diagramParameters.getCreatedDeploymentNodes().put(deploymentNodeDSLIdentifier, deploymentNode);
    }

    public void setDeploymentNodeRelationships(Session session, DeploymentNode deploymentNode,
                                               String deploymentNodeDSLIdentifier, DiagramParameters diagramParameters) {
        deploymentNode.setRelationships(new ArrayList<>());
        Result result = createDiagramsQuery.getDeploymentNodeRelationships(session, deploymentNodeDSLIdentifier);
        while (result.hasNext()) {
            Record record = result.next();
            String destinationDSLIdentifier = record.get("m.structurizr_dsl_identifier").asString();
            String destinationId = diagramParameters.getObjectMap().get(destinationDSLIdentifier).toString();
            RelationshipEntity relationship = containerComponentBuilder.getRelationship(record.get("r").asRelationship(), deploymentNode.getId(),
                    destinationId, diagramParameters);
            if (relationship != null) {
                deploymentNode.getRelationships().add(relationship);
            }
        }
    }

    public void getChildDeploymentNodes(Session session, String deploymentNodeDSLIdentifier, String environment,
                                        DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getChildDeploymentNodes(session, deploymentNodeDSLIdentifier);
        while (result.hasNext()) {
            Record record = result.next();
            getDeploymentNode(session, record.get("m").asNode(), environment, diagramParameters);
        }
    }

    public void setDeploymentNodeProperties(Node node, DeploymentNode deploymentNode) {
        for (String key : node.keys()) {
            try {
                Field field = DeploymentNode.class.getDeclaredField(key);
                field.setAccessible(true);
                field.set(deploymentNode, node.get(key).asObject());
            } catch (Exception e) {
                deploymentNode.getProperties().put(key, node.get(key).asObject());
            }
        }
    }

    public void setDeploymentNodeEnvironment(Session session, String deploymentNodeDSLIdentifier, DeploymentNode deploymentNode) {
        Result result = createDiagramsQuery.getDeploymentNodeEnvironment(session, deploymentNodeDSLIdentifier);
        deploymentNode.setEnvironment(result.next().get("n.name").asString());
    }

    public void getInfrastructureNodes(Session session, String deploymentNodeDSLIdentifier, String environment,
                                       DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getInfrastructureNodes(session, deploymentNodeDSLIdentifier);
        while (result.hasNext()) {
            Record record = result.next();
            getInfrastructureNode(session, record.get("m").asNode(), environment, diagramParameters);
        }
    }

    public void setChildDeploymentNodes(Session session, DeploymentNode deploymentNode,
                                        String deploymentNodeDSLIdentifier, DiagramParameters diagramParameters) {
        deploymentNode.setChildren(new ArrayList<>());
        Result result = createDiagramsQuery.getChildDeploymentNodes(session, deploymentNodeDSLIdentifier);
        while (result.hasNext()) {
            Record record = result.next();
            String childDeploymentNodeDSLIdentifier = record.get("m.structurizr_dsl_identifier").asString();
            deploymentNode.getChildren().add(getDeploymentNodeRelationships(session,
                    diagramParameters.getCreatedDeploymentNodes().get(childDeploymentNodeDSLIdentifier), diagramParameters));
        }
    }

    public DeploymentNode getDeploymentNodeRelationships(Session session, DeploymentNode deploymentNode, DiagramParameters diagramParameters) {
        String deploymentNodeDSLIdentifier = deploymentNode.getProperties().get("structurizr_dsl_identifier").toString();
        setDeploymentNodeRelationships(session, deploymentNode, deploymentNodeDSLIdentifier, diagramParameters);
        setInfrastructureNodes(session, deploymentNode, deploymentNodeDSLIdentifier, diagramParameters);
        containerComponentBuilder.setContainerInstances(session, deploymentNode, deploymentNodeDSLIdentifier, diagramParameters);
        setChildDeploymentNodes(session, deploymentNode, deploymentNodeDSLIdentifier, diagramParameters);
        return deploymentNode;
    }
}
