package ru.beeline.architecting_graph.service.createDiagrams;

import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.model.*;
import ru.beeline.architecting_graph.repository.neo4j.CreateDiagramsQuery;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class ContainerComponentBuilder {

    @Autowired
    CreateDiagramsQuery createDiagramsQuery;

    public void getComponent(Session session, Node node, DiagramParameters diagramParameters) {
        Component component = new Component();
        component.setProperties(new HashMap<>());
        component.setRelationships(new ArrayList<>());
        component.setId(String.valueOf(diagramParameters.getLastObjectId()));
        setComponentProperties(node, component);
        String componentDSLIdentifier = component.getProperties().get("structurizr_dsl_identifier").toString();
        diagramParameters.getObjectMap().put(componentDSLIdentifier, diagramParameters.getLastObjectId());
        diagramParameters.getCreatedComponents().put(componentDSLIdentifier, component);
        diagramParameters.setLastObjectId(diagramParameters.getLastObjectId() + 1);
        addComponentToContainer(session, component, diagramParameters);
    }

    public void setContainerProperties(Node node, Container container) {
        BeanWrapper wrapper = new BeanWrapperImpl(container);
        for (String key : node.keys()) {
            Object value = node.get(key).asObject();
            if (wrapper.isWritableProperty(key)) {
                wrapper.setPropertyValue(key, value);
            } else {
                container.getProperties().put(key, value);
            }
        }
    }

    public void setComponentProperties(Node node, Component component) {
        for (String key : node.keys()) {
            try {
                Field field = Component.class.getDeclaredField(key);
                field.setAccessible(true);
                field.set(component, node.get(key).asObject());
            } catch (Exception e) {
                component.getProperties().put(key, node.get(key).asObject());
            }
        }
    }

    public void addComponentToContainer(Session session, Component component, DiagramParameters diagramParameters) {

        String componentDSLIdentifier = component.getProperties().get("structurizr_dsl_identifier").toString();
        Result result = createDiagramsQuery.getParentContainer(session, componentDSLIdentifier);
        Record record = result.next();
        String containerDSLIdentifier = record.get("m.structurizr_dsl_identifier").asString();
        if (!diagramParameters.getObjectMap().containsKey(containerDSLIdentifier)) {
            getContainer(session, record.get("m").asNode(), diagramParameters);
        }
        Container container = diagramParameters.getCreatedContainers().get(containerDSLIdentifier);
        container.getComponents().add(component);
        diagramParameters.getCreatedContainers().put(containerDSLIdentifier, container);
        diagramParameters.getParents().put(componentDSLIdentifier, containerDSLIdentifier);
    }

    public void getDirectComponentRelationships(Session session, Component component, SoftwareSystem system,
                                                Container container, String componentDSLIndentifier, Boolean needAddingToSystem,
                                                Boolean needAddingToContainer, DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getDirectComponentRelationships(session, componentDSLIndentifier);
        while (result.hasNext()) {
            Record record = result.next();
            Node destinationNode = record.get("m").asNode();
            Relationship relation = record.get("r").asRelationship();
            String destinationDSLIdentifier = record.get("m.structurizr_dsl_identifier").asString();
            String destinationSystemId = getExternalSystem(session, destinationNode, destinationDSLIdentifier, diagramParameters).getId();
            RelationshipEntity relationship = addDirectRelationship(component.getId(), system, container, destinationSystemId,
                    destinationDSLIdentifier, relation, needAddingToSystem, needAddingToContainer, diagramParameters);
            if (relationship != null) {
                component.getRelationships().add(relationship);
            }
        }
    }

    public Container getContainer(Session session, Node node, DiagramParameters diagramParameters) {
        Long id = diagramParameters.getLastObjectId();
        Container container = new Container();
        container.setProperties(new HashMap<>());
        container.setRelationships(new ArrayList<>());
        container.setComponents(new ArrayList<>());
        container.setId(String.valueOf(id));
        setContainerProperties(node, container);
        String containerDSLIdentifier = container.getProperties().get("structurizr_dsl_identifier").toString();
        diagramParameters.getObjectMap().put(containerDSLIdentifier, diagramParameters.getLastObjectId());
        diagramParameters.getCreatedContainers().put(containerDSLIdentifier, container);
        diagramParameters.setLastObjectId(id + 1);
        addContainerToSystem(session, container, diagramParameters);
        return container;
    }

    public void getComponents(Session session, String containerDSLIdentifier, DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getComponents(session, containerDSLIdentifier);
        while (result.hasNext()) {
            Record record = result.next();
            diagramParameters.getSystemChilds().add(diagramParameters.getLastObjectId().toString());
            getComponent(session, record.get("m").asNode(), diagramParameters);
        }
    }

    public void getContainerRelationships(Session session, SoftwareSystem system, String systemtDSLIndentifier,
                                          Boolean needAddingToSystem, DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getContainers(session, systemtDSLIndentifier);
        while (result.hasNext()) {
            Record record = result.next();
            String containerDSLIndentifier = record.get("m.structurizr_dsl_identifier").asString();
            Container container = diagramParameters.getCreatedContainers().get(containerDSLIndentifier);
            getDirectContainerRelationships(session, container, system, containerDSLIndentifier, needAddingToSystem, diagramParameters);
            getReverseContainerRelationships(session, container, system, containerDSLIndentifier, needAddingToSystem, diagramParameters);
            getComponentRelationships(session, containerDSLIndentifier, system, container, needAddingToSystem, true,
                    diagramParameters);
            diagramParameters.getCreatedContainers().put(containerDSLIndentifier, container);
        }
    }

    public void createComponentView(Session session, String softwareSystemMnemonic, String containerMnemonic,
                                    SoftwareSystem system, DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getContainer(session, softwareSystemMnemonic, containerMnemonic);
        getContainer(session, result.next().get("m").asNode(), diagramParameters);
        getComponents(session, containerMnemonic, diagramParameters);
        getComponentRelationships(session, containerMnemonic, system, null, false, false, diagramParameters);
    }

    public void getContainers(Session session, String softwareSystemMnemonic, DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getContainers(session, softwareSystemMnemonic);
        while (result.hasNext()) {
            Record record = result.next();
            diagramParameters.getSystemChilds().add(diagramParameters.getLastObjectId().toString());
            getContainer(session, record.get("m").asNode(), diagramParameters);
            String containerDSLIdentifier = record.get("m.structurizr_dsl_identifier").asString();
            getComponents(session, containerDSLIdentifier, diagramParameters);
        }
    }

    public void getReverseComponentRelationships(Session session, Component component, String systemId, String containerId,
                                                 String componentDSLIndentifier, Boolean needAddingToSystem,
                                                 Boolean needAddingToContainer, DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getReverseComponentRelationships(session, componentDSLIndentifier);
        while (result.hasNext()) {
            Record record = result.next();
            Node sourceNode = record.get("m").asNode();
            Relationship relation = record.get("r").asRelationship();
            String sourceDSLIdentifier = record.get("m.structurizr_dsl_identifier").asString();
            if (diagramParameters.getObjectMap().containsKey(sourceDSLIdentifier) && diagramParameters.getSystemChilds()
                    .contains(diagramParameters.getObjectMap().get(sourceDSLIdentifier).toString())) {
                continue;
            }
            SoftwareSystem sourceSystem = getExternalSystem(session, sourceNode, sourceDSLIdentifier, diagramParameters);
            addReverseRelationship(sourceSystem, component.getId(), systemId, containerId, sourceDSLIdentifier,
                    relation, needAddingToSystem, needAddingToContainer, diagramParameters);
        }
    }

    public void getComponentRelationships(Session session, String containerDSLIdentifier, SoftwareSystem system,
                                          Container container, Boolean needAddingToSystem, Boolean needAddingToContainer,
                                          DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getComponents(session, containerDSLIdentifier);
        while (result.hasNext()) {
            Record record = result.next();
            String componentDSLIndentifier = record.get("m.structurizr_dsl_identifier").asString();
            Component component = diagramParameters.getCreatedComponents().get(componentDSLIndentifier);
            getDirectComponentRelationships(session, component, system, container, componentDSLIndentifier,
                    needAddingToSystem, needAddingToContainer, diagramParameters);
            getReverseComponentRelationships(session, component, system.getId(), null,
                    componentDSLIndentifier, needAddingToSystem, needAddingToContainer, diagramParameters);
            diagramParameters.getCreatedComponents().put(componentDSLIndentifier, component);
        }
    }

    public void setContainerInstances(Session session, DeploymentNode deploymentNode,
                                      String deploymentNodeDSLIdentifier, DiagramParameters diagramParameters) {
        deploymentNode.setContainerInstances(new ArrayList<>());
        Result result = createDiagramsQuery.getContainerInstances(session, deploymentNodeDSLIdentifier);
        while (result.hasNext()) {
            Record record = result.next();
            String containerInstanceDSLIdentifier = record.get("m.structurizr_dsl_identifier").asString();
            ContainerInstance containerInstance = diagramParameters.getCreatedContainerInstances().get(containerInstanceDSLIdentifier);
            getContainerInstanceRelationships(session, containerInstance, containerInstanceDSLIdentifier, diagramParameters);
            deploymentNode.getContainerInstances().add(containerInstance);
        }
    }

    public void setContainerInstanceProperties(Node node, ContainerInstance containerInstance) {
        for (String key : node.keys()) {
            try {
                Field field = ContainerInstance.class.getDeclaredField(key);
                field.setAccessible(true);
                field.set(containerInstance, node.get(key).asObject());
            } catch (Exception e) {
                containerInstance.getProperties().put(key, node.get(key).asObject());
            }
        }
    }

    public void setContainerInstanceEnvironment(Session session, String containerInstanceDSLIdentifier,
                                                ContainerInstance containerInstance) {
        Result result = createDiagramsQuery.getContainerInstanceEnvironment(session, containerInstanceDSLIdentifier);
        containerInstance.setEnvironment(result.next().get("n.name").asString());
    }

    public void setContainerInstanceContainerId(Session session, String containerInstanceDSLIdentifier,
                                                ContainerInstance containerInstance, DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getContainerInstanceContainerId(session, containerInstanceDSLIdentifier);
        String containerDSLIdentifier = result.next().get("n.structurizr_dsl_identifier").asString();
        containerInstance.setContainerId(diagramParameters.getObjectMap().get(containerDSLIdentifier).toString());
    }

    public void getContainerInstance(Session session, Node node, String environment, DiagramParameters diagramParameters) {
        Long id = diagramParameters.getLastObjectId();
        ContainerInstance containerInstance = new ContainerInstance();
        containerInstance.setProperties(new HashMap<>());
        containerInstance.setRelationships(new ArrayList<>());
        containerInstance.setId(String.valueOf(id));
        setContainerInstanceProperties(node, containerInstance);
        String containerInstanceDSLIdentifier = containerInstance.getProperties().get("structurizr_dsl_identifier").toString();
        setContainerInstanceEnvironment(session, containerInstanceDSLIdentifier, containerInstance);
        if (!containerInstance.getEnvironment().equals(environment)) {
            return;
        }
        setContainerInstanceContainerId(session, containerInstanceDSLIdentifier, containerInstance, diagramParameters);
        diagramParameters.getObjectMap().put(containerInstanceDSLIdentifier, id);
        diagramParameters.setLastObjectId(id + 1);
        diagramParameters.getCreatedContainerInstances().put(containerInstanceDSLIdentifier, containerInstance);
    }

    public void getContainerInstances(Session session, String deploymentNodeDSLIdentifier, String environment,
                                      DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getContainerInstances(session, deploymentNodeDSLIdentifier);
        while (result.hasNext()) {
            Record record = result.next();
            getContainerInstance(session, record.get("m").asNode(), environment, diagramParameters);
        }
    }

    public SoftwareSystem getSystem(Session session, String systemDSLIdentifier, DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getSystem(session, systemDSLIdentifier);
        Node node = result.next().get("n").asNode();
        SoftwareSystem softwareSystem = new SoftwareSystem();
        softwareSystem.setProperties(new HashMap<>());
        softwareSystem.setRelationships(new ArrayList<>());
        softwareSystem.setContainers(new ArrayList<>());
        softwareSystem.setId(String.valueOf(diagramParameters.getLastObjectId()));
        setSystemProperties(node, softwareSystem);
        diagramParameters.getObjectMap().put(systemDSLIdentifier, diagramParameters.getLastObjectId());
        diagramParameters.getCreatedSystems().put(systemDSLIdentifier, softwareSystem);
        diagramParameters.setLastObjectId(diagramParameters.getLastObjectId() + 1);
        return softwareSystem;
    }

    public void setSystemProperties(Node node, SoftwareSystem softwareSystem) {
        for (String key : node.keys()) {
            try {
                Field field = SoftwareSystem.class.getDeclaredField(key);
                field.setAccessible(true);
                field.set(softwareSystem, node.get(key).asObject());
            } catch (Exception e) {
                softwareSystem.getProperties().put(key, node.get(key).asObject());
            }
        }
    }

    public void addContainerToSystem(Session session, Container container, DiagramParameters diagramParameters) {
        String containerDSLIdentifier = container.getProperties().get("structurizr_dsl_identifier").toString();
        Result result = createDiagramsQuery.getParentSystem(session, containerDSLIdentifier);
        String systemDSLIdentifier = result.next().get("m.structurizr_dsl_identifier").asString();
        if (!diagramParameters.getObjectMap().containsKey(systemDSLIdentifier)) {
            getSystem(session, systemDSLIdentifier, diagramParameters);
        }
        SoftwareSystem system = diagramParameters.getCreatedSystems().get(systemDSLIdentifier);
        system.getContainers().add(container);
        diagramParameters.getCreatedSystems().put(systemDSLIdentifier, system);
        diagramParameters.getParents().put(containerDSLIdentifier, systemDSLIdentifier);
    }

    public SoftwareSystem getExternalSystem(Session session, Node node, String nodeDSLIdentifier, DiagramParameters diagramParameters) {
        String label = node.labels().toString();
        label = label.substring(1, label.length() - 1);
        switch (label) {
            case "SoftwareSystem":
                if (!diagramParameters.getObjectMap().containsKey(nodeDSLIdentifier)) {
                    getSystem(session, nodeDSLIdentifier, diagramParameters);
                }
                return diagramParameters.getCreatedSystems().get(nodeDSLIdentifier);
            case "Container":
                if (!diagramParameters.getObjectMap().containsKey(nodeDSLIdentifier)) {
                    getContainer(session, node, diagramParameters);
                }
                return diagramParameters.getCreatedSystems().get(diagramParameters.getParents().get(nodeDSLIdentifier));
            case "Component":
                if (!diagramParameters.getObjectMap().containsKey(nodeDSLIdentifier)) {
                    getComponent(session, node, diagramParameters);
                }
                return diagramParameters.getCreatedSystems()
                        .get(diagramParameters.getParents().get(diagramParameters.getParents().get(nodeDSLIdentifier)));
            default:
                return null;
        }
    }

    public void getDirectSystemRelationships(Session session, SoftwareSystem system, String systemtDSLIndentifier,
                                             DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getDirectSystemRelationships(session, systemtDSLIndentifier);
        while (result.hasNext()) {
            Record record = result.next();
            Node destinationNode = record.get("m").asNode();
            Relationship relation = record.get("r").asRelationship();
            String destinationDSLIdentifier = record.get("m.structurizr_dsl_identifier").asString();
            String destinationSystemId = getExternalSystem(session, destinationNode, destinationDSLIdentifier, diagramParameters).getId();
            RelationshipEntity relationship = getRelationship(relation, system.getId(), destinationSystemId, diagramParameters);
            if (relationship != null) {
                system.getRelationships().add(relationship);
            }
        }
    }

    public void getReverseSystemRelationships(Session session, SoftwareSystem system, String systemtDSLIndentifier,
                                              DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getReverseComponentRelationships(session, systemtDSLIndentifier);
        while (result.hasNext()) {
            Record record = result.next();
            Node sourceNode = record.get("m").asNode();
            Relationship relation = record.get("r").asRelationship();
            String sourceDSLIdentifier = record.get("m.structurizr_dsl_identifier").asString();
            if (diagramParameters.getObjectMap().containsKey(sourceDSLIdentifier) && diagramParameters.getSystemChilds()
                    .contains(diagramParameters.getObjectMap().get(sourceDSLIdentifier).toString())) {
                continue;
            }
            SoftwareSystem sourceSystem = getExternalSystem(session, sourceNode, sourceDSLIdentifier, diagramParameters);
            addReverseRelationship(sourceSystem, system.getId(), system.getId(), null, sourceDSLIdentifier, relation,
                    false, false, diagramParameters);
        }
    }

    public void getSystemRelationships(Session session, SoftwareSystem system, String systemtDSLIndentifier, DiagramParameters diagramParameters) {
        getDirectSystemRelationships(session, system, systemtDSLIndentifier, diagramParameters);
        getReverseSystemRelationships(session, system, systemtDSLIndentifier, diagramParameters);
    }

    public void getReverseContainerRelationships(Session session, Container container, SoftwareSystem system, String containerDSLIndentifier,
                                                 Boolean needAddingToSystem, DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getReverseContainerRelationships(session, containerDSLIndentifier);
        while (result.hasNext()) {
            Record record = result.next();
            Node sourceNode = record.get("m").asNode();
            Relationship relation = record.get("r").asRelationship();
            String sourceDSLIdentifier = record.get("m.structurizr_dsl_identifier").asString();
            if (diagramParameters.getObjectMap().containsKey(sourceDSLIdentifier) && diagramParameters.getSystemChilds()
                    .contains(diagramParameters.getObjectMap().get(sourceDSLIdentifier).toString())) {
                continue;
            }
            SoftwareSystem sourceSystem = getExternalSystem(session, sourceNode, sourceDSLIdentifier, diagramParameters);
            addReverseRelationship(sourceSystem, container.getId(), system.getId(), null, sourceDSLIdentifier, relation,
                    needAddingToSystem, false, diagramParameters);
        }
    }


    public void getDirectContainerRelationships(Session session, Container container, SoftwareSystem system, String containerDSLIndentifier,
                                                Boolean needAddingToSystem, DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getDirectContainerRelationships(session, containerDSLIndentifier);
        while (result.hasNext()) {
            Record record = result.next();
            Node destinationNode = record.get("m").asNode();
            Relationship relation = record.get("r").asRelationship();
            String destinationDSLIdentifier = record.get("m.structurizr_dsl_identifier").asString();
            String destinationSystemId = getExternalSystem(session, destinationNode, destinationDSLIdentifier,
                    diagramParameters).getId();
            RelationshipEntity relationship = addDirectRelationship(container.getId(), system, null, destinationSystemId,
                    destinationDSLIdentifier, relation, needAddingToSystem, false, diagramParameters);
            if (relationship != null) {
                container.getRelationships().add(relationship);
            }
        }
    }

    public void addReverseRelationship(SoftwareSystem sourceSystem, String destinationtId, String systemId,
                                       String containerId, String sourceDSLIdentifier, Relationship relation,
                                       Boolean needAddingToSystem, Boolean needAddingToContainer, DiagramParameters diagramParameters) {
        RelationshipEntity relationship = getRelationship(relation, sourceSystem.getId(), destinationtId, diagramParameters);
        if (relationship != null) {
            sourceSystem.getRelationships().add(relationship);
        }
        if (needAddingToSystem && !sourceSystem.getId().equals(systemId)) {
            relationship = getRelationship(relation, sourceSystem.getId(), systemId, diagramParameters);
            if (relationship != null) {
                sourceSystem.getRelationships().add(relationship);
            }
        }
        if (needAddingToContainer && !sourceSystem.getId().equals(systemId)) {
            relationship = getRelationship(relation, sourceSystem.getId(), containerId, diagramParameters);
            if (relationship != null) {
                sourceSystem.getRelationships().add(relationship);
            }
        }
        diagramParameters.getCreatedSystems().put(sourceDSLIdentifier, sourceSystem);
    }

    public RelationshipEntity getRelationship(Relationship relation, String source, String destination, DiagramParameters diagramParameters) {
        RelationshipEntity relationship = new RelationshipEntity();
        relationship.setProperties(new HashMap<>());
        relationship.setSourceId(source);
        relationship.setDestinationId(destination);
        setRelationshipProperties(relation, relationship);
        if (checkIfRelationshipExists(relationship, source, destination, diagramParameters)) {
            return null;
        }
        relationship.setId(String.valueOf(diagramParameters.getLastObjectId()));
        diagramParameters.setLastObjectId(diagramParameters.getLastObjectId() + 1);
        return relationship;
    }

    public void setRelationshipProperties(Relationship relation, RelationshipEntity relationship) {
        for (Map.Entry<String, Object> entry : relation.asMap().entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            try {
                Field field = RelationshipEntity.class.getDeclaredField(key);
                field.setAccessible(true);
                field.set(relationship, val);
            } catch (Exception e) {
                relationship.getProperties().put(key, val);
            }
        }
    }

    public Boolean checkIfRelationshipExists(RelationshipEntity relationship, String source, String destination,
                                             DiagramParameters diagramParameters) {
        Edge edge = new Edge(source, destination, relationship.getDescription());
        if (diagramParameters.getCreatedRelationships().contains(edge)) {
            return true;
        }
        diagramParameters.getCreatedRelationships().add(edge);
        return false;
    }

    public RelationshipEntity addDirectRelationship(String sourceId, SoftwareSystem system, Container container,
                                                    String destinationSystemId, String destinationDSLIdentifier, Relationship relation,
                                                    Boolean needAddingToSystem, Boolean needAddingToContainer, DiagramParameters diagramParameters) {
        if (!destinationSystemId.equals(system.getId())) {
            RelationshipEntity relationship = getRelationship(relation, sourceId, destinationSystemId, diagramParameters);
            if (needAddingToSystem) {
                RelationshipEntity systemRelationship = getRelationship(relation, system.getId(), destinationSystemId, diagramParameters);
                if (systemRelationship != null) {
                    system.getRelationships().add(systemRelationship);
                }
            }
            if (needAddingToContainer) {
                RelationshipEntity containerRelationship = getRelationship(relation, container.getId(), destinationSystemId,
                        diagramParameters);
                if (containerRelationship != null) {
                    container.getRelationships().add(containerRelationship);
                }
            }
            return relationship;
        } else {
            String destinationId = diagramParameters.getObjectMap().get(destinationDSLIdentifier).toString();
            return getRelationship(relation, sourceId, destinationId, diagramParameters);
        }
    }

    public void getContainerInstanceRelationships(Session session, ContainerInstance containerInstance,
                                                  String containerInstanceDSLIdentifier, DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getContainerInstanceRelationships(session, containerInstanceDSLIdentifier);
        while (result.hasNext()) {
            Record record = result.next();
            String destinationDSLIdentifier = record.get("m.structurizr_dsl_identifier").asString();
            String destinationId = diagramParameters.getObjectMap().get(destinationDSLIdentifier).toString();
            RelationshipEntity relationship = getRelationship(record.get("r").asRelationship(), containerInstance.getId(),
                    destinationId, diagramParameters);
            if (relationship != null) {
                containerInstance.getRelationships().add(relationship);
            }
        }
    }
}
