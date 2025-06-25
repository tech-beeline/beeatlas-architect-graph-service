package ru.beeline.architecting_graph.service.createDiagrams;

import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.beeline.architecting_graph.model.*;
import ru.beeline.architecting_graph.model.ViewObjects.Views;
import ru.beeline.architecting_graph.repository.neo4j.CreateDiagramsQuery;

import java.util.*;

@Service
public class GetView {


    @Autowired
    ContainerComponentBuilder containerComponentBuilder;

    @Autowired
    SetElements setElements;

    @Autowired
    NodesBuilder nodesBuilder;

    @Autowired
    CreateDiagramsQuery createDiagramsQuery;

    public Workspace GetContextView(Session session, String softwareSystemMnemonic) {
        DiagramParameters diagramParameters = createNewDiagramParameters(session, softwareSystemMnemonic);
        createContextView(session, diagramParameters.getSystem(), softwareSystemMnemonic, diagramParameters);
        updateElements(diagramParameters.getWorkspace(), diagramParameters.getModel(), diagramParameters.getSystem(),
                softwareSystemMnemonic, diagramParameters);
        setElements.setContextView(diagramParameters.getWorkspace(), diagramParameters.getSystem(), diagramParameters);
        setElements.setContainerView(diagramParameters.getWorkspace(), diagramParameters.getSystem(), diagramParameters);
        return diagramParameters.getWorkspace();
    }

    public Workspace GetComponentView(Session session, String softwareSystemMnemonic, String containerMnemonic) {
        DiagramParameters diagramParameters = createNewDiagramParameters(session, softwareSystemMnemonic);
        containerComponentBuilder.createComponentView(session, softwareSystemMnemonic, containerMnemonic, diagramParameters.getSystem(),
                diagramParameters);
        updateElements(diagramParameters.getWorkspace(), diagramParameters.getModel(), diagramParameters.getSystem(),
                softwareSystemMnemonic, diagramParameters);
        setElements.setComponentView(diagramParameters.getWorkspace(), containerMnemonic, diagramParameters);
        return diagramParameters.getWorkspace();
    }

    public Workspace GetDeploymentView(Session session, String softwareSystemMnemonic, String environment) {
        DiagramParameters diagramParameters = createNewDiagramParameters(session, softwareSystemMnemonic);
        createDeploymentView(session, softwareSystemMnemonic, environment, diagramParameters.getModel(), diagramParameters);

        updateElements(diagramParameters.getWorkspace(), diagramParameters.getModel(), diagramParameters.getSystem(),
                softwareSystemMnemonic, diagramParameters);

        setElements.setDeploymentView(diagramParameters.getWorkspace(), diagramParameters.getModel(), diagramParameters.getSystem().getId(),
                environment, diagramParameters);
        return diagramParameters.getWorkspace();
    }

    void createDeploymentView(Session session, String softwareSystemMnemonic, String environment, Model model,
                              DiagramParameters diagramParameters) {
        Result result = createDiagramsQuery.getContainers(session, softwareSystemMnemonic);
        while (result.hasNext()) {
            Record record = result.next();
            containerComponentBuilder.getContainer(session, record.get("m").asNode(), diagramParameters);
        }
        result = createDiagramsQuery.getDeploymentNodes(session, softwareSystemMnemonic);
        while (result.hasNext()) {
            Record record = result.next();
            nodesBuilder.getDeploymentNode(session, record.get("m").asNode(), environment, diagramParameters);
        }
        model.setDeploymentNodes(new ArrayList<>());
        result = createDiagramsQuery.getDeploymentNodes(session, softwareSystemMnemonic);
        while (result.hasNext()) {
            Record record = result.next();
            String deploymentNodeDSLIdentifier = record.get("m.structurizr_dsl_identifier").asString();
            DeploymentNode deploymentNode = diagramParameters.getCreatedDeploymentNodes().get(deploymentNodeDSLIdentifier);
            if (deploymentNode != null) {
                model.getDeploymentNodes()
                        .add(nodesBuilder.getDeploymentNodeRelationships(session, deploymentNode, diagramParameters));
            }
        }
    }

    public void createContextView(Session session, SoftwareSystem system, String softwareSystemMnemonic, DiagramParameters diagramParameters) {
        containerComponentBuilder.getContainers(session, softwareSystemMnemonic, diagramParameters);
        containerComponentBuilder.getSystemRelationships(session, system, softwareSystemMnemonic, diagramParameters);
        containerComponentBuilder.getContainerRelationships(session, system, softwareSystemMnemonic, true, diagramParameters);

    }

    public DiagramParameters createNewDiagramParameters(Session session, String softwareSystemMnemonic) {
        DiagramParameters diagramParameters = new DiagramParameters();
        diagramParameters.setLastObjectId(2L);
        diagramParameters.setObjectMap(new HashMap<>());
        diagramParameters.setParents(new HashMap<>());
        diagramParameters.setSystemChilds(new HashSet<>());
        diagramParameters.setCreatedRelationships(new HashSet<>());
        diagramParameters.setViewObjects(new HashSet<>());
        diagramParameters.setCreatedSystems(new HashMap<>());
        diagramParameters.setCreatedContainers(new HashMap<>());
        diagramParameters.setCreatedComponents(new HashMap<>());
        diagramParameters.setCreatedDeploymentNodes(new HashMap<>());
        diagramParameters.setCreatedInfrastructureNodes(new HashMap<>());
        diagramParameters.setCreatedContainerInstances(new HashMap<>());
        diagramParameters.setWorkspace(new Workspace());
        diagramParameters.setModel(new Model());
        diagramParameters.setSystem(containerComponentBuilder.getSystem(session, softwareSystemMnemonic, diagramParameters));
        return diagramParameters;
    }

    public void updateElements(Workspace workspace, Model model, SoftwareSystem system, String softwareSystemMnemonic,
                               DiagramParameters diagramParameters) {
        diagramParameters.getCreatedSystems().put(softwareSystemMnemonic, system);
        for (Map.Entry<String, SoftwareSystem> entry : diagramParameters.getCreatedSystems().entrySet()) {
            SoftwareSystem currentSystem = entry.getValue();
            String systemDSLIdentifier = currentSystem.getProperties().get("structurizr_dsl_identifier").toString();
            List<Container> containers = updateSystemContainers(currentSystem, diagramParameters);
            currentSystem.setContainers(containers);
            diagramParameters.getCreatedSystems().put(systemDSLIdentifier, currentSystem);
        }
        system = diagramParameters.getCreatedSystems().get(softwareSystemMnemonic);
        workspace.setId(1L);
        workspace.setViews(new Views());
        model.setSoftwareSystems(new ArrayList<>());
        for (Map.Entry<String, SoftwareSystem> entry : diagramParameters.getCreatedSystems().entrySet()) {
            model.getSoftwareSystems().add(entry.getValue());
        }
        workspace.setModel(model);
    }

    public List<Component> updateContainerComponents(Container container, DiagramParameters diagramParameters) {
        List<Component> components = new ArrayList<>();
        for (Component component : container.getComponents()) {
            String componentDSLIdentifier = component.getProperties().get("structurizr_dsl_identifier").toString();
            components.add(diagramParameters.getCreatedComponents().get(componentDSLIdentifier));
        }
        return components;
    }

    public List<Container> updateSystemContainers(SoftwareSystem system, DiagramParameters diagramParameters) {
        List<Container> containers = new ArrayList<>();
        for (Container container : system.getContainers()) {
            List<Component> components = updateContainerComponents(container, diagramParameters);
            String containerDSLIdentifier = container.getProperties().get("structurizr_dsl_identifier").toString();
            Container updatedContainer = diagramParameters.getCreatedContainers().get(containerDSLIdentifier);
            updatedContainer.setComponents(components);
            diagramParameters.getCreatedContainers().put(containerDSLIdentifier, updatedContainer);
            containers.add(updatedContainer);
        }
        return containers;
    }
}
